package io.androidpoet.superwall.models

/**
 * Interface for handling purchases externally (e.g., via RevenueCat).
 * When provided, the SDK delegates all purchase and restore logic to this controller.
 * The app is then responsible for calling [Superwall.setSubscriptionStatus]
 * on every subscription state change.
 */
public interface PurchaseController {

  /**
   * Called when a purchase is initiated from a paywall.
   * @param product The product the user wants to purchase.
   * @return The result of the purchase attempt.
   */
  public suspend fun purchase(product: StoreProduct): PurchaseResult

  /**
   * Called when the user taps "Restore Purchases" on a paywall.
   * @return The result of the restoration attempt.
   */
  public suspend fun restorePurchases(): RestorationResult
}
