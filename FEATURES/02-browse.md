# 02 — Browse Tab, Sources, Extensions, Migration

> Feature inventory of the Browse bottom-nav tab and everything reachable from
> it: the source list, source browsing, the extension manager, extension
> repos, and the migration pipeline. Research/documentation only — no source
> files modified. All paths relative to `/home/z/kuta`.

---

### Browse Tab (Sub-tabs Pager)

- **Description**: The "Browse" bottom-nav tab. A scrollable top-bar pager
  hosting six sub-tabs in this order: Anime Sources, Manga Sources, Anime
  Extensions, Manga Extensions, Migrate (anime), Migrate (manga). Each sub-tab
  renders into the same `TabbedScreen` shell with a shared search bar (the
  search bar is wired to whichever extensions sub-tab is visible). Long-press
  the Browse tab to open global anime search.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/BrowseTab.kt` (Voyager
    `Tab`; builds `persistentListOf(animeSourcesTab(), mangaSourcesTab(),
    animeExtensionsTab(...), mangaExtensionsTab(...), migrateAnimeSourceTab(),
    migrateMangaSourceTab())` and passes to `TabbedScreen(scrollable = true)`)
  - `app/src/main/java/eu/kanade/presentation/components/TabbedScreen.kt`
    (shared shell: title, optional pager state, optional search bar, optional
    overflow actions)
  - Tab-content builders: `ui/browse/anime/source/AnimeSourcesTab.kt` →
    `animeSourcesTab()`, `ui/browse/manga/source/MangaSourcesTab.kt` →
    `mangaSourcesTab()`, `ui/browse/anime/extension/AnimeExtensionsTab.kt` →
    `animeExtensionsTab(...)`, `ui/browse/manga/extension/MangaExtensionsTab.kt`,
    `ui/browse/anime/migration/sources/MigrateAnimeSourceTab.kt` →
    `migrateAnimeSourceTab()`, `ui/browse/manga/migration/sources/MigrateMangaSourceTab.kt`.
- **Status**: `modify`
  - This is the "BrowseTab still has manga sub-tabs" coupling issue flagged
    as a known gap in worklog Task 4. The anime-only redesign should drop the
    three manga sub-tabs (and the `scrollable = true` may no longer be needed
    with only 2 anime sub-tabs).
- **Dependencies**:
  - Depends on: every sub-tab builder; `AnimeExtensionsScreenModel` /
    `MangaExtensionsScreenModel` are hoisted into `BrowseTab.Content()` so the
    search bar can read/write their `searchQuery` state.
  - Depended on by: `HomeScreen` (one of 6 tabs), `HomeScreen.Tab.Browse(toExtensions =
    true)` deep-link (calls `BrowseTab.showAnimeExtension()` which sends to
    `switchToTabNumberChannel` → `state.scrollToPage(2)`).
- **Notes**:
  - `BrowseTab.onReselect` pushes `GlobalAnimeSearchScreen()` (no context
    awareness — comment in source says "TODO: Find a way to let it open
    Global Anime/Manga Search depending on what Tab is open").
  - `BrowseTab.showExtension()` (no prefix) actually switches to **manga**
    extensions (tab index 3) — historical naming, easy footgun.
  - The `MainActivity.ready` flag is set true inside `BrowseTab.Content()`
    so the splash dismisses when Browse is the user's default tab.

---

### Anime Sources Tab (Source List + Pin/Disable)

- **Description**: The default Browse sub-tab. Lists every enabled
  `AnimeCatalogueSource` grouped by language (with "Last used" and "Pinned"
  pseudo-groups at the top). Tap a source → browse its catalog; long-press →
  options dialog (pin / disable); toolbar overflow → "Global search" and
  "Filter sources" (the latter opens the language filter screen). Source
  ordering and grouping is purely derived from `SourcePreferences` state.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/anime/source/AnimeSourcesTab.kt`
    (TabContent builder with two AppBar actions: TravelExplore → global
    search, FilterList → `AnimeSourcesFilterScreen`)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/anime/source/AnimeSourcesScreenModel.kt`
    (subscribes to `GetEnabledAnimeSources`; groups by `lang` /
    `LAST_USED_KEY` / `PINNED_KEY`; toggle source enable, toggle pin)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/anime/source/AnimeSourcesFilterScreen.kt`
    + `AnimeSourcesFilterScreenModel.kt` (language + source enable matrix)
  - `app/src/main/java/eu/kanade/presentation/browse/anime/AnimeSourcesScreen.kt`
  - Domain/interactors: `domain/.../source/anime/interactor/GetEnabledAnimeSources.kt`,
    `ToggleAnimeSource.kt`, `ToggleAnimeSourcePin.kt`
  - Preferences: `SourcePreferences.enabledLanguages()`, `disabledAnimeSources()`
    (`hidden_anime_catalogues`), `pinnedAnimeSources()`
    (`pinned_anime_catalogues`), `lastUsedAnimeSource()`
    (`__APP_STATE_last_anime_catalogue_source`), `showNsfwSource()`.
  - Source model: `domain/.../source/anime/model/AnimeSource.kt` +
    `StubAnimeSource.kt` + `Pin.kt`
