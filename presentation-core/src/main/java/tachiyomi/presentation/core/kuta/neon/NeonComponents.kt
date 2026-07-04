// FORK: Phase 2B
package tachiyomi.presentation.core.kuta.neon

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.kuta.components.KutaBadgeVariant
import tachiyomi.presentation.core.kuta.components.KutaButtonVariant
import tachiyomi.presentation.core.kuta.components.KutaCardElevation
import tachiyomi.presentation.core.kuta.components.KutaDropdownItem
import tachiyomi.presentation.core.kuta.components.KutaInputVariant
import tachiyomi.presentation.core.kuta.components.KutaNavigationItem
import tachiyomi.presentation.core.kuta.components.KutaTabItem
import tachiyomi.presentation.core.kuta.components.KutaToggleStyle
import tachiyomi.presentation.core.kuta.effects.neonGlow
import tachiyomi.presentation.core.kuta.theme.KutaMode
import tachiyomi.presentation.core.kuta.theme.KutaSpacing
import tachiyomi.presentation.core.kuta.theme.LocalKutaMode
import tachiyomi.presentation.core.kuta.theme.kutaColors
import tachiyomi.presentation.core.kuta.theme.kutaTypography

/**
 * FORK: Phase 2B — Real Neon component implementations.
 * Per DOCS/design-system/01-neon.md §5 (component specs) and §6 (effects).
 *
 * Design principles (§1):
 * - Dark-first: every surface starts dark; light is added via accents.
 * - Glow, not shadow: use [neonGlow] for hover/active emphasis (dark mode only —
 *   glow is invisible on light backgrounds, replaced with solid 2dp accent borders).
 * - Glass-morphism: semi-transparent backgrounds via `bgGlass` (backdrop-blur
 *   requires API 31+; Haze fallback is configured in build.gradle.kts but the
 *   effect modifier itself is not yet implemented — for now we use bgGlass as a
 *   translucent color overlay).
 * - Thin borders: 1dp with `borderDefault` or `accentPrimary`.
 * - Corner radius: 10dp (buttons/inputs), 14dp (cards), 18dp (dialogs).
 * - Monospace for data: use `kutaTypography.monoValue` for numbers/timestamps.
 *
 * Disabled state per §5.1: 50% opacity, no glow.
 *
 * The 29 component functions below have identical signatures to the corresponding
 * Material3 wrappers in `material/Material3Components.kt` so they can be
 * drop-in replacements via the `Kuta*` delegator in `components/KutaComponents.kt`.
 */

// ===== Helpers =====

/**
 * FORK: Draws a border only on the top edge (Neon's navigation bars / bottom
 * sheets get a single 1dp top border rather than a full rectangle).
 *
 * Uses [drawWithContent] (not [drawBehind]) so the line is drawn ON TOP of the
 * composable's content — necessary because M3 components like NavigationBar
 * fill their container with an opaque color that would otherwise hide a
 * behind-content line.
 */
private fun Modifier.topBorder(color: Color, thickness: Dp = 1.dp): Modifier = this.drawWithContent {
    drawContent()
    drawLine(
        color = color,
        start = Offset(0f, 0f),
        end = Offset(size.width, 0f),
        strokeWidth = thickness.toPx(),
    )
}

/**
 * FORK: Draws a border only on the bottom edge (Neon's TopAppBar gets a single
 * 1dp bottom border). Same drawWithContent rationale as [topBorder].
 */
private fun Modifier.bottomBorder(color: Color, thickness: Dp = 1.dp): Modifier = this.drawWithContent {
    drawContent()
    drawLine(
        color = color,
        start = Offset(0f, size.height),
        end = Offset(size.width, size.height),
        strokeWidth = thickness.toPx(),
    )
}

// ===== Buttons (§5.1) =====

/**
 * Neon button — dark bg, accent border, accent text, glow on hover.
 * Per 01-neon.md §5.1.
 */
