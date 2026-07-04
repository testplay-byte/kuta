# Module Structure

This document describes the Gradle module layout of the **Kuta** fork (a fork of
[Aniyomi](https://github.com/aniyomiorg/aniyomi)). It enumerates every module
declared in `settings.gradle.kts`, gives each module's filesystem path and
purpose, and maps out the inter-module dependency graph.

> Source of truth: `/home/z/kuta/settings.gradle.kts` and each module's
> `build.gradle.kts`. All project dependency lines below were extracted directly
> from those files — none are inferred.

---

## 1. Module list

`settings.gradle.kts` declares 13 modules (one `include(...)` per line, lines 48–60).
The root project is named `Aniyomi` (line 47) — *not* renamed for the fork yet.

| # | Include name | Filesystem path | Android namespace | Purpose |
|---|---|---|---|---|
| 1 | `:app` | `app/` | `eu.kanade.tachiyomi` | The Android application shell. Houses all UI (Compose + legacy Views), the manga reader, the anime player (mpv), DI wiring, downloads, backup, trackers, extensions, etc. The only `com.android.application` module. |
| 2 | `:core-metadata` | `core-metadata/` | `tachiyomi.core.metadata` | Metadata parsing helpers for manga/anime entries. Depends on `:source-api`. |
| 3 | `:core:archive` | `core/archive/` | `mihon.core.archive` | Archive (CBZ/CBR/EPUB/zip/rar) extraction built on `libarchive`. Standalone — no project deps. |
| 4 | `:core:common` | `core/common/` | `eu.kanade.tachiyomi.core.common` | Shared core utilities: OkHttp stack, RxJava, preferences, JS engine (quickjs), FFmpeg-kit, image decoder, natural-comparator. The "everything an extension/source needs" bag. |
| 5 | `:data` | `data/` | `tachiyomi.data` | SQLDelight persistence layer. Defines two databases: `Database` (manga) and `AnimeDatabase` (anime). |
| 6 | `:domain` | `domain/` | `tachiyomi.domain` | Domain models, repository interfaces, and use-cases. Pure-Kotlin-ish business layer. |
| 7 | `:i18n` | `i18n/` | `tachiyomi.i18n` | Moko Multiplatform-Resources module holding the core string translations (`strings.xml` per locale under `src/commonMain/moko-resources/`). Generates `locales_config.xml` at build time. |
| 8 | `:i18n-aniyomi` | `i18n-aniyomi/` | `tachiyomi.i18n.aniyomi` | Aniyomi-specific (anime/player) translations, parallel to `:i18n`. Generated resource class is `AYMR` (see `i18n-aniyomi/build.gradle.kts:49`). |
| 9 | `:macrobenchmark` | `macrobenchmark/` | `tachiyomi.macrobenchmark` | `com.android.test` module that produces baseline profiles / macrobenchmarks against `:app` (`targetProjectPath = ":app"`). |
| 10 | `:presentation-core` | `presentation-core/` | `tachiyomi.presentation.core` | Reusable Jetpack Compose UI components (app bars, dialogs, sheets, components used across screens). |
| 11 | `:presentation-widget` | `presentation-widget/` | `tachiyomi.presentation.widget` | Home-screen app widgets built with Glance. |
| 12 | `:source-api` | `source-api/` | `eu.kanade.tachiyomi.source` | Kotlin Multiplatform contract module: `Source`, `AnimeSource`, `MangaSource`, DTOs, etc. Used by both the app and external extensions. |
| 13 | `:source-local` | `source-local/` | `tachiyomi.source.local` | Built-in "local source" that reads manga/anime from on-disk archives. |

### Multiplatform vs. Android-only

A few modules opt into Kotlin Multiplatform (`kotlin("multiplatform")`) and only
target `androidTarget()`:

- `:i18n`, `:i18n-aniyomi` — Moko resources (`alias(libs.plugins.moko)`)
- `:source-api` — KMP with `kotlin("plugin.serialization")`
- `:source-local` — KMP

All others use `kotlin("android")` and are pure Android library/application modules.

---

## 2. Inter-module dependency graph

Dependencies were read from each module's `build.gradle.kts`. Both `api(...)`
and `implementation(...)` project dependencies are listed; the kind is noted in
parentheses. `androidMain`-scoped dependencies (KMP modules) are tagged.

### `:app`  →  depends on 11 modules
Source: `app/build.gradle.kts:184–196`
```
implementation(projects.i18n)
implementation(projects.i18nAniyomi)
implementation(projects.core.archive)
implementation(projects.core.common)
implementation(projects.coreMetadata)
implementation(projects.sourceApi)
implementation(projects.sourceLocal)
implementation(projects.data)
implementation(projects.domain)
implementation(projects.presentationCore)
implementation(projects.presentationWidget)
```

### `:core-metadata`
Source: `core-metadata/build.gradle.kts:17`
```
implementation(projects.sourceApi)
```

### `:core:archive`
Source: `core/archive/build.gradle.kts` — no project dependencies.

### `:core:common`
Source: `core/common/build.gradle.kts:21`
```
implementation(projects.i18n)
```

### `:data`
Source: `data/build.gradle.kts:40–43`
```
implementation(projects.sourceApi)
implementation(projects.domain)
implementation(projects.core.common)
```

### `:domain`
Source: `domain/build.gradle.kts:23–24`
```
implementation(projects.sourceApi)
implementation(projects.core.common)
```

### `:i18n` and `:i18n-aniyomi`
No project dependencies (only `api(libs.moko.core)`).

### `:macrobenchmark`
No `implementation(project(...))` line, but it targets `:app` via the
`com.android.test` mechanism:
```kotlin
targetProjectPath = ":app"          // macrobenchmark/build.gradle.kts:24
```

### `:presentation-core`
Source: `presentation-core/build.gradle.kts:31–32`
```
api(projects.core.common)
api(projects.i18n)
```

### `:presentation-widget`
Source: `presentation-widget/build.gradle.kts:17–21`
```
implementation(projects.core.common)
implementation(projects.domain)
implementation(projects.presentationCore)
api(projects.i18n)
api(projects.i18nAniyomi)
```

### `:source-api`  (KMP; deps are in `androidMain`)
Source: `source-api/build.gradle.kts:23–31`
```
// androidMain only:
implementation(projects.core.common)
```

### `:source-local`  (KMP; deps split across commonMain / androidMain)
Source: `source-local/build.gradle.kts:11–30`
```
// commonMain:
implementation(projects.sourceApi)
api(projects.i18n)
api(projects.i18nAniyomi)

// androidMain:
implementation(projects.core.archive)
implementation(projects.core.common)
implementation(projects.coreMetadata)
implementation(projects.domain)
```

### ASCII diagram

```
                       ┌──────────────┐
                       │   :source-api │  (KMP contract; no project deps
                       └──────┬───────┘   in commonMain)
              ┌───────────────┼───────────────┐
              │               │               │
        ┌─────▼─────┐   ┌─────▼─────┐   ┌─────▼──────────┐
        │:core-meta  │   │ :domain   │   │ :source-local   │ (KMP)
        │  data      │   │           │   │  └─ androidMain:│
        └────────────┘   └─────┬─────┘   │    :core:archive,│
                               │         │    :core:common, │
                         ┌─────▼─────┐   │    :core-metadata│
                         │   :data   │   │    :domain       │
                         └─────┬─────┘   └──────────────────┘
                               │
        ┌──────────────────────┴──────────────────────┐
        │             :core:common                     │  ← (depends on :i18n)
        └──────────────────────┬──────────────────────┘
                               │
        ┌──────────────────────┼──────────────────────┐
        │                      │                      │
  ┌─────▼──────────┐   ┌──────▼────────┐    ┌─────────▼────────┐
  │:presentation-  │   │  :core:archive │    │      :i18n       │  ← leaf
  │  core          │   │   (leaf)       │    │      :i18n-      │
  │ api(:i18n) too │   └────────────────┘    │   aniyomi (leaf) │
  └─────┬──────────┘                         └──────────────────┘
        │
  ┌─────▼──────────┐
  │:presentation-  │  api(:i18n), api(:i18n-aniyomi),
  │  widget        │  impl(:core:common, :domain, :presentation-core)
  └────────────────┘

                         ┌──────────────────────────────────┐
                         │               :app               │
                         │  depends on ALL 11 non-benchmark │
                         │  modules (see list above)        │
                         └──────────────────────────────────┘
                                          ▲
                                          │ targetProjectPath
                                  ┌───────┴───────┐
                                  │:macrobenchmark│
                                  └───────────────┘
```

### Layering summary (top-down)

1. **Contract layer** — `:source-api` (KMP). Everything source-related compiles against it.
2. **Core utility layer** — `:core:common` (network, prefs, JS, FFmpeg), `:core:archive`, `:core-metadata`.
3. **Domain layer** — `:domain` (models/repos, depends on `:source-api` + `:core:common`).
4. **Data layer** — `:data` (SQLDelight; depends on `:domain`, `:source-api`, `:core:common`).
5. **Source impl layer** — `:source-local` (depends on nearly everything core).
6. **i18n layer** — `:i18n`, `:i18n-aniyomi` (leaves; consumed by app + presentation + source-local).
7. **Presentation layer** — `:presentation-core`, `:presentation-widget` (Compose components / widgets).
8. **App layer** — `:app` (the only application module; depends on all of the above).
9. **Test layer** — `:macrobenchmark` (`com.android.test`, targets `:app`).

### Cycles

No cyclic project dependencies were found. The graph is a DAG.

---

## 3. Composite builds (`includeBuild`)

**None.** A repo-wide search for `includeBuild` returned zero matches:

```
$ rg -n "includeBuild" /home/z/kuta --glob '!**/build/**'
(no matches)
```

All code lives in the single root Gradle build; `buildSrc/` is used (not a
composite build) for the convention plugins and helpers under
`mihon.buildlogic.*`. See `11-build-variants.md` for the full `buildSrc`
walkthrough.

---

## 4. Other settings-block notes

From `settings.gradle.kts`:

- **`pluginManagement`** (lines 1–16): maps any `com.android.library` /
  `com.android.application` plugin request to
  `com.android.tools.build:gradle:<version>` (the AGP module). Repositories:
  `gradlePluginPortal()`, `google()`, `mavenCentral()`, JitPack.
- **`dependencyResolutionManagement`** (lines 18–39): declares 4 *additional*
  version catalogs beyond the default `libs` (see below), sets
  `RepositoriesMode.FAIL_ON_PROJECT_REPOS`, and re-lists the same repos.
- **`TYPESAFE_PROJECT_ACCESSORS`** feature preview is enabled (line 45). This is
  why the `build.gradle.kts` files use `projects.core.common` /
  `projects.i18nAniyomi` etc. instead of `project(":core:common")`.
- **`foojay-resolver-convention`** plugin (line 42, v0.9.0) — auto-provisions
  JVM toolchains.

### Version catalogs in use

| Accessor name | TOML file | Defined in |
|---|---|---|
| `libs` (default) | `gradle/libs.versions.toml` | auto-loaded by Gradle |
| `kotlinx` | `gradle/kotlinx.versions.toml` | `settings.gradle.kts:20–22` |
| `androidx` | `gradle/androidx.versions.toml` | `settings.gradle.kts:23–25` |
| `compose` | `gradle/compose.versions.toml` | `settings.gradle.kts:26–28` |
| `aniyomilibs` | `gradle/aniyomi.versions.toml` | `settings.gradle.kts:29–31` |

Note: `buildSrc/settings.gradle.kts` *re-declares* all five catalogs (including
`libs`) so the `kotlin-dsl` buildSrc project can resolve them too.

---

## 5. Fork-specific observations

- The `applicationId` in `app/build.gradle.kts:20` is `app.kuta` (forked from
  Aniyomi's `xyz.jmir.tachiyomi.mi`), but the package namespace
  (`eu.kanade.tachiyomi`) and `rootProject.name = "Aniyomi"` are unchanged.
- `app/build.gradle.kts:93–101` contains a `// FORK:` marker restricting
  `splits.abi` to `arm64-v8a` only — documented in `11-build-variants.md`.
- No modules have been added or removed relative to upstream Aniyomi at the
  `settings.gradle.kts` level.
