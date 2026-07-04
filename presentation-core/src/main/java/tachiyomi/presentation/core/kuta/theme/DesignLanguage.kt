package tachiyomi.presentation.core.kuta.theme

import androidx.compose.ui.graphics.Color

/**
 * FORK: Phase 2 — Design language enum, mode, and accent data class.
 * Per DOCS/design-system/00-shared-architecture.md §3.1.
 */

enum class DesignLanguage {
    NEON,
    NOTEBOOK,
    BRUTALIST,
    MATERIAL,
}

enum class KutaMode {
    LIGHT,
    DARK,
    SYSTEM,
}

data class KutaAccent(
    val id: String,
    val color: Color,
    val isCustom: Boolean,
)
