package io.androidpoet.superwall.store

import io.androidpoet.superwall.models.PurchaseResult
import io.androidpoet.superwall.models.RestorationResult
import io.androidpoet.superwall.models.StoreProduct

/**
 * Platform-agnostic interface for interacting with the app store.
 * Android: wraps Google Play Billing. iOS: wraps StoreKit 2.
 * Bound via Koin in platform-specific modules.
 */
public interface StoreManager {

  /** Fetch products from the store by their identifiers. */
  public suspend fun fetchProducts(ids: Set<String>): List<StoreProduct>

  /** Initiate a purchase for the given product. */
  public suspend fun purchase(product: StoreProduct): PurchaseResult

  /** Restore previously purchased products. */
  public suspend fun restorePurchases(): RestorationResult
}
