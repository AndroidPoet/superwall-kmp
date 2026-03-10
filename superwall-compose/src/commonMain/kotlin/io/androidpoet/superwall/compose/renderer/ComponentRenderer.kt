package io.androidpoet.superwall.compose.renderer

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.androidpoet.superwall.models.components.ColorScheme
import io.androidpoet.superwall.models.components.CornerRadius
import io.androidpoet.superwall.models.components.CrossAlignment
import io.androidpoet.superwall.models.components.Dimension
import io.androidpoet.superwall.models.components.FlexAlignment
import io.androidpoet.superwall.models.components.Margin
import io.androidpoet.superwall.models.components.OverflowBehavior
import io.androidpoet.superwall.models.components.Padding
import io.androidpoet.superwall.models.components.PaywallComponent
import io.androidpoet.superwall.models.components.SizeConstraint

/**
 * Renders a [PaywallComponent] tree into native Compose UI.
 *
 * Follows the extension-function dispatch pattern from skydoves/server-driven-compose:
 * each component type maps to a dedicated render function with consistent
 * spacing, corner radius, and interaction handling across all platforms.
 */
@Composable
public fun RenderComponent(
  component: PaywallComponent,
  state: PaywallRenderState,
  onAction: (io.androidpoet.superwall.models.components.Action) -> Unit,
) {
  when (component) {
    is PaywallComponent.Stack -> RenderStack(component, state, onAction)
    is PaywallComponent.Text -> RenderText(component, state)
    is PaywallComponent.Image -> RenderImage(component, state)
    is PaywallComponent.Button -> RenderButton(component, state, onAction)
    is PaywallComponent.PurchaseButton -> RenderPurchaseButton(component, state, onAction)
    is PaywallComponent.Package -> RenderPackage(component, state, onAction)
    is PaywallComponent.Spacer -> RenderSpacer(component)
    is PaywallComponent.Divider -> RenderDivider(component, state)
    is PaywallComponent.CloseButton -> RenderCloseButton(component, state, onAction)
    is PaywallComponent.Badge -> RenderBadge(component, state)
    is PaywallComponent.Icon -> RenderIcon(component, state)
  }
}

// ── Stack ────────────────────────────────────────────────────────────

@Composable
private fun RenderStack(
  stack: PaywallComponent.Stack,
  state: PaywallRenderState,
  onAction: (io.androidpoet.superwall.models.components.Action) -> Unit,
) {
  val shape = stack.cornerRadius.toShape()
  val modifier = Modifier
    .applySize(stack.size)
    .applyMargin(stack.margin)
    .applyShadow(stack.shadow, state.isDarkMode, shape)
    .clip(shape)
    .applyBackground(stack.backgroundColor, state.isDarkMode)
    .applyBorder(stack.border, state.isDarkMode, shape)
    .applyPadding(stack.padding)
    .applyOverflow(stack.overflow)

  when (stack.dimension) {
    Dimension.VERTICAL -> {
      Column(
        modifier = modifier,
        verticalArrangement = stack.alignment.toVerticalArrangement(stack.spacing),
        horizontalAlignment = stack.crossAlignment.toHorizontalAlignment(),
      ) {
        stack.components.forEach { child ->
          RenderComponent(child, state, onAction)
        }
      }
    }
    Dimension.HORIZONTAL -> {
      Row(
        modifier = modifier,
        horizontalArrangement = stack.alignment.toHorizontalArrangement(stack.spacing),
        verticalAlignment = stack.crossAlignment.toVerticalAlignment(),
      ) {
        stack.components.forEach { child ->
          RenderComponent(child, state, onAction)
        }
      }
    }
    Dimension.Z_LAYER -> {
      Box(modifier = modifier) {
        stack.components.forEach { child ->
          RenderComponent(child, state, onAction)
        }
      }
    }
  }
}

// ── Text ─────────────────────────────────────────────────────────────

