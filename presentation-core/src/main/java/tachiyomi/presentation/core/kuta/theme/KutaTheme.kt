package tachiyomi.presentation.core.kuta.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * FORK: Phase 2 — KutaTheme: the multi-design-language theme wrapper.
 * Per DOCS/design-system/00-shared-architecture.md §3.2.
 *
 * Provides [DesignLanguage], [KutaMode], [KutaAccent], [KutaColors], and
 * [KutaTypography] via composition locals. When the user changes design/mode/
 * accent in Settings, the locals update and Compose recomposes — instant switch.
 *
 * For [DesignLanguage.MATERIAL], the caller must wrap content in TachiyomiTheme
 * (which provides MaterialTheme.colorScheme). KutaTheme itself doesn't depend
 * on TachiyomiTheme (it lives in presentation-core, TachiyomiTheme in app).
 *
 * For the 3 new designs, KutaTheme builds the design's [KutaColors] and
 * [KutaTypography] and provides them via locals.
 */
@Composable
fun KutaTheme(
    designLanguage: DesignLanguage,
    mode: KutaMode,
    accent: KutaAccent,
    content: @Composable () -> Unit,
) {
    val resolvedMode = resolveMode(mode)

    val colors = when (designLanguage) {
        DesignLanguage.NEON ->
            if (resolvedMode == KutaMode.DARK) NeonDarkColors(accent) else NeonLightColors(accent)
        DesignLanguage.NOTEBOOK ->
            if (resolvedMode == KutaMode.DARK) NotebookDarkColors(accent) else NotebookLightColors(accent)
        DesignLanguage.BRUTALIST ->
            if (resolvedMode == KutaMode.DARK) BrutalistDarkColors(accent) else BrutalistLightColors(accent)
        DesignLanguage.MATERIAL ->
            KutaColors.unspecified() // M3 components use MaterialTheme.colorScheme directly
    }

    val typography = when (designLanguage) {
        DesignLanguage.NEON -> NeonTypography
        DesignLanguage.NOTEBOOK -> NotebookTypography
        DesignLanguage.BRUTALIST -> BrutalistTypography
        DesignLanguage.MATERIAL -> MaterialTypographyFallback
    }

    CompositionLocalProvider(
        LocalDesignLanguage provides designLanguage,
        LocalKutaMode provides resolvedMode,
        LocalKutaAccent provides accent,
        LocalKutaColors provides colors,
        LocalKutaTypography provides typography,
    ) {
        content()
    }
}

@Composable
private fun resolveMode(mode: KutaMode): KutaMode {
    return if (mode == KutaMode.SYSTEM) {
        if (isSystemInDarkTheme()) KutaMode.DARK else KutaMode.LIGHT
    } else {
        mode
    }
}
