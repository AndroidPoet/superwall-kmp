package io.androidpoet.superwall.storage

import platform.Foundation.NSUserDefaults

/**
 * iOS implementation of [LocalStorage] using NSUserDefaults.
 */
public class IOSLocalStorage : LocalStorage {

  private val defaults = NSUserDefaults(suiteName = SUITE_NAME)

  override fun getString(key: String): String? =
    defaults.stringForKey(key)

  override fun putString(key: String, value: String) {
    defaults.setObject(value, forKey = key)
    defaults.synchronize()
  }

  override fun getBoolean(key: String, default: Boolean): Boolean =
    if (defaults.objectForKey(key) != null) {
      defaults.boolForKey(key)
    } else {
      default
    }

  override fun putBoolean(key: String, value: Boolean) {
    defaults.setBool(value, forKey = key)
    defaults.synchronize()
  }

  override fun getLong(key: String, default: Long): Long =
    if (defaults.objectForKey(key) != null) {
      defaults.integerForKey(key)
    } else {
      default
    }

  override fun putLong(key: String, value: Long) {
    defaults.setInteger(value, forKey = key)
    defaults.synchronize()
  }

  override fun remove(key: String) {
    defaults.removeObjectForKey(key)
    defaults.synchronize()
  }

  override fun clear() {
    defaults.dictionaryRepresentation().keys.forEach { key ->
      defaults.removeObjectForKey(key as String)
    }
    defaults.synchronize()
  }

  private companion object {
    const val SUITE_NAME = "io.androidpoet.superwall"
  }
}
