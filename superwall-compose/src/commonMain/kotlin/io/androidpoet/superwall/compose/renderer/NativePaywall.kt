package io.androidpoet.superwall.compose.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.androidpoet.superwall.models.StoreProduct
import io.androidpoet.superwall.models.components.Action
import io.androidpoet.superwall.models.components.ImageFit
import io.androidpoet.superwall.models.components.PaywallComponentsConfig

/**
 * Top-level composable that renders a server-driven native paywall.
 *
 * Wraps the component tree in a scrollable container and provides
 * the rendering state for variable resolution and product selection.
 *
 * @param config The component tree and metadata from the server.
 * @param products Available products keyed by product ID.
 * @param defaultProductId The initially selected product.
 * @param onAction Callback for user actions (purchase, close, restore, etc.).
 * @param imageLoader Optional platform-specific image loading composable.
 * @param iconLoader Optional platform-specific icon loading composable.
 */
@Composable
public fun NativePaywall(
  config: PaywallComponentsConfig,
  products: Map<String, StoreProduct> = emptyMap(),
  defaultProductId: String? = null,
  onAction: (Action) -> Unit = {},
  imageLoader: (@Composable (String, ImageFit, String?) -> Unit)? = null,
  iconLoader: (@Composable (String, String?, Color, Double) -> Unit)? = null,
) {
  var selectedProductId by remember {
    mutableStateOf(defaultProductId ?: products.keys.firstOrNull())
  }

  val isDark = isSystemInDarkTheme()

  val state = remember(selectedProductId, products, isDark) {
    PaywallRenderState(
      products = products,
      selectedProductId = selectedProductId,
      customVariables = config.variables,
      localizations = config.localizations,
      locale = config.defaultLocale,
      isDarkMode = isDark,
      onProductSelected = { selectedProductId = it },
      imageLoader = imageLoader,
      iconLoader = iconLoader,
    )
  }

  if (config.components.isEmpty()) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center,
    ) {
      CircularProgressIndicator()
    }
  } else {
    // Render all root components — the component tree itself
    // manages its own layout (Stack with Fill size, scroll, etc.)
    config.components.forEach { component ->
      RenderComponent(
        component = component,
        state = state,
        onAction = onAction,
      )
    }
  }
}
