// FORK: Phase 2B
package tachiyomi.presentation.core.kuta.notebook

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tachiyomi.presentation.core.kuta.components.KutaBadgeVariant
import tachiyomi.presentation.core.kuta.components.KutaButtonVariant
import tachiyomi.presentation.core.kuta.components.KutaCardElevation
import tachiyomi.presentation.core.kuta.components.KutaDropdownItem
import tachiyomi.presentation.core.kuta.components.KutaInputVariant
import tachiyomi.presentation.core.kuta.components.KutaNavigationItem
import tachiyomi.presentation.core.kuta.components.KutaTabItem
import tachiyomi.presentation.core.kuta.components.KutaToggleStyle
import tachiyomi.presentation.core.kuta.effects.paperTexture
import tachiyomi.presentation.core.kuta.theme.KutaFonts
import tachiyomi.presentation.core.kuta.theme.KutaSpacing
import tachiyomi.presentation.core.kuta.theme.kutaColors
import tachiyomi.presentation.core.kuta.theme.kutaTypography

/**
 * FORK: Phase 2B — Full Notebook component implementations.
 * Per DOCS/design-system/02-notebook.md §5.
 *
 * Each function has the SAME signature as the corresponding Material3 wrapper
 * (so [tachiyomi.presentation.core.kuta.components.KutaComponents] can swap
 * implementations without changes).
 *
 * Design principles applied throughout:
 *   - Warm earth-tone palette read from [kutaColors] (composition local).
 *   - Paper texture via [Modifier.paperTexture] on cards / paper surfaces.
 *   - Soft warm shadows via [Modifier.shadow] with ambient/spot = paperShadow.
 *   - Slight rotations (-0.3deg hover, -2deg sticky-note) for hand-placed feel.
 *   - Caveat hand-written font (via [KutaFonts.Caveat]) for hero / dialog titles.
 *   - Dashed dividers for "torn paper" feel.
 *   - Sticky-note-styled badges/snackbars (stickyNote bg + slight rotation).
 */

// ===== Helpers =====

/**
 * FORK: Adaptive paper-texture dot color. Light-mode uses dark warm dots,
 * dark-mode uses light warm dots. Spec §6.1 says 6% alpha for light and 4% for
 * dark — we approximate with a single 6% alpha applied to [KutaColors.fgPrimary]
 * (which is dark in light mode, light in dark mode). Keeps the API simple.
 */
private val paperDotColor: Color
    @Composable get() = kutaColors.fgPrimary.copy(alpha = 0.06f)

/**
 * Washi-tape decoration — a semi-transparent strip rotated -2deg.
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
 * FORK: warm paper-shadow with consistent ambient/spot colors.
 * centralizes the [Modifier.shadow] call so all Notebook surfaces share the
 * same shadow tone.
 */
