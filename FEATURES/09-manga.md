# 09 — Manga Features (currently UI-gated; status TBD)

Manga is intact at the data layer (DB tables, mappers, repos, migrations,
extension subsystem, trackers) — Phase 1 only gated the **UI entry points**
(removed `MangaLibraryTab` from the bottom nav, gated `MoreTab.onClickAlt`,
made `HomeScreen.defaultTab` fall back to `AnimeLibraryTab`). Every other
manga UI is still reachable through `BrowseTab`, `HistoriesTab`,
`CategoriesTab`, `StatsTab`, `StorageTab`, `DownloadsTab` (all of which have
**dual anime + manga sub-tabs**) and via the manga detail screen `MangaScreen`
which can still be pushed from those sub-tabs.

**The user has not yet decided whether to fully remove manga (à la Animiru —
physical deletion) or re-enable it.** Every entry below is `TBD`.

> See `DOCS/architecture/08-database.md`, `09-preferences.md`, `10-data-models.md`
> for the data-layer coupling, and `DOCS/architecture/12-animiru-diff.md` for
> the reference "manga fully removed" implementation.

---

### Manga Library (gated)

- **Description**: A bottom-nav tab listing manga the user has added to their
  library, grouped by category, with a per-category swipe-pager, filter sheet,
  global-update trigger, multi-select bottom action bar (change category, mark
  read/unread, download, delete), and "continue reading" quick action.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/ui/library/manga/MangaLibraryTab.kt` —
    Voyager `Tab`.
  - `app/src/main/java/eu/kanade/tachiyomi/ui/library/manga/MangaLibraryScreenModel.kt`,
    `MangaLibrarySettingsScreenModel.kt`, `MangaLibraryItem.kt`.
  - Compose UI: `app/src/main/java/eu/kanade/presentation/library/manga/`.
  - Back-end: `LibraryPreferences` (manga-side keys: `default_category`,
    `mangaUpdateCategories`, `mangaUpdateCategoriesExclude`,
    `pref_filter_library_*`, `library_sorting_mode`).
  - Background updater: `app/src/main/java/eu/kanade/tachiyomi/data/library/manga/MangaLibraryUpdateJob.kt`
    (WorkManager periodic + OneTime) + `MangaMetadataUpdateJob.kt` +
    `MangaLibraryUpdateNotifier.kt`.
- **Status**: `TBD` — currently gated; data layer untouched.
- **Dependencies**: `MangaScreen` (entry detail), `ReaderActivity` (continue
  reading), `CategoriesTab` (edit categories), `MangaLibraryUpdateJob`
  (refresh). `LibraryPreferences.autoUpdateInterval().onValueChanged` in
  `SettingsLibraryScreen` still calls `MangaLibraryUpdateJob.setupTask` —
  this is the only thing keeping manga library updates alive in the
  background.
- **Notes**: Phase 1 gating is enforced by `NavStyle.tabs` always removing
  `MangaLibraryTab` (see `app/.../ui/home/HomeScreen.kt` and
  `domain/ui/model/NavStyle.kt` — the FORK marker strips manga regardless of
  the user's chosen `NavStyle`).

---

### Manga Reader (page-based reader; NOT the anime player)

- **Description**: A full-screen paged or vertical-scroll (webtoon) image
  reader for manga chapters. Supports left-to-right / right-to-left / vertical
  / webtoon viewing modes, edge/L/kindlish/right-left navigation overlays,
  page transitions, double-tap animation, E-Ink refresh flashing, page-number
  display, fullscreen + cutout handling, keep-screen-on, custom reader theme,
  share / save / set-as-cover page actions. Loads chapters from one of:
  DownloadPageLoader, HttpPageLoader, DirectoryPageLoader, ArchivePageLoader,
  EpubPageLoader.
- **Location**:
  - Activity: `app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderActivity.kt`
    (declared in manifest with `launchMode="singleTask"`, exported=false).
  - ViewModel: `app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderViewModel.kt`.
  - Viewers: `app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/` —
    `pager/` (Pager, PagerViewer, PagerConfig, PagerPageHolder,
    PagerTransitionHolder, PagerViewers, PagerViewerAdapter) and
    `webtoon/` (WebtoonViewer, WebtoonConfig, WebtoonFrame, WebtoonRecyclerView,
    WebtoonLayoutManager, WebtoonAdapter, WebtoonBaseHolder, WebtoonPageHolder,
    WebtoonTransitionHolder, WebtoonSubsamplingImageView).
  - Loaders: `app/src/main/java/eu/kanade/tachiyomi/ui/reader/loader/` —
    `PageLoader.kt`, `ChapterLoader.kt`, `DownloadPageLoader.kt`,
    `HttpPageLoader.kt`, `DirectoryPageLoader.kt`, `ArchivePageLoader.kt`,
    `EpubPageLoader.kt`.
  - Models: `app/src/main/java/eu/kanade/tachiyomi/ui/reader/model/` —
    `ReaderChapter.kt`, `ReaderPage.kt`, `InsertPage.kt`,
    `ChapterTransition.kt`, `ViewerChapters.kt`.
  - Settings: `app/src/main/java/eu/kanade/tachiyomi/ui/reader/setting/` —
    `ReaderPreferences.kt`, `ReaderSettingsScreenModel.kt`,
    `ReaderOrientation.kt`, `ReadingMode.kt`.
  - Other: `ReaderNavigationOverlayView.kt`, `ReaderPageImageView.kt`,
    `ReaderProgressIndicator.kt`, `ReaderButton.kt`,
    `ReaderTransitionView.kt`, `SaveImageNotifier.kt`,
    `GestureDetectorWithLongTap.kt`, navigation overlays under `viewer/navigation/`.
  - DB tables: `mangas`, `chapters`, `manga_history`, `manga_sync` (in
    `tachiyomi.db`).
- **Status**: `TBD` — the reader itself is manga-specific; if manga is fully
  removed, this entire directory tree plus `ReaderActivity`'s manifest entry
  should go. If manga is re-enabled, no change needed.
- **Dependencies**: `MangaScreen` (which pushes `ReaderActivity.newIntent`),
    `MangaHistoryTab` (resume-next-chapter), `MangaScreenModel` (open chapter
    from entry detail). `ReaderPreferences` is consumed both by
    `SettingsReaderScreen` and by the in-reader settings dialog.
- **Notes**: This is the second-largest feature tree after the player.
  Viewers and loaders are completely decoupled from the player pipeline (no
  shared MPV code).

---

### Manga Entry Detail (MangaScreen)

- **Description**: Per-manga overview screen: cover, title, author/artist,
  description, genre tags, status, source link, favorite toggle, category
  assignment, tracking chips, chapter list (with sort, filter by scanlator,
  download-all, mark-all-read), "merge with another manga" + "migrate to
  another source" actions. Reaching this screen is the only path to the
  reader.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/ui/entries/manga/MangaScreen.kt` —
    Voyager `Screen`.
  - `MangaScreenModel.kt`, `MangaCoverScreenModel.kt`.
  - Tracking UI: `track/MangaTrackItem.kt`, `track/MangaTrackInfoDialog.kt`
    (875 lines — score / status / date / search / delete / refresh).
  - Migration: `app/src/main/java/eu/kanade/tachiyomi/ui/browse/manga/migration/`
    (`MangaMigrationFlags.kt`, `manga/MigrateMangaScreen.kt`,
    `manga/MigrateMangaScreenModel.kt`,
    `search/MigrateMangaSearchScreen.kt`, `search/MigrateMangaSearchScreenModel.kt`,
    `search/MigrateMangaDialog.kt`,
    `search/MangaSourceSearchScreen.kt`,
    `search/MangaMigrateSearchScreenDialogScreenModel.kt`,
    `sources/MigrateMangaSourceTab.kt`,
    `sources/MigrateMangaSourceScreenModel.kt`).
  - Compose UI: `app/src/main/java/eu/kanade/presentation/entries/manga/`.
