// FORK: Phase 2B
package tachiyomi.presentation.core.kuta.brutalist

import android.graphics.DashPathEffect
import android.graphics.Paint as AndroidPaint
import android.graphics.RectF as AndroidRectF
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import tachiyomi.presentation.core.kuta.components.KutaBadgeVariant
import tachiyomi.presentation.core.kuta.components.KutaButtonVariant
import tachiyomi.presentation.core.kuta.components.KutaCardElevation
import tachiyomi.presentation.core.kuta.components.KutaDropdownItem
import tachiyomi.presentation.core.kuta.components.KutaInputVariant
import tachiyomi.presentation.core.kuta.components.KutaNavigationItem
import tachiyomi.presentation.core.kuta.components.KutaTabItem
import tachiyomi.presentation.core.kuta.components.KutaToggleStyle
import tachiyomi.presentation.core.kuta.effects.brutalistGrid
import tachiyomi.presentation.core.kuta.effects.hardShadow
import tachiyomi.presentation.core.kuta.theme.KutaSpacing
import tachiyomi.presentation.core.kuta.theme.LocalKutaColors
import tachiyomi.presentation.core.kuta.theme.LocalKutaTypography

/**
 * FORK: Phase 2B — REAL Brutalist component implementations.
 * Per DOCS/design-system/03-brutalist.md §5.
 *
 * Replaces Phase 2A stubs (which delegated every call to Material3) with full
 * Brutalist styling:
 *
 * - 3dp `brutalistBorder` on everything (buttons, cards, inputs, dialogs, etc.)
 * - Hard zero-blur shadows via [Modifier.hardShadow]
 * - Uppercase heavy typography (Inter ExtraBold/Black) — applied via `text.uppercase()`
 * - Press animation: translate +2/+2dp, shadow shrinks to 1dp (element "pushes into canvas")
 * - Hover: bg shifts to `hoverBgTint`, shadow grows to 5dp, lift -1/-1dp
 * - Disabled: 50% opacity
 * - Bright saturated accents (accentPrimary + pink/green/yellow/orange/purple/red)
 * - `indication = null` on all clickables (no ripple — press animation IS the feedback)
 *
 * Component signatures match [tachiyomi.presentation.core.kuta.material.Material3Components]
 * exactly so they can be swapped via [tachiyomi.presentation.core.kuta.theme.LocalDesignLanguage].
 *
 * For complex pop-up components (Dialog, BottomSheet, DropdownMenu), we wrap M3 with
 * Brutalist colors + 3dp borders + hard shadows. For simple components, we build from scratch.
 */

// ===== Helpers =====

/**
 * Animated press/hover state for a Brutalist interactive element.
 * Press = element pushes into canvas (+offset, shadow shrinks).
 * Hover = element lifts (-offset, shadow grows).
 */
private data class BrutalistPressState(
    val offset: Dp,
    val shadowSize: Dp,
    val isPressed: Boolean,
    val isHovered: Boolean,
)

/**
 * Collects press/hover interactions and animates the offset + shadow size.
 * Defaults match button spec (§5.1); callers override for cards/other elements.
 */
@Composable
private fun rememberBrutalistPressState(
    interactionSource: MutableInteractionSource,
    defaultShadow: Dp = 3.dp,
    hoverShadow: Dp = 5.dp,
    pressShadow: Dp = 1.dp,
    pressOffset: Dp = 2.dp,
    hoverOffset: Dp = (-1).dp,
): BrutalistPressState {
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val targetOffset = when {
        isPressed -> pressOffset
        isHovered -> hoverOffset
        else -> 0.dp
    }
    val targetShadow = when {
        isPressed -> pressShadow
        isHovered -> hoverShadow
        else -> defaultShadow
    }
    val offset by animateDpAsState(targetOffset, label = "brutalist-press-offset")
    val shadow by animateDpAsState(targetShadow, label = "brutalist-press-shadow")

    return BrutalistPressState(
        offset = offset,
        shadowSize = shadow,
        isPressed = isPressed,
        isHovered = isHovered,
    )
}

/**
 * Dashed border for skeletons — raw, unfinished look per §5.11.
 * Compose's `Modifier.border` doesn't support dashed strokes, so we use Android's
 * native `Paint` + `DashPathEffect` via `drawBehind`.
 */