- **Status**: `modify`
- **Dependencies**:
  - Depends on: `AnimeSourceManager` (via `GetEnabledAnimeSources`),
    `SourcePreferences`, `AnimeExtensionManager` (sources come from
    installed extensions; stub sources for uninstalled extensions also
    appear).
  - Depended on by: `BrowseTab` (default sub-tab), `BrowseAnimeSourceScreen`
    (push target), `GlobalAnimeSearchScreen` (push target).
- **Notes**:
  - `Pin` is an enum with `Actual` and `None` values — `Pin.Actual in
    source.pin` is the check used by the screen model.
  - "Last used" is set by `BrowseAnimeSourceScreenModel.init` (only when not
    incognito for that source).
  - Stub sources (`StubAnimeSource`) appear when an anime in the DB
    references a source whose extension is uninstalled — the Browse tab
    hides them but `AnimeSourceManager.getOrStub(...)` returns them; the
    `BrowseAnimeSourceScreen` shows a `MissingSourceScreen` for them.

---

### Source Browsing (Popular / Latest / Search / Filters)

- **Description**: The catalog browser pushed when tapping a source. Three
  listing modes via `FilterChip` row: Popular (always), Latest (when
  `source.supportsLatest`), and Search (with the source's
  `getFilterList()` exposed via a filter sheet). Paged with
  `androidx.paging.Pager(pageSize = 25)`; per-item long-press → favorite /
  duplicate / migrate dialog. Toolbar has display-mode toggle, web-view
  jump-off, source-preferences jump-off (for `ConfigurableAnimeSource`),
  and a help link for local sources.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/anime/source/browse/BrowseAnimeSourceScreen.kt`
    (Screen + Content; toolbar with FilterChips; duplicate/migrate/category
    dialogs)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/anime/source/browse/BrowseAnimeSourceScreenModel.kt`
    (`Listing` sealed class: `Popular` / `Latest` / `Search(query, filters)`;
    `animePagerFlowFlow` re-keys on listing; `search`, `searchGenre`,
    `changeAnimeFavorite`, `addFavorite` with default-category resolution,
    `getDuplicateAnimelibAnime`)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/anime/source/browse/SourceFilterAnimeDialog.kt`
    (filter sheet — `AnimeFilterList` UI)
  - `app/src/main/java/eu/kanade/presentation/browse/anime/BrowseAnimeSourceContent.kt`
  - Paging source: `data/.../source/anime/AnimeSourcePagingSource.kt` /
    `GetRemoteAnime.kt` (calls `source.getPopularAnime` /
    `getLatestUpdates` / `getSearchAnime`)
  - Source API: `source-api/.../animesource/AnimeCatalogueSource.kt` (the
    interface declaring `supportsLatest`, `getPopularAnime`,
    `getSearchAnime`, `getLatestUpdates`, `getFilterList`)
- **Status**: `keep`
- **Dependencies**:
  - Depends on: `AnimeSourceManager.getOrStub(sourceId)`,
    `GetRemoteAnime.subscribe(sourceId, query, filters)` (PagingSource),
    `NetworkToLocalAnime.await(...)` (inserts remote-only anime into DB
    before showing), `GetAnime.subscribe(url, source)` (refreshes row once
    local), `GetDuplicateLibraryAnime` (long-press duplicate detection),
    `SetAnimeCategories` + `SetAnimeDefaultEpisodeFlags` + `AddAnimeTracks`
    (on `addFavorite`), `LibraryPreferences.defaultAnimeCategory()`,
    `SourcePreferences.sourceDisplayMode()` /
    `hideInAnimeLibraryItems()` / `lastUsedAnimeSource()`.
  - Depended on by: `AnimeSourcesTab` (push target), `AnimeScreen` genre-tap
    (pushes `BrowseAnimeSourceScreen.searchGenre(name)`), `GlobalAnimeSearchScreen`
    ("Open in source" action).
- **Notes**:
  - `BrowseAnimeSourceScreen` has two `Channel`-based `suspend fun search()`
    / `searchGenre()` — used by `AnimeScreen` for genre chips. The `Channel`
    is on the companion object so it's a process-wide singleton keyed only
    by `SearchType` (a second `BrowseAnimeSourceScreen` for a different
    source would clobber the first — current known limitation, accepted
    because only one is visible at a time).
  - `Listing.valueOf(query)` recognizes the sentinel strings
    `GetRemoteAnime.QUERY_POPULAR` ("<popular>") and `QUERY_LATEST`
    ("<latest>") to round-trip listing mode through the screen's `listingQuery`
    constructor parameter (used for source long-press → "Popular" / "Latest"
    jump-offs in `AnimeSourcesScreen`).
  - `searchGenre(name)` walks the source's `AnimeFilterList` looking for a
    matching `TriState` / `CheckBox` / `Select` filter; if none matches it
    falls back to a plain text search.
  - The "hide items already in library" toggle
    (`SourcePreferences.hideInAnimeLibraryItems()`) filters paged items
    client-side via `paging.filter { !it.value.favorite }`.

---

### Extension Manager (Install / Update / Uninstall / Trust)

- **Description**: The "Anime Extensions" Browse sub-tab. Lists installed /
  updatable / available / untrusted extensions grouped by language. Supports
  install (from a configured repo's `index.min.json`), update (single or
  "Update all"), uninstall (with a confirmation dialog for private
  side-loaded extensions that aren't PackageManager-installed), trust (for
  extensions signed by an unknown key), and an overflow action for
  per-extension "Open in WebView" (jumps to the first source's `baseUrl`).
  The toolbar shows an updates-count badge.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/anime/extension/AnimeExtensionsTab.kt`
    (TabContent builder; `AnimeExtensionUninstallConfirmation` for private
    extensions)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/anime/extension/AnimeExtensionsScreenModel.kt`
    (`installExtension`, `updateExtension`, `updateAllExtensions`,
    `cancelInstallUpdateExtension`, `uninstallExtension`,
    `findAvailableExtensions` (refresh), `trustExtension`)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/anime/extension/AnimeExtensionFilterScreen.kt`
    + `AnimeExtensionFilterScreenModel.kt` (language filter — controls which
    languages show up in the available list)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/anime/extension/details/AnimeExtensionDetailsScreen.kt`
    + `AnimeExtensionDetailsScreenModel.kt` (per-extension screen: list
    sources with enable toggles, "Enable all" / "Disable all", "Clear
    cookies", "Uninstall", per-extension incognito toggle, source-preferences
    jump-off for `ConfigurableAnimeSource`)
  - `app/src/main/java/eu/kanade/tachiyomi/extension/anime/AnimeExtensionManager.kt`
    (singleton; `installedExtensionsFlow`, `availableExtensionsFlow`,
    `untrustedExtensionsFlow`; `findAvailableExtensions()` aggregates across
    all configured repos via `AnimeExtensionApi`)
  - `app/src/main/java/eu/kanade/tachiyomi/extension/anime/util/AnimeExtensionLoader.kt`
    (`ChildFirstPathClassLoader`, lib version 12..16, SHA-256 trust check)
  - `app/src/main/java/eu/kanade/tachiyomi/extension/anime/util/AnimeExtensionInstaller.kt`
    + `installer/PackageInstallerInstallerAnime.kt` +
    `installer/ShizukuInstallerAnime.kt` + `util/AnimeExtensionInstallService.kt`
    + `util/AnimeExtensionInstallReceiver.kt` (the install pipeline)
  - `app/src/main/java/eu/kanade/tachiyomi/extension/anime/api/AnimeExtensionApi.kt`
    (fetches `index.min.json` from each configured repo)
  - Trust: `domain/.../extension/anime/interactor/TrustAnimeExtension.kt`
    (persists SHA-256 fingerprints to
    `SourcePreferences.trustedExtensions()` = `__APP_STATE_trusted_extensions`)
  - Installer preference: `BasePreferences.extensionInstaller()` (enum:
    `PACKAGEINSTALLER` / `SHIZUKU` / `LEGACY`); used by
    `AnimeExtensionsScreenModel` for state and by
    `AnimeExtensionInstaller` for dispatch.
  - Updates counter: `SourcePreferences.animeExtensionUpdatesCount()`
    (`animeext_updates_count`).
- **Status**: `keep`
- **Dependencies**:
  - Depends on: `AnimeExtensionApi` (network), `AnimeExtensionLoader`
    (classloader + trust), `AnimeExtensionInstallService` /
    `AnimeExtensionInstallReceiver` (install pipeline), `ShizukuInstallerAnime`
    (when installer pref is Shizuku), `PackageManager` (for shared/private
    package detection), `AnimeExtensionManager.installedExtensionsFlow` (drives
    `AndroidAnimeSourceManager` to rebuild the source map).
  - Depended on by: `AnimeSourcesTab` (sources only appear once their
    extension is installed & trusted), `AnimeSourceManager`, library-update
    job (sources come from extensions), `AnimeExtensionReposScreen` (pushed
    from overflow).
- **Notes**:
  - The install pipeline is identical-shape to the manga twin
    (`extension/manga/*`). Removing the manga UI does not require touching
    the anime pipeline; the manga pipeline just becomes unreachable.
  - Private extensions (side-loaded via `filesDir/exts/*.ext`) get an extra
    confirmation dialog because they can't be removed via `PackageManager`
    — see `AnimeExtensionUninstallConfirmation` in `AnimeExtensionsTab.kt`.
  - `AnimeExtensionManager.trust(ext)` persists the signature and *re-loads*
    the extension synchronously via
    `AnimeExtensionLoader.loadExtensionFromPkgName(...)`.
  - `AnimeExtensionsScreenModel.findAvailableExtensions()` adds a fake 1-second
    delay so the refresh spinner is visible — UX choice, not a real perf
    concern.

---

### Extension Repos (Add / Replace / Delete / Refresh)

- **Description**: A settings sub-screen listing all configured anime
  extension repository URLs (each with name, website, signing-key
  fingerprint). User can add a new repo by URL (validated against
  `^https://.*/index\.min\.json$`), delete a repo, or refresh all repos'
  metadata. Adding a repo whose signing-key fingerprint matches an existing
  repo offers to *replace* the old one (conflict dialog). Also reachable via
  the `aniyomi://add-repo` / `tachiyomi://add-repo` deep links.