- **Status**: `TBD` — coupled to manga decision; if removed, also delete the
  entire `ui/browse/manga/migration/` tree.
- **Dependencies**: `MangaSourcesTab` + `BrowseMangaSourceScreen` (open from
  browse), `CategoriesTab` (change category), `ReaderActivity` (open chapter),
  `MangaTrackInfoDialog` (open tracker sheet), `WebViewScreen` (open source
  webview).
- **Notes**: `MangaScreen` is reachable from `BrowseMangaSourceScreen`,
  `MangaHistoryTab`, `MangaUpdatesTab`, `MigrateMangaSearchScreen`,
  `GlobalMangaSearchScreen`, and launcher shortcuts (`SHORTCUT_MANGA` →
  `HomeScreen.Tab.Library(mangaIdToOpen)`). Phase 1 doesn't disable any of
  these reach-paths.

---

### Manga Browse / Sources / Extensions / Migration

- **Description**: Browse-side manga UI. Six sub-surfaces: (1) **Manga
  Sources** list (pinned + last-used + all) with global-search + filter
  actions; (2) **Browse Manga Source** (per-source listing with pagination,
  filters via `SourceFilterMangaDialog`); (3) **Global Manga Search** (queries
  all enabled sources in parallel); (4) **Manga Extensions** tab (installed /
  available / updates) plus per-extension details + source-preferences
  screens; (5) **Migrate Manga Source** (pick source → migrate all manga from
  it); (6) **Manga Extension Filter** (per-source language filter).
