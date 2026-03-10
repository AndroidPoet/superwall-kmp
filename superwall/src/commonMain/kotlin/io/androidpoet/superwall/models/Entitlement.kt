package io.androidpoet.superwall.models

import kotlinx.serialization.Serializable

/**
 * A feature access tier that a user can unlock through a purchase.
 * Products map to entitlements, enabling multi-tier monetization
 * (e.g., Bronze, Silver, Gold).
 */
@Serializable
public data class Entitlement(
  val id: String,
  val name: String = id,
)