@Composable
private fun RenderText(
  text: PaywallComponent.Text,
  state: PaywallRenderState,
) {
  val resolved = state.resolveText(text.text)
  val color = text.color.resolveColor(state.isDarkMode)
    ?: MaterialTheme.colorScheme.onSurface

  Text(
    text = resolved,
    modifier = Modifier
      .applySize(text.size)
      .applyMargin(text.margin)
      .applyPadding(text.padding),
    color = color,
    fontSize = text.fontSize.sp,
    fontWeight = text.fontWeight.toCompose(),
    textAlign = text.textAlign.toCompose(),
    maxLines = text.maxLines ?: Int.MAX_VALUE,
    overflow = TextOverflow.Ellipsis,
    lineHeight = text.lineHeight?.sp ?: (text.fontSize * 1.5).sp,
    letterSpacing = text.letterSpacing?.sp ?: 0.sp,
  )
}

// ── Image ────────────────────────────────────────────────────────────

@Composable
private fun RenderImage(
  image: PaywallComponent.Image,
  state: PaywallRenderState,
) {
  val shape = image.cornerRadius.toShape()
  Box(
    modifier = Modifier
      .applySize(image.size)
      .applyMargin(image.margin)
      .clip(shape)
      .applyPadding(image.padding)
      .background(MaterialTheme.colorScheme.surfaceVariant),
    contentAlignment = Alignment.Center,
  ) {
    state.imageLoader?.invoke(image.url ?: image.asset ?: "", image.fit, image.accessibilityLabel)
  }
}

// ── Button ───────────────────────────────────────────────────────────

@Composable
private fun RenderButton(
  button: PaywallComponent.Button,
  state: PaywallRenderState,
  onAction: (io.androidpoet.superwall.models.components.Action) -> Unit,
) {
  val shape = button.cornerRadius.toShape()
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val pressAlpha = if (isPressed) 0.85f else 1f

  Box(
    modifier = Modifier
      .applySize(button.size)
      .applyMargin(button.margin)
      .applyShadow(button.shadow, state.isDarkMode, shape)
      .clip(shape)
      .applyBackground(button.backgroundColor, state.isDarkMode, pressAlpha)
      .applyBorder(button.border, state.isDarkMode, shape)
      .clickable(interactionSource = interactionSource, indication = null) {
        onAction(button.action)
      }
      .applyPadding(button.padding),
    contentAlignment = Alignment.Center,
  ) {
    button.components.forEach { child ->
      RenderComponent(child, state, onAction)
    }
  }
}

// ── Purchase Button ──────────────────────────────────────────────────

@Composable
private fun RenderPurchaseButton(
  button: PaywallComponent.PurchaseButton,
  state: PaywallRenderState,
  onAction: (io.androidpoet.superwall.models.components.Action) -> Unit,
) {
  val shape = button.cornerRadius.toShape()
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val pressAlpha = if (isPressed) 0.85f else 1f

  Box(
    modifier = Modifier
      .applySize(button.size)
      .applyMargin(button.margin)
      .applyShadow(null, state.isDarkMode, shape) // Purchase buttons get subtle built-in shadow
      .clip(shape)
      .applyBackground(button.backgroundColor, state.isDarkMode, pressAlpha)
      .applyBorder(button.border, state.isDarkMode, shape)
      .clickable(interactionSource = interactionSource, indication = null) {
        onAction(
          io.androidpoet.superwall.models.components.Action.Purchase(
            button.productId ?: state.selectedProductId,
          ),
        )
      }
      .applyPadding(button.padding),
    contentAlignment = Alignment.Center,
  ) {
    button.components.forEach { child ->
      RenderComponent(child, state, onAction)
    }
  }
}

// ── Package ──────────────────────────────────────────────────────────

