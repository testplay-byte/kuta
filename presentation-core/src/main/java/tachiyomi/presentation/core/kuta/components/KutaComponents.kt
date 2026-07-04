package tachiyomi.presentation.core.kuta.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import tachiyomi.presentation.core.kuta.brutalist.BrutalistAvatar
import tachiyomi.presentation.core.kuta.brutalist.BrutalistBadge
import tachiyomi.presentation.core.kuta.brutalist.BrutalistBottomAppBar
import tachiyomi.presentation.core.kuta.brutalist.BrutalistBottomSheet
import tachiyomi.presentation.core.kuta.brutalist.BrutalistButton
import tachiyomi.presentation.core.kuta.brutalist.BrutalistCard
import tachiyomi.presentation.core.kuta.brutalist.BrutalistChip
import tachiyomi.presentation.core.kuta.brutalist.BrutalistDialog
import tachiyomi.presentation.core.kuta.brutalist.BrutalistDivider
import tachiyomi.presentation.core.kuta.brutalist.BrutalistDropdownMenu
import tachiyomi.presentation.core.kuta.brutalist.BrutalistElevatedCard
import tachiyomi.presentation.core.kuta.brutalist.BrutalistFAB
import tachiyomi.presentation.core.kuta.brutalist.BrutalistAlertDialog
import tachiyomi.presentation.core.kuta.brutalist.BrutalistIconButton
import tachiyomi.presentation.core.kuta.brutalist.BrutalistInput
import tachiyomi.presentation.core.kuta.brutalist.BrutalistListItem
import tachiyomi.presentation.core.kuta.brutalist.BrutalistNavigationBar
import tachiyomi.presentation.core.kuta.brutalist.BrutalistNavigationRail
import tachiyomi.presentation.core.kuta.brutalist.BrutalistOutlinedButton
import tachiyomi.presentation.core.kuta.brutalist.BrutalistProgressBar
import tachiyomi.presentation.core.kuta.brutalist.BrutalistScaffold
import tachiyomi.presentation.core.kuta.brutalist.BrutalistSearchInput
import tachiyomi.presentation.core.kuta.brutalist.BrutalistSkeleton
import tachiyomi.presentation.core.kuta.brutalist.BrutalistSlider
import tachiyomi.presentation.core.kuta.brutalist.BrutalistSnackbar
import tachiyomi.presentation.core.kuta.brutalist.BrutalistTabRow
import tachiyomi.presentation.core.kuta.brutalist.BrutalistTextButton
import tachiyomi.presentation.core.kuta.brutalist.BrutalistToggle
import tachiyomi.presentation.core.kuta.brutalist.BrutalistTopAppBar
import tachiyomi.presentation.core.kuta.theme.DesignLanguage
import tachiyomi.presentation.core.kuta.theme.LocalDesignLanguage
import tachiyomi.presentation.core.kuta.material.Material3Avatar
import tachiyomi.presentation.core.kuta.material.Material3Badge
import tachiyomi.presentation.core.kuta.material.Material3BottomAppBar
import tachiyomi.presentation.core.kuta.material.Material3BottomSheet
import tachiyomi.presentation.core.kuta.material.Material3Button
import tachiyomi.presentation.core.kuta.material.Material3Card
import tachiyomi.presentation.core.kuta.material.Material3Chip
import tachiyomi.presentation.core.kuta.material.Material3Dialog
import tachiyomi.presentation.core.kuta.material.Material3Divider
import tachiyomi.presentation.core.kuta.material.Material3DropdownMenu
import tachiyomi.presentation.core.kuta.material.Material3ElevatedCard
import tachiyomi.presentation.core.kuta.material.Material3FAB
import tachiyomi.presentation.core.kuta.material.Material3AlertDialog
import tachiyomi.presentation.core.kuta.material.Material3IconButton
import tachiyomi.presentation.core.kuta.material.Material3Input
import tachiyomi.presentation.core.kuta.material.Material3ListItem
import tachiyomi.presentation.core.kuta.material.Material3NavigationBar
import tachiyomi.presentation.core.kuta.material.Material3NavigationRail
import tachiyomi.presentation.core.kuta.material.Material3OutlinedButton
import tachiyomi.presentation.core.kuta.material.Material3ProgressBar
import tachiyomi.presentation.core.kuta.material.Material3Scaffold
import tachiyomi.presentation.core.kuta.material.Material3SearchInput
import tachiyomi.presentation.core.kuta.material.Material3Skeleton
import tachiyomi.presentation.core.kuta.material.Material3Slider
import tachiyomi.presentation.core.kuta.material.Material3Snackbar
import tachiyomi.presentation.core.kuta.material.Material3TabRow
import tachiyomi.presentation.core.kuta.material.Material3TextButton
import tachiyomi.presentation.core.kuta.material.Material3Toggle
import tachiyomi.presentation.core.kuta.material.Material3TopAppBar
import tachiyomi.presentation.core.kuta.neon.NeonAvatar
import tachiyomi.presentation.core.kuta.neon.NeonBadge
import tachiyomi.presentation.core.kuta.neon.NeonBottomAppBar
import tachiyomi.presentation.core.kuta.neon.NeonBottomSheet
import tachiyomi.presentation.core.kuta.neon.NeonButton
import tachiyomi.presentation.core.kuta.neon.NeonCard
import tachiyomi.presentation.core.kuta.neon.NeonChip
import tachiyomi.presentation.core.kuta.neon.NeonDialog
import tachiyomi.presentation.core.kuta.neon.NeonDivider
import tachiyomi.presentation.core.kuta.neon.NeonDropdownMenu
import tachiyomi.presentation.core.kuta.neon.NeonElevatedCard
import tachiyomi.presentation.core.kuta.neon.NeonFAB
import tachiyomi.presentation.core.kuta.neon.NeonAlertDialog
import tachiyomi.presentation.core.kuta.neon.NeonIconButton
import tachiyomi.presentation.core.kuta.neon.NeonInput
import tachiyomi.presentation.core.kuta.neon.NeonListItem
import tachiyomi.presentation.core.kuta.neon.NeonNavigationBar
import tachiyomi.presentation.core.kuta.neon.NeonNavigationRail
import tachiyomi.presentation.core.kuta.neon.NeonOutlinedButton
import tachiyomi.presentation.core.kuta.neon.NeonProgressBar
import tachiyomi.presentation.core.kuta.neon.NeonScaffold
import tachiyomi.presentation.core.kuta.neon.NeonSearchInput
import tachiyomi.presentation.core.kuta.neon.NeonSkeleton
import tachiyomi.presentation.core.kuta.neon.NeonSlider
import tachiyomi.presentation.core.kuta.neon.NeonSnackbar
import tachiyomi.presentation.core.kuta.neon.NeonTabRow
import tachiyomi.presentation.core.kuta.neon.NeonTextButton
import tachiyomi.presentation.core.kuta.neon.NeonToggle
import tachiyomi.presentation.core.kuta.neon.NeonTopAppBar
import tachiyomi.presentation.core.kuta.notebook.NotebookAvatar
import tachiyomi.presentation.core.kuta.notebook.NotebookBadge
import tachiyomi.presentation.core.kuta.notebook.NotebookBottomAppBar
import tachiyomi.presentation.core.kuta.notebook.NotebookBottomSheet
import tachiyomi.presentation.core.kuta.notebook.NotebookButton
import tachiyomi.presentation.core.kuta.notebook.NotebookCard
import tachiyomi.presentation.core.kuta.notebook.NotebookChip
import tachiyomi.presentation.core.kuta.notebook.NotebookDialog
import tachiyomi.presentation.core.kuta.notebook.NotebookDivider
import tachiyomi.presentation.core.kuta.notebook.NotebookDropdownMenu
import tachiyomi.presentation.core.kuta.notebook.NotebookElevatedCard
import tachiyomi.presentation.core.kuta.notebook.NotebookFAB
import tachiyomi.presentation.core.kuta.notebook.NotebookAlertDialog
import tachiyomi.presentation.core.kuta.notebook.NotebookIconButton
import tachiyomi.presentation.core.kuta.notebook.NotebookInput
import tachiyomi.presentation.core.kuta.notebook.NotebookListItem
import tachiyomi.presentation.core.kuta.notebook.NotebookNavigationBar
import tachiyomi.presentation.core.kuta.notebook.NotebookNavigationRail
import tachiyomi.presentation.core.kuta.notebook.NotebookOutlinedButton
import tachiyomi.presentation.core.kuta.notebook.NotebookProgressBar
import tachiyomi.presentation.core.kuta.notebook.NotebookScaffold
import tachiyomi.presentation.core.kuta.notebook.NotebookSearchInput
import tachiyomi.presentation.core.kuta.notebook.NotebookSkeleton
import tachiyomi.presentation.core.kuta.notebook.NotebookSlider
import tachiyomi.presentation.core.kuta.notebook.NotebookSnackbar
import tachiyomi.presentation.core.kuta.notebook.NotebookTabRow
import tachiyomi.presentation.core.kuta.notebook.NotebookTextButton
import tachiyomi.presentation.core.kuta.notebook.NotebookToggle
import tachiyomi.presentation.core.kuta.notebook.NotebookTopAppBar

