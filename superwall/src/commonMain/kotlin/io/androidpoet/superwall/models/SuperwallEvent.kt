package io.androidpoet.superwall.models

import kotlinx.datetime.Instant

/**
 * Events emitted by the SDK throughout the paywall lifecycle.
 * Forwarded to [SuperwallDelegate.handleSuperwallEvent] and analytics integrations.
 */
public sealed interface SuperwallEvent {

  /** SDK finished configuring. */
  public data object ConfigReady : SuperwallEvent

  /** A placement was registered. */
  public data class PlacementRegistered(
    val placement: String,
    val params: Map<String, Any?>,
  ) : SuperwallEvent

  /** A paywall is about to be presented. */
  public data class PaywallOpen(val info: PaywallInfo) : SuperwallEvent

  /** A paywall was dismissed. */
  public data class PaywallClose(val info: PaywallInfo) : SuperwallEvent

  /** A purchase was initiated from a paywall. */
  public data class TransactionStart(
    val product: StoreProduct,
    val info: PaywallInfo,
  ) : SuperwallEvent

  /** A purchase completed successfully. */
  public data class TransactionComplete(
    val product: StoreProduct,
    val info: PaywallInfo,
  ) : SuperwallEvent

  /** A purchase failed. */
  public data class TransactionFail(
    val error: Throwable,
    val info: PaywallInfo,
  ) : SuperwallEvent

  /** A subscription started. */
  public data class SubscriptionStart(
    val product: StoreProduct,
    val info: PaywallInfo,
  ) : SuperwallEvent

  /** A free trial started. */
  public data class FreeTrialStart(
    val product: StoreProduct,
    val info: PaywallInfo,
  ) : SuperwallEvent

  /** Purchases were restored. */
  public data class Restore(val info: PaywallInfo) : SuperwallEvent

  /** Restore failed. */
  public data class RestoreFail(
    val error: Throwable,
    val info: PaywallInfo,
  ) : SuperwallEvent

  /** A URL was opened from a paywall. */
  public data class PaywallOpenUrl(
    val url: String,
    val info: PaywallInfo,
  ) : SuperwallEvent

  /** A deep link was opened from a paywall. */
  public data class PaywallOpenDeepLink(
    val url: String,
    val info: PaywallInfo,
  ) : SuperwallEvent

  /** A custom action was triggered from a paywall. */
  public data class CustomAction(
    val name: String,
    val info: PaywallInfo,
  ) : SuperwallEvent

  /** Subscription status changed. */
  public data class SubscriptionStatusDidChange(
    val status: SubscriptionStatus,
  ) : SuperwallEvent
}

/**
 * Wrapper that pairs an event with its timestamp.
 */
public data class SuperwallEventInfo(
  val event: SuperwallEvent,
  val timestamp: Instant,
)
