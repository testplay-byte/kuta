package tachiyomi.presentation.core.kuta.neon

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
 * FORK: Phase 2A — STUB Neon component implementations.
 * Each delegates to Material3. Phase 2B subagent will replace with real Neon
 * implementations per DOCS/design-system/01-neon.md §5.
 */

// TODO: Phase 2B — replace all stubs with real Neon implementations per 01-neon.md

@Composable fun NeonButton(text: String, onClick: () -> Unit, modifier: Modifier, enabled: Boolean, icon: ImageVector?, variant: KutaButtonVariant) = Material3Button(text, onClick, modifier, enabled, icon, variant)
@Composable fun NeonOutlinedButton(text: String, onClick: () -> Unit, modifier: Modifier, enabled: Boolean, icon: ImageVector?) = Material3OutlinedButton(text, onClick, modifier, enabled, icon)
@Composable fun NeonTextButton(text: String, onClick: () -> Unit, modifier: Modifier, enabled: Boolean, icon: ImageVector?) = Material3TextButton(text, onClick, modifier, enabled, icon)
@Composable fun NeonIconButton(icon: ImageVector, onClick: () -> Unit, modifier: Modifier, enabled: Boolean, contentDescription: String?) = Material3IconButton(icon, onClick, modifier, enabled, contentDescription)
@Composable fun NeonCard(modifier: Modifier, elevation: KutaCardElevation, content: @Composable () -> Unit) = Material3Card(modifier, elevation, content)
@Composable fun NeonElevatedCard(modifier: Modifier, content: @Composable () -> Unit) = Material3ElevatedCard(modifier, content)
@Composable fun NeonInput(value: String, onValueChange: (String) -> Unit, modifier: Modifier, placeholder: String, enabled: Boolean, variant: KutaInputVariant) = Material3Input(value, onValueChange, modifier, placeholder, enabled, variant)
@Composable fun NeonSearchInput(value: String, onValueChange: (String) -> Unit, modifier: Modifier, placeholder: String) = Material3SearchInput(value, onValueChange, modifier, placeholder)
@Composable fun NeonDialog(onDismissRequest: () -> Unit, modifier: Modifier, content: @Composable () -> Unit) = Material3Dialog(onDismissRequest, modifier, content)
@Composable fun NeonAlertDialog(title: String, message: String, onConfirm: () -> Unit, onDismiss: () -> Unit, modifier: Modifier, confirmText: String, dismissText: String) = Material3AlertDialog(title, message, onConfirm, onDismiss, modifier, confirmText, dismissText)
@Composable fun NeonBottomSheet(onDismissRequest: () -> Unit, modifier: Modifier, content: @Composable () -> Unit) = Material3BottomSheet(onDismissRequest, modifier, content)
@Composable fun NeonNavigationBar(items: List<KutaNavigationItem>, modifier: Modifier) = Material3NavigationBar(items, modifier)
@Composable fun NeonNavigationRail(items: List<KutaNavigationItem>, modifier: Modifier) = Material3NavigationRail(items, modifier)
@Composable fun NeonTabRow(tabs: List<KutaTabItem>, modifier: Modifier) = Material3TabRow(tabs, modifier)
@Composable fun NeonListItem(title: String, modifier: Modifier, subtitle: String?, icon: ImageVector?, trailing: @Composable (() -> Unit)?) = Material3ListItem(title, modifier, subtitle, icon, trailing)
@Composable fun NeonBadge(text: String, modifier: Modifier, variant: KutaBadgeVariant) = Material3Badge(text, modifier, variant)
@Composable fun NeonChip(text: String, modifier: Modifier, selected: Boolean, onClick: (() -> Unit)?) = Material3Chip(text, modifier, selected, onClick)
@Composable fun NeonToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier, style: KutaToggleStyle) = Material3Toggle(checked, onCheckedChange, modifier, style)
@Composable fun NeonSlider(value: Float, onValueChange: (Float) -> Unit, modifier: Modifier, valueRange: ClosedFloatingPointRange<Float>) = Material3Slider(value, onValueChange, modifier, valueRange)
@Composable fun NeonProgressBar(progress: Float?, modifier: Modifier) = Material3ProgressBar(progress, modifier)
@Composable fun NeonSnackbar(message: String, modifier: Modifier, actionLabel: String?, onAction: (() -> Unit)?) = Material3Snackbar(message, modifier, actionLabel, onAction)
@Composable fun NeonDropdownMenu(items: List<KutaDropdownItem>, onDismissRequest: () -> Unit, modifier: Modifier, expanded: Boolean) = Material3DropdownMenu(items, onDismissRequest, modifier, expanded)
@Composable fun NeonScaffold(modifier: Modifier, topBar: @Composable () -> Unit, bottomBar: @Composable () -> Unit, floatingActionButton: @Composable () -> Unit, content: @Composable (PaddingValues) -> Unit) = Material3Scaffold(modifier, topBar, bottomBar, floatingActionButton, content)
@Composable fun NeonTopAppBar(title: String, modifier: Modifier, onBack: (() -> Unit)?, actions: @Composable () -> Unit) = Material3TopAppBar(title, modifier, onBack, actions)
@Composable fun NeonBottomAppBar(modifier: Modifier, content: @Composable () -> Unit) = Material3BottomAppBar(modifier, content)
@Composable fun NeonFAB(icon: ImageVector, onClick: () -> Unit, modifier: Modifier, text: String?) = Material3FAB(icon, onClick, modifier, text)
@Composable fun NeonSkeleton(modifier: Modifier, height: Int) = Material3Skeleton(modifier, height)
@Composable fun NeonDivider(modifier: Modifier) = Material3Divider(modifier)
@Composable fun NeonAvatar(modifier: Modifier, size: Int, content: @Composable () -> Unit) = Material3Avatar(modifier, size, content)
