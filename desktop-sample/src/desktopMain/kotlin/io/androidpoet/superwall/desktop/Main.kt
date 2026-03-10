package io.androidpoet.superwall.desktop

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.androidpoet.superwall.compose.renderer.NativePaywall
import io.androidpoet.superwall.models.PeriodUnit
import io.androidpoet.superwall.models.StoreProduct
import io.androidpoet.superwall.models.components.Action
import io.androidpoet.superwall.models.components.Border
import io.androidpoet.superwall.models.components.ColorScheme
import io.androidpoet.superwall.models.components.CornerRadius
import io.androidpoet.superwall.models.components.CrossAlignment
import io.androidpoet.superwall.models.components.Dimension
import io.androidpoet.superwall.models.components.FlexAlignment
import io.androidpoet.superwall.models.components.FontWeight
import io.androidpoet.superwall.models.components.Margin
import io.androidpoet.superwall.models.components.OverflowBehavior
import io.androidpoet.superwall.models.components.Padding
import io.androidpoet.superwall.models.components.PartialStyle
import io.androidpoet.superwall.models.components.PaywallComponent
import io.androidpoet.superwall.models.components.PaywallComponentsConfig
import io.androidpoet.superwall.models.components.Shadow
import io.androidpoet.superwall.models.components.Size
import io.androidpoet.superwall.models.components.SizeConstraint
import io.androidpoet.superwall.models.components.TextAlign
import io.androidpoet.superwall.models.components.ThemeColor

