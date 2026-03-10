package io.androidpoet.superwall.models

import io.androidpoet.superwall.models.components.PaywallComponentsConfig
import kotlinx.serialization.Serializable

/**
 * Remote configuration fetched from the Superwall backend.
 * Contains paywall definitions, trigger rules, and product mappings.
 */
@Serializable
public data class SuperwallConfig(
  val buildId: String,
  val paywalls: List<PaywallDefinition> = emptyList(),
  val triggers: List<Trigger> = emptyList(),
  val locales: List<String> = emptyList(),
)

@Serializable
public data class PaywallDefinition(
  val id: String,
  val identifier: String,
  val name: String,
  val url: String,
  val presentationStyle: PaywallPresentationStyle = PaywallPresentationStyle.Fullscreen,
  val productIds: List<String> = emptyList(),
  /** Native component tree for server-driven UI rendering. When present, renders natively instead of WebView. */
  val componentsConfig: PaywallComponentsConfig? = null,
)

@Serializable
public data class Trigger(
  val placementName: String,
  val rules: List<TriggerRule> = emptyList(),
)

@Serializable
public data class TriggerRule(
  val experiment: Experiment,
  val expression: String? = null,
  val expressionJs: String? = null,
)