@Composable
private fun RenderPackage(
  pkg: PaywallComponent.Package,
  state: PaywallRenderState,
  onAction: (io.androidpoet.superwall.models.components.Action) -> Unit,
) {
  val isSelected = state.selectedProductId == pkg.productId
  val override = if (isSelected) pkg.selectedOverride else null

  val bgColor = override?.backgroundColor ?: pkg.backgroundColor
  val borderStyle = override?.border ?: pkg.border
  val radius = override?.cornerRadius ?: pkg.cornerRadius
  val shape = radius.toShape()

  // Animate border width for smooth selection transitions
  val borderWidth by animateDpAsState(
    targetValue = (borderStyle?.width ?: 0.0).dp,
    animationSpec = tween(200),
  )
  val borderColor by animateColorAsState(
    targetValue = borderStyle?.color.resolveColor(state.isDarkMode) ?: Color.Transparent,
    animationSpec = tween(200),
  )

  val packageState = state.withProduct(pkg.productId)

  Box(
    modifier = Modifier
      .applySize(pkg.size)
      .applyMargin(pkg.margin)
      .applyShadow(override?.shadow, state.isDarkMode, shape)
      .clip(shape)
      .applyBackground(bgColor, state.isDarkMode)
      .then(
        if (borderWidth > 0.dp) Modifier.border(borderWidth, borderColor, shape)
        else Modifier,
      )
      .clickable { state.onProductSelected(pkg.productId) }
      .applyPadding(pkg.padding),
  ) {
    pkg.components.forEach { child ->
      RenderComponent(child, packageState, onAction)
    }
  }
}

// ── Spacer ───────────────────────────────────────────────────────────

@Composable
private fun RenderSpacer(spacer: PaywallComponent.Spacer) {
  Spacer(modifier = Modifier.applySize(spacer.size))
}

// ── Divider ──────────────────────────────────────────────────────────

@Composable
private fun RenderDivider(
  divider: PaywallComponent.Divider,
  state: PaywallRenderState,
) {
  HorizontalDivider(
    modifier = Modifier
      .fillMaxWidth()
      .applyMargin(divider.margin),
    thickness = divider.thickness.dp,
    color = divider.color.resolveColor(state.isDarkMode)
      ?: MaterialTheme.colorScheme.outlineVariant,
  )
}

// ── Close Button ─────────────────────────────────────────────────────

@Composable
private fun RenderCloseButton(
  closeButton: PaywallComponent.CloseButton,
  state: PaywallRenderState,
  onAction: (io.androidpoet.superwall.models.components.Action) -> Unit,
) {
  val color = closeButton.color.resolveColor(state.isDarkMode)
    ?: MaterialTheme.colorScheme.onSurfaceVariant

  val interactionSource = remember { MutableInteractionSource() }
  val isHovered by interactionSource.collectIsHoveredAsState()

  Box(
    modifier = Modifier
      .applySize(closeButton.size)
      .applyMargin(closeButton.margin)
      .clip(RoundedCornerShape(50))
      .then(
        if (isHovered) Modifier.background(Color.Black.copy(alpha = 0.06f))
        else Modifier,
      )
      .clickable(interactionSource = interactionSource, indication = null) {
        onAction(io.androidpoet.superwall.models.components.Action.Close)
      }
      .applyPadding(closeButton.padding),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = "\u2715",
      color = color,
      fontSize = 18.sp,
      fontWeight = FontWeight.Normal,
    )
  }
}

// ── Badge ────────────────────────────────────────────────────────────

@Composable
private fun RenderBadge(
  badge: PaywallComponent.Badge,
  state: PaywallRenderState,
) {
  val resolved = state.resolveText(badge.text)
  val shape = badge.cornerRadius.toShape()

  Text(
    text = resolved,
    modifier = Modifier
      .applyMargin(badge.margin)
      .clip(shape)
      .applyBackground(badge.backgroundColor, state.isDarkMode)
      .applyPadding(badge.padding),
    color = badge.textColor.resolveColor(state.isDarkMode)
      ?: MaterialTheme.colorScheme.onPrimary,
    fontSize = badge.fontSize.sp,
    fontWeight = badge.fontWeight.toCompose(),
    letterSpacing = 0.5.sp,
  )
}

