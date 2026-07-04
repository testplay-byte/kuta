package tachiyomi.presentation.core.kuta.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import tachiyomi.presentation.core.R

/**
 * FORK: Phase 2 — Font families for all 4 design languages.
 *
 * Uses variable fonts (downloaded from Google Fonts, OFL license) — one file
 * per family, all weights covered. Android 8+ (API 26, our minSdk) selects
 * the correct weight from the variable font based on the [FontWeight] parameter.
 *
 * Files in presentation-core/src/main/res/font/:
 * - inter_variable.ttf (covers Thin..Black)
 * - jetbrains_mono_variable.ttf (covers Thin..Bold)
 * - caveat_variable.ttf (covers Regular..Bold)
 */
object KutaFonts {
    val Inter = FontFamily(
        Font(R.font.inter_variable, weight = FontWeight.Normal),
        Font(R.font.inter_variable, weight = FontWeight.Medium),
        Font(R.font.inter_variable, weight = FontWeight.SemiBold),
        Font(R.font.inter_variable, weight = FontWeight.Bold),
        Font(R.font.inter_variable, weight = FontWeight.Black),
    )

    val JetBrainsMono = FontFamily(
        Font(R.font.jetbrains_mono_variable, weight = FontWeight.Normal),
        Font(R.font.jetbrains_mono_variable, weight = FontWeight.Bold),
    )

    val Caveat = FontFamily(
        Font(R.font.caveat_variable, weight = FontWeight.Normal),
        Font(R.font.caveat_variable, weight = FontWeight.Bold),
    )
}