/**
 * FORK: Phase 2 — Kuta* component delegators.
 * Per DOCS/design-system/00-shared-architecture.md §4.
 *
 * Each function reads [LocalDesignLanguage] and delegates to the active
 * design's implementation. Screens use these — never raw M3 or design-specific
 * components.
 *
 * During Phase 2A, Neon/Notebook/Brutalist implementations are stubs that
 * delegate to Material3. Phase 2B replaces them with real implementations.
 */

// ===== Buttons =====

@Composable fun KutaButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null, variant: KutaButtonVariant = KutaButtonVariant.PRIMARY) {
    when (LocalDesignLanguage.current) { DesignLanguage.NEON -> NeonButton(text, onClick, modifier, enabled, icon, variant); DesignLanguage.NOTEBOOK -> NotebookButton(text, onClick, modifier, enabled, icon, variant); DesignLanguage.BRUTALIST -> BrutalistButton(text, onClick, modifier, enabled, icon, variant); DesignLanguage.MATERIAL -> Material3Button(text, onClick, modifier, enabled, icon, variant) }
}
@Composable fun KutaOutlinedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null) {
    when (LocalDesignLanguage.current) { DesignLanguage.NEON -> NeonOutlinedButton(text, onClick, modifier, enabled, icon); DesignLanguage.NOTEBOOK -> NotebookOutlinedButton(text, onClick, modifier, enabled, icon); DesignLanguage.BRUTALIST -> BrutalistOutlinedButton(text, onClick, modifier, enabled, icon); DesignLanguage.MATERIAL -> Material3OutlinedButton(text, onClick, modifier, enabled, icon) }
}
@Composable fun KutaTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null) {
    when (LocalDesignLanguage.current) { DesignLanguage.NEON -> NeonTextButton(text, onClick, modifier, enabled, icon); DesignLanguage.NOTEBOOK -> NotebookTextButton(text, onClick, modifier, enabled, icon); DesignLanguage.BRUTALIST -> BrutalistTextButton(text, onClick, modifier, enabled, icon); DesignLanguage.MATERIAL -> Material3TextButton(text, onClick, modifier, enabled, icon) }
}
@Composable fun KutaIconButton(icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, contentDescription: String? = null) {
    when (LocalDesignLanguage.current) { DesignLanguage.NEON -> NeonIconButton(icon, onClick, modifier, enabled, contentDescription); DesignLanguage.NOTEBOOK -> NotebookIconButton(icon, onClick, modifier, enabled, contentDescription); DesignLanguage.BRUTALIST -> BrutalistIconButton(icon, onClick, modifier, enabled, contentDescription); DesignLanguage.MATERIAL -> Material3IconButton(icon, onClick, modifier, enabled, contentDescription) }
}