private fun Modifier.dashedBorder(
    width: Dp,
    color: Color,
    cornerRadius: Dp,
    dashOn: Dp = 8.dp,
    dashOff: Dp = 4.dp,
): Modifier = this.then(
    Modifier.drawBehind {
        val paint = AndroidPaint().apply {
            this.color = color.toArgb()
            style = AndroidPaint.Style.STROKE
            strokeWidth = width.toPx()
            pathEffect = DashPathEffect(floatArrayOf(dashOn.toPx(), dashOff.toPx()), 0f)
            isAntiAlias = true
        }
        val halfStroke = width.toPx() / 2f
        val rect = AndroidRectF(
            halfStroke,
            halfStroke,
            size.width - halfStroke,
            size.height - halfStroke,
        )
        drawContext.canvas.nativeCanvas.drawRoundRect(
            rect,
            cornerRadius.toPx(),
            cornerRadius.toPx(),
            paint,
        )
    },
)

// ===== Buttons (§5.1) =====

/**
 * BrutalistButton — §5.1.
 * Height 48dp, 8dp corner radius, 3dp border, hard shadow.
 * Variants: PRIMARY (accent bg), SECONDARY (bgSurface), DESTRUCTIVE (accentRed), GHOST (transparent, no border).
 */
@Composable
fun BrutalistButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    variant: KutaButtonVariant = KutaButtonVariant.PRIMARY,
) {
    val colors = LocalKutaColors.current
    val typography = LocalKutaTypography.current
    val interactionSource = remember { MutableInteractionSource() }
    val press = rememberBrutalistPressState(interactionSource)

    val shape = RoundedCornerShape(8.dp)

    // FORK: bg shifts to tint on hover/press for PRIMARY/SECONDARY/DESTRUCTIVE — color event per §1
    val bgColor = when (variant) {
        KutaButtonVariant.PRIMARY -> when {
            press.isPressed -> colors.activeBgTint
            press.isHovered -> colors.hoverBgTint
            else -> colors.accentPrimary
        }
        KutaButtonVariant.SECONDARY -> when {
            press.isPressed -> colors.activeBgTint
            press.isHovered -> colors.hoverBgTint
            else -> colors.bgSurface
        }
        KutaButtonVariant.DESTRUCTIVE -> when {
            press.isPressed -> colors.activeBgTint
            press.isHovered -> colors.hoverBgTint
            else -> colors.accentRed
        }
        KutaButtonVariant.GHOST -> when {
            press.isPressed -> colors.activeBgTint
            press.isHovered -> colors.hoverBgTint
            else -> Color.Transparent
        }
    }
    val textColor = when (variant) {
        KutaButtonVariant.GHOST -> colors.fgMuted
        KutaButtonVariant.SECONDARY -> colors.fgPrimary
        else -> Color.White
    }
    val borderColor = if (variant == KutaButtonVariant.GHOST) Color.Transparent else colors.brutalistBorder
    val borderWidth = if (variant == KutaButtonVariant.GHOST) 0.dp else 3.dp

    Box(
        modifier = modifier
            .then(if (enabled) Modifier else Modifier.alpha(0.5f))
            .offset(x = press.offset, y = press.offset)
            .hardShadow(
                color = colors.shadowColor,
                offsetX = press.shadowSize,
                offsetY = press.shadowSize,
                cornerRadius = 8.dp,
            )
            .height(48.dp)
            .clip(shape)
            .background(bgColor)
            .border(borderWidth, borderColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // FORK: no ripple — press animation IS the feedback
                enabled = enabled,
                onClick = onClick,
            )
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
            Text(
                text = text.uppercase(),
                color = textColor,
                style = typography.button,
            )
        }
    }
}

/** OutlinedButton = SECONDARY variant of [BrutalistButton]. */
@Composable
fun BrutalistOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    BrutalistButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        variant = KutaButtonVariant.SECONDARY,
    )
}

/** TextButton = GHOST variant of [BrutalistButton]. */
@Composable
fun BrutalistTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    BrutalistButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        variant = KutaButtonVariant.GHOST,
    )
}

/**
 * IconButton — 48x48 square, 3dp border, hard shadow, press animation.
 * Per §5.7 (Back button) and §8.3 (player buttons) spec.
 */
@Composable
fun BrutalistIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    val colors = LocalKutaColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val press = rememberBrutalistPressState(interactionSource)

    val shape = RoundedCornerShape(8.dp)
    val bgColor = when {
        press.isPressed -> colors.activeBgTint
        press.isHovered -> colors.hoverBgTint
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .then(if (enabled) Modifier else Modifier.alpha(0.5f))
            .offset(x = press.offset, y = press.offset)
            .hardShadow(
                color = colors.shadowColor,
                offsetX = press.shadowSize,
                offsetY = press.shadowSize,
                cornerRadius = 8.dp,
            )
            .size(48.dp)
            .clip(shape)
            .background(bgColor)
            .border(3.dp, colors.brutalistBorder, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = colors.fgPrimary,
            modifier = Modifier.size(22.dp),
        )
    }
}

