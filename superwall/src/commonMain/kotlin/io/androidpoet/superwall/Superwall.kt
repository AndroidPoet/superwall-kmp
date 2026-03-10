package io.androidpoet.superwall

import io.androidpoet.superwall.analytics.AnalyticsTracker
import io.androidpoet.superwall.config.ConfigManager
import io.androidpoet.superwall.config.ConfigState
import io.androidpoet.superwall.di.superwallCoreModule
import io.androidpoet.superwall.identity.IdentityManager
import io.androidpoet.superwall.models.Entitlement
import io.androidpoet.superwall.models.NetworkEnvironment
import io.androidpoet.superwall.models.PaywallInfo
import io.androidpoet.superwall.models.PaywallPresentationStyle
import io.androidpoet.superwall.models.PurchaseController
import io.androidpoet.superwall.models.PurchaseResult
import io.androidpoet.superwall.models.RestorationResult
import io.androidpoet.superwall.models.SubscriptionStatus
import io.androidpoet.superwall.models.SuperwallDelegate
import io.androidpoet.superwall.models.SuperwallEvent
import io.androidpoet.superwall.models.SuperwallEventInfo
import io.androidpoet.superwall.models.SuperwallOptions
import io.androidpoet.superwall.paywall.PaywallCallback
import io.androidpoet.superwall.paywall.PaywallPresenter
import io.androidpoet.superwall.placement.PlacementManager
import io.androidpoet.superwall.placement.PlacementResult
import io.androidpoet.superwall.store.StoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Main entry point for the Superwall SDK.
 *
 * Usage:
 * ```kotlin
 * Superwall.configure(apiKey = "pk_...")
 * Superwall.instance.register("placement_name") {
 *   // Feature gated behind paywall
 * }
 * ```
 */
