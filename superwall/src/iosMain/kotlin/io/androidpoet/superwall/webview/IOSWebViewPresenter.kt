package io.androidpoet.superwall.webview

import io.androidpoet.superwall.models.PaywallInfo
import io.androidpoet.superwall.models.PaywallPresentationStyle
import io.androidpoet.superwall.paywall.PaywallCallback
import io.androidpoet.superwall.paywall.PaywallPresenter
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUserContentController
import platform.WebKit.WKUserScript
import platform.WebKit.WKUserScriptInjectionTime
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * iOS implementation of [PaywallPresenter] using WKWebView.
 * Renders paywalls via WebKit with WKScriptMessageHandler for the JS bridge.
 */
public class IOSWebViewPresenter : PaywallPresenter {

  private var _isPresenting = false
  private var presentedViewController: UIViewController? = null
  private val json = Json { ignoreUnknownKeys = true }

  override val isPresenting: Boolean
    get() = _isPresenting

  @OptIn(ExperimentalForeignApi::class)
  override suspend fun present(
    url: String,
    info: PaywallInfo,
    style: PaywallPresentationStyle,
    callback: PaywallCallback,
  ) {
    if (_isPresenting) return
    _isPresenting = true

    val messageHandler = PaywallMessageHandler(callback, json)

    val config = WKWebViewConfiguration().apply {
      userContentController.addScriptMessageHandler(messageHandler, BRIDGE_NAME)
      val userScript = WKUserScript(
        source = BRIDGE_CONNECTOR_SCRIPT,
        injectionTime = WKUserScriptInjectionTime.WKUserScriptInjectionTimeAtDocumentEnd,
        forMainFrameOnly = true,
      )
      userContentController.addUserScript(userScript)
    }

    val webView = WKWebView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), configuration = config)
    webView.setOpaque(false)
    webView.navigationDelegate = PaywallNavigationDelegate(callback)

    val nsUrl = NSURL.URLWithString(url) ?: run {
      _isPresenting = false
      callback.onError(Exception("Invalid paywall URL: $url"))
      return
    }

    webView.loadRequest(NSURLRequest.requestWithURL(nsUrl))

    val viewController = UIViewController()
    viewController.view.addSubview(webView)
    webView.setTranslatesAutoresizingMaskIntoConstraints(false)
    webView.topAnchor.constraintEqualToAnchor(viewController.view.topAnchor).setActive(true)
    webView.bottomAnchor.constraintEqualToAnchor(viewController.view.bottomAnchor).setActive(true)
    webView.leadingAnchor.constraintEqualToAnchor(viewController.view.leadingAnchor).setActive(true)
    webView.trailingAnchor.constraintEqualToAnchor(viewController.view.trailingAnchor).setActive(true)

    // UIModalPresentationStyle is a typealias for Long in K/N
    // 0 = fullScreen, 1 = pageSheet, 2 = formSheet, 5 = overFullScreen
    val modalStyle: Long = when (style) {
      is PaywallPresentationStyle.Fullscreen,
      is PaywallPresentationStyle.FullscreenNoAnimation,
      is PaywallPresentationStyle.Push -> 5L // overFullScreen

      is PaywallPresentationStyle.Modal,
      is PaywallPresentationStyle.Drawer -> 1L // pageSheet

      is PaywallPresentationStyle.Popup -> 2L // formSheet
    }
    viewController.setModalPresentationStyle(modalStyle)

    presentedViewController = viewController

    val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController
    val topVC = findTopViewController(rootVC)
    val animated = style !is PaywallPresentationStyle.FullscreenNoAnimation

    topVC?.presentViewController(viewController, animated = animated) {
      callback.onPresented(info)
    }
  }

  override suspend fun dismiss(animated: Boolean) {
    presentedViewController?.dismissViewControllerAnimated(animated) {
      _isPresenting = false
      presentedViewController = null
    }
  }

  private fun findTopViewController(root: UIViewController?): UIViewController? {
    var top = root
    while (top?.presentedViewController != null) {
      top = top.presentedViewController
    }
    return top
  }

  public companion object {
    public const val BRIDGE_NAME: String = "SWNativeBridge"
  }
}

/**
 * WKScriptMessageHandler that receives events from Paywall.js.
 */