// ===== Cards (§5.2, §5.3) =====

/**
 * BrutalistCard — §5.2.
 * 10dp corner, 3dp border. FLAT = no shadow, ELEVATED = 4dp hard shadow.
 * (Hover/press with tint shifts and shadow size changes is opt-in via parent clickable.)
 */
@Composable
fun BrutalistCard(
    modifier: Modifier = Modifier,
    elevation: KutaCardElevation = KutaCardElevation.FLAT,
    content: @Composable () -> Unit,
) {
    val colors = LocalKutaColors.current
    val shape = RoundedCornerShape(10.dp)
    val shadowSize = if (elevation == KutaCardElevation.ELEVATED) 4.dp else 0.dp

    Box(
        modifier = modifier
            .then(
                if (shadowSize > 0.dp) {
                    Modifier.hardShadow(
                        color = colors.shadowColor,
                        offsetX = shadowSize,
                        offsetY = shadowSize,
                        cornerRadius = 10.dp,
                    )
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(colors.bgSurface)
            .border(3.dp, colors.brutalistBorder, shape),
    ) {
        content()
    }
}

/**
 * ElevatedCard — bigger shadow (8dp) for visual hierarchy.
 * Per §5.2 hover spec — but applied as the resting state for elevated cards.
 */
@Composable
fun BrutalistElevatedCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = LocalKutaColors.current
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .hardShadow(
                color = colors.shadowColor,
                offsetX = 8.dp,
                offsetY = 8.dp,
                cornerRadius = 10.dp,
            )
            .clip(shape)
            .background(colors.bgSurface)
            .border(3.dp, colors.brutalistBorder, shape),
    ) {
        content()
    }
}

// ===== Inputs (§5.4) =====

/**
 * BrutalistInput — §5.4.
 * 44dp height, 8dp corner, 3dp border. Focus = bg shifts to hoverBgTint, 3dp accentPrimary shadow appears.
 * Built on BasicTextField for full control over the box styling.
 */
@Composable
fun BrutalistInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    variant: KutaInputVariant = KutaInputVariant.DEFAULT,
) {
    val colors = LocalKutaColors.current
    val typography = LocalKutaTypography.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isError = variant == KutaInputVariant.ERROR
    val isDisabled = !enabled || variant == KutaInputVariant.DISABLED

    val shape = RoundedCornerShape(8.dp)
    // FORK: focus → tinted bg + accent shadow; default → no shadow
    val bgColor = if (isFocused) colors.hoverBgTint else colors.bgSurface
    val borderColor = if (isError) colors.accentRed else colors.brutalistBorder
    val shadowColor = if (isFocused) colors.accentPrimary else colors.shadowColor
    val shadowSize = if (isFocused) 3.dp else 0.dp

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .then(if (isDisabled) Modifier.alpha(0.5f) else Modifier)
            .height(44.dp)
            .hardShadow(
                color = shadowColor,
                offsetX = shadowSize,
                offsetY = shadowSize,
                cornerRadius = 8.dp,
            )
            .clip(shape)
            .background(bgColor)
            .border(3.dp, borderColor, shape)
            .padding(horizontal = KutaSpacing.lg),
        enabled = !isDisabled,
        singleLine = true,
        textStyle = typography.body.copy(color = colors.fgPrimary),
        cursorBrush = SolidColor(colors.accentPrimary),
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = colors.fgMuted,
                        style = typography.body,
                    )
                }
                innerTextField()
            }
        },
    )
}

/**
 * SearchInput — input + leading Search icon.
 * Same Brutalist styling as [BrutalistInput].
 */
@Composable
fun BrutalistSearchInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
) {
    val colors = LocalKutaColors.current
    val typography = LocalKutaTypography.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val shape = RoundedCornerShape(8.dp)
    val bgColor = if (isFocused) colors.hoverBgTint else colors.bgSurface
    val shadowColor = if (isFocused) colors.accentPrimary else colors.shadowColor
    val shadowSize = if (isFocused) 3.dp else 0.dp

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .height(44.dp)
            .hardShadow(
                color = shadowColor,
                offsetX = shadowSize,
                offsetY = shadowSize,
                cornerRadius = 8.dp,
            )
            .clip(shape)
            .background(bgColor)
            .border(3.dp, colors.brutalistBorder, shape)
            .padding(horizontal = KutaSpacing.lg),
        singleLine = true,
        textStyle = typography.body.copy(color = colors.fgPrimary),
        cursorBrush = SolidColor(colors.accentPrimary),
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KutaSpacing.sm),
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = colors.fgMuted,
                    modifier = Modifier.size(18.dp),
                )
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = colors.fgMuted,
                            style = typography.body,
                        )
                    }
                    innerTextField()
                }
            }
        },
    )
}