- **Location**:
  - `app/src/main/java/eu/kanade/presentation/more/settings/screen/browse/AnimeExtensionReposScreen.kt`
    (Voyager `Screen`; deep-link aware via constructor `url: String?` →
    `screenModel.showDialog(RepoDialog.Confirm(url))`)
  - `app/src/main/java/eu/kanade/presentation/more/settings/screen/browse/AnimeExtensionReposScreenModel.kt`
    (`createRepo`, `replaceRepo`, `deleteRepo`, `refreshRepos`;
    dispatches to interactors)
  - Shared components:
    `app/src/main/java/eu/kanade/presentation/more/settings/screen/browse/components/ExtensionReposScreen.kt`,
    `ExtensionReposContent.kt`, `ExtensionReposDialogs.kt`
  - Domain/interactors:
    `domain/.../mihon/domain/extensionrepo/anime/interactor/{Create,Delete,Replace,Update,Get}AnimeExtensionRepo.kt`
  - Service: `domain/.../mihon/domain/extensionrepo/service/ExtensionRepoService.kt`
    (fetches `repo.json`, throws on conflict)
  - DB: `data/src/main/sqldelightanime/dataanime/extension_repos.sq` (anime
    side; manga twin in `sqldelight/data/extension_repos.sq`)
  - Repo URL validation regex: defined inline in `CreateAnimeExtensionRepo.kt`
    as `^https://.*/index\.min\.json$`.
  - Migration: `core/common/.../mihon/core/migration/.../ExternalRepoMigration.kt`
    (one-time rewrite of legacy user repos to the
    `https://raw.githubusercontent.com/<owner>/<repo>/repo` shape).
  - Deep-link handling: `app/.../ui/main/MainActivity.kt` (handles
    `aniyomi://add-repo` / `tachiyomi://add-repo` URIs → pushes
    `AnimeExtensionReposScreen(url = ...)`).
