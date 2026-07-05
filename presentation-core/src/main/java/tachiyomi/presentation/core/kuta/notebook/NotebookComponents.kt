// FORK: Phase 3 — Notebook rewrite from primitives
package tachiyomi.presentation.core.kuta.notebook

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.random.Random
import tachiyomi.presentation.core.kuta.components.KutaBadgeVariant
import tachiyomi.presentation.core.kuta.components.KutaButtonVariant
import tachiyomi.presentation.core.kuta.components.KutaCardElevation
import tachiyomi.presentation.core.kuta.components.KutaDropdownItem
import tachiyomi.presentation.core.kuta.components.KutaInputVariant
import tachiyomi.presentation.core.kuta.components.KutaNavigationItem
import tachiyomi.presentation.core.kuta.components.KutaTabItem
import tachiyomi.presentation.core.kuta.components.KutaToggleStyle
import tachiyomi.presentation.core.kuta.effects.paperTexture
import tachiyomi.presentation.core.kuta.theme.KutaColors
import tachiyomi.presentation.core.kuta.theme.KutaFonts
import tachiyomi.presentation.core.kuta.theme.KutaSpacing
import tachiyomi.presentation.core.kuta.theme.KutaTypography
import tachiyomi.presentation.core.kuta.theme.kutaColors
import tachiyomi.presentation.core.kuta.theme.kutaTypography

/**
 * FORK: Phase 3 — Notebook component implementations built from Compose
 * primitives (Box / Row / Column / BasicText / Canvas / BasicTextField).
 *
 * Per DOCS/design-system/02-notebook.md §5. This file deliberately avoids
 * `androidx.compose.material3.*` UI components — every surface, button, dialog,
 * input, nav item, tab, toggle, slider, snackbar, dropdown, etc. is rebuilt
 * from primitives so Notebook reads as a cozy paper journal, not "M3 with
 * brown colors".
 *
 * The ONLY material-* symbols used are:
 *   - `androidx.compose.material.icons.Icons.*` (icon ImageVector definitions;
 *     rendered via `androidx.compose.foundation.Image` + `rememberVectorPainter`).
 *
 * Each public function preserves the exact signature from Phase 2B so
 * [tachiyomi.presentation.core.kuta.components.KutaComponents] keeps working
 * unchanged.
 *
 * Design principles applied throughout (per §1, §5, §6, §7):
 *   - Warm earth-tone palette read from [kutaColors] (composition local).
 *   - Paper texture via [Modifier.paperTexture] on cards / paper surfaces.
 *   - Soft warm shadows via the private [Modifier.notebookShadow] helper
 *     (uses `kutaColors.paperShadow` for ambient/spot so dark mode is right).
 *   - Slight rotations (-0.3° hover, -1°..-2° sticky-note/chip) for
 *     hand-placed feel.
 *   - Caveat hand-written font (via [KutaFonts.Caveat] / typography.handWritten
 *     or display) for hero / dialog titles, sticky notes.
 *   - Dashed dividers + dashed borders for "torn paper" feel.
 *   - Sticky-note-styled badges/snackbars (stickyNote bg + slight rotation).
 *   - Press: lift down 1dp + shadow shrink; Hover: lift up 2dp + rotate -0.3°.
 *
 * NOTE: text rendering uses [BasicText] (foundation) instead of M3 `Text`.
 * [BasicText] takes color via `style.color`, so call sites use
 * `style = typography.foo.copy(color = …)` instead of a separate `color = …`
 * parameter.
 */

// ===== Helpers =====

/**
 * FORK: Adaptive paper-texture dot color. Light-mode uses dark warm dots,
 * dark-mode uses light warm dots. Spec §6.1 says 6% alpha for light and 4% for
 * dark — we approximate with a single 6% alpha applied to [KutaColors.fgPrimary]
 * (which is dark in light mode, light in dark mode).
 */
private val paperDotColor: Color
    @Composable get() = kutaColors.fgPrimary.copy(alpha = 0.06f)

/**
 * Washi-tape decoration — a semi-transparent strip rotated -2°.
 * Per 02-notebook.md §6.4. Caller places it (e.g. at top of a card).
 */
@Composable
fun WashiTape(modifier: Modifier = Modifier, color: Color? = null) {
    val colors = kutaColors
    Box(
        modifier = modifier
            .width(80.dp)
            .height(18.dp)
            .rotate(-2f)
            .clip(RoundedCornerShape(2.dp))
            .background(color ?: colors.washiTape),
    )
}

/**
 * FORK: warm paper-shadow with consistent ambient/spot colors taken from the
 * active [KutaColors.paperShadow] token. Centralizes [Modifier.shadow] so all
 * Notebook surfaces share the same warm shadow tone (brown in light mode,
 * near-black in dark mode — per §2.3).
 *
 * Returns `this` unchanged when elevation is zero so callers can blindly chain.
 */
