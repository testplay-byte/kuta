# 00 — Shared Architecture: Multi-Design-Language System

> This file defines the architecture that lets Kuta ship 4 user-switchable design languages (Neon, Notebook, Brutalist, Material) across all screens. Read this first — the per-design files (01–04) assume this architecture.

---

## 1. Overview

Kuta supports **4 design languages**, selectable in Settings → Appearance:

| # | Name | Internal ID | Status |
|---|---|---|---|
| 1 | Neon | `NEON` | New — cyberpunk/synthwave |
| 2 | Notebook | `NOTEBOOK` | New — cozy journal/coffee |
| 3 | Brutalist | `BRUTALIST` | New — neobrutalism |
| 4 | Material | `MATERIAL` | Existing — Aniyomi's Material 3 (kept as-is) |

Each design language has:
- A complete color palette (light + dark modes)
- Typography (font families, weights, sizes)
- Spacing system
- Component implementations (buttons, cards, inputs, etc.)
- Animation specs
- Accessibility considerations

The user picks a design language + light/dark mode + accent color in Settings. The entire app reskins instantly — no restart, no reload.

---

## 2. The Core Problem This Solves

"4 designs on all screens, user-switchable" requires that every screen renders correctly in all 4 designs. The 4 designs are radically different:

- **Material button:** soft shadow, rounded corners, M3 color
- **Neon button:** glow effect, dark bg, neon accent
- **Notebook button:** paper texture, washi tape accent, hand-drawn feel
- **Brutalist button:** 3px black border, hard zero-blur shadow, uppercase bold

If a screen calls `MaterialTheme.button(...)` directly, it's hardcoded to Material. If it calls `NeonButton(...)`, it's hardcoded to Neon. Neither works for "user can switch."

**Solution: an abstraction layer.** We define design-language-agnostic components (`KutaButton`, `KutaCard`, `KutaInput`, etc.) that delegate to the active design's implementation.

---

## 3. Architecture: KutaTheme

### 3.1 Composition Locals (The Switching Mechanism)

Compose's composition local system is how `MaterialTheme` works — it provides colors, typography, and shapes via `CompositionLocalProvider`. We use the same pattern.

```kotlin
// In presentation-core/.../kuta/theme/

/** The active design language. Read this to switch behavior. */
val LocalDesignLanguage = compositionLocalOf<DesignLanguage> {
    error("No DesignLanguage provided — wrap content in KutaTheme")
}

/** The active mode (light/dark). */
val LocalKutaMode = compositionLocalOf<KutaMode> {
    error("No KutaMode provided")
}

/** The active accent color (user-customizable). */
val LocalKutaAccent = compositionLocalOf<KutaAccent> {
    error("No KutaAccent provided")
}

enum class DesignLanguage { NEON, NOTEBOOK, BRUTALIST, MATERIAL }
enum class KutaMode { LIGHT, DARK }
data class KutaAccent(val id: String, val color: Color, val isCustom: Boolean)
```

### 3.2 The KutaTheme Composable

```kotlin
@Composable
fun KutaTheme(
    designLanguage: DesignLanguage,
    mode: KutaMode,
    accent: KutaAccent,
    content: @Composable () -> Unit,
) {
    val colors = when (designLanguage) {
        DesignLanguage.NEON -> if (mode == DARK) NeonDarkColors(accent) else NeonLightColors(accent)
        DesignLanguage.NOTEBOOK -> if (mode == DARK) NotebookDarkColors(accent) else NotebookLightColors(accent)
        DesignLanguage.BRUTALIST -> if (mode == DARK) BrutalistDarkColors(accent) else BrutalistLightColors(accent)
        DesignLanguage.MATERIAL -> /* delegate to existing TachiyomiTheme */
    }
    val typography = when (designLanguage) {
        DesignLanguage.NEON -> NeonTypography
        DesignLanguage.NOTEBOOK -> NotebookTypography
        DesignLanguage.BRUTALIST -> BrutalistTypography
        DesignLanguage.MATERIAL -> MaterialTypography
    }
    // ... shapes, spacing, motion

    CompositionLocalProvider(
        LocalDesignLanguage provides designLanguage,
        LocalKutaMode provides mode,
        LocalKutaAccent provides accent,
        LocalKutaColors provides colors,
        LocalKutaTypography provides typography,
        // ...
    ) {
        if (designLanguage == DesignLanguage.MATERIAL) {
            TachiyomiTheme(content = content)  // existing M3 theme
        } else {
            content()
        }
    }
}
```

### 3.3 Where KutaTheme Is Applied