// ===== Dialogs / Sheets (§5.5) =====

/**
 * BrutalistDialog — §5.5.
 * Custom Dialog window with 10dp corner, 3dp border, 5dp hard shadow, bgSurface.
 * Caller provides content.
 */
@Composable
fun BrutalistDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = LocalKutaColors.current
    val shape = RoundedCornerShape(10.dp)
    Dialog(onDismissRequest = onDismissRequest) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .hardShadow(
                    color = colors.shadowColor,
                    offsetX = 5.dp,
                    offsetY = 5.dp,
                    cornerRadius = 10.dp,
                )
                .clip(shape)
                .background(colors.bgSurface)
                .border(3.dp, colors.brutalistBorder, shape)
                .padding(KutaSpacing.xl),
        ) {
            content()
        }
    }
}

/**
 * AlertDialog — Brutalist-styled alert with title/message + confirm/dismiss buttons.
 */
@Composable
fun BrutalistAlertDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = "OK",
    dismissText: String = "Cancel",
) {
    val colors = LocalKutaColors.current
    val typography = LocalKutaTypography.current
    val shape = RoundedCornerShape(10.dp)
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .hardShadow(
                    color = colors.shadowColor,
                    offsetX = 5.dp,
                    offsetY = 5.dp,
                    cornerRadius = 10.dp,
                )
                .clip(shape)
                .background(colors.bgSurface)
                .border(3.dp, colors.brutalistBorder, shape)
                .padding(KutaSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(KutaSpacing.md),
        ) {
            Text(
                text = title.uppercase(),
                color = colors.fgPrimary,
                style = typography.title,
            )
            Text(
                text = message,
                color = colors.fgSecondary,
                style = typography.body,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrutalistTextButton(text = dismissText, onClick = onDismiss)
                Spacer(Modifier.width(KutaSpacing.sm))
                BrutalistButton(text = confirmText, onClick = onConfirm)
            }
        }
    }
}

/**
 * BottomSheet — wraps M3 ModalBottomSheet with Brutalist colors, 10dp top corner,
 * 3dp top border, thick drag handle.
 */
@Composable
fun BrutalistBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = LocalKutaColors.current
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = colors.bgSurface,
        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
        dragHandle = {
            // FORK: thick black drag handle — brutalist bar per §5.6 aesthetic
            Box(
                modifier = Modifier
                    .padding(vertical = KutaSpacing.sm)
                    .width(48.dp)
                    .height(4.dp)
                    .background(colors.brutalistBorder),
            )
        },
    ) {
        Column {
            // FORK: 3dp top border mimics the brutalistBorder on the sheet's top edge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(colors.brutalistBorder),
            )
            Box(modifier = Modifier.padding(KutaSpacing.lg)) {
                content()
            }
        }
    }
}

// ===== Navigation (§5.6, §5.7) =====

/**
 * NavigationBar (bottom) — §5.6.
 * 64dp height, bgSidebar, 3dp top border. Active item: accentPrimary bg, 2dp border, white icon.
 * Inactive item: fgMuted icon.
 */