private fun Modifier.notebookShadow(
    elevation: Dp,
    shape: Shape = RoundedCornerShape(10.dp),
    colors: KutaColors,
): Modifier {
    if (elevation <= 0.dp) return this
    val ambient = colors.paperShadow
    // Spot slightly stronger than ambient (per spec: hover shadow grows).
    val spotAlpha = (colors.paperShadow.alpha * 1.4f).coerceAtMost(1f)
    return this.shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = ambient,
        spotColor = colors.paperShadow.copy(alpha = spotAlpha),
    )
}

/**
 * FORK: dashed-rectangle border used by [NotebookSkeleton]. Draws a dashed
 * round-rect outline matching the box's bounds. Per §5.10 ("Dashed border
 * looks like a sketch/placeholder").
 */
private fun Modifier.dashedRectangleBorder(
    color: Color,
    width: Dp,
    dashLength: Dp = 6.dp,
    gapLength: Dp = 4.dp,
    cornerRadius: Dp = 8.dp,
): Modifier = this.then(
    Modifier.drawBehind {
        drawRoundRect(
            color = color,
            style = Stroke(
                width = width.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(dashLength.toPx(), gapLength.toPx()),
                    0f,
                ),
            ),
            cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
        )
    },
)

/**
 * FORK: icon renderer that avoids `androidx.compose.material3.Icon`. Renders
 * an [ImageVector] via [rememberVectorPainter] + [Image] (foundation), with an
 * optional [ColorFilter.tint] for color. Per the Phase 3 brief: only M3 *UI*
 * components are off-limits; the `Icons.Filled.*` ImageVectors themselves are
 * just data and remain available.
 */
@Composable
private fun IconImage(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    Image(
        painter = rememberVectorPainter(imageVector),
        contentDescription = contentDescription,
        modifier = modifier,
        colorFilter = if (tint != Color.Unspecified) ColorFilter.tint(tint) else null,
    )
}

// ===== Buttons (per §5.1) =====

@Composable
fun NotebookButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    variant: KutaButtonVariant = KutaButtonVariant.PRIMARY,
) {
    val colors = kutaColors
    val typography = kutaTypography
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val bgColor = when (variant) {
        KutaButtonVariant.PRIMARY -> colors.accentPrimary
        KutaButtonVariant.SECONDARY -> colors.bgSurface
        KutaButtonVariant.DESTRUCTIVE -> colors.accentTertiary
        KutaButtonVariant.GHOST -> Color.Transparent
    }
    val textColor = when (variant) {
        KutaButtonVariant.PRIMARY -> colors.onAccent
        KutaButtonVariant.DESTRUCTIVE -> Color.White
        KutaButtonVariant.GHOST -> colors.fgMuted
        KutaButtonVariant.SECONDARY -> colors.accentPrimary
    }
    val borderColor = when (variant) {
        KutaButtonVariant.SECONDARY -> colors.accentPrimary
        else -> Color.Transparent
    }
    val isGhost = variant == KutaButtonVariant.GHOST
    val isElevated = !isGhost

    // FORK: hover lift + rotate per §5.1 + §7. Press overrides hover (paper
    // pressed down 1dp). Ghost has no shadow/lift.
    val lift by animateDpAsState(
        targetValue = when {
            !enabled || !isElevated -> 0.dp
            isPressed -> 1.dp
            isHovered -> (-2).dp
            else -> 0.dp
        },
        animationSpec = tween(durationMillis = 200),
        label = "nb-btn-lift",
    )
    val rotation by animateFloatAsState(
        targetValue = if (isHovered && enabled && isElevated) -0.3f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "nb-btn-rotate",
    )
    val shadowElevation by animateDpAsState(
        targetValue = when {
            !enabled || !isElevated -> 0.dp
            isPressed -> 2.dp
            isHovered -> 8.dp
            else -> 4.dp
        },
        animationSpec = tween(durationMillis = 200),
        label = "nb-btn-shadow",
    )

    Box(
        modifier = modifier
            .height(48.dp)
            .then(if (lift != 0.dp) Modifier.offset(y = lift) else Modifier)
            .then(if (rotation != 0f) Modifier.rotate(rotation) else Modifier)
            .then(
                if (isElevated && enabled) {
                    Modifier.notebookShadow(shadowElevation, RoundedCornerShape(8.dp), colors)
                } else {
                    Modifier
                },
            )
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) bgColor else bgColor.copy(alpha = 0.4f))
            .then(
                if (borderColor != Color.Transparent) {
                    Modifier.border(1.dp, borderColor, RoundedCornerShape(8.dp))
                } else {
                    Modifier
                },
            )
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                onClick = onClick,
            )
            .hoverable(interactionSource)
            .padding(horizontal = KutaSpacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KutaSpacing.sm),
        ) {
            if (icon != null) {
                IconImage(
                    icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(18.dp),
                )
            }
            BasicText(text, style = typography.button.copy(color = textColor))
        }
    }
}

@Composable
fun NotebookOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    // FORK: outlined = SECONDARY variant per 02-notebook.md §5.1.
    NotebookButton(text, onClick, modifier, enabled, icon, KutaButtonVariant.SECONDARY)
}