- **Status**: `keep`
  - **Important caveat**: there is *no* hard-coded default repo URL in this
    fork (verified, see `DOCS/architecture/07-extensions.md` §3a). After
    install, the user must add at least one repo manually or via deep link
    before any extensions appear in the "Available" list. The orchestrator
    may want to seed a default repo URL on first run.
- **Dependencies**:
  - Depends on: anime `extension_repos` table, `ExtensionRepoService`
    (HTTP fetch of `repo.json`), `AnimeExtensionApi.findExtensions()`
    (re-fetches `index.min.json` from all repos when the list changes).
  - Depended on by: `AnimeExtensionsTab` (overflow action → this screen),
    `MainActivity` deep-link handler, `AnimeExtensionApi` (consumes the repo
    list at refresh time).
- **Notes**:
  - Two parallel `extension_repos` tables exist (one per DB); anime repos are
    in `tachiyomi.animedb`, manga repos in `tachiyomi.db`. The
    `SourcePreferences.animeExtensionRepos()` /
    `mangaExtensionRepos()` SharedPreferences entries are *legacy* (the
    source-of-truth is now the DB tables) — kept for migration compatibility.
  - The signing-key fingerprint conflict flow (`DuplicateFingerprint` result
    → `RepoDialog.Conflict(oldRepo, newRepo)` → `replaceRepo`) prevents the
    same maintainer from being added under two URLs.
  - `refreshRepos()` calls `UpdateAnimeExtensionRepo.awaitAll()` which
    re-fetches `repo.json` for every repo and updates name/website/fingerprint
    in place.

