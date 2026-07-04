package tachiyomi.presentation.core.kuta.theme

import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.milliseconds

/**
 * FORK: Phase 2 — Shared spacing scale and motion durations.
 * Per DOCS/design-system/00-shared-architecture.md §5.1 and §5.2.
 * All 4 designs use these same values; differences are in HOW they're applied.
 */
object KutaSpacing {
    val xs = 4.dp    // tight: icon-to-text gap
    val sm = 8.dp    // default: list item padding
    val md = 12.dp   // comfortable: card inner padding
    val lg = 16.dp   // section: card outer padding
    val xl = 24.dp   // large: section gap
    val xxl = 32.dp  // huge: screen edge padding
    val xxxl = 48.dp // hero: top of screen
}

object KutaMotion {
    val instant = 0.milliseconds       // no animation (state changes)
    val fast = 100.milliseconds        // hover, press
    val normal = 200.milliseconds      // card lift, toggle
    val slow = 300.milliseconds        // sheet, dialog
    val slower = 500.milliseconds      // screen transition
}
