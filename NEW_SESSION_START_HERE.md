# NEW SESSION START HERE

> **If you're a new AI agent starting a fresh session, read this file FIRST.**
> It's the complete handoff: where the project is, what's done, what's next, and
> how to get up and running in 10 minutes.

**Last updated**: 2026-07-05 (Phase 2 complete)

---

## What is this project?

**Kuta** is a fork of [Aniyomi](https://github.com/aniyomiorg/aniyomi) — an
open-source Android app (Kotlin + Jetpack Compose) for watching anime via
"extensions" (separate APKs that provide streaming sources).

We're rebuilding the front-end with a **custom multi-design-language system**
(4 user-switchable designs: Neon, Notebook, Brutalist, Material) while keeping
Aniyomi's extension/source/player machinery intact behind the scenes.

**The plan going forward**: we are NOT migrating Aniyomi's existing screens.
We're building **entirely new custom screens** (new layouts, new UX) on top of
the `Kuta*` component library. The current Aniyomi screens stay as fallback
(they render as Material 3 regardless of selected design).

---

## Current state (Phase 2 complete)

### What works
- **CI builds green**: push to `main` → GitHub Actions builds a debug APK
  (arm64-v8a only) → uploaded as artifact. ~2-4 min with warm cache.
  - Workflow: `.github/workflows/build.yml`
  - APK path: `app/build/outputs/apk/debug/app-arm64-v8a-debug.apk`
- **App launches**: placeholder home screen ("Kuta" + "AniList browse coming soon"
  + Library/Settings buttons). Branding is "Kuta" throughout.
- **Manga gated**: manga Library tab removed from bottom nav. (Manga code/data
  layer fully intact — only the UI entry point is gated. Shared tabs like Browse
  still expose manga sub-tabs — known gap.)
- **Design system foundation**: `KutaTheme` + 5 composition locals + 29 `Kuta*`
  component delegators + 4 design implementations (Neon, Notebook, Brutalist,
  Material) + 7 effect Modifiers (neonGlow, hardShadow, paperTexture, etc.).
- **Settings → Appearance → "Kuta Design"**: switch between the 4 designs live
  (no restart). The settings screen itself uses `Kuta*` components, so it
  reskins when you switch.
- **Fonts bundled**: Inter (variable), JetBrains Mono (variable), Caveat
  (variable) in `presentation-core/src/main/res/font/`.
- **Haze 0.7.0** dependency added (for future Neon glass-morphism).

### What doesn't work yet (known gaps)
- **Only the KutaAppearanceScreen uses `Kuta*` components.** All other screens
  (home, library, browse, player, settings) still use raw `androidx.compose.material3.*`
  → they look Material 3 **regardless of selected design**. This is the expected
  fallback. **Phase 3 = build new custom screens** that use `Kuta*` components.
- **Glass-morphism (Haze)**: dependency added but not wired. `bgGlass` is a
  translucent color, not real backdrop-blur.
- **Animations**: minimal (no smooth hover/focus/press transitions).
- **Custom color picker**: basic RGB dialog (no sliders/hex/HSV).
- **User feedback on designs** (2026-07-05): Neon "doesn't feel neon enough",
  Notebook "better but sections not themed", Brutalist "promising but needs
  improvements". The core issue is that only one screen (the settings screen
  itself) shows the design — the rest of the app is still M3. Building new
  custom screens will make the designs visible app-wide.

### Commits (most recent first)
```
0ab5a7e37 Phase 2B fix: resolve 6 compile errors
88cd30bdd Phase 2B: implement Neon, Notebook, Brutalist designs (3 × 29 components + palettes)
1fda401dc Phase 2A fix 3: Material3Dialog AlertDialog API
6b1054b18 Phase 2A fix 2: FontVariation + move KutaAppearanceScreen to app
619baf2d0 Phase 2A fix: ms unit + missing imports in KutaTheme
6952cc881 Phase 2A: KutaTheme foundation — 4 design languages, 31 Kuta* components, settings UI
287baf049 Phase 2 design specs (coordinator-pushed)
... (earlier: Phase 1 branding/manga-gating/placeholder, Phase 1.5 docs, Phase 0 setup)
```

---

## How to get up and running (10 minutes)

### 1. Pull the repo

```sh
cd /home/z/kuta   # if it already exists from a prior session
git pull origin main
# OR clone fresh:
# cd ~ && git clone https://github.com/testplay-byte/kuta.git && cd kuta
```

Verify remotes: `git remote -v` → `origin` = testplay-byte/kuta, `upstream` = aniyomiorg/aniyomi.

### 2. Set up the Android toolchain (if not already)

A portable, non-root toolchain should already be at:
- `/home/z/jdk/jdk-21.0.11+10` (Temurin JDK 21)
- `/home/z/Android/Sdk/` (cmdline-tools + platform-tools)
- Sourced via `~/.android-env.sh` (JAVA_HOME, ANDROID_HOME, PATH)

