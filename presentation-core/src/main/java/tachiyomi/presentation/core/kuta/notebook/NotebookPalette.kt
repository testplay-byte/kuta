// FORK: Phase 2B
package tachiyomi.presentation.core.kuta.notebook

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import tachiyomi.presentation.core.kuta.theme.KutaAccent
import tachiyomi.presentation.core.kuta.theme.KutaColors
import tachiyomi.presentation.core.kuta.theme.KutaFonts
import tachiyomi.presentation.core.kuta.theme.KutaTypography

/**
 * FORK: Phase 2B — Full Notebook color palette + typography.
 * Per DOCS/design-system/02-notebook.md §2 (colors) and §3 (typography).
 *
 * Two named palettes: "Coffee White" (light) and "Dark Coffee" (dark).
 * The user's selected accent drives [KutaColors.accentPrimary]; the rest of the
 * palette is fixed earth-tone tokens that give Notebook its cozy feel.
 */

// ===== Light mode — "Coffee White" =====

private val LightBgBase = Color(0xFFF0E6D8)
private val LightBgSurface = Color(0xFFFDF8F2)
private val LightBgSidebar = Color(0xFFFDF8F2)
private val LightBgElevated = Color(0xFFE8DDD0)
private val LightBgPaper = Color(0xFFFDF8F2)

private val LightAccentSecondary = Color(0xFF6B8E5B) // sage
private val LightAccentTertiary = Color(0xFFC44040) // warm red
private val LightAccentQuaternary = Color(0xFFC99545) // caramel
private val LightAccentPlum = Color(0xFF966B94)

private val LightFgPrimary = Color(0xFF2E1A0E) // dark coffee
private val LightFgSecondary = Color(0xFF4A3425)
private val LightFgMuted = Color(0xFF7A6450)
private val LightFgDim = Color(0xFF9A8470)

private val LightBorderDefault = Color(0xFFD5C8B8)
private val LightBorderSubtle = Color(0xFFE5DAD0)
private val LightBorderStrong = Color(0xFFB5A590)

private val LightRuledLine = Color(0xFFDDD2C2)
private val LightMarginLine = Color(0xFFD4A0A0)
private val LightStickyNote = Color(0xFFFFF8CC)
private val LightWashiTape = Color(0xFFD2BEA0).copy(alpha = 0.8f)
private val LightPaperShadow = Color(0xFF785F41).copy(alpha = 0.15f)

// ===== Dark mode — "Dark Coffee" =====

private val DarkBgBase = Color(0xFF1A1412)
private val DarkBgSurface = Color(0xFF2A2220)
private val DarkBgSidebar = Color(0xFF2A2220)
private val DarkBgElevated = Color(0xFF3A3230)
private val DarkBgPaper = Color(0xFF2A2220)

private val DarkAccentSecondary = Color(0xFF7B9E6B) // sage
private val DarkAccentTertiary = Color(0xFFE06060) // warm red
private val DarkAccentQuaternary = Color(0xFFD4A55A) // caramel
private val DarkAccentPlum = Color(0xFF966B94)

private val DarkFgPrimary = Color(0xFFF0E0D0) // cream
private val DarkFgSecondary = Color(0xFFD4B8A0)
private val DarkFgMuted = Color(0xFFA89080)
private val DarkFgDim = Color(0xFF7A6855)

private val DarkBorderDefault = Color(0xFF3A3230)
private val DarkBorderSubtle = Color(0xFF2E2624)
private val DarkBorderStrong = Color(0xFF4A4240)

private val DarkRuledLine = Color(0xFF3A3230)
private val DarkMarginLine = Color(0xFF5A3A3A)
private val DarkStickyNote = Color(0xFF3A3220)
private val DarkWashiTape = Color(0xFF504640).copy(alpha = 0.5f)
private val DarkPaperShadow = Color(0xFF000000).copy(alpha = 0.25f)

/**
 * Notebook "Coffee White" light palette.
 * Per 02-notebook.md §2.1. [accent] overrides accentPrimary (user's pick).
 */