fun main() = application {
  val windowState = rememberWindowState(
    size = DpSize(440.dp, 860.dp),
    position = WindowPosition(Alignment.Center),
  )

  Window(
    onCloseRequest = ::exitApplication,
    state = windowState,
    title = "Superwall \u2014 Native Paywall Preview",
    resizable = true,
  ) {
    MaterialTheme(
      colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
    ) {
      Surface(modifier = Modifier.fillMaxSize()) {
        var currentPaywall by remember { mutableStateOf("premium") }
        var lastAction by remember { mutableStateOf<String?>(null) }

        Column(modifier = Modifier.fillMaxSize()) {
          // ── Toolbar ──
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(MaterialTheme.colorScheme.surfaceContainerLow)
              .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            listOf("premium" to "Premium", "minimal" to "Minimal", "feature" to "Feature").forEach { (key, label) ->
              if (currentPaywall == key) {
                FilledTonalButton(onClick = {}) {
                  Text(label, fontSize = 13.sp)
                }
              } else {
                ElevatedButton(onClick = { currentPaywall = key }) {
                  Text(label, fontSize = 13.sp)
                }
              }
            }
            Spacer(Modifier.weight(1f))
            lastAction?.let {
              Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
              )
            }
          }

          // ── Paywall Area ──
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(Color(0xFFF8F9FA))
              .padding(20.dp),
            contentAlignment = Alignment.TopCenter,
          ) {
            // Phone-like frame
            Surface(
              modifier = Modifier.width(390.dp),
              shape = RoundedCornerShape(24.dp),
              shadowElevation = 8.dp,
              tonalElevation = 0.dp,
              color = Color.White,
            ) {
              Box(modifier = Modifier.clip(RoundedCornerShape(24.dp))) {
                Crossfade(
                  targetState = currentPaywall,
                  animationSpec = tween(300),
                ) { paywall ->
                  val config = when (paywall) {
                    "premium" -> premiumPaywallConfig()
                    "minimal" -> minimalPaywallConfig()
                    "feature" -> featureGateConfig()
                    else -> premiumPaywallConfig()
                  }

                  NativePaywall(
                    config = config,
                    products = sampleProducts(),
                    defaultProductId = "yearly",
                    onAction = { action ->
                      lastAction = when (action) {
                        is Action.Close -> "\u2715 Close"
                        is Action.Purchase -> "\uD83D\uDCB3 Purchase(${action.productId})"
                        is Action.Restore -> "\u21BA Restore"
                        is Action.OpenUrl -> "\u2197 URL"
                        is Action.DeepLink -> "\u2197 DeepLink"
                        is Action.Navigate -> "\u2192 ${action.destination}"
                        is Action.Custom -> "\u2699 ${action.name}"
                      }
                    },
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

// ── Sample Products ──────────────────────────────────────────────────

private fun sampleProducts(): Map<String, StoreProduct> = mapOf(
  "weekly" to StoreProduct(
    id = "weekly",
    name = "Weekly",
    description = "Billed weekly",
    price = 4.99,
    currencyCode = "USD",
    localizedPrice = "$4.99",
    periodUnit = PeriodUnit.WEEK,
    periodValue = 1,
    trialPeriodDays = 3,
  ),
  "monthly" to StoreProduct(
    id = "monthly",
    name = "Monthly",
    description = "Billed monthly",
    price = 9.99,
    currencyCode = "USD",
    localizedPrice = "$9.99",
    periodUnit = PeriodUnit.MONTH,
    periodValue = 1,
    trialPeriodDays = 7,
  ),
  "yearly" to StoreProduct(
    id = "yearly",
    name = "Yearly",
    description = "Billed annually",
    price = 49.99,
    currencyCode = "USD",
    localizedPrice = "$49.99",
    periodUnit = PeriodUnit.YEAR,
    periodValue = 1,
    trialPeriodDays = 14,
  ),
)

// ── Colors ───────────────────────────────────────────────────────────

private val white = ColorScheme(light = ThemeColor("#FFFFFF"))
private val black = ColorScheme(light = ThemeColor("#1A1A2E"))
private val gray500 = ColorScheme(light = ThemeColor("#6B7280"))
private val gray400 = ColorScheme(light = ThemeColor("#9CA3AF"))
private val gray100 = ColorScheme(light = ThemeColor("#F3F4F6"))
private val gray50 = ColorScheme(light = ThemeColor("#F9FAFB"))
private val blue600 = ColorScheme(light = ThemeColor("#2563EB"))
private val blue500 = ColorScheme(light = ThemeColor("#3B82F6"))
private val blue50 = ColorScheme(light = ThemeColor("#EFF6FF"))
private val green500 = ColorScheme(light = ThemeColor("#22C55E"))
private val purple600 = ColorScheme(light = ThemeColor("#7C3AED"))
private val purple50 = ColorScheme(light = ThemeColor("#F5F3FF"))
private val amber500 = ColorScheme(light = ThemeColor("#F59E0B"))
private val transparent = ColorScheme(light = ThemeColor("#000000", alpha = 0.0))

// ── Premium Paywall ──────────────────────────────────────────────────

private fun premiumPaywallConfig() = PaywallComponentsConfig(
  components = listOf(
    // Root: scrollable vertical stack
    PaywallComponent.Stack(
      dimension = Dimension.VERTICAL,
      size = Size(width = SizeConstraint.Fill),
      backgroundColor = white,
      overflow = OverflowBehavior.SCROLL,
      spacing = 0.0,
      components = listOf(
        // ── Gradient header ──
        PaywallComponent.Stack(
          dimension = Dimension.Z_LAYER,
          size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fixed(220.0)),
          backgroundColor = blue600,
          padding = Padding(top = 16.0, bottom = 24.0, leading = 24.0, trailing = 24.0),
          components = listOf(
            // Close button top-right
            PaywallComponent.Stack(
              dimension = Dimension.HORIZONTAL,
              size = Size(width = SizeConstraint.Fill),
              alignment = FlexAlignment.END,
              components = listOf(
                PaywallComponent.CloseButton(
                  color = ColorScheme(light = ThemeColor("#FFFFFF", alpha = 0.8)),
                  size = Size(width = SizeConstraint.Fixed(36.0), height = SizeConstraint.Fixed(36.0)),
                ),
              ),
            ),
            // Centered title group
            PaywallComponent.Stack(
              dimension = Dimension.VERTICAL,
              size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fill),
              alignment = FlexAlignment.CENTER,
              crossAlignment = CrossAlignment.CENTER,
              spacing = 10.0,
              components = listOf(
                PaywallComponent.Text(
                  text = "\u2728",
                  fontSize = 40.0,
                ),
                PaywallComponent.Text(
                  text = "Unlock Premium",
                  fontSize = 28.0,
                  fontWeight = FontWeight.BOLD,
                  color = white,
                  textAlign = TextAlign.CENTER,
                ),
                PaywallComponent.Text(
                  text = "Get unlimited access to every feature",
                  fontSize = 15.0,
                  color = ColorScheme(light = ThemeColor("#FFFFFF", alpha = 0.75)),
                  textAlign = TextAlign.CENTER,
                ),
              ),
            ),
          ),
        ),

        // ── Features ──
        PaywallComponent.Stack(
          dimension = Dimension.VERTICAL,
          size = Size(width = SizeConstraint.Fill),
          padding = Padding(top = 28.0, bottom = 8.0, leading = 28.0, trailing = 28.0),
          spacing = 16.0,
          components = listOf(
            featureRow("\u2705", "Unlimited paywalls & placements"),
            featureRow("\uD83E\uDDEA", "A/B testing with real-time results"),
            featureRow("\uD83D\uDCCA", "Advanced analytics dashboard"),
            featureRow("\u26A1", "Priority support & onboarding"),
          ),
        ),

        // ── Divider ──
        PaywallComponent.Divider(
          color = gray100,
          thickness = 1.0,
          margin = Margin(top = 12.0, bottom = 4.0, leading = 28.0, trailing = 28.0),
        ),

        // ── Package selector ──
        PaywallComponent.Stack(
          dimension = Dimension.VERTICAL,
          size = Size(width = SizeConstraint.Fill),
          padding = Padding(top = 16.0, leading = 20.0, trailing = 20.0),
          spacing = 10.0,
          components = listOf(
            packageCard("yearly", "Annual", "{{ product.price }}/{{ product.period }}", "SAVE 58%", true),
            packageCard("monthly", "Monthly", "{{ product.price }}/{{ product.period }}", null, false),
            packageCard("weekly", "Weekly", "{{ product.price }}/{{ product.period }}", null, false),
          ),
        ),

        // ── CTA ──
        PaywallComponent.Stack(
          dimension = Dimension.VERTICAL,
          size = Size(width = SizeConstraint.Fill),
          padding = Padding(top = 20.0, bottom = 8.0, leading = 20.0, trailing = 20.0),
          spacing = 10.0,
          crossAlignment = CrossAlignment.CENTER,
          components = listOf(
            PaywallComponent.PurchaseButton(
              size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fixed(54.0)),
              backgroundColor = blue600,
              cornerRadius = CornerRadius.Uniform(14.0),
              components = listOf(
                PaywallComponent.Text(
                  text = "Start Free Trial",
                  fontSize = 17.0,
                  fontWeight = FontWeight.SEMI_BOLD,
                  color = white,
                  textAlign = TextAlign.CENTER,
                ),
              ),
            ),
            PaywallComponent.Text(
              text = "{{ product.trial_days }}-day free trial \u2022 Cancel anytime",
              fontSize = 12.0,
              color = gray400,
              textAlign = TextAlign.CENTER,
              padding = Padding(top = 2.0),
            ),
          ),
        ),

        // ── Restore ──
        PaywallComponent.Button(
          action = Action.Restore,
          size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fixed(44.0)),
          backgroundColor = transparent,
          padding = Padding(bottom = 16.0),
          components = listOf(
            PaywallComponent.Text(
              text = "Restore Purchases",
              fontSize = 13.0,
              color = gray400,
              textAlign = TextAlign.CENTER,
            ),
          ),
        ),
      ),
    ),
  ),
)

// ── Minimal Paywall ──────────────────────────────────────────────────

private fun minimalPaywallConfig() = PaywallComponentsConfig(
  components = listOf(
    PaywallComponent.Stack(
      dimension = Dimension.VERTICAL,
      size = Size(width = SizeConstraint.Fill),
      backgroundColor = white,
      overflow = OverflowBehavior.SCROLL,
      padding = Padding(top = 16.0, bottom = 32.0, leading = 32.0, trailing = 32.0),
      spacing = 0.0,
      components = listOf(
        // Close
        PaywallComponent.Stack(
          dimension = Dimension.HORIZONTAL,
          size = Size(width = SizeConstraint.Fill),
          alignment = FlexAlignment.END,
          components = listOf(PaywallComponent.CloseButton()),
        ),

        PaywallComponent.Spacer(size = Size(height = SizeConstraint.Fixed(48.0))),

        // Icon + Title
        PaywallComponent.Stack(
          dimension = Dimension.VERTICAL,
          size = Size(width = SizeConstraint.Fill),
          crossAlignment = CrossAlignment.CENTER,
          spacing = 16.0,
          components = listOf(
            PaywallComponent.Text(text = "\uD83D\uDE80", fontSize = 52.0),
            PaywallComponent.Text(
              text = "Go Pro",
              fontSize = 34.0,
              fontWeight = FontWeight.BLACK,
              color = black,
              textAlign = TextAlign.CENTER,
            ),
            PaywallComponent.Text(
              text = "Remove all limits.\nUnlock everything.",
              fontSize = 17.0,
              color = gray500,
              textAlign = TextAlign.CENTER,
              lineHeight = 26.0,
            ),
          ),
        ),

        PaywallComponent.Spacer(size = Size(height = SizeConstraint.Fixed(36.0))),

        // Price card
        PaywallComponent.Stack(
          dimension = Dimension.VERTICAL,
          size = Size(width = SizeConstraint.Fill),
          backgroundColor = gray50,
          cornerRadius = CornerRadius.Uniform(20.0),
          padding = Padding(top = 28.0, bottom = 28.0, leading = 24.0, trailing = 24.0),
          spacing = 6.0,
          crossAlignment = CrossAlignment.CENTER,
          border = Border(color = gray100, width = 1.0),
          components = listOf(
            PaywallComponent.Text(
              text = "{{ product.price }}",
              fontSize = 44.0,
              fontWeight = FontWeight.BOLD,
              color = black,
              textAlign = TextAlign.CENTER,
            ),
            PaywallComponent.Text(
              text = "per {{ product.period }}",
              fontSize = 15.0,
              color = gray500,
              textAlign = TextAlign.CENTER,
            ),
            PaywallComponent.Spacer(size = Size(height = SizeConstraint.Fixed(4.0))),
            PaywallComponent.Text(
              text = "{{ product.trial_days }}-day free trial included",
              fontSize = 13.0,
              color = green500,
              fontWeight = FontWeight.MEDIUM,
              textAlign = TextAlign.CENTER,
            ),
          ),
        ),

        PaywallComponent.Spacer(size = Size(height = SizeConstraint.Fixed(28.0))),

        // CTA
        PaywallComponent.PurchaseButton(
          productId = "yearly",
          size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fixed(56.0)),
          backgroundColor = black,
          cornerRadius = CornerRadius.Uniform(16.0),
          components = listOf(
            PaywallComponent.Text(
              text = "Subscribe Now",
              fontSize = 17.0,
              fontWeight = FontWeight.SEMI_BOLD,
              color = white,
              textAlign = TextAlign.CENTER,
            ),
          ),
        ),

        PaywallComponent.Spacer(size = Size(height = SizeConstraint.Fixed(12.0))),

        PaywallComponent.Button(
          action = Action.Restore,
          size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fixed(40.0)),
          backgroundColor = transparent,
          components = listOf(
            PaywallComponent.Text(
              text = "Restore Purchases",
              fontSize = 13.0,
              color = gray400,
              textAlign = TextAlign.CENTER,
            ),
          ),
        ),
      ),
    ),
  ),
)

// ── Feature Gate Paywall ─────────────────────────────────────────────

private fun featureGateConfig() = PaywallComponentsConfig(
  components = listOf(
    PaywallComponent.Stack(
      dimension = Dimension.VERTICAL,
      size = Size(width = SizeConstraint.Fill),
      backgroundColor = white,
      overflow = OverflowBehavior.SCROLL,
      padding = Padding(top = 16.0, bottom = 24.0, leading = 24.0, trailing = 24.0),
      spacing = 0.0,
      components = listOf(
        // Header row
        PaywallComponent.Stack(
          dimension = Dimension.HORIZONTAL,
          size = Size(width = SizeConstraint.Fill),
          alignment = FlexAlignment.SPACE_BETWEEN,
          crossAlignment = CrossAlignment.CENTER,
          components = listOf(
            PaywallComponent.Badge(
              text = "PRO",
              fontSize = 12.0,
              fontWeight = FontWeight.BOLD,
              backgroundColor = purple600,
              textColor = white,
              cornerRadius = CornerRadius.Uniform(8.0),
              padding = Padding.symmetric(horizontal = 14.0, vertical = 5.0),
            ),
            PaywallComponent.CloseButton(
              size = Size(width = SizeConstraint.Fixed(36.0), height = SizeConstraint.Fixed(36.0)),
            ),
          ),
        ),

        PaywallComponent.Spacer(size = Size(height = SizeConstraint.Fixed(20.0))),

        PaywallComponent.Text(
          text = "This Feature\nRequires Pro",
          fontSize = 26.0,
          fontWeight = FontWeight.BOLD,
          color = black,
          lineHeight = 34.0,
        ),

        PaywallComponent.Spacer(size = Size(height = SizeConstraint.Fixed(8.0))),

        PaywallComponent.Text(
          text = "Upgrade to unlock powerful tools for your team.",
          fontSize = 15.0,
          color = gray500,
          lineHeight = 22.0,
        ),

        PaywallComponent.Spacer(size = Size(height = SizeConstraint.Fixed(24.0))),

        // Feature cards
        PaywallComponent.Stack(
          dimension = Dimension.VERTICAL,
          size = Size(width = SizeConstraint.Fill),
          spacing = 10.0,
          components = listOf(
            featureCard("\uD83D\uDCCA", "Advanced Analytics", "Deep insights into your conversion funnels"),
            featureCard("\u26A1", "Custom Triggers", "Build complex server-side placement rules"),
            featureCard("\uD83D\uDC65", "Team Collaboration", "Invite unlimited team members"),
          ),
        ),

        PaywallComponent.Spacer(size = Size(height = SizeConstraint.Fixed(28.0))),

        // CTA
        PaywallComponent.PurchaseButton(
          productId = "monthly",
          size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fixed(54.0)),
          backgroundColor = purple600,
          cornerRadius = CornerRadius.Uniform(14.0),
          components = listOf(
            PaywallComponent.Text(
              text = "Upgrade \u2014 {{ product.price }}/{{ product.period }}",
              fontSize = 16.0,
              fontWeight = FontWeight.SEMI_BOLD,
              color = white,
              textAlign = TextAlign.CENTER,
            ),
          ),
        ),

        PaywallComponent.Spacer(size = Size(height = SizeConstraint.Fixed(8.0))),

        PaywallComponent.Button(
          action = Action.Restore,
          size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fixed(40.0)),
          backgroundColor = transparent,
          components = listOf(
            PaywallComponent.Text(
              text = "Restore Purchases",
              fontSize = 13.0,
              color = gray400,
              textAlign = TextAlign.CENTER,
            ),
          ),
        ),
      ),
    ),
  ),
)