@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    icon: ImageVector?,
    variant: KutaButtonVariant,
) {
    val colors = kutaColors
    val typography = kutaTypography
    val isDark = LocalKutaMode.current == KutaMode.DARK
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    // FORK: per §5.1, primary = accent border + accent text + glow on hover;
    // secondary = transparent + accent text; destructive = coral border + coral text;
    // ghost = transparent + muted text.
    val borderColor = when (variant) {
        KutaButtonVariant.PRIMARY -> colors.accentPrimary
        KutaButtonVariant.SECONDARY -> colors.borderDefault
        KutaButtonVariant.DESTRUCTIVE -> colors.accentTertiary
        KutaButtonVariant.GHOST -> Color.Transparent
    }
    val textColor = when (variant) {
        KutaButtonVariant.PRIMARY -> colors.accentPrimary
        KutaButtonVariant.SECONDARY -> colors.accentPrimary
        KutaButtonVariant.DESTRUCTIVE -> colors.accentTertiary
        KutaButtonVariant.GHOST -> if (isHovered) colors.fgSecondary else colors.fgMuted
    }
    val bgColor = when (variant) {
        // FORK: primary lightens from bgBase → bgElevated on hover (§5.1 "bg lightens slightly").
        KutaButtonVariant.PRIMARY -> if (isHovered) colors.bgElevated else colors.bgBase
        KutaButtonVariant.SECONDARY,
        KutaButtonVariant.DESTRUCTIVE,
        KutaButtonVariant.GHOST -> Color.Transparent
    }
    val glowColor = when (variant) {
        KutaButtonVariant.PRIMARY -> colors.accentPrimary
        KutaButtonVariant.DESTRUCTIVE -> colors.accentTertiary
        else -> null
    }

    // FORK: glow modifier applied BEFORE clip so the shadow halo extends beyond
    // the rounded-rect bounds (otherwise clip would crop the halo and the glow
    // would be invisible). Disabled state per §5.1: 50% opacity, no glow.
    val glowMod = if (isHovered && isDark && enabled && glowColor != null) {
        Modifier.neonGlow(glowColor)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .height(48.dp)
            .then(glowMod)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                enabled = enabled,
                onClick = onClick,
            )
            .then(if (!enabled) Modifier.alpha(0.5f) else Modifier)
            .padding(horizontal = KutaSpacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KutaSpacing.sm),
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(text, color = textColor, style = typography.button)
        }
    }
}

/**
 * Neon outlined button — transparent bg, 1dp borderDefault border, fgPrimary text.
 * Distinct from `KutaButtonVariant.SECONDARY` (which uses accent text); this is
 * the "neutral secondary action" button.
 */
@Composable
fun NeonOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    icon: ImageVector?,
) {
    val colors = kutaColors
    val typography = kutaTypography
    val isDark = LocalKutaMode.current == KutaMode.DARK
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val borderColor = if (isHovered) colors.borderAccent else colors.borderDefault
    val bgColor = if (isHovered) colors.bgElevated.copy(alpha = 0.5f) else Color.Transparent
    val textColor = colors.fgPrimary

    Box(
        modifier = modifier
            .height(48.dp)
            .then(
                if (isHovered && isDark && enabled) {
                    Modifier.neonGlow(colors.accentPrimary.copy(alpha = 0.15f))
                } else {
                    Modifier
                },
            )
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                enabled = enabled,
                onClick = onClick,
            )
            .then(if (!enabled) Modifier.alpha(0.5f) else Modifier)
            .padding(horizontal = KutaSpacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KutaSpacing.sm),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
            }
            Text(text, color = textColor, style = typography.button)
        }
    }
}

/**
 * Neon text button — transparent, no border, accent text, hover bg.
 */
@Composable
fun NeonTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    icon: ImageVector?,
) {
    val colors = kutaColors
    val typography = kutaTypography
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isHovered) colors.bgElevated.copy(alpha = 0.5f) else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                enabled = enabled,
                onClick = onClick,
            )
            .then(if (!enabled) Modifier.alpha(0.5f) else Modifier)
            .padding(horizontal = KutaSpacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KutaSpacing.sm),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = colors.accentPrimary, modifier = Modifier.size(18.dp))
            }
            Text(text, color = colors.accentPrimary, style = typography.button)
        }
    }
}

/**
 * Neon icon button — 40dp square, transparent bg, accent on hover.
 */
@Composable
fun NeonIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    contentDescription: String?,
) {
    val colors = kutaColors
    val isDark = LocalKutaMode.current == KutaMode.DARK
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val tint = if (isHovered) colors.accentPrimary else colors.fgSecondary
    Box(
        modifier = modifier
            .size(40.dp)
            .then(
                if (isHovered && isDark && enabled) {
                    Modifier.neonGlow(colors.accentPrimary.copy(alpha = 0.15f))
                } else {
                    Modifier
                },
            )
            .clip(RoundedCornerShape(8.dp))
            .background(if (isHovered) colors.bgElevated.copy(alpha = 0.5f) else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                enabled = enabled,
                onClick = onClick,
            )
            .then(if (!enabled) Modifier.alpha(0.5f) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(20.dp))
    }
}

