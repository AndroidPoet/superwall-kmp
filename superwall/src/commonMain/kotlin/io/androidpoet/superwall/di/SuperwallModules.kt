package io.androidpoet.superwall.di

import io.androidpoet.superwall.analytics.AnalyticsTracker
import io.androidpoet.superwall.config.ConfigManager
import io.androidpoet.superwall.identity.IdentityManager
import io.androidpoet.superwall.models.NetworkEnvironment
import io.androidpoet.superwall.network.SuperwallApi
import io.androidpoet.superwall.placement.PlacementManager
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Core Koin module providing shared business logic.
 * Platform-specific modules must provide: [StoreManager], [PaywallPresenter], [LocalStorage].
 */
public val superwallCoreModule: org.koin.core.module.Module = module {

  single {
    Json {
      ignoreUnknownKeys = true
      isLenient = true
      encodeDefaults = true
      prettyPrint = false
    }
  }

  single {
    HttpClient {
      install(ContentNegotiation) {
        json(get())
      }
    }
  }

  single {
    CoroutineScope(SupervisorJob() + Dispatchers.Default)
  }

  single {
    SuperwallApi(
      httpClient = get(),
      baseUrl = get<NetworkEnvironment>().baseUrl,
      apiKey = get(named("apiKey")),
    )
  }

  single { IdentityManager(storage = get()) }

  single {
    ConfigManager(
      api = get(),
      storage = get(),
      json = get(),
    )
  }

  single {
    AnalyticsTracker(
      api = get(),
      identityManager = get(),
      scope = get(),
    )
  }

  single {
    PlacementManager(
      configManager = get(),
      identityManager = get(),
      subscriptionStatus = get(named("subscriptionStatus")),
    )
  }
}
