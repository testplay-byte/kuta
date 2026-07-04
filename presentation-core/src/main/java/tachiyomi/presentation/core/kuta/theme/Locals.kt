package tachiyomi.presentation.core.kuta.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf

/**
 * FORK: Phase 2 — Composition locals for the Kuta design system.
 * Per DOCS/design-system/00-shared-architecture.md §3.1.
 *
 * These locals provide the active design language, mode, accent, colors, and
 * typography to all composables in the tree. [KutaTheme] is the only thing
 * that should provide these.
 */

val LocalDesignLanguage = compositionLocalOf<DesignLanguage> {
    error("No DesignLanguage provided — wrap content in KutaTheme")
}

val LocalKutaMode = compositionLocalOf<KutaMode> {
    error("No KutaMode provided — wrap content in KutaTheme")
}

val LocalKutaAccent = compositionLocalOf<KutaAccent> {
    error("No KutaAccent provided — wrap content in KutaTheme")
}

val LocalKutaColors = compositionLocalOf<KutaColors> {
    error("No KutaColors provided — wrap content in KutaTheme")
}

val LocalKutaTypography = compositionLocalOf<KutaTypography> {
    error("No KutaTypography provided — wrap content in KutaTheme")
}

/** Convenience accessor for the current [KutaColors]. */
val kutaColors: KutaColors
    @Composable
    @ReadOnlyComposable
    get() = LocalKutaColors.current

/** Convenience accessor for the current [KutaTypography]. */
val kutaTypography: KutaTypography
    @Composable
    @ReadOnlyComposable
    get() = LocalKutaTypography.current
