package io.androidpoet.superwall.compose.renderer

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.androidpoet.superwall.models.StoreProduct
import io.androidpoet.superwall.models.components.ImageFit
import io.androidpoet.superwall.models.components.VariableResolver

/**
 * Rendering state passed down the component tree.
 * Holds the current product selection, variable resolution context,
 * and platform-specific rendering callbacks.
 */
public class PaywallRenderState(
  /** All available products keyed by ID. */
  public val products: Map<String, StoreProduct> = emptyMap(),
  /** Currently selected product ID. */
  public val selectedProductId: String? = null,
  /** Custom variables from server config. */
  public val customVariables: Map<String, String> = emptyMap(),
  /** Locale-specific string overrides. */
  public val localizations: Map<String, Map<String, String>> = emptyMap(),
  /** Current locale. */
  public val locale: String = "en",
  /** Whether the system is in dark mode. */
  public val isDarkMode: Boolean = false,
  /** Callback when a product package is selected. */
  public val onProductSelected: (String) -> Unit = {},
  /**
   * Platform-specific image loader composable.
   * Parameters: url/asset, fit mode, accessibility label.
   */
  public val imageLoader: (@Composable (String, ImageFit, String?) -> Unit)? = null,
  /**
   * Platform-specific icon loader composable.
   * Parameters: icon name, optional URL, tint color, size.
   */
  public val iconLoader: (@Composable (String, String?, Color, Double) -> Unit)? = null,
) {

  /** The currently selected [StoreProduct], if any. */
  public val selectedProduct: StoreProduct?
    get() = selectedProductId?.let { products[it] }

  /** Resolve template variables in text (e.g., `{{ product.price }}`). */
  public fun resolveText(template: String): String =
    VariableResolver.resolve(
      template = template,
      selectedProduct = selectedProduct,
      products = products,
      customVariables = customVariables,
      localizations = localizations,
      locale = locale,
    )

  /** Create a child state scoped to a specific product (for Package components). */
  public fun withProduct(productId: String): PaywallRenderState =
    PaywallRenderState(
      products = products,
      selectedProductId = productId,
      customVariables = customVariables,
      localizations = localizations,
      locale = locale,
      isDarkMode = isDarkMode,
      onProductSelected = onProductSelected,
      imageLoader = imageLoader,
      iconLoader = iconLoader,
    )
}
