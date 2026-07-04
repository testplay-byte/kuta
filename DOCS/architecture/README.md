# Architecture Docs — Aniyomi (Kuta fork)

Reference documentation for the Aniyomi codebase as it exists in this fork.
Generated 2026-07-04 by reading the source at `/home/z/kuta` (Aniyomi upstream
commit `2f5cf775c`, 2026-07-04). Each file is substantive — file paths, code
snippets, version numbers — so you can navigate the codebase without
re-discovering everything.

## Headline facts (read these first)

- **Player: MPV** (via `com.github.aniyomiorg:aniyomi-mpv-lib:1.18.n`), NOT
  Media3/ExoPlayer. Native `libmpv` + FFmpeg. See `06-player.md`.
- **Database: SQLDelight 2.0.2**, NOT Room. **Two** separate SQLite DBs:
  `tachiyomi.db` (manga) + `tachiyomi.animedb` (anime). See `08-database.md`.
- **DI: Injekt** (Mihon fork `com.github.mihonapp:injekt:91edab2317`), NOT
  Hilt/Koin. See `05-di.md`.
- **Navigation: Voyager 1.0.1** (single root Navigator + one TabNavigator in
  HomeScreen). See `04-navigation.md`.
- **Toolchain**: compileSdk **35**, minSdk **26**, targetSdk **34**, build-tools
  35.0.1, NDK 27.1.12297006, Java/Kotlin target **17**. No `jvmToolchain()` calls
  in the build, but Gradle's daemon auto-provisions JDK 17. See `11-build-variants.md`.
- **Modules**: 13 Gradle modules; `:app` is the only application module and
  depends on the other 11 (excluding `:macrobenchmark`). See `01-modules.md`.
- **Extensions**: loaded via a parent-last `ChildFirstPathClassLoader`
  (PathClassLoader subclass); manifest feature `tachiyomi.animeextension`; lib
  version range 12..16; SHA-256 signature trust. **No hard-coded default extension
  repo URL** in this fork — repos are user-added. See `07-extensions.md`.
- **Anime-only path**: Animiru (reference fork) used **physical removal** of manga
  code (not build flags), and is ~6 months stale vs current Aniyomi. See
  `12-animiru-diff.md`.

## Index

| File | Topic |
|------|-------|
| [01-modules.md](01-modules.md) | Module structure + dependency graph |
| [02-package-tree.md](02-package-tree.md) | Top 3 levels of packages under `eu.kanade.tachiyomi` |
| [03-entry-points.md](03-entry-points.md) | `App` (Application) + `MainActivity` startup |
| [04-navigation.md](04-navigation.md) | Voyager nav, root graph, 6 top-level tabs |
| [05-di.md](05-di.md) | Injekt DI, modules, how to add a binding |
| [06-player.md](06-player.md) | MPV player, PlayerActivity, Video model, data flow |
| [07-extensions.md](07-extensions.md) | AnimeSource interface, extension loading, repo format |
| [08-database.md](08-database.md) | SQLDelight, 2 DBs, 18 tables, schema versions 32/135, `.sqm` migrations |
| [09-preferences.md](09-preferences.md) | PreferenceStore abstraction, ~17 feature `*Preferences` classes |
| [10-data-models.md](10-data-models.md) | Anime, Episode, Manga, Chapter, Track, History, Category, Download |
| [11-build-variants.md](11-build-variants.md) | build types, flavors (none), buildSrc conventions, resolved SDK |
| [12-animiru-diff.md](12-animiru-diff.md) | How Animiru stripped manga (physical removal, ~6mo stale) |

## Conventions

- `// FORK:` marker in source = our modification to an upstream file (for
  upstream-sync visibility).
- Anime ↔ manga parallelism is exhaustive across the codebase (separate DBs,
  separate Source interfaces, parallel domain models). Going anime-only means
  gating/removing manga UI entry points; the data layer can stay intact.

## Module map (quick)

```
:app  ──depends on──> :core:common, :core:archive, :core-metadata, :data, :domain,
                       :i18n, :i18n-aniyomi, :source-api, :source-local,
                       :presentation-core, :presentation-widget
:macrobenchmark ──targets──> :app  (com.android.test)
```
