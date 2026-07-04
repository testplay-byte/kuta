# Current State (snapshot: 2026-07-05, Phase 2 complete)

Detailed companion to `NEW_SESSION_START_HERE.md`. Read that first for the
10-minute orientation; this file has the full detail.

---

## Phases completed

### Phase 0: Setup (done)
- Forked Aniyomi → kuta repo, full git history preserved.
- CI workflow (`.github/workflows/build.yml`): JDK 21, gradle cache,
  `:app:assembleDebug --no-daemon`, upload arm64-v8a APK artifact.
- Android toolchain installed (portable, non-root): Temurin JDK 21,
  Android cmdline-tools + platform-tools.
- Git identity: `Kuta Coder <testplay-byte@users.noreply.github.com>`.
- PAT secured in `MEMORY/credentials/github-pat.txt` (gitignored) +
  `~/.git-credentials` (credential.helper store).

### Phase 1: Branding + manga gating + placeholder home (done, verified on device)
- `applicationId`: `xyz.jmir.tachiyomi.mi` → `app.kuta`.
- Branding: "Aniyomi" → "Kuta" in `app_name` + 2 i18n-aniyomi strings +
  About screen ("Kuta / Based on Aniyomi" item). Internal identifiers
  (User-Agent, deep-link scheme, extension name parsing) left untouched.
- Manga UI gating: `MangaLibraryTab` removed from `NavStyle.tabs`; `MoreTab`
  onClickAlt gated; `HomeScreen.defaultTab` falls back to AnimeLibraryTab.
  Manga screen files / DB / repos / source interfaces ALL intact.
- Placeholder home: `PlaceholderHomeScreen.kt` (centered "Kuta" + subtitle +
  Library/Settings buttons). `MainActivity` root Navigator → PlaceholderHomeScreen.
- Known gap: BrowseTab still has manga sub-tabs (coupling issue, deferred).
  Shared tabs (Updates/History/Categories/Stats/Downloads) still show mixed content.

### Phase 1.5: Architecture investigation + docs (done)
- `FEATURES/` — 92 features across 11 category files + README index.
  Status: 35 keep, 34 modify, 9 remove, 15 TBD.
- `DIAGRAMS/` — 7 Mermaid diagrams (nav flow, module deps, feature
  relationships, data flow, UI structure, DB schema, extension lifecycle).
- `DOCS/architecture/` — 12 architecture docs (modules, package tree, entry
  points, navigation, DI, player, extensions, database, preferences, data
  models, build variants, Animiru diff).
- `DOCS/design-system/current.md` — Material 3 baseline investigation
  (coupling 7/10, 291 files import M3 directly, 18-theme picker, etc.).
- `DOCS/design-system/alternatives-research.md` — 6 design languages researched
  (user picked 4: Neon, Notebook, Brutalist, Material).

### Phase 2: Design system (done, CI green, user-tested)
- **Foundation**: `KutaTheme` + 5 composition locals + `KutaPreferences`
  (PreferenceStore-backed, registered in Injekt). `MainActivity` wrapped in
  `KutaTheme`. TachiyomiTheme stays as M3 fallback.
- **29 `Kuta*` components**: delegators that read `LocalDesignLanguage.current`
  and dispatch to the active design.
- **4 design implementations** (29 components each):
  - `material/` — wraps existing androidx.compose.material3 components.
  - `neon/` — dark-first, glow-on-hover, glass borders, JetBrains Mono for data.
  - `notebook/` — warm earth tones, paper texture, slight rotations, Caveat font.
  - `brutalist/` — 3dp black borders, hard zero-blur shadows, press animations,
    uppercase Inter Black.
- **7 effect Modifiers**: `neonGlow`, `neonGridPattern`, `paperTexture`,
  `ruledLines`, `notebookMarginLine`, `hardShadow`, `brutalistGrid`.
- **Fonts**: Inter, JetBrains Mono, Caveat (variable TTFs in
  `presentation-core/src/main/res/font/`).
