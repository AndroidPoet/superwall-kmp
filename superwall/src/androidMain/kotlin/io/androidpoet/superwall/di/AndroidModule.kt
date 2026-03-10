package io.androidpoet.superwall.di

import android.app.Activity
import android.content.Context
import io.androidpoet.superwall.billing.GooglePlayStoreManager
import io.androidpoet.superwall.paywall.PaywallPresenter
import io.androidpoet.superwall.storage.AndroidLocalStorage
import io.androidpoet.superwall.storage.LocalStorage
import io.androidpoet.superwall.store.StoreManager
import io.androidpoet.superwall.webview.AndroidWebViewPresenter
import org.koin.dsl.module

/**
 * Android-specific Koin module.
 * Provides platform implementations for storage, billing, and paywall presentation.
 *
 * Usage:
 * ```kotlin
 * // In Application.onCreate
 * Superwall.configure(
 *   apiKey = "pk_...",
 *   platformModule = superwallAndroidModule(
 *     context = applicationContext,
 *     activityProvider = { currentActivity },
 *   ),
 * )
 * ```
 *
 * @param context Application context.
 * @param activityProvider Lambda that returns the current foreground Activity,
 *   needed for launching the billing flow. Return null if no activity is available.
 */
public fun superwallAndroidModule(
  context: Context,
  activityProvider: () -> Activity? = { null },
): org.koin.core.module.Module = module {
  single<LocalStorage> { AndroidLocalStorage(context) }
  single<StoreManager> { GooglePlayStoreManager(context, activityProvider) }
  single<PaywallPresenter> { AndroidWebViewPresenter(context) }
}
