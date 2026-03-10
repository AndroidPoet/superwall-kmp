package io.androidpoet.superwall.models

/**
 * Result of a purchase attempt, returned by [PurchaseController.purchase].
 */
public sealed interface PurchaseResult {

  /** Purchase completed successfully. */
  public data object Purchased : PurchaseResult

  /** Purchase is pending (e.g., parental approval, deferred payment). */
  public data object Pending : PurchaseResult

  /** User cancelled the purchase. */
  public data object Cancelled : PurchaseResult

  /** Purchase failed with an error. */
  public data class Failed(val error: Throwable) : PurchaseResult
}

/**
 * Result of a restore attempt, returned by [PurchaseController.restorePurchases].
 */
public sealed interface RestorationResult {

  /** Purchases were successfully restored. */
  public data object Restored : RestorationResult

  /** Restoration failed. */
  public data class Failed(val error: Throwable) : RestorationResult
}