- **Location**:
  - All under `app/src/main/java/eu/kanade/tachiyomi/ui/browse/manga/`:
    - `source/MangaSourcesTab.kt`, `MangaSourcesScreenModel.kt`,
      `MangaSourcesFilterScreen.kt`, `MangaSourcesFilterScreenModel.kt`,
      `source/browse/BrowseMangaSourceScreen.kt`,
      `BrowseMangaSourceScreenModel.kt`, `SourceFilterMangaDialog.kt`,
      `source/globalsearch/GlobalMangaSearchScreen.kt`,
      `GlobalMangaSearchScreenModel.kt`, `MangaSearchScreenModel.kt`.
    - `extension/MangaExtensionsTab.kt`, `MangaExtensionsScreenModel.kt`,
      `MangaExtensionFilterScreen.kt`, `MangaExtensionFilterScreenModel.kt`,
      `extension/details/MangaExtensionDetailsScreen.kt`,
      `MangaExtensionDetailsScreenModel.kt`,
      `MangaSourcePreferencesScreen.kt`.
    - `migration/` (see previous entry).
  - Sub-tab registration: `BrowseTab.Content()` in
    `app/src/main/java/eu/kanade/tachiyomi/ui/browse/BrowseTab.kt` lists
    `animeSourcesTab()`, `mangaSourcesTab()`, `animeExtensionsTab()`,
    `mangaExtensionsTab()`, `migrateAnimeSourceTab()`,
    `migrateMangaSourceTab()` — six pager pages.
  - Manga extension loader / API:
    `app/src/main/java/eu/kanade/tachiyomi/extension/manga/` (parallel to
    `extension/anime/`).
  - Manga source API:
    `source-api/src/main/java/tachiyomi/source/manga/` and
    `source-api/src/main/java/tachiyomi/source/` (MangaSource hierarchy).
- **Status**: `TBD` — coupling point: `BrowseTab` is the bottom-nav tab and
  currently exposes 6 pager pages (3 anime + 3 manga). Phase 1 did **not**
  gate the manga sub-tabs (known gap, see worklog Task 4).
- **Dependencies**: `MangaSourceManager` (Injekt singleton),
  `MangaExtensionLoader`, `MangaExtensionApi`,
  `mihon.domain.extensionrepo.manga.*` interactors, `SourcePreferences`
  (`hidden_catalogues`, `extension_repos`, `manga_extension_repos`).
- **Notes**: Removing manga sub-tabs from `BrowseTab` is the smallest
  immediate Phase-1.5 win — drop 3 of 6 entries from the `persistentListOf`
  in `BrowseTab.Content`.

---

### Manga Downloads

- **Description**: Background chapter-download manager (CBZ packing, image
  splitting for tall pages, parallel downloads, retry, auto-delete after read,
  per-category exclusion). UI is a sub-tab inside the shared `DownloadsTab`.
