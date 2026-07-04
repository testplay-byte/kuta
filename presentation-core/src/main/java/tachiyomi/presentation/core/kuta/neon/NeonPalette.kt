// FORK: Phase 2B
package tachiyomi.presentation.core.kuta.neon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import tachiyomi.presentation.core.kuta.theme.KutaAccent
import tachiyomi.presentation.core.kuta.theme.KutaColors
import tachiyomi.presentation.core.kuta.theme.KutaFonts
import tachiyomi.presentation.core.kuta.theme.KutaTypography

/**
 * FORK: Phase 2B — Full Neon color palette + typography.
 * Per DOCS/design-system/01-neon.md §2 (colors) and §3 (typography).
 *
 * Dark mode is Neon's natural state. Light mode inverts the canvas while keeping
 * the same neon accents (per §2.2 — "striking but readable"). Glow tokens in
 * light mode hold the accent color at full alpha so callers can use them as a
 * 2dp solid border (glow is invisible on light backgrounds).
 *
 * Design tokens that don't apply to Neon (paper, washi tape, brutalist border,
 * etc.) are left as [Color.Unspecified] so the [KutaColors] union type stays
 * satisfied without polluting Neon's identity.
 */

// ===== Dark Mode Palette (§2.1) =====

fun NeonDarkColors(accent: KutaAccent): KutaColors {
    val accentPrimary = accent.color
    val accentSecondary = Color(0xFFBCFF5F) // lime — Success
    val accentTertiary = Color(0xFFFF5F7E) // coral — Danger
    val accentQuaternary = Color(0xFFFFB45F) // amber — Warning
    val accentPurple = Color(0xFFA78BFA) // highlight

    return KutaColors(
        // === Backgrounds ===
        bgBase = Color(0xFF0F0F14),
        bgSurface = Color(0xFF1A1A22),
        bgSidebar = Color(0xFF15151C),
        bgElevated = Color(0xFF25252F),
        bgGlass = Color(0xFF1A1A22).copy(alpha = 0.85f), // rgba(26,26,34,0.85)
        bgPaper = Color.Unspecified,
        // === Accents ===
        accentPrimary = accentPrimary,
        accentSecondary = accentSecondary,
        accentTertiary = accentTertiary,
        accentQuaternary = accentQuaternary,
        accentPurple = accentPurple,
        // FORK: Brutalist-only accent slots — unused in Neon, left unspecified.
        accentPink = Color.Unspecified,
        accentGreen = Color.Unspecified,
        accentYellow = Color.Unspecified,
        accentOrange = Color.Unspecified,
        accentRed = Color.Unspecified,
        // === Foreground ===
        fgPrimary = Color(0xFFFFFFFF),
        fgSecondary = Color(0xFFC8C8D4),
        fgMuted = Color(0xFF8888A0),
        fgDim = Color(0xFF55556A),
        // === Borders ===
        borderDefault = Color.White.copy(alpha = 0.08f),
        borderSubtle = Color.White.copy(alpha = 0.04f),
        borderStrong = Color.White.copy(alpha = 0.12f),
        borderAccent = accentPrimary.copy(alpha = 0.20f),
        // === Notebook-specific (unused) ===
        ruledLine = Color.Unspecified,
        marginLine = Color.Unspecified,
        stickyNote = Color.Unspecified,
        washiTape = Color.Unspecified,
        paperShadow = Color.Unspecified,
        // === Brutalist-specific (unused) ===
        brutalistBorder = Color.Unspecified,
        shadowColor = Color.Unspecified,
        gridLineColor = Color.Unspecified,
        hoverBgTint = Color.Unspecified,
        activeBgTint = Color.Unspecified,
        // === Glow (§2.5) — in dark mode, real glow at 25% accent alpha ===
        glowPrimary = accentPrimary.copy(alpha = 0.25f),
        glowSecondary = accentSecondary.copy(alpha = 0.25f),
        glowTertiary = accentTertiary.copy(alpha = 0.25f),
        // === Misc ===
        error = accentTertiary,
        onSuccess = Color(0xFF0F0F14),
        onAccent = Color(0xFF0F0F14), // dark text on bright accent for contrast
    )
}

// ===== Light Mode Palette (§2.2) =====

