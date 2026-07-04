# 12 — Animiru vs Aniyomi (Kuta) Diff: How Animiru Stripped Manga

> Reference research only. Animiru (https://github.com/Quickdesh/Animiru) is an
> anime-only fork of Aniyomi maintained by another team (Quickdev / Secozzi).
> We are **not** forking Animiru. This document records how they became anime-only,
> so our own fork ("Kuta") can learn from — but not copy — their approach.
>
> Animiru was inspected as a `--depth 1` clone at `/home/z/animiru`. Baseline is
> our Kuta checkout at `/home/z/kuta` (Aniyomi upstream commit `2f5cf775c`,
> 2026-07-04, plus a few Kuta-specific build/CI tweaks which are ignored here).

## TL;DR

- **Physical removal, not build flags.** Animiru deleted every `manga/` source
  directory, every manga SQLdelight table, the manga source-API, the manga
  extension subsystem, and the manga-only trackers. There is no
  `enableManga`/`isAnimeOnly` flag anywhere; the only `Config` flags are
  `enableUpdater`, `enableCodeShrink`, `includeDependencyInfo`.
- **No whole Gradle modules were deleted** (Animiru still ships `app`, `data`,
  `domain`, `source-api`, `source-local`, `i18n`, `i18n-aniyomi`,
  `presentation-*`, `core*`, `macrobenchmark`). The manga code lived *inside*
  the shared modules and was surgically stripped from each. Animiru *adds* one
  module: `i18n-animiru` (their custom anime-only strings, `AMMR`).
- **Animiru is on an older Aniyomi base.** Last "Merged from Aniyomi" entry in
  their CHANGELOG is **v0.19.3.0 (2025-12-25)**. Since then they only "Merge
  from Mihon" (the manga-only upstream), rejecting manga-side changes by hand.
  Their latest release is **v0.19.7.7 (2026-06-24, commit `f562649`)** — about
  6 months stale vs our Kuta/Aniyomi base, and ~10 days behind by wall clock.
- Because they forked before Aniyomi's recent "Entry" refactor, Animiru still
  uses the older non-split class names (`AnimeScreen`, `BrowseSourceScreen`,
  `LibraryTab`, `Episode`) at flat paths (`ui/anime/`, `ui/library/`), whereas
  current Aniyomi split everything into `anime/`+`manga/` siblings under
  `ui/entries/`, `ui/library/`, `ui/browse/`, etc.

---

## 1. Modules / folders removed entirely

### Top-level Gradle modules

Comparing `settings.gradle.kts` `include(...)` lists:

| Module            | Aniyomi (Kuta) | Animiru | Notes |
|-------------------|:--------------:|:-------:|-------|
| `app`             | ✅ | ✅ | |
| `core:archive`, `core:common` | ✅ | ✅ | |
| `core-metadata`   | ✅ | ✅ | |
| `data`            | ✅ | ✅ | manga tables/code stripped from inside |
| `domain`          | ✅ | ✅ | manga domain stripped from inside |
| `source-api`      | ✅ | ✅ | manga source API stripped from inside |
| `source-local`    | ✅ | ✅ | manga local source stripped from inside |
| `i18n`            | ✅ | ✅ | `app_name = "Animiru"` lives here |
| `i18n-aniyomi`    | ✅ | ✅ | inherited AYMR strings, unchanged |
| **`i18n-animiru`** | ❌ | ✅ | **Animiru-only** — custom `AMMR` strings |
| `macrobenchmark`  | ✅ | ✅ | |
| `presentation-core`, `presentation-widget` | ✅ | ✅ | |

**No manga-specific Gradle module exists in either repo.** Manga code is
interleaved inside the shared modules, so Animiru's removal had to be
intra-module, not "delete a module".

Kuta additionally has top-level `buildSrc/` (auto-included) and `fastlane/`;
Animiru instead uses `includeBuild("gradle/build-logic")` (composite build,
newer Mihon-style infra). This is an infra difference, not a manga difference.

### `manga/` source directories

The single most telling structural fact. In Kuta (Aniyomi) there are **28
`manga/` directories** under `app/src/main/java` alone; in Animiru there are
**zero** (`find ... -type d -name manga` returns 28 vs 0):

```
# Kuta (Aniyomi) — manga dirs in app module
mihon/feature/upcoming/manga
eu/kanade/domain/download/manga
eu/kanade/domain/entries/manga
eu/kanade/domain/source/manga
eu/kanade/domain/track/manga
eu/kanade/domain/extension/manga
eu/kanade/presentation/entries/manga
eu/kanade/presentation/browse/manga
eu/kanade/presentation/history/manga
eu/kanade/presentation/updates/manga
eu/kanade/presentation/track/manga
eu/kanade/presentation/library/manga
eu/kanade/tachiyomi/ui/deeplink/manga
eu/kanade/tachiyomi/ui/stats/manga
eu/kanade/tachiyomi/ui/download/manga
eu/kanade/tachiyomi/ui/entries/manga
eu/kanade/tachiyomi/ui/storage/manga
eu/kanade/tachiyomi/ui/category/manga
eu/kanade/tachiyomi/ui/browse/manga
eu/kanade/tachiyomi/ui/browse/manga/migration/manga
eu/kanade/tachiyomi/ui/history/manga
eu/kanade/tachiyomi/ui/updates/manga
eu/kanade/tachiyomi/ui/library/manga
eu/kanade/tachiyomi/data/database/models/manga
eu/kanade/tachiyomi/data/download/manga
eu/kanade/tachiyomi/data/library/manga
eu/kanade/tachiyomi/source/manga
eu/kanade/tachiyomi/extension/manga
... (28 total)

# Animiru — manga dirs in app module
(none)
```

The same is true in the library modules:

- **`source-api`**: Kuta has both `animesource/` and `source/` (the latter is
  the manga source API: `MangaSource.kt`, `CatalogueSource.kt`, `SManga.kt`,
  `SChapter.kt`, `MangasPage.kt`, `HttpSource.kt`, `ParsedHttpSource.kt`).
  Animiru has **only `animesource/`** — the entire `source/` package is gone.
- **`source-local`**: Kuta splits into `entries/manga/`, `entries/anime/`,
  `image/manga/`, `image/anime/`, `io/manga/`, `io/anime/`, `filter/manga/`,
  `filter/anime/`, plus `metadata/EpubReaderExtensions.kt` (EPUB = manga).
  Animiru has only the anime equivalents and no EPUB reader.
- **`data` module (SQLdelight)**: Kuta defines `mangas.sq`, `chapters.sq`,
  `manga_sync.sq`, `mangas_categories.sq` alongside the anime `animes.sq`,
  `episodes.sq`, `anime_sync.sq`, `animes_categories.sq`. Animiru has **only
  the anime `.sq` files** — the manga schema is physically absent from the DB.
- **`data` module (Kotlin)**: Kuta has `data/handlers/manga/`,
  `data/entries/manga/`, `data/category/manga/`, `data/history/manga/`,
  `data/updates/manga/`, `data/source/manga/`, `data/track/manga/`,
  `data/items/chapter/`. Animiru has none of these — only the anime side
  (`data/episode/`, `data/anime/`, `data/history/`, `data/updates/`,
  `data/source/`, `data/track/`, `data/category/`).
- **`domain` module**: Kuta has `mihon/domain/extensionrepo/manga/`,
  `mihon/domain/upcoming/manga/`, `mihon/domain/items/chapter/`. Animiru has
  only `extensionrepo/` (flat, anime-only), `upcoming/` (flat), `episode/` —
  no `items/chapter`, no manga anywhere.

### Custom Animiru package

Animiru adds one small custom package at the app root:

```
app/src/main/java/animiru/feature/mpvfiles/MpvConfig.kt
```

This is the only file under the `animiru.` package. The bulk of Animiru's
custom code lives under the standard `eu.kanade.*` / `tachiyomi.*` packages
and is delimited by `// AM -->` / `// <-- AM` comment markers (and
`// AM (FEATURE_NAME) -->` for feature-tagged sections).

### Package-structure divergence (a side effect of staleness, not of stripping)

Animiru forked Aniyomi *before* the recent "Entry" refactor. So even on the
anime side the paths differ:

| Concept | Kuta (current Aniyomi) | Animiru (older Aniyomi base) |
|---|---|---|
| Anime detail screen | `ui/entries/anime/AnimeScreen.kt` | `ui/anime/AnimeScreen.kt` |
| Browse source screen | `BrowseAnimeSourceScreen.kt` (under `presentation/browse/anime/`) | `BrowseSourceScreen.kt` (under `presentation/browse/`) |
| Global search | `GlobalAnimeSearchScreen.kt` | `GlobalSearchScreen.kt` |
| Deep link | `DeepLinkAnimeScreen.kt` | `DeepLinkScreen.kt` |
| Library tab | `ui/library/anime/AnimeLibraryTab.kt` | `ui/library/LibraryTab.kt` |
| Library update job | `data/library/anime/AnimeLibraryUpdateJob.kt` | `data/library/LibraryUpdateJob.kt` |
| Incognito interactor | `domain/source/anime/interactor/GetAnimeIncognitoState.kt` (+ `GetMangaIncognitoState.kt`) | `domain/source/interactor/GetIncognitoState.kt` (single) |

In other words: Animiru never adopted the `Anime*`/`Manga*` prefix split,
because they removed the manga side before Aniyomi introduced the split. They
kept the older unprefixed names and the older flat paths, and the anime side
became "the only side" by default.

---

## 2. How manga UI entry points were disabled

**Animiru does not "disable" manga UI — it was never built.** There are no
gating flags, no commented-out tabs, no `if (enableManga)` branches. Every
manga entry point was deleted at the source. Concrete evidence from the
navigation layer:

### 2.1 `HomeScreen.kt` — bottom-nav tab list

Animiru (`ui/home/HomeScreen.kt`) hard-codes a 4-tab list with a single
anime-only library:

```kotlin
private val TABS = listOf(
    LibraryTab,
    // AM (RECENTS) -->
    RecentsTab,
    // <-- AM (RECENTS)
    // AM (BROWSE) -->
    BrowseTab,
    // <-- AM (BROWSE)
    MoreTab,
)
```

Kuta (Aniyomi) instead builds the tab list from `uiPreferences.navStyle().get().tabs`,
whose default set includes both `AnimeLibraryTab` and `MangaLibraryTab`, plus
`UpdatesTab`, `HistoriesTab`, `BrowseTab`, `MoreTab` (see `NavStyle.kt`,
which **does not exist in Animiru**). Kuta's `HomeScreen.Tab` sealed
interface has both `AnimeLib` and `Library` (manga):

```kotlin
// Kuta HomeScreen.Tab
sealed interface Tab {
    data class AnimeLib(val animeIdToOpen: Long? = null) : Tab
    data class Library(val mangaIdToOpen: Long? = null) : Tab   // ← manga
    data object Updates : Tab
    data object History : Tab
    data class Browse(val toExtensions: Boolean = false, val anime: Boolean = false) : Tab
    data class More(val toDownloads: Boolean) : Tab
}
```

```kotlin
// Animiru HomeScreen.Tab — no manga variant
sealed interface Tab {
    data class Library(val animeIdToOpen: Long? = null) : Tab
    data class Recents(val toHistory: Boolean) : Tab
    data class Browse(val toExtensions: Boolean = false) : Tab
    data class More(val toDownloads: Boolean) : Tab
}
```

### 2.2 `StartScreen.kt` — start-screen preference enum

```kotlin
// Kuta (Aniyomi) — has MANGA option
enum class StartScreen(val titleRes: StringResource, val tab: Tab) {
    ANIME(AYMR.strings.label_anime, AnimeLibraryTab),
    MANGA(AYMR.strings.manga, MangaLibraryTab),   // ← absent in Animiru
    UPDATES(MR.strings.label_recent_updates, UpdatesTab),
    HISTORY(MR.strings.label_recent_manga, HistoriesTab),
    BROWSE(MR.strings.browse, BrowseTab),
}

// Animiru — MANGA option removed; ANIME re-labelled as plain "Library"
enum class StartScreen(val titleRes: StringResource, val tab: HomeScreen.Tab) {
    ANIME(MR.strings.label_library, HomeScreen.Tab.Library()),
    UPDATES(MR.strings.label_recent_updates, HomeScreen.Tab.Recents(toHistory = false)),
    HISTORY(MR.strings.label_recent_manga, HomeScreen.Tab.Recents(toHistory = true)),
    BROWSE(MR.strings.browse, HomeScreen.Tab.Browse(toExtensions = false)),
}
```

Note Animiru uses `MR.strings.label_library` ("Library") for the anime tab
because there is no longer a manga library to disambiguate from. Kuta uses
`AYMR.strings.label_anime` ("Anime") because manga also exists.

### 2.3 `LibraryTab.kt` — single anime-only library

Animiru ships one `LibraryTab` (at `ui/library/LibraryTab.kt`) that imports
exclusively anime types:

```kotlin
import eu.kanade.presentation.anime.components.LibraryBottomActionMenu
import eu.kanade.presentation.library.DeleteLibraryAnimeDialog
import eu.kanade.presentation.library.LibrarySettingsDialog
import eu.kanade.presentation.library.components.LibraryContent
import eu.kanade.presentation.library.components.LibraryToolbar
import eu.kanade.tachiyomi.ui.anime.AnimeScreen
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.episode.model.Episode
import tachiyomi.domain.library.model.LibraryAnime
// ...no Manga/Chapter/MangaLibrary equivalents
```

Kuta splits this into two sibling tabs at `ui/library/anime/AnimeLibraryTab.kt`
and `ui/library/manga/MangaLibraryTab.kt`, each importing the corresponding
`Manga*`/`Anime*` types.

### 2.4 `BrowseTab.kt` — single source/extension set

Animiru's `BrowseTab` has a single set of screens
(`SourcesScreen`, `ExtensionScreen`, `BrowseSourceScreen`,
`GlobalSearchScreen`, `MigrateSourceScreen`) — no inner anime/manga tab split.

Kuta's `BrowseTab` is a `TabbedScreen` with paired sub-tabs:
`animeSourcesTab()` + `mangaSourcesTab()`, `animeExtensionsTab()` +
`mangaExtensionsTab()`, `migrateAnimeSourceTab` + `migrateMangaSourceTab`.

### 2.5 `MainActivity.kt` — imports tell the story

```kotlin
// Animiru MainActivity imports (single set, anime-only)
import eu.kanade.domain.source.interactor.GetIncognitoState
import eu.kanade.tachiyomi.ui.anime.AnimeScreen
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreen
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.deeplink.DeepLinkScreen
import eu.kanade.presentation.more.settings.screen.browse.ExtensionReposScreen
import eu.kanade.tachiyomi.extension.api.ExtensionApi   // single API

// Kuta MainActivity imports (doubled, anime + manga)
import eu.kanade.domain.source.anime.interactor.GetAnimeIncognitoState
import eu.kanade.domain.source.manga.interactor.GetMangaIncognitoState
import eu.kanade.tachiyomi.ui.entries.anime.AnimeScreen
import eu.kanade.tachiyomi.ui.entries.manga.MangaScreen
import eu.kanade.tachiyomi.ui.browse.anime.source.browse.BrowseAnimeSourceScreen
import eu.kanade.tachiyomi.ui.browse.manga.source.browse.BrowseMangaSourceScreen
import eu.kanade.tachiyomi.ui.browse.anime.source.globalsearch.GlobalAnimeSearchScreen
import eu.kanade.tachiyomi.ui.browse.manga.source.globalsearch.GlobalMangaSearchScreen
import eu.kanade.tachiyomi.ui.deeplink.anime.DeepLinkAnimeScreen
import eu.kanade.tachiyomi.ui.deeplink.manga.DeepLinkMangaScreen
import eu.kanade.presentation.more.settings.screen.browse.AnimeExtensionReposScreen
import eu.kanade.presentation.more.settings.screen.browse.MangaExtensionReposScreen
import eu.kanade.tachiyomi.extension.anime.api.AnimeExtensionApi
import eu.kanade.tachiyomi.extension.manga.api.MangaExtensionApi
import eu.kanade.tachiyomi.data.cache.ChapterCache                  // manga-only cache
import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadCache
import eu.kanade.tachiyomi.data.download.manga.MangaDownloadCache
```

### 2.6 `AndroidManifest.xml` — extension install service

Kuta declares **both** extension install services:

```xml
<service android:name=".extension.manga.util.MangaExtensionInstallService" ... />
<service android:name=".extension.anime.util.AnimeExtensionInstallService" ... />
```

Animiru declares **only** the anime one — `MangaExtensionInstallService` is
gone (and the entire `extension/manga/` package that would supply it is gone):

```xml
<service android:name=".extension.util.ExtensionInstallService" ... />
```

(Animiru uses the older unprefixed name `ExtensionInstallService`.)

### 2.7 Recents combined tab (Animiru custom)

Where Kuta has separate `UpdatesTab` (`ui/updates/UpdatesTab.kt`) and
`HistoriesTab` (`ui/history/HistoriesTab.kt`), Animiru merges them into one
custom `RecentsTab` (`ui/recents/RecentsTab.kt`) that can flip between
"Updates" and "History" via filter chips. This is an Animiru UX feature, not a
manga-stripping measure, but it does halve the bottom-nav slot count that
would otherwise have been needed.

---

## 3. Build flags vs physical removal

**Verdict: physical removal.** No compile-time flag, no run-time flag, no
gated code path. Evidence:

### 3.1 No manga flag in the build system

Animiru's only build-config switch is `mihon.gradle.BuildConfig`
(`gradle/build-logic/src/main/kotlin/mihon/gradle/BuildConfig.kt`):

```kotlin
interface BuildConfig {
    val enableUpdater: Boolean
    val enableCodeShrink: Boolean
    val includeDependencyInfo: Boolean
}

val Project.Config: BuildConfig get() = object : BuildConfig {
    override val enableUpdater = project.hasProperty("enable-updater")
    override val enableCodeShrink = !project.hasProperty("disable-code-shrink")
    override val includeDependencyInfo = project.hasProperty("include-dependency-info")
}
```

`app/build.gradle.kts` only consumes `Config.enableUpdater` and
`Config.enableCodeShrink`. There is no `enableManga`/`enableAnime`/
`isAnimeOnly`/`MANGA_ENABLED` flag. Searching the whole Animiru tree for
those patterns returns nothing relevant (the single hit for `enableManga` was
a substring false-positive in `MigrationConfigScreen.kt`).

### 3.2 The manga code is genuinely absent

`find ... -type d -name manga` under `app/src/main/java` returns **28 hits in
Kuta and 0 in Animiru** (see §1). Equivalent absence holds in `source-api`,
`source-local`, `domain`, `data` (both Kotlin and SQLdelight). The manga
source-API package `eu.kanade.tachiyomi.source.*` (which holds
`MangaSource`, `CatalogueSource`, `SManga`, `SChapter`, `MangasPage`,
`HttpSource`, `ParsedHttpSource`) simply does not exist in Animiru's
`source-api` module.

### 3.3 The 18 files that still mention "manga" are not active manga code

A case-insensitive `rg -il "manga"` over Animiru's `app/src/main/java` returns
18 files. Inspecting each, they fall into two benign categories:

**(a) Backward-compatibility preference migrations** — code that renames old
manga-named keys to the new anime-named keys for users upgrading from
Aniyomi→Animiru:

```kotlin
// mihon/core/migration/migrations/RenameKeysMigration.kt
listOf("pref_mangasync_username_", "pref_mangasync_password_").forEach { ... 
    val newKey = oldKey.replace("mangasync", "animesync")
}
"pref_auto_update_manga_on_mark_read" to "pref_auto_update_anime_on_mark_seen",
"pref_auto_update_manga_sync_key"    to "pref_auto_update_anime_sync_key",
"library_update_manga_restriction"   to "library_update_anime_restriction",
```

```kotlin
// RenameEnumMigration.kt
"LAST_CHECKED" -> "LAST_MANGA_UPDATE"
```

**(b) Localization key reuse** — the i18n string IDs still contain "manga" as
a legacy key name, but they label anime-side UI:

```kotlin
// BackupOptions.kt — libraryEntries option labelled with MR.strings.manga
Entry(label = MR.strings.manga, getter = BackupOptions::libraryEntries, ...)

// LibraryToolbar.kt
title = stringResource(MR.strings.action_open_random_manga)

// DiscordRPCModels.kt
HISTORY(MR.strings.label_recent_manga, AMMR.strings.scrolling, HISTORY_IMAGE_URL)
```

The `MR.strings.*` keys themselves still exist in `i18n/.../strings.xml` for
translation stability, but no manga data model or screen is wired to them.

### 3.4 Tracker pruning (further evidence of surgical removal)

Kuta's `data/track/` ships 11 trackers: `anilist`, `myanimelist`, `shikimori`,
`bangumi`, `kitsu`, `simkl`, `jellyfin`, **`mangaupdates`**, **`kavita`**,
**`suwayomi`**, **`komga`**. The last four are manga-only / manga-first
services.

Animiru's `data/track/` ships only 7: `anilist`, `myanimelist`, `shikimori`,
`bangumi`, `kitsu`, `simkl`, `jellyfin` — the four manga-oriented trackers
were physically deleted.

---

## 4. How recent is Animiru's last sync with Aniyomi?

### 4.1 Latest commit (from the depth-1 clone)

```
$ git -C /home/z/animiru log -1 --format='%H %ci %s'
f56264978a969136d61b7e94b6fa3eb06b9dc014  2026-06-24 14:12:25 +0200  Release v0.19.7.7
```

The depth-1 clone exposes only this single commit on `animiru-new-main`
(tag `v0.19.7.7` → `0eedf078ce0f7f41f3d335ef652aa047562d1db9`). No deeper
history is available locally.

### 4.2 Sync history reconstructed from `CHANGELOG.md`

| Version | Date | Upstream merge |
|---|---|---|
| v0.17.2.0 | 2024-07-27 | (older) |
| v0.19.0.0 | 2025-12-24 | **Merged from Aniyomi and Mihon** (#102, #110) |
| v0.19.3.0 | 2025-12-25 | **Merged from Aniyomi and Mihon** (#115) ← last Aniyomi merge |
| v0.19.4.0 | 2026-02-26 | Merged from Mihon (#131) |
| v0.19.7.0 | 2026-03-30 | Merged from Mihon (#136) |
| v0.19.7.4 | 2026-05-27 | Merge from Mihon (#155) |
| v0.19.7.7 | 2026-06-24 | (latest; bugfix-only release) |

### 4.3 Inferred staleness

- **Last "Merged from Aniyomi": 2025-12-25** (v0.19.3.0, PR #115). Every
  release since has merged only from **Mihon** (the manga-only upstream that
  Aniyomi itself forked from). Animiru pulls shared infrastructure (build
  system, migration framework, backup, SQLdelight schema plumbing) from Mihon
  and surgically discards manga-side changes.
- Relative to our Kuta base (Aniyomi commit `2f5cf775c`, 2026-07-04):
  - By Aniyomi-sync date: **~6 months stale** (2025-12-25 → 2026-07-04).
  - By wall-clock release date: **~10 days behind** (2026-06-24 → 2026-07-04).
- Concrete structural proof of the gap: Animiru is missing several post-Dec-2025
  Aniyomi refactors — most visibly the "Entry" abstraction (no `ui/entries/`,
  no `AnimeScreen` under `entries/anime/`) and the `NavStyle` preference
  (no `domain/ui/model/NavStyle.kt`).

### 4.4 Why "Merge from Mihon" doesn't re-introduce manga

This is the key trick that keeps Animiru anime-only without diverging
unbounded from upstream: Animiru's maintainer hand-curates each Mihon merge,
accepting only shared/non-manga changes (build tooling, migration helpers,
backup format, SQLdelight plumbing, preference infra) and dropping anything
that touches `mangas.sq`/`chapters.sq`/`SManga`/`SChapter`/manga source API/
manga extension API. The CHANGELOG credits `@Secozzi` for these merges, and
the `// AM -->` markers throughout the code denote places where Animiru
deviates from the upstream they merged from.

---

## 5. Other notable differences (relevant to an anime-only fork)

### 5.1 Branding / identity

- **App name**: `Animiru` — defined in `i18n/src/commonMain/moko-resources/base/strings.xml`
  as `<string name="app_name" translatable="false">Animiru</string>`.
- **applicationId**: `xyz.Quickdev.Animiru.mi` (note the `.mi` suffix — a
  variant tag). Debug suffix `.dev`, preview suffix `.debug`, benchmark
  suffix `.benchmark`.
- **namespace**: `eu.kanade.tachiyomi` (unchanged from Aniyomi — package
  roots are not renamed).
- **Version**: `versionCode = 143`, `versionName = "0.19.7.7"`.
- **Updater**: gated behind `Config.enableUpdater` (Gradle property
  `enable-updater`), off by default — same pattern as upstream.

### 5.2 Storage permission

Animiru's manifest requests `WRITE_EXTERNAL_STORAGE`; Kuta (current Aniyomi)
requests `MANAGE_EXTERNAL_STORAGE`. This is an upstream Aniyomi change that
Animiru has not adopted (possibly deliberately, for installability / scope).

### 5.3 Custom i18n module (`i18n-animiru`, `AMMR`)

Animiru adds a third string-resources module with **197 custom strings**,
all prefixed `am.` (e.g. `am.action_mark_as_seen`, `am.pref_auto_update_anime_on_mark_seen`,
`am.pref_remove_after_seen`). These reword the UI in anime terms:
"episode" (not "chapter"), "seen" (not "read"), "watching" (not "reading").
The existing `i18n-aniyomi` (`AYMR`) module is inherited unchanged for
translation continuity.

Marker convention used throughout the codebase:
- `// AM -->` / `// <-- AM` — Animiru custom code.
- `// AM (FEATURE) -->` / `// <-- AM (FEATURE)` — feature-tagged Animiru code
  (e.g. `RECENTS`, `BROWSE`, `DISCORD_RPC`, `CONNECTION`, `CUSTOM_INFORMATION`).
- `// AY -->` / `// <-- AY` — Aniyomi-inherited code carried forward.

### 5.4 Custom features Animiru layered on top of the anime-only base

| Feature | Location | Notes |
|---|---|---|
| **Discord Rich Presence** | `data/connection/discord/`, `domain/connection/`, `presentation/connection/`, `SettingsDiscordScreen.kt` | Foreground service `DiscordRPCService`; uses token-based login; manifest registers a Samsung-account OAuth callback URL for it. |
| **Syncmiru cloud sync** | `data/connection/syncmiru/`, `SettingsSyncmiruScreen.kt`, `SyncSettingsSelector.kt`, `SyncTriggerOptionsScreen.kt` | Google Drive sync of library/history (CHANGELOG mentions "Fix Google drive sync"). |
| **Combined Recents tab** | `ui/recents/RecentsTab.kt`, `RecentsTab.kt` filter chips | Replaces separate Updates + History tabs. |
| **Custom nav pill** | `NavigationPill(...)` in `HomeScreen.kt` | Replaces the standard Material3 `NavigationBar`/`NavigationRail` with a label-fading pill. |
| **Storage usage screen** | `presentation/more/storage/` | Per-category storage breakdown. |
| **Catppuccin theme** | `presentation/theme/colorscheme/CatppuccinColorScheme.kt` | Extra color scheme option. |
| **MPV config file copy** | `animiru/feature/mpvfiles/MpvConfig.kt` | Copies user `mpv.conf` into place on app resume. |
| **Fillermark** | i18n: `action_filter_fillermarked`, `action_fillermark_episode` | Mark episodes as filler; optionally block filler downloads. |
| **Seasons support (enhanced trackers)** | `animeseasonsView.sq`, `animeseasonstatsView.sq`, "smart sync for seasons" | AniList/Shikimori/etc. season-aware tracking. |
| **Custom buttons** | `custom_buttons.sq`, `data/custombutton/`, `BackupOptions.customButton` | User-defined action buttons in player/library. |
| **Custom entry info edit** | i18n: `am.action_edit_info`, `custom_entry_info` | Edit title/description/author/artist locally. |

### 5.5 Different mpv library

| | Aniyomi (Kuta) | Animiru |
|---|---|---|
| Coordinate | `com.github.aniyomiorg:aniyomi-mpv-lib:1.18.n` | `io.github.secozzi:mpv-android-lib:0.1.14` |
| ffmpeg-kit | `com.github.jmir1:ffmpeg-kit:1.18` | same |

Animiru uses Secozzi's personal mpv-lib fork (Secozzi is a core Animiru
contributor), not the aniyomiorg-published artifact.

### 5.6 Build infrastructure (Mihon-style composite build)

Animiru's `settings.gradle.kts` uses:

```kotlin
pluginManagement { includeBuild("gradle/build-logic") }
dependencyResolutionManagement {
    versionCatalogs {
        create("mihonx") { from(files("gradle/mihon.versions.toml")) }
        create("aniyomilibs") { from(files("gradle/aniyomi.versions.toml")) }
    }
}
```

Plugin aliases look like `alias(mihonx.plugins.android.application)`. Kuta
still uses the older `buildSrc/` + per-library catalogs (`kotlinx`,
`androidx`, `compose`, `aniyomilibs`). This is a Mihon infra upgrade Animiru
picked up via their Mihon merges; it is unrelated to manga stripping but
matters if we ever rebase onto Animiru-style infra.

### 5.7 Samsung account OAuth in manifest

Animiru's `AndroidManifest.xml` registers an `android:scheme="https"
android:host="account.samsung.com" android:path="/accounts/oauth/callback"`
intent filter on both an activity and `DiscordRPCService`. This is part of
the Discord RPC login flow and has nothing to do with manga.

---

## Implications for Kuta

1. **Animiru's approach is "fork once, never re-merge the manga side".** They
   stripped manga physically at a single point in time (the v0.19.3.0 sync,
   2025-12-25) and have since refused all manga-touching upstream changes.
   This is sustainable only because one maintainer hand-curates every merge.
2. **Physical removal is cleaner at compile time** (no dead code, smaller APK,
   no risk of manga strings leaking into UI) but **much more expensive to
   maintain**: every upstream Aniyomi refactor that touches the anime/manga
   split (like the recent "Entry" abstraction, `NavStyle`, etc.) requires
   manual conflict resolution to avoid re-introducing manga.
3. **A build-flag approach (`enableManga`) would be easier to maintain** but
   Animiru's experience shows it is not necessary for a working anime-only
   app — and physical removal gives stronger guarantees (no manga code paths
   reachable at all).
4. **If Kuta goes anime-only**, the highest-value targets for removal
   (mirroring Animiru) are, in order of impact:
   - `app/src/main/java/**/manga/` (28 directories) — UI, screens, presenters.
   - `source-api/.../source/` package — manga source API (`MangaSource`,
     `SManga`, `SChapter`, `HttpSource`, `ParsedHttpSource`).
   - `source-local/.../{entries,image,io,filter}/manga/` + `metadata/EpubReaderExtensions.kt`.
   - `data/src/main/sqldelight/data/{mangas,chapters,manga_sync,mangas_categories}.sq`.
   - `data/.../{handlers,entries,category,history,updates,source,track,items}/manga/` (+ `items/chapter/`).
   - `domain/.../{extensionrepo/upcoming/items}/manga/` + `items/chapter/`.
   - `app/.../extension/manga/` + `AndroidManifest.xml` `MangaExtensionInstallService`.
   - Manga-only trackers: `data/track/{mangaupdates,kavita,suwayomi,komga}/`.
   - `StartScreen.MANGA`, `HomeScreen.Tab.Library` (manga variant),
     `MangaLibraryTab`, `MangaExtensionApi`, `MangaDownloadCache`, `ChapterCache`.
5. **Don't copy Animiru's class names or paths.** Their `AnimeScreen` /
   `BrowseSourceScreen` / `LibraryTab` (unprefixed) reflect an older Aniyomi
   layout. Kuta is on the newer `AnimeScreen`/`MangaScreen` split layout, so
     our removal should keep the `Anime*` prefix and the `entries/anime/`
   paths, simply deleting the `manga/` siblings.

---

## Evidence index (key files inspected)

- `/home/z/animiru/settings.gradle.kts` — module list, build-logic include.
- `/home/z/animiru/app/build.gradle.kts` — applicationId, version, mpv/ffmpeg deps.
- `/home/z/animiru/gradle/build-logic/src/main/kotlin/mihon/gradle/BuildConfig.kt` — only `enableUpdater`/`enableCodeShrink`/`includeDependencyInfo`.
- `/home/z/animiru/app/src/main/AndroidManifest.xml` — only `AnimeExtensionInstallService`, Samsung OAuth for Discord.
- `/home/z/animiru/app/src/main/java/eu/kanade/tachiyomi/ui/home/HomeScreen.kt` — 4-tab list, `Tab` sealed interface.
- `/home/z/animiru/app/src/main/java/eu/kanade/domain/ui/model/StartScreen.kt` — no `MANGA` enum value.
- `/home/z/animiru/app/src/main/java/eu/kanade/tachiyomi/ui/library/LibraryTab.kt` — single anime-only tab.
- `/home/z/animiru/app/src/main/java/eu/kanade/tachiyomi/ui/browse/BrowseTab.kt` — single source/extension set.
- `/home/z/animiru/app/src/main/java/eu/kanade/tachiyomi/ui/main/MainActivity.kt` — anime-only imports.
- `/home/z/animiru/source-api/src/commonMain/kotlin/eu/kanade/tachiyomi/` — only `animesource/`, no `source/`.
- `/home/z/animiru/data/src/main/sqldelight/tachiyomi/data/` — only `animes.sq`/`episodes.sq`/`anime_sync.sq`/`animes_categories.sq`.
- `/home/z/animiru/app/src/main/java/eu/kanade/tachiyomi/data/track/` — 7 anime trackers, no MangaUpdates/Kavita/Suwayomi/Komga.
- `/home/z/animiru/app/src/main/java/mihon/core/migration/migrations/RenameKeysMigration.kt` — backward-compat `mangasync`→`animesync` key renames.
- `/home/z/animiru/i18n/src/commonMain/moko-resources/base/strings.xml` — `app_name = "Animiru"`.
- `/home/z/animiru/i18n-animiru/src/commonMain/moko-resources/base/strings.xml` — 197 `am.*` custom strings.
- `/home/z/animiru/CHANGELOG.md` — release/merge timeline.
- `git -C /home/z/animiru log -1` → `f56264978a969136d61b7e94b6fa3eb06b9dc014  2026-06-24  Release v0.19.7.7`.
