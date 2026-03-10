package io.androidpoet.superwall.storekit

import io.androidpoet.superwall.models.PeriodUnit
import io.androidpoet.superwall.models.PurchaseResult
import io.androidpoet.superwall.models.RestorationResult
import io.androidpoet.superwall.models.StoreProduct
import io.androidpoet.superwall.store.StoreManager
import platform.StoreKit.SKPaymentQueue
import platform.StoreKit.SKProduct
import platform.StoreKit.SKProductsRequest
import platform.StoreKit.SKProductsRequestDelegateProtocol
import platform.StoreKit.SKProductsResponse
import platform.StoreKit.SKRequest
import platform.StoreKit.SKPayment
import platform.StoreKit.SKPaymentTransaction
import platform.StoreKit.SKPaymentTransactionObserverProtocol
import platform.StoreKit.SKPaymentTransactionState
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterCurrencyStyle
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * iOS implementation of [StoreManager] using StoreKit 1.
 * Uses SKProductsRequest for product fetching and SKPaymentQueue for purchases.
 */
public class StoreKit2Manager : StoreManager {

  private val productCache = mutableMapOf<String, SKProduct>()
  private val paymentQueue = SKPaymentQueue.defaultQueue()

  override suspend fun fetchProducts(ids: Set<String>): List<StoreProduct> {
    if (ids.isEmpty()) return emptyList()

    val skProducts = requestProducts(ids)
    skProducts.forEach { productCache[it.productIdentifier] = it }
    return skProducts.map { it.toStoreProduct() }
  }

  override suspend fun purchase(product: StoreProduct): PurchaseResult {
    val skProduct = productCache[product.id]
      ?: return PurchaseResult.Failed(Exception("SKProduct not found for ${product.id}. Call fetchProducts first."))

    return performPurchase(skProduct)
  }

  override suspend fun restorePurchases(): RestorationResult {
    return performRestore()
  }

  private suspend fun requestProducts(ids: Set<String>): List<SKProduct> =
    suspendCancellableCoroutine { continuation ->
      val delegate = object : NSObject(), SKProductsRequestDelegateProtocol {
        override fun productsRequest(request: SKProductsRequest, didReceiveResponse: SKProductsResponse) {
          @Suppress("UNCHECKED_CAST")
          val products = didReceiveResponse.products as? List<SKProduct> ?: emptyList()
          continuation.resume(products)
        }

        override fun request(request: SKRequest, didFailWithError: platform.Foundation.NSError) {
          continuation.resumeWithException(
            Exception("StoreKit product request failed: ${didFailWithError.localizedDescription}"),
          )
        }
      }

      val request = SKProductsRequest(productIdentifiers = ids)
      request.delegate = delegate
      request.start()
    }

  private suspend fun performPurchase(product: SKProduct): PurchaseResult {
    val deferred = CompletableDeferred<PurchaseResult>()

    val observer = object : NSObject(), SKPaymentTransactionObserverProtocol {
      override fun paymentQueue(
        queue: SKPaymentQueue,
        updatedTransactions: List<*>,
      ) {
        for (transaction in updatedTransactions) {
          val txn = transaction as? SKPaymentTransaction ?: continue
          if (txn.payment.productIdentifier != product.productIdentifier) continue

          val state = txn.transactionState
          when {
            state == SKPaymentTransactionState.SKPaymentTransactionStatePurchased -> {
              queue.finishTransaction(txn)
              paymentQueue.removeTransactionObserver(this)
              deferred.complete(PurchaseResult.Purchased)
            }
            state == SKPaymentTransactionState.SKPaymentTransactionStateFailed -> {
              queue.finishTransaction(txn)
              paymentQueue.removeTransactionObserver(this)
              val error = txn.error
              if (error?.code == 2L) { // SKErrorPaymentCancelled
                deferred.complete(PurchaseResult.Cancelled)
              } else {
                deferred.complete(
                  PurchaseResult.Failed(
                    Exception(error?.localizedDescription ?: "Purchase failed"),
                  ),
                )
              }
            }
            state == SKPaymentTransactionState.SKPaymentTransactionStateDeferred -> {
              paymentQueue.removeTransactionObserver(this)
              deferred.complete(PurchaseResult.Pending)
            }
            else -> { /* Purchasing state — wait */ }
          }
        }
      }
    }

    paymentQueue.addTransactionObserver(observer)
    paymentQueue.addPayment(SKPayment.paymentWithProduct(product))

    return deferred.await()
  }

