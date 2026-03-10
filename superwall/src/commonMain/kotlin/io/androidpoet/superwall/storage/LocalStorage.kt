package io.androidpoet.superwall.storage

/**
 * Platform-agnostic key-value storage.
 * Android: SharedPreferences. iOS: UserDefaults.
 * Bound via Koin in platform-specific modules.
 */
public interface LocalStorage {

  public fun getString(key: String): String?
  public fun putString(key: String, value: String)
  public fun getBoolean(key: String, default: Boolean = false): Boolean
  public fun putBoolean(key: String, value: Boolean)
  public fun getLong(key: String, default: Long = 0L): Long
  public fun putLong(key: String, value: Long)
  public fun remove(key: String)
  public fun clear()
}
