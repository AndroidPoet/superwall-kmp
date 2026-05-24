package io.androidpoet.superwall.di

import io.androidpoet.superwall.storage.IOSLocalStorage
import io.androidpoet.superwall.storekit.StoreKit2Manager
import io.androidpoet.superwall.webview.IOSWebViewPresenter

/**
 * iOS-specific dependency provider.
 * Supplies platform implementations for storage, StoreKit, and paywall presentation.
 */
public val superwallIOSDependencies: SuperwallPlatformDependencies = SuperwallPlatformDependencies(
  localStorage = IOSLocalStorage(),
  storeManager = StoreKit2Manager(),
  paywallPresenter = IOSWebViewPresenter(),
)