---

### Migration (Source → Anime → Search → Migrate)

- **Description**: A multi-step "migration" flow for moving favorited anime
  from one source to another (e.g. when a source dies). Entry point: the
  "Migrate" sub-tab in Browse → list of sources that have favorited anime →
  pick a source → list of favorited anime from that source → pick an anime →
  global-style search across other sources → pick a new match → migration
  dialog (choose what to copy: episodes, seen-state, bookmarks, scores,
  categories; or copy a season grouping). Also surfaced as a "migrate"
  option from duplicate-anime dialogs in browse/history.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/anime/migration/sources/MigrateAnimeSourceTab.kt`
    (Browse sub-tab: list of sources with favorites, sortable)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/anime/migration/sources/MigrateAnimeSourceScreenModel.kt`
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/anime/migration/anime/MigrateAnimeScreen.kt`
    + `MigrateAnimeScreenModel.kt` (list of favorite anime for a given
    source; tap → `MigrateAnimeSearchScreen`; long-press cover → `AnimeScreen`)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/anime/migration/search/MigrateAnimeSearchScreen.kt`
    + `MigrateAnimeSearchScreenModel.kt` (global-search UI against all
    sources except the current one; tap result → `MigrateAnimeDialog`)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/anime/migration/search/MigrateAnimeDialog.kt`
    + `AnimeMigrateSearchScreenDialogScreenModel.kt` (the actual migrate
    action; user toggles which data to copy via `AnimeMigrationFlags`)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/anime/migration/AnimeMigrationFlags.kt`
    (bitmask flags: episodes, seen-state, bookmarks, scores, categories)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/anime/migration/anime/season/MigrateSeasonSelectScreen.kt`
    (season-aware migration: choose which seasons to migrate when the new
    source uses season grouping)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/anime/migration/search/AnimeSourceSearchScreen.kt`
    (per-source search within migration — pushed from "Open in source"
    inside the migrate search results)
  - Manga twins: `ui/browse/manga/migration/*` (entire parallel subtree).
  - Sort prefs: `SourcePreferences.migrationSortingMode()` /
    `migrationSortingDirection()`.
