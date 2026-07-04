package tachiyomi.presentation.core.kuta.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.components.AppBar
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.kuta.components.KutaButton
import tachiyomi.presentation.core.kuta.components.KutaButtonVariant
import tachiyomi.presentation.core.kuta.components.KutaCard
import tachiyomi.presentation.core.kuta.components.KutaListItem
import tachiyomi.presentation.core.kuta.components.KutaToggle
import tachiyomi.presentation.core.kuta.preferences.KutaPreferences
import tachiyomi.presentation.core.kuta.theme.BrutalistAccents
import tachiyomi.presentation.core.kuta.theme.DesignLanguage
import tachiyomi.presentation.core.kuta.theme.KutaAccent
import tachiyomi.presentation.core.kuta.theme.KutaMode
import tachiyomi.presentation.core.kuta.theme.MaterialAccents
import tachiyomi.presentation.core.kuta.theme.NeonAccents
import tachiyomi.presentation.core.kuta.theme.NotebookAccents
import tachiyomi.presentation.core.kuta.theme.accentPresetsFor
import tachiyomi.presentation.core.kuta.theme.defaultAccentFor
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * FORK: Phase 2 — Settings → Appearance screen for Kuta design system.
 * Per DOCS/design-system/00-shared-architecture.md §6.3.
 *
 * Lets the user pick: design language (Neon/Notebook/Brutalist/Material),
 * mode (Light/Dark/System), and accent color (curated presets + custom).
 *
 * Uses Kuta* components — so the screen itself reskins when the user switches
 * design language (live preview).
 */
object KutaAppearanceScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val prefs = remember { Injekt.get<KutaPreferences>() }
        val designLanguage by prefs.designLanguage().collectAsState()
        val mode by prefs.mode().collectAsState()
        val accent by prefs.accentChanges().collectAsState(initial = prefs.accent())

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = "Appearance",
                    navigateUp = { navigator.pop() },
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // === Design Language ===
                Text("Design Language", style = MaterialTheme.typography.titleMedium)
                DesignLanguageSelector(designLanguage) { newDesign ->
                    if (newDesign != designLanguage) {
                        // Switching design resets accent to that design's default
                        prefs.designLanguage().set(newDesign)
                        prefs.setAccent(defaultAccentFor(newDesign))
                    }
                }

                // === Mode ===
                Text("Mode", style = MaterialTheme.typography.titleMedium)
                ModeSelector(mode) { prefs.mode().set(it) }

                // === Accent Color ===
                Text("Accent Color", style = MaterialTheme.typography.titleMedium)
                AccentSelector(
                    designLanguage = designLanguage,
                    selectedAccent = accent,
                    onAccentSelected = { prefs.setAccent(it) },
                )
            }
        }
    }

    @Composable
    private fun DesignLanguageSelector(
        selected: DesignLanguage,
        onSelect: (DesignLanguage) -> Unit,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DesignLanguage.entries.forEach { design ->
                val isSelected = design == selected
                KutaCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(design) }
                        .then(
                            if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                            else Modifier,
                        ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(design.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleSmall)
                            Text(
                                when (design) {
                                    DesignLanguage.NEON -> "Cyberpunk glass + glow"
                                    DesignLanguage.NOTEBOOK -> "Cozy journal + paper"
                                    DesignLanguage.BRUTALIST -> "Bold borders + hard shadows"
                                    DesignLanguage.MATERIAL -> "Standard Material 3"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        KutaToggle(checked = isSelected, onCheckedChange = { onSelect(design) })
                    }
                }
            }
        }
    }

    @Composable
    private fun ModeSelector(selected: KutaMode, onSelect: (KutaMode) -> Unit) {
        Column {
            KutaListItem(title = "Light", trailing = { KutaToggle(checked = selected == KutaMode.LIGHT, onCheckedChange = { onSelect(KutaMode.LIGHT) }) })
            KutaListItem(title = "Dark", trailing = { KutaToggle(checked = selected == KutaMode.DARK, onCheckedChange = { onSelect(KutaMode.DARK) }) })
            KutaListItem(title = "System", trailing = { KutaToggle(checked = selected == KutaMode.SYSTEM, onCheckedChange = { onSelect(KutaMode.SYSTEM) }) })
        }
    }

    @Composable
    private fun AccentSelector(
        designLanguage: DesignLanguage,
        selectedAccent: KutaAccent,
        onAccentSelected: (KutaAccent) -> Unit,
    ) {
        val presets = accentPresetsFor(designLanguage)
        var showCustomPicker by remember { mutableStateOf(false) }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(presets.size) { index ->
                val preset = presets[index]
                AccentSwatch(
                    color = preset.color,
                    isSelected = preset.id == selectedAccent.id,
                    onClick = { onAccentSelected(preset) },
                )
            }
            item {
                AccentSwatch(
                    color = selectedAccent.takeIf { it.isCustom }?.color ?: Color.Gray,
                    isSelected = selectedAccent.isCustom,
                    onClick = { showCustomPicker = true },
                    label = "Custom",
                )
            }
        }

        if (showCustomPicker) {
            CustomColorPickerDialog(
                initialColor = selectedAccent.color,
                onConfirm = { color ->
                    onAccentSelected(KutaAccent("custom", color, isCustom = true))
                    showCustomPicker = false
                },
                onDismiss = { showCustomPicker = false },
            )
        }
    }

    @Composable
    private fun AccentSwatch(color: Color, isSelected: Boolean, onClick: () -> Unit, label: String? = null) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color)
                    .clickable(onClick = onClick)
                    .then(if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier),
            )
            if (label != null) {
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }

    @Composable
    private fun CustomColorPickerDialog(
        initialColor: Color,
        onConfirm: (Color) -> Unit,
        onDismiss: () -> Unit,
    ) {
        var red by remember { mutableStateOf((initialColor.red * 255).toInt()) }
        var green by remember { mutableStateOf((initialColor.green * 255).toInt()) }
        var blue by remember { mutableStateOf((initialColor.blue * 255).toInt()) }

        tachiyomi.presentation.core.kuta.components.KutaAlertDialog(
            title = "Custom Color",
            message = "RGB: $red, $green, $blue",
            onConfirm = { onConfirm(Color(red, green, blue)) },
            onDismiss = onDismiss,
            confirmText = "Set",
        )
        // Note: for Phase 2A, this is a simple confirm dialog.
        // A full color slider picker can be added in a later iteration.
    }
}
