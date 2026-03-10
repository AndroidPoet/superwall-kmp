package io.androidpoet.superwall.models

/**
 * Delegate for receiving SDK lifecycle events and paywall callbacks.
 * Set via [Superwall.delegate].
 */
public interface SuperwallDelegate {

  /** Subscription status changed. */
  public fun subscriptionStatusDidChange(status: SubscriptionStatus) {}

  /** An SDK event occurred. */
  public fun handleSuperwallEvent(eventInfo: SuperwallEventInfo) {}

  /** A custom paywall action was triggered (via data-pw-custom in paywall HTML). */
  public fun handleCustomPaywallAction(name: String) {}

  /** A URL was opened from a paywall. Return true to handle it yourself. */
  public fun paywallWillOpenUrl(url: String): Boolean = false

  /** A deep link was opened from a paywall. Return true to handle it yourself. */
  public fun paywallWillOpenDeepLink(url: String): Boolean = false
}
