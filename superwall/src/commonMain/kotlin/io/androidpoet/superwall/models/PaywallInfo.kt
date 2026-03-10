package io.androidpoet.superwall.models

import kotlinx.serialization.Serializable

/**
 * Metadata about a paywall, provided in lifecycle callbacks.
 */
@Serializable
public data class PaywallInfo(
  val id: String,
  val identifier: String,
  val name: String,
  val url: String,
  val experiment: Experiment? = null,
  val products: List<StoreProduct> = emptyList(),
  val presentationStyle: PaywallPresentationStyle = PaywallPresentationStyle.Fullscreen,
)

@Serializable
public sealed interface PaywallPresentationStyle {

  @Serializable
  public data object Modal : PaywallPresentationStyle

  @Serializable
  public data object Fullscreen : PaywallPresentationStyle

  @Serializable
  public data object FullscreenNoAnimation : PaywallPresentationStyle

  @Serializable
  public data object Push : PaywallPresentationStyle

  @Serializable
  public data class Drawer(
    val height: Double = 0.6,
    val cornerRadius: Double = 16.0,
  ) : PaywallPresentationStyle

  @Serializable
  public data class Popup(
    val height: Double = 0.5,
    val width: Double = 0.8,
    val cornerRadius: Double = 16.0,
  ) : PaywallPresentationStyle
}
