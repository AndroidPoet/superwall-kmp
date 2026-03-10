package io.androidpoet.superwall.models

import kotlinx.serialization.Serializable

/**
 * Platform-agnostic representation of an in-app purchase product.
 * Wraps StoreKit's Product (iOS) and BillingClient's ProductDetails (Android).
 */
@Serializable
public data class StoreProduct(
  val id: String,
  val name: String,
  val description: String,
  val price: Double,
  val currencyCode: String,
  val localizedPrice: String,
  val periodUnit: PeriodUnit? = null,
  val periodValue: Int? = null,
  val trialPeriodDays: Int? = null,
  val entitlements: Set<Entitlement> = emptySet(),
)

@Serializable
public enum class PeriodUnit {
  DAY,
  WEEK,
  MONTH,
  YEAR,
}
