package io.androidpoet.superwall.placement

import io.androidpoet.superwall.config.ConfigManager
import io.androidpoet.superwall.config.ConfigState
import io.androidpoet.superwall.identity.IdentityManager
import io.androidpoet.superwall.models.PaywallDefinition
import io.androidpoet.superwall.models.PaywallInfo
import io.androidpoet.superwall.models.SubscriptionStatus
import io.androidpoet.superwall.models.TriggerRule
import kotlinx.coroutines.flow.StateFlow

/**
 * Evaluates placement triggers against the remote config and user state
 * to determine whether a paywall should be presented.
 */
public class PlacementManager(
  private val configManager: ConfigManager,
  private val identityManager: IdentityManager,
  private val subscriptionStatus: StateFlow<SubscriptionStatus>,
) {

  /**
   * Evaluate a placement and determine the presentation result.
   */
  public fun evaluate(
    placement: String,
    params: Map<String, Any?> = emptyMap(),
  ): PlacementResult {
    val configState = configManager.config.value
    if (configState !is ConfigState.Ready) {
      return PlacementResult.NoConfig
    }

    // Active subscribers skip paywalls by default
    if (subscriptionStatus.value is SubscriptionStatus.Active) {
      return PlacementResult.UserIsSubscribed
    }

    val trigger = configState.config.triggers.find { it.placementName == placement }
      ?: return PlacementResult.NoTriggerMatch

    val matchedRule = trigger.rules.firstOrNull { rule ->
      evaluateRule(rule, params)
    } ?: return PlacementResult.NoRuleMatch

    val paywall = configState.config.paywalls.find {
      it.id == matchedRule.experiment.variant.paywallId
    } ?: return PlacementResult.NoPaywallMatch

    return PlacementResult.ShowPaywall(
      paywall = paywall,
      rule = matchedRule,
    )
  }

  /**
   * Evaluate a single trigger rule against user attributes and params.
   * Returns true if the rule matches (or has no expression).
   */
  private fun evaluateRule(
    rule: TriggerRule,
    params: Map<String, Any?>,
  ): Boolean {
    // No expression means the rule always matches
    if (rule.expression == null && rule.expressionJs == null) return true

    val evaluator = ExpressionEvaluator(
      userAttributes = identityManager.userAttributes.value,
      params = params,
    )

    // Prefer the standard expression; fall back to JS expression
    return evaluator.evaluate(rule.expression ?: rule.expressionJs)
  }
}

/**
 * Result of evaluating a placement.
 */
public sealed interface PlacementResult {
  /** Config hasn't loaded yet. */
  public data object NoConfig : PlacementResult

  /** User has an active subscription — skip paywall. */
  public data object UserIsSubscribed : PlacementResult

  /** No trigger matches this placement name. */
  public data object NoTriggerMatch : PlacementResult

  /** Trigger found but no rule matched the current state. */
  public data object NoRuleMatch : PlacementResult

  /** Trigger matched but the assigned paywall wasn't found. */
  public data object NoPaywallMatch : PlacementResult

  /** A paywall should be presented. */
  public data class ShowPaywall(
    val paywall: PaywallDefinition,
    val rule: TriggerRule,
  ) : PlacementResult
}