private fun Modifier.paperShadow(
    elevation: Dp,
    shape: Shape = RoundedCornerShape(10.dp),
    colors: tachiyomi.presentation.core.kuta.theme.KutaColors,
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

    // FORK: hover lift + rotate per 02-notebook.md §5.1 + §7.
    val lift by animateDpAsState(
        targetValue = if (isHovered && enabled) (-2).dp else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "nb-btn-lift",
    )
    val rotation by animateFloatAsState(
        targetValue = if (isHovered && enabled && !isGhost) -0.3f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "nb-btn-rotate",
    )
    val shadowElevation by animateDpAsState(
        targetValue = if (isHovered && enabled && !isGhost) 8.dp else 4.dp,
        animationSpec = tween(durationMillis = 200),
        label = "nb-btn-shadow",
    )

    Box(
        modifier = modifier
            .height(48.dp)
            .then(if (enabled && lift != 0.dp) Modifier.offset(y = lift) else Modifier)
            .then(if (enabled && rotation != 0f) Modifier.rotate(rotation) else Modifier)
            .then(
                if (!isGhost && enabled) {
                    Modifier.paperShadow(shadowElevation, RoundedCornerShape(8.dp), colors)
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
                indication = ripple(),
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
            Text(text, color = textColor, style = typography.button)
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
    // FORK: subtle bg tint on hover for tactile paper feel.
    val bgAlpha by animateFloatAsState(
        targetValue = if (isHovered && enabled) 0.5f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "nb-icon-btn-tint",
    )

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(colors.bgElevated.copy(alpha = bgAlpha * 0.5f))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (enabled) colors.fgSecondary else colors.fgDim,
            modifier = Modifier.size(22.dp),
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
            .paperShadow(shadowElevation, RoundedCornerShape(10.dp), colors)
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
    val actualEnabled = enabled && variant != KutaInputVariant.DISABLED
    val borderColor = if (isError) colors.accentTertiary else colors.borderDefault
    val focusedBorderColor = if (isError) colors.accentTertiary else colors.accentPrimary

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = actualEnabled,
        textStyle = typography.body,
        placeholder = {
            // FORK: italic fgDim placeholder per 02-notebook.md §5.3.
            Text(
                placeholder,
                color = colors.fgDim,
                style = typography.body.copy(fontStyle = FontStyle.Italic),
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.bgSurface,
            unfocusedContainerColor = colors.bgSurface,
            disabledContainerColor = colors.bgElevated,
            errorContainerColor = colors.bgSurface,
            focusedBorderColor = focusedBorderColor,
            unfocusedBorderColor = borderColor,
            disabledBorderColor = colors.borderSubtle,
            errorBorderColor = colors.accentTertiary,
            focusedTextColor = colors.fgPrimary,
            unfocusedTextColor = colors.fgPrimary,
            disabledTextColor = colors.fgDim,
            errorTextColor = colors.fgPrimary,
            focusedLeadingIconColor = colors.accentPrimary,
            unfocusedLeadingIconColor = colors.fgMuted,
            cursorColor = colors.accentPrimary,
            errorCursorColor = colors.accentTertiary,
        ),
    )
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
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = typography.body,
        placeholder = {
            Text(
                placeholder,
                color = colors.fgDim,
                style = typography.body.copy(fontStyle = FontStyle.Italic),
            )
        },
        leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = null, tint = colors.fgMuted)
        },
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.bgSurface,
            unfocusedContainerColor = colors.bgSurface,
            focusedBorderColor = colors.accentPrimary,
            unfocusedBorderColor = colors.borderDefault,
            focusedTextColor = colors.fgPrimary,
            unfocusedTextColor = colors.fgPrimary,
            focusedLeadingIconColor = colors.accentPrimary,
            unfocusedLeadingIconColor = colors.fgMuted,
            cursorColor = colors.accentPrimary,
        ),
    )
}

// ===== Dialogs / Sheets (per §5.4) =====