private class PaywallMessageHandler(
  private val callback: PaywallCallback,
  private val json: Json,
) : NSObject(), WKScriptMessageHandlerProtocol {

  override fun userContentController(
    userContentController: WKUserContentController,
    didReceiveScriptMessage: WKScriptMessage,
  ) {
    val body = didReceiveScriptMessage.body as? String ?: return
    val message = try {
      json.decodeFromString<JsonObject>(body)
    } catch (_: Exception) {
      return
    }

    val event = message["event"]?.jsonPrimitive?.content ?: return

    when (event) {
      "close" -> callback.onDismissed(emptyPaywallInfo())
      "purchase" -> {
        val productId = message["productId"]?.jsonPrimitive?.content ?: return
        callback.onPurchaseInitiated(productId)
      }
      "restore" -> callback.onRestoreInitiated()
      "open_url" -> {
        val url = message["url"]?.jsonPrimitive?.content ?: return
        callback.onOpenUrl(url)
      }
      "open_deep_link" -> {
        val url = message["url"]?.jsonPrimitive?.content ?: return
        callback.onOpenDeepLink(url)
      }
      "custom" -> {
        val action = message["action"]?.jsonPrimitive?.content ?: return
        callback.onCustomAction(action)
      }
    }
  }

  private fun emptyPaywallInfo() = io.androidpoet.superwall.models.PaywallInfo(
    id = "",
    identifier = "",
    name = "",
    url = "",
  )
}

/**
 * WKNavigationDelegate that intercepts deep link navigation.
 */
private class PaywallNavigationDelegate(
  private val callback: PaywallCallback,
) : NSObject(), WKNavigationDelegateProtocol {

  override fun webView(
    webView: WKWebView,
    decidePolicyForNavigationAction: WKNavigationAction,
    decisionHandler: (WKNavigationActionPolicy) -> Unit,
  ) {
    val url = decidePolicyForNavigationAction.request.URL?.absoluteString ?: run {
      decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
      return
    }

    if (url.startsWith("http://") || url.startsWith("https://")) {
      decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
    } else {
      callback.onOpenDeepLink(url)
      decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
    }
  }
}

private const val BRIDGE_CONNECTOR_SCRIPT: String = """
(function() {
  window.addEventListener('message', function(event) {
    try {
      var data = typeof event.data === 'string' ? JSON.parse(event.data) : event.data;
      if (data && data.event) {
        window.webkit.messageHandlers.SWNativeBridge.postMessage(JSON.stringify(data));
      }
    } catch(e) {}
  });

  document.addEventListener('click', function(event) {
    var target = event.target.closest('[data-pw-close]');
    if (target) {
      window.webkit.messageHandlers.SWNativeBridge.postMessage(JSON.stringify({event: 'close'}));
      return;
    }
    target = event.target.closest('[data-pw-purchase]');
    if (target) {
      var productType = target.getAttribute('data-pw-purchase');
      window.webkit.messageHandlers.SWNativeBridge.postMessage(JSON.stringify({event: 'purchase', productId: productType}));
      return;
    }
    target = event.target.closest('[data-pw-restore]');
    if (target) {
      window.webkit.messageHandlers.SWNativeBridge.postMessage(JSON.stringify({event: 'restore'}));
      return;
    }
    target = event.target.closest('[data-pw-custom]');
    if (target) {
      window.webkit.messageHandlers.SWNativeBridge.postMessage(JSON.stringify({event: 'custom', action: target.getAttribute('data-pw-custom')}));
      return;
    }
    target = event.target.closest('[data-pw-open-url]');
    if (target) {
      window.webkit.messageHandlers.SWNativeBridge.postMessage(JSON.stringify({event: 'open_url', url: target.getAttribute('data-pw-open-url') || target.getAttribute('href')}));
      event.preventDefault();
      return;
    }
    target = event.target.closest('[data-pw-open-deep-link]');
    if (target) {
      window.webkit.messageHandlers.SWNativeBridge.postMessage(JSON.stringify({event: 'open_deep_link', url: target.getAttribute('data-pw-open-deep-link') || target.getAttribute('href')}));
      event.preventDefault();
      return;
    }
  });
})();
"""
