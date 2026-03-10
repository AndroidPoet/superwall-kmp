package io.androidpoet.superwall.webview

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import io.androidpoet.superwall.models.PaywallInfo
import io.androidpoet.superwall.models.PaywallPresentationStyle
import io.androidpoet.superwall.paywall.PaywallCallback
import kotlinx.serialization.json.Json

/**
 * Activity that hosts the paywall WebView.
 * Supports multiple presentation styles: fullscreen, modal, drawer, popup.
 */
internal class SuperwallPaywallActivity : AppCompatActivity() {

  private var webView: SWWebView? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Edge-to-edge rendering
    WindowCompat.setDecorFitsSystemWindows(window, false)

    val url = intent.getStringExtra(EXTRA_URL) ?: run {
      finish()
      return
    }
    val styleOrdinal = intent.getIntExtra(EXTRA_STYLE, STYLE_FULLSCREEN)

    applyPresentationStyle(styleOrdinal)

    val container = FrameLayout(this).apply {
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
      )
    }

    val callback = activeCallback ?: object : PaywallCallback {}

    webView = SWWebView(this, callback).apply {
      layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT,
      )
    }

    container.addView(webView)
    setContentView(container)

    webView?.loadPaywall(url)
  }

  private fun applyPresentationStyle(styleOrdinal: Int) {
    when (styleOrdinal) {
      STYLE_FULLSCREEN -> {
        // Already edge-to-edge
      }
      STYLE_FULLSCREEN_NO_ANIMATION -> {
        overridePendingTransition(0, 0)
      }
      STYLE_MODAL -> {
        // Apply modal theme if desired
        window.setDimAmount(0.5f)
      }
    }
  }

  override fun onDestroy() {
    webView?.destroy()
    webView = null
    activeCallback = null
    super.onDestroy()
  }

  override fun finish() {
    super.finish()
    if (intent.getIntExtra(EXTRA_STYLE, STYLE_FULLSCREEN) == STYLE_FULLSCREEN_NO_ANIMATION) {
      overridePendingTransition(0, 0)
    }
  }

  companion object {
    const val EXTRA_URL = "superwall_url"
    const val EXTRA_STYLE = "superwall_style"
    const val STYLE_FULLSCREEN = 0
    const val STYLE_FULLSCREEN_NO_ANIMATION = 1
    const val STYLE_MODAL = 2
    const val STYLE_PUSH = 3
    const val STYLE_DRAWER = 4
    const val STYLE_POPUP = 5

    // Static callback reference — cleared in onDestroy.
    // This avoids serialization of complex callback objects.
    @Volatile
    var activeCallback: PaywallCallback? = null

    fun createIntent(
      context: Context,
      url: String,
      style: PaywallPresentationStyle,
    ): Intent {
      return Intent(context, SuperwallPaywallActivity::class.java).apply {
        putExtra(EXTRA_URL, url)
        putExtra(EXTRA_STYLE, style.toStyleOrdinal())
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    }

    private fun PaywallPresentationStyle.toStyleOrdinal(): Int = when (this) {
      is PaywallPresentationStyle.Fullscreen -> STYLE_FULLSCREEN
      is PaywallPresentationStyle.FullscreenNoAnimation -> STYLE_FULLSCREEN_NO_ANIMATION
      is PaywallPresentationStyle.Modal -> STYLE_MODAL
      is PaywallPresentationStyle.Push -> STYLE_PUSH
      is PaywallPresentationStyle.Drawer -> STYLE_DRAWER
      is PaywallPresentationStyle.Popup -> STYLE_POPUP
    }
  }
}
