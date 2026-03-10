package io.androidpoet.superwall.paywall

import io.androidpoet.superwall.models.PaywallInfo
import io.androidpoet.superwall.models.PaywallPresentationStyle

/**
 * Platform-agnostic interface for presenting paywalls.
 * Android: presents via Activity/WebView. iOS: presents via UIViewController/WKWebView.
 * Bound via Koin in platform-specific modules.
 */
public interface PaywallPresenter {

  /** Present a paywall with the given URL and style. */
  public suspend fun present(
    url: String,
    info: PaywallInfo,
    style: PaywallPresentationStyle,
    callback: PaywallCallback,
  )

  /** Dismiss the currently presented paywall. */
  public suspend fun dismiss(animated: Boolean = true)

  /** Whether a paywall is currently being presented. */
  public val isPresenting: Boolean
}

/**
 * Callbacks for paywall lifecycle events.
 */
public interface PaywallCallback {
  public fun onPresented(info: PaywallInfo) {}
  public fun onDismissed(info: PaywallInfo) {}
  public fun onError(error: Throwable) {}
  public fun onPurchaseInitiated(productId: String) {}
  public fun onRestoreInitiated() {}
  public fun onCustomAction(name: String) {}
  public fun onOpenUrl(url: String) {}
  public fun onOpenDeepLink(url: String) {}
}
