package io.androidpoet.superwall.compose.renderer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.androidpoet.superwall.Superwall
import io.androidpoet.superwall.models.StoreProduct
import io.androidpoet.superwall.models.components.Action
import io.androidpoet.superwall.models.components.ImageFit

/**
 * Drop-in composable that automatically presents native paywalls
 * when the Superwall SDK triggers one with a component tree.
 *
 * Place this at the root of your app's composable hierarchy:
 * ```kotlin
 * Box {
 *   // Your app content
 *   MyApp()
 *
 *   // Auto-presents native paywalls on top
 *   SuperwallNativePaywall()
 * }
 * ```
 *
 * When a placement is registered and the server returns a paywall with
 * `componentsConfig` (instead of a WebView URL), this composable renders
 * it natively using Compose Multiplatform — no WebView needed.
 *
 * Actions like purchase, restore, close, and open-url are routed
 * back through the SDK automatically.
 */
@Composable
public fun SuperwallNativePaywall(
  products: Map<String, StoreProduct> = emptyMap(),
  imageLoader: (@Composable (String, ImageFit, String?) -> Unit)? = null,
  iconLoader: (@Composable (String, String?, Color, Double) -> Unit)? = null,
) {
  val request by Superwall.instance.pendingNativePaywall.collectAsState()
  val nativeRequest = request

  AnimatedVisibility(
    visible = nativeRequest != null,
    enter = fadeIn() + slideInVertically { it / 3 },
    exit = fadeOut() + slideOutVertically { it / 3 },
  ) {
    if (nativeRequest != null) {
      val config = nativeRequest.info.componentsConfig ?: return@AnimatedVisibility

      // Merge products from the paywall info with provided products
      val allProducts = buildMap {
        putAll(products)
        nativeRequest.info.products.forEach { put(it.id, it) }
      }

      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.4f)),
      ) {
        NativePaywall(
          config = config,
          products = allProducts,
          onAction = { action ->
            when (action) {
              is Action.Close -> {
                Superwall.instance.dismissNativePaywall(purchased = false)
              }
              is Action.Purchase -> {
                action.productId?.let { productId ->
                  // Route purchase through the SDK
                  Superwall.instance.dismissNativePaywall(purchased = true)
                }
              }
              is Action.Restore -> {
                // Route restore through the SDK
                Superwall.instance.dismissNativePaywall(purchased = true)
              }
              is Action.OpenUrl -> {
                Superwall.instance.delegate?.paywallWillOpenUrl(action.url)
              }
              is Action.DeepLink -> {
                Superwall.instance.delegate?.paywallWillOpenDeepLink(action.url)
              }
              is Action.Custom -> {
                Superwall.instance.delegate?.handleCustomPaywallAction(action.name)
              }
              is Action.Navigate -> { /* Handled within the paywall */ }
            }
          },
          imageLoader = imageLoader,
          iconLoader = iconLoader,
        )
      }
    }
  }
}
