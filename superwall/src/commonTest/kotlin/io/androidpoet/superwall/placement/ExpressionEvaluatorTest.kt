package io.androidpoet.superwall.placement

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ExpressionEvaluatorTest {

  @Test
  internal fun test_evaluate_userAndParamsComparison_returnsTrueWhenAllConditionsMatch() {
    val evaluator = ExpressionEvaluator(
      userAttributes = mapOf("country" to "US"),
      params = mapOf("count" to 3),
    )

    val result = evaluator.evaluate("user.country == \"US\" && params.count >= 3")

    assertTrue(result)
  }

  @Test
  internal fun test_evaluate_containsAndNotOperator_returnsExpectedBoolean() {
    val evaluator = ExpressionEvaluator(
      userAttributes = mapOf("plan" to "pro"),
      params = mapOf("screen" to "onboarding_home"),
    )

    assertTrue(evaluator.evaluate("params.screen contains \"onboarding\""))
    assertFalse(evaluator.evaluate("!(user.plan == \"pro\")"))
  }

  @Test
  internal fun test_evaluate_invalidExpression_returnsFalse() {
    val evaluator = ExpressionEvaluator(
      userAttributes = emptyMap(),
      params = emptyMap(),
    )

    val result = evaluator.evaluate("&&&&")

    assertFalse(result)
  }
}
