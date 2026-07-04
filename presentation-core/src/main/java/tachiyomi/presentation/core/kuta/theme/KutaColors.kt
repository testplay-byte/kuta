package tachiyomi.presentation.core.kuta.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified

/**
 * FORK: Phase 2 — Color token definitions for all 4 design languages.
 * Per DOCS/design-system/00-shared-architecture.md §3 and per-design specs.
 *
 * This is a union of all color tokens across all 4 designs. Each design fills
 * the tokens it uses; unused tokens default to [Color.Unspecified].
 */
data class KutaColors(
    // === Backgrounds ===
    val bgBase: Color,
    val bgSurface: Color,
    val bgSidebar: Color,
    val bgElevated: Color,
    val bgGlass: Color,
    val bgPaper: Color,

    // === Accents ===
    val accentPrimary: Color,
    val accentSecondary: Color,
    val accentTertiary: Color,
    val accentQuaternary: Color,
    val accentPurple: Color,
    // Brutalist-specific accent colors (used for colored shadows)
    val accentPink: Color,
    val accentGreen: Color,
    val accentYellow: Color,
    val accentOrange: Color,
    val accentRed: Color,

    // === Foreground (text) ===
    val fgPrimary: Color,
    val fgSecondary: Color,
    val fgMuted: Color,
    val fgDim: Color,

    // === Borders ===
    val borderDefault: Color,
    val borderSubtle: Color,
    val borderStrong: Color,
    val borderAccent: Color,

    // === Notebook-specific ===
    val ruledLine: Color,
    val marginLine: Color,
    val stickyNote: Color,
    val washiTape: Color,
    val paperShadow: Color,

    // === Brutalist-specific ===
    val brutalistBorder: Color,
    val shadowColor: Color,
    val gridLineColor: Color,
    val hoverBgTint: Color,
    val activeBgTint: Color,

    // === Glow (Neon) ===
    val glowPrimary: Color,
    val glowSecondary: Color,
    val glowTertiary: Color,

    // === Misc ===
    val error: Color,
    val onSuccess: Color,
    val onAccent: Color,
) {
    companion object {
        fun unspecified() = KutaColors(
            bgBase = Color.Unspecified,
            bgSurface = Color.Unspecified,
            bgSidebar = Color.Unspecified,
            bgElevated = Color.Unspecified,
            bgGlass = Color.Unspecified,
            bgPaper = Color.Unspecified,
            accentPrimary = Color.Unspecified,
            accentSecondary = Color.Unspecified,
            accentTertiary = Color.Unspecified,
            accentQuaternary = Color.Unspecified,
            accentPurple = Color.Unspecified,
            accentPink = Color.Unspecified,
            accentGreen = Color.Unspecified,
            accentYellow = Color.Unspecified,
            accentOrange = Color.Unspecified,
            accentRed = Color.Unspecified,
            fgPrimary = Color.Unspecified,
            fgSecondary = Color.Unspecified,
            fgMuted = Color.Unspecified,
            fgDim = Color.Unspecified,
            borderDefault = Color.Unspecified,
            borderSubtle = Color.Unspecified,
            borderStrong = Color.Unspecified,
            borderAccent = Color.Unspecified,
            ruledLine = Color.Unspecified,
            marginLine = Color.Unspecified,
            stickyNote = Color.Unspecified,
            washiTape = Color.Unspecified,
            paperShadow = Color.Unspecified,
            brutalistBorder = Color.Unspecified,
            shadowColor = Color.Unspecified,
            gridLineColor = Color.Unspecified,
            hoverBgTint = Color.Unspecified,
            activeBgTint = Color.Unspecified,
            glowPrimary = Color.Unspecified,
            glowSecondary = Color.Unspecified,
            glowTertiary = Color.Unspecified,
            error = Color.Unspecified,
            onSuccess = Color.Unspecified,
            onAccent = Color.Unspecified,
        )
    }
}