At the app root (in `MainActivity`), wrap the entire UI:

```kotlin
val prefs = kutaPreferences()  // reads user's saved design/mode/accent
KutaTheme(
    designLanguage = prefs.designLanguage,
    mode = prefs.mode,
    accent = prefs.accent,
) {
    // existing Navigator + screens
}
```

When the user changes design/mode/accent in Settings, the preference updates, the composition local updates, and Compose recomposes — **instant switch, no restart.**

---

## 4. Kuta* Component Library

Every screen uses `Kuta*` components — never raw Material 3 or design-specific components. The `Kuta*` components delegate to the active design's implementation.

### 4.1 Component Inventory (Build These)

| Component | Purpose |
|---|---|
| `KutaButton` | Primary action button |
| `KutaOutlinedButton` | Secondary action button |
| `KutaTextButton` | Tertiary/text-only button |
| `KutaIconButton` | Icon-only button |
| `KutaCard` | Content card (library item, setting group, etc.) |
| `KutaElevatedCard` | Card with shadow/elevation |
| `KutaInput` | Text input field |
| `KutaSearchInput` | Search bar (input + icon) |
| `KutaDialog` | Modal dialog |
| `KutaAlertDialog` | Confirm/cancel dialog |
| `KutaBottomSheet` | Bottom sheet (AdaptiveSheet replacement) |
| `KutaNavigationBar` | Bottom nav bar (tab bar) |
| `KutaNavigationRail` | Side nav rail (tablet/desktop) |
| `KutaNavigationDrawer` | Side drawer |
| `KutaTabRow` | Tab row (browse sub-tabs, etc.) |
| `KutaListItem` | List row (settings, library list) |
| `KutaBadge` | Small status indicator |
| `KutaChip` | Filter chip / tag |
| `KutaToggle` | Switch / checkbox |
| `KutaSlider` | Slider (player scrubber, settings) |
| `KutaProgressBar` | Linear progress |
| `KutaSnackbar` | Snackbar notification |
| `KutaTooltip` | Tooltip |
| `KutaDropdownMenu` | Dropdown menu |
| `KutaScaffold` | Top-level scaffold (app bar + body + FAB) |
| `KutaTopAppBar` | Top app bar |
| `KutaBottomAppBar` | Bottom app bar |
| `KutaFAB` | Floating action button |
| `KutaSkeleton` | Loading placeholder |
| `KutaDivider` | Section divider |
| `KutaAvatar` | Circular avatar/image |

### 4.2 Example: KutaButton

```kotlin
// In presentation-core/.../kuta/components/KutaButton.kt

@Composable
fun KutaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    variant: KutaButtonVariant = KutaButtonVariant.PRIMARY,
) {
    when (LocalDesignLanguage.current) {
        DesignLanguage.NEON -> NeonButton(text, onClick, modifier, enabled, icon, variant)
        DesignLanguage.NOTEBOOK -> NotebookButton(text, onClick, modifier, enabled, icon, variant)
        DesignLanguage.BRUTALIST -> BrutalistButton(text, onClick, modifier, enabled, icon, variant)
        DesignLanguage.MATERIAL -> Material3Button(text, onClick, modifier, enabled, icon, variant)
    }
}

enum class KutaButtonVariant { PRIMARY, SECONDARY, DESTRUCTIVE, GHOST }
```

Each design's `NeonButton`, `NotebookButton`, etc. lives in its own package:
```
presentation-core/.../kuta/
├── theme/
│   ├── KutaTheme.kt
│   ├── KutaColors.kt
│   ├── KutaTypography.kt
│   └── Locals.kt
├── components/
│   ├── KutaButton.kt          # delegates
│   ├── KutaCard.kt            # delegates
│   └── ...
├── neon/
│   ├── NeonButton.kt
│   ├── NeonCard.kt
│   ├── NeonColors.kt
│   ├── NeonTypography.kt
│   └── ...
├── notebook/
│   ├── NotebookButton.kt
│   ├── NotebookCard.kt
│   └── ...
├── brutalist/
│   ├── BrutalistButton.kt
│   ├── BrutalistCard.kt
│   └── ...
└── material/
    └── Material3Button.kt     # wraps existing M3 components
```

### 4.3 Fallback Strategy (Per User's Request)

Screens that haven't been migrated still use raw `MaterialTheme` components (`androidx.compose.material3.Button`, etc.). These screens look Material 3 **regardless of selected design** — which is fine, because Material 3 is one of the 4 options anyway.

