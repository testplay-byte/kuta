package tachiyomi.presentation.core.kuta.brutalist

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import tachiyomi.presentation.core.kuta.components.KutaBadgeVariant
import tachiyomi.presentation.core.kuta.components.KutaButtonVariant
import tachiyomi.presentation.core.kuta.components.KutaCardElevation
import tachiyomi.presentation.core.kuta.components.KutaDropdownItem
import tachiyomi.presentation.core.kuta.components.KutaInputVariant
import tachiyomi.presentation.core.kuta.components.KutaNavigationItem
import tachiyomi.presentation.core.kuta.components.KutaTabItem
import tachiyomi.presentation.core.kuta.components.KutaToggleStyle
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

/**
 * FORK: Phase 2A — STUB Brutalist component implementations.
 * Each delegates to Material3. Phase 2B subagent will replace with real Brutalist
 * implementations per DOCS/design-system/03-brutalist.md §5.
 */

// TODO: Phase 2B — replace all stubs with real Brutalist implementations per 03-brutalist.md

@Composable fun BrutalistButton(text: String, onClick: () -> Unit, modifier: Modifier, enabled: Boolean, icon: ImageVector?, variant: KutaButtonVariant) = Material3Button(text, onClick, modifier, enabled, icon, variant)
@Composable fun BrutalistOutlinedButton(text: String, onClick: () -> Unit, modifier: Modifier, enabled: Boolean, icon: ImageVector?) = Material3OutlinedButton(text, onClick, modifier, enabled, icon)
@Composable fun BrutalistTextButton(text: String, onClick: () -> Unit, modifier: Modifier, enabled: Boolean, icon: ImageVector?) = Material3TextButton(text, onClick, modifier, enabled, icon)
@Composable fun BrutalistIconButton(icon: ImageVector, onClick: () -> Unit, modifier: Modifier, enabled: Boolean, contentDescription: String?) = Material3IconButton(icon, onClick, modifier, enabled, contentDescription)
@Composable fun BrutalistCard(modifier: Modifier, elevation: KutaCardElevation, content: @Composable () -> Unit) = Material3Card(modifier, elevation, content)
@Composable fun BrutalistElevatedCard(modifier: Modifier, content: @Composable () -> Unit) = Material3ElevatedCard(modifier, content)
@Composable fun BrutalistInput(value: String, onValueChange: (String) -> Unit, modifier: Modifier, placeholder: String, enabled: Boolean, variant: KutaInputVariant) = Material3Input(value, onValueChange, modifier, placeholder, enabled, variant)
@Composable fun BrutalistSearchInput(value: String, onValueChange: (String) -> Unit, modifier: Modifier, placeholder: String) = Material3SearchInput(value, onValueChange, modifier, placeholder)
@Composable fun BrutalistDialog(onDismissRequest: () -> Unit, modifier: Modifier, content: @Composable () -> Unit) = Material3Dialog(onDismissRequest, modifier, content)
@Composable fun BrutalistAlertDialog(title: String, message: String, onConfirm: () -> Unit, onDismiss: () -> Unit, modifier: Modifier, confirmText: String, dismissText: String) = Material3AlertDialog(title, message, onConfirm, onDismiss, modifier, confirmText, dismissText)
@Composable fun BrutalistBottomSheet(onDismissRequest: () -> Unit, modifier: Modifier, content: @Composable () -> Unit) = Material3BottomSheet(onDismissRequest, modifier, content)
@Composable fun BrutalistNavigationBar(items: List<KutaNavigationItem>, modifier: Modifier) = Material3NavigationBar(items, modifier)
@Composable fun BrutalistNavigationRail(items: List<KutaNavigationItem>, modifier: Modifier) = Material3NavigationRail(items, modifier)
@Composable fun BrutalistTabRow(tabs: List<KutaTabItem>, modifier: Modifier) = Material3TabRow(tabs, modifier)
@Composable fun BrutalistListItem(title: String, modifier: Modifier, subtitle: String?, icon: ImageVector?, trailing: @Composable (() -> Unit)?) = Material3ListItem(title, modifier, subtitle, icon, trailing)
@Composable fun BrutalistBadge(text: String, modifier: Modifier, variant: KutaBadgeVariant) = Material3Badge(text, modifier, variant)
@Composable fun BrutalistChip(text: String, modifier: Modifier, selected: Boolean, onClick: (() -> Unit)?) = Material3Chip(text, modifier, selected, onClick)
@Composable fun BrutalistToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier, style: KutaToggleStyle) = Material3Toggle(checked, onCheckedChange, modifier, style)
@Composable fun BrutalistSlider(value: Float, onValueChange: (Float) -> Unit, modifier: Modifier, valueRange: ClosedFloatingPointRange<Float>) = Material3Slider(value, onValueChange, modifier, valueRange)
@Composable fun BrutalistProgressBar(progress: Float?, modifier: Modifier) = Material3ProgressBar(progress, modifier)
@Composable fun BrutalistSnackbar(message: String, modifier: Modifier, actionLabel: String?, onAction: (() -> Unit)?) = Material3Snackbar(message, modifier, actionLabel, onAction)
@Composable fun BrutalistDropdownMenu(items: List<KutaDropdownItem>, onDismissRequest: () -> Unit, modifier: Modifier, expanded: Boolean) = Material3DropdownMenu(items, onDismissRequest, modifier, expanded)
@Composable fun BrutalistScaffold(modifier: Modifier, topBar: @Composable () -> Unit, bottomBar: @Composable () -> Unit, floatingActionButton: @Composable () -> Unit, content: @Composable (PaddingValues) -> Unit) = Material3Scaffold(modifier, topBar, bottomBar, floatingActionButton, content)
@Composable fun BrutalistTopAppBar(title: String, modifier: Modifier, onBack: (() -> Unit)?, actions: @Composable () -> Unit) = Material3TopAppBar(title, modifier, onBack, actions)
@Composable fun BrutalistBottomAppBar(modifier: Modifier, content: @Composable () -> Unit) = Material3BottomAppBar(modifier, content)
@Composable fun BrutalistFAB(icon: ImageVector, onClick: () -> Unit, modifier: Modifier, text: String?) = Material3FAB(icon, onClick, modifier, text)
@Composable fun BrutalistSkeleton(modifier: Modifier, height: Int) = Material3Skeleton(modifier, height)
@Composable fun BrutalistDivider(modifier: Modifier) = Material3Divider(modifier)
@Composable fun BrutalistAvatar(modifier: Modifier, size: Int, content: @Composable () -> Unit) = Material3Avatar(modifier, size, content)
