package io.androidpoet.superwall.identity

import io.androidpoet.superwall.storage.LocalStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages user identification, attributes, and anonymous aliases.
 */
public class IdentityManager(
  private val storage: LocalStorage,
) {

  private val _userId = MutableStateFlow<String?>(null)
  public val userId: StateFlow<String?> = _userId.asStateFlow()

  private val _aliasId = MutableStateFlow(loadOrCreateAlias())
  public val aliasId: StateFlow<String> = _aliasId.asStateFlow()

  private val _userAttributes = MutableStateFlow<Map<String, Any?>>(emptyMap())
  public val userAttributes: StateFlow<Map<String, Any?>> = _userAttributes.asStateFlow()

  /** The effective identifier: userId if set, otherwise aliasId. */
  public val effectiveId: String
    get() = _userId.value ?: _aliasId.value

  /**
   * Identify the user with a known user ID.
   */
  public fun identify(userId: String) {
    _userId.value = userId
    storage.putString(USER_ID_KEY, userId)
  }

  /**
   * Set custom user attributes for targeting and analytics.
   */
  public fun setUserAttributes(attributes: Map<String, Any?>) {
    _userAttributes.value = _userAttributes.value + attributes
  }

  /**
   * Reset identity to anonymous state. Generates a new alias.
   */
  public fun reset() {
    _userId.value = null
    _userAttributes.value = emptyMap()
    storage.remove(USER_ID_KEY)
    val newAlias = generateAlias()
    _aliasId.value = newAlias
    storage.putString(ALIAS_KEY, newAlias)
  }

  private fun loadOrCreateAlias(): String {
    val saved = storage.getString(ALIAS_KEY)
    if (saved != null) return saved

    // Restore userId if previously identified
    _userId.value = storage.getString(USER_ID_KEY)

    val alias = generateAlias()
    storage.putString(ALIAS_KEY, alias)
    return alias
  }

  private fun generateAlias(): String {
    // Simple UUID-like generation using random bytes
    val chars = "0123456789abcdef"
    val segments = intArrayOf(8, 4, 4, 4, 12)
    return segments.joinToString("-") { length ->
      (1..length).map { chars.random() }.joinToString("")
    }
  }

  private companion object {
    const val USER_ID_KEY = "superwall_user_id"
    const val ALIAS_KEY = "superwall_alias_id"
  }
}