@Composable
fun NotebookTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    // FORK: text-only = GHOST variant per 02-notebook.md §5.1.
    NotebookButton(text, onClick, modifier, enabled, icon, KutaButtonVariant.GHOST)
}

@Composable
fun NotebookIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    val colors = kutaColors
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    // FORK: subtle bg tint on hover for tactile paper feel (bgElevated @ 50%).
    val bgAlpha by animateFloatAsState(
        targetValue = if (isHovered && enabled) 0.5f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "nb-icon-btn-tint",
    )
    val iconTint = if (isHovered && enabled) colors.accentPrimary
    else if (enabled) colors.fgSecondary else colors.fgDim

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(colors.bgElevated.copy(alpha = bgAlpha * 0.5f))
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                onClick = onClick,
            )
            .hoverable(interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        IconImage(
            icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(20.dp),
        )
    }
}

// ===== Cards (per §5.2) =====

@Composable
fun NotebookCard(
    modifier: Modifier = Modifier,
    elevation: KutaCardElevation = KutaCardElevation.FLAT,
    content: @Composable () -> Unit,
) {
    val colors = kutaColors
    val isElevated = elevation == KutaCardElevation.ELEVATED
    val interactionSource = remember { MutableInteractionSource() }
    // FORK: hoverable (not clickable) — cards are styled containers; consumers
    // wrap with their own clickable when needed. Hover still tracked for
    // desktop/foldable devices per spec §7.
    val isHovered by interactionSource.collectIsHoveredAsState()

    val lift by animateDpAsState(
        targetValue = if (isHovered && isElevated) (-3).dp else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "nb-card-lift",
    )
    val rotation by animateFloatAsState(
        targetValue = if (isHovered && isElevated) -0.3f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "nb-card-rotate",
    )
    val shadowElevation by animateDpAsState(
        targetValue = when {
            !isElevated -> 0.dp
            isHovered -> 8.dp
            else -> 4.dp
        },
        animationSpec = tween(durationMillis = 200),
        label = "nb-card-shadow",
    )

    Box(
        modifier = modifier
            .hoverable(interactionSource)
            .then(if (lift != 0.dp) Modifier.offset(y = lift) else Modifier)
            .then(if (rotation != 0f) Modifier.rotate(rotation) else Modifier)
            .notebookShadow(shadowElevation, RoundedCornerShape(10.dp), colors)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.bgPaper)
            .paperTexture(dotColor = paperDotColor)
            .border(1.dp, colors.borderDefault, RoundedCornerShape(10.dp)),
    ) {
        content()
    }
}

@Composable
fun NotebookElevatedCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    NotebookCard(modifier, KutaCardElevation.ELEVATED, content)
}

// ===== Inputs (per §5.3) =====

@Composable
fun NotebookInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    variant: KutaInputVariant = KutaInputVariant.DEFAULT,
) {
    val colors = kutaColors
    val typography = kutaTypography
    val isError = variant == KutaInputVariant.ERROR
    val isDisabled = variant == KutaInputVariant.DISABLED || !enabled
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val borderColor = when {
        isDisabled -> colors.borderSubtle
        isError -> colors.accentTertiary
        isFocused -> colors.accentPrimary
        else -> colors.borderDefault
    }
    val bgColor = if (isDisabled) colors.bgElevated else colors.bgSurface
    val textColor = if (isDisabled) colors.fgDim else colors.fgPrimary
    val cursorColor = if (isError) colors.accentTertiary else colors.accentPrimary

    // FORK: subtle paperShadow on focus for tactile "lifted off the page" feel.
    val shadowElevation by animateDpAsState(
        targetValue = if (isFocused && !isError && !isDisabled) 2.dp else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "nb-input-shadow",
    )

    Box(
        modifier = modifier
            .height(44.dp)
            .notebookShadow(shadowElevation, RoundedCornerShape(8.dp), colors)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = KutaSpacing.md),
        contentAlignment = Alignment.CenterStart,
    ) {
        // FORK: BasicTextField (NOT M3 OutlinedTextField) + decorationBox with
        // a placeholder overlay when value is empty. Per §5.3.
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isDisabled,
            textStyle = typography.body.copy(color = textColor),
            singleLine = true,
            interactionSource = interactionSource,
            cursorBrush = SolidColor(cursorColor),
            visualTransformation = VisualTransformation.None,
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        BasicText(
                            placeholder,
                            style = typography.body.copy(
                                fontStyle = FontStyle.Italic,
                                color = colors.fgDim,
                            ),
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
fun NotebookSearchInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
) {
    val colors = kutaColors
    val typography = kutaTypography
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val borderColor = if (isFocused) colors.accentPrimary else colors.borderDefault
    val cursorColor = colors.accentPrimary
    val textColor = colors.fgPrimary
    val iconTint = if (isFocused) colors.accentPrimary else colors.fgMuted

    val shadowElevation by animateDpAsState(
        targetValue = if (isFocused) 2.dp else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "nb-search-shadow",
    )

    Box(
        modifier = modifier
            .height(44.dp)
            .notebookShadow(shadowElevation, RoundedCornerShape(8.dp), colors)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.bgSurface)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = KutaSpacing.md),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KutaSpacing.sm),
        ) {
            IconImage(
                Icons.Filled.Search,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp),
            )
            // FORK: BasicTextField takes the remaining width (weight 1f).
            Box(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = typography.body.copy(color = textColor),
                    singleLine = true,
                    interactionSource = interactionSource,
                    cursorBrush = SolidColor(cursorColor),
                    visualTransformation = VisualTransformation.None,
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (value.isEmpty()) {
                                BasicText(
                                    placeholder,
                                    style = typography.body.copy(
                                        fontStyle = FontStyle.Italic,
                                        color = colors.fgDim,
                                    ),
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }
        }
    }
}

