package io.androidpoet.superwall.models

import kotlinx.serialization.Serializable

/**
 * Represents an A/B test experiment and the variant assigned to this user.
 */
@Serializable
public data class Experiment(
  val id: String,
  val groupId: String,
  val variant: Variant,
)

@Serializable
public data class Variant(
  val id: String,
  val type: VariantType,
  val paywallId: String? = null,
)

@Serializable
public enum class VariantType {
  TREATMENT,
  HOLDOUT,
}
