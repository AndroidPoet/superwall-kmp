package io.androidpoet.superwall.models

import kotlinx.serialization.Serializable

/**
 * Represents the user's current subscription state.
 * Apps must update this via [Superwall.setSubscriptionStatus] whenever
 * the subscription state changes.
 */
@Serializable
public sealed interface SubscriptionStatus {

  /** Subscription state has not yet been determined. */
  @Serializable
  public data object Unknown : SubscriptionStatus

  /** User has an active subscription with the given entitlements. */
  @Serializable
  public data class Active(
    val entitlements: Set<Entitlement> = emptySet(),
  ) : SubscriptionStatus

  /** User does not have an active subscription. */
  @Serializable
  public data object Inactive : SubscriptionStatus
}
