package io.androidpoet.superwall.models.components

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Color represented as a hex string with optional alpha.
 * Supports light/dark color schemes.
 */
@Serializable
public data class ColorScheme(
  val light: ThemeColor,
  val dark: ThemeColor? = null,
)

@Serializable
public data class ThemeColor(
  val hex: String,
  val alpha: Double = 1.0,
)

/**
 * Padding for all four edges.
 */
@Serializable
public data class Padding(
  val top: Double = 0.0,
  val bottom: Double = 0.0,
  val leading: Double = 0.0,
  val trailing: Double = 0.0,
) {
  public companion object {
    public val Zero: Padding = Padding()
    public fun all(value: Double): Padding = Padding(value, value, value, value)
    public fun symmetric(horizontal: Double = 0.0, vertical: Double = 0.0): Padding =
      Padding(top = vertical, bottom = vertical, leading = horizontal, trailing = horizontal)
  }
}

/**
 * Margin (spacing outside the component).
 */
@Serializable
public data class Margin(
  val top: Double = 0.0,
  val bottom: Double = 0.0,
  val leading: Double = 0.0,
  val trailing: Double = 0.0,
)

/**
 * Border styling.
 */
@Serializable
public data class Border(
  val color: ColorScheme,
  val width: Double = 1.0,
)

/**
 * Shadow styling.
 */
@Serializable
public data class Shadow(
  val color: ColorScheme,
  val radius: Double = 4.0,
  val x: Double = 0.0,
  val y: Double = 2.0,
)

/**
 * Corner radius — uniform or per-corner.
 */
@Serializable
public sealed interface CornerRadius {
  @Serializable
  @SerialName("uniform")
  public data class Uniform(val radius: Double) : CornerRadius

  @Serializable
  @SerialName("individual")
  public data class Individual(
    val topLeading: Double = 0.0,
    val topTrailing: Double = 0.0,
    val bottomLeading: Double = 0.0,
    val bottomTrailing: Double = 0.0,
  ) : CornerRadius
}

/**
 * Size constraint for width or height.
 */
@Serializable
public sealed interface SizeConstraint {
  /** Fill all available space. */
  @Serializable
  @SerialName("fill")
  public data object Fill : SizeConstraint

  /** Fit to content size. */
  @Serializable
  @SerialName("fit")
  public data object Fit : SizeConstraint

  /** Fixed size in dp. */
  @Serializable
  @SerialName("fixed")
  public data class Fixed(val value: Double) : SizeConstraint
}

/**
 * Size specification with width and height constraints.
 */
@Serializable
public data class Size(
  val width: SizeConstraint = SizeConstraint.Fit,
  val height: SizeConstraint = SizeConstraint.Fit,
)

/**
 * How children are aligned within a layout.
 */
@Serializable
public enum class FlexAlignment {
  @SerialName("start") START,
  @SerialName("center") CENTER,
  @SerialName("end") END,
  @SerialName("space_between") SPACE_BETWEEN,
  @SerialName("space_around") SPACE_AROUND,
  @SerialName("space_evenly") SPACE_EVENLY,
}

/**
 * Cross-axis alignment for layout children.
 */
@Serializable
public enum class CrossAlignment {
  @SerialName("start") START,
  @SerialName("center") CENTER,
  @SerialName("end") END,
  @SerialName("stretch") STRETCH,
}

/**
 * Text alignment.
 */
@Serializable
public enum class TextAlign {
  @SerialName("start") START,
  @SerialName("center") CENTER,
  @SerialName("end") END,
}

/**
 * Font weight matching Material/system conventions.
 */
@Serializable
public enum class FontWeight {
  @SerialName("thin") THIN,
  @SerialName("extra_light") EXTRA_LIGHT,
  @SerialName("light") LIGHT,
  @SerialName("regular") REGULAR,
  @SerialName("medium") MEDIUM,
  @SerialName("semi_bold") SEMI_BOLD,
  @SerialName("bold") BOLD,
  @SerialName("extra_bold") EXTRA_BOLD,
  @SerialName("black") BLACK,
}

/**
 * Overflow behavior when content exceeds bounds.
 */
@Serializable
public enum class OverflowBehavior {
  @SerialName("clip") CLIP,
  @SerialName("visible") VISIBLE,
  @SerialName("scroll") SCROLL,
}

/**
 * Image scaling mode.
 */
@Serializable
public enum class ImageFit {
  @SerialName("fill") FILL,
  @SerialName("fit") FIT,
  @SerialName("crop") CROP,
}

/**
 * Shape for clipping.
 */
@Serializable
public sealed interface Shape {
  @Serializable
  @SerialName("rectangle")
  public data class Rectangle(val cornerRadius: CornerRadius = CornerRadius.Uniform(0.0)) : Shape

  @Serializable
  @SerialName("circle")
  public data object Circle : Shape

  @Serializable
  @SerialName("pill")
  public data object Pill : Shape
}