public class Superwall private constructor(
  private val configManager: ConfigManager,
  private val identityManager: IdentityManager,
  private val analyticsTracker: AnalyticsTracker,
  private val placementManager: PlacementManager,
  private val paywallPresenter: PaywallPresenter,
  private val storeManager: StoreManager,
  private val purchaseController: PurchaseController?,
  private val scope: CoroutineScope,
) {

  private val _subscriptionStatus = MutableStateFlow<SubscriptionStatus>(SubscriptionStatus.Unknown)
  private val _pendingNativePaywall = MutableStateFlow<NativePaywallRequest?>(null)

  /** Current subscription status. */
  public val subscriptionStatus: StateFlow<SubscriptionStatus> =
    _subscriptionStatus.asStateFlow()

  /**
   * Pending native paywall request. The Compose layer observes this to render
   * a native paywall when the server provides a component tree instead of a URL.
   */
  public val pendingNativePaywall: StateFlow<NativePaywallRequest?> =
    _pendingNativePaywall.asStateFlow()

  /** Dismiss a native paywall and optionally unlock the feature. */
  public fun dismissNativePaywall(purchased: Boolean = false) {
    val request = _pendingNativePaywall.value ?: return
    _pendingNativePaywall.value = null
    val info = request.info
    analyticsTracker.track(SuperwallEvent.PaywallClose(info))
    if (purchased) {
      request.onFeatureUnlocked?.invoke()
    }
  }

  /** Current user's entitlements (derived from subscription status). */
  public val entitlements: Set<Entitlement>
    get() = when (val status = _subscriptionStatus.value) {
      is SubscriptionStatus.Active -> status.entitlements
      else -> emptySet()
    }

  /** Delegate for receiving lifecycle events. */
  public var delegate: SuperwallDelegate? = null

  /** Stream of all SDK events. */
  public val events: SharedFlow<SuperwallEventInfo>
    get() = analyticsTracker.events

  // ── User Identity ───────────────────────────────────────────────

  /** Identify the user. Call after sign-in. */
  public fun identify(userId: String) {
    identityManager.identify(userId)
  }

  /** Set custom user attributes for targeting. */
  public fun setUserAttributes(attributes: Map<String, Any?>) {
    identityManager.setUserAttributes(attributes)
  }

  /** Reset to anonymous state. Call on sign-out. */
  public fun reset() {
    identityManager.reset()
    _subscriptionStatus.value = SubscriptionStatus.Unknown
  }

  // ── Subscription ────────────────────────────────────────────────

  /** Update subscription status. Required when using a custom PurchaseController. */
  public fun setSubscriptionStatus(status: SubscriptionStatus) {
    val previous = _subscriptionStatus.value
    _subscriptionStatus.value = status
    if (previous != status) {
      analyticsTracker.track(SuperwallEvent.SubscriptionStatusDidChange(status))
      delegate?.subscriptionStatusDidChange(status)
    }
  }

  // ── Placements ──────────────────────────────────────────────────

  /**
   * Register a placement. If a paywall is configured for this placement
   * and the user is not subscribed, the paywall will be presented.
   * If no paywall matches, [onFeatureUnlocked] is called immediately.
   *
   * @param placement The placement name configured in the Superwall dashboard.
   * @param params Optional parameters for rule evaluation.
   * @param onFeatureUnlocked Called when the feature should be unlocked
   *   (either no paywall matched or user completed purchase).
   */
  public fun register(
    placement: String,
    params: Map<String, Any?> = emptyMap(),
    onFeatureUnlocked: (() -> Unit)? = null,
  ) {
    analyticsTracker.track(SuperwallEvent.PlacementRegistered(placement, params))

    when (val result = placementManager.evaluate(placement, params)) {
      is PlacementResult.ShowPaywall -> {
        presentPaywall(result, onFeatureUnlocked)
      }
      is PlacementResult.UserIsSubscribed -> {
        onFeatureUnlocked?.invoke()
      }
      else -> {
        onFeatureUnlocked?.invoke()
      }
    }
  }

  /**
   * Get the presentation result for a placement without presenting.
   * Useful for checking whether a paywall would be shown.
   */
  public fun getPresentationResult(
    placement: String,
    params: Map<String, Any?> = emptyMap(),
  ): PlacementResult {
    return placementManager.evaluate(placement, params)
  }

  // ── Paywall Presentation ────────────────────────────────────────

  private fun presentPaywall(
    result: PlacementResult.ShowPaywall,
    onFeatureUnlocked: (() -> Unit)?,
  ) {
    val paywall = result.paywall
    val info = PaywallInfo(
      id = paywall.id,
      identifier = paywall.identifier,
      name = paywall.name,
      url = paywall.url,
      experiment = result.rule.experiment,
      presentationStyle = paywall.presentationStyle,
      componentsConfig = paywall.componentsConfig,
    )

    // If the paywall has a native component tree, notify via event
    // so the Compose layer can render it instead of the WebView presenter.
    if (info.isNativeRendering) {
      analyticsTracker.track(SuperwallEvent.PaywallOpen(info))
      _pendingNativePaywall.value = NativePaywallRequest(info, onFeatureUnlocked)
      return
    }

    scope.launch {
      paywallPresenter.present(
        url = paywall.url,
        info = info,
        style = paywall.presentationStyle,
        callback = object : PaywallCallback {
          override fun onPresented(info: PaywallInfo) {
            analyticsTracker.track(SuperwallEvent.PaywallOpen(info))
            delegate?.handleSuperwallEvent(
              SuperwallEventInfo(
                event = SuperwallEvent.PaywallOpen(info),
                timestamp = kotlinx.datetime.Clock.System.now(),
              ),
            )
          }

          override fun onDismissed(info: PaywallInfo) {
            analyticsTracker.track(SuperwallEvent.PaywallClose(info))
          }

          override fun onPurchaseInitiated(productId: String) {
            handlePurchase(productId, info, onFeatureUnlocked)
          }

          override fun onRestoreInitiated() {
            handleRestore(info)
          }

          override fun onCustomAction(name: String) {
            delegate?.handleCustomPaywallAction(name)
          }

          override fun onOpenUrl(url: String) {
            if (delegate?.paywallWillOpenUrl(url) != true) {
              analyticsTracker.track(SuperwallEvent.PaywallOpenUrl(url, info))
            }
          }

          override fun onOpenDeepLink(url: String) {
            if (delegate?.paywallWillOpenDeepLink(url) != true) {
              analyticsTracker.track(SuperwallEvent.PaywallOpenDeepLink(url, info))
            }
          }

          override fun onError(error: Throwable) {
            onFeatureUnlocked?.invoke()
          }
        },
      )
    }
  }

  private fun handlePurchase(
    productId: String,
    info: PaywallInfo,
    onFeatureUnlocked: (() -> Unit)?,
  ) {
    scope.launch {
      val products = storeManager.fetchProducts(setOf(productId))
      val product = products.firstOrNull() ?: return@launch

      analyticsTracker.track(SuperwallEvent.TransactionStart(product, info))

      val result = if (purchaseController != null) {
        purchaseController.purchase(product)
      } else {
        storeManager.purchase(product)
      }

      when (result) {
        is PurchaseResult.Purchased -> {
          analyticsTracker.track(SuperwallEvent.TransactionComplete(product, info))
          if (product.trialPeriodDays != null && product.trialPeriodDays > 0) {
            analyticsTracker.track(SuperwallEvent.FreeTrialStart(product, info))
          } else {
            analyticsTracker.track(SuperwallEvent.SubscriptionStart(product, info))
          }
          paywallPresenter.dismiss()
          onFeatureUnlocked?.invoke()
        }
        is PurchaseResult.Failed -> {
          analyticsTracker.track(SuperwallEvent.TransactionFail(result.error, info))
        }
        is PurchaseResult.Cancelled -> { /* User cancelled, stay on paywall */ }
        is PurchaseResult.Pending -> { /* Waiting for approval */ }
      }
    }
  }

  private fun handleRestore(info: PaywallInfo) {
    scope.launch {
      val result = if (purchaseController != null) {
        purchaseController.restorePurchases()
      } else {
        storeManager.restorePurchases()
      }

      when (result) {
        is RestorationResult.Restored -> {
          analyticsTracker.track(SuperwallEvent.Restore(info))
          paywallPresenter.dismiss()
        }
        is RestorationResult.Failed -> {
          analyticsTracker.track(SuperwallEvent.RestoreFail(result.error, info))
        }
      }
    }
  }

  // ── Preloading ──────────────────────────────────────────────────

  /** Preload all paywalls for faster presentation. */
  public fun preloadAllPaywalls() {
    val config = (configManager.config.value as? ConfigState.Ready)?.config ?: return
    scope.launch {
      config.paywalls.forEach { paywall ->
        storeManager.fetchProducts(paywall.productIds.toSet())
      }
    }
  }

  /** Refresh the remote configuration. */
  public fun refreshConfiguration() {
    scope.launch { configManager.refresh() }
  }

  /** Flush pending analytics events. */
  public fun flushEvents() {
    analyticsTracker.flush()
  }

  // ── Static Configuration ────────────────────────────────────────

  public companion object {

    @kotlin.concurrent.Volatile
    private var _instance: Superwall? = null
    private var koinApp: KoinApplication? = null

    /** The shared Superwall instance. Throws if not configured. */
    public val instance: Superwall
      get() = _instance ?: error(
        "Superwall not configured. Call Superwall.configure(apiKey:) first.",
      )

    /**
     * Configure and initialize the SDK.
     *
     * @param apiKey Your Superwall API key.
     * @param options Optional configuration.
     * @param platformModule Koin module providing platform-specific implementations.
     *   Must bind: [StoreManager], [PaywallPresenter], [LocalStorage].
     */
    public fun configure(
      apiKey: String,
      options: SuperwallOptions = SuperwallOptions(),
      platformModule: org.koin.core.module.Module,
      completion: ((Result<Superwall>) -> Unit)? = null,
    ) {
      if (_instance != null) {
        completion?.invoke(Result.success(_instance!!))
        return
      }

      val parametersModule = module {
        single(named("apiKey")) { apiKey }
        single { options.networkEnvironment }
        single(named("subscriptionStatus")) {
          MutableStateFlow<SubscriptionStatus>(SubscriptionStatus.Unknown)
        }
      }

      koinApp = startKoin {
        modules(parametersModule, superwallCoreModule, platformModule)
      }

      val koin = koinApp!!.koin

      val superwall = Superwall(
        configManager = koin.get(),
        identityManager = koin.get(),
        analyticsTracker = koin.get(),
        placementManager = koin.get(),
        paywallPresenter = koin.get(),
        storeManager = koin.get(),
        purchaseController = options.purchaseController,
        scope = koin.get(),
      )

      _instance = superwall

      // Fetch config on init
      superwall.scope.launch {
        try {
          superwall.configManager.fetchConfig()
          superwall.analyticsTracker.track(SuperwallEvent.ConfigReady)
          if (options.shouldPreloadPaywalls) {
            superwall.preloadAllPaywalls()
          }
          completion?.invoke(Result.success(superwall))
        } catch (e: Exception) {
          completion?.invoke(Result.failure(e))
        }
      }
    }
  }
}

/**
 * Request to present a native paywall.
 * The Compose layer observes [Superwall.pendingNativePaywall] and renders
 * a [NativePaywall] composable when this is non-null.
 */
public data class NativePaywallRequest(
  /** Paywall metadata including the component tree. */
  public val info: PaywallInfo,
  /** Called when the feature should be unlocked after purchase. */
  public val onFeatureUnlocked: (() -> Unit)? = null,
)
