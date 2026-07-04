package tachiyomi.presentation.core.kuta.components

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * FORK: Phase 2 — Shared enums and API definitions for Kuta* components.
 * Per DOCS/design-system/00-shared-architecture.md §4.1.
 */

/** Button style variant — interpreted per-design. */
enum class KutaButtonVariant { PRIMARY, SECONDARY, DESTRUCTIVE, GHOST }

/** Card elevation level. */
enum class KutaCardElevation { FLAT, ELEVATED }

/** Toggle (switch/checkbox) state representation. */
enum class KutaToggleStyle { SWITCH, CHECKBOX }

/** Chip variant. */
enum class KutaChipVariant { DEFAULT, SELECTED, OUTLINED }

/** Input variant. */
enum class KutaInputVariant { DEFAULT, ERROR, DISABLED }

/** Badge variant. */
enum class KutaBadgeVariant { DEFAULT, ACCENT, WARNING, ERROR }

/** Navigation item for [KutaNavigationBar] / [KutaNavigationRail]. */
data class KutaNavigationItem(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit,
)

/** Tab item for [KutaTabRow]. */
data class KutaTabItem(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
)

/** Dropdown menu item. */
data class KutaDropdownItem(
    val label: String,
    val icon: ImageVector? = null,
    val onClick: () -> Unit,
)