- **Location**:
  - Manager: `app/src/main/java/eu/kanade/tachiyomi/data/download/manga/MangaDownloadManager.kt`,
    `MangaDownloadJob.kt`, `MangaDownloader.kt`, `MangaDownloadCache.kt`,
    `MangaDownloadProvider.kt`, `MangaDownloadStore.kt`,
    `MangaDownloadPendingDeleter.kt`, `MangaDownloadNotifier.kt`,
    `model/MangaDownload.kt`.
  - UI: `app/src/main/java/eu/kanade/tachiyomi/ui/download/manga/` —
    `MangaDownloadQueueTab.kt`, `MangaDownloadQueueScreen.kt`,
    `MangaDownloadQueueScreenModel.kt`, `MangaDownloadAdapter.kt`,
    `MangaDownloadItem.kt`, `MangaDownloadHeaderItem.kt`,
    `MangaDownloadHeaderHolder.kt`, `MangaDownloadHolder.kt`.
  - Sub-tab registration: `DownloadsTab.Content()` in
    `app/src/main/java/eu/kanade/tachiyomi/ui/download/DownloadsTab.kt` —
    pager with anime (page 0) + manga (page 1).
  - Settings: `DownloadPreferences` (`pref_download_new`,
    `auto_download_while_reading`, `remove_after_read_slots`,
    `remove_exclude_categories` (manga keys), `save_chapter_as_cbz`,
    `split_tall_images`).
- **Status**: `TBD` — coupled to manga decision; the `DownloadsTab` is
  shared UI with both anime and manga sub-tabs.
- **Dependencies**: `MangaSourceManager` + manga `HttpSource` /
  `ParsedMangaHttpSource` for fetching pages; `StoragePreferences` for
  download location.
- **Notes**: Downloads are **not** persisted to the DB — they live in memory
  (`MangaDownloadStore` is a JSON serializer to disk for queue state across
  restarts). The cache (`MangaDownloadCache`) is invalidated by
  `SettingsAdvancedScreen`'s "Invalidate download cache" action.

---

### Manga History

