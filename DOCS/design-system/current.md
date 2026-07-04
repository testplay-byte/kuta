# Current Material 3 Design System (Kuta / Aniyomi fork)

> **Task 5-d baseline.** This document captures the Material 3 design system as it
> exists today in the Kuta repo (`/home/z/kuta`, commit `cbe42a1e`, post-Phase-1).
> It is the reference baseline for the upcoming design-language replacement.
>
> All paths are relative to `/home/z/kuta`. Counts of M3 component usage were
> gathered with `rg` across `app/`, `presentation-core/`, and `presentation-widget/`.

---

## 1. Theme definition

### 1.1 Compose entry point: `TachiyomiTheme`

**File:** `app/src/main/java/eu/kanade/presentation/theme/TachiyomiTheme.kt`

The single Compose theme wrapper. Public surface:

```kotlin
@Composable
fun TachiyomiTheme(
    appTheme: AppTheme? = null,
    amoled: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val uiPreferences = Injekt.get<UiPreferences>()
    BaseTachiyomiTheme(
        appTheme = appTheme ?: uiPreferences.appTheme().get(),
        isAmoled = amoled ?: uiPreferences.themeDarkAmoled().get(),
        content = content,
    )
}

@Composable
private fun BaseTachiyomiTheme(appTheme: AppTheme, isAmoled: Boolean, content) {
    MaterialTheme(
        colorScheme = getThemeColorScheme(appTheme, isAmoled),
        content = content,
    )
}
```

Notable facts:

- It calls `androidx.compose.material3.MaterialTheme` directly.
- **No `typography = …` argument** — the app uses the **default M3 `Typography`**.
- **No `shapes = …` argument** — the app uses the **default M3 `Shapes`**.
- The selected `AppTheme` and AMOLED flag are pulled from `UiPreferences` (Injekt
  singleton) unless explicitly overridden (used by `AppThemePreferenceWidget`
  previews).
- Light/dark mode is decided by `isSystemInDarkTheme()` inside
  `getThemeColorScheme` — there is no app-level override of `isSystemInDarkTheme`;
  the user's "theme mode" preference is applied at the Android platform level via
  `AppCompatDelegate.setDefaultNightMode(...)` (see §7).
- A `TachiyomiPreviewTheme` companion exists for `@Preview` composables.

A separate `playerRippleConfiguration` val (white-on-dark, black-on-light, all
ripple alphas = 0.1) is exported for the MPV player overlay.

### 1.2 Where the theme is applied at the app root

**File:** `app/src/main/java/eu/kanade/tachiyomi/util/view/ViewExtensions.kt`

Theme application does **not** happen in `MainActivity.kt` directly. It happens
inside a small extension function that every Compose-hosting activity calls
instead of `setContent`:

```kotlin
inline fun ComponentActivity.setComposeContent(
    parent: CompositionContext? = null,
    crossinline content: @Composable () -> Unit,
) {
    setContent(parent) {
        TachiyomiTheme {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.bodySmall,
                LocalContentColor provides MaterialTheme.colorScheme.onBackground,
            ) {
                content()
            }
        }
    }
}

fun ComposeView.setComposeContent(content: @Composable () -> Unit) { /* same wrapper */ }
```

`MainActivity.onCreate` calls `setComposeContent { … }` (line 168), which:

1. Wraps everything in `TachiyomiTheme` (→ M3 `MaterialTheme`).
2. Overrides `LocalTextStyle` to `bodySmall` and `LocalContentColor` to
   `colorScheme.onBackground` by default — meaning every `Text()` that doesn't
   explicitly set a style renders as **M3 bodySmall in onBackground**.

`setComposeContent` is also called by:

- `PlayerActivity` (line 258–259 wraps `PlayerControls` in `TachiyomiTheme`
  directly — note: PlayerActivity uses `TachiyomiTheme { … }` on a `ComposeView`,
  not `setComposeContent`, but the wrapper effect is the same).
- `ReaderActivity` (page-number + dialog-root `ComposeView`s).
- `WebViewActivity`, `BaseOAuthLoginActivity`, `CrashActivity`.

So **every Compose tree in the app is rooted in `TachiyomiTheme`**. There is no
`App.kt`-level theme setup; `App.kt` only configures Injekt, notifications, Coil,
etc. (confirmed by reading `app/src/main/java/eu/kanade/tachiyomi/App.kt`).

### 1.3 View-system theme (XML)

For activities/windows still using AppCompat (splash, system bars, WebView
hierarchy, popup menus), there is a parallel XML theme system:

**Files:**

- `app/src/main/res/values/themes.xml` (679 lines)
- `app/src/main/res/values-night/themes.xml`
- `app/src/main/res/values-v27/themes.xml`
- `app/src/main/res/values-v31/themes.xml`
- `app/src/main/res/values-night-v31/themes.xml`
- `app/src/main/res/values/styles.xml`

Base theme:

```xml
<style name="Base.Theme.Tachiyomi" parent="Theme.Material3.DayNight.NoActionBar">
    <item name="android:forceDarkAllowed" tools:targetApi="Q">false</item>
    <item name="colorPrimary">@color/tachiyomi_primary</item>
    <!-- … full M3 color attribute set … -->
</style>

<style name="Theme.Tachiyomi" parent="Base.Theme.Tachiyomi" />
```

There are 18 `Theme.Tachiyomi.<Name>` variants (one per `AppTheme`), an empty
`ThemeOverlay.Tachiyomi.Amoled` overlay marker, and a
`Theme.Tachiyomi.SplashScreen` based on `Theme.SplashScreen`.

`ThemingDelegate.getThemeResIds(appTheme, isAmoled)` (file
`app/src/main/java/eu/kanade/tachiyomi/ui/base/delegate/ThemingDelegate.kt`)
returns the list of style res IDs to apply; `ThemingDelegateImpl.applyAppTheme`
calls `activity.setTheme(...)` for each. This is invoked by activities that
need the XML theme (e.g., before `super.onCreate` for splash).

---

## 2. Color palette

### 2.1 Architecture