// ===== Dialogs / Sheets (per §5.4) =====

@Composable
fun NotebookDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = kutaColors
    // FORK: androidx.compose.ui.window.Dialog (NOT M3 AlertDialog) so we
    // fully control the surface. usePlatformDefaultWidth = false lets us
    // impose the 560dp max-width per §5.4.
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .notebookShadow(8.dp, RoundedCornerShape(12.dp), colors)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.bgPaper)
                .paperTexture(dotColor = paperDotColor)
                .border(1.dp, colors.borderStrong, RoundedCornerShape(12.dp)),
        ) {
            content()
        }
    }
}

@Composable
fun NotebookAlertDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = "OK",
    dismissText: String = "Cancel",
) {
    val typography = kutaTypography
    val colors = kutaColors
    NotebookDialog(onDismissRequest = onDismiss, modifier = modifier) {
        Column(
            modifier = Modifier.padding(KutaSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(KutaSpacing.md),
        ) {
            // FORK: dialog title in Caveat (hand-written) for cozy feel
            // per 02-notebook.md §8.4 ("Section headers… in Caveat for cozy feel").
            BasicText(
                title,
                style = typography.headline.copy(
                    fontFamily = KutaFonts.Caveat,
                    fontSize = 32.sp,
                    color = colors.fgPrimary,
                ),
            )
            BasicText(
                message,
                style = typography.body.copy(color = colors.fgSecondary),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = KutaSpacing.sm),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NotebookTextButton(dismissText, onDismiss)
                Spacer(Modifier.width(KutaSpacing.sm))
                NotebookButton(confirmText, onConfirm)
            }
        }
    }
}

@Composable
fun NotebookBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = kutaColors
    // FORK: Dialog with usePlatformDefaultWidth = false + bottom-aligned Box
    // replaces M3 ModalBottomSheet (per Phase 3 brief). Top corner radius 16dp
    // + paper texture + paperShadow + borderStrong + drag handle pill.
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.BottomCenter),
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .notebookShadow(8.dp, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp), colors)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(colors.bgPaper)
                    .paperTexture(dotColor = paperDotColor)
                    .border(
                        1.dp,
                        colors.borderStrong,
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    )
                    .navigationBarsPadding(),
            ) {
                // FORK: drag handle — a small accentPrimary pill at top center.
                Box(
                    modifier = Modifier
                        .padding(vertical = KutaSpacing.sm)
                        .align(Alignment.CenterHorizontally)
                        .width(32.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.accentPrimary.copy(alpha = 0.7f)),
                )
                content()
            }
        }
    }
}

// ===== Navigation (per §5.5) =====

@Composable
fun NotebookNavigationBar(
    items: List<KutaNavigationItem>,
    modifier: Modifier = Modifier,
) {
    val colors = kutaColors
    val typography = kutaTypography
    // FORK: Row of icon+label Columns — NO M3 NavigationBar/NavigationBarItem.
    // bgSurface + paperTexture + top borderDefault; 64dp tall + nav inset.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(colors.bgSurface)
            .paperTexture(dotColor = colors.fgPrimary.copy(alpha = 0.04f))
            .navigationBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        items.forEach { item ->
            NotebookNavItem(item, colors, typography)
        }
    }
}

@Composable
private fun NotebookNavItem(
    item: KutaNavigationItem,
    colors: KutaColors,
    typography: KutaTypography,
) {
    // FORK: ink underline beneath active label per §5.5.
    val tint = if (item.selected) colors.accentPrimary else colors.fgMuted
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .padding(horizontal = KutaSpacing.sm)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = item.onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        IconImage(
            item.icon,
            contentDescription = item.label,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
        BasicText(
            item.label,
            style = typography.bodySmall.copy(color = tint),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        // Ink underline — small accent bar beneath active label.
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(2.dp)
                .then(
                    if (item.selected) {
                        Modifier.background(colors.accentPrimary.copy(alpha = 0.7f))
                    } else {
                        Modifier.background(Color.Transparent)
                    },
                ),
        )
    }
}

