package io.androidpoet.superwall.webview

import android.content.Context
import io.androidpoet.superwall.models.PaywallInfo
import io.androidpoet.superwall.models.PaywallPresentationStyle
import io.androidpoet.superwall.paywall.PaywallCallback
import io.androidpoet.superwall.paywall.PaywallPresenter

/**
 * Android implementation of [PaywallPresenter].
 * Launches [SuperwallPaywallActivity] with an [SWWebView] to render the paywall.
 */
public class AndroidWebViewPresenter(
  private val context: Context,
) : PaywallPresenter {

  private var _isPresenting = false
  private var currentInfo: PaywallInfo? = null

  override val isPresenting: Boolean
    get() = _isPresenting

  override suspend fun present(
    url: String,
    info: PaywallInfo,
    style: PaywallPresentationStyle,
    callback: PaywallCallback,
  ) {
    if (_isPresenting) return

    _isPresenting = true
    currentInfo = info

    // Wire up a delegating callback that tracks dismiss state
    val wrappedCallback = object : PaywallCallback {
      override fun onPresented(info: PaywallInfo) {
        callback.onPresented(info)
      }

      override fun onDismissed(info: PaywallInfo) {
        _isPresenting = false
        currentInfo = null
        callback.onDismissed(info)
      }

      override fun onError(error: Throwable) {
        _isPresenting = false
        currentInfo = null
        callback.onError(error)
      }

      override fun onPurchaseInitiated(productId: String) {
        callback.onPurchaseInitiated(productId)
      }

      override fun onRestoreInitiated() {
        callback.onRestoreInitiated()
      }

      override fun onCustomAction(name: String) {
        callback.onCustomAction(name)
      }

      override fun onOpenUrl(url: String) {
        callback.onOpenUrl(url)
      }

      override fun onOpenDeepLink(url: String) {
        callback.onOpenDeepLink(url)
      }
    }

    SuperwallPaywallActivity.activeCallback = wrappedCallback

    val intent = SuperwallPaywallActivity.createIntent(context, url, style)
    context.startActivity(intent)

    callback.onPresented(info)
  }

  override suspend fun dismiss(animated: Boolean) {
    // Activity dismissal is handled by the activity itself responding to close events
    // or by finishing from the callback. Here we just update state.
    _isPresenting = false
    currentInfo = null
  }
}