  private suspend fun performRestore(): RestorationResult {
    val deferred = CompletableDeferred<RestorationResult>()

    val observer = object : NSObject(), SKPaymentTransactionObserverProtocol {
      private var restoredAny = false

      override fun paymentQueue(
        queue: SKPaymentQueue,
        updatedTransactions: List<*>,
      ) {
        for (transaction in updatedTransactions) {
          val txn = transaction as? SKPaymentTransaction ?: continue
          val state = txn.transactionState
          when {
            state == SKPaymentTransactionState.SKPaymentTransactionStateRestored -> {
              restoredAny = true
              queue.finishTransaction(txn)
            }
            state == SKPaymentTransactionState.SKPaymentTransactionStateFailed -> {
              queue.finishTransaction(txn)
            }
            else -> {}
          }
        }
      }

      override fun paymentQueueRestoreCompletedTransactionsFinished(queue: SKPaymentQueue) {
        paymentQueue.removeTransactionObserver(this)
        if (restoredAny) {
          deferred.complete(RestorationResult.Restored)
        } else {
          deferred.complete(RestorationResult.Failed(Exception("No purchases found to restore")))
        }
      }

      override fun paymentQueue(
        queue: SKPaymentQueue,
        restoreCompletedTransactionsFailedWithError: platform.Foundation.NSError,
      ) {
        paymentQueue.removeTransactionObserver(this)
        deferred.complete(
          RestorationResult.Failed(
            Exception(restoreCompletedTransactionsFailedWithError.localizedDescription),
          ),
        )
      }
    }

    paymentQueue.addTransactionObserver(observer)
    paymentQueue.restoreCompletedTransactions()

    return deferred.await()
  }
}

private fun SKProduct.toStoreProduct(): StoreProduct {
  val formatter = NSNumberFormatter().apply {
    numberStyle = NSNumberFormatterCurrencyStyle
    locale = priceLocale
  }

  val period = subscriptionPeriod
  // period.unit is SKProductPeriodUnit which is a typealias for NSUInteger (ULong)
  // 0=day, 1=week, 2=month, 3=year
  val periodUnit = period?.let { unitFromRaw(it.unit) }
  val periodValue = period?.numberOfUnits?.toInt()

  val trialDays = introductoryPrice?.let { intro ->
    val trialPeriod = intro.subscriptionPeriod
    val trialUnit = unitFromRaw(trialPeriod.unit)
    val trialValue = trialPeriod.numberOfUnits.toInt()
    when (trialUnit) {
      PeriodUnit.DAY -> trialValue
      PeriodUnit.WEEK -> trialValue * 7
      PeriodUnit.MONTH -> trialValue * 30
      PeriodUnit.YEAR -> trialValue * 365
      null -> null
    }
  }

  val currencyCode = formatter.currencyCode ?: "USD"

  return StoreProduct(
    id = productIdentifier,
    name = localizedTitle,
    description = localizedDescription,
    price = price.doubleValue,
    currencyCode = currencyCode,
    localizedPrice = formatter.stringFromNumber(price) ?: "$${price.doubleValue}",
    periodUnit = periodUnit,
    periodValue = periodValue,
    trialPeriodDays = trialDays,
  )
}

// SKProductPeriodUnit raw values: 0=day, 1=week, 2=month, 3=year
@Suppress("NOTHING_TO_INLINE")
private inline fun unitFromRaw(raw: Any?): PeriodUnit? {
  val value = (raw as? Number)?.toLong() ?: return null
  return when (value) {
    0L -> PeriodUnit.DAY
    1L -> PeriodUnit.WEEK
    2L -> PeriodUnit.MONTH
    3L -> PeriodUnit.YEAR
    else -> null
  }
}
