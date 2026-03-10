package io.androidpoet.superwall.storage

import android.content.Context
import android.content.SharedPreferences

/**
 * Android implementation of [LocalStorage] using SharedPreferences.
 */
public class AndroidLocalStorage(context: Context) : LocalStorage {

  private val prefs: SharedPreferences =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  override fun getString(key: String): String? = prefs.getString(key, null)

  override fun putString(key: String, value: String) {
    prefs.edit().putString(key, value).apply()
  }

  override fun getBoolean(key: String, default: Boolean): Boolean =
    prefs.getBoolean(key, default)

  override fun putBoolean(key: String, value: Boolean) {
    prefs.edit().putBoolean(key, value).apply()
  }

  override fun getLong(key: String, default: Long): Long =
    prefs.getLong(key, default)

  override fun putLong(key: String, value: Long) {
    prefs.edit().putLong(key, value).apply()
  }

  override fun remove(key: String) {
    prefs.edit().remove(key).apply()
  }

  override fun clear() {
    prefs.edit().clear().apply()
  }

  private companion object {
    const val PREFS_NAME = "superwall_sdk"
  }
}