@Composable
fun NotebookNavigationRail(
    items: List<KutaNavigationItem>,
    modifier: Modifier = Modifier,
) {
    val colors = kutaColors
    val typography = kutaTypography
    // FORK: Column of items in bgSidebar with right borderDefault — NO M3
    // NavigationRail/NavigationRailItem. Active item gets ink underline.
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(colors.bgSidebar)
            .paperTexture(dotColor = colors.fgPrimary.copy(alpha = 0.04f))
            .border(1.dp, colors.borderDefault, RoundedCornerShape(0.dp))
            .padding(vertical = KutaSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(KutaSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items.forEach { item ->
            NotebookNavItem(item, colors, typography)
        }
    }
}

@Composable
fun NotebookTabRow(
    tabs: List<KutaTabItem>,
    modifier: Modifier = Modifier,
) {
    val colors = kutaColors
    val typography = kutaTypography
    // FORK: Row of Box+BasicText tabs with bottom borderSubtle divider; active
    // tab gets accentPrimary bottom indicator (2dp, slightly rotated). NO M3
    // TabRow / Tab.
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bgSurface)
                .height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                val interactionSource = remember { MutableInteractionSource() }
                val textColor = if (tab.selected) colors.accentPrimary else colors.fgMuted
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = LocalIndication.current,
                            onClick = tab.onClick,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        BasicText(
                            tab.label,
                            style = typography.subtitle.copy(color = textColor),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        // FORK: ink-underline indicator — accent bar with -0.3°
                        // rotation (per Phase 3 brief). Active only.
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(2.dp)
                                .rotate(-0.3f)
                                .then(
                                    if (tab.selected) {
                                        Modifier.background(colors.accentPrimary)
                                    } else {
                                        Modifier.background(Color.Transparent)
                                    },
                                ),
                        )
                    }
                }
            }
        }
        // Dashed divider between tabs and content (torn-paper feel).
        NotebookDivider()
    }
}

// ===== Items / Badges / Chips =====

@Composable
fun NotebookListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val colors = kutaColors
    val typography = kutaTypography
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    // FORK: bgElevated @ 50% on hover per §5.7.
    val bgAlpha by animateFloatAsState(
        targetValue = if (isHovered) 0.5f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "nb-list-item-hover",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgElevated.copy(alpha = bgAlpha * 0.5f))
            .hoverable(interactionSource)
            // FORK: 16dp vertical / 24dp horizontal per §5.7.
            .padding(horizontal = KutaSpacing.xl, vertical = KutaSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            IconImage(
                icon,
                contentDescription = null,
                tint = colors.fgSecondary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(KutaSpacing.lg))
        }
        Column(Modifier.weight(1f)) {
            BasicText(title, style = typography.subtitle.copy(color = colors.fgPrimary))
            if (subtitle != null) {
                // FORK: italic subtitle mimics hand-written metadata per §8.1.
                BasicText(
                    subtitle,
                    style = typography.bodySmall.copy(
                        fontStyle = FontStyle.Italic,
                        color = colors.fgMuted,
                    ),
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(KutaSpacing.sm))
            trailing()
        }
    }
}

@Composable
fun NotebookBadge(
    text: String,
    modifier: Modifier = Modifier,
    variant: KutaBadgeVariant = KutaBadgeVariant.DEFAULT,
) {
    val colors = kutaColors
    val typography = kutaTypography
    val bgColor = when (variant) {
        KutaBadgeVariant.DEFAULT -> colors.stickyNote
        KutaBadgeVariant.ACCENT -> colors.accentPrimary.copy(alpha = 0.15f)
        KutaBadgeVariant.WARNING -> colors.accentQuaternary.copy(alpha = 0.2f)
        KutaBadgeVariant.ERROR -> colors.accentTertiary.copy(alpha = 0.2f)
    }
    val textColor = when (variant) {
        KutaBadgeVariant.DEFAULT -> colors.fgPrimary
        KutaBadgeVariant.ACCENT -> colors.accentPrimary
        KutaBadgeVariant.WARNING -> colors.accentQuaternary
        KutaBadgeVariant.ERROR -> colors.accentTertiary
    }
    // FORK: random -2..+2 deg rotation per-instance — looks hand-pinned (§5.9).
    val rotation = remember { Random.nextFloat() * 4f - 2f }

    Box(
        modifier = modifier
            .rotate(rotation)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(4.dp))
            .padding(horizontal = KutaSpacing.sm, vertical = KutaSpacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            // FORK: small pin dot at left for sticky-note feel (only on DEFAULT
            // variant — colored badges already convey their semantics).
            if (variant == KutaBadgeVariant.DEFAULT) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(colors.accentTertiary.copy(alpha = 0.7f)),
                )
            }
            BasicText(text, style = typography.label.copy(color = textColor))
        }
    }
}