@Composable
fun NotebookDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = kutaColors
    // FORK: M3 AlertDialog with paper-textured surface, strong border, deep
    // warm shadow. Max width 560dp per 02-notebook.md §5.4. We use AlertDialog
    // (not Dialog) so we inherit M3 scrim + dismiss behavior for free.
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {},
        modifier = modifier
            .widthIn(max = 560.dp)
            .paperTexture(dotColor = paperDotColor),
        containerColor = colors.bgPaper,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 0.dp,
        text = { content() },
    )
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
    NotebookDialog(onDismissRequest = onDismiss, modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(KutaSpacing.md)) {
            // FORK: dialog title in Caveat (hand-written) for cozy feel
            // per 02-notebook.md §8.4 ("Section headers… in Caveat for cozy feel").
            Text(
                title,
                style = typography.headline.copy(
                    fontFamily = KutaFonts.Caveat,
                    fontSize = 32.sp,
                ),
            )
            Text(message, style = typography.body)
            Row(
                modifier = Modifier.fillMaxWidth(),
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
    val sheetState = rememberModalBottomSheetState()
    // FORK: M3 ModalBottomSheet with Notebook colors + paper texture overlay.
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = colors.bgPaper,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = null,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .paperTexture(dotColor = paperDotColor),
        ) {
            content()
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
    NavigationBar(
        modifier = modifier
            .paperTexture(dotColor = colors.fgPrimary.copy(alpha = 0.04f)),
        containerColor = colors.bgSurface,
        contentColor = colors.fgSecondary,
        tonalElevation = 0.dp,
    ) {
        items.forEach { item ->
            NavigationBarItem(
                selected = item.selected,
                onClick = item.onClick,
                icon = {
                    // FORK: ink underline beneath active icon per §5.5.
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(item.icon, contentDescription = item.label)
                        if (item.selected) {
                            Box(
                                Modifier
                                    .padding(top = 2.dp)
                                    .width(16.dp)
                                    .height(2.dp)
                                    .background(colors.accentPrimary.copy(alpha = 0.6f)),
                            )
                        }
                    }
                },
                label = { Text(item.label, style = typography.bodySmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.accentPrimary,
                    selectedTextColor = colors.accentPrimary,
                    unselectedIconColor = colors.fgMuted,
                    unselectedTextColor = colors.fgMuted,
                    indicatorColor = colors.accentPrimary.copy(alpha = 0.15f),
                ),
            )
        }
    }
}

@Composable
fun NotebookNavigationRail(
    items: List<KutaNavigationItem>,
    modifier: Modifier = Modifier,
) {
    val colors = kutaColors
    val typography = kutaTypography
    NavigationRail(
        modifier = modifier
            .paperTexture(dotColor = colors.fgPrimary.copy(alpha = 0.04f)),
        containerColor = colors.bgSidebar,
        contentColor = colors.fgSecondary,
    ) {
        items.forEach { item ->
            NavigationRailItem(
                selected = item.selected,
                onClick = item.onClick,
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label, style = typography.bodySmall) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = colors.accentPrimary,
                    selectedTextColor = colors.accentPrimary,
                    unselectedIconColor = colors.fgMuted,
                    unselectedTextColor = colors.fgMuted,
                    indicatorColor = colors.accentPrimary.copy(alpha = 0.15f),
                ),
            )
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
    val selectedIndex = tabs.indexOfFirst { it.selected }.coerceAtLeast(0)
    TabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier,
        containerColor = colors.bgSurface,
        contentColor = colors.accentPrimary,
        divider = {
            // FORK: dashed divider for torn-paper feel between tabs and content.
            NotebookDivider()
        },
        indicator = { tabPositions ->
            if (selectedIndex < tabPositions.size) {
                // FORK: ink-underline indicator — accent-colored bar with
                // rounded ends, positioned via M3's tabIndicatorOffset.
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[selectedIndex])
                        .rotate(-0.3f),
                    height = 3.dp,
                    color = colors.accentPrimary,
                )
            }
        },
    ) {
        tabs.forEach { tab ->
            Tab(
                selected = tab.selected,
                onClick = tab.onClick,
                text = { Text(tab.label, style = typography.subtitle) },
                selectedContentColor = colors.accentPrimary,
                unselectedContentColor = colors.fgMuted,
            )
        }
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            // FORK: 16dp vertical / 24dp horizontal per §5.7.
            .padding(horizontal = KutaSpacing.xl, vertical = KutaSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = colors.fgSecondary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(KutaSpacing.lg))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.fgPrimary, style = typography.subtitle)
            if (subtitle != null) {
                // FORK: italic subtitle mimics hand-written metadata per §8.1.
                Text(
                    subtitle,
                    color = colors.fgMuted,
                    style = typography.bodySmall.copy(fontStyle = FontStyle.Italic),
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
    val rotation = remember { (-2f..2f).random() }

    Box(
        modifier = modifier
            .rotate(rotation)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(4.dp))
            .padding(horizontal = KutaSpacing.sm, vertical = KutaSpacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = textColor, style = typography.label)
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
    // FORK: per §5.8 — accent at 15% bg / 30% border when selected; rotation -1deg.
    val bgColor = if (selected) colors.accentPrimary.copy(alpha = 0.15f) else Color.Transparent
    val borderColor = if (selected) colors.accentPrimary.copy(alpha = 0.3f) else colors.borderDefault
    val textColor = if (selected) colors.accentPrimary else colors.fgSecondary
    val rotation = if (selected) -1f else 0f

    val interactionSource = remember { MutableInteractionSource() }
    val chipModifier = if (onClick != null) {
        modifier.clickable(
            interactionSource = interactionSource,
            indication = ripple(),
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
        Text(text, color = textColor, style = typography.label)
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
    if (style == KutaToggleStyle.SWITCH) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onAccent,
                checkedTrackColor = colors.accentPrimary,
                uncheckedThumbColor = colors.fgMuted,
                uncheckedTrackColor = colors.bgElevated,
            ),
        )
    } else {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            colors = CheckboxDefaults.colors(
                checkedColor = colors.accentPrimary,
                uncheckedColor = colors.borderDefault,
                checkmarkColor = colors.onAccent,
            ),
        )
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
    // FORK: accentPrimary track + thumb per §8.3 (player scrubber).
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        valueRange = valueRange,
        colors = SliderDefaults.colors(
            thumbColor = colors.accentPrimary,
            activeTrackColor = colors.accentPrimary,
            inactiveTrackColor = colors.borderDefault,
            activeTickColor = colors.accentPrimary.copy(alpha = 0.6f),
            inactiveTickColor = colors.borderSubtle,
        ),
    )
}

