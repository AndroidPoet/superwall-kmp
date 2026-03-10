package io.androidpoet.superwall.models.components

import io.androidpoet.superwall.models.PeriodUnit
import io.androidpoet.superwall.models.StoreProduct

/**
 * Resolves `{{ variable }}` placeholders in component text.
 *
 * Supports product variables:
 * - `{{ product.price }}` — localized price string
 * - `{{ product.name }}` — product display name
 * - `{{ product.period }}` — subscription period (e.g., "month")
 * - `{{ product.price_per_month }}` — normalized monthly price
 * - `{{ product.trial_days }}` — free trial length in days
 * - `{{ product.currency }}` — currency code
 *
 * And custom variables from the server config.
 */
public object VariableResolver {

  private val VARIABLE_PATTERN = Regex("""\{\{\s*([a-zA-Z0-9_.]+)\s*}}""")

  /**
   * Resolve all `{{ var }}` placeholders in [template].
   *
   * @param template Text with placeholders.
   * @param selectedProduct The currently selected product (if any).
   * @param products All available products keyed by ID.
   * @param customVariables Additional server-provided variables.
   * @param localizations Locale-specific string overrides.
   * @param locale Current locale.
   */
  public fun resolve(
    template: String,
    selectedProduct: StoreProduct? = null,
    products: Map<String, StoreProduct> = emptyMap(),
    customVariables: Map<String, String> = emptyMap(),
    localizations: Map<String, Map<String, String>> = emptyMap(),
    locale: String = "en",
  ): String {
    // First check if the template is a localization key
    val localized = localizations[locale]?.get(template) ?: template

    return VARIABLE_PATTERN.replace(localized) { match ->
      val key = match.groupValues[1]
      resolveKey(key, selectedProduct, products, customVariables)
    }
  }

  private fun resolveKey(
    key: String,
    selectedProduct: StoreProduct?,
    products: Map<String, StoreProduct>,
    customVariables: Map<String, String>,
  ): String {
    // Product variable: product.price, product.name, etc.
    if (key.startsWith("product.")) {
      val field = key.removePrefix("product.")
      val product = selectedProduct ?: return "{{ $key }}"
      return resolveProductField(product, field)
    }

    // Named product: products.monthly.price
    if (key.startsWith("products.")) {
      val parts = key.removePrefix("products.").split(".", limit = 2)
      if (parts.size == 2) {
        val product = products[parts[0]] ?: return "{{ $key }}"
        return resolveProductField(product, parts[1])
      }
    }

    // Custom variable
    customVariables[key]?.let { return it }

    // Unreresolved — return placeholder as-is
    return "{{ $key }}"
  }

  private fun resolveProductField(product: StoreProduct, field: String): String =
    when (field) {
      "price" -> product.localizedPrice
      "name" -> product.name
      "description" -> product.description
      "currency" -> product.currencyCode
      "id" -> product.id
      "period" -> product.periodUnit?.toDisplayString() ?: ""
      "period_value" -> product.periodValue?.toString() ?: ""
      "trial_days" -> product.trialPeriodDays?.toString() ?: ""
      "price_per_month" -> calculateMonthlyPrice(product)
      else -> "{{ product.$field }}"
    }

  private fun calculateMonthlyPrice(product: StoreProduct): String {
    val monthly = when (product.periodUnit) {
      PeriodUnit.DAY -> product.price * 30.0 / (product.periodValue ?: 1)
      PeriodUnit.WEEK -> product.price * (30.0 / 7.0) / (product.periodValue ?: 1)
      PeriodUnit.MONTH -> product.price / (product.periodValue ?: 1)
      PeriodUnit.YEAR -> product.price / (12.0 * (product.periodValue ?: 1))
      null -> product.price
    }
    val rounded = (monthly * 100).toLong() / 100.0
    val whole = rounded.toLong()
    val frac = ((rounded - whole) * 100).toLong()
    return "${product.currencyCode} $whole.${frac.toString().padStart(2, '0')}"
  }

  private fun PeriodUnit.toDisplayString(): String =
    when (this) {
      PeriodUnit.DAY -> "day"
      PeriodUnit.WEEK -> "week"
      PeriodUnit.MONTH -> "month"
      PeriodUnit.YEAR -> "year"
    }
}