@Composable
fun NotebookChip(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val colors = kutaColors
    val typography = kutaTypography
    // FORK: per §5.8 — accent at 15% bg / 30% border when selected; rotation -1°.
    // Unselected uses muted bgElevated + fgMuted text.
    val bgColor = if (selected) colors.accentPrimary.copy(alpha = 0.15f) else colors.bgElevated
    val borderColor = if (selected) colors.accentPrimary.copy(alpha = 0.3f) else colors.borderDefault
    val textColor = if (selected) colors.accentPrimary else colors.fgMuted
    val rotation = if (selected) -1f else 0f

    val interactionSource = remember { MutableInteractionSource() }
    val chipModifier = if (onClick != null) {
        modifier.clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            onClick = onClick,
        )
    } else {
        modifier
    }

    Box(
        modifier = chipModifier
            .rotate(rotation)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = KutaSpacing.md, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = typography.label.copy(color = textColor))
    }
}

// ===== Controls =====

@Composable
fun NotebookToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    style: KutaToggleStyle = KutaToggleStyle.SWITCH,
) {
    val colors = kutaColors
    val interactionSource = remember { MutableInteractionSource() }
    if (style == KutaToggleStyle.SWITCH) {
        // FORK: custom track (48dp × 24dp) + thumb (20dp circle). accentPrimary
        // track when on, borderDefault when off. Animate thumb slide via
        // animateDpAsState. NO M3 Switch.
        val thumbOffset by animateDpAsState(
            targetValue = if (checked) 26.dp else 2.dp,
            animationSpec = tween(durationMillis = 200),
            label = "nb-toggle-thumb",
        )
        val trackColor = if (checked) colors.accentPrimary else colors.bgElevated
        val trackBorder = if (checked) colors.accentPrimary else colors.borderDefault
        val thumbColor = if (checked) colors.onAccent else colors.fgMuted
        Box(
            modifier = modifier
                .size(width = 48.dp, height = 24.dp)
                .clip(CircleShape)
                .background(trackColor)
                .border(1.dp, trackBorder, CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = { onCheckedChange(!checked) },
                ),
        ) {
            Box(
                Modifier
                    .offset(x = thumbOffset, y = 2.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(thumbColor),
            )
        }
    } else {
        // FORK: custom 24dp Box with 2dp accentPrimary border + 4dp corner.
        // Checkmark icon when checked. NO M3 Checkbox.
        val boxColor = if (checked) colors.accentPrimary else Color.Transparent
        val borderColor = if (checked) colors.accentPrimary else colors.borderDefault
        Box(
            modifier = modifier
                .size(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(boxColor)
                .border(2.dp, borderColor, RoundedCornerShape(4.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = { onCheckedChange(!checked) },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                IconImage(
                    Icons.Filled.Check,
                    contentDescription = "Checked",
                    tint = colors.onAccent,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
fun NotebookSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
) {
    val colors = kutaColors
    val range = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0f)
    val fraction = if (range == 0f) 0f else ((value - valueRange.start) / range).coerceIn(0f, 1f)

    // FORK: BoxWithConstraints so we know the track width in Dp — needed to
    // convert drag delta (px) → value delta, and to position the thumb.
    // NO M3 Slider.
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        val trackWidthDp = maxWidth
        val density = LocalDensity.current
        val trackWidthPx = with(density) { trackWidthDp.toPx() }
        val draggableState = rememberDraggableState { delta ->
            val fractionDelta = if (trackWidthPx > 0f) delta / trackWidthPx else 0f
            val newValue = (value + fractionDelta * range)
                .coerceIn(valueRange.start, valueRange.endInclusive)
            onValueChange(newValue)
        }

        // Track background — full width, 4dp tall, bgElevated.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.bgElevated),
        ) {
            // Filled portion — accentPrimary, fraction of width.
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(colors.accentPrimary),
            )
        }

        // Thumb — 20dp circle, accentPrimary, paperShadow. Positioned via offset.
        val thumbSize = 20.dp
        val thumbOffsetDp = (trackWidthDp - thumbSize) * fraction
        Box(
            modifier = Modifier
                .offset(x = thumbOffsetDp)
                .size(thumbSize)
                .align(Alignment.CenterStart)
                .notebookShadow(2.dp, CircleShape, colors)
                .clip(CircleShape)
                .background(colors.accentPrimary),
        )

        // Drag overlay — full-size transparent Box that captures drags.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = draggableState,
                ),
        )
    }
}

@Composable
fun NotebookProgressBar(
    progress: Float?,
    modifier: Modifier = Modifier,
) {
    val colors = kutaColors
    // FORK: indeterminate sweep uses rememberInfiniteTransition; determinate
    // uses a fixed fraction fill. Both render via Canvas so we get one draw
    // pass for track + fill (no M3 LinearProgressIndicator).
    val transition = rememberInfiniteTransition(label = "nb-progress")
    val animProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500),
            repeatMode = RepeatMode.Restart,
        ),
        label = "nb-progress-anim",
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp)),
    ) {
        // Track
        drawRoundRect(
            color = colors.bgElevated,
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
        )
        if (progress != null) {
            val width = size.width * progress.coerceIn(0f, 1f)
            drawRoundRect(
                color = colors.accentPrimary,
                size = Size(width, size.height),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            )
        } else {
            // Indeterminate — animated 30%-width sweep.
            val sweepWidth = size.width * 0.3f
            val startX = (size.width - sweepWidth) * animProgress
            drawRoundRect(
                color = colors.accentPrimary,
                topLeft = Offset(startX, 0f),
                size = Size(sweepWidth, size.height),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            )
        }
    }
}