- **Haze 0.7.0** dependency added (not yet wired for real blur).
- **Settings UI**: `KutaAppearanceScreen` — design selector + mode + accent
  picker. Uses `Kuta*` components → live reskins. Wired into
  SettingsAppearanceScreen as "Kuta Design" entry.

**User feedback (2026-07-05)**: designs work on the settings screen but the
rest of the app still looks Material 3 (expected — no other screens migrated).
Neon "doesn't feel neon enough", Notebook "better", Brutalist "promising".
Root cause = only one screen uses `Kuta*` components. Phase 3 builds new custom
screens to make designs visible app-wide.

---

## What's NOT done (known gaps, by priority)

1. **No custom screens built yet** — only `KutaAppearanceScreen` uses `Kuta*`
   components. All other screens use raw M3. **This is the Phase 3 work.**
2. **Glass-morphism (Haze)**: dependency added, not wired. `bgGlass` is a
   translucent color (alpha 0.85), not real backdrop-blur.
3. **Animations**: minimal. No `animate*AsState` for smooth hover/focus/press
   transitions. States change binary.
4. **Custom color picker**: basic RGB confirm dialog (no sliders/hex/HSV).
5. **Slider thumb glow**: M3 doesn't expose thumb modifier.
6. **BrowseTab manga sub-tabs**: still expose manga (coupling issue from Phase 1).
7. **Shared tabs show mixed anime+manga**: Updates/History/Categories/Stats/
   Downloads still show both. Deeper gating deferred.
8. **Branding leakage**: `AppUpdateChecker.GITHUB_REPO` still "aniyomiorg/aniyomi",
   About footer links still aniyomi.org, CSV filename "aniyomi_library.csv".
9. **18-theme picker**: Aniyomi's existing theme picker still exists alongside
   the new Kuta Design picker. Decision pending: keep/shrink/drop.
10. **TextStyle.textTransform Uppercase**: not applied (callers use
    `String.uppercase()` as workaround).

---

## Key architecture facts (quick reference)

- **Player**: MPV (`com.github.aniyomiorg:aniyomi-mpv-lib:1.18.n`), NOT Media3.
- **Database**: SQLDelight 2.0.2, NOT Room. **Two** DBs: `tachiyomi.db` (manga,
  schema v32) + `tachiyomi.animedb` (anime, schema v135). 18 tables + 11 views.
- **DI**: Injekt (Mihon fork `com.github.mihonapp:injekt:91edab2317`), NOT Hilt.
- **Navigation**: Voyager 1.0.1. Single root Navigator + one TabNavigator.
- **No hard-coded default extension repo URL** — user-added via UI/deep-link.
- **Toolchain**: compileSdk 35, minSdk 26, targetSdk 34, build-tools 35.0.1,
  NDK 27.1.12297006, Java/Kotlin target 17. No `jvmToolchain()` in build, but
  Gradle daemon auto-provisions JDK 17 via foojay.
- **Modules**: 13 Gradle modules. `:app` depends on 11 others; `:macrobenchmark`
  targets `:app`. Convention plugins in `buildSrc/` (not `build-logic/`).
- **Build types**: 4 (debug/.dev, release/minify, preview/.debug, benchmark/.benchmark).
  **No flavors** (stale `androidComponents` flavor refs at app/build.gradle.kts:315-329
  are dead no-ops).

---

## How the design system works (for new agents)

```
User selects design in Settings → KutaPreferences updates (SharedPreferences)
  → StateFlow emits → MainActivity collectAsState recomposes
  → KutaTheme recomposes with new designLanguage/mode/accent
  → CompositionLocals (LocalDesignLanguage, LocalKutaColors, etc.) update
  → Every Kuta* component reads the new locals and re-renders in the new design
  → Instant switch, no restart
```

**Rule for new screens**: use `Kuta*` components (`KutaButton`, `KutaCard`,
`KutaInput`, etc.) — NEVER raw `androidx.compose.material3.*`. Read colors via
`kutaColors` (convenience accessor) or `LocalKutaColors.current`. Read typography
via `kutaTypography` or `LocalKutaTypography.current`. Read mode via
`LocalKutaMode.current`.