If missing, see `SETUP/new-session-checklist.md` for the rebuild recipe, or
read `/home/z/my-project/MEMORY/01-environment/02-toolchain-setup.md` (if the
private memory folder exists).

Verify: `javac -version` → 21.0.11, `sdkmanager --version` → 16.0.

**You do NOT need to build locally.** CI is the build system. Local Gradle
is for `./gradlew tasks` / light checks only (and even that may OOM with 4GB
RAM / no swap — see environment notes).

### 3. Get the GitHub PAT

A fine-grained PAT for `testplay-byte/kuta` is needed to push. Ask the user
for it (or check if `/home/z/my-project/MEMORY/credentials/github-pat.txt`
exists from a prior session — it's gitignored).

If the PAT file exists:
```sh
PAT=$(grep -E '^github_pat_' /home/z/my-project/MEMORY/credentials/github-pat.txt | head -1)
git config --global credential.helper store
echo "https://testplay-byte:$PAT@github.com" > ~/.git-credentials
chmod 600 ~/.git-credentials
unset PAT
```

If not, ask the user. See `SETUP/pat-requirements.md` for scopes.

### 4. Set git identity (if not global already)

```sh
git config --global user.name "Kuta Coder"
git config --global user.email "testplay-byte@users.noreply.github.com"
```

### 5. Verify push access + CI

```sh
git push --dry-run origin main   # should succeed silently

# latest CI run
PAT=$(grep -E '^github_pat_' /home/z/my-project/MEMORY/credentials/github-pat.txt | head -1)
curl -s -H "Authorization: token $PAT" \
  "https://api.github.com/repos/testplay-byte/kuta/actions/runs?per_page=1" \
  | jq '.workflow_runs[0] | {status, conclusion, html_url}'
unset PAT
```

### 6. Read the context docs (in this order)

| Doc | What it tells you |
|-----|-------------------|
| `DOCS/design-system/00-shared-architecture.md` | THE architecture: KutaTheme, composition locals, Kuta* component layer, fallback strategy |
| `DOCS/design-system/01-neon.md` | Neon spec (colors, typography, components, effects) |
| `DOCS/design-system/02-notebook.md` | Notebook spec |
| `DOCS/design-system/03-brutalist.md` | Brutalist spec |
| `DOCS/design-system/04-material.md` | Material = existing M3, kept as-is |
| `FEATURES/README.md` | 92-feature inventory with status (keep/remove/modify/TBD) |
| `DIAGRAMS/README.md` | 7 Mermaid diagrams (nav flow, modules, data flow, DB schema, etc.) |
| `DOCS/architecture/README.md` | 12 architecture docs (modules, player, extensions, DB, DI, nav, etc.) |
| `SETUP/current-state.md` | Detailed current-state snapshot (what's done, what's next) |

### 7. Read the private memory (if it exists)

If `/home/z/my-project/MEMORY/` exists (it's gitignored, persists across
sessions in the same sandbox):
- `MEMORY/README.md` — entry point
- `MEMORY/01-environment/` — sandbox specs + toolchain setup
- `MEMORY/02-project/01-overview.md` — project understanding
- `MEMORY/03-aniyomi-reference/` — architecture headline facts
- `MEMORY/04-session-log/` — chronological session logs (read the latest)

If it doesn't exist, build it as you go.

---

## Where the code lives

### Design system (the Phase 2 deliverable)
```
presentation-core/src/main/java/tachiyomi/presentation/core/kuta/
├── theme/          KutaTheme, KutaColors, KutaTypography, KutaSpacing,
│                   KutaMotion, Locals, KutaFonts, AccentPresets,
│                   DesignLanguage, MaterialTypographyFallback
├── components/     KutaComponents (29 delegators) + KutaComponentApi (enums)
├── material/       Material3Components (29 M3 wrappers)
├── neon/           NeonPalette + NeonComponents (29, ~1692 lines)
├── notebook/       NotebookPalette + NotebookComponents (29, ~1454 lines)
├── brutalist/      BrutalistPalette + BrutalistComponents (29, ~1892 lines)
├── preferences/    KutaPreferences (PreferenceStore-backed)
└── effects/        Effects (7 Modifier extensions)
```

### App-level wiring
- `app/src/main/java/eu/kanade/tachiyomi/ui/main/MainActivity.kt` — wraps
  `Navigator` in `KutaTheme` (reads from `KutaPreferences`)
- `app/src/main/java/eu/kanade/presentation/more/settings/screen/KutaAppearanceScreen.kt`
  — the settings screen (uses `Kuta*` components → live reskins)
- `app/src/main/java/eu/kanade/tachiyomi/di/PreferenceModule.kt` — registers
  `KutaPreferences` in Injekt
- `app/src/main/java/eu/kanade/presentation/more/settings/screen/SettingsAppearanceScreen.kt`
  — added "Kuta Design" TextPreference entry → pushes KutaAppearanceScreen

### Phase 1 changes (still present)
- `app/src/main/java/eu/kanade/tachiyomi/ui/home/PlaceholderHomeScreen.kt` —
  the placeholder home (new file)
- `app/build.gradle.kts` — `applicationId = "app.kuta"`, `splits.abi` arm64-v8a only
- `NavStyle.kt`, `MoreTab.kt`, `HomeScreen.kt` — manga UI gating
- `i18n/.../strings.xml`, `i18n-aniyomi/.../strings.xml` — "Aniyomi" → "Kuta"

### Build config
- `app/build.gradle.kts` — applicationId `app.kuta`, namespace `eu.kanade.tachiyomi`
- `presentation-core/build.gradle.kts` — Haze 0.7.0 dependency
- `buildSrc/src/main/kotlin/mihon/buildlogic/AndroidConfig.kt` — compileSdk 35,
  minSdk 26, targetSdk 34, Java/Kotlin target 17

---

## How to build / get an APK

**CI is the build system.** Push to `main` → GitHub Actions runs
`./gradlew :app:assembleDebug --no-daemon` → uploads `app-arm64-v8a-debug.apk`
as an artifact.

1. Push your changes: `git push origin main`
2. Wait for CI (~2-4 min with warm cache): https://github.com/testplay-byte/kuta/actions
3. Download the APK from the latest green run's Artifacts section (`app-debug-apk`)

**Do NOT run `./gradlew assembleDebug` locally** — 4GB RAM / no swap → OOM.

---

## What's next (Phase 3)

**The user has decided**: we are NOT migrating Aniyomi's existing screens to
`Kuta*` components. We're building **entirely new custom screens** — new
layouts, new UX, new navigation — that use `Kuta*` components from the start.

The Aniyomi screens (library, browse, player, etc.) stay as-is and act as the
Material 3 fallback. Over time, new custom screens replace them.

The design feedback from the user (2026-07-05):
- Neon "doesn't feel neon enough" — because only the settings screen shows it
- Notebook "better but sections not themed"
- Brutalist "promising but needs improvements"
- The root cause: **only one screen uses `Kuta*` components**. Building new
  custom screens will make the designs visible app-wide.

**Next prompt will likely be**: start building the first new custom screen
(probably the new home/browse screen, since AniList is the front door in Phase 2
of the original plan). Wait for the user's specific instructions.

---

## Constraints (still in effect)

- **Every modified upstream file gets a `// FORK:` or `<!-- FORK: -->` marker.**
- **Don't change the applicationId** (`app.kuta` stays until Phase 5).
- **Don't change the `eu.kanade.tachiyomi` namespace** (deferred to Phase 5).
- **Don't delete existing M3 code** — `components/material/` and `TachiyomiTheme.kt`
  stay. Material is one of the 4 design options.
- **Don't try to build locally** — CI is the build system.
- **Don't touch the manga data layer** (DB tables, repos, source interfaces).
- **Push incrementally** — after each logical chunk, push and verify CI green.

---

## Quick reference

| Thing | Value / Location |
|-------|------------------|
| Repo | https://github.com/testplay-byte/kuta |
| Account | `testplay-byte` |
| Default branch | `main` |
| applicationId | `app.kuta` |
| namespace | `eu.kanade.tachiyomi` |
| compileSdk / minSdk / targetSdk | 35 / 26 / 34 |
| JDK | 21 (Temurin) |
| Gradle | 8.13 (wrapper) |
| Kotlin | 2.2.0 |
| AGP | 8.9.0 |
| Player | MPV (not Media3) |
| DB | SQLDelight 2.0.2 (two DBs: manga + anime) |
| DI | Injekt (Mihon fork) |
| Nav | Voyager 1.0.1 |
| CI | `.github/workflows/build.yml` (JDK 21, assembleDebug, upload APK) |
| APK artifact | `app-debug-apk` (arm64-v8a only) |
| Design system | `presentation-core/.../kuta/` |
| Design specs | `DOCS/design-system/00-04-*.md` |

---

## If something goes wrong

- **Can't push (401)**: PAT expired. Ask the user for a new one.
- **CI red**: read the error log (download from the run's job logs via API).
  Most common: compile errors from new code. Fix and push again.
- **Lost context**: re-read this file + `SETUP/current-state.md` + the latest
  session log in `MEMORY/04-session-log/`.
- **Sandbox reset**: re-clone the repo, rebuild the toolchain from
  `SETUP/new-session-checklist.md`, re-seed the PAT.

---

*This file is the single source of truth for "where are we." Keep it updated
at the end of every session.*
