package tachiyomi.presentation.core.kuta.preferences

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum
import tachiyomi.presentation.core.kuta.theme.BrutalistAccents
import tachiyomi.presentation.core.kuta.theme.DesignLanguage
import tachiyomi.presentation.core.kuta.theme.KutaAccent
import tachiyomi.presentation.core.kuta.theme.KutaMode
import tachiyomi.presentation.core.kuta.theme.MaterialAccents
import tachiyomi.presentation.core.kuta.theme.NeonAccents
import tachiyomi.presentation.core.kuta.theme.NotebookAccents

/**
 * FORK: Phase 2 — KutaPreferences: stores design language, mode, and accent.
 * Per DOCS/design-system/00-shared-architecture.md §7.
 *
 * Follows Aniyomi's existing preference pattern (concrete class, [PreferenceStore]-backed).
 * Registered as an Injekt singleton in PreferenceModule.
 */
class KutaPreferences(
    private val preferenceStore: PreferenceStore,
) {
    fun designLanguage(): Preference<DesignLanguage> =
        preferenceStore.getEnum("kuta_design_language", DesignLanguage.MATERIAL)

    fun mode(): Preference<KutaMode> =
        preferenceStore.getEnum("kuta_mode", KutaMode.SYSTEM)

    fun accentId(): Preference<String> =
        preferenceStore.getString("kuta_accent_id", MaterialAccents.default.id)

    fun accentArgb(): Preference<Int> =
        preferenceStore.getInt("kuta_accent_argb", MaterialAccents.default.color.value.toInt())

    fun accentIsCustom(): Preference<Boolean> =
        preferenceStore.getBoolean("kuta_accent_is_custom", false)

    /** Convenience: get the current [KutaAccent] (reads the 3 accent prefs). */
    fun accent(): KutaAccent {
        val isCustom = accentIsCustom().get()
        val argb = accentArgb().get()
        return if (isCustom) {
            KutaAccent("custom", Color(argb), isCustom = true)
        } else {
            val id = accentId().get()
            val allPresets = NeonAccents.all + NotebookAccents.all + BrutalistAccents.all + MaterialAccents.all
            allPresets.find { it.id == id } ?: MaterialAccents.default
        }
    }

    /** Flow that emits the current [KutaAccent] whenever any accent pref changes. */
    fun accentChanges(): Flow<KutaAccent> = combine(
        accentId().changes(),
        accentArgb().changes(),
        accentIsCustom().changes(),
    ) { id, argb, isCustom ->
        if (isCustom) {
            KutaAccent("custom", Color(argb), isCustom = true)
        } else {
            val allPresets = NeonAccents.all + NotebookAccents.all + BrutalistAccents.all + MaterialAccents.all
            allPresets.find { it.id == id } ?: MaterialAccents.default
        }
    }

    /** Set the accent (writes the 3 accent prefs). */
    fun setAccent(accent: KutaAccent) {
        accentId().set(accent.id)
        accentArgb().set(accent.color.value.toInt())
        accentIsCustom().set(accent.isCustom)
    }
}