// ===== Cards =====

@Composable fun KutaCard(modifier: Modifier = Modifier, elevation: KutaCardElevation = KutaCardElevation.FLAT, content: @Composable () -> Unit) {
    when (LocalDesignLanguage.current) { DesignLanguage.NEON -> NeonCard(modifier, elevation, content); DesignLanguage.NOTEBOOK -> NotebookCard(modifier, elevation, content); DesignLanguage.BRUTALIST -> BrutalistCard(modifier, elevation, content); DesignLanguage.MATERIAL -> Material3Card(modifier, elevation, content) }
}
@Composable fun KutaElevatedCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    when (LocalDesignLanguage.current) { DesignLanguage.NEON -> NeonElevatedCard(modifier, content); DesignLanguage.NOTEBOOK -> NotebookElevatedCard(modifier, content); DesignLanguage.BRUTALIST -> BrutalistElevatedCard(modifier, content); DesignLanguage.MATERIAL -> Material3ElevatedCard(modifier, content) }
}

// ===== Inputs =====

@Composable fun KutaInput(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, placeholder: String = "", enabled: Boolean = true, variant: KutaInputVariant = KutaInputVariant.DEFAULT) {
    when (LocalDesignLanguage.current) { DesignLanguage.NEON -> NeonInput(value, onValueChange, modifier, placeholder, enabled, variant); DesignLanguage.NOTEBOOK -> NotebookInput(value, onValueChange, modifier, placeholder, enabled, variant); DesignLanguage.BRUTALIST -> BrutalistInput(value, onValueChange, modifier, placeholder, enabled, variant); DesignLanguage.MATERIAL -> Material3Input(value, onValueChange, modifier, placeholder, enabled, variant) }
}
@Composable fun KutaSearchInput(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, placeholder: String = "Search") {
    when (LocalDesignLanguage.current) { DesignLanguage.NEON -> NeonSearchInput(value, onValueChange, modifier, placeholder); DesignLanguage.NOTEBOOK -> NotebookSearchInput(value, onValueChange, modifier, placeholder); DesignLanguage.BRUTALIST -> BrutalistSearchInput(value, onValueChange, modifier, placeholder); DesignLanguage.MATERIAL -> Material3SearchInput(value, onValueChange, modifier, placeholder) }
}