@Composable
fun BrutalistNavigationBar(
    items: List<KutaNavigationItem>,
    modifier: Modifier = Modifier,
) {
    val colors = LocalKutaColors.current
    val typography = LocalKutaTypography.current
    val itemShape = RoundedCornerShape(6.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(colors.bgSidebar)
            .border(width = 3.dp, color = colors.brutalistBorder)
            .padding(KutaSpacing.xs),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            val bgColor = if (item.selected) colors.accentPrimary else Color.Transparent
            val fgColor = if (item.selected) Color.White else colors.fgMuted
            val borderColor = if (item.selected) colors.brutalistBorder else Color.Transparent
            val borderWidth = if (item.selected) 2.dp else 0.dp
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(itemShape)
                    .background(bgColor)
                    .border(borderWidth, borderColor, itemShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = item.onClick,
                    )
                    .padding(vertical = KutaSpacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(
                    item.icon,
                    contentDescription = item.label,
                    tint = fgColor,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = item.label.uppercase(),
                    color = fgColor,
                    style = typography.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * NavigationRail (side) — same styling as [BrutalistNavigationBar] but vertical.
 */
@Composable
fun BrutalistNavigationRail(
    items: List<KutaNavigationItem>,
    modifier: Modifier = Modifier,
) {
    val colors = LocalKutaColors.current
    val typography = LocalKutaTypography.current
    val itemShape = RoundedCornerShape(6.dp)

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(colors.bgSidebar)
            .border(width = 3.dp, color = colors.brutalistBorder)
            .padding(KutaSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(KutaSpacing.xs),
    ) {
        items.forEach { item ->
            val bgColor = if (item.selected) colors.accentPrimary else Color.Transparent
            val fgColor = if (item.selected) Color.White else colors.fgMuted
            val borderColor = if (item.selected) colors.brutalistBorder else Color.Transparent
            val borderWidth = if (item.selected) 2.dp else 0.dp
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(itemShape)
                    .background(bgColor)
                    .border(borderWidth, borderColor, itemShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = item.onClick,
                    )
                    .padding(vertical = KutaSpacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(
                    item.icon,
                    contentDescription = item.label,
                    tint = fgColor,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = item.label.uppercase(),
                    color = fgColor,
                    style = typography.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * TabRow — §5.7-styled: bgSidebar bg, 3dp border around the row, selected tab uses accentPrimary.
 */
@Composable
fun BrutalistTabRow(
    tabs: List<KutaTabItem>,
    modifier: Modifier = Modifier,
) {
    val colors = LocalKutaColors.current
    val typography = LocalKutaTypography.current
    val tabShape = RoundedCornerShape(6.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgSidebar)
            .border(width = 3.dp, color = colors.brutalistBorder)
            .padding(KutaSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(KutaSpacing.xs),
    ) {
        tabs.forEach { tab ->
            val bgColor = if (tab.selected) colors.accentPrimary else Color.Transparent
            val textColor = if (tab.selected) Color.White else colors.fgPrimary
            val borderColor = if (tab.selected) colors.brutalistBorder else Color.Transparent
            val borderWidth = if (tab.selected) 2.dp else 0.dp
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(tabShape)
                    .background(bgColor)
                    .border(borderWidth, borderColor, tabShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = tab.onClick,
                    )
                    .padding(vertical = KutaSpacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tab.label.uppercase(),
                    color = textColor,
                    style = typography.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ===== Items / Badges / Chips (§5.8, §5.9, §5.10) =====

/**
 * ListItem — §5.8.
 * Transparent default. Hover: hoverBgTint bg + 2dp border + 2dp shadow. Padding 14dp vertical, 16dp horizontal.
 * No dividers — borders separate items in Brutalist (per §5.8).
 * Not clickable per API — caller wraps with clickable if needed.
 */
@Composable
fun BrutalistListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val colors = LocalKutaColors.current
    val typography = LocalKutaTypography.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val shape = RoundedCornerShape(4.dp)

    val bgColor = if (isHovered) colors.hoverBgTint else Color.Transparent
    val borderColor = if (isHovered) colors.brutalistBorder else Color.Transparent
    val shadowSize = if (isHovered) 2.dp else 0.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (shadowSize > 0.dp) {
                    Modifier.hardShadow(
                        color = colors.shadowColor,
                        offsetX = shadowSize,
                        offsetY = shadowSize,
                        cornerRadius = 4.dp,
                    )
                } else {
                    Modifier
                },
            )
            .hoverable(interactionSource)
            .clip(shape)
            .background(bgColor)
            .border(2.dp, borderColor, shape)
            .padding(horizontal = KutaSpacing.lg, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = colors.fgPrimary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(KutaSpacing.lg))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title.uppercase(),
                color = colors.fgPrimary,
                style = typography.subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = colors.fgMuted,
                    style = typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(KutaSpacing.sm))
            trailing()
        }
    }
}

/**
 * Badge — §5.10.
 * Bright accent bg (yellow default, or other variant), 2dp border, 4dp corner, -2deg rotation.
 */
@Composable
fun BrutalistBadge(
    text: String,
    modifier: Modifier = Modifier,
    variant: KutaBadgeVariant = KutaBadgeVariant.DEFAULT,
) {
    val colors = LocalKutaColors.current
    val typography = LocalKutaTypography.current
    val shape = RoundedCornerShape(4.dp)

    val bgColor = when (variant) {
        KutaBadgeVariant.DEFAULT -> colors.accentYellow
        KutaBadgeVariant.ACCENT -> colors.accentPrimary
        KutaBadgeVariant.WARNING -> colors.accentOrange
        KutaBadgeVariant.ERROR -> colors.accentRed
    }
    // FORK: dark text on bright bg per §5.10
    val textColor = colors.fgPrimary

    Box(
        modifier = modifier
            .hardShadow(
                color = colors.shadowColor,
                offsetX = 2.dp,
                offsetY = 2.dp,
                cornerRadius = 4.dp,
            )
            .clip(shape)
            .background(bgColor)
            .border(2.dp, colors.brutalistBorder, shape)
            .padding(horizontal = KutaSpacing.sm, vertical = 2.dp)
            .rotate(-2f), // FORK: hand-pinned look per §5.10
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            color = textColor,
            style = typography.label,
        )
    }
}

/**
 * Chip — §5.9.
 * bgSurface default (accentPrimary when selected), 2dp border, 6dp corner, -1deg rotation.
 */
@Composable
fun BrutalistChip(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val colors = LocalKutaColors.current
    val typography = LocalKutaTypography.current
    val shape = RoundedCornerShape(6.dp)
    val bgColor = if (selected) colors.accentPrimary else colors.bgSurface
    val textColor = if (selected) Color.White else colors.fgPrimary

    val interactionSource = remember { MutableInteractionSource() }
    val press = rememberBrutalistPressState(
        interactionSource,
        defaultShadow = 2.dp,
        hoverShadow = 4.dp,
        pressShadow = 1.dp,
        pressOffset = 1.dp,
        hoverOffset = (-1).dp,
    )

    Box(
        modifier = modifier
            .offset(x = press.offset, y = press.offset)
            .hardShadow(
                color = colors.shadowColor,
                offsetX = press.shadowSize,
                offsetY = press.shadowSize,
                cornerRadius = 6.dp,
            )
            .clip(shape)
            .background(bgColor)
            .border(2.dp, colors.brutalistBorder, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = KutaSpacing.sm, vertical = KutaSpacing.xs)
            .rotate(-1f), // FORK: raw look per §5.9
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            color = textColor,
            style = typography.label,
        )
    }
}

// ===== Controls =====

/**
 * Toggle — §5.4 Settings guidance.
 * SWITCH: custom 50x28 track + 20dp thumb, 3dp border, hard shadow.
 * CHECKBOX: 24dp square with 3dp border, accentPrimary bg + check icon when checked.
 */
@Composable
fun BrutalistToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    style: KutaToggleStyle = KutaToggleStyle.SWITCH,
) {
    val colors = LocalKutaColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val press = rememberBrutalistPressState(
        interactionSource,
        defaultShadow = 2.dp,
        hoverShadow = 3.dp,
        pressShadow = 1.dp,
        pressOffset = 1.dp,
        hoverOffset = 0.dp,
    )

    when (style) {
        KutaToggleStyle.SWITCH -> {
            val trackShape = RoundedCornerShape(50)
            val thumbShape = CircleShape
            val trackColor = if (checked) colors.accentPrimary else colors.bgElevated
            val thumbColor = if (checked) Color.White else colors.fgMuted
            val thumbSize = 20.dp
            val thumbTarget = if (checked) 24.dp else 4.dp
            val thumbOffset by animateDpAsState(thumbTarget, label = "br-toggle-thumb")
            Box(
                modifier = modifier
                    .offset(x = press.offset, y = press.offset)
                    .hardShadow(
                        color = colors.shadowColor,
                        offsetX = press.shadowSize,
                        offsetY = press.shadowSize,
                        cornerRadius = 50.dp,
                    )
                    .size(width = 50.dp, height = 28.dp)
                    .clip(trackShape)
                    .background(trackColor)
                    .border(3.dp, colors.brutalistBorder, trackShape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onCheckedChange(!checked) },
                    ),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = thumbOffset)
                        .size(thumbSize)
                        .clip(thumbShape)
                        .background(thumbColor)
                        .border(2.dp, colors.brutalistBorder, thumbShape),
                )
            }
        }
        KutaToggleStyle.CHECKBOX -> {
            val boxShape = RoundedCornerShape(4.dp)
            val bgColor = if (checked) colors.accentPrimary else Color.Transparent
            Box(
                modifier = modifier
                    .offset(x = press.offset, y = press.offset)
                    .size(24.dp)
                    .hardShadow(
                        color = colors.shadowColor,
                        offsetX = press.shadowSize,
                        offsetY = press.shadowSize,
                        cornerRadius = 4.dp,
                    )
                    .clip(boxShape)
                    .background(bgColor)
                    .border(3.dp, colors.brutalistBorder, boxShape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onCheckedChange(!checked) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (checked) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

/**
 * Slider — wraps M3 Slider with Brutalist accent color overrides.
 * (M3 Slider's thumb/track geometry is hard to fully replace without losing drag behavior.)
 */
@Composable
fun BrutalistSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
) {
    val colors = LocalKutaColors.current
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        valueRange = valueRange,
        colors = SliderDefaults.colors(
            thumbColor = colors.accentPrimary,
            activeTrackColor = colors.accentPrimary,
            inactiveTrackColor = colors.bgElevated,
        ),
    )
}

/**
 * ProgressBar — determinate: custom Box with 12dp height, 3dp border, accentPrimary fill.
 * Indeterminate: M3 fallback with Brutalist colors.
 */
@Composable
fun BrutalistProgressBar(
    progress: Float?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalKutaColors.current
    val shape = RoundedCornerShape(4.dp)
    if (progress != null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(shape)
                .background(colors.bgElevated)
                .border(3.dp, colors.brutalistBorder, shape),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(colors.accentPrimary),
            )
        }
    } else {
        // FORK: indeterminate — wrap M3 with brutalist border; animating our own is overkill
        LinearProgressIndicator(
            modifier = modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(shape)
                .border(3.dp, colors.brutalistBorder, shape),
            color = colors.accentPrimary,
            trackColor = colors.bgElevated,
        )
    }
}

// ===== Feedback =====

/**
 * Snackbar — bgSurface, 3dp border, 4dp hard shadow, 8dp corner.
 * Action label is a small BrutalistButton-style chip.
 */
@Composable
fun BrutalistSnackbar(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = LocalKutaColors.current
    val typography = LocalKutaTypography.current
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .hardShadow(
                color = colors.shadowColor,
                offsetX = 4.dp,
                offsetY = 4.dp,
                cornerRadius = 8.dp,
            )
            .clip(shape)
            .background(colors.bgSurface)
            .border(3.dp, colors.brutalistBorder, shape)
            .padding(KutaSpacing.lg),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = message,
                color = colors.fgPrimary,
                style = typography.body,
                modifier = Modifier.weight(1f),
            )
            if (actionLabel != null && onAction != null) {
                val actionShape = RoundedCornerShape(6.dp)
                Box(
                    modifier = Modifier
                        .padding(start = KutaSpacing.sm)
                        .clip(actionShape)
                        .background(colors.accentPrimary)
                        .border(2.dp, colors.brutalistBorder, actionShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onAction,
                        )
                        .padding(horizontal = KutaSpacing.sm, vertical = KutaSpacing.xs),
                ) {
                    Text(
                        text = actionLabel.uppercase(),
                        color = Color.White,
                        style = typography.label,
                    )
                }
            }
        }
    }
}

