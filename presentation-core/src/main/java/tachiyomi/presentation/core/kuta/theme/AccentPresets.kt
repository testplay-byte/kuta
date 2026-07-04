package tachiyomi.presentation.core.kuta.theme

import androidx.compose.ui.graphics.Color

/**
 * FORK: Phase 2 — Curated accent color presets per design language.
 * Per DOCS/design-system/00-shared-architecture.md §6.1.
 */
object NeonAccents {
    val Cyan = KutaAccent("neon-cyan", Color(0xFF5FC9FF), isCustom = false)
    val Magenta = KutaAccent("neon-magenta", Color(0xFFFF5F7E), isCustom = false)
    val Lime = KutaAccent("neon-lime", Color(0xFFBCFF5F), isCustom = false)
    val Purple = KutaAccent("neon-purple", Color(0xFFA78BFA), isCustom = false)
    val Orange = KutaAccent("neon-orange", Color(0xFFFFB45F), isCustom = false)

    val all = listOf(Cyan, Magenta, Lime, Purple, Orange)
    val default = Cyan
}

object NotebookAccents {
    val Coffee = KutaAccent("nb-coffee", Color(0xFFB8653F), isCustom = false)
    val Sage = KutaAccent("nb-sage", Color(0xFF6B8E5B), isCustom = false)
    val Terracotta = KutaAccent("nb-terracotta", Color(0xFFC99545), isCustom = false)
    val Navy = KutaAccent("nb-navy", Color(0xFF5A7D96), isCustom = false)
    val Plum = KutaAccent("nb-plum", Color(0xFF966B94), isCustom = false)

    val all = listOf(Coffee, Sage, Terracotta, Navy, Plum)
    val default = Coffee
}

object BrutalistAccents {
    val Blue = KutaAccent("br-blue", Color(0xFF2563EB), isCustom = false)
    val Pink = KutaAccent("br-pink", Color(0xFFEC4899), isCustom = false)
    val Green = KutaAccent("br-green", Color(0xFF22C55E), isCustom = false)
    val Yellow = KutaAccent("br-yellow", Color(0xFFF59E0B), isCustom = false)
    val Orange = KutaAccent("br-orange", Color(0xFFF97316), isCustom = false)
    val Purple = KutaAccent("br-purple", Color(0xFF8B5CF6), isCustom = false)
    val Red = KutaAccent("br-red", Color(0xFFEF4444), isCustom = false)

    val all = listOf(Blue, Pink, Green, Yellow, Orange, Purple, Red)
    val default = Blue
}

object MaterialAccents {
    val Blue = KutaAccent("mat-blue", Color(0xFF2979FF), isCustom = false)
    val Green = KutaAccent("mat-green", Color(0xFF47A84A), isCustom = false)
    val Teal = KutaAccent("mat-teal", Color(0xFF00897B), isCustom = false)
    val Purple = KutaAccent("mat-purple", Color(0xFF7E57C2), isCustom = false)
    val Pink = KutaAccent("mat-pink", Color(0xFFEC407A), isCustom = false)
    val Red = KutaAccent("mat-red", Color(0xFFE53935), isCustom = false)

    val all = listOf(Blue, Green, Teal, Purple, Pink, Red)
    val default = Blue
}

/** Get the preset list for a given design language. */
fun accentPresetsFor(design: DesignLanguage): List<KutaAccent> = when (design) {
    DesignLanguage.NEON -> NeonAccents.all
    DesignLanguage.NOTEBOOK -> NotebookAccents.all
    DesignLanguage.BRUTALIST -> BrutalistAccents.all
    DesignLanguage.MATERIAL -> MaterialAccents.all
}

/** Get the default accent for a given design language. */
fun defaultAccentFor(design: DesignLanguage): KutaAccent = when (design) {
    DesignLanguage.NEON -> NeonAccents.default
    DesignLanguage.NOTEBOOK -> NotebookAccents.default
    DesignLanguage.BRUTALIST -> BrutalistAccents.default
    DesignLanguage.MATERIAL -> MaterialAccents.default
}