- **Description**: A list of recently-read manga chapters with resume
  buttons, grouped by date, with delete-one / delete-all actions and a
  global search bar. Tapping a cover opens `MangaScreen`; the resume button
  opens `ReaderActivity` directly on the next unread chapter.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/ui/history/manga/MangaHistoryTab.kt`,
    `MangaHistoryScreenModel.kt`.
  - Sub-tab registration: `HistoriesTab.Content()` in
    `app/src/main/java/eu/kanade/tachiyomi/ui/history/HistoriesTab.kt` —
    pager with anime (page 0) + manga (page 1).
  - DB: `manga_history` table + `manga_history_with_relations` view in
    `tachiyomi.db`.
  - Interactors: `domain/.../history/manga/interactor/` (`GetMangaHistory`,
    `UpsertMangaHistory`, `RemoveMangaHistory`, `GetNextChapters`).
- **Status**: `TBD` — shared `HistoriesTab` would need its manga page dropped.
- **Dependencies**: `ReaderActivity`, `MangaScreen`, `CategoriesTab`
  (add-to-library from history), `MigrateMangaDialog` (dedup → migrate).
- **Notes**: `resumeLastChapterReadEvent` is a top-level `Channel<Unit>` —
  fired when the user long-taps the History nav tab (`onReselect`). Currently
  only resumes anime (the manga equivalent, `resumeLastChapterReadEvent`,
  is unused by `HistoriesTab.onReselect`).

---

### Manga Tracking

- **Description**: Per-manga tracker integration — score, status (reading /
  completed / on-hold / dropped / plan-to-read), progress (chapters read),
  start/finish dates, per-tracker search-and-bind, refresh, unbind. Uses the
  same 7 standard trackers as anime (`MAL`, `Anilist`, `Kitsu`, `MangaUpdates`,
  `Shikimori`, `Simkl`, `Bangumi`) plus 4 manga-only enhanced trackers
  (`Komga`, `Kavita`, `Suwayomi`, `MangaUpdates`-as-enhanced). The
  `MangaTrackInfoDialog` is the user-facing UI.
- **Location**:
  - UI: `app/src/main/java/eu/kanade/tachiyomi/ui/entries/manga/track/MangaTrackInfoDialog.kt`,
    `MangaTrackItem.kt`.
  - Tracker interfaces: `app/src/main/java/eu/kanade/tachiyomi/data/track/MangaTracker.kt`,
    `EnhancedMangaTracker.kt`, `DeletableMangaTracker.kt`.
  - Per-tracker implementations under `data/track/<service>/` (each service
    has both anime and manga paths in the same dir — e.g. `MyAnimeList.kt`
    implements both `AnimeTracker` and `MangaTracker`).
  - Manga-only enhanced trackers: `data/track/komga/Komga.kt`,
    `data/track/kavita/Kavita.kt`, `data/track/suwayomi/Suwayomi.kt`,
    `data/track/mangaupdates/MangaUpdates.kt` (MangaUpdates is both a
    standard tracker and an enhanced one — its `EnhancedMangaTracker`
    impl binds to a specific source class).
  - DB: `manga_sync` table in `tachiyomi.db`.
  - Domain: `domain/.../track/manga/` (interactors + models).
  - Back-up model: `data/backup/models/BackupTracking.kt` (manga) — distinct
    from `BackupAnimeTracking.kt`.
- **Status**: `TBD` — standard trackers serve both anime and manga so they
  stay; manga-only enhanced trackers (Komga, Kavita, Suwayomi, plus the
  enhanced side of MangaUpdates) follow the manga decision.
- **Dependencies**: `TrackerManager` (registered in `DomainModule`),
  `TrackPreferences` (`__PRIVATE_track_token_<id>`,
  `__PRIVATE_pref_mangasync_username_<id>`,
  `__PRIVATE_pref_mangasync_password_<id>`).
- **Notes**: `SettingsTrackingScreen` shows enhanced trackers conditionally —
  only when their accepted source class is installed. Removing manga
  extensions would hide the enhanced-manga-tracker rows automatically.

---

### Manga Categories

- **Description**: CRUD UI for manga categories — create / rename / reorder /
  hide / delete. Categories are used to group library entries and to scope
  update + auto-download + delete-exclude rules. The `Category` model is
  **shared** between anime and manga (single table `categories` with a
  `manga_not_anime` boolean column — see `08-database.md`).
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/ui/category/manga/MangaCategoryTab.kt`,
    `MangaCategoryScreenModel.kt`.
  - Sub-tab registration: `CategoriesTab.Content()` in
    `app/src/main/java/eu/kanade/tachiyomi/ui/category/CategoriesTab.kt` —
    pager with anime (page 0) + manga (page 1); `CategoriesTab.showMangaCategory()`
    sends a `Channel<Unit>` to programmatically switch to page 1 (used by
    `SettingsLibraryScreen` and `MangaLibraryTab`).
  - Compose UI: `app/src/main/java/eu/kanade/presentation/category/`
    (`MangaCategoryScreen.kt`, shared `CategoryCreateDialog.kt`,
    `CategoryRenameDialog.kt`, `CategoryDeleteDialog.kt`).
  - Domain: `domain/.../category/manga/` (interactors) — the `Category`
    model itself lives in `domain/.../category/model/Category.kt` (shared).
  - DB: `categories` table + `mangas_categories` link table.
- **Status**: `TBD` — even if manga is removed, the shared `categories` table
  has anime rows and manga rows in the same schema; removal requires a
  migration to drop manga category rows.
- **Dependencies**: `LibraryPreferences.defaultMangaCategory()`,
  `mangaUpdateCategories()`, `mangaUpdateCategoriesExclude()`,
  `DownloadPreferences.downloadNewChapterCategories()`,
  `DownloadPreferences.removeExcludeCategories()`.
- **Notes**: The `CategoriesTab.showMangaCategory()` channel survives across
  tab switches because `CategoriesTab` is a singleton `data object` — its
  `Channel` is process-singleton.

---

### Manga Storage (sub-tab of shared StorageTab)