// ===== Dialogs / Sheets =====

@Composable fun KutaDialog(onDismissRequest: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    when (LocalDesignLanguage.current) { DesignLanguage.NEON -> NeonDialog(onDismissRequest, modifier, content); DesignLanguage.NOTEBOOK -> NotebookDialog(onDismissRequest, modifier, content); DesignLanguage.BRUTALIST -> BrutalistDialog(onDismissRequest, modifier, content); DesignLanguage.MATERIAL -> Material3Dialog(onDismissRequest, modifier, content) }
}
@Composable fun KutaAlertDialog(title: String, message: String, onConfirm: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier, confirmText: String = "OK", dismissText: String = "Cancel") {
    when (LocalDesignLanguage.current) { DesignLanguage.NEON -> NeonAlertDialog(title, message, onConfirm, onDismiss, modifier, confirmText, dismissText); DesignLanguage.NOTEBOOK -> NotebookAlertDialog(title, message, onConfirm, onDismiss, modifier, confirmText, dismissText); DesignLanguage.BRUTALIST -> BrutalistAlertDialog(title, message, onConfirm, onDismiss, modifier, confirmText, dismissText); DesignLanguage.MATERIAL -> Material3AlertDialog(title, message, onConfirm, onDismiss, modifier, confirmText, dismissText) }
}
@Composable fun KutaBottomSheet(onDismissRequest: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    when (LocalDesignLanguage.current) { DesignLanguage.NEON -> NeonBottomSheet(onDismissRequest, modifier, content); DesignLanguage.NOTEBOOK -> NotebookBottomSheet(onDismissRequest, modifier, content); DesignLanguage.BRUTALIST -> BrutalistBottomSheet(onDismissRequest, modifier, content); DesignLanguage.MATERIAL -> Material3BottomSheet(onDismissRequest, modifier, content) }
}

// ===== Navigation =====

@Composable fun KutaNavigationBar(items: List<KutaNavigationItem>, modifier: Modifier = Modifier) {
    when (LocalDesignLanguage.current) { DesignLanguage.NEON -> NeonNavigationBar(items, modifier); DesignLanguage.NOTEBOOK -> NotebookNavigationBar(items, modifier); DesignLanguage.BRUTALIST -> BrutalistNavigationBar(items, modifier); DesignLanguage.MATERIAL -> Material3NavigationBar(items, modifier) }
}
@Composable fun KutaNavigationRail(items: List<KutaNavigationItem>, modifier: Modifier = Modifier) {
    when (LocalDesignLanguage.current) { DesignLanguage.NEON -> NeonNavigationRail(items, modifier); DesignLanguage.NOTEBOOK -> NotebookNavigationRail(items, modifier); DesignLanguage.BRUTALIST -> BrutalistNavigationRail(items, modifier); DesignLanguage.MATERIAL -> Material3NavigationRail(items, modifier) }
}
@Composable fun KutaTabRow(tabs: List<KutaTabItem>, modifier: Modifier = Modifier) {
    when (LocalDesignLanguage.current) { DesignLanguage.NEON -> NeonTabRow(tabs, modifier); DesignLanguage.NOTEBOOK -> NotebookTabRow(tabs, modifier); DesignLanguage.BRUTALIST -> BrutalistTabRow(tabs, modifier); DesignLanguage.MATERIAL -> Material3TabRow(tabs, modifier) }
}

