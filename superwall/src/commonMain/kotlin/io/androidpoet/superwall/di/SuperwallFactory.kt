package io.androidpoet.superwall.di

import io.androidpoet.superwall.analytics.AnalyticsTracker
import io.androidpoet.superwall.config.ConfigManager
import io.androidpoet.superwall.identity.IdentityManager
import io.androidpoet.superwall.models.NetworkEnvironment
import io.androidpoet.superwall.models.SubscriptionStatus
import io.androidpoet.superwall.network.SuperwallApi
import io.androidpoet.superwall.paywall.PaywallPresenter
import io.androidpoet.superwall.placement.PlacementManager
import io.androidpoet.superwall.storage.LocalStorage
import io.androidpoet.superwall.store.StoreManager
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json

public data class SuperwallPlatformDependencies(
  val localStorage: LocalStorage,
  val storeManager: StoreManager,
  val paywallPresenter: PaywallPresenter,
)

internal data class SuperwallCoreDependencies(
  val configManager: ConfigManager,
  val identityManager: IdentityManager,
  val analyticsTracker: AnalyticsTracker,
  val placementManager: PlacementManager,
  val scope: CoroutineScope,
)

internal fun createSuperwallCoreDependencies(
  apiKey: String,
  networkEnvironment: NetworkEnvironment,
  localStorage: LocalStorage,
  subscriptionStatus: MutableStateFlow<SubscriptionStatus>,
): SuperwallCoreDependencies {
  val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    prettyPrint = false
  }

  val httpClient = HttpClient {
    install(ContentNegotiation) {
      json(json)
    }
  }

  val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  val api = SuperwallApi(
    httpClient = httpClient,
    baseUrl = networkEnvironment.baseUrl,
    apiKey = apiKey,
  )

  val identityManager = IdentityManager(storage = localStorage)
  val configManager = ConfigManager(api = api, storage = localStorage, json = json)
  val analyticsTracker = AnalyticsTracker(api = api, identityManager = identityManager, scope = scope)
  val placementManager = PlacementManager(
    configManager = configManager,
    identityManager = identityManager,
    subscriptionStatus = subscriptionStatus,
  )

  return SuperwallCoreDependencies(
    configManager = configManager,
    identityManager = identityManager,
    analyticsTracker = analyticsTracker,
    placementManager = placementManager,
    scope = scope,
  )
}