**Unmigrated screens** (those still using raw M3) look Material 3 regardless of
selected design — that's the intended fallback. Don't touch them unless the task
specifically asks.

---

## Design system file map

| File | Purpose |
|------|---------|
| `presentation-core/.../kuta/theme/DesignLanguage.kt` | `enum DesignLanguage { NEON, NOTEBOOK, BRUTALIST, MATERIAL }`, `enum KutaMode`, `data class KutaAccent` |
| `.../theme/KutaColors.kt` | `data class KutaColors` — 38 color tokens (union of all 4 designs) |
| `.../theme/KutaTypography.kt` | `data class KutaTypography` — 11 text styles |
| `.../theme/KutaSpacing.kt` | `object KutaSpacing` (7 dp values) + `object KutaMotion` (5 durations) |
| `.../theme/Locals.kt` | 5 composition locals + `kutaColors` / `kutaTypography` accessors |
| `.../theme/KutaTheme.kt` | The wrapper — resolves SYSTEM mode, builds colors/typography, provides locals |
| `.../theme/KutaFonts.kt` | `KutaFonts.Inter`, `.JetBrainsMono`, `.Caveat` (variable fonts) |
| `.../theme/AccentPresets.kt` | 5 Neon + 5 Notebook + 7 Brutalist + 6 Material accent presets |
| `.../components/KutaComponents.kt` | 29 `Kuta*` delegators (when block over 4 designs) |
| `.../components/KutaComponentApi.kt` | Enums: `KutaButtonVariant`, `KutaCardElevation`, `KutaToggleStyle`, etc. |
| `.../material/Material3Components.kt` | 29 M3 wrappers |
| `.../neon/NeonPalette.kt` | `NeonDarkColors(accent)`, `NeonLightColors(accent)`, `NeonTypography` |
| `.../neon/NeonComponents.kt` | 29 Neon component implementations (~1499 lines) |
| `.../notebook/NotebookPalette.kt` | `NotebookDarkColors`, `NotebookLightColors`, `NotebookTypography` |
| `.../notebook/NotebookComponents.kt` | 29 Notebook component implementations (~1204 lines) |
| `.../brutalist/BrutalistPalette.kt` | `BrutalistDarkColors`, `BrutalistLightColors`, `BrutalistTypography` |
| `.../brutalist/BrutalistComponents.kt` | 29 Brutalist component implementations (~1681 lines) |
| `.../preferences/KutaPreferences.kt` | `KutaPreferences` class (PreferenceStore-backed) |
| `.../effects/Effects.kt` | 7 Modifier extensions (neonGlow, hardShadow, paperTexture, etc.) |
| `app/.../settings/screen/KutaAppearanceScreen.kt` | Settings screen (uses Kuta* components) |

---

## Commit history (full)

```
0ab5a7e37 Phase 2B fix: resolve 6 compile errors (RowScope actions + Float random)
88cd30bdd Phase 2B: implement Neon, Notebook, Brutalist designs (3 × 29 components + palettes)
1fda401dc Phase 2A fix 3: fix Material3Dialog AlertDialog API
6b1054b18 Phase 2A fix 2: FontVariation API + move KutaAppearanceScreen to app
619baf2d0 Phase 2A fix: resolve compile errors (ms unit + missing imports in KutaTheme)
6952cc881 Phase 2A: KutaTheme foundation — 4 design languages, 31 Kuta* components, settings UI
287baf049 Phase 2 design specs (coordinator-pushed)
c179606ac Phase 1.5: feature inventory, diagrams, design system investigation
cbe42a1ee Phase 1: strip Aniyomi branding, gate manga UI, placeholder home
eee85aa3e DOCS: add architecture documentation (12 files)
60a1c7bd4 CI: build arm64-v8a only for faster debug builds; fix artifact path
e7de93d97 CI: add workflow_dispatch trigger
e42ba5927 Trigger initial CI run
23a55a472 Initial setup: rename to Kuta, add NOTICE, SETUP folder, CI workflow
2f5cf775c (upstream Aniyomi) chore(i18n): Translations update from Hosted Weblate
```