// ── Component Helpers ────────────────────────────────────────────────

private fun featureRow(emoji: String, label: String) = PaywallComponent.Stack(
  dimension = Dimension.HORIZONTAL,
  size = Size(width = SizeConstraint.Fill),
  spacing = 14.0,
  crossAlignment = CrossAlignment.CENTER,
  components = listOf(
    PaywallComponent.Text(text = emoji, fontSize = 18.0),
    PaywallComponent.Text(
      text = label,
      fontSize = 15.0,
      fontWeight = FontWeight.MEDIUM,
      color = black,
    ),
  ),
)

private fun packageCard(
  productId: String,
  label: String,
  detail: String,
  badge: String?,
  isDefault: Boolean,
) = PaywallComponent.Package(
  productId = productId,
  isDefault = isDefault,
  size = Size(width = SizeConstraint.Fill),
  padding = Padding(top = 14.0, bottom = 14.0, leading = 16.0, trailing = 16.0),
  backgroundColor = gray50,
  cornerRadius = CornerRadius.Uniform(14.0),
  border = Border(color = gray100, width = 1.5),
  selectedOverride = PartialStyle(
    backgroundColor = blue50,
    border = Border(color = blue500, width = 2.0),
    shadow = Shadow(
      color = ColorScheme(light = ThemeColor("#3B82F6", alpha = 0.12)),
      radius = 12.0,
    ),
  ),
  components = listOf(
    PaywallComponent.Stack(
      dimension = Dimension.HORIZONTAL,
      size = Size(width = SizeConstraint.Fill),
      alignment = FlexAlignment.SPACE_BETWEEN,
      crossAlignment = CrossAlignment.CENTER,
      components = buildList {
        // Left: label + price
        add(
          PaywallComponent.Stack(
            dimension = Dimension.VERTICAL,
            spacing = 3.0,
            components = listOf(
              PaywallComponent.Text(
                text = label,
                fontSize = 16.0,
                fontWeight = FontWeight.SEMI_BOLD,
                color = black,
              ),
              PaywallComponent.Text(
                text = detail,
                fontSize = 13.0,
                color = gray500,
              ),
            ),
          ),
        )
        // Right: badge
        if (badge != null) {
          add(
            PaywallComponent.Badge(
              text = badge,
              fontSize = 11.0,
              fontWeight = FontWeight.BOLD,
              backgroundColor = amber500,
              textColor = white,
              cornerRadius = CornerRadius.Uniform(6.0),
              padding = Padding.symmetric(horizontal = 10.0, vertical = 4.0),
            ),
          )
        }
      },
    ),
  ),
)

private fun featureCard(emoji: String, title: String, description: String) = PaywallComponent.Stack(
  dimension = Dimension.HORIZONTAL,
  size = Size(width = SizeConstraint.Fill),
  backgroundColor = purple50,
  cornerRadius = CornerRadius.Uniform(14.0),
  padding = Padding(top = 16.0, bottom = 16.0, leading = 16.0, trailing = 16.0),
  spacing = 14.0,
  crossAlignment = CrossAlignment.CENTER,
  components = listOf(
    PaywallComponent.Text(text = emoji, fontSize = 24.0),
    PaywallComponent.Stack(
      dimension = Dimension.VERTICAL,
      spacing = 3.0,
      components = listOf(
        PaywallComponent.Text(
          text = title,
          fontSize = 15.0,
          fontWeight = FontWeight.SEMI_BOLD,
          color = black,
        ),
        PaywallComponent.Text(
          text = description,
          fontSize = 13.0,
          color = gray500,
        ),
      ),
    ),
  ),
)
