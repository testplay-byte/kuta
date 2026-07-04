package tachiyomi.presentation.core.kuta.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import tachiyomi.presentation.core.R

/**
 * FORK: Phase 2 — Font families for all 4 design languages.
 *
 * Uses variable fonts (downloaded from Google Fonts, OFL license) with
 * [FontVariation] to select weights — one file per family, all weights covered.
 * This is the modern Android approach (API 26+, which is our minSdk).
 *
 * Files in app/src/main/res/font/:
 * - inter_variable.ttf (covers Thin..Black)
 * - jetbrains_mono_variable.ttf (covers Thin..Bold)
 * - caveat_variable.ttf (covers Regular..Bold)
 */
object KutaFonts {
    val Inter = FontFamily(
        Font(R.font.inter_variable, weight = FontWeight.Normal,
            variation = FontVariation("wght" to 400f)),
        Font(R.font.inter_variable, weight = FontWeight.Medium,
            variation = FontVariation("wght" to 500f)),
        Font(R.font.inter_variable, weight = FontWeight.SemiBold,
            variation = FontVariation("wght" to 600f)),
        Font(R.font.inter_variable, weight = FontWeight.Bold,
            variation = FontVariation("wght" to 700f)),
        Font(R.font.inter_variable, weight = FontWeight.Black,
            variation = FontVariation("wght" to 900f)),
    )

    val JetBrainsMono = FontFamily(
        Font(R.font.jetbrains_mono_variable, weight = FontWeight.Normal,
            variation = FontVariation("wght" to 400f)),
        Font(R.font.jetbrains_mono_variable, weight = FontWeight.Bold,
            variation = FontVariation("wght" to 700f)),
    )

    val Caveat = FontFamily(
        Font(R.font.caveat_variable, weight = FontWeight.Normal,
            variation = FontVariation("wght" to 400f)),
        Font(R.font.caveat_variable, weight = FontWeight.Bold,
            variation = FontVariation("wght" to 700f)),
    )
}