// ===== Feedback =====

@Composable
fun NotebookSnackbar(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = kutaColors
    val typography = kutaTypography
    // FORK: sticky-note styled snackbar (per §5.11 aesthetic): yellow bg,
    // slight rotation, paper shadow, hand-pinned feel. NO M3 Snackbar.
    val rotation = remember { Random.nextFloat() * 2f - 1f }
    Box(
        modifier = modifier
            .rotate(rotation)
            .notebookShadow(6.dp, RoundedCornerShape(8.dp), colors)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.stickyNote)
            .paperTexture(dotColor = paperDotColor)
            .border(1.dp, colors.borderDefault, RoundedCornerShape(8.dp))
            .padding(horizontal = KutaSpacing.lg, vertical = KutaSpacing.md),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KutaSpacing.md),
        ) {
            BasicText(
                message,
                modifier = Modifier.weight(1f),
                style = typography.body.copy(color = colors.fgPrimary),
            )
            if (actionLabel != null) {
                NotebookTextButton(
                    text = actionLabel,
                    onClick = { onAction?.invoke() },
                )
            }
        }
    }
}

@Composable
fun NotebookDropdownMenu(
    items: List<KutaDropdownItem>,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
) {
    val colors = kutaColors
    val typography = kutaTypography
    // FORK: androidx.compose.ui.window.Popup (NOT M3 DropdownMenu). When
    // expanded == false, render nothing. NO M3 DropdownMenuItem — each item is
    // a Row with hover bg + clickable.
    if (!expanded) return
    Popup(
        alignment = Alignment.TopCenter,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier = modifier
                .widthIn(max = 280.dp)
                .notebookShadow(6.dp, RoundedCornerShape(8.dp), colors)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.bgPaper)
                .paperTexture(dotColor = paperDotColor)
                .border(1.dp, colors.borderDefault, RoundedCornerShape(8.dp))
                .padding(vertical = KutaSpacing.xs),
        ) {
            items.forEach { item ->
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isHovered) colors.accentPrimary.copy(alpha = 0.1f) else Color.Transparent,
                        )
                        .hoverable(interactionSource)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = LocalIndication.current,
                            onClick = {
                                item.onClick()
                                onDismissRequest()
                            },
                        )
                        .padding(horizontal = KutaSpacing.lg, vertical = KutaSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(KutaSpacing.md),
                ) {
                    if (item.icon != null) {
                        IconImage(
                            item.icon,
                            contentDescription = null,
                            tint = colors.fgSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    BasicText(
                        item.label,
                        style = typography.body.copy(color = colors.fgPrimary),
                    )
                }
            }
        }
    }
}

// ===== Layout =====

@Composable
fun NotebookScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val colors = kutaColors
    // FORK: Column (bgBase bg) with topBar slot, content (weight 1f in a Box),
    // bottomBar at bottom, FAB overlaid. NO M3 Scaffold. Paper texture is NOT
    // applied to the whole screen (perf — would draw dots across the entire
    // surface); child surfaces bring their own texture.
    Box(modifier = modifier.fillMaxSize().background(colors.bgBase)) {
        Column(modifier = Modifier.fillMaxSize()) {
            topBar()
            Box(modifier = Modifier.weight(1f)) {
                content(PaddingValues(0.dp))
            }
            bottomBar()
        }
        // FAB overlaid at bottom-end with a cozy margin.
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = KutaSpacing.lg, bottom = KutaSpacing.lg),
        ) {
            floatingActionButton()
        }
    }
}

@Composable
fun NotebookTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    val colors = kutaColors
    val typography = kutaTypography
    // FORK: Row (bgSurface + paperTexture + bottom borderDefault + statusBar
    // inset) with nav icon slot + title BasicText + actions slot. 56dp + status
    // bar inset. NO M3 TopAppBar.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .statusBarsPadding()
            .background(colors.bgSurface)
            .paperTexture(dotColor = colors.fgPrimary.copy(alpha = 0.04f))
            .border(1.dp, colors.borderDefault, RoundedCornerShape(0.dp))
            .padding(horizontal = KutaSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            NotebookIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onClick = onBack,
                contentDescription = "Back",
            )
        } else {
            Spacer(Modifier.width(KutaSpacing.sm))
        }
        // FORK: headline typography + fgPrimary per §5.6. Caveat is reserved
        // for screen hero titles (per §8.2); here we use Inter to keep the app
        // bar readable at small sizes.
        BasicText(
            title,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = KutaSpacing.sm),
            style = typography.headline.copy(color = colors.fgPrimary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        actions()
    }
}