// ===== Cards (§5.2) =====

/**
 * Neon card — bgSurface, 1dp borderDefault, 14dp radius. Hover: accent border + glow.
 * Per 01-neon.md §5.2.
 *
 * FORK: uses [hoverable] (not [clickable]) since the KutaCard API has no onClick —
 * we only need hover events for the hover-glow state.
 */
@Composable
fun NeonCard(
    modifier: Modifier,
    elevation: KutaCardElevation,
    content: @Composable () -> Unit,
) {
    val colors = kutaColors
    val isDark = LocalKutaMode.current == KutaMode.DARK
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    // FORK: per §8.1, cards in large grids should NOT use glow (perf). For a
    // standalone card, we enable hover glow; for ELEVATED we use bgGlass + glow.
    val borderColor = if (isHovered) colors.borderAccent else colors.borderDefault
    val bgColor = when (elevation) {
        KutaCardElevation.FLAT -> colors.bgSurface
        KutaCardElevation.ELEVATED -> colors.bgGlass
    }

    Box(
        modifier = modifier
            .then(
                if (isHovered && isDark) {
                    Modifier.neonGlow(colors.accentPrimary.copy(alpha = 0.15f))
                } else {
                    Modifier
                },
            )
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .hoverable(interactionSource),
    ) {
        content()
    }
}

/**
 * Neon elevated card — glass-morphism (bgGlass), borderStrong, 14dp radius.
 */
@Composable
fun NeonElevatedCard(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val colors = kutaColors
    val isDark = LocalKutaMode.current == KutaMode.DARK
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = modifier
            .then(
                if (isHovered && isDark) {
                    Modifier.neonGlow(colors.accentPrimary.copy(alpha = 0.2f))
                } else {
                    Modifier
                },
            )
            .clip(RoundedCornerShape(14.dp))
            .background(colors.bgGlass)
            .border(1.dp, colors.borderStrong, RoundedCornerShape(14.dp))
            .hoverable(interactionSource),
    ) {
        content()
    }
}

// ===== Inputs (§5.3) =====

/**
 * Neon text input — bgBase, 1dp borderDefault → accentPrimary on focus, glow on focus.
 * Per 01-neon.md §5.3.
 *
 * FORK: wraps M3 OutlinedTextField with a fully-custom colors config (all
 * container/border colors set to Transparent) so the Neon visual identity
 * (bgBase fill, 1dp border, accent focus ring) shows through the outer Box.
 */
@Composable
fun NeonInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    placeholder: String,
    enabled: Boolean,
    variant: KutaInputVariant,
) {
    val colors = kutaColors
    val typography = kutaTypography
    val isError = variant == KutaInputVariant.ERROR
    val inputEnabled = enabled && variant != KutaInputVariant.DISABLED
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val borderColor = when {
        isError -> colors.accentTertiary
        isFocused -> colors.accentPrimary
        else -> colors.borderDefault
    }
    val textColor = if (isError) colors.accentTertiary else colors.fgPrimary

    // FORK: glow on focus (dark mode only) per §5.3 "Focus: glow glowPrimary".
    val isDark = LocalKutaMode.current == KutaMode.DARK
    val glowMod = if (isFocused && isDark && inputEnabled) {
        Modifier.neonGlow(colors.accentPrimary.copy(alpha = 0.2f))
    } else {
        Modifier
    }

    // FORK: no fixed height on the outer Box — OutlinedTextField sizes itself
    // (~56dp naturally); the visible input area is ~44dp after subtracting
    // M3's internal padding, matching §5.3's 44dp target.
    Box(
        modifier = modifier
            .then(glowMod)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.bgBase)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .then(if (!inputEnabled) Modifier.alpha(0.5f) else Modifier),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    placeholder,
                    color = colors.fgDim,
                    style = typography.body,
                )
            },
            enabled = inputEnabled,
            isError = isError,
            singleLine = true,
            textStyle = typography.body.copy(color = textColor),
            // FORK: disable M3's default border/indicator — we draw our own via Modifier.border above.
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                errorContainerColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                errorBorderColor = Color.Transparent,
                focusedLabelColor = colors.accentPrimary,
                unfocusedLabelColor = colors.fgMuted,
                cursorColor = colors.accentPrimary,
                errorCursorColor = colors.accentTertiary,
                focusedLeadingIconColor = colors.accentPrimary,
                unfocusedLeadingIconColor = colors.fgMuted,
                focusedTrailingIconColor = colors.accentPrimary,
                unfocusedTrailingIconColor = colors.fgMuted,
                focusedSupportingTextColor = colors.fgSecondary,
                unfocusedSupportingTextColor = colors.fgMuted,
            ),
            interactionSource = interactionSource,
        )
    }
}

