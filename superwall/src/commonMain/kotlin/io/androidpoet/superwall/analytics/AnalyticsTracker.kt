package io.androidpoet.superwall.analytics

import io.androidpoet.superwall.identity.IdentityManager
import io.androidpoet.superwall.models.SuperwallEvent
import io.androidpoet.superwall.models.SuperwallEventInfo
import io.androidpoet.superwall.network.EventPayload
import io.androidpoet.superwall.network.SuperwallApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

/**
 * Tracks and batches analytics events for submission to the Superwall backend.
 * Also exposes a flow for in-app event observation.
 */
public class AnalyticsTracker(
  private val api: SuperwallApi,
  private val identityManager: IdentityManager,
  private val scope: CoroutineScope,
) {

  private val _events = MutableSharedFlow<SuperwallEventInfo>(extraBufferCapacity = 64)
  public val events: SharedFlow<SuperwallEventInfo> = _events.asSharedFlow()

  private val pendingEvents = mutableListOf<EventPayload>()
  private val mutex = Mutex()

  /**
   * Track an event. Emits to the flow and queues for batch submission.
   */
  public fun track(event: SuperwallEvent) {
    val info = SuperwallEventInfo(
      event = event,
      timestamp = Clock.System.now(),
    )
    _events.tryEmit(info)

    val payload = EventPayload(
      name = event.toEventName(),
      params = event.toParams() + mapOf("user_id" to identityManager.effectiveId),
      createdAt = info.timestamp.toString(),
    )

    scope.launch {
      mutex.withLock {
        pendingEvents.add(payload)
        if (pendingEvents.size >= BATCH_SIZE) {
          flushLocked()
        }
      }
    }
  }

  /**
   * Flush all pending events to the backend.
   */
  public fun flush() {
    scope.launch {
      mutex.withLock {
        flushLocked()
      }
    }
  }

  private suspend fun flushLocked() {
    if (pendingEvents.isEmpty()) return
    val batch = pendingEvents.toList()
    pendingEvents.clear()

    try {
      api.postEvents(batch)
    } catch (_: Exception) {
      // Re-queue on failure
      pendingEvents.addAll(0, batch)
    }
  }

  private companion object {
    const val BATCH_SIZE = 20
  }
}

private fun SuperwallEvent.toEventName(): String = when (this) {
  is SuperwallEvent.ConfigReady -> "config_ready"
  is SuperwallEvent.PlacementRegistered -> "placement_registered"
  is SuperwallEvent.PaywallOpen -> "paywall_open"
  is SuperwallEvent.PaywallClose -> "paywall_close"
  is SuperwallEvent.TransactionStart -> "transaction_start"
  is SuperwallEvent.TransactionComplete -> "transaction_complete"
  is SuperwallEvent.TransactionFail -> "transaction_fail"
  is SuperwallEvent.SubscriptionStart -> "subscription_start"
  is SuperwallEvent.FreeTrialStart -> "free_trial_start"
  is SuperwallEvent.Restore -> "restore"
  is SuperwallEvent.RestoreFail -> "restore_fail"
  is SuperwallEvent.PaywallOpenUrl -> "paywall_open_url"
  is SuperwallEvent.PaywallOpenDeepLink -> "paywall_open_deep_link"
  is SuperwallEvent.CustomAction -> "custom_action"
  is SuperwallEvent.SubscriptionStatusDidChange -> "subscription_status_did_change"
}

private fun SuperwallEvent.toParams(): Map<String, String> = when (this) {
  is SuperwallEvent.PlacementRegistered -> mapOf("placement" to placement)
  is SuperwallEvent.PaywallOpen -> mapOf("paywall_id" to info.id)
  is SuperwallEvent.PaywallClose -> mapOf("paywall_id" to info.id)
  is SuperwallEvent.TransactionStart -> mapOf("product_id" to product.id, "paywall_id" to info.id)
  is SuperwallEvent.TransactionComplete -> mapOf("product_id" to product.id, "paywall_id" to info.id)
  is SuperwallEvent.TransactionFail -> mapOf("paywall_id" to info.id, "error" to error.message.orEmpty())
  is SuperwallEvent.SubscriptionStart -> mapOf("product_id" to product.id)
  is SuperwallEvent.FreeTrialStart -> mapOf("product_id" to product.id)
  is SuperwallEvent.Restore -> mapOf("paywall_id" to info.id)
  is SuperwallEvent.RestoreFail -> mapOf("paywall_id" to info.id)
  is SuperwallEvent.PaywallOpenUrl -> mapOf("url" to url)
  is SuperwallEvent.PaywallOpenDeepLink -> mapOf("url" to url)
  is SuperwallEvent.CustomAction -> mapOf("name" to name)
  is SuperwallEvent.SubscriptionStatusDidChange -> mapOf("status" to status.toString())
  else -> emptyMap()
}