// ===== Items / Badges / Chips =====

@Composable fun KutaListItem(title: String, modifier: Modifier = Modifier, subtitle: String? = null, icon: ImageVector? = null, trailing: @Composable (() -> Unit)? = null) {
    when (LocalDesignLanguage.current) { DesignLanguage.NEON -> NeonListItem(title, modifier, subtitle, icon, trailing); DesignLanguage.NOTEBOOK -> NotebookListItem(title, modifier, subtitle, icon, trailing); DesignLanguage.BRUTALIST -> BrutalistListItem(title, modifier, subtitle, icon, trailing); DesignLanguage.MATERIAL -> Material3ListItem(title, modifier, subtitle, icon, trailing) }
}
@Composable fun KutaBadge(text: String, modifier: Modifier = Modifier, variant: KutaBadgeVariant = KutaBadgeVariant.DEFAULT) {
    when (LocalDesignLanguage.current) { DesignLanguage.NEON -> NeonBadge(text, modifier, variant); DesignLanguage.NOTEBOOK -> NotebookBadge(text, modifier, variant); DesignLanguage.BRUTALIST -> BrutalistBadge(text, modifier, variant); DesignLanguage.MATERIAL -> Material3Badge(text, modifier, variant) }
}
@Composable fun KutaChip(text: String, modifier: Modifier = Modifier, selected: Boolean = false, onClick: (() -> Unit)? = null) {
    when (LocalDesignLanguage.current) { DesignLanguage.NEON -> NeonChip(text, modifier, selected, onClick); DesignLanguage.NOTEBOOK -> NotebookChip(text, modifier, selected, onClick); DesignLanguage.BRUTALIST -> BrutalistChip(text, modifier, selected, onClick); DesignLanguage.MATERIAL -> Material3Chip(text, modifier, selected, onClick) }
}

// ===== Controls =====

@Composable fun KutaToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, style: KutaToggleStyle = KutaToggleStyle.SWITCH) {
    when (LocalDesignLanguage.current) { DesignLanguage.NEON -> NeonToggle(checked, onCheckedChange, modifier, style); DesignLanguage.NOTEBOOK -> NotebookToggle(checked, onCheckedChange, modifier, style); DesignLanguage.BRUTALIST -> BrutalistToggle(checked, onCheckedChange, modifier, style); DesignLanguage.MATERIAL -> Material3Toggle(checked, onCheckedChange, modifier, style) }
}
@Composable fun KutaSlider(value: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier, valueRange: ClosedFloatingPointRange<Float> = 0f..1f) {
    when (LocalDesignLanguage.current) { DesignLanguage.NEON -> NeonSlider(value, onValueChange, modifier, valueRange); DesignLanguage.NOTEBOOK -> NotebookSlider(value, onValueChange, modifier, valueRange); DesignLanguage.BRUTALIST -> BrutalistSlider(value, onValueChange, modifier, valueRange); DesignLanguage.MATERIAL -> Material3Slider(value, onValueChange, modifier, valueRange) }
}
@Composable fun KutaProgressBar(progress: Float?, modifier: Modifier = Modifier) {
    when (LocalDesignLanguage.current) { DesignLanguage.NEON -> NeonProgressBar(progress, modifier); DesignLanguage.NOTEBOOK -> NotebookProgressBar(progress, modifier); DesignLanguage.BRUTALIST -> BrutalistProgressBar(progress, modifier); DesignLanguage.MATERIAL -> Material3ProgressBar(progress, modifier) }
}

// ===== Feedback =====

@Composable fun KutaSnackbar(message: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    when (LocalDesignLanguage.current) { DesignLanguage.NEON -> NeonSnackbar(message, modifier, actionLabel, onAction); DesignLanguage.NOTEBOOK -> NotebookSnackbar(message, modifier, actionLabel, onAction); DesignLanguage.BRUTALIST -> BrutalistSnackbar(message, modifier, actionLabel, onAction); DesignLanguage.MATERIAL -> Material3Snackbar(message, modifier, actionLabel, onAction) }
}
@Composable fun KutaDropdownMenu(items: List<KutaDropdownItem>, onDismissRequest: () -> Unit, modifier: Modifier = Modifier, expanded: Boolean = true) {
    when (LocalDesignLanguage.current) { DesignLanguage.NEON -> NeonDropdownMenu(items, onDismissRequest, modifier, expanded); DesignLanguage.NOTEBOOK -> NotebookDropdownMenu(items, onDismissRequest, modifier, expanded); DesignLanguage.BRUTALIST -> BrutalistDropdownMenu(items, onDismissRequest, modifier, expanded); DesignLanguage.MATERIAL -> Material3DropdownMenu(items, onDismissRequest, modifier, expanded) }
}

