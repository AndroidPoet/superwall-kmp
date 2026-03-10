package io.androidpoet.superwall.models.components

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Actions that can be triggered by user interaction with paywall components.
 */
@Serializable
public sealed interface Action {
  /** Close the paywall. */
  @Serializable
  @SerialName("close")
  public data object Close : Action

  /** Initiate purchase of the selected product. */
  @Serializable
  @SerialName("purchase")
  public data class Purchase(val productId: String? = null) : Action

  /** Restore previous purchases. */
  @Serializable
  @SerialName("restore")
  public data object Restore : Action

  /** Open a URL in the browser. */
  @Serializable
  @SerialName("open_url")
  public data class OpenUrl(val url: String) : Action

  /** Open a deep link. */
  @Serializable
  @SerialName("deep_link")
  public data class DeepLink(val url: String) : Action

  /** Navigate to another screen/section within the paywall. */
  @Serializable
  @SerialName("navigate")
  public data class Navigate(val destination: String) : Action

  /** Custom action dispatched to the delegate. */
  @Serializable
  @SerialName("custom")
  public data class Custom(val name: String, val params: Map<String, String> = emptyMap()) : Action
}
