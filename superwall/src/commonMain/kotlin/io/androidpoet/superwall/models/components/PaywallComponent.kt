package io.androidpoet.superwall.models.components

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A paywall component tree node. Server sends a JSON tree of these components,
 * which is rendered natively via Compose Multiplatform.
 *
 * Inspired by RevenueCat's server-driven UI architecture:
 * - Stack → Column/Row/Box depending on [Dimension]
 * - Text/Image/Button → Material 3 composables
 * - Package/PurchaseButton → billing-aware components
 */
@Serializable
public sealed interface PaywallComponent {

  /**
   * Stack layout — the core container component.
   * Maps to Column (vertical), Row (horizontal), or Box (z-layer) in Compose.
   */
  @Serializable
  @SerialName("stack")
  public data class Stack(
    val dimension: Dimension = Dimension.VERTICAL,
    val components: List<PaywallComponent> = emptyList(),
    val size: Size = Size(),
    val padding: Padding = Padding.Zero,
    val margin: Margin = Margin(),
    val spacing: Double = 0.0,
    val backgroundColor: ColorScheme? = null,
    val alignment: FlexAlignment = FlexAlignment.START,
    val crossAlignment: CrossAlignment = CrossAlignment.START,
    val cornerRadius: CornerRadius? = null,
    val border: Border? = null,
    val shadow: Shadow? = null,
    val overflow: OverflowBehavior = OverflowBehavior.CLIP,
  ) : PaywallComponent

  /**
   * Text component with localization support.
   * Text content can include variable placeholders like `{{ product.price }}`.
   */
  @Serializable
  @SerialName("text")
  public data class Text(
    val text: String,
    val fontFamily: String? = null,
    val fontSize: Double = 16.0,
    val fontWeight: FontWeight = FontWeight.REGULAR,
    val color: ColorScheme? = null,
    val textAlign: TextAlign = TextAlign.START,
    val maxLines: Int? = null,
    val lineHeight: Double? = null,
    val letterSpacing: Double? = null,
    val size: Size = Size(),
    val padding: Padding = Padding.Zero,
    val margin: Margin = Margin(),
  ) : PaywallComponent

  /**
   * Image component — supports remote URLs and local asset names.
   */
  @Serializable
  @SerialName("image")
  public data class Image(
    val url: String? = null,
    val asset: String? = null,
    val fit: ImageFit = ImageFit.FIT,
    val size: Size = Size(),
    val padding: Padding = Padding.Zero,
    val margin: Margin = Margin(),
    val cornerRadius: CornerRadius? = null,
    val tintColor: ColorScheme? = null,
    val accessibilityLabel: String? = null,
  ) : PaywallComponent

  /**
   * Button — wraps child components and fires an [Action] on tap.
   */
  @Serializable
  @SerialName("button")
  public data class Button(
    val action: Action,
    val components: List<PaywallComponent> = emptyList(),
    val size: Size = Size(),
    val padding: Padding = Padding.Zero,
    val margin: Margin = Margin(),
    val backgroundColor: ColorScheme? = null,
    val cornerRadius: CornerRadius? = null,
    val border: Border? = null,
    val shadow: Shadow? = null,
  ) : PaywallComponent

  /**
   * Purchase button — triggers purchase of the selected (or specified) product.
   * Special case of Button with built-in purchase semantics.
   */
  @Serializable
  @SerialName("purchase_button")
  public data class PurchaseButton(
    val productId: String? = null,
    val components: List<PaywallComponent> = emptyList(),
    val size: Size = Size(),
    val padding: Padding = Padding.Zero,
    val margin: Margin = Margin(),
    val backgroundColor: ColorScheme? = null,
    val cornerRadius: CornerRadius? = null,
    val border: Border? = null,
  ) : PaywallComponent

  /**
   * Package selector — shows product options. Children can reference
   * the selected package's variables (price, period, trial, etc.).
   */
  @Serializable
  @SerialName("package")
  public data class Package(
    val productId: String,
    val isDefault: Boolean = false,
    val components: List<PaywallComponent> = emptyList(),
    val selectedOverride: PartialStyle? = null,
    val size: Size = Size(),
    val padding: Padding = Padding.Zero,
    val margin: Margin = Margin(),
    val backgroundColor: ColorScheme? = null,
    val cornerRadius: CornerRadius? = null,
    val border: Border? = null,
  ) : PaywallComponent

  /**
   * Spacer — flexible or fixed space between components.
   */
  @Serializable
  @SerialName("spacer")
  public data class Spacer(
    val size: Size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fill),
  ) : PaywallComponent

  /**
   * Divider — thin horizontal or vertical line.
   */
  @Serializable
  @SerialName("divider")
  public data class Divider(
    val color: ColorScheme? = null,
    val thickness: Double = 1.0,
    val margin: Margin = Margin(),
  ) : PaywallComponent

  /**
   * Close button — positioned by the layout, fires close action.
   */
  @Serializable
  @SerialName("close_button")
  public data class CloseButton(
    val size: Size = Size(width = SizeConstraint.Fixed(44.0), height = SizeConstraint.Fixed(44.0)),
    val padding: Padding = Padding.Zero,
    val margin: Margin = Margin(),
    val color: ColorScheme? = null,
  ) : PaywallComponent

  /**
   * Badge/pill — small label (e.g., "BEST VALUE", "POPULAR").
   */
  @Serializable
  @SerialName("badge")
  public data class Badge(
    val text: String,
    val fontSize: Double = 12.0,
    val fontWeight: FontWeight = FontWeight.BOLD,
    val textColor: ColorScheme? = null,
    val backgroundColor: ColorScheme? = null,
    val cornerRadius: CornerRadius = CornerRadius.Uniform(100.0),
    val padding: Padding = Padding.symmetric(horizontal = 12.0, vertical = 4.0),
    val margin: Margin = Margin(),
  ) : PaywallComponent

  /**
   * Icon — renders a named icon from Material Icons or a custom icon URL.
   */
  @Serializable
  @SerialName("icon")
  public data class Icon(
    val name: String? = null,
    val url: String? = null,
    val color: ColorScheme? = null,
    val size: Double = 24.0,
    val padding: Padding = Padding.Zero,
    val margin: Margin = Margin(),
  ) : PaywallComponent
}

/**
 * Layout dimension — determines how a Stack arranges its children.
 */
@Serializable
public enum class Dimension {
  @SerialName("vertical") VERTICAL,
  @SerialName("horizontal") HORIZONTAL,
  @SerialName("z_layer") Z_LAYER,
}

/**
 * Partial style overrides — used for state-dependent styling
 * (e.g., selected package highlight).
 */
@Serializable
public data class PartialStyle(
  val backgroundColor: ColorScheme? = null,
  val border: Border? = null,
  val shadow: Shadow? = null,
  val cornerRadius: CornerRadius? = null,
)

/**
 * Root paywall layout definition sent from the server.
 * Contains the component tree + metadata for rendering.
 */
@Serializable
public data class PaywallComponentsConfig(
  val components: List<PaywallComponent>,
  val defaultLocale: String = "en",
  val localizations: Map<String, Map<String, String>> = emptyMap(),
  val variables: Map<String, String> = emptyMap(),
)
