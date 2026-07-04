package tachiyomi.presentation.core.kuta.material

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.kuta.components.KutaBadgeVariant
import tachiyomi.presentation.core.kuta.components.KutaButtonVariant
import tachiyomi.presentation.core.kuta.components.KutaCardElevation
import tachiyomi.presentation.core.kuta.components.KutaChipVariant
import tachiyomi.presentation.core.kuta.components.KutaDropdownItem
import tachiyomi.presentation.core.kuta.components.KutaInputVariant
import tachiyomi.presentation.core.kuta.components.KutaNavigationItem
import tachiyomi.presentation.core.kuta.components.KutaTabItem
import tachiyomi.presentation.core.kuta.components.KutaToggleStyle

/**
 * FORK: Phase 2 — Material3 component wrappers.
 * Per DOCS/design-system/04-material.md §3.
 *
 * Each function wraps the corresponding androidx.compose.material3 component.
 * Used when [DesignLanguage.MATERIAL] is active, and as the fallback for
 * Neon/Notebook/Brutalist stubs during Phase 2A.
 */

// ===== Buttons =====

@Composable
fun Material3Button(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    variant: KutaButtonVariant = KutaButtonVariant.PRIMARY,
) {
    when (variant) {
        KutaButtonVariant.PRIMARY -> Button(onClick = onClick, modifier = modifier, enabled = enabled) {
            if (icon != null) { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)); }
            Text(text)
        }
        KutaButtonVariant.SECONDARY -> OutlinedButton(onClick = onClick, modifier = modifier, enabled = enabled) {
            if (icon != null) { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)); }
            Text(text)
        }
        KutaButtonVariant.DESTRUCTIVE -> Button(onClick = onClick, modifier = modifier, enabled = enabled) {
            if (icon != null) { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)); }
            Text(text)
        }
        KutaButtonVariant.GHOST -> TextButton(onClick = onClick, modifier = modifier, enabled = enabled) {
            if (icon != null) { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)); }
            Text(text)
        }
    }
}

@Composable
fun Material3OutlinedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null) {
    OutlinedButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(text)
    }
}

@Composable
fun Material3TextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null) {
    TextButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(text)
    }
}

@Composable
fun Material3IconButton(icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, contentDescription: String? = null) {
    IconButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        Icon(icon, contentDescription = contentDescription)
    }
}

// ===== Cards =====

@Composable
fun Material3Card(modifier: Modifier = Modifier, elevation: KutaCardElevation = KutaCardElevation.FLAT, content: @Composable () -> Unit) {
    if (elevation == KutaCardElevation.ELEVATED) {
        ElevatedCard(modifier = modifier) { content() }
    } else {
        Card(modifier = modifier) { content() }
    }
}

@Composable
fun Material3ElevatedCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    ElevatedCard(modifier = modifier) { content() }
}

// ===== Inputs =====

@Composable
fun Material3Input(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, placeholder: String = "", enabled: Boolean = true, variant: KutaInputVariant = KutaInputVariant.DEFAULT) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = { Text(placeholder) },
        enabled = enabled && variant != KutaInputVariant.DISABLED,
        isError = variant == KutaInputVariant.ERROR,
        singleLine = true,
    )
}

@Composable
fun Material3SearchInput(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, placeholder: String = "Search") {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
    )
}

// ===== Dialogs / Sheets =====

@Composable
fun Material3Dialog(onDismissRequest: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {},
        modifier = modifier,
        text = { content() },
    )
}

@Composable
fun Material3AlertDialog(title: String, message: String, onConfirm: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier, confirmText: String = "OK", dismissText: String = "Cancel") {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { Button(onConfirm) { Text(confirmText) } },
        dismissButton = { TextButton(onDismiss) { Text(dismissText) } },
        modifier = modifier,
    )
}

@Composable
fun Material3BottomSheet(onDismissRequest: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState, modifier = modifier) {
        content()
    }
}

// ===== Navigation =====

@Composable
fun Material3NavigationBar(items: List<KutaNavigationItem>, modifier: Modifier = Modifier) {
    NavigationBar(modifier = modifier) {
        items.forEach { item ->
            NavigationBarItem(
                selected = item.selected,
                onClick = item.onClick,
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
            )
        }
    }
}

@Composable
fun Material3NavigationRail(items: List<KutaNavigationItem>, modifier: Modifier = Modifier) {
    NavigationRail(modifier = modifier) {
        items.forEach { item ->
            NavigationRailItem(
                selected = item.selected,
                onClick = item.onClick,
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
            )
        }
    }
}

