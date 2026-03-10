package io.androidpoet.superwall.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.androidpoet.superwall.Superwall
import io.androidpoet.superwall.models.PaywallInfo
import io.androidpoet.superwall.models.SubscriptionStatus
import io.androidpoet.superwall.placement.PlacementResult

/**
 * State holder for paywall presentation in Compose.
 */
public class PaywallState {
  internal var isVisible by mutableStateOf(false)
  internal var placementResult by mutableStateOf<PlacementResult?>(null)
  internal var paywallInfo by mutableStateOf<PaywallInfo?>(null)

  /** Whether the paywall is currently showing. */
  public val isPresenting: Boolean get() = isVisible

  /** Show the paywall for a placement. */
  public fun present(placement: String, params: Map<String, Any?> = emptyMap()) {
    val result = Superwall.instance.getPresentationResult(placement, params)
    placementResult = result
    isVisible = result is PlacementResult.ShowPaywall
  }

  /** Dismiss the paywall. */
  public fun dismiss() {
    isVisible = false
    placementResult = null
    paywallInfo = null
  }
}

/**
 * Remember a [PaywallState] for managing paywall presentation.
 */
@Composable
public fun rememberPaywallState(): PaywallState {
  return remember { PaywallState() }
}

/**
 * Composable that observes a placement and presents a paywall when triggered.
 * Wraps the platform WebView presenter in a Compose-friendly API.
 *
 * @param placement The placement name configured in the Superwall dashboard.
 * @param params Optional parameters for rule evaluation.
 * @param onFeatureUnlocked Called when the feature is unlocked (no paywall or purchase complete).
 * @param onDismiss Called when the paywall is dismissed.
 * @param loading Composable shown while the paywall loads.
 */
@Composable
public fun SuperwallPaywall(
  placement: String,
  params: Map<String, Any?> = emptyMap(),
  onFeatureUnlocked: () -> Unit = {},
  onDismiss: () -> Unit = {},
  loading: @Composable () -> Unit = { DefaultLoadingIndicator() },
) {
  val subscriptionStatus by Superwall.instance.subscriptionStatus.collectAsState()

  // Auto-unlock for subscribers
  if (subscriptionStatus is SubscriptionStatus.Active) {
    DisposableEffect(Unit) {
      onFeatureUnlocked()
      onDispose {}
    }
    return
  }

  val result = remember(placement, params) {
    Superwall.instance.getPresentationResult(placement, params)
  }

  when (result) {
    is PlacementResult.ShowPaywall -> {
      // The actual paywall is rendered via platform WebView, not Compose.
      // This composable orchestrates the presentation lifecycle.
      var isLoading by remember { mutableStateOf(true) }

      AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it },
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
          contentAlignment = Alignment.Center,
        ) {
          if (isLoading) {
            loading()
          }
          // Platform WebView is presented via PaywallPresenter.
          // In a full implementation, this would host an AndroidView/UIKitView
          // wrapping the platform WebView.
        }
      }
    }
    else -> {
      DisposableEffect(Unit) {
        onFeatureUnlocked()
        onDispose {}
      }
    }
  }
}

/**
 * Convenience composable that triggers a placement registration
 * with Compose lifecycle awareness.
 *
 * @param placement The placement name.
 * @param params Optional parameters.
 * @param content The feature content, shown when unlocked.
 */
@Composable
public fun SuperwallGate(
  placement: String,
  params: Map<String, Any?> = emptyMap(),
  content: @Composable () -> Unit,
) {
  val subscriptionStatus by Superwall.instance.subscriptionStatus.collectAsState()
  var isUnlocked by remember { mutableStateOf(false) }

  when (subscriptionStatus) {
    is SubscriptionStatus.Active -> {
      content()
    }
    is SubscriptionStatus.Inactive -> {
      if (isUnlocked) {
        content()
      } else {
        DisposableEffect(placement) {
          Superwall.instance.register(
            placement = placement,
            params = params,
            onFeatureUnlocked = { isUnlocked = true },
          )
          onDispose {}
        }
      }
    }
    is SubscriptionStatus.Unknown -> {
      // Show loading while subscription status resolves
      DefaultLoadingIndicator()
    }
  }
}

@Composable
private fun DefaultLoadingIndicator() {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    CircularProgressIndicator()
  }
}
