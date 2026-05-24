package io.androidpoet.superwall.di

import android.app.Activity
import android.content.Context
import io.androidpoet.superwall.billing.GooglePlayStoreManager
import io.androidpoet.superwall.storage.AndroidLocalStorage
import io.androidpoet.superwall.webview.AndroidWebViewPresenter

/**
 * Android-specific dependency provider.
 * Supplies platform implementations for storage, billing, and paywall presentation.
 */
public fun superwallAndroidDependencies(
  context: Context,
  activityProvider: () -> Activity? = { null },
): SuperwallPlatformDependencies = SuperwallPlatformDependencies(
  localStorage = AndroidLocalStorage(context),
  storeManager = GooglePlayStoreManager(context, activityProvider),
  paywallPresenter = AndroidWebViewPresenter(context),
)