/**
 * Neon search input — input + leading search icon.
 * Per 01-neon.md §5.3 (input spec) with search icon per Material3Components.
 */
@Composable
fun NeonSearchInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    placeholder: String,
) {
    val colors = kutaColors
    val typography = kutaTypography
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val borderColor = if (isFocused) colors.accentPrimary else colors.borderDefault

    // FORK: no fixed height — OutlinedTextField sizes itself; the Row wraps
    // tightly around the icon + text field.
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colors.bgBase)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(horizontal = KutaSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KutaSpacing.sm),
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = if (isFocused) colors.accentPrimary else colors.fgMuted,
            modifier = Modifier.size(18.dp),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(placeholder, color = colors.fgDim, style = typography.body) },
            singleLine = true,
            textStyle = typography.body.copy(color = colors.fgPrimary),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                cursorColor = colors.accentPrimary,
            ),
            interactionSource = interactionSource,
        )
    }
}

// ===== Dialogs / Sheets (§5.4) =====

/**
 * Neon dialog — bgGlass + borderStrong, 18dp radius, max width 560dp.
 * Per 01-neon.md §5.4.
 *
 * FORK: M3's AlertDialog has `containerColor` and `shape` params which let us
 * override the surface color/shape. We use bgGlass for the glass-morphism look
 * (real backdrop-blur requires Haze; we approximate with translucent bg).
 */
@Composable
fun NeonDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val colors = kutaColors
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {},
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, colors.borderStrong, RoundedCornerShape(18.dp)),
        containerColor = colors.bgGlass,
        shape = RoundedCornerShape(18.dp),
        text = { content() },
    )
}

/**
 * Neon alert dialog — title + message + confirm/dismiss.
 * Uses NeonButton colors on M3 TextButtons for accent primary/ghost actions.
 */
@Composable
fun NeonAlertDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier,
    confirmText: String,
    dismissText: String,
) {
    val colors = kutaColors
    val typography = kutaTypography
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, color = colors.fgPrimary, style = typography.title)
        },
        text = {
            Text(message, color = colors.fgSecondary, style = typography.body)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = colors.accentPrimary, style = typography.button)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText, color = colors.fgMuted, style = typography.button)
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, colors.borderStrong, RoundedCornerShape(18.dp)),
        containerColor = colors.bgGlass,
        shape = RoundedCornerShape(18.dp),
        titleContentColor = colors.fgPrimary,
        textContentColor = colors.fgSecondary,
    )
}

/**
 * Neon bottom sheet — bgSurface, top-rounded, accent drag handle.
 * Per 01-neon.md §5.4 (dialog spec applied to sheets).
 *
 * FORK: delegates structure to M3 ModalBottomSheet (custom bottom-sheet logic
 * is complex), but overrides container color, drag handle, and adds top border.
 */
@Composable
fun NeonBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val colors = kutaColors
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = colors.bgSurface,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        dragHandle = {
            // FORK: Neon-styled drag handle — accent pill instead of M3 default.
            Box(
                Modifier
                    .padding(vertical = KutaSpacing.sm)
                    .size(width = 32.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.accentPrimary.copy(alpha = 0.5f)),
            )
        },
    ) {
        content()
    }
}

// ===== Navigation (§5.5) =====

/**
 * Neon bottom navigation bar — bgGlass + 1dp top border, accent active item with glow.
 * Per 01-neon.md §5.5.
 *
 * FORK: wraps M3 NavigationBar in a Box so we can add a top border (M3 has no
 * direct top-border API for NavigationBar). Active item indicator: accent
 * icon/text color + transparent M3 indicator pill (Neon's "active" signal is
 * the accent color, not a pill background).
 */
