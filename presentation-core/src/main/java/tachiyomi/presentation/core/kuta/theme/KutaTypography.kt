package tachiyomi.presentation.core.kuta.theme

import androidx.compose.ui.text.TextStyle

/**
 * FORK: Phase 2 — Typography token definitions for all 4 design languages.
 * Per DOCS/design-system/00-shared-architecture.md §3 and per-design specs.
 *
 * A union of all typography slots. Designs that don't use a slot (e.g., Neon's
 * monoValue is null for Notebook) leave it null; callers null-check.
 */
data class KutaTypography(
    val display: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    val subtitle: TextStyle,
    val body: TextStyle,
    val bodySmall: TextStyle,
    val label: TextStyle,
    val button: TextStyle,
    // Neon-specific
    val monoValue: TextStyle?,
    val monoLarge: TextStyle?,
    // Notebook-specific
    val handWritten: TextStyle?,
)