// ── Icon ─────────────────────────────────────────────────────────────

@Composable
private fun RenderIcon(
  icon: PaywallComponent.Icon,
  state: PaywallRenderState,
) {
  val color = icon.color.resolveColor(state.isDarkMode)
    ?: MaterialTheme.colorScheme.onSurface

  Box(
    modifier = Modifier
      .size(icon.size.dp)
      .applyMargin(icon.margin)
      .applyPadding(icon.padding),
    contentAlignment = Alignment.Center,
  ) {
    state.iconLoader?.invoke(icon.name ?: "", icon.url, color, icon.size)
      ?: Text(text = "\u2022", color = color, fontSize = icon.size.sp)
  }
}

// ── Modifier Extensions ──────────────────────────────────────────────

private fun Modifier.applySize(size: io.androidpoet.superwall.models.components.Size): Modifier {
  var m = this
  m = when (size.width) {
    is SizeConstraint.Fill -> m.fillMaxWidth()
    is SizeConstraint.Fixed -> m.width((size.width as SizeConstraint.Fixed).value.dp)
    is SizeConstraint.Fit -> m
  }
  m = when (size.height) {
    is SizeConstraint.Fill -> m.fillMaxHeight()
    is SizeConstraint.Fixed -> m.height((size.height as SizeConstraint.Fixed).value.dp)
    is SizeConstraint.Fit -> m.wrapContentHeight()
  }
  return m
}

private fun Modifier.applyPadding(padding: Padding): Modifier =
  this.padding(
    start = padding.leading.dp,
    end = padding.trailing.dp,
    top = padding.top.dp,
    bottom = padding.bottom.dp,
  )

private fun Modifier.applyMargin(margin: Margin): Modifier =
  this.padding(
    start = margin.leading.dp,
    end = margin.trailing.dp,
    top = margin.top.dp,
    bottom = margin.bottom.dp,
  )

private fun Modifier.applyBackground(
  color: ColorScheme?,
  isDark: Boolean,
  alpha: Float = 1f,
): Modifier {
  val resolved = color.resolveColor(isDark) ?: return this
  return this.background(if (alpha < 1f) resolved.copy(alpha = resolved.alpha * alpha) else resolved)
}

private fun Modifier.applyBorder(
  border: io.androidpoet.superwall.models.components.Border?,
  isDark: Boolean,
  shape: Shape = RoundedCornerShape(0.dp),
): Modifier {
  if (border == null) return this
  val color = border.color.resolveColor(isDark) ?: return this
  return this.border(border.width.dp, color, shape)
}

private fun Modifier.applyShadow(
  shadow: io.androidpoet.superwall.models.components.Shadow?,
  isDark: Boolean,
  shape: Shape = RoundedCornerShape(0.dp),
): Modifier {
  if (shadow == null) return this
  shadow.color.resolveColor(isDark) ?: return this
  return this.shadow(shadow.radius.dp, shape, clip = false)
}

@Composable
private fun Modifier.applyOverflow(overflow: OverflowBehavior): Modifier =
  when (overflow) {
    OverflowBehavior.SCROLL -> this.verticalScroll(rememberScrollState())
    OverflowBehavior.CLIP -> this
    OverflowBehavior.VISIBLE -> this
  }

// ── Type Conversions ─────────────────────────────────────────────────

internal fun ColorScheme?.resolveColor(isDark: Boolean): Color? {
  if (this == null) return null
  val theme = if (isDark) (dark ?: light) else light
  return parseHexColor(theme.hex, theme.alpha)
}

private fun parseHexColor(hex: String, alpha: Double): Color {
  val cleaned = hex.removePrefix("#")
  val rgb = cleaned.toLong(16)
  return when (cleaned.length) {
    6 -> Color(
      red = ((rgb shr 16) and 0xFF).toInt(),
      green = ((rgb shr 8) and 0xFF).toInt(),
      blue = (rgb and 0xFF).toInt(),
      alpha = (alpha * 255).toInt(),
    )
    8 -> Color(rgb.toInt())
    else -> Color.Unspecified
  }
}