**Migration is incremental:**
1. A screen uses raw M3 → looks Material 3 always
2. We migrate the screen to use `KutaButton`, `KutaCard`, etc. → now respects selected design
3. CI stays green throughout — no big-bang rewrite

**Rule:** New screens use `Kuta*` components from day one. Existing screens migrate one at a time. No screen is ever "broken" — it just may not respect the selected design until migrated.

---

## 5. Shared Tokens

Some tokens are shared across all 4 designs (so the app feels cohesive even when switching):

### 5.1 Spacing Scale

```kotlin
object KutaSpacing {
    val xs = 4.dp    // tight: icon-to-text gap
    val sm = 8.dp    // default: list item padding
    val md = 12.dp   // comfortable: card inner padding
    val lg = 16.dp   // section: card outer padding
    val xl = 24.dp   // large: section gap
    val xxl = 32.dp  // huge: screen edge padding
    val xxxl = 48.dp // hero: top of screen
}
```

All 4 designs use this scale. Differences are in HOW the spacing is applied (e.g., Notebook adds paper-margin padding), not in the values themselves.

### 5.2 Motion Durations

```kotlin
object KutaMotion {
    val instant = 0.ms       // no animation (state changes)
    val fast = 100.ms        // hover, press
    val normal = 200.ms      // card lift, toggle
    val slow = 300.ms        // sheet, dialog
    val slower = 500.ms      // screen transition
}
```

### 5.3 Elevation (Per Design)

Each design defines its own elevation system (since elevation is design-specific):
- **Material:** standard M3 elevation (0-5 levels, soft shadows)
- **Neon:** glow-based "elevation" (no shadow, just glow intensity)
- **Notebook:** paper-shadow-based (warm soft shadows)
- **Brutalist:** hard-offset shadows (no blur, fixed pixel offsets)

### 5.4 Corner Radius (Per Design)

- **Material:** 4-16dp (M3 standard)
- **Neon:** 8-14dp (slightly less rounded, modern)
- **Notebook:** 4-10dp (less rounded, paper-like)
- **Brutalist:** 8-10dp (consistent, bold)

---

## 6. Accent Color System

Per user's decision: curated presets per design + 1 custom color option.

### 6.1 Preset Palettes

Each design has its own curated accent presets (colors that look good with that design):

```kotlin
object NeonAccents {
    val Cyan = KutaAccent("neon-cyan", Color(0xFF5FC9FF), isCustom = false)
    val Magenta = KutaAccent("neon-magenta", Color(0xFFFF5F7E), isCustom = false)
    val Lime = KutaAccent("neon-lime", Color(0xFFBCFF5F), isCustom = false)
    val Purple = KutaAccent("neon-purple", Color(0xFFA78BFA), isCustom = false)
    val Orange = KutaAccent("neon-orange", Color(0xFFFFB45F), isCustom = false)
}

object NotebookAccents {
    val Coffee = KutaAccent("nb-coffee", Color(0xFFB8653F), isCustom = false)
    val Sage = KutaAccent("nb-sage", Color(0xFF6B8E5B), isCustom = false)
    val Terracotta = KutaAccent("nb-terracotta", Color(0xFFC99545), isCustom = false)
    val Navy = KutaAccent("nb-navy", Color(0xFF5A7D96), isCustom = false)
    val Plum = KutaAccent("nb-plum", Color(0xFF966B94), isCustom = false)
}

object BrutalistAccents {
    val Blue = KutaAccent("br-blue", Color(0xFF2563EB), isCustom = false)
    val Pink = KutaAccent("br-pink", Color(0xFFEC4899), isCustom = false)
    val Green = KutaAccent("br-green", Color(0xFF22C55E), isCustom = false)
    val Yellow = KutaAccent("br-yellow", Color(0xFFF59E0B), isCustom = false)
    val Orange = KutaAccent("br-orange", Color(0xFFF97316), isCustom = false)
    val Purple = KutaAccent("br-purple", Color(0xFF8B5CF6), isCustom = false)
    val Red = KutaAccent("br-red", Color(0xFFEF4444), isCustom = false)
}

object MaterialAccents {
    // Use Aniyomi's existing 18 themes' key colors as presets
    val Blue = KutaAccent("mat-blue", Color(0xFF2979FF), isCustom = false)
    val Green = KutaAccent("mat-green", Color(0xFF47A84A), isCustom = false)
    // ... pull from existing AppTheme enum
}
```

### 6.2 Custom Accent

User can pick ANY color via a color picker. Stored as `KutaAccent("custom", userColor, isCustom = true)`.

### 6.3 Settings UI