@Composable
fun NotebookProgressBar(
    progress: Float?,
    modifier: Modifier = Modifier,
) {
    val colors = kutaColors
    if (progress != null) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = modifier,
            color = colors.accentPrimary,
            trackColor = colors.borderSubtle,
        )
    } else {
        LinearProgressIndicator(
            modifier = modifier,
            color = colors.accentPrimary,
            trackColor = colors.borderSubtle,
        )
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
    // slight rotation, paper shadow, hand-pinned feel.
    val rotation = remember { (-1f..1f).random() }
    Box(
        modifier = modifier
            .rotate(rotation)
            .paperShadow(6.dp, RoundedCornerShape(8.dp), colors)
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
            Text(
                message,
                color = colors.fgPrimary,
                style = typography.body,
                modifier = Modifier.weight(1f),
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
    // FORK: M3 DropdownMenu with Notebook container/border + paper texture.
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.paperTexture(dotColor = paperDotColor),
        containerColor = colors.bgPaper,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, colors.borderDefault),
        shadowElevation = 6.dp,
    ) {
        items.forEach { item ->
            DropdownMenuItem(
                text = {
                    Text(
                        item.label,
                        color = colors.fgPrimary,
                        style = typography.body,
                    )
                },
                onClick = { item.onClick(); onDismissRequest() },
                leadingIcon = if (item.icon != null) {
                    { Icon(item.icon, contentDescription = null, tint = colors.fgSecondary) }
                } else {
                    null
                },
            )
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
    // FORK: M3 Scaffold with Notebook bgBase container color. Paper texture is
    // not applied here (perf — would draw dots across the whole screen). Cards
    // inside the scaffold bring their own texture.
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

@Composable
fun NotebookTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    val colors = kutaColors
    val typography = kutaTypography
    TopAppBar(
        title = {
            // FORK: headline typography + fgPrimary per §5.6. Caveat is
            // reserved for screen hero titles (per §8.2); here we use Inter
            // to keep the app bar readable at small sizes.
            Text(
                title,
                color = colors.fgPrimary,
                style = typography.headline,
            )
        },
        modifier = modifier,
        navigationIcon = {
            if (onBack != null) {
                NotebookIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = onBack,
                    contentDescription = "Back",
                )
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colors.bgSurface,
            titleContentColor = colors.fgPrimary,
            navigationIconContentColor = colors.fgSecondary,
            actionIconContentColor = colors.fgSecondary,
        ),
    )
}

@Composable
fun NotebookBottomAppBar(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = kutaColors
    BottomAppBar(
        modifier = modifier
            .paperTexture(dotColor = colors.fgPrimary.copy(alpha = 0.04f)),
        containerColor = colors.bgSurface,
        contentColor = colors.fgSecondary,
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
    val shape = RoundedCornerShape(12.dp)
    val fabModifier = modifier.paperShadow(8.dp, shape, colors)
    if (text != null) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            modifier = fabModifier,
            containerColor = colors.accentPrimary,
            contentColor = colors.onAccent,
            shape = shape,
            icon = { Icon(icon, contentDescription = null) },
            text = { Text(text) },
        )
    } else {
        FloatingActionButton(
            onClick = onClick,
            modifier = fabModifier,
            containerColor = colors.accentPrimary,
            contentColor = colors.onAccent,
            shape = shape,
        ) {
            Icon(icon, contentDescription = null)
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
    // Uses CircularProgressIndicator with caramel tint (per spec's "caramel at
    // 10%" shimmer direction — a full Brush shimmer is heavier than needed).
    Box(
        modifier = modifier
            .height(height.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.bgElevated)
            .dashedRectangleBorder(colors.borderDefault, 1.dp)
            .padding(KutaSpacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = colors.accentQuaternary,
            strokeWidth = 2.dp,
        )
    }
}

@Composable
fun NotebookDivider(
    modifier: Modifier = Modifier,
) {
    val colors = kutaColors
    // FORK: dashed line for "torn paper" feel per §8.4 / §5.7.
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp),
    ) {
        val strokeWidth = 1.dp.toPx()
        val dashPx = 6.dp.toPx()
        val gapPx = 4.dp.toPx()
        val totalWidth = size.width
        var x = 0f
        while (x < totalWidth) {
            val endX = (x + dashPx).coerceAtMost(totalWidth)
            drawLine(
                color = colors.borderSubtle,
                start = Offset(x, 0f),
                end = Offset(endX, 0f),
                strokeWidth = strokeWidth,
            )
            x += dashPx + gapPx
        }
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
            .paperShadow(4.dp, CircleShape, colors)
            .clip(CircleShape)
            .background(colors.bgElevated)
            .border(1.dp, colors.borderDefault, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

// ===== Private drawing helpers =====

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
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                cornerRadius.toPx(),
                cornerRadius.toPx(),
            ),
        )
    },
)
