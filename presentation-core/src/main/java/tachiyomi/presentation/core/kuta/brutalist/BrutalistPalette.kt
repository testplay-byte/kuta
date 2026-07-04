// FORK: Phase 2B
package tachiyomi.presentation.core.kuta.brutalist

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
 * FORK: Phase 2B — REAL Brutalist color palette + typography.
 * Per DOCS/design-system/03-brutalist.md §2 and §3.
 *
 * Replaces Phase 2A stubs (which only set accentPrimary).
 *
 * Light mode ("Acid Cream"): warm off-white canvas (#D9D5CC), cards (#EDEAE3),
 * near-black universal border (#1A1A1A), hard shadow same color, plus 7 bright
 * saturated accent colors. Hover/press tints are pale blue washes.
 *
 * Dark mode ("Midnight Raw"): deep charcoal canvas (#2A2A32), gray border
 * (#555555), darker shadow (#1A1A1E), accent colors brightened so they pop on
 * the dark canvas.
 *
 * The user's selected accent (from Settings → Appearance) becomes [KutaColors.accentPrimary].
 * The other accent tokens (pink/green/yellow/orange/purple/red) are used for semantic states
 * and the colored-shadow card variants.
 */

// ===== Light Mode: "Acid Cream" — per 03-brutalist.md §2.1 =====

fun BrutalistLightColors(accent: KutaAccent): KutaColors = KutaColors.unspecified().copy(
    // Backgrounds
    bgBase = Color(0xFFD9D5CC),
    bgSurface = Color(0xFFEDEAE3),
    bgSidebar = Color(0xFFEDEAE3),
    bgElevated = Color(0xFFCDC9C0), // FORK: hover bg is darker than surface — inverted from other designs per §2.1
    bgGlass = Color(0xCCFFFFFF), // FORK: not used by Brutalist, populated for cross-design API compat
    bgPaper = Color.Unspecified,

    // Accents — user's accent becomes accentPrimary
    accentPrimary = accent.color,
    // FORK: alias secondary/tertiary/quaternary to Brutalist's named accents for shared-component compat
    accentSecondary = Color(0xFFEC4899),
    accentTertiary = Color(0xFF22C55E),
    accentQuaternary = Color(0xFFF59E0B),
    accentPurple = Color(0xFF8B5CF6),
    accentPink = Color(0xFFEC4899),
    accentGreen = Color(0xFF22C55E),
    accentYellow = Color(0xFFF59E0B),
    accentOrange = Color(0xFFF97316),
    accentRed = Color(0xFFEF4444),

    // Text colors per §2.4
    fgPrimary = Color(0xFF1A1A1A),
    fgSecondary = Color(0xFF3A3A3A),
    fgMuted = Color(0xFF5A5A5A),
    fgDim = Color(0xFF7A7A7A),

    // Borders — Brutalist uses brutalistBorder for everything; mirror into generic slots
    borderDefault = Color(0xFF1A1A1A),
    borderSubtle = Color(0xFF1A1A1A),
    borderStrong = Color(0xFF1A1A1A),
    borderAccent = accent.color,

    // Notebook-specific — unused by Brutalist
    ruledLine = Color.Unspecified,
    marginLine = Color.Unspecified,
    stickyNote = Color.Unspecified,
    washiTape = Color.Unspecified,
    paperShadow = Color.Unspecified,

    // Brutalist-specific per §2.3
    brutalistBorder = Color(0xFF1A1A1A),
    shadowColor = Color(0xFF1A1A1A),
    // FORK: rgba(26,26,26,0.14) → Color(red=26, green=26, blue=26, alpha=36) (36/255 ≈ 0.14)
    gridLineColor = Color(26, 26, 26, 36),
    hoverBgTint = Color(0xFFDBEAFE), // light blue wash
    activeBgTint = Color(0xFFBFDBFE), // deeper blue wash

    // Glow (Neon) — unused by Brutalist
    glowPrimary = Color.Unspecified,
    glowSecondary = Color.Unspecified,
    glowTertiary = Color.Unspecified,

    // Misc
    error = Color(0xFFEF4444),
    onSuccess = Color(0xFF1A1A1A),
    onAccent = Color.White, // FORK: accents are bright, white text reads on them
)

// ===== Dark Mode: "Midnight Raw" — per 03-brutalist.md §2.2 =====

fun BrutalistDarkColors(accent: KutaAccent): KutaColors = KutaColors.unspecified().copy(
    // Backgrounds
    bgBase = Color(0xFF2A2A32),
    bgSurface = Color(0xFF363640),
    bgSidebar = Color(0xFF32323C),
    bgElevated = Color(0xFF3E3E48),
    bgGlass = Color(0xCC000000),
    bgPaper = Color.Unspecified,

    // Accents — brightened in dark mode per §2.2
    accentPrimary = accent.color,
    accentSecondary = Color(0xFFF472B6),
    accentTertiary = Color(0xFF4ADE80),
    accentQuaternary = Color(0xFFFBBF24),
    accentPurple = Color(0xFFA78BFA),
    accentPink = Color(0xFFF472B6),
    accentGreen = Color(0xFF4ADE80),
    accentYellow = Color(0xFFFBBF24),
    accentOrange = Color(0xFFFB923C),
    accentRed = Color(0xFFFF3333),

    // Text colors per §2.4
    fgPrimary = Color(0xFFE8E8E8),
    fgSecondary = Color(0xFFB0B0B8),
    fgMuted = Color(0xFF909098),
    fgDim = Color(0xFF707078),

    // Borders — gray in dark per §2.3
    borderDefault = Color(0xFF555555),
    borderSubtle = Color(0xFF555555),
    borderStrong = Color(0xFF555555),
    borderAccent = accent.color,

    ruledLine = Color.Unspecified,
    marginLine = Color.Unspecified,
    stickyNote = Color.Unspecified,
    washiTape = Color.Unspecified,
    paperShadow = Color.Unspecified,

    // Brutalist-specific
    brutalistBorder = Color(0xFF555555),
    shadowColor = Color(0xFF1A1A1E),
    // FORK: rgba(255,255,255,0.08) → Color(255, 255, 255, 20) (20/255 ≈ 0.08)
    gridLineColor = Color(255, 255, 255, 20),
    hoverBgTint = Color(0xFF3E4258),
    activeBgTint = Color(0xFF4A5068),

    glowPrimary = Color.Unspecified,
    glowSecondary = Color.Unspecified,
    glowTertiary = Color.Unspecified,

    error = Color(0xFFFF3333),
    onSuccess = Color(0xFFE8E8E8),
    onAccent = Color.White,
)

// ===== Typography per 03-brutalist.md §3.2 =====
// Inter everywhere; Black (900) for display/headline, ExtraBold (800) for title/label/button,
// Bold (700) for subtitle, SemiBold (600) for body, Medium (500) for bodySmall.
// Negative tracking (-0.02em) for display/headline/title/button; positive (+0.04em) for label.
// Uppercase transforms are applied at the call site (text.uppercase()) — Compose TextStyle doesn't
// have a textTransform field on all supported API levels.

val BrutalistTypography: KutaTypography = KutaTypography(
    display = TextStyle(
        fontFamily = KutaFonts.Inter,
        fontSize = 48.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.02).em,
    ),
    headline = TextStyle(
        fontFamily = KutaFonts.Inter,
        fontSize = 28.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.02).em,
    ),
    title = TextStyle(
        fontFamily = KutaFonts.Inter,
        fontSize = 22.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.02).em,
    ),
    subtitle = TextStyle(
        fontFamily = KutaFonts.Inter,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
    ),
    body = TextStyle(
        fontFamily = KutaFonts.Inter,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    bodySmall = TextStyle(
        fontFamily = KutaFonts.Inter,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
    ),
    label = TextStyle(
        fontFamily = KutaFonts.Inter,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.04.em,
    ),
    button = TextStyle(
        fontFamily = KutaFonts.Inter,
        fontSize = 14.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.02).em,
    ),
    // Neon-specific slots — unused by Brutalist
    monoValue = null,
    monoLarge = null,
    // Notebook-specific slot — unused by Brutalist
    handWritten = null,
)
