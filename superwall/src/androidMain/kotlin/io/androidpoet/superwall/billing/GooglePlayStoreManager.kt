package io.androidpoet.superwall.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import io.androidpoet.superwall.models.PeriodUnit
import io.androidpoet.superwall.models.PurchaseResult
import io.androidpoet.superwall.models.RestorationResult
import io.androidpoet.superwall.models.StoreProduct
import io.androidpoet.superwall.store.StoreManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

/**
 * Android implementation of [StoreManager] using Google Play Billing Library.
 * Manages BillingClient lifecycle, product queries, purchases, and restores.
 */
public class GooglePlayStoreManager(
  private val context: Context,
  private val activityProvider: () -> Activity?,
) : StoreManager {

  private val connectionMutex = Mutex()
  private var pendingPurchaseResult: CompletableDeferred<PurchaseResult>? = null

  private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
    val deferred = pendingPurchaseResult ?: return@PurchasesUpdatedListener
    when (billingResult.responseCode) {
      BillingClient.BillingResponseCode.OK -> {
        val purchase = purchases?.firstOrNull()
        if (purchase != null && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
          deferred.complete(PurchaseResult.Purchased)
        } else if (purchase != null && purchase.purchaseState == Purchase.PurchaseState.PENDING) {
          deferred.complete(PurchaseResult.Pending)
        } else {
          deferred.complete(PurchaseResult.Failed(Exception("Unknown purchase state")))
        }
      }
      BillingClient.BillingResponseCode.USER_CANCELED -> {
        deferred.complete(PurchaseResult.Cancelled)
      }
      else -> {
        deferred.complete(
          PurchaseResult.Failed(
            BillingException(billingResult.responseCode, billingResult.debugMessage),
          ),
        )
      }
    }
    pendingPurchaseResult = null
  }

  private val billingClient: BillingClient = BillingClient.newBuilder(context)
    .setListener(purchasesUpdatedListener)
    .enablePendingPurchases()
    .build()

  // Cache product details for purchase flow
  private val productDetailsCache = mutableMapOf<String, com.android.billingclient.api.ProductDetails>()

  override suspend fun fetchProducts(ids: Set<String>): List<StoreProduct> {
    ensureConnected()

    val subsProducts = queryProductDetails(ids, BillingClient.ProductType.SUBS)
    val inappProducts = queryProductDetails(ids, BillingClient.ProductType.INAPP)
    val allDetails = subsProducts + inappProducts

    allDetails.forEach { productDetailsCache[it.productId] = it }

    return allDetails.map { it.toStoreProduct() }
  }

  override suspend fun purchase(product: StoreProduct): PurchaseResult {
    ensureConnected()

    val activity = activityProvider()
      ?: return PurchaseResult.Failed(Exception("No activity available for purchase flow"))

    val productDetails = productDetailsCache[product.id]
      ?: return PurchaseResult.Failed(Exception("Product details not found for ${product.id}. Call fetchProducts first."))

    val flowParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
      .setProductDetails(productDetails)

    // For subscriptions, select the first offer
    productDetails.subscriptionOfferDetails?.firstOrNull()?.let { offer ->
      flowParamsBuilder.setOfferToken(offer.offerToken)
    }

    val billingFlowParams = BillingFlowParams.newBuilder()
      .setProductDetailsParamsList(listOf(flowParamsBuilder.build()))
      .build()

    val deferred = CompletableDeferred<PurchaseResult>()
    pendingPurchaseResult = deferred

    val result = billingClient.launchBillingFlow(activity, billingFlowParams)
    if (result.responseCode != BillingClient.BillingResponseCode.OK) {
      pendingPurchaseResult = null
      return PurchaseResult.Failed(
        BillingException(result.responseCode, result.debugMessage),
      )
    }

    val purchaseResult = deferred.await()

    // Acknowledge the purchase if successful
    if (purchaseResult is PurchaseResult.Purchased) {
      acknowledgePendingPurchases()
    }

    return purchaseResult
  }

  override suspend fun restorePurchases(): RestorationResult {
    ensureConnected()

    return try {
      val subsResult = billingClient.queryPurchasesAsync(
        QueryPurchasesParams.newBuilder()
          .setProductType(BillingClient.ProductType.SUBS)
          .build(),
      )
      val inappResult = billingClient.queryPurchasesAsync(
        QueryPurchasesParams.newBuilder()
          .setProductType(BillingClient.ProductType.INAPP)
          .build(),
      )

      val allPurchases = (subsResult.purchasesList + inappResult.purchasesList)
        .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }

      if (allPurchases.isNotEmpty()) {
        acknowledgePendingPurchases()
        RestorationResult.Restored
      } else {
        RestorationResult.Failed(Exception("No purchases found to restore"))
      }
    } catch (e: Exception) {
      RestorationResult.Failed(e)
    }
  }

  private suspend fun queryProductDetails(
    ids: Set<String>,
    productType: String,
  ): List<com.android.billingclient.api.ProductDetails> {
    val productList = ids.map { id ->
      QueryProductDetailsParams.Product.newBuilder()
        .setProductId(id)
        .setProductType(productType)
        .build()
    }

    val params = QueryProductDetailsParams.newBuilder()
      .setProductList(productList)
      .build()

    val result: ProductDetailsResult = billingClient.queryProductDetails(params)
    return if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
      result.productDetailsList.orEmpty()
    } else {
      emptyList()
    }
  }

  private suspend fun acknowledgePendingPurchases() {
    val subsResult = billingClient.queryPurchasesAsync(
      QueryPurchasesParams.newBuilder()
        .setProductType(BillingClient.ProductType.SUBS)
        .build(),
    )
    val inappResult = billingClient.queryPurchasesAsync(
      QueryPurchasesParams.newBuilder()
        .setProductType(BillingClient.ProductType.INAPP)
        .build(),
    )

    (subsResult.purchasesList + inappResult.purchasesList)
      .filter { !it.isAcknowledged && it.purchaseState == Purchase.PurchaseState.PURCHASED }
      .forEach { purchase ->
        val ackParams = com.android.billingclient.api.AcknowledgePurchaseParams.newBuilder()
          .setPurchaseToken(purchase.purchaseToken)
          .build()
        billingClient.acknowledgePurchase(ackParams)
      }
  }

  private suspend fun ensureConnected() {
    connectionMutex.withLock {
      if (billingClient.isReady) return

      suspendCancellableCoroutine { continuation ->
        billingClient.startConnection(object : BillingClientStateListener {
          override fun onBillingSetupFinished(billingResult: BillingResult) {
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
              continuation.resume(Unit)
            } else {
              continuation.resume(Unit) // Continue anyway, individual calls will fail
            }
          }

          override fun onBillingServiceDisconnected() {
            // Will reconnect on next call
          }
        })
      }
    }
  }
}