/**
 * DropdownMenu — wraps M3 DropdownMenu with Brutalist bg + 3dp border + uppercase text.
 */
@Composable
fun BrutalistDropdownMenu(
    items: List<KutaDropdownItem>,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
) {
    val colors = LocalKutaColors.current
    val typography = LocalKutaTypography.current
    val menuShape = RoundedCornerShape(8.dp)
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
            .clip(menuShape)
            .background(colors.bgSurface)
            .border(3.dp, colors.brutalistBorder, menuShape),
    ) {
        items.forEach { item ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = item.label.uppercase(),
                        color = colors.fgPrimary,
                        style = typography.body,
                    )
                },
                onClick = { item.onClick(); onDismissRequest() },
                leadingIcon = if (item.icon != null) {
                    { Icon(item.icon, contentDescription = null, tint = colors.fgPrimary) }
                } else {
                    null
                },
            )
        }
    }
}

// ===== Layout =====

/**
 * Scaffold — wraps M3 Scaffold with the Brutalist grid canvas behind the content.
 * Per §6.2: the grid is the canvas; cards sit on the grid (not the other way around).
 *
 * Implementation: outer Box paints bgBase + brutalistGrid; M3 Scaffold (transparent
 * containerColor) sits on top so its content draws over the grid.
 */
