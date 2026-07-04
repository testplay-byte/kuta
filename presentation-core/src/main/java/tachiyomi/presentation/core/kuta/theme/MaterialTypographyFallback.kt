package tachiyomi.presentation.core.kuta.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * FORK: Phase 2 — Fallback typography for MATERIAL design language.
 * Maps to M3 defaults. Used when [LocalDesignLanguage] is MATERIAL.
 */
val MaterialTypographyFallback: KutaTypography = KutaTypography(
    display = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Bold),
    headline = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    title = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    subtitle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    body = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
    label = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
    button = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    monoValue = null,
    monoLarge = null,
    handWritten = null,
)