fun NeonLightColors(accent: KutaAccent): KutaColors {
    val accentPrimary = accent.color
    val accentSecondary = Color(0xFFBCFF5F)
    val accentTertiary = Color(0xFFFF5F7E)
    val accentQuaternary = Color(0xFFFFB45F)
    val accentPurple = Color(0xFFA78BFA)

    // FORK: light mode uses off-white #F5F5F8 (not pure white) for the base canvas per §2.2.
    // Accents stay the SAME as dark mode (high contrast on light bg, slightly unusual but readable).
    return KutaColors(
        // === Backgrounds ===
        bgBase = Color(0xFFF5F5F8),
        bgSurface = Color(0xFFFFFFFF),
        bgSidebar = Color(0xFFEFEFF3),
        bgElevated = Color(0xFFE5E5EC),
        bgGlass = Color(0xFFFFFFFF).copy(alpha = 0.85f),
        bgPaper = Color.Unspecified,
        // === Accents ===
        accentPrimary = accentPrimary,
        accentSecondary = accentSecondary,
        accentTertiary = accentTertiary,
        accentQuaternary = accentQuaternary,
        accentPurple = accentPurple,
        accentPink = Color.Unspecified,
        accentGreen = Color.Unspecified,
        accentYellow = Color.Unspecified,
        accentOrange = Color.Unspecified,
        accentRed = Color.Unspecified,
        // === Foreground ===
        fgPrimary = Color(0xFF0F0F14),
        fgSecondary = Color(0xFF3A3A45),
        fgMuted = Color(0xFF6A6A7A),
        fgDim = Color(0xFF9A9AAB),
        // === Borders — derived from #0F0F14 at varying alpha (§2.4) ===
        borderDefault = Color(0xFF0F0F14).copy(alpha = 0.10f),
        borderSubtle = Color(0xFF0F0F14).copy(alpha = 0.05f),
        borderStrong = Color(0xFF0F0F14).copy(alpha = 0.18f),
        borderAccent = accentPrimary.copy(alpha = 0.20f),
        // === Notebook-specific (unused) ===
        ruledLine = Color.Unspecified,
        marginLine = Color.Unspecified,
        stickyNote = Color.Unspecified,
        washiTape = Color.Unspecified,
        paperShadow = Color.Unspecified,
        // === Brutalist-specific (unused) ===
        brutalistBorder = Color.Unspecified,
        shadowColor = Color.Unspecified,
        gridLineColor = Color.Unspecified,
        hoverBgTint = Color.Unspecified,
        activeBgTint = Color.Unspecified,
        // FORK: In light mode, glow is replaced with a 2dp solid accent border (§2.5).
        // Store full-alpha accent color so light-mode callers can use it as a border color.
        glowPrimary = accentPrimary,
        glowSecondary = accentSecondary,
        glowTertiary = accentTertiary,
        // === Misc ===
        error = accentTertiary,
        onSuccess = Color(0xFFFFFFFF),
        onAccent = Color(0xFFFFFFFF),
    )
}

// ===== Typography (§3) =====

/**
 * Neon typography — Inter for UI/body, JetBrains Mono for numbers/code.
 * Per 01-neon.md §3.2.
 *
 * The `label` style is uppercase with +0.08em tracking per spec. Compose's
 * [TextStyle.textTransform] requires API 28+; we apply [letterSpacing] here
 * and rely on call sites to call [String.uppercase] when uppercase is needed
 * (broadly compatible with minSdk 26).
 */
val NeonTypography: KutaTypography = KutaTypography(
    display = TextStyle(fontFamily = KutaFonts.Inter, fontSize = 48.sp, fontWeight = FontWeight.Bold),
    headline = TextStyle(fontFamily = KutaFonts.Inter, fontSize = 28.sp, fontWeight = FontWeight.Bold),
    title = TextStyle(fontFamily = KutaFonts.Inter, fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    subtitle = TextStyle(fontFamily = KutaFonts.Inter, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    body = TextStyle(fontFamily = KutaFonts.Inter, fontSize = 14.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontFamily = KutaFonts.Inter, fontSize = 12.sp, fontWeight = FontWeight.Normal),
    // FORK: 0.08em tracking at 11sp → 0.88sp absolute letterSpacing per §3.2.
    label = TextStyle(
        fontFamily = KutaFonts.Inter,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.88.sp,
    ),
    button = TextStyle(fontFamily = KutaFonts.Inter, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    // FORK: mono styles use JetBrainsMono with tabular-nums (fontFeatureSettings = "tnum") per §3.2.
    monoValue = TextStyle(
        fontFamily = KutaFonts.JetBrainsMono,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        fontFeatureSettings = "tnum",
    ),
    monoLarge = TextStyle(
        fontFamily = KutaFonts.JetBrainsMono,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        fontFeatureSettings = "tnum",
    ),
    handWritten = null,
)