@Composable
fun BrutalistScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val colors = LocalKutaColors.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgBase),
    ) {
        // FORK: grid canvas — drawn behind all content per §6.2
        Box(
            modifier = Modifier
                .fillMaxSize()
                .brutalistGrid(gridColor = colors.gridLineColor),
        )
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent, // let the grid show through
            contentColor = colors.fgPrimary,
            topBar = topBar,
            bottomBar = bottomBar,
            floatingActionButton = floatingActionButton,
        ) { padding ->
            content(padding)
        }
    }
}

/**
 * TopAppBar — §5.7.
 * bgSidebar, 3dp bottom border, 56dp height, uppercase headline title.
 * Back button: 40dp box with 2dp border, accentPrimary bg on hover.
 */
@Composable
fun BrutalistTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    val colors = LocalKutaColors.current
    val typography = LocalKutaTypography.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(colors.bgSidebar)
            .border(width = 3.dp, color = colors.brutalistBorder)
            .padding(horizontal = KutaSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            val backInteraction = remember { MutableInteractionSource() }
            val isHovered by backInteraction.collectIsHoveredAsState()
            val backShape = RoundedCornerShape(6.dp)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(backShape)
                    .background(if (isHovered) colors.accentPrimary else Color.Transparent)
                    .border(2.dp, colors.brutalistBorder, backShape)
                    .clickable(
                        interactionSource = backInteraction,
                        indication = null,
                        onClick = onBack,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = if (isHovered) Color.White else colors.fgPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(KutaSpacing.lg))
        }
        Text(
            text = title.uppercase(),
            color = colors.fgPrimary,
            style = typography.headline,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        actions()
    }
}