**Files:** `app/src/main/java/eu/kanade/presentation/theme/colorscheme/`

```
BaseColorScheme.kt          ← abstract base
TachiyomiColorScheme.kt     ← DEFAULT
MonetColorScheme.kt         ← Material You / dynamic color
CloudflareColorScheme.kt
CottoncandyColorScheme.kt
DoomColorScheme.kt
GreenAppleColorScheme.kt
LavenderColorScheme.kt
MatrixColorScheme.kt
MidnightDuskColorScheme.kt
MochaColorScheme.kt
MonochromeColorScheme.kt
NordColorScheme.kt
SapphireColorScheme.kt
StrawberryColorScheme.kt
TakoColorScheme.kt
TealTurqoiseColorScheme.kt
TidalWaveColorScheme.kt
YinYangColorScheme.kt
YotsubaColorScheme.kt
```

**Base class** (`BaseColorScheme.kt`):

```kotlin
internal abstract class BaseColorScheme {
    abstract val darkScheme: ColorScheme
    abstract val lightScheme: ColorScheme

    // Cannot be pure black as there's content scrolling behind it
    private val surfaceContainer        = Color(0xFF0C0C0C)
    private val surfaceContainerHigh    = Color(0xFF131313)
    private val surfaceContainerHighest = Color(0xFF1B1B1B)

    fun getColorScheme(isDark: Boolean, isAmoled: Boolean): ColorScheme {
        if (!isDark)   return lightScheme
        if (!isAmoled) return darkScheme
        return darkScheme.copy(
            background = Color.Black, onBackground = Color.White,
            surface = Color.Black,    onSurface = Color.White,
            surfaceVariant = surfaceContainer,
            surfaceContainerLowest = surfaceContainer,
            surfaceContainerLow = surfaceContainer,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
        )
    }
}
```

So AMOLED mode = the dark scheme with the surface tonal palette flattened to
near-black greys. Every theme inherits this behaviour.

### 2.2 The default theme (Tachiyomi)

`TachiyomiColorScheme.kt` — colors generated via Material Theme Builder.
Key colors (from file header):

```
Primary   #2979FF
Secondary #2979FF
Tertiary  #47A84A
Neutral   #919094
```

Light scheme (selected swatches):

| Token                   | Light        | Dark         |
| ----------------------- | ------------ | ------------ |
| primary                 | `#0058CA`    | `#B0C6FF`    |
| onPrimary               | `#FFFFFF`    | `#002D6E`    |
| primaryContainer        | `#D9E2FF`    | `#00429B`    |
| secondary               | `#0058CA`    | `#B0C6FF`    |
| secondaryContainer      | `#D9E2FF`    | `#00429B`    |
| tertiary                | `#006E1B`    | `#7ADC77`    |
| tertiaryContainer       | `#95F990`    | `#005312`    |
| background / surface    | `#FEFBFF`    | `#1B1B1F`    |
| onBackground / onSurface| `#1B1B1F`    | `#E3E2E6`    |
| surfaceVariant          | `#F3EDF7`    | `#211F26`    |
| onSurfaceVariant        | `#44464F`    | `#C5C6D0`    |
| error                   | `#BA1A1A`    | `#FFB4AB`    |
| errorContainer          | `#FFDAD6`    | `#93000A`    |
| outline                 | `#757780`    | `#8F9099`    |
| outlineVariant          | `#C5C6D0`    | `#44464F`    |
| surfaceContainer        | `#F3EDF7`    | `#211F26`    |
| surfaceContainerHigh    | `#FCF7FF`    | `#292730`    |
| surfaceContainerHighest | `#FCF7FF`    | `#302E38`    |

Inline code comments document semantic overloads, e.g.:
`secondary = Color(0xFFB0C6FF) // Unread badge`,
`tertiary = Color(0xFF7ADC77) // Downloaded badge`,
`secondaryContainer // Navigation bar selector pill & progress`.

### 2.3 Custom themes

