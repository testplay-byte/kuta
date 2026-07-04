package tachiyomi.presentation.core.kuta.neon

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import tachiyomi.presentation.core.kuta.theme.KutaAccent
import tachiyomi.presentation.core.kuta.theme.KutaColors
import tachiyomi.presentation.core.kuta.theme.KutaFonts
import tachiyomi.presentation.core.kuta.theme.KutaTypography

/**
 * FORK: Phase 2A — STUB Neon palette + typography.
 * Phase 2B subagent will replace this with the full implementation per 01-neon.md.
 */

// TODO: Phase 2B — implement full Neon color palette per DOCS/design-system/01-neon.md §2
fun NeonDarkColors(accent: KutaAccent): KutaColors = KutaColors.unspecified().copy(
    accentPrimary = accent.color,
)

// TODO: Phase 2B — implement full Neon light palette
fun NeonLightColors(accent: KutaAccent): KutaColors = KutaColors.unspecified().copy(
    accentPrimary = accent.color,
)

// TODO: Phase 2B — implement full Neon typography per 01-neon.md §3
val NeonTypography: KutaTypography = KutaTypography(
    display = TextStyle(fontFamily = KutaFonts.Inter, fontSize = 48.sp, fontWeight = FontWeight.Bold),
    headline = TextStyle(fontFamily = KutaFonts.Inter, fontSize = 28.sp, fontWeight = FontWeight.Bold),
    title = TextStyle(fontFamily = KutaFonts.Inter, fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    subtitle = TextStyle(fontFamily = KutaFonts.Inter, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    body = TextStyle(fontFamily = KutaFonts.Inter, fontSize = 14.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontFamily = KutaFonts.Inter, fontSize = 12.sp, fontWeight = FontWeight.Normal),
    label = TextStyle(fontFamily = KutaFonts.Inter, fontSize = 11.sp, fontWeight = FontWeight.Medium),
    button = TextStyle(fontFamily = KutaFonts.Inter, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    monoValue = TextStyle(fontFamily = KutaFonts.JetBrainsMono, fontSize = 14.sp, fontWeight = FontWeight.Bold),
    monoLarge = TextStyle(fontFamily = KutaFonts.JetBrainsMono, fontSize = 26.sp, fontWeight = FontWeight.Bold),
    handWritten = null,
)