// ===== Layout =====

@Composable fun KutaScaffold(modifier: Modifier = Modifier, topBar: @Composable () -> Unit = {}, bottomBar: @Composable () -> Unit = {}, floatingActionButton: @Composable () -> Unit = {}, content: @Composable (PaddingValues) -> Unit) {
    when (LocalDesignLanguage.current) { DesignLanguage.NEON -> NeonScaffold(modifier, topBar, bottomBar, floatingActionButton, content); DesignLanguage.NOTEBOOK -> NotebookScaffold(modifier, topBar, bottomBar, floatingActionButton, content); DesignLanguage.BRUTALIST -> BrutalistScaffold(modifier, topBar, bottomBar, floatingActionButton, content); DesignLanguage.MATERIAL -> Material3Scaffold(modifier, topBar, bottomBar, floatingActionButton, content) }
}
@Composable fun KutaTopAppBar(title: String, modifier: Modifier = Modifier, onBack: (() -> Unit)? = null, actions: @Composable () -> Unit = {}) {
    when (LocalDesignLanguage.current) { DesignLanguage.NEON -> NeonTopAppBar(title, modifier, onBack, actions); DesignLanguage.NOTEBOOK -> NotebookTopAppBar(title, modifier, onBack, actions); DesignLanguage.BRUTALIST -> BrutalistTopAppBar(title, modifier, onBack, actions); DesignLanguage.MATERIAL -> Material3TopAppBar(title, modifier, onBack, actions) }
}
@Composable fun KutaBottomAppBar(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    when (LocalDesignLanguage.current) { DesignLanguage.NEON -> NeonBottomAppBar(modifier, content); DesignLanguage.NOTEBOOK -> NotebookBottomAppBar(modifier, content); DesignLanguage.BRUTALIST -> BrutalistBottomAppBar(modifier, content); DesignLanguage.MATERIAL -> Material3BottomAppBar(modifier, content) }
}
@Composable fun KutaFAB(icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, text: String? = null) {
    when (LocalDesignLanguage.current) { DesignLanguage.NEON -> NeonFAB(icon, onClick, modifier, text); DesignLanguage.NOTEBOOK -> NotebookFAB(icon, onClick, modifier, text); DesignLanguage.BRUTALIST -> BrutalistFAB(icon, onClick, modifier, text); DesignLanguage.MATERIAL -> Material3FAB(icon, onClick, modifier, text) }
}

// ===== Misc =====

@Composable fun KutaSkeleton(modifier: Modifier = Modifier, height: Int = 48) {
    when (LocalDesignLanguage.current) { DesignLanguage.NEON -> NeonSkeleton(modifier, height); DesignLanguage.NOTEBOOK -> NotebookSkeleton(modifier, height); DesignLanguage.BRUTALIST -> BrutalistSkeleton(modifier, height); DesignLanguage.MATERIAL -> Material3Skeleton(modifier, height) }
}
@Composable fun KutaDivider(modifier: Modifier = Modifier) {
    when (LocalDesignLanguage.current) { DesignLanguage.NEON -> NeonDivider(modifier); DesignLanguage.NOTEBOOK -> NotebookDivider(modifier); DesignLanguage.BRUTALIST -> BrutalistDivider(modifier); DesignLanguage.MATERIAL -> Material3Divider(modifier) }
}
@Composable fun KutaAvatar(modifier: Modifier = Modifier, size: Int = 40, content: @Composable () -> Unit) {
    when (LocalDesignLanguage.current) { DesignLanguage.NEON -> NeonAvatar(modifier, size, content); DesignLanguage.NOTEBOOK -> NotebookAvatar(modifier, size, content); DesignLanguage.BRUTALIST -> BrutalistAvatar(modifier, size, content); DesignLanguage.MATERIAL -> Material3Avatar(modifier, size, content) }
}