17 hand-tuned themes (beyond DEFAULT and MONET). Examples of their key colors
(taken from each file's header comment):

| Theme            | Primary                                  | File                                   |
| ---------------- | ---------------------------------------- | -------------------------------------- |
| Cloudflare       | `#F38020` (orange)                       | `CloudflareColorScheme.kt`             |
| Cottoncandy      | pink `#FFB1C1` / blue `#64D3FF` accent   | `CottoncandyColorScheme.kt`            |
| Doom             | (red/dark — see file)                    | `DoomColorScheme.kt`                   |
| Green Apple      | green                                    | `GreenAppleColorScheme.kt`             |
| Lavender         | purple                                   | `LavenderColorScheme.kt`               |
| Matrix           | (green/black terminal aesthetic)         | `MatrixColorScheme.kt`                 |
| Midnight Dusk    | (dark blue/purple)                       | `MidnightDuskColorScheme.kt`           |
| Mocha            | `#EBC248` (gold on brown)                | `MochaColorScheme.kt`                  |
| Monochrome       | (greyscale)                              | `MonochromeColorScheme.kt`             |
| Nord             | `#5E81AC` (Nord palette)                 | `NordColorScheme.kt`                   |
| Sapphire         | (deep blue)                              | `SapphireColorScheme.kt`               |
| Strawberry       | (pink/red)                               | `StrawberryColorScheme.kt`             |
| Tako             | (teal/purple)                            | `TakoColorScheme.kt`                   |
| Teal Turquoise   | (teal)                                   | `TealTurqoiseColorScheme.kt`           |
| Tidal Wave       | (blue)                                   | `TidalWaveColorScheme.kt`              |
| Yin Yang         | (black/white)                            | `YinYangColorScheme.kt`                |
| Yotsuba          | (orange `#F38020`-family, see file)      | `YotsubaColorScheme.kt`                |

Most themes were generated via Material Theme Builder, so each defines a full
~30-token M3 `ColorScheme` (light + dark).

### 2.4 Material You / Monet (dynamic color)

`MonetColorScheme.kt` is special-cased in `TachiyomiTheme.getThemeColorScheme`:

```kotlin
val colorScheme = if (appTheme == AppTheme.MONET) {
    MonetColorScheme(LocalContext.current)
} else {
    colorSchemes.getOrDefault(appTheme, TachiyomiColorScheme)
}
```

Internally it branches on Android version:

- **S+ (`Build.VERSION_CODES.S`)**: `MonetSystemColorScheme` calls
  `androidx.compose.material3.dynamicLightColorScheme(context)` and
  `dynamicDarkColorScheme(context)` — the platform Material You APIs.
- **O_MR1+ pre-S**: `MonetCompatColorScheme` reads the wallpaper's primary
  color via `WallpaperManager.getWallpaperColors(FLAG_SYSTEM).primaryColor`,
  then runs it through `com.google.android.material.color.utilities`:
  `Hct.fromInt(seed)` → `SchemeContent` → `MaterialDynamicColors()` to build
  a full `ColorScheme` (including `surfaceBright`/`surfaceDim`/`surfaceContainer*`
  M3 Expressive tokens).
- **Below O_MR1**: falls back to `TachiyomiColorScheme`.

Also exposes `extractSeedColorFromImage(bitmap)` using `QuantizerCelebi` +
`Score.score`.

### 2.5 XML color mirror

Every Kotlin theme has a parallel XML color file under
`presentation-core/src/main/res/values/` and `…/values-night/` (17 + 17 files):

```
colors.xml                       ← shared (splash, AMOLED, cover_placeholder, divider)
colors_tachiyomi.xml             ← DEFAULT theme
colors_greenapple.xml
colors_midnightdusk.xml
colors_nord.xml
colors_strawberry.xml
colors_tako.xml
colors_tealturqoise.xml
colors_tidalwave.xml
colors_yinyang.xml
colors_yotsuba.xml
colors_monochrome.xml
color_cloudflare.xml             ← note singular "color_"
color_doom.xml
color_lavender.xml
color_matrix.xml
color_sapphire.xml
```

Plus `app/src/main/res/values/colors_mocha.xml` and
`colors_cottoncandy.xml` (and their `values-night/` counterparts) — these two
live in `:app` rather than `:presentation-core`.

`colors.xml` (shared) also defines the AMOLED overrides used by the Compose
side as a reference (`amoled_background = #000000`, `amoled_surfaceContainer
= #0C0C0C`, etc.) and `accent_blue = #54759E` used by the splash screen.

The XML colors back the `Theme.Tachiyomi.<Name>` styles (§1.3) and the
`AppThemePreferenceWidget` mini-preview (§7).

---

## 3. Typography

### 3.1 No custom Typography

A `rg "Typography\(|typography ="` across `app/` and `presentation-core/`
returns **zero hits**. The `TachiyomiTheme` call to `MaterialTheme(...)` does
not pass a `typography` argument, so the app uses **`androidx.compose.material3.Typography()`**
defaults (Roboto, M3 type scale).

The single typography file is:

**File:** `presentation-core/src/main/java/tachiyomi/presentation/core/theme/Typography.kt`

```kotlin
val Typography.header: TextStyle
    @Composable
    get() = bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
    )
```

It defines one extension property, `Typography.header`, used for section
headers in settings screens. Nothing else.

### 3.2 Fonts

- **Default Roboto** (system/M3 default). No custom `FontFamily`, no `Font(...)`
  resource, no variable fonts.
- `FontFamily.Monospace` is used in exactly 3 places for code-style content:
  - `WorkerInfoScreen.kt`
  - `BackupSchemaScreen.kt`
  - `CodeEditScreen.kt` (player subtitle/script editor)
- Subtitle font selection for the MPV player is **not** Compose typography —
  it's an MPV-side `sub-font` property controlled via
  `SubtitlePreferences.subtitleFont()` (default "Sans Serif") in
  `app/src/main/java/eu/kanade/tachiyomi/ui/player/settings/SubtitlePreferences.kt`.

### 3.3 Default text style override

Per §1.2, `setComposeContent` overrides `LocalTextStyle` to
`MaterialTheme.typography.bodySmall`. So any `Text(...)` without an explicit
`style` argument renders as **M3 bodySmall**, not M3 bodyLarge as M3's own
defaults would dictate. This is a small but pervasive divergence from pure M3.

---

## 4. Spacing

### 4.1 The `Padding` system (light, but real)

**File:** `presentation-core/src/main/java/tachiyomi/presentation/core/components/material/Constants.kt`

```kotlin
val topSmallPaddingValues = PaddingValues(top = MaterialTheme.padding.small)

const val DISABLED_ALPHA = .38f
const val SECONDARY_ALPHA = .78f

class Padding {
    val extraLarge  = 32.dp
    val large       = 24.dp
    val medium      = 16.dp
    val mediumSmall = 12.dp
    val small       = 8.dp
    val extraSmall  = 4.dp
}

val MaterialTheme.padding: Padding
    get() = Padding()
```

It is exposed as `MaterialTheme.padding` (an extension property on
`MaterialTheme`). Used across the codebase:

```
$ rg "MaterialTheme\.padding\." app/ presentation-core/ presentation-widget/ | wc -l
329
```

So there **is** a spacing system, but it is opt-in. The 6-step scale
(4/8/12/16/24/32 dp) is the M3 baseline layout grid.

### 4.2 Inline `dp` literals (pervasive)

Inline `.dp` literals are used far more than `MaterialTheme.padding.*`. A few
illustrative hot-spots:

- `MangaInfoHeader.kt` / `AnimeInfoHeader.kt`: 16 inline `16.dp` each.
- `CommonEntryItem.kt`: 8 inline `4.dp`.
- `AnimeEpisodeListItem.kt`: 7 inline `4.dp`, 4 inline `12.dp`.
- `AppThemePreferenceWidget.kt`: 6 inline `8.dp`, 4 inline `4.dp`.

There is no `Spacing` object. There is no design-token file. Inline values
outside the 4/8/12/16/24/32 ladder do appear (e.g., `17.dp` for the theme
preview corner radius, `56.dp` for sheet anchors, `125.dp` for swipe velocity
thresholds) but are local to single composables.

### 4.3 Other layout utilities

- `presentation-core/src/main/java/tachiyomi/presentation/core/util/PaddingValues.kt`
  — adds `operator fun PaddingValues.plus(other: PaddingValues): PaddingValues`
  for combining insets.
- `Constants.kt` also exports `DISABLED_ALPHA = 0.38f` and
  `SECONDARY_ALPHA = 0.78f` (M3-aligned alpha constants) used by button and
  text-secondary modifiers.
- `topSmallPaddingValues` — a process-wide `PaddingValues(top = small)`
  constant.

---

## 5. Component library

### 5.1 Forked M3 components (`presentation-core/.../components/material/`)

A distinctive feature of this codebase: Tachiyomi/Aniyomi **forks the source**
of several M3 components into `presentation-core/src/main/java/tachiyomi/presentation/core/components/material/`
rather than using the M3 artifacts directly. Each fork is a verbatim copy of
the AOSP source with Tachiyomi patches (the file headers explicitly say
"Straight copy from Compose M3" / "Tachiyomi changes:").

| File                          | M3 counterpart                              | Tachiyomi modifications |
| ----------------------------- | ------------------------------------------- | ----------------------- |
| `Button.kt`                   | `Button`, `TextButton`, `ButtonDefaults`, `ButtonElevation`, `ButtonColors` | Adds `onLongClick` slot to `Button`/`TextButton`; copies the whole elevation animation machinery |
| `Scaffold.kt`                 | `Scaffold`                                  | Adds `startBar` slot for NavigationRail; passes scroll behavior to topBar by default; removes expanded-app-bar height constraint; FAB height included in inner padding; consumed-insets handling |
| `Slider.kt`                   | `Slider`, `SliderState`, `SliderColors`, `SliderDefaults` | Fork (see file) |
| `Surface.kt`                  | `Surface`                                   | Fork |
| `NavigationBar.kt`            | `NavigationBar`                             | Removes the M3 horizontal spacer; hard-codes 80.dp height |
| `NavigationRail.kt`           | `NavigationRail`                            | Fork |
| `Tabs.kt`                     | `Tab` (text/badge row)                      | Exposes `TabText` composable with optional `Pill` badge count |
| `FloatingActionButton.kt`     | `FloatingActionButton`, `ExtendedFloatingActionButton` | Fork |
| `AlertDialog.kt`              | `AlertDialog`                               | Fork |
| `IconToggleButton.kt`         | `IconToggleButton`                          | Fork |
| `IconButtonTokens.kt`         | `IconButtonTokens` (M3 internal tokens)     | Fork |
| `PullRefresh.kt`              | `PullToRefresh`-related                     | Fork |
| `Constants.kt`                | (n/a)                                       | `Padding`, `DISABLED_ALPHA`, `SECONDARY_ALPHA`, `topSmallPaddingValues` |

**Implication for the design replacement:** these forks are a leverage point.
The forked files form a natural API seam — replacing their internal
implementation (or replacing what they delegate to) gives the new design
language a single place to intercept `Scaffold`/`Button`/`NavigationBar`/
etc. for the whole app — *but only where callers use the forked import*.
Many call sites import `androidx.compose.material3.*` directly (see §6), so
the fork doesn't fully decouple the app from M3.

### 5.2 Custom composables in `presentation-core/.../components/`

| File                          | Purpose |
| ----------------------------- | ------- |
| `LinkIcon.kt`                 | `IconButton` + `Icon` that opens a URL via `LocalUriHandler` |
| `Pill.kt`                     | Rounded pill badge (used for unread counts, tab badges) |
| `Badges.kt`                   | Stacked badge composable |
| `LabeledCheckbox.kt`          | Checkbox with label |
| `AdaptiveSheet.kt`            | Bottom-sheet on phones / centered dialog on tablets; swipe-to-dismiss; **used instead of `ModalBottomSheet`** (M3 `ModalBottomSheet` count = 0) |
| `WheelPicker.kt`              | Custom scroll-wheel picker |
| `ActionButton.kt`             | Large action button (e.g., "Add to library") |
| `LazyGrid.kt`                 | Lazy grid helpers (grid layouts for library) |
| `LazyList.kt`, `LazyColumnWithAction.kt` | Lazy list helpers |
| `SectionCard.kt`              | Card with a header section (settings grouping) |
| `VerticalFastScroller.kt`     | Fast-scroll handle for vertical lists |
| `CircularProgressIndicator.kt`| Wrapper around M3 `CircularProgressIndicator` |
| `TwoPanelBox.kt`              | Two-pane layout (master/detail) |
| `ListGroupHeader.kt`          | Sticky-style group header |
| `CollapsibleBox.kt`           | Expandable/collapsible container |
| `SettingsItems.kt`            | Shared settings-row composables |

### 5.3 Screens (`presentation-core/.../screens/`)

- `InfoScreen.kt` — generic info screen with icon, message, action button.
- `LoadingScreen.kt` — centered `CircularProgressIndicator`.
- `EmptyScreen.kt` — empty-state screen with icon + text + actions.

### 5.4 App-level components (`app/.../eu/kanade/presentation/components/`)

| File                              | Purpose |
| --------------------------------- | ------- |
| `AppBar.kt`                       | The app's `AppBar` / `SearchToolbar` / `AppBarActions` / `AppBarTitle` — wraps M3 `TopAppBar` (457 lines) |
| `TabbedScreen.kt`                 | Scaffold + `PrimaryTabRow`/`ScrollableTabRow` + `HorizontalPager` (used by History/Browse tabs) |
| `TabbedDialog.kt`                 | Same but inside a dialog |
| `DropdownMenu.kt`                 | Custom dropdown wrapper |
| `AdaptiveSheet.kt`                | Duplicate of presentation-core's (app-side) |
| `FloatingActionAddButton.kt`      | FAB with a `+` icon and progress arc |
| `Banners.kt`                      | AppStateBanners (incognito / download-only / indexing) + color constants |
| `DateText.kt`                     | Relative/absolute date text |
| `ItemDownloadIndicator.kt`        | Download progress indicator for entries |
| `EntryDownloadDropdownMenu.kt`    | Download-action overflow menu |
| `EmptyScreen.kt`                  | App-level empty screen |

### 5.5 Other component clusters

- `app/.../presentation/more/LogoHeader.kt` — `LogoHeader` (icon + divider).
- `app/.../presentation/more/NewUpdateScreen.kt`, `MoreScreen.kt`.
- `app/.../presentation/more/settings/widget/` — full preference widget library:
  `BasePreferenceWidget`, `SwitchPreferenceWidget`, `TextPreferenceWidget`,
  `EditTextPreferenceWidget`, `ListPreferenceWidget`,
  `MultiSelectListPreferenceWidget`, `TriStateListDialog`,
  `TrackingPreferenceWidget`, `InfoWidget`, `PreferenceGroupHeader`,
  `AppThemePreferenceWidget`, `AppThemeModePreferenceWidget`.
- `app/.../presentation/entries/components/` — `ItemCover`, `ItemHeader`,
  `EntryToolbar`, `EntryBottomActionMenu`, `DotSeparatorText`,
  `MissingItemCountListItem`, `ItemsDialogs`.
- `app/.../presentation/player/components/` — player-specific composables:
  `RepeatingIconButton`, `OvalBox`, `ExposedTextDropDownMenu`, `PlayerSheet`,
  `SliderItem`, `TintedSliderItem`, `OutlinedNumericChooser`,
  `SwitchPreference`, `ExpandableCard`.
- `app/.../presentation/reader/` — reader-specific: `ChapterTransition`,
  `DisplayRefreshHost`, `ReadingModeSelectDialog`, `OrientationSelectDialog`,
  `ReaderPageActionsDialog`, `ReaderContentOverlay`, `PageIndicatorText`,
  `ModeSelectionDialog`, plus `appbars/ReaderAppBars.kt` + `BottomReaderBar.kt`,
  and `settings/` (ReaderSettingsDialog, GeneralSettingsPage, etc.).
- `presentation-widget/` — Glance-based home-screen widgets
  (`UpdatesAnimeWidget`, `LockedAnimeWidget`, etc.). Uses its own
  `colors_appwidget.xml` and `dimens.xml`; not Compose-M3.

---

## 6. Material 3 component usage

### 6.1 Files importing `androidx.compose.material3.*`

```
291  files  import androidx.compose.material3.*
1112 total M3 import statements
```

### 6.2 Top symbols imported from `androidx.compose.material3.*`

```
203  MaterialTheme              ← the dominant access point (colorScheme, typography, shapes)
184  Text
105  Icon
 50  TextButton
 42  IconButton
 35  AlertDialog
 29  SnackbarHostState
 28  HorizontalDivider
 24  Surface
 22  LocalContentColor
 22  DropdownMenuItem
 19  lightColorScheme
 19  darkColorScheme
 17  Button
 15  SnackbarHost
 14  CircularProgressIndicator
 11  LocalTextStyle
 11  FilterChip
  9  ProvideTextStyle
  9  OutlinedTextField
  9  Checkbox
  8  ripple
  8  contentColorFor
  8  OutlinedButton
  7  TopAppBarScrollBehavior
  6  VerticalDivider
  6  TopAppBarDefaults
  6  ColorScheme
  5  TopAppBar
  5  ExperimentalMaterial3Api
  5  ButtonDefaults
  5  Badge
  4  surfaceColorAtElevation
  4  rememberTopAppBarState
  4  minimumInteractiveComponentSize
  4  Tab
  4  Switch
  4  SnackbarResult
  4  SnackbarDuration
  4  RadioButton
```

### 6.3 Token-occurrence counts (across `app/`, `presentation-core/`, `presentation-widget/`)

These are total occurrences of the bare token (so `Text` will also match
`Text()` calls and any local variable named `Text`, but the relative ranking
is still informative). Sorted by frequency:

| Rank | Component                    | Occurrences | Notes |
| ---: | ---------------------------- | ----------: | ----- |
|  1   | `Text`                       | 895         | Universal |
|  2   | `Icon`                       | 343         | Universal |
|  3   | `Dialog`                     | 230         | Includes `AlertDialog`, `Dialog(...)` and the `Dialog` slot |
|  4   | `Scaffold`                   | 162         | Mostly via `tachiyomi.presentation.core.components.material.Scaffold` (forked) |
|  5   | `IconButton`                 | 125         | |
|  6   | `Button`                     |  85         | Includes forked `Button`/`TextButton` |
|  7   | `AlertDialog`                |  83         | |
|  8   | `Surface`                    |  64         | |
|  9   | `CircularProgressIndicator`  |  44         | Includes a forked wrapper |
| 10   | `DropdownMenu`               |  42         | Mostly `DropdownMenuItem` |
| 11   | `OutlinedTextField`          |  27         | |
| 12   | `Switch`                     |  26         | |
| 13   | `PullRefresh`                |  23         | Forked component family |
| 14   | `Checkbox`                   |  22         | |
| 15   | `Slider`                     |  13         | Includes forked `Slider` |
| 16   | `TopAppBar`                  |  10         | |
| 17   | `LinearProgressIndicator`    |   9         | |
| 18   | `RadioButton`                |   8         | |
| 19   | `NavigationBar`              |   8         | Mostly via forked wrapper |
| 20   | `FloatingActionButton`       |   7         | Mostly via forked wrapper |
| 21   | `ListItem`                   |   6         | |
| 22   | `Card`                       |   6         | Low — the app uses `Surface` and custom `SectionCard` more than `Card` |
| 23   | `BadgedBox`                  |   6         | |
| 24   | `TooltipBox`                 |   5         | |
| 25   | `Snackbar`                   |   5         | |
| 26   | `SegmentedButton`            |   5         | |
| 27   | `NavigationRail`             |   5         | Mostly via forked wrapper |
| 28   | `TextField`                  |   4         | Used via `TextFieldDefaults` mostly |
| –   | `ModalBottomSheet`           |   0         | **Replaced by custom `AdaptiveSheet`** |
| –   | `TabRow`                     |   0         | App uses `PrimaryTabRow` / `ScrollableTabRow` (newer M3 APIs) |
| –   | `CenterAlignedTopAppBar`     |   0         | App uses `TopAppBar` directly |
| –   | `Divider`                    |   0         | App uses `HorizontalDivider`/`VerticalDivider` (28+6 imports) |

Also note `app/build.gradle.kts` opts the whole `:app` module into
`-opt-in=androidx.compose.material3.ExperimentalMaterial3Api`.

### 6.4 What this tells us about replacement effort

- `Text`, `Icon` are universal — every screen uses them. These are the
  lowest-level building blocks; any new design system must provide drop-in
  equivalents or accept a sweeping call-site rewrite.
- `Scaffold` (162 occurrences) is mostly funneled through the **forked**
  `tachiyomi.presentation.core.components.material.Scaffold`. Replacing the
  fork's internals is a leverage point, but only ~1 file directly imports M3's
  own `Scaffold` (see §6.2 import counts); the rest import the fork.
- `AlertDialog` (83), `IconButton` (125), `Button` (85), `Surface` (64),
  `CircularProgressIndicator` (44), `DropdownMenu` (42) are imported from
  `androidx.compose.material3.*` **directly** in most call sites — there is
  no fork indirection. These will need either (a) an import-rewrite pass
  to swap to new design system equivalents, or (b) a new design system that
  deliberately re-uses the M3 type names so existing imports resolve.
- `ModalBottomSheet` is unused — replacing `AdaptiveSheet` is sufficient for
  bottom-sheet UX.

---

## 7. Custom theming (theme preference system)

### 7.1 `AppTheme` enum

**File:** `app/src/main/java/eu/kanade/domain/ui/model/AppTheme.kt`

```kotlin
enum class AppTheme(val titleRes: StringResource?) {
    DEFAULT(MR.strings.label_default),
    MONET(MR.strings.theme_monet),
    CLOUDFLARE(AYMR.strings.theme_cloudflare),
    COTTONCANDY(AYMR.strings.theme_cottoncandy),
    DOOM(AYMR.strings.theme_doom),
    GREEN_APPLE(MR.strings.theme_greenapple),
    LAVENDER(MR.strings.theme_lavender),
    MATRIX(AYMR.strings.theme_matrix),
    MIDNIGHT_DUSK(MR.strings.theme_midnightdusk),
    MOCHA(AYMR.strings.theme_mocha),
    SAPPHIRE(AYMR.strings.theme_sapphire),
    NORD(MR.strings.theme_nord),
    STRAWBERRY_DAIQUIRI(MR.strings.theme_strawberrydaiquiri),
    TAKO(MR.strings.theme_tako),
    TEALTURQUOISE(MR.strings.theme_tealturquoise),
    TIDAL_WAVE(MR.strings.theme_tidalwave),
    YINYANG(MR.strings.theme_yinyang),
    YOTSUBA(MR.strings.theme_yotsuba),
    MONOCHROME(MR.strings.theme_monochrome),

    // Deprecated
    DARK_BLUE(null),
    HOT_PINK(null),
    BLUE(null),
}
```

**19 active themes** (DEFAULT + MONET + 17 custom) plus 3 deprecated stubs
(retained for migration only — they have `titleRes = null` so they are filtered
out of the picker).

The picker further filters `MONET` out when `!DeviceUtil.isDynamicColorAvailable`.

### 7.2 `ThemeMode` enum

**File:** `app/src/main/java/eu/kanade/domain/ui/model/ThemeMode.kt`

```kotlin
enum class ThemeMode { LIGHT, DARK, SYSTEM }

fun setAppCompatDelegateThemeMode(themeMode: ThemeMode) {
    AppCompatDelegate.setDefaultNightMode(
        when (themeMode) {
            ThemeMode.LIGHT  -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.DARK   -> AppCompatDelegate.MODE_NIGHT_YES
            ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        },
    )
}
```

Light/dark mode is therefore controlled at the **Android platform level** via
`AppCompatDelegate.setDefaultNightMode`, not via a Compose override of
`isSystemInDarkTheme()`. `TachiyomiTheme` reads `isSystemInDarkTheme()` and
trusts the platform to honor the AppCompat night mode.

### 7.3 `UiPreferences` theme keys

**File:** `app/src/main/java/eu/kanade/domain/ui/UiPreferences.kt`

```kotlin
class UiPreferences(private val preferenceStore: PreferenceStore) {
    fun themeMode()      = preferenceStore.getEnum("pref_theme_mode_key",   ThemeMode.SYSTEM)
    fun appTheme()       = preferenceStore.getEnum(
        "pref_app_theme",
        if (DeviceUtil.isDynamicColorAvailable) AppTheme.MONET else AppTheme.DEFAULT,
    )
    fun themeDarkAmoled()= preferenceStore.getBoolean("pref_theme_dark_amoled_key", false)
    // … plus relativeTime, dateFormat, tabletUiMode, startScreen, navStyle
}
```

Three keys:

| Preference key                  | Type      | Default                              |
| ------------------------------- | --------- | ------------------------------------ |
| `pref_theme_mode_key`           | `ThemeMode` enum | `SYSTEM`                       |
| `pref_app_theme`                | `AppTheme` enum  | `MONET` if dynamic color available, else `DEFAULT` |
| `pref_theme_dark_amoled_key`    | `Boolean` | `false`                              |

Default app theme = MONET (Material You) on Android 12+, DEFAULT (blue) below.

### 7.4 Dynamic-color availability

**File:** `app/src/main/java/eu/kanade/tachiyomi/util/system/DeviceUtilExtensions.kt`

```kotlin
val DeviceUtil.isDynamicColorAvailable by lazy {
    DynamicColors.isDynamicColorAvailable() ||
        (DeviceUtil.isSamsung && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
}
```

Note the special-case for Samsung devices (Samsung's `DynamicColors` check is
known to mis-report on One UI).

### 7.5 Theme selection UI

Two entry points:

1. **Onboarding** — `app/.../presentation/more/onboarding/ThemeStep.kt`:
   ```kotlin
   AppThemeModePreferenceWidget(value = themeMode, onItemClick = {
       themeModePref.set(it); setAppCompatDelegateThemeMode(it)
   })
   AppThemePreferenceWidget(value = appTheme, amoled = amoled, onItemClick = { appThemePref.set(it) })
   ```

2. **Settings → Appearance** —
   `app/.../presentation/more/settings/screen/SettingsAppearanceScreen.kt`
   exposes the same two widgets plus an AMOLED `SwitchPreference`. AMOLED is
   disabled when `themeMode == LIGHT`.

The `AppThemePreferenceWidget` (`app/.../presentation/more/settings/widget/AppThemePreferenceWidget.kt`)
renders a horizontally-scrolling row of live `TachiyomiTheme(appTheme = …, amoled = …)`
mini-previews — each preview is a 9:16 card with a fake app bar, cover, and
bottom bar painted in that theme's `colorScheme`. Selecting a theme calls
`onItemClick(appTheme)` and then `ActivityCompat.recreate(activity)` so the
new XML theme takes effect.

### 7.6 `ThemingDelegate` (XML side)

`app/.../ui/base/delegate/ThemingDelegate.kt` maps `AppTheme` →
`R.style.Theme_Tachiyomi_<Name>` for the AppCompat/XML world, plus the
`R.style.ThemeOverlay_Tachiyomi_Amoled` overlay when AMOLED is on.

```kotlin
private val themeResources: Map<AppTheme, Int> = mapOf(
    AppTheme.MONET to R.style.Theme_Tachiyomi_Monet,
    AppTheme.COTTONCANDY to R.style.Theme_Tachiyomi_CottonCandy,
    // … 18 entries …
)
```

---

## 8. Icon system

### 8.1 Library

**Catalog:** `gradle/compose.versions.toml`
```toml
material-icons = { module = "androidx.compose.material:material-icons-extended" }
```

**Consumers:** `:app` and `:presentation-core` (via `implementation(compose.material.icons)`).

This is the **Material Icons Extended** pack from the Compose Material (M2)
artifact — the same icon library that M3 docs recommend. There is no Lucide,
no Phosphor, no icon font.

### 8.2 Usage breakdown

```
575  total `import androidx.compose.material.icons.*` lines

241  outlined       (Icons.Outlined.*)
150  generic Icons  (Icons.<Name>.* defaulting to Filled; some auto-resolved)
133  filled         (Icons.Filled.*)
 43  automirrored   (Icons.AutoMirrored.* — e.g., ArrowBack, HelpOutline)
  8  rounded        (Icons.Rounded.*)
```

Most-used individual icons: `HelpOutline`, `FilterList`, `Close`, `Refresh`,
`Delete`, `Public`, `Settings`, `SelectAll`, `Info`, `FlipToBack`,
`CheckCircle`, `Schedule`, `Save`, `Download`, `DoneAll`, `Done`,
`CollectionsBookmark`, `VisibilityOff`, `Bookmark`, `Share`, `PushPin`,
`GetApp`, `FavoriteBorder`.

### 8.3 Custom icons

**Files:** `presentation-core/src/main/java/tachiyomi/presentation/core/icons/`

```
CustomIcons.kt    ← empty object: `object CustomIcons` (extension receiver)
Discord.kt        ← val CustomIcons.Discord: ImageVector
Github.kt         ← val CustomIcons.Github: ImageVector
```

Two custom brand icons (`Discord`, `Github`) hand-converted from
[simpleicons.org](https://simpleicons.org) using
[`svg-to-compose`](https://github.com/DevSrSouza/svg-to-compose). Each is a
full `ImageVector.Builder` definition with explicit path data. They are
referenced as `CustomIcons.Discord` / `CustomIcons.Github` (extension
properties on the `CustomIcons` object).

### 8.4 Vector drawables (XML)

`app/src/main/res/drawable/` contains **71 XML vector drawables** (no PNGs;
PNGs live in `drawable-nodpi/` for things like the launcher icon). Examples:

```
ic_ani.xml                       ← app logo (used by LogoHeader)
ic_ani_monochrome_launcher.xml
ic_animelibrary_filled_24dp.xml  ← tab icons (filled + outline + selector)
ic_animelibrary_outline_24dp.xml
ic_arrow_back_24dp.xml
ic_book_24dp.xml
ic_brightness_negative_20dp.xml
ic_chrome_player_mode_24dp.xml
ic_close_24dp.xml
ic_crop_24dp.xml
ic_done_24dp.xml
ic_download_item_24dp.xml
ic_drag_handle_24dp.xml
ic_extension_24dp.xml
ic_folder_24dp.xml
ic_forward_10_24dp.xml           ← player skip-forward
ic_glasses_24dp.xml
ic_info_24dp.xml
ic_launcher_background.xml
ic_launcher_foreground.xml
ic_overflow_24dp.xml
… (etc., 71 total)
```

These are referenced from Kotlin via `painterResource(R.drawable.ic_*)`:

```
13  `painterResource(...)` call sites in Kotlin
```

The split is therefore:

- **Material Icons Extended** (`Icons.Outlined.X` etc.) — the dominant icon
  source for toolbar/action icons.
- **Custom XML drawables** (`R.drawable.ic_*`) — used for: app/launcher
  branding (`ic_ani`, `ic_tachi_splash`), the 6 bottom-nav tab icons
  (`ic_animelibrary_*_24dp`, `ic_browse_*`, etc.), and a handful of player
  /reader-specific icons (`ic_forward_10_24dp`, `ic_brightness_negative_20dp`,
  `ic_chrome_player_mode_24dp`).
- **`CustomIcons.Discord` / `CustomIcons.Github`** — used only in the About
  screen's `LinkIcon` row.

### 8.5 Splash icon

`themes.xml` defines:
```xml
<style name="Theme.Tachiyomi.SplashScreen" parent="Theme.SplashScreen">
    <item name="windowSplashScreenAnimatedIcon">@drawable/ic_tachi_splash</item>
    <item name="windowSplashScreenBackground">@color/splash</item>
    <item name="postSplashScreenTheme">@style/Theme.Tachiyomi</item>
    …
</style>
```

`@color/splash` is `@color/accent_blue` = `#54759E` (a non-M3 legacy accent
blue, kept for splash only).

---

## Appendix A — Key file paths (quick reference)

| Concern                  | Path |
| ------------------------ | ---- |
| Compose theme entry      | `app/src/main/java/eu/kanade/presentation/theme/TachiyomiTheme.kt` |
| Color scheme base        | `app/src/main/java/eu/kanade/presentation/theme/colorscheme/BaseColorScheme.kt` |
| Default color scheme     | `app/src/main/java/eu/kanade/presentation/theme/colorscheme/TachiyomiColorScheme.kt` |
| Monet (dynamic color)    | `app/src/main/java/eu/kanade/presentation/theme/colorscheme/MonetColorScheme.kt` |
| All color schemes        | `app/src/main/java/eu/kanade/presentation/theme/colorscheme/*.kt` (20 files) |
| Theme application helper | `app/src/main/java/eu/kanade/tachiyomi/util/view/ViewExtensions.kt` |
| Theme preferences        | `app/src/main/java/eu/kanade/domain/ui/UiPreferences.kt` |
| `AppTheme` enum          | `app/src/main/java/eu/kanade/domain/ui/model/AppTheme.kt` |
| `ThemeMode` enum         | `app/src/main/java/eu/kanade/domain/ui/model/ThemeMode.kt` |
| XML themes               | `app/src/main/res/values/themes.xml`, `values-night/themes.xml`, `values-v27/themes.xml`, `values-v31/themes.xml`, `values-night-v31/themes.xml` |
| XML styles               | `app/src/main/res/values/styles.xml` |
| XML colors (shared)      | `presentation-core/src/main/res/values/colors.xml` |
| XML colors (themes)      | `presentation-core/src/main/res/values/colors_*.xml` + `values-night/colors_*.xml` (17 + 17) |
| XML colors (app-only)    | `app/src/main/res/values/colors_mocha.xml`, `colors_cottoncandy.xml` (+ night) |
| Typography ext           | `presentation-core/src/main/java/tachiyomi/presentation/core/theme/Typography.kt` |
| Color ext                | `presentation-core/src/main/java/tachiyomi/presentation/core/theme/Color.kt` |
| Padding system           | `presentation-core/src/main/java/tachiyomi/presentation/core/components/material/Constants.kt` |
| Forked M3 components     | `presentation-core/src/main/java/tachiyomi/presentation/core/components/material/` (13 files) |
| Custom composables       | `presentation-core/src/main/java/tachiyomi/presentation/core/components/` (~20 files) |
| Generic screens          | `presentation-core/src/main/java/tachiyomi/presentation/core/screens/` (3 files) |
| Custom icons             | `presentation-core/src/main/java/tachiyomi/presentation/core/icons/` (3 files) |
| App-level components     | `app/src/main/java/eu/kanade/presentation/components/` |
| App settings widgets     | `app/src/main/java/eu/kanade/presentation/more/settings/widget/` |
| Theme picker UI          | `app/src/main/java/eu/kanade/presentation/more/settings/widget/AppThemePreferenceWidget.kt`, `AppThemeModePreferenceWidget.kt` |
| Appearance settings      | `app/src/main/java/eu/kanade/presentation/more/settings/screen/SettingsAppearanceScreen.kt` |
| Onboarding theme step    | `app/src/main/java/eu/kanade/presentation/more/onboarding/ThemeStep.kt` |
| XML theme mapping        | `app/src/main/java/eu/kanade/tachiyomi/ui/base/delegate/ThemingDelegate.kt` |
| Dynamic-color check      | `app/src/main/java/eu/kanade/tachiyomi/util/system/DeviceUtilExtensions.kt` |
| Vector drawables         | `app/src/main/res/drawable/*.xml` (71 files) |
| Theme previews           | `presentation-core/src/main/java/tachiyomi/presentation/core/util/ThemePreviews.kt` (`@ThemePreviews` annotation) |

## Appendix B — Coupling summary (preview of replacement analysis)

- **Theme entry**: 1 file (`TachiyomiTheme.kt`) wraps `MaterialTheme`. **High
  leverage** — replacing this file alone swaps the colorScheme for the whole
  app. The `setComposeContent` extension is the only path into Compose for
  activities.
- **Color**: 20 Kotlin color-scheme files + 34 XML color files + 5 XML themes
  files. AMOLED + Monet are special-cased. M3 `ColorScheme` is the contract
  everywhere; downstream code reads `MaterialTheme.colorScheme.<token>`.
- **Typography**: zero custom typography. Default M3 only. Trivial to replace
  (one extension property to migrate).
- **Shapes**: zero custom shapes. Default M3 only. Trivial.
- **Spacing**: `MaterialTheme.padding` (6-step ladder) used 329 times; inline
  `dp` literals everywhere else. Moderate to replace.
- **Components**: 13 forked M3 components in `presentation-core/.../material/`
  are a leverage point. But ~290 files import `androidx.compose.material3.*`
  directly, with `Text`/`Icon`/`Button`/`AlertDialog`/`IconButton`/`Surface`
  being imported directly in most call sites rather than via the fork.
- **Icons**: Material Icons Extended + 71 XML vector drawables + 2 custom
  `CustomIcons`. Tight coupling to `Icons.Outlined.X` pattern.
- **Theme preference system**: 19 active themes, 3 deprecated, plus AMOLED +
  light/dark/system. `AppTheme` enum + `UiPreferences` keys + `ThemingDelegate`
  XML style map. Any new design language must decide how much of this user-
  facing theme picker survives.