```
Settings → Appearance
├── Design Language:  [Neon] [Notebook] [Brutalist] [Material]
├── Mode:             [Light] [Dark] [System]
└── Accent:           [preset1] [preset2] ... [Custom 🎨]
```

The accent picker shows the curated presets for the SELECTED design language. Switching design language resets to that design's default accent (with a confirmation dialog if the user had a custom color).

---

## 7. Persistence

User's choices persist via a new `KutaPreferences` (alongside Aniyomi's existing `UiPreferences`):

```kotlin
interface KutaPreferences {
    val designLanguage: StateFlow<DesignLanguage>
    val mode: StateFlow<KutaMode>  // LIGHT, DARK, SYSTEM
    val accent: StateFlow<KutaAccent>

    fun setDesignLanguage(design: DesignLanguage)
    fun setMode(mode: KutaMode)
    fun setAccent(accent: KutaAccent)
}
```

Stored in SharedPreferences (or DataStore) — survives app restarts.

**System mode:** If user picks `SYSTEM`, the app follows the device's dark/light setting. The `KutaMode` resolves to `LIGHT` or `DARK` at composition time based on `Configuration.uiMode`.

---

## 8. Migration Strategy

### 8.1 Phase 2 (Next): Build The Foundation

1. Create `presentation-core/.../kuta/` package structure
2. Implement `KutaTheme`, composition locals, `KutaPreferences`
3. Implement `Kuta*` component interfaces (delegating to Material 3 by default — so the app still works)
4. Wrap `MainActivity` in `KutaTheme`
5. Add Settings → Appearance screen with design/mode/accent pickers
6. CI green — app behaves exactly as before (everything delegates to Material 3)

### 8.2 Phase 2.5: Implement The 3 New Designs

In parallel (per user's decision), implement:
- `neon/` — NeonColors, NeonTypography, NeonButton, NeonCard, etc.
- `notebook/` — NotebookColors, NotebookTypography, NotebookButton, etc.
- `brutalist/` — BrutalistColors, BrutalistTypography, BrutalistButton, etc.

Each design's components are wired into the `Kuta*` delegation. Once a design is implemented, switching to it in Settings reskins the app (for migrated screens).

### 8.3 Phase 3+: Screen Migration

One screen at a time, replace raw `MaterialTheme.*` / `androidx.compose.material3.*` calls with `Kuta*` calls. Each migration is a separate PR. CI stays green.

**Priority order** (suggested):
1. Placeholder home screen (already uses some Kuta* — finish it)
2. Anime library screen
3. Browse screen
4. Anime detail screen
5. Player UI
6. Settings screens
7. History, Updates, Categories, Stats, Downloads
8. Onboarding (last — users see it once)
9. Manga screens (when re-enabled in Phase 6)

### 8.4 Per-Design Files

For the implementation details of each design (colors, typography, component specs, effects), see:
- `01-neon.md`
- `02-notebook.md`
- `03-brutalist.md`
- `04-material.md`

---

## 9. Accessibility

All 4 designs must meet WCAG AA contrast ratios (4.5:1 for normal text, 3:1 for large text).

- **Material:** Already meets this (M3 default).
- **Neon:** Neon accents on pure black = high contrast. Watch neon-on-neon combos.
- **Notebook:** Warm earth tones — verify coffee brown on cream meets 4.5:1. May need to darken browns.
- **Brutalist:** Black on bright colors = high contrast. Watch colored-shadow-on-colored-bg combos.

**Reduced motion:** All 4 designs respect `Settings.Global.ANIMATOR_DURATION_SCALE` and `AccessibilityManager.isHighTextContrastEnabled`. Animations disable or simplify.

**Color-blind:** Don't rely on color alone — use icons + text labels for status indicators.

---

## 10. Implementation Notes For The Coder

- **Start with `00-shared-architecture.md` (this file).** The `Kuta*` component interfaces and `KutaTheme` must exist before any design implementation.
- **Material 3 delegation is the default.** Until a design is implemented, `Kuta*` components delegate to M3 — the app works exactly as before.
- **Each design is independent.** Implementing Neon doesn't require Notebook or Brutalist to exist.
- **Test switching.** After Phase 2.5, switch between all 4 designs in Settings — verify no crashes, no visual glitches on migrated screens.
- **Don't touch unmigrated screens.** They use raw M3 and look Material 3 — that's the fallback, by design.
- **`// FORK:` markers** on every upstream file modified (per existing convention).

---

*This file is the contract. The per-design files (01–04) define WHAT each design looks like; this file defines HOW they coexist.*
