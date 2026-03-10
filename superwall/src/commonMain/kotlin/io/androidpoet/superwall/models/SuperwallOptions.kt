package io.androidpoet.superwall.models

/**
 * Configuration options for the Superwall SDK.
 */
public data class SuperwallOptions(
  /** Network environment for API requests. */
  val networkEnvironment: NetworkEnvironment = NetworkEnvironment.Release,

  /** Whether to preload all paywalls on configuration. */
  val shouldPreloadPaywalls: Boolean = true,

  /** Log level for SDK diagnostics. */
  val logLevel: LogLevel = LogLevel.WARN,

  /** External purchase controller, if using custom purchase handling. */
  val purchaseController: PurchaseController? = null,
)

public enum class NetworkEnvironment(public val baseUrl: String) {
  Release("https://api.superwall.com"),
  Developer("https://dev-api.superwall.com"),
}

public enum class LogLevel {
  VERBOSE,
  DEBUG,
  INFO,
  WARN,
  ERROR,
  NONE,
}
