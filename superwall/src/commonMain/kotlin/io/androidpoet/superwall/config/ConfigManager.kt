package io.androidpoet.superwall.config

import io.androidpoet.superwall.models.SuperwallConfig
import io.androidpoet.superwall.network.SuperwallApi
import io.androidpoet.superwall.storage.LocalStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Manages remote configuration fetching, caching, and state.
 * Fetches on init and periodically refreshes.
 */
public class ConfigManager(
  private val api: SuperwallApi,
  private val storage: LocalStorage,
  private val json: Json,
) {

  private val _config = MutableStateFlow<ConfigState>(ConfigState.Loading)
  public val config: StateFlow<ConfigState> = _config.asStateFlow()

  /**
   * Fetch configuration from the backend.
   * Falls back to cached config on failure.
   */
  public suspend fun fetchConfig() {
    _config.value = ConfigState.Loading
    try {
      val remoteConfig = api.getConfig()
      _config.value = ConfigState.Ready(remoteConfig)
      cacheConfig(remoteConfig)
    } catch (e: Exception) {
      val cached = loadCachedConfig()
      _config.value = if (cached != null) {
        ConfigState.Ready(cached)
      } else {
        ConfigState.Failed(e)
      }
    }
  }

  /**
   * Force refresh the configuration.
   */
  public suspend fun refresh() {
    fetchConfig()
  }

  private fun cacheConfig(config: SuperwallConfig) {
    storage.putString(CACHE_KEY, json.encodeToString(config))
  }

  private fun loadCachedConfig(): SuperwallConfig? {
    val cached = storage.getString(CACHE_KEY) ?: return null
    return try {
      json.decodeFromString<SuperwallConfig>(cached)
    } catch (_: Exception) {
      null
    }
  }

  private companion object {
    const val CACHE_KEY = "superwall_config_cache"
  }
}

/**
 * The state of remote configuration.
 */
public sealed interface ConfigState {
  public data object Loading : ConfigState
  public data class Ready(val config: SuperwallConfig) : ConfigState
  public data class Failed(val error: Throwable) : ConfigState
}