public class BillingException(
  public val responseCode: Int,
  public val debugMessage: String,
) : Exception("Billing error ($responseCode): $debugMessage")

private fun com.android.billingclient.api.ProductDetails.toStoreProduct(): StoreProduct {
  // Try subscription pricing first, fall back to one-time
  val subOffer = subscriptionOfferDetails?.firstOrNull()
  val pricingPhase = subOffer?.pricingPhases?.pricingPhaseList?.firstOrNull()

  return if (pricingPhase != null) {
    StoreProduct(
      id = productId,
      name = name,
      description = description,
      price = pricingPhase.priceAmountMicros / 1_000_000.0,
      currencyCode = pricingPhase.priceCurrencyCode,
      localizedPrice = pricingPhase.formattedPrice,
      periodUnit = pricingPhase.billingPeriod.toPeriodUnit(),
      periodValue = pricingPhase.billingPeriod.toPeriodValue(),
      trialPeriodDays = subOffer.detectTrialDays(),
    )
  } else {
    // One-time purchase
    val oneTimeOffer = oneTimePurchaseOfferDetails
    StoreProduct(
      id = productId,
      name = name,
      description = description,
      price = (oneTimeOffer?.priceAmountMicros ?: 0) / 1_000_000.0,
      currencyCode = oneTimeOffer?.priceCurrencyCode ?: "USD",
      localizedPrice = oneTimeOffer?.formattedPrice ?: "$0.00",
    )
  }
}

// ISO 8601 period parsing (P1W, P1M, P3M, P6M, P1Y)
private fun String.toPeriodUnit(): PeriodUnit? = when {
  endsWith("D") -> PeriodUnit.DAY
  endsWith("W") -> PeriodUnit.WEEK
  endsWith("M") -> PeriodUnit.MONTH
  endsWith("Y") -> PeriodUnit.YEAR
  else -> null
}

private fun String.toPeriodValue(): Int? {
  val digits = filter { it.isDigit() }
  return digits.toIntOrNull()
}

private fun com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails.detectTrialDays(): Int? {
  val trialPhase = pricingPhases.pricingPhaseList.firstOrNull {
    it.priceAmountMicros == 0L
  } ?: return null

  val period = trialPhase.billingPeriod
  val value = period.toPeriodValue() ?: return null
  return when (period.toPeriodUnit()) {
    PeriodUnit.DAY -> value
    PeriodUnit.WEEK -> value * 7
    PeriodUnit.MONTH -> value * 30
    PeriodUnit.YEAR -> value * 365
    null -> null
  }
}
