# 04 — Material Design Language

> The existing Material 3 (Material You) design that Aniyomi ships with. Kept as the 4th design option — no changes needed.

---

## 1. Philosophy

Material 3 is Google's design system. Aniyomi already uses it with custom color overrides. We keep it as-is so:
- Users who prefer the "standard" Android look can use it
- Unmigrated screens (those still using raw `MaterialTheme` components) render correctly regardless of selected design
- We have a stable baseline to fall back to

---

## 2. What Stays As-Is

Everything. We do NOT modify Aniyomi's existing Material 3 implementation:

- `TachiyomiTheme.kt` — the theme entry point (unchanged)
- 18-theme picker (kept, but rebranded as "Material" accent presets)
- Monet (Material You) dynamic color (kept as one of the accent options)
- All 13 forked M3 components in `presentation-core/.../components/material/`
- All `androidx.compose.material3.*` imports across 291 files
- The `Padding` class in `Constants.kt`

See `DOCS/design-system/current.md` (from Phase 1.5) for the full investigation of how Aniyomi uses Material 3.

---

## 3. How Material Fits Into KutaTheme

When the user selects "Material" in Settings → Appearance:

```kotlin
KutaTheme(
    designLanguage = DesignLanguage.MATERIAL,
    mode = prefs.mode,  // light/dark/system
    accent = prefs.accent,
) {
    // KutaTheme detects MATERIAL and delegates to TachiyomiTheme
    TachiyomiTheme {
        content()
    }
}
```

`Kuta*` components, when `LocalDesignLanguage.current == MATERIAL`, delegate to the existing M3 components:

```kotlin
@Composable
fun KutaButton(text: String, onClick: () -> Unit, ...) {
    when (LocalDesignLanguage.current) {
        DesignLanguage.MATERIAL -> Material3Button(text, onClick, ...)
        // ... other designs
    }
}

@Composable
fun Material3Button(text: String, onClick: () -> Unit, ...) {
    // Wraps the existing M3 Button
    androidx.compose.material3.Button(onClick = onClick) {
        Text(text)
    }
}
```

---

## 4. Material Accent Presets

Material's accent presets come from Aniyomi's existing 18 themes' key colors:

```kotlin
object MaterialAccents {
    val Blue = KutaAccent("mat-blue", Color(0xFF2979FF), isCustom = false)        // default
    val Green = KutaAccent("mat-green", Color(0xFF47A84A), isCustom = false)
    val Teal = KutaAccent("mat-teal", Color(0xFF00897B), isCustom = false)
    val Purple = KutaAccent("mat-purple", Color(0xFF7E57C2), isCustom = false)
    val Pink = KutaAccent("mat-pink", Color(0xFFEC407A), isCustom = false)
    val Red = KutaAccent("mat-red", Color(0xFFE53935), isCustom = false)
    val Orange = KutaAccent("mat-orange", Color(0xFFFB8C00), isCustom = false)
    val Yellow = KutaAccent("mat-yellow", Color(0xFFFDD835), isCustom = false)
    // ... pull the rest from AppTheme enum

    val Monet = KutaAccent("mat-monet", Color.Transparent, isCustom = false)  // dynamic color
}
```

The `Monet` accent is special — when selected, the app uses Material You dynamic color (pulls from the device wallpaper on Android 12+). On older devices, falls back to `Blue`.

---

## 5. Migration Notes

- **No migration needed** for Material. It's the default and the fallback.
- Screens that haven't been migrated to use `Kuta*` components will render as Material 3 **regardless of selected design** (because they use raw `MaterialTheme` components).
- This is intentional — it's the fallback behavior the user requested.
- As screens migrate to `Kuta*`, they'll start respecting the selected design.

---

## 6. When To Use Material

Material is the right choice for users who:
- Prefer the "standard" Android look
- Want Material You dynamic color (wallpaper-based)
- Have devices where the other 3 designs' effects (glass, paper, hard shadows) don't render well
- Want maximum accessibility (M3 has the best accessibility documentation and tooling)

---

*Material is the stable baseline. It's always there, always works, and always looks "normal." The other 3 designs are the distinctive options.*