fun NotebookLightColors(accent: KutaAccent): KutaColors = KutaColors.unspecified().copy(
    // Backgrounds
    bgBase = LightBgBase,
    bgSurface = LightBgSurface,
    bgSidebar = LightBgSidebar,
    bgElevated = LightBgElevated,
    bgPaper = LightBgPaper,
    bgGlass = LightBgSurface.copy(alpha = 0.7f),
    // Accents
    accentPrimary = accent.color,
    accentSecondary = LightAccentSecondary,
    accentTertiary = LightAccentTertiary,
    accentQuaternary = LightAccentQuaternary,
    accentPurple = LightAccentPlum,
    // Brutalist-only tokens — fill with warm equivalents so any cross-design
    // fallback renders acceptably. (Notebook never reads these.)
    accentPink = LightAccentPlum,
    accentGreen = LightAccentSecondary,
    accentYellow = LightStickyNote,
    accentOrange = LightAccentQuaternary,
    accentRed = LightAccentTertiary,
    // Foreground
    fgPrimary = LightFgPrimary,
    fgSecondary = LightFgSecondary,
    fgMuted = LightFgMuted,
    fgDim = LightFgDim,
    // Borders
    borderDefault = LightBorderDefault,
    borderSubtle = LightBorderSubtle,
    borderStrong = LightBorderStrong,
    borderAccent = accent.color.copy(alpha = 0.3f),
    // Notebook-specific
    ruledLine = LightRuledLine,
    marginLine = LightMarginLine,
    stickyNote = LightStickyNote,
    washiTape = LightWashiTape,
    paperShadow = LightPaperShadow,
    // Brutalist-specific (unused but populated for safety)
    brutalistBorder = LightBorderStrong,
    shadowColor = LightPaperShadow,
    gridLineColor = LightRuledLine,
    hoverBgTint = LightBgElevated.copy(alpha = 0.5f),
    activeBgTint = accent.color.copy(alpha = 0.15f),
    // Glow (Neon-only; unused by Notebook)
    glowPrimary = accent.color,
    glowSecondary = LightAccentSecondary,
    glowTertiary = LightAccentTertiary,
    // Misc
    error = LightAccentTertiary,
    onSuccess = LightBgSurface,
    onAccent = Color(0xFFFFFAF5), // cream-on-coffee for primary buttons
)

/**
 * Notebook "Dark Coffee" dark palette.
 * Per 02-notebook.md §2.2. [accent] overrides accentPrimary (user's pick).
 */
fun NotebookDarkColors(accent: KutaAccent): KutaColors = KutaColors.unspecified().copy(
    // Backgrounds
    bgBase = DarkBgBase,
    bgSurface = DarkBgSurface,
    bgSidebar = DarkBgSidebar,
    bgElevated = DarkBgElevated,
    bgPaper = DarkBgPaper,
    bgGlass = DarkBgSurface.copy(alpha = 0.7f),
    // Accents
    accentPrimary = accent.color,
    accentSecondary = DarkAccentSecondary,
    accentTertiary = DarkAccentTertiary,
    accentQuaternary = DarkAccentQuaternary,
    accentPurple = DarkAccentPlum,
    // Brutalist-only tokens — populated for safety; Notebook never reads them.
    accentPink = DarkAccentPlum,
    accentGreen = DarkAccentSecondary,
    accentYellow = DarkStickyNote,
    accentOrange = DarkAccentQuaternary,
    accentRed = DarkAccentTertiary,
    // Foreground
    fgPrimary = DarkFgPrimary,
    fgSecondary = DarkFgSecondary,
    fgMuted = DarkFgMuted,
    fgDim = DarkFgDim,
    // Borders
    borderDefault = DarkBorderDefault,
    borderSubtle = DarkBorderSubtle,
    borderStrong = DarkBorderStrong,
    borderAccent = accent.color.copy(alpha = 0.3f),
    // Notebook-specific
    ruledLine = DarkRuledLine,
    marginLine = DarkMarginLine,
    stickyNote = DarkStickyNote,
    washiTape = DarkWashiTape,
    paperShadow = DarkPaperShadow,
    // Brutalist-specific
    brutalistBorder = DarkBorderStrong,
    shadowColor = DarkPaperShadow,
    gridLineColor = DarkRuledLine,
    hoverBgTint = DarkBgElevated.copy(alpha = 0.5f),
    activeBgTint = accent.color.copy(alpha = 0.15f),
    // Glow (Neon-only; unused by Notebook)
    glowPrimary = accent.color,
    glowSecondary = DarkAccentSecondary,
    glowTertiary = DarkAccentTertiary,
    // Misc
    error = DarkAccentTertiary,
    onSuccess = DarkBgSurface,
    onAccent = Color(0xFFFFFAF5),
)

/**
 * Notebook typography — Inter body + Caveat for handwritten accents.
 * Per 02-notebook.md §3.
 *
 * - display / handWritten use Caveat (hand-written)
 * - everything else uses Inter
 * - label is uppercase + 0.08em letter-spacing (the spec's "label" element)
 */
val NotebookTypography: KutaTypography = KutaTypography(
    display = TextStyle(
        fontFamily = KutaFonts.Caveat,
        fontSize = 48.sp,
        fontWeight = FontWeight.Bold,
    ),
    headline = TextStyle(
        fontFamily = KutaFonts.Inter,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
    ),
    title = TextStyle(
        fontFamily = KutaFonts.Inter,
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    subtitle = TextStyle(
        fontFamily = KutaFonts.Inter,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    body = TextStyle(
        fontFamily = KutaFonts.Inter,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodySmall = TextStyle(
        fontFamily = KutaFonts.Inter,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
    ),
    // FORK: label uses uppercase + 0.08em tracking per 02-notebook.md §3.2.
    label = TextStyle(
        fontFamily = KutaFonts.Inter,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.08.em,
    ),
    button = TextStyle(
        fontFamily = KutaFonts.Inter,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    monoValue = null,
    monoLarge = null,
    handWritten = TextStyle(
        fontFamily = KutaFonts.Caveat,
        fontSize = 20.sp,
        fontWeight = FontWeight.Normal,
    ),
)
