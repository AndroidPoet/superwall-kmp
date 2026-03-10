package io.androidpoet.superwall.webview

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import io.androidpoet.superwall.paywall.PaywallCallback
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Custom WebView for rendering Superwall paywalls.
 * Injects a JavaScript bridge for native ↔ web communication,
 * handling data-pw-* attributes from Paywall.js.
 */
@SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
internal class SWWebView(
  context: Context,
  private val callback: PaywallCallback,
  private val json: Json = Json { ignoreUnknownKeys = true },
) : WebView(context) {

  init {
    setupWebView()
    addJavascriptInterface(SWWebViewBridge(callback, json), BRIDGE_NAME)
  }

  private fun setupWebView() {
    settings.apply {
      javaScriptEnabled = true
      domStorageEnabled = true
      allowFileAccess = false
      allowContentAccess = false
      mediaPlaybackRequiresUserGesture = false
      mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
      cacheMode = WebSettings.LOAD_DEFAULT
      setSupportZoom(false)
      builtInZoomControls = false
      displayZoomControls = false
      useWideViewPort = true
      loadWithOverviewMode = true
    }

    webViewClient = SWWebViewClient(callback)
    webChromeClient = WebChromeClient()

    // Transparent background for seamless integration
    setBackgroundColor(0x00000000)
  }

  /**
   * Load a paywall URL and inject the native bridge script.
   */
  fun loadPaywall(url: String) {
    loadUrl(url)
  }

  /**
   * Pass template variables and product data to the paywall via JavaScript.
   */
  fun passDataToPaywall(productsJson: String, variablesJson: String) {
    val script = """
      (function() {
        if (window.paywall) {
          window.paywall.accept($productsJson, $variablesJson);
        }
      })();
    """.trimIndent()
    evaluateJavascript(script, null)
  }

  override fun destroy() {
    removeJavascriptInterface(BRIDGE_NAME)
    super.destroy()
  }

  companion object {
    const val BRIDGE_NAME = "SWNativeBridge"
  }
}

/**
 * JavaScript interface exposed to the paywall HTML.
 * Handles events from data-pw-* attributes via Paywall.js.
 */
private class SWWebViewBridge(
  private val callback: PaywallCallback,
  private val json: Json,
) {

  @JavascriptInterface
  fun postMessage(messageJson: String) {
    val message = try {
      json.decodeFromString<PaywallMessage>(messageJson)
    } catch (_: Exception) {
      return
    }

    when (message.event) {
      "close" -> callback.onDismissed(emptyPaywallInfo())
      "purchase" -> {
        val productId = message.productId ?: return
        callback.onPurchaseInitiated(productId)
      }
      "restore" -> callback.onRestoreInitiated()
      "open_url" -> {
        val url = message.url ?: return
        callback.onOpenUrl(url)
      }
      "open_deep_link" -> {
        val url = message.url ?: return
        callback.onOpenDeepLink(url)
      }
      "custom" -> {
        val name = message.action ?: return
        callback.onCustomAction(name)
      }
    }
  }

  // Minimal PaywallInfo for bridge events — full info is held by the presenter
  private fun emptyPaywallInfo() = io.androidpoet.superwall.models.PaywallInfo(
    id = "",
    identifier = "",
    name = "",
    url = "",
  )
}

@Serializable
private data class PaywallMessage(
  val event: String,
  val productId: String? = null,
  val url: String? = null,
  val action: String? = null,
  val data: Map<String, String>? = null,
)

/**
 * WebViewClient that intercepts navigation and notifies on page load.
 */
private class SWWebViewClient(
  private val callback: PaywallCallback,
) : WebViewClient() {

  override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
    val url = request?.url?.toString() ?: return false

    // Intercept deep links and external URLs
    return when {
      url.startsWith("http://") || url.startsWith("https://") -> false // Allow normal loading
      else -> {
        callback.onOpenDeepLink(url)
        true
      }
    }
  }

  override fun onPageFinished(view: WebView?, url: String?) {
    super.onPageFinished(view, url)
    // Inject the native bridge connector script
    view?.evaluateJavascript(BRIDGE_CONNECTOR_SCRIPT, null)
  }
}

/**
 * JavaScript injected after page load to connect Paywall.js events
 * to the native bridge.
 */
private const val BRIDGE_CONNECTOR_SCRIPT = """
(function() {
  // Listen for Paywall.js postMessage events
  window.addEventListener('message', function(event) {
    try {
      var data = typeof event.data === 'string' ? JSON.parse(event.data) : event.data;
      if (data && data.event && window.SWNativeBridge) {
        window.SWNativeBridge.postMessage(JSON.stringify(data));
      }
    } catch(e) {}
  });

  // Override click handlers for data-pw-* elements
  document.addEventListener('click', function(event) {
    var target = event.target.closest('[data-pw-close]');
    if (target) {
      window.SWNativeBridge.postMessage(JSON.stringify({event: 'close'}));
      return;
    }

    target = event.target.closest('[data-pw-purchase]');
    if (target) {
      var productType = target.getAttribute('data-pw-purchase');
      window.SWNativeBridge.postMessage(JSON.stringify({
        event: 'purchase',
        productId: productType
      }));
      return;
    }

    target = event.target.closest('[data-pw-restore]');
    if (target) {
      window.SWNativeBridge.postMessage(JSON.stringify({event: 'restore'}));
      return;
    }

    target = event.target.closest('[data-pw-custom]');
    if (target) {
      var action = target.getAttribute('data-pw-custom');
      window.SWNativeBridge.postMessage(JSON.stringify({
        event: 'custom',
        action: action
      }));
      return;
    }

    target = event.target.closest('[data-pw-open-url]');
    if (target) {
      var url = target.getAttribute('data-pw-open-url') || target.getAttribute('href');
      window.SWNativeBridge.postMessage(JSON.stringify({
        event: 'open_url',
        url: url
      }));
      event.preventDefault();
      return;
    }

    target = event.target.closest('[data-pw-open-deep-link]');
    if (target) {
      var deepLink = target.getAttribute('data-pw-open-deep-link') || target.getAttribute('href');
      window.SWNativeBridge.postMessage(JSON.stringify({
        event: 'open_deep_link',
        url: deepLink
      }));
      event.preventDefault();
      return;
    }
  });
})();
"""