@Composable
fun NeonNavigationBar(
    items: List<KutaNavigationItem>,
    modifier: Modifier,
) {
    val colors = kutaColors
    val typography = kutaTypography
    val isDark = LocalKutaMode.current == KutaMode.DARK

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgGlass)
            .topBorder(colors.borderDefault, 1.dp),
    ) {
        NavigationBar(
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color.Transparent,
            contentColor = colors.fgSecondary,
            tonalElevation = 0.dp,
        ) {
            items.forEach { item ->
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()
                NavigationBarItem(
                    selected = item.selected,
                    onClick = item.onClick,
                    icon = {
                        // FORK: subtle hover-glow on the active icon — kept small (radius 8dp) to
                        // avoid perf issues with many tabs (per §6.2 perf warning).
                        val iconMod = if (item.selected && isHovered && isDark) {
                            Modifier.neonGlow(colors.accentPrimary.copy(alpha = 0.2f), radius = 8.dp)
                        } else {
                            Modifier
                        }
                        Icon(
                            item.icon,
                            contentDescription = item.label,
                            tint = if (item.selected) colors.accentPrimary else colors.fgMuted,
                            modifier = Modifier.size(24.dp).then(iconMod),
                        )
                    },
                    label = {
                        Text(
                            item.label,
                            color = if (item.selected) colors.accentPrimary else colors.fgMuted,
                            style = typography.bodySmall,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colors.accentPrimary,
                        selectedTextColor = colors.accentPrimary,
                        unselectedIconColor = colors.fgMuted,
                        unselectedTextColor = colors.fgMuted,
                        indicatorColor = Color.Transparent,
                    ),
                    interactionSource = interactionSource,
                )
            }
        }
    }
}

/**
 * Neon navigation rail — bgSidebar + 1dp right border, accent active item.
 * Per 01-neon.md §5.5 (rail variant of bottom nav).
 *
 * FORK: delegates structure to M3 NavigationRail; Neon styling via colors.
 */
@Composable
fun NeonNavigationRail(
    items: List<KutaNavigationItem>,
    modifier: Modifier,
) {
    val colors = kutaColors
    val typography = kutaTypography
    val isDark = LocalKutaMode.current == KutaMode.DARK

    NavigationRail(
        modifier = modifier
            .background(colors.bgSidebar)
            .border(1.dp, colors.borderDefault, RectangleShape),
        containerColor = Color.Transparent,
        contentColor = colors.fgSecondary,
    ) {
        items.forEach { item ->
            val interactionSource = remember { MutableInteractionSource() }
            NavigationRailItem(
                selected = item.selected,
                onClick = item.onClick,
                icon = {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        tint = if (item.selected) colors.accentPrimary else colors.fgMuted,
                        modifier = Modifier.size(24.dp),
                    )
                },
                label = {
                    Text(
                        item.label,
                        color = if (item.selected) colors.accentPrimary else colors.fgMuted,
                        style = typography.bodySmall,
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = colors.accentPrimary,
                    selectedTextColor = colors.accentPrimary,
                    unselectedIconColor = colors.fgMuted,
                    unselectedTextColor = colors.fgMuted,
                    indicatorColor = if (isDark) {
                        colors.accentPrimary.copy(alpha = 0.10f)
                    } else {
                        colors.accentPrimary.copy(alpha = 0.15f)
                    },
                ),
                interactionSource = interactionSource,
            )
        }
    }
}

/**
 * Neon tab row — bgSurface, accent indicator, accent selected text.
 *
 * FORK: delegates structure to M3 TabRow. The default M3 indicator already uses
 * `contentColor` (which we set to accentPrimary) and 2dp height — matching the
 * Neon spec — so we don't need a custom `indicator` lambda. We override only
 * `containerColor`, `divider`, and per-Tab content colors.
 */
@Composable
fun NeonTabRow(
    tabs: List<KutaTabItem>,
    modifier: Modifier,
) {
    val colors = kutaColors
    val typography = kutaTypography
    val selectedIndex = tabs.indexOfFirst { it.selected }.coerceAtLeast(0)

    TabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier,
        containerColor = colors.bgSurface,
        contentColor = colors.accentPrimary,
        divider = {
            // FORK: thin borderSubtle divider per §5.7.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.borderSubtle),
            )
        },
    ) {
        tabs.forEach { tab ->
            Tab(
                selected = tab.selected,
                onClick = tab.onClick,
                text = {
                    Text(
                        tab.label,
                        color = if (tab.selected) colors.accentPrimary else colors.fgMuted,
                        style = typography.button,
                    )
                },
                selectedContentColor = colors.accentPrimary,
                unselectedContentColor = colors.fgMuted,
            )
        }
    }
}

// ===== Items / Badges / Chips (§5.7, §5.8, §5.9) =====

/**
 * Neon list item — transparent, hover bgElevated@50%, 16v/24h padding, bottom divider.
 * Per 01-neon.md §5.7.
 *
 * FORK: uses [hoverable] (no onClick in API) so we get hover events for the
 * bgElevated@50% hover state without making the row clickable.
 */
