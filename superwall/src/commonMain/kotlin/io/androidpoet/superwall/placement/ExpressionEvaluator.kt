package io.androidpoet.superwall.placement

/**
 * Evaluates trigger rule expressions against user attributes and event parameters.
 *
 * Supports a subset of expression syntax used by Superwall's backend:
 * - Comparison operators: ==, !=, <, >, <=, >=
 * - Logical operators: &&, ||, !
 * - Property access: user.attribute_name, params.param_name, device.property
 * - Literal values: strings ("..."), numbers, booleans (true/false), null
 * - Containment: in, contains
 */
internal class ExpressionEvaluator(
  private val userAttributes: Map<String, Any?>,
  private val params: Map<String, Any?>,
  private val deviceAttributes: Map<String, Any?> = emptyMap(),
) {

  /**
   * Evaluate an expression string against the current context.
   * Returns true if the expression matches, false otherwise.
   * Returns true for null/empty expressions (no filter = always match).
   */
  fun evaluate(expression: String?): Boolean {
    if (expression.isNullOrBlank()) return true

    return try {
      val tokens = tokenize(expression)
      val result = parseExpression(tokens, 0)
      result.value as? Boolean ?: false
    } catch (_: Exception) {
      // Expression evaluation failure = rule doesn't match
      false
    }
  }

  private data class EvalResult(val value: Any?, val nextIndex: Int)

  private fun parseExpression(tokens: List<Token>, startIndex: Int): EvalResult {
    var result = parseComparison(tokens, startIndex)

    var index = result.nextIndex
    while (index < tokens.size) {
      when (tokens[index]) {
        is Token.And -> {
          val right = parseComparison(tokens, index + 1)
          result = EvalResult(
            (result.value as? Boolean ?: false) && (right.value as? Boolean ?: false),
            right.nextIndex,
          )
          index = result.nextIndex
        }
        is Token.Or -> {
          val right = parseComparison(tokens, index + 1)
          result = EvalResult(
            (result.value as? Boolean ?: false) || (right.value as? Boolean ?: false),
            right.nextIndex,
          )
          index = result.nextIndex
        }
        else -> break
      }
    }

    return result
  }

  private fun parseComparison(tokens: List<Token>, startIndex: Int): EvalResult {
    if (startIndex >= tokens.size) return EvalResult(false, startIndex)

    // Handle NOT operator
    if (tokens[startIndex] is Token.Not) {
      val inner = parseComparison(tokens, startIndex + 1)
      return EvalResult(!(inner.value as? Boolean ?: false), inner.nextIndex)
    }

    // Handle parentheses
    if (tokens[startIndex] is Token.OpenParen) {
      val inner = parseExpression(tokens, startIndex + 1)
      val closeIndex = if (inner.nextIndex < tokens.size && tokens[inner.nextIndex] is Token.CloseParen) {
        inner.nextIndex + 1
      } else {
        inner.nextIndex
      }
      return EvalResult(inner.value, closeIndex)
    }

    val left = resolveValue(tokens, startIndex)
    var index = left.nextIndex

    if (index >= tokens.size) {
      // Single value — treat as boolean
      return EvalResult(isTruthy(left.value), index)
    }

    val operator = tokens.getOrNull(index) ?: return EvalResult(isTruthy(left.value), index)

    return when (operator) {
      is Token.Eq -> {
        val right = resolveValue(tokens, index + 1)
        EvalResult(left.value == right.value, right.nextIndex)
      }
      is Token.Neq -> {
        val right = resolveValue(tokens, index + 1)
        EvalResult(left.value != right.value, right.nextIndex)
      }
      is Token.Lt -> {
        val right = resolveValue(tokens, index + 1)
        EvalResult(compareValues(left.value, right.value) < 0, right.nextIndex)
      }
      is Token.Gt -> {
        val right = resolveValue(tokens, index + 1)
        EvalResult(compareValues(left.value, right.value) > 0, right.nextIndex)
      }
      is Token.Lte -> {
        val right = resolveValue(tokens, index + 1)
        EvalResult(compareValues(left.value, right.value) <= 0, right.nextIndex)
      }
      is Token.Gte -> {
        val right = resolveValue(tokens, index + 1)
        EvalResult(compareValues(left.value, right.value) >= 0, right.nextIndex)
      }
      is Token.Contains -> {
        val right = resolveValue(tokens, index + 1)
        val leftStr = left.value?.toString() ?: ""
        val rightStr = right.value?.toString() ?: ""
        EvalResult(leftStr.contains(rightStr), right.nextIndex)
      }
      is Token.In -> {
        val right = resolveValue(tokens, index + 1)
        val collection = right.value
        val result = when (collection) {
          is List<*> -> collection.contains(left.value)
          is Set<*> -> collection.contains(left.value)
          is String -> collection.contains(left.value?.toString() ?: "")
          else -> false
        }
        EvalResult(result, right.nextIndex)
      }
      else -> EvalResult(isTruthy(left.value), index)
    }
  }

  private fun resolveValue(tokens: List<Token>, index: Int): EvalResult {
    if (index >= tokens.size) return EvalResult(null, index)

    return when (val token = tokens[index]) {
      is Token.StringLiteral -> EvalResult(token.value, index + 1)
      is Token.NumberLiteral -> EvalResult(token.value, index + 1)
      is Token.BooleanLiteral -> EvalResult(token.value, index + 1)
      is Token.NullLiteral -> EvalResult(null, index + 1)
      is Token.Identifier -> {
        val value = resolveIdentifier(token.name)
        EvalResult(value, index + 1)
      }
      is Token.OpenParen -> parseExpression(tokens, index)
      else -> EvalResult(null, index + 1)
    }
  }

  private fun resolveIdentifier(name: String): Any? {
    val parts = name.split(".")
    return when {
      parts.size >= 2 && parts[0] == "user" -> {
        userAttributes[parts.drop(1).joinToString(".")]
      }
      parts.size >= 2 && parts[0] == "params" -> {
        params[parts.drop(1).joinToString(".")]
      }
      parts.size >= 2 && parts[0] == "device" -> {
        deviceAttributes[parts.drop(1).joinToString(".")]
      }
      else -> {
        // Try all contexts in order
        userAttributes[name] ?: params[name] ?: deviceAttributes[name]
      }
    }
  }

  private fun isTruthy(value: Any?): Boolean = when (value) {
    null -> false
    is Boolean -> value
    is Number -> value.toDouble() != 0.0
    is String -> value.isNotEmpty()
    else -> true
  }

  private fun compareValues(left: Any?, right: Any?): Int {
    if (left == null && right == null) return 0
    if (left == null) return -1
    if (right == null) return 1

    val leftNum = toNumber(left)
    val rightNum = toNumber(right)
    if (leftNum != null && rightNum != null) {
      return leftNum.compareTo(rightNum)
    }

    return left.toString().compareTo(right.toString())
  }

  private fun toNumber(value: Any?): Double? = when (value) {
    is Number -> value.toDouble()
    is String -> value.toDoubleOrNull()
    else -> null
  }

  // ── Tokenizer ────────────────────────────────────────────────────

  private sealed interface Token {
    data class StringLiteral(val value: String) : Token
    data class NumberLiteral(val value: Double) : Token
    data class BooleanLiteral(val value: Boolean) : Token
    data object NullLiteral : Token
    data class Identifier(val name: String) : Token
    data object Eq : Token
    data object Neq : Token
    data object Lt : Token
    data object Gt : Token
    data object Lte : Token
    data object Gte : Token
    data object And : Token
    data object Or : Token
    data object Not : Token
    data object Contains : Token
    data object In : Token
    data object OpenParen : Token
    data object CloseParen : Token
  }

  private fun tokenize(expression: String): List<Token> {
    val tokens = mutableListOf<Token>()
    var i = 0
    val len = expression.length

    while (i < len) {
      when {
        expression[i].isWhitespace() -> i++

        expression[i] == '"' || expression[i] == '\'' -> {
          val quote = expression[i]
          i++
          val start = i
          while (i < len && expression[i] != quote) i++
          tokens.add(Token.StringLiteral(expression.substring(start, i)))
          if (i < len) i++ // skip closing quote
        }

        expression[i] == '(' -> { tokens.add(Token.OpenParen); i++ }
        expression[i] == ')' -> { tokens.add(Token.CloseParen); i++ }

        expression.startsWith("==", i) -> { tokens.add(Token.Eq); i += 2 }
        expression.startsWith("!=", i) -> { tokens.add(Token.Neq); i += 2 }
        expression.startsWith("<=", i) -> { tokens.add(Token.Lte); i += 2 }
        expression.startsWith(">=", i) -> { tokens.add(Token.Gte); i += 2 }
        expression.startsWith("&&", i) -> { tokens.add(Token.And); i += 2 }
        expression.startsWith("||", i) -> { tokens.add(Token.Or); i += 2 }
        expression[i] == '<' -> { tokens.add(Token.Lt); i++ }
        expression[i] == '>' -> { tokens.add(Token.Gt); i++ }
        expression[i] == '!' -> { tokens.add(Token.Not); i++ }

        expression[i].isDigit() || (expression[i] == '-' && i + 1 < len && expression[i + 1].isDigit()) -> {
          val start = i
          if (expression[i] == '-') i++
          while (i < len && (expression[i].isDigit() || expression[i] == '.')) i++
          tokens.add(Token.NumberLiteral(expression.substring(start, i).toDouble()))
        }

        expression[i].isLetter() || expression[i] == '_' -> {
          val start = i
          while (i < len && (expression[i].isLetterOrDigit() || expression[i] == '_' || expression[i] == '.')) i++
          val word = expression.substring(start, i)
          tokens.add(
            when (word) {
              "true" -> Token.BooleanLiteral(true)
              "false" -> Token.BooleanLiteral(false)
              "null", "nil" -> Token.NullLiteral
              "contains" -> Token.Contains
              "in" -> Token.In
              "and" -> Token.And
              "or" -> Token.Or
              "not" -> Token.Not
              else -> Token.Identifier(word)
            },
          )
        }

        else -> i++ // skip unknown characters
      }
    }

    return tokens
  }
}