@Composable
fun Material3TabRow(tabs: List<KutaTabItem>, modifier: Modifier = Modifier) {
    TabRow(selectedTabIndex = tabs.indexOfFirst { it.selected }.coerceAtLeast(0), modifier = modifier) {
        tabs.forEach { tab ->
            Tab(selected = tab.selected, onClick = tab.onClick, text = { Text(tab.label) })
        }
    }
}

// ===== Items / Badges / Chips =====

@Composable
fun Material3ListItem(title: String, modifier: Modifier = Modifier, subtitle: String? = null, icon: ImageVector? = null, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) { Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp)); }
        if (icon != null) { Modifier.size(12.dp).let {} }
        Column(Modifier.weight(1f).padding(start = if (icon != null) 16.dp else 0.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing?.invoke()
    }
}

@Composable
fun Material3Badge(text: String, modifier: Modifier = Modifier, variant: KutaBadgeVariant = KutaBadgeVariant.DEFAULT) {
    Badge(modifier = modifier) { Text(text) }
}

@Composable
fun Material3Chip(text: String, modifier: Modifier = Modifier, selected: Boolean = false, onClick: (() -> Unit)? = null) {
    androidx.compose.material3.AssistChip(
        onClick = { onClick?.invoke() },
        label = { Text(text) },
        modifier = modifier,
    )
}

// ===== Controls =====

@Composable
fun Material3Toggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, style: KutaToggleStyle = KutaToggleStyle.SWITCH) {
    if (style == KutaToggleStyle.SWITCH) {
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier)
    } else {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier)
    }
}

@Composable
fun Material3Slider(value: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier, valueRange: ClosedFloatingPointRange<Float> = 0f..1f) {
    Slider(value = value, onValueChange = onValueChange, modifier = modifier, valueRange = valueRange)
}

@Composable
fun Material3ProgressBar(progress: Float?, modifier: Modifier = Modifier) {
    if (progress != null) {
        LinearProgressIndicator(progress = { progress }, modifier = modifier)
    } else {
        LinearProgressIndicator(modifier = modifier)
    }
}

// ===== Feedback =====

@Composable
fun Material3Snackbar(message: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Snackbar(modifier = modifier, action = { if (actionLabel != null) TextButton(onAction ?: {}) { Text(actionLabel) } }) {
        Text(message)
    }
}

@Composable
fun Material3DropdownMenu(items: List<KutaDropdownItem>, onDismissRequest: () -> Unit, modifier: Modifier = Modifier, expanded: Boolean = true) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest, modifier = modifier) {
        items.forEach { item ->
            DropdownMenuItem(text = { Text(item.label) }, onClick = { item.onClick(); onDismissRequest() }, leadingIcon = if (item.icon != null) { { Icon(item.icon, contentDescription = null) } } else null)
        }
    }
}

// ===== Layout =====

@Composable
fun Material3Scaffold(modifier: Modifier = Modifier, topBar: @Composable () -> Unit = {}, bottomBar: @Composable () -> Unit = {}, floatingActionButton: @Composable () -> Unit = {}, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(modifier = modifier, topBar = topBar, bottomBar = bottomBar, floatingActionButton = floatingActionButton, content = content)
}

@Composable
fun Material3TopAppBar(title: String, modifier: Modifier = Modifier, onBack: (() -> Unit)? = null, actions: @Composable () -> Unit = {}) {
    TopAppBar(title = { Text(title) }, navigationIcon = { if (onBack != null) IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }, actions = { actions() }, modifier = modifier)
}

@Composable
fun Material3BottomAppBar(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    BottomAppBar(modifier = modifier) { content() }
}

@Composable
fun Material3FAB(icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, text: String? = null) {
    if (text != null) {
        ExtendedFloatingActionButton(onClick = onClick, modifier = modifier, icon = { Icon(icon, contentDescription = null) }, text = { Text(text) })
    } else {
        FloatingActionButton(onClick = onClick, modifier = modifier) { Icon(icon, contentDescription = null) }
    }
}

// ===== Misc =====

@Composable
fun Material3Skeleton(modifier: Modifier = Modifier, height: Int = 48) {
    Box(modifier = modifier.height(height.dp).fillMaxWidth().padding(8.dp)) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.Center))
    }
}

@Composable
fun Material3Divider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier)
}

@Composable
fun Material3Avatar(modifier: Modifier = Modifier, size: Int = 40, content: @Composable () -> Unit) {
    Box(modifier = modifier.size(size.dp)) { content() }
}