@Composable
fun NeonListItem(
    title: String,
    modifier: Modifier,
    subtitle: String?,
    icon: ImageVector?,
    trailing: @Composable (() -> Unit)?,
) {
    val colors = kutaColors
    val typography = kutaTypography
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isHovered) colors.bgElevated.copy(alpha = 0.5f) else Color.Transparent)
            // FORK: bottomBorder BEFORE padding so it draws on the full row bounds
            // (y = full height) — applied AFTER background so it sits on top of the bg.
            .bottomBorder(colors.borderSubtle, 1.dp)
            .hoverable(interactionSource)
            .padding(horizontal = KutaSpacing.xl, vertical = KutaSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = colors.accentPrimary,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = if (icon != null) KutaSpacing.lg else 0.dp),
        ) {
            Text(title, color = colors.fgPrimary, style = typography.body)
            if (subtitle != null) {
                Text(
                    subtitle,
                    color = colors.fgMuted,
                    style = typography.bodySmall,
                )
            }
        }
        trailing?.invoke()
    }
}

/**
 * Neon badge — accent bg, bgBase text, 4dp radius, label typography.
 * Per 01-neon.md §5.9.
 *
 * FORK: variant drives the bg/fg color pair. ACCENT=purple, WARNING=amber,
 * ERROR=coral, DEFAULT=accentPrimary (per §2.1 semantic roles).
 */
@Composable
fun NeonBadge(
    text: String,
    modifier: Modifier,
    variant: KutaBadgeVariant,
) {
    val colors = kutaColors
    val typography = kutaTypography

    val (bgColor, fgColor) = when (variant) {
        KutaBadgeVariant.DEFAULT -> colors.accentPrimary to colors.onAccent
        KutaBadgeVariant.ACCENT -> colors.accentPurple to colors.onAccent
        KutaBadgeVariant.WARNING -> colors.accentQuaternary to colors.onAccent
        KutaBadgeVariant.ERROR -> colors.accentTertiary to colors.onAccent
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = KutaSpacing.sm, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            color = fgColor,
            style = typography.label,
        )
    }
}

/**
 * Neon chip — accent@10% bg, accent@20% border, accent text, 6dp radius, 24dp height.
 * Per 01-neon.md §5.8.
 */
@Composable
fun NeonChip(
    text: String,
    modifier: Modifier,
    selected: Boolean,
    onClick: (() -> Unit)?,
) {
    val colors = kutaColors
    val typography = kutaTypography
    val isDark = LocalKutaMode.current == KutaMode.DARK
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    // FORK: per §5.8, selected chip uses accent@10% bg + accent@20% border + accent text.
    // Unselected chip is more muted: borderDefault border + fgSecondary text + transparent bg.
    val bgColor = if (selected) colors.accentPrimary.copy(alpha = 0.10f) else Color.Transparent
    val borderColor = if (selected) colors.accentPrimary.copy(alpha = 0.20f) else colors.borderDefault
    val textColor = if (selected) colors.accentPrimary else colors.fgSecondary

    val clickableMod = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = ripple(),
            enabled = true,
            onClick = onClick,
        )
    } else if (selected) {
        // FORK: even without onClick, we want hover events for the glow-on-hover.
        Modifier.hoverable(interactionSource)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .height(24.dp)
            .then(
                if (selected && isHovered && isDark) {
                    Modifier.neonGlow(colors.accentPrimary.copy(alpha = 0.15f))
                } else {
                    Modifier
                },
            )
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .then(clickableMod)
            .padding(horizontal = KutaSpacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = textColor, style = typography.label)
    }
}

// ===== Controls =====

/**
 * Neon toggle — accent thumb/track when on, borderStrong when off.
 * Per 01-neon.md §8.4 ("Toggles: accentPrimary when on, borderStrong when off").
 *
 * FORK: delegates to M3 Switch / Checkbox with Neon color overrides.
 */
@Composable
fun NeonToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier,
    style: KutaToggleStyle,
) {
    val colors = kutaColors
    when (style) {
        KutaToggleStyle.SWITCH -> Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onAccent,
                checkedTrackColor = colors.accentPrimary,
                checkedBorderColor = colors.accentPrimary,
                uncheckedThumbColor = colors.fgMuted,
                uncheckedTrackColor = colors.bgElevated,
                uncheckedBorderColor = colors.borderStrong,
            ),
        )
        KutaToggleStyle.CHECKBOX -> Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            colors = CheckboxDefaults.colors(
                checkedColor = colors.accentPrimary,
                uncheckedColor = colors.borderStrong,
                checkmarkColor = colors.onAccent,
            ),
        )
    }
}