- **Description**: Per-category manga disk-usage breakdown — shows each
  category with its total downloaded-chapter size, lets the user drill in
  and delete entries to reclaim space. The anime equivalent lives on page 0
  of the same `StorageTab`.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/ui/storage/manga/MangaStorageTab.kt`,
    `MangaStorageScreenModel.kt`.
  - Shared host: `app/src/main/java/eu/kanade/tachiyomi/ui/storage/StorageTab.kt`
    (pager: anime + manga).
  - Shared logic: `CommonStorageScreenModel.kt`.
  - Compose UI: `app/src/main/java/eu/kanade/presentation/more/storage/`.
- **Status**: `TBD` — drops trivially if manga is removed (delete page 1 of
  the `StorageTab` pager).
- **Dependencies**: `MangaDownloadProvider` (resolves on-disk paths),
  `MangaDownloadManager`, `GetMangaFavorites`.
- **Notes**: `StorageTab` is opened from `MoreTab.onClickStorage` and from
  `SettingsDataScreen.getDataGroup()` (the "Storage" row).

---

### Cross-cutting coupling summary

If manga is fully removed (the Animiru approach — see `12-animiru-diff.md`),
the following **shared** files will need surgery (not just deletion):

- **Top-level Tabs** (each hosts an anime + manga pager — drop the manga page):
  `BrowseTab.kt`, `HistoriesTab.kt`, `UpdatesTab.kt`, `CategoriesTab.kt`,
  `StatsTab.kt`, `StorageTab.kt`, `DownloadsTab.kt`.
- **`HomeScreen.Tab` sealed interface** — drop the `Library` (manga) variant
  and the `Browse(toExtensions, anime)` manga branches.
- **`NavStyle` enum** — drop the `MOVE_MANGA_TO_MORE` variant entirely.
- **`StartScreen` enum** — drop the `MANGA` variant.
- **`MainActivity.handleIntentAction`** — drop `SHORTCUT_LIBRARY`,
  `SHORTCUT_MANGA`, the `INTENT_SEARCH` (manga) branch, the
  `tachiyomi://add-repo` deep-link branch, and the `.tachibk` restore branch
  is anime+manga-shared (keep).
- **`shortcuts.xml`** at `app/shortcuts.xml` — drop the
  `show_library` (manga) shortcut; rename the manga-library labels.
- **`*Preferences` classes** (shared anime+manga keys, see `09-preferences.md`):
  `LibraryPreferences`, `DownloadPreferences`, `SourcePreferences`,
  `TrackPreferences` — manga keys become dead entries; classes themselves
  stay.
- **Backup engine** — `BackupOptions`, `BackupCreator`, `BackupRestorer`,
  `models/BackupManga.kt`, `models/BackupChapter.kt`, `models/BackupTracking.kt`,
  `models/BackupHistory.kt`, `models/BackupSource.kt`, `models/BackupCategory.kt`,
  `create/creators/Manga*Creator.kt`, `restore/restorers/Manga*Restorer.kt` —
  the manga backup models and creators need removal; the format's
  `Backup.kt` aggregate needs to drop its manga field.
- **`App.onCreate`** — `MangaWidgetManager` init, `MangaCoverFetcher` /
  `MangaCoverKeyer` / `MangaKeyer` Coil3 registrations, the incognito
  broadcast receiver (which serves both anime and manga) stays.
- **`AppModule` / `DomainModule`** — many `addSingletonFactory` entries for
  manga interactors and the `AnimeDatabase`/`Database` (manga) split — the
  manga `Database` instance would be removed entirely (see `08-database.md`).
- **`AndroidManifest.xml`** — drop `ReaderActivity`, `MangaExtensionInstallService`,
  `MangaExtensionInstallActivity`, `DeepLinkMangaActivity`, the
  `tachiyomi://add-repo` intent-filter, the `.tachibk` filter stays.
- **`presentation-widget` module** — drop the entire `entries/manga/` and
  `components/manga/` trees plus `MangaWidgetManager` (instantiated in
  `App.onCreate`).
- **`source-api` module** — drop the `tachiyomi.source.manga/` and
  `tachiyomi.source/` (MangaSource hierarchy) packages.
- **`data` module's SQLDelight** — drop `sqldelight/` (manga DB) entirely,
  keeping only `sqldelightanime/` (anime DB); rename for clarity.
- **i18n** — manga-only string keys (`label_manga_library`,
  `manga_categories`, `pref_chapter_swipe_*`, etc.) become unused.
