package io.androidpoet.superwall.di

import io.androidpoet.superwall.paywall.PaywallPresenter
import io.androidpoet.superwall.storage.IOSLocalStorage
import io.androidpoet.superwall.storage.LocalStorage
import io.androidpoet.superwall.store.StoreManager
import io.androidpoet.superwall.storekit.StoreKit2Manager
import io.androidpoet.superwall.webview.IOSWebViewPresenter
import org.koin.dsl.module

/**
 * iOS-specific Koin module.
 * Provides platform implementations for storage, StoreKit, and paywall presentation.
 *
 * Usage (from Swift via Kotlin interop):
 * ```swift
 * import SuperwallKMP
 *
 * Superwall.companion.configure(
 *   apiKey: "pk_...",
 *   options: SuperwallOptions(),
 *   platformModule: IOSModuleKt.superwallIOSModule
 * )
 * ```
 */
public val superwallIOSModule: org.koin.core.module.Module = module {
  single<LocalStorage> { IOSLocalStorage() }
  single<StoreManager> { StoreKit2Manager() }
  single<PaywallPresenter> { IOSWebViewPresenter() }
}