/**
 * Neon slider — accent track, accent handle.
 * Per 01-neon.md §8.3 (player UI guidance — accent scrubber).
 *
 * FORK: M3 Slider's thumb doesn't expose a glow modifier; for full glow-on-thumb
 * we'd need a custom Slider. For now we use M3 Slider with Neon colors.
 */
@Composable
fun NeonSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier,
    valueRange: ClosedFloatingPointRange<Float>,
) {
    val colors = kutaColors
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        valueRange = valueRange,
        colors = SliderDefaults.colors(
            thumbColor = colors.accentPrimary,
            activeTrackColor = colors.accentPrimary,
            inactiveTrackColor = colors.bgElevated,
            activeTickColor = colors.accentPrimary,
            inactiveTickColor = colors.borderStrong,
        ),
    )
}

/**
 * Neon progress bar — accent track on bgElevated base, 4dp height, 2dp rounded.
 *
 * FORK: delegates to M3 LinearProgressIndicator (which handles determinate vs
 * indeterminate animation) with Neon color overrides.
 */
@Composable
fun NeonProgressBar(
    progress: Float?,
    modifier: Modifier,
) {
    val colors = kutaColors
    val baseModifier = modifier
        .height(4.dp)
        .clip(RoundedCornerShape(2.dp))
    if (progress != null) {
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = baseModifier,
            color = colors.accentPrimary,
            trackColor = colors.bgElevated,
        )
    } else {
        LinearProgressIndicator(
            modifier = baseModifier,
            color = colors.accentPrimary,
            trackColor = colors.bgElevated,
        )
    }
}

// ===== Feedback =====

/**
 * Neon snackbar — bgGlass + borderStrong, accent action.
 * Per 01-neon.md §5.4 (glass panel spec) + §5.1 (accent for action).
 */
@Composable
fun NeonSnackbar(
    message: String,
    modifier: Modifier,
    actionLabel: String?,
    onAction: (() -> Unit)?,
) {
    val colors = kutaColors
    val typography = kutaTypography
    Snackbar(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colors.bgGlass)
            .border(1.dp, colors.borderStrong, RoundedCornerShape(10.dp)),
        containerColor = Color.Transparent,
        contentColor = colors.fgPrimary,
        action = if (actionLabel != null) {
            {
                TextButton(onClick = onAction ?: {}) {
                    Text(
                        actionLabel.uppercase(),
                        color = colors.accentPrimary,
                        style = typography.label,
                    )
                }
            }
        } else {
            null
        },
        actionContentColor = colors.accentPrimary,
    ) {
        Text(message, color = colors.fgPrimary, style = typography.body)
    }
}

/**
 * Neon dropdown menu — bgSurface + borderStrong, 10dp radius, accent icons.
 *
 * FORK: wraps M3 DropdownMenu in a MaterialTheme override so the popup's surface
 * tint comes from Neon's bgSurface (M3's default surfaceColorAtElevation would
 * tint accent over surface, which dilutes the Neon look). Per spec §5.4 (dialog
 * panel styling) applied to dropdowns.
 */
@Composable
fun NeonDropdownMenu(
    items: List<KutaDropdownItem>,
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    expanded: Boolean,
) {
    val colors = kutaColors
    val typography = kutaTypography
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            surface = colors.bgSurface,
            onSurface = colors.fgPrimary,
            // FORK: surfaceTint defaults to primary; setting it to bgSurface prevents
            // M3 from tinting the dropdown with the accent color (which would muddy it).
            primary = colors.bgSurface,
            onSurfaceVariant = colors.fgSecondary,
        ),
    ) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier
                .background(colors.bgSurface)
                .border(1.dp, colors.borderStrong, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp)),
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(item.label, color = colors.fgPrimary, style = typography.body)
                    },
                    onClick = { item.onClick(); onDismissRequest() },
                    leadingIcon = if (item.icon != null) {
                        {
                            Icon(
                                item.icon,
                                contentDescription = null,
                                tint = colors.accentPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

// ===== Layout =====

/**
 * Neon scaffold — bgBase background, delegates structure to M3 Scaffold.
 *
 * FORK: complex to rebuild from scratch — Scaffold handles insets, FAB positioning,
 * top/bottom bar slots. We use M3 Scaffold with Neon containerColor (bgBase).
 */
@Composable
fun NeonScaffold(
    modifier: Modifier,
    topBar: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    floatingActionButton: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val colors = kutaColors
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        containerColor = colors.bgBase,
        contentColor = colors.fgPrimary,
        content = content,
    )
}

/**
 * Neon top app bar — bgGlass + 1dp bottom border, headline title, accent back on hover.
 * Per 01-neon.md §5.6.
 *
 * FORK: delegates structure to M3 TopAppBar; Neon colors via TopAppBarDefaults.
 */
@Composable
fun NeonTopAppBar(
    title: String,
    modifier: Modifier,
    onBack: (() -> Unit)?,
    actions: @Composable () -> Unit,
) {
    val colors = kutaColors
    val typography = kutaTypography
    TopAppBar(
        title = {
            Text(title, color = colors.fgPrimary, style = typography.headline)
        },
        navigationIcon = {
            if (onBack != null) {
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isHovered) colors.bgElevated.copy(alpha = 0.5f) else Color.Transparent)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(),
                            enabled = true,
                            onClick = onBack,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (isHovered) colors.accentPrimary else colors.fgSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        },
        actions = actions,
        modifier = modifier.bottomBorder(colors.borderDefault, 1.dp),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colors.bgGlass,
            titleContentColor = colors.fgPrimary,
            navigationIconContentColor = colors.fgSecondary,
            actionIconContentColor = colors.fgSecondary,
        ),
    )
}