private fun CornerRadius?.toShape(): Shape {
  if (this == null) return RoundedCornerShape(0.dp)
  return when (this) {
    is CornerRadius.Uniform -> RoundedCornerShape(radius.dp)
    is CornerRadius.Individual -> RoundedCornerShape(
      topStart = topLeading.dp,
      topEnd = topTrailing.dp,
      bottomStart = bottomLeading.dp,
      bottomEnd = bottomTrailing.dp,
    )
  }
}

private fun io.androidpoet.superwall.models.components.FontWeight.toCompose(): FontWeight =
  when (this) {
    io.androidpoet.superwall.models.components.FontWeight.THIN -> FontWeight.Thin
    io.androidpoet.superwall.models.components.FontWeight.EXTRA_LIGHT -> FontWeight.ExtraLight
    io.androidpoet.superwall.models.components.FontWeight.LIGHT -> FontWeight.Light
    io.androidpoet.superwall.models.components.FontWeight.REGULAR -> FontWeight.Normal
    io.androidpoet.superwall.models.components.FontWeight.MEDIUM -> FontWeight.Medium
    io.androidpoet.superwall.models.components.FontWeight.SEMI_BOLD -> FontWeight.SemiBold
    io.androidpoet.superwall.models.components.FontWeight.BOLD -> FontWeight.Bold
    io.androidpoet.superwall.models.components.FontWeight.EXTRA_BOLD -> FontWeight.ExtraBold
    io.androidpoet.superwall.models.components.FontWeight.BLACK -> FontWeight.Black
  }

private fun io.androidpoet.superwall.models.components.TextAlign.toCompose(): TextAlign =
  when (this) {
    io.androidpoet.superwall.models.components.TextAlign.START -> TextAlign.Start
    io.androidpoet.superwall.models.components.TextAlign.CENTER -> TextAlign.Center
    io.androidpoet.superwall.models.components.TextAlign.END -> TextAlign.End
  }

private fun FlexAlignment.toVerticalArrangement(spacing: Double): Arrangement.Vertical =
  when (this) {
    FlexAlignment.START -> if (spacing > 0) Arrangement.spacedBy(spacing.dp) else Arrangement.Top
    FlexAlignment.CENTER -> Arrangement.Center
    FlexAlignment.END -> Arrangement.Bottom
    FlexAlignment.SPACE_BETWEEN -> Arrangement.SpaceBetween
    FlexAlignment.SPACE_AROUND -> Arrangement.SpaceAround
    FlexAlignment.SPACE_EVENLY -> Arrangement.SpaceEvenly
  }

private fun FlexAlignment.toHorizontalArrangement(spacing: Double): Arrangement.Horizontal =
  when (this) {
    FlexAlignment.START -> if (spacing > 0) Arrangement.spacedBy(spacing.dp) else Arrangement.Start
    FlexAlignment.CENTER -> Arrangement.Center
    FlexAlignment.END -> Arrangement.End
    FlexAlignment.SPACE_BETWEEN -> Arrangement.SpaceBetween
    FlexAlignment.SPACE_AROUND -> Arrangement.SpaceAround
    FlexAlignment.SPACE_EVENLY -> Arrangement.SpaceEvenly
  }

private fun CrossAlignment.toHorizontalAlignment(): Alignment.Horizontal =
  when (this) {
    CrossAlignment.START -> Alignment.Start
    CrossAlignment.CENTER -> Alignment.CenterHorizontally
    CrossAlignment.END -> Alignment.End
    CrossAlignment.STRETCH -> Alignment.Start
  }

private fun CrossAlignment.toVerticalAlignment(): Alignment.Vertical =
  when (this) {
    CrossAlignment.START -> Alignment.Top
    CrossAlignment.CENTER -> Alignment.CenterVertically
    CrossAlignment.END -> Alignment.Bottom
    CrossAlignment.STRETCH -> Alignment.Top
  }