@Composable
fun NotebookBottomAppBar(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = kutaColors
    // FORK: Row (bgSurface + paperTexture + top borderDefault) with nav inset.
    // 56dp + navigationBars inset. NO M3 BottomAppBar.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .navigationBarsPadding()
            .background(colors.bgSurface)
            .paperTexture(dotColor = colors.fgPrimary.copy(alpha = 0.04f))
            .border(1.dp, colors.borderDefault, RoundedCornerShape(0.dp))
            .padding(horizontal = KutaSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KutaSpacing.sm),
    ) {
        content()
    }
}

@Composable
fun NotebookFAB(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
) {
    val colors = kutaColors
    val typography = kutaTypography
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    // FORK: hover lift 2dp + rotate -0.3° (per Phase 3 brief).
    val lift by animateDpAsState(
        targetValue = if (isHovered) (-2).dp else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "nb-fab-lift",
    )
    val rotation by animateFloatAsState(
        targetValue = if (isHovered) -0.3f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "nb-fab-rotate",
    )
    val shadowElevation by animateDpAsState(
        targetValue = if (isHovered) 12.dp else 8.dp,
        animationSpec = tween(durationMillis = 200),
        label = "nb-fab-shadow",
    )

    val shape = RoundedCornerShape(16.dp)
    val contentModifier = Modifier
        .then(if (lift != 0.dp) Modifier.offset(y = lift) else Modifier)
        .then(if (rotation != 0f) Modifier.rotate(rotation) else Modifier)
        .notebookShadow(shadowElevation, shape, colors)
        .clip(shape)
        .background(colors.accentPrimary)
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            onClick = onClick,
        )
        .hoverable(interactionSource)
        .padding(horizontal = KutaSpacing.lg, vertical = KutaSpacing.lg)

    if (text != null) {
        // Extended FAB — Row with icon + text.
        Row(
            modifier = modifier.then(contentModifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KutaSpacing.sm),
        ) {
            IconImage(
                icon,
                contentDescription = null,
                tint = colors.onAccent,
                modifier = Modifier.size(24.dp),
            )
            BasicText(text, style = typography.button.copy(color = colors.onAccent))
        }
    } else {
        Box(
            modifier = modifier.then(contentModifier),
            contentAlignment = Alignment.Center,
        ) {
            IconImage(
                icon,
                contentDescription = null,
                tint = colors.onAccent,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

// ===== Misc =====

@Composable
fun NotebookSkeleton(
    modifier: Modifier = Modifier,
    height: Int = 48,
) {
    val colors = kutaColors
    // FORK: bgElevated + dashed border for sketch/placeholder feel per §5.10.
    // Shimmer: warm-tinted (caramel) diagonal sweep via rememberInfiniteTransition.
    // NO M3 CircularProgressIndicator.
    val transition = rememberInfiniteTransition(label = "nb-skeleton")
    val animProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500),
            repeatMode = RepeatMode.Restart,
        ),
        label = "nb-skeleton-anim",
    )

    Box(
        modifier = modifier
            .height(height.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.bgElevated)
            .dashedRectangleBorder(colors.borderDefault, 1.dp)
            .drawWithContent {
                drawContent()
                // FORK: caramel-tinted diagonal sweep shimmer (10% alpha).
                val shimmerColor = colors.accentQuaternary.copy(alpha = 0.10f)
                val sweepWidth = size.width * 0.4f
                val sweepX = (size.width + sweepWidth) * animProgress - sweepWidth
                // Draw a translucent diagonal band as a skewed rectangle.
                val skewHeight = size.height
                val path = Path().apply {
                    moveTo(sweepX, 0f)
                    lineTo(sweepX + sweepWidth, 0f)
                    lineTo(sweepX + sweepWidth - skewHeight, size.height)
                    lineTo(sweepX - skewHeight, size.height)
                    close()
                }
                drawPath(path = path, color = shimmerColor)
            },
    )
}

@Composable
fun NotebookDivider(
    modifier: Modifier = Modifier,
) {
    val colors = kutaColors
    // FORK: 1dp DASHED borderSubtle line (torn-paper feel) per §8.4 / §5.7.
    // Built via Canvas + dashPathEffect (per Phase 3 brief).
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp),
    ) {
        val strokeWidth = 1.dp.toPx()
        drawLine(
            color = colors.borderSubtle,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = strokeWidth,
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(6.dp.toPx(), 4.dp.toPx()),
                0f,
            ),
        )
    }
}

@Composable
fun NotebookAvatar(
    modifier: Modifier = Modifier,
    size: Int = 40,
    content: @Composable () -> Unit,
) {
    val colors = kutaColors
    // FORK: circular paper-shadowed avatar frame. Border + shadow give the
    // photo-cover a "stuck on the page" feel.
    Box(
        modifier = modifier
            .size(size.dp)
            .notebookShadow(4.dp, CircleShape, colors)
            .clip(CircleShape)
            .background(colors.bgElevated)
            .border(1.dp, colors.borderDefault, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