/**
 * Neon bottom app bar — bgGlass + 1dp top border.
 */
@Composable
fun NeonBottomAppBar(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val colors = kutaColors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgGlass)
            .topBorder(colors.borderDefault, 1.dp)
            .padding(horizontal = KutaSpacing.lg, vertical = KutaSpacing.sm),
        contentAlignment = Alignment.CenterStart,
    ) {
        content()
    }
}

/**
 * Neon FAB — accent bg, onAccent icon, glow on hover (dark mode).
 * Per 01-neon.md §5.1 (button color semantics applied to FAB).
 */
@Composable
fun NeonFAB(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier,
    text: String?,
) {
    val colors = kutaColors
    val typography = kutaTypography
    val isDark = LocalKutaMode.current == KutaMode.DARK
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val glowMod = if (isHovered && isDark) Modifier.neonGlow(colors.accentPrimary) else Modifier

    if (text != null) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            modifier = modifier.then(glowMod),
            icon = { Icon(icon, contentDescription = null, tint = colors.onAccent) },
            text = { Text(text, color = colors.onAccent, style = typography.button) },
            containerColor = colors.accentPrimary,
            contentColor = colors.onAccent,
            interactionSource = interactionSource,
        )
    } else {
        FloatingActionButton(
            onClick = onClick,
            modifier = modifier.then(glowMod),
            containerColor = colors.accentPrimary,
            contentColor = colors.onAccent,
            interactionSource = interactionSource,
        ) {
            Icon(icon, contentDescription = null, tint = colors.onAccent)
        }
    }
}

// ===== Misc =====

/**
 * Neon skeleton — bgElevated base + accent@5% shimmer sweep.
 * Per 01-neon.md §5.10.
 */
@Composable
fun NeonSkeleton(
    modifier: Modifier,
    height: Int,
) {
    val colors = kutaColors
    // FORK: shimmer uses an infinite transition sweeping an accent@5% gradient
    // per §5.10 + §7 (Loading: shimmer sweep).
    val transition = rememberInfiniteTransition(label = "neon-skeleton-shimmer")
    val shimmerProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer-progress",
    )

    Box(
        modifier = modifier
            .height(height.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.bgElevated)
            .drawWithContent {
                drawContent()
                val width = size.width
                val sweepWidth = width * 0.4f
                val x = -sweepWidth + (width + sweepWidth * 2) * shimmerProgress
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            colors.accentPrimary.copy(alpha = 0.05f),
                            Color.Transparent,
                        ),
                        start = Offset(x - sweepWidth / 2, 0f),
                        end = Offset(x + sweepWidth / 2, size.height),
                    ),
                )
            },
    )
}

/**
 * Neon divider — 1dp borderSubtle line.
 * Per 01-neon.md §8.4 ("Section dividers: 1dp borderSubtle").
 */
@Composable
fun NeonDivider(
    modifier: Modifier,
) {
    val colors = kutaColors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.borderSubtle),
    )
}

/**
 * Neon avatar — circular, 1dp accent border.
 */
@Composable
fun NeonAvatar(
    modifier: Modifier,
    size: Int,
    content: @Composable () -> Unit,
) {
    val colors = kutaColors
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(colors.bgElevated)
            .border(1.dp, colors.borderAccent, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
