package tachiyomi.presentation.core.kuta.brutalist

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import tachiyomi.presentation.core.kuta.theme.KutaAccent
import tachiyomi.presentation.core.kuta.theme.KutaColors
import tachiyomi.presentation.core.kuta.theme.KutaFonts
import tachiyomi.presentation.core.kuta.theme.KutaTypography

/**
 * FORK: Phase 2A — STUB Brutalist palette + typography.
 * Phase 2B subagent will replace this with the full implementation per 03-brutalist.md.
 */

// TODO: Phase 2B — implement full Brutalist color palette per DOCS/design-system/03-brutalist.md §2
fun BrutalistDarkColors(accent: KutaAccent): KutaColors = KutaColors.unspecified().copy(
    accentPrimary = accent.color,
)

// TODO: Phase 2B — implement full Brutalist light palette
fun BrutalistLightColors(accent: KutaAccent): KutaColors = KutaColors.unspecified().copy(
    accentPrimary = accent.color,
)

// TODO: Phase 2B — implement full Brutalist typography per 03-brutalist.md §3
val BrutalistTypography: KutaTypography = KutaTypography(
    display = TextStyle(fontFamily = KutaFonts.Inter, fontSize = 48.sp, fontWeight = FontWeight.Black),
    headline = TextStyle(fontFamily = KutaFonts.Inter, fontSize = 28.sp, fontWeight = FontWeight.Black),
    title = TextStyle(fontFamily = KutaFonts.Inter, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold),
    subtitle = TextStyle(fontFamily = KutaFonts.Inter, fontSize = 16.sp, fontWeight = FontWeight.Bold),
    body = TextStyle(fontFamily = KutaFonts.Inter, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    bodySmall = TextStyle(fontFamily = KutaFonts.Inter, fontSize = 12.sp, fontWeight = FontWeight.Medium),
    label = TextStyle(fontFamily = KutaFonts.Inter, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold),
    button = TextStyle(fontFamily = KutaFonts.Inter, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold),
    monoValue = null,
    monoLarge = null,
    handWritten = null,
)