/**
 * BottomAppBar — bgSidebar, 3dp top border, 56dp height.
 */
@Composable
fun BrutalistBottomAppBar(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = LocalKutaColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(colors.bgSidebar)
            .border(width = 3.dp, color = colors.brutalistBorder)
            .padding(horizontal = KutaSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

/**
 * FAB — accentPrimary bg, 3dp border, 10dp corner, 4dp hard shadow.
 * text=null → 56dp square icon FAB; text!=null → extended FAB with icon + uppercase text.
 */
@Composable
fun BrutalistFAB(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
) {
    val colors = LocalKutaColors.current
    val typography = LocalKutaTypography.current
    val interactionSource = remember { MutableInteractionSource() }
    val press = rememberBrutalistPressState(
        interactionSource,
        defaultShadow = 4.dp,
        hoverShadow = 6.dp,
        pressShadow = 2.dp,
    )

    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier = modifier
            .offset(x = press.offset, y = press.offset)
            .hardShadow(
                color = colors.shadowColor,
                offsetX = press.shadowSize,
                offsetY = press.shadowSize,
                cornerRadius = 10.dp,
            )
            .clip(shape)
            .background(colors.accentPrimary)
            .border(3.dp, colors.brutalistBorder, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (text != null) {
            Row(
                modifier = Modifier.padding(horizontal = KutaSpacing.lg, vertical = KutaSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KutaSpacing.sm),
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = text.uppercase(),
                    color = Color.White,
                    style = typography.button,
                )
            }
        } else {
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

// ===== Misc (§5.11) =====

/**
 * Skeleton — §5.11.
 * bgElevated fill, 2dp DASHED brutalistBorder (raw, unfinished look), 10dp corner, pulse shimmer.
 */
@Composable
fun BrutalistSkeleton(
    modifier: Modifier = Modifier,
    height: Int = 48,
) {
    val colors = LocalKutaColors.current
    // FORK: subtle pulse shimmer (alpha 0.6 ↔ 1.0) — cheap stand-in for the diagonal sweep per §5.11
    val transition = rememberInfiniteTransition(label = "brutalist-skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "brutalist-skeleton-alpha",
    )
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .dashedBorder(width = 2.dp, color = colors.brutalistBorder, cornerRadius = 10.dp)
            .clip(shape)
            .background(colors.bgElevated.copy(alpha = alpha)),
    )
}

/**
 * Divider — solid 3dp brutalistBorder line (no M3 soft divider).
 */
@Composable
fun BrutalistDivider(
    modifier: Modifier = Modifier,
) {
    val colors = LocalKutaColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(colors.brutalistBorder),
    )
}

/**
 * Avatar — circular, 3dp border, hard shadow.
 * Caller provides content (image, initials, etc.).
 */
@Composable
fun BrutalistAvatar(
    modifier: Modifier = Modifier,
    size: Int = 40,
    content: @Composable () -> Unit,
) {
    val colors = LocalKutaColors.current
    Box(
        modifier = modifier
            .size(size.dp)
            .hardShadow(
                color = colors.shadowColor,
                offsetX = 3.dp,
                offsetY = 3.dp,
                cornerRadius = (size / 2).dp,
            )
            .clip(CircleShape)
            .background(colors.bgElevated)
            .border(3.dp, colors.brutalistBorder, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

// ===== Brutalist-specific extras (not in Material3Components.kt — opt-in) =====

/**
 * SectionHeader — §3.3.
 * 4dp wide accent bar on the left + uppercase title typography. Used in settings/library sections.
 * Not part of the Kuta* delegation; callers can use directly when in Brutalist design.
 */
@Composable
fun BrutalistSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalKutaColors.current
    val typography = LocalKutaTypography.current
    Row(
        modifier = modifier
            .padding(start = KutaSpacing.lg, top = KutaSpacing.lg, bottom = KutaSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // FORK: 4dp accent bar with hard shadow per §3.3
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(24.dp)
                .hardShadow(
                    color = colors.shadowColor,
                    offsetX = 2.dp,
                    offsetY = 2.dp,
                    cornerRadius = 2.dp,
                )
                .background(colors.accentPrimary),
        )
        Spacer(Modifier.width(KutaSpacing.md))
        Text(
            text = text.uppercase(),
            color = colors.fgPrimary,
            style = typography.title,
        )
    }
}