- **Status**: `TBD`
  - Migration is a power-user feature. The user needs to decide whether to
    keep it in the anime-only redesign (most consumer-facing anime apps
    don't have it). If kept, the manga sub-tab and manga migration screens
    should be removed.
- **Dependencies**:
  - Depends on: `GetLibraryAnime` (to find favorites per source),
    `AnimeSourceManager`, the search infrastructure (reuses
    `AnimeSearchScreenModel` patterns), `AnimeMigrationFlags` bitmask,
    `MigrateAnimeDialogScreenModel` for the actual copy.
  - Depended on by: `BrowseTab` (sub-tab), `AnimeHistoryScreenModel`
    (duplicate-anime dialog → `MigrateAnimeDialog`), `BrowseAnimeSourceScreenModel`
    (duplicate-anime dialog → migrate), `AnimeScreen` (entry point on
    duplicate detection).
- **Notes**:
  - `AnimeMigrationFlags` is a bitmask enum mirroring `MangaMigrationFlags` —
    the dialog renders a fixed list of checkboxes.
  - The migration dialog supports a "seasons" branch
    (`MigrateSeasonSelectScreen`) that only shows up for anime whose source
    uses `FetchType.Seasons`. This is anime-only (no manga equivalent).
  - `MigrateAnimeSearchScreen` is essentially `GlobalAnimeSearchScreen` minus
    the source being migrated from — reuses `AnimeSearchScreenModel` as its
    base class.
  - Help link in the Browse migration sub-tab points to
    `https://aniyomi.org/help/guides/source-migration/` — should be
    rebranded or removed.

---

### Manga-side Browse Sub-tabs (Sources / Extensions / Migration)

- **Description**: The three manga sub-tabs that `BrowseTab` includes by
  default: `mangaSourcesTab()`, `mangaExtensionsTab(...)`,
  `migrateMangaSourceTab()`. Fully parallel implementations of every
  feature above but for the manga side (manga sources, manga extensions,
  manga migration). All functional and reachable via the Browse pager.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/manga/source/MangaSourcesTab.kt`
    + `MangaSourcesScreenModel.kt` + `MangaSourcesFilterScreen.kt`
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/manga/source/browse/BrowseMangaSourceScreen.kt`
    + `BrowseMangaSourceScreenModel.kt` + `SourceFilterMangaDialog.kt`
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/manga/source/globalsearch/GlobalMangaSearchScreen.kt`
    + `GlobalMangaSearchScreenModel.kt` + `MangaSearchScreenModel.kt`
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/manga/extension/MangaExtensionsTab.kt`
    + `MangaExtensionsScreenModel.kt` + `MangaExtensionFilterScreen.kt`
    + `details/MangaExtensionDetailsScreen.kt` +
    `details/MangaExtensionDetailsScreenModel.kt`
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/manga/migration/sources/MigrateMangaSourceTab.kt`
    + `MigrateMangaSourceScreenModel.kt`
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/manga/migration/manga/MigrateMangaScreen.kt`
    + `MigrateMangaScreenModel.kt`
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/manga/migration/search/MigrateMangaSearchScreen.kt`
    + `MigrateMangaSearchScreenModel.kt` + `MigrateMangaDialog.kt` +
    `MangaMigrateSearchScreenDialogScreenModel.kt` + `MangaSourceSearchScreen.kt`
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/manga/migration/MangaMigrationFlags.kt`
  - Extension mgr: `extension/manga/MangaExtensionManager.kt` +
    `util/MangaExtensionLoader.kt` + `api/MangaExtensionApi.kt` +
    `installer/*` (manga twins of the anime install pipeline)
  - Manga source mgr: `source/manga/AndroidMangaSourceManager.kt`
  - Manga repos screen:
    `presentation/more/settings/screen/browse/MangaExtensionReposScreen.kt`
    + `MangaExtensionReposScreenModel.kt`
- **Status**: `remove`
  - These are UI entry points only — the manga *data* layer (DB, mappers,
    repos, domain models) stays. Removing the UI means dropping the
    `BrowseTab` entries for the three manga sub-tabs (and their tab builders,
    screens, screen models, presenters). The manga extension/source managers
    can stay loaded (they're harmless) or be removed later.
- **Dependencies**:
  - Depends on: the manga data layer (`DOCS/architecture/08-database.md` §5.1),
    `MangaExtensionManager`, `AndroidMangaSourceManager`.
  - Depended on by: `BrowseTab.Content()` (instantiates all three).
- **Notes**:
  - Phase 1 worklog noted this as a known gap: "BrowseTab still has manga
    sub-tabs (coupling issue, deferred)". This is the recommended
    Phase-2 cleanup.
  - Note the asymmetric naming: anime paths use `Anime*` prefix
    (`AnimeSourcesTab`, `AnimeExtensionDetailsScreen`); manga paths use the
    legacy unprefixed `Manga*` prefix (added when anime was bolted on).
    Anime-side code is the "newer" naming convention.
  - The manga `MangaExtensionInstallService` is declared in
    `AndroidManifest.xml` alongside the anime one — if the manga UI is
    removed, the manga install service declaration can also go.
