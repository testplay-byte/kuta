# 01 — Anime Library, Categories, Updates

> Feature inventory of the anime Library tab, category management, and the
> "new episode" Updates feed. Scope: research/documentation only — no source
> files modified. All paths relative to `/home/z/kuta`.

---

### Anime Library Tab

- **Description**: The primary "Library" bottom-nav tab. Shows the user's
  favorited anime as a paginated, swipeable per-category list/grid. Provides a
  search box that filters the library in-place, a "global search" jump-off, a
  pull-to-refresh per category / global update trigger, a random-anime button,
  multi-select with bottom action sheet (mark seen, download, change category,
  delete), and a settings sheet (filter/sort/display per category). Tapping an
  anime pushes `AnimeScreen`; the optional "Continue watching" button calls
  `GetNextEpisodes` and launches the player directly.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/ui/library/anime/AnimeLibraryTab.kt`
    (Voyager `Tab` + `Content()` composable)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/library/anime/AnimeLibraryScreenModel.kt`
    (`StateScreenModel`; owns filter+sort pipeline, search query, selection)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/library/anime/AnimeLibrarySettingsScreenModel.kt`
    (display/sort/filter mutators for the settings sheet)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/library/anime/AnimeLibraryItem.kt`
    (per-anime presentation wrapper)
  - `app/src/main/java/eu/kanade/presentation/library/anime/` (`AnimeLibraryContent`,
    `AnimeLibraryPager`, `AnimeLibraryComfortableGrid`, `AnimeLibraryCompactGrid`,
    `AnimeLibraryList`, `AnimeLibrarySettingsDialog`)
  - `app/src/main/java/eu/kanade/presentation/library/components/LibraryToolbar.kt`,
    `LibraryBadges.kt`, `LazyLibraryGrid.kt`
  - Data layer: `data/src/main/sqldelightanime/view/animelibView.sq` (the
    `animelibView` view drives `GetLibraryAnime`); repository
    `data/.../entries/anime/AnimeRepositoryImpl.kt`; interactor
    `domain/.../entries/anime/interactor/GetLibraryAnime.kt`
  - Preferences: `domain/.../library/service/LibraryPreferences.kt`
    (`displayMode`, `animeSortingMode`, `animePortrait/LandscapeColumns`,
    `showContinueViewingButton`, `categoryTabs`, `categoryNumberOfItems`, etc.)
- **Status**: `modify`
  - The feature is core and definitely staying, but per Phase 1 the home
    surface is being redesigned; the library tab UI is slated for a visual
    overhaul. Underlying screen-model/repository/prefs logic stays.
- **Dependencies**:
  - Depends on: `AnimeSourceManager` (cover/thumbnail loading), `GetLibraryAnime`,
    `GetVisibleAnimeCategories`, `AnimeDownloadCache` + `AnimeDownloadManager`
    (download badges & download-action), `TrackerManager` (tracking filter),
    `PlayerPreferences.alwaysUseExternalPlayer()` (continue-watching launch),
    `AnimeLibraryUpdateJob.startNow(...)` (refresh).
  - Depended on by: `HomeScreen` (one of 6 tabs), `AnimeScreen` (push target),
    `GlobalAnimeSearchScreen` (pushed from library search), `CategoriesTab`
    (pushed from "edit categories" in change-category dialog), deep-link
    shortcut `Constants.SHORTCUT_ANIME`.
- **Notes**:
  - Phase 1 already removed `MangaLibraryTab` from the bottom nav
    (see worklog Task 4). The manga twin files
    (`ui/library/manga/MangaLibraryTab.kt` + `presentation/library/manga/*`)
    remain on disk and load into the build but are never instantiated.
  - `AnimeLibraryScreenModel.search(query)` is exposed via the `queryEvent`
    `Channel` on the singleton `AnimeLibraryTab` object — `HomeScreen.search()`
    and `MainActivity.handleIntentAction` use it to drive library search from
    outside Compose.
  - Selection-mode bottom action sheet hard-codes `isManga = false` — this
    flag is presentation-only (controls a label or two) and will be safe to
    drop in the redesign.

---

### Library Filter / Sort / Display Settings Sheet

- **Description**: A 3-tab bottom sheet opened from the toolbar "filter" icon
  (or by re-selecting the Library tab). Per-category (or globally) the user
  can: filter by downloaded / unseen / started / bookmarked / completed /
  tracking / outside-release-period; sort by title, last seen, last checked,
  unseen count, total episodes, latest upload, score, tracker score; choose
  display mode (list / compact grid / comfortable grid) + badges (download,
  unseen, local, language) + column count + "show continue watching button".
  All persisted through `LibraryPreferences` and `Category.flags`.
- **Location**:
  - `app/src/main/java/eu/kanade/presentation/library/anime/AnimeLibrarySettingsDialog.kt`
    (`FilterPage`, `SortPage`, `DisplayPage` composables)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/library/anime/AnimeLibrarySettingsScreenModel.kt`
    (`toggleFilter`, `setDisplayMode`, `setSort`, `toggleTracker`)
  - `domain/.../library/anime/model/AnimeLibrarySort.kt` (sort enum + serializer)
  - `domain/.../library/model/LibraryDisplayMode.kt` (display mode enum)
  - `domain/.../category/anime/interactor/SetSortModeForAnimeCategory.kt`,
    `SetAnimeDisplayMode.kt` (per-category writes; falls through to global
    `LibraryPreferences.animeSortingMode()` when `category == null`)
  - Preference keys (all in `LibraryPreferences.kt`): `pref_filter_animelib_*_v2`
    (TriState per filter), `display_*_badge`, `pref_animelib_columns_{portrait,landscape}_key`,
    `display_continue_reading_button`, `display_category_tabs`,
    `display_number_of_items`, `categorized_display`.
- **Status**: `modify`
- **Dependencies**:
  - Depends on: `LibraryPreferences`, `SetSortModeForAnimeCategory`,
    `SetAnimeDisplayMode`, `TrackerManager.loggedInTrackersFlow()` (for the
    per-tracker TriState filter chips).
  - Depended on by: `AnimeLibraryTab` (opens via `requestOpenSettingsSheet()` /
    `onReselect`).
- **Notes**:
  - Per-category display/sort is gated by `categorizedDisplaySettings()`
    (key `categorized_display`); when off, all categories share the global sort
    mode (so `SetSortModeForAnimeCategory` writes only the global pref).
  - `LibraryPreferences` is *co-located* — anime and manga keys live in the
    same class disambiguated by string only (`pref_filter_animelib_*_v2` vs
    `pref_filter_library_*_v2`). Removing manga UI leaves dead keys, but no
    code change needed.
  - Sort types include `AnimeLibrarySort.Type.Score` and `TrackerScore` which
    average tracker scores per anime — implemented inline in
    `AnimeLibraryScreenModel.applySort`.

---

### Categories (Anime)

- **Description**: User-defined library categories ("Watching", "Plan to
  watch", …). Created/renamed/reordered/hidden/deleted from a dedicated
  screen reachable via the library's change-category dialog "Edit categories"
  action. Categories drive the library's swipeable pager (`categoryTabs`), the
  per-category sort/display flags, the default category for new favorites, and
  the include/exclude lists for library-update jobs.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/ui/category/CategoriesTab.kt`
    (Voyager `Tab` — pushed, not in bottom nav; swipeable anime/manga pager)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/category/anime/AnimeCategoryTab.kt`
    + `AnimeCategoryScreenModel.kt` (CRUD via `CreateAnimeCategoryWithName`,
    `RenameAnimeCategory`, `ReorderAnimeCategory`, `HideAnimeCategory`,
    `DeleteAnimeCategory`)
  - `app/src/main/java/eu/kanade/presentation/category/components/` (dialogs)
  - DB: `data/src/main/sqldelightanime/dataanime/categories.sq` (table +
    `_id=0` system "Default" row + `system_category_delete_trigger`)
  - Join: `data/src/main/sqldelightanime/dataanime/animes_categories.sq`
    (FK CASCADE both sides; trigger bumps anime `version`)
  - Repos/interactors: `data/.../category/anime/AnimeCategoryRepositoryImpl.kt`,
    `domain/.../category/anime/interactor/*` (Create/Rename/Reorder/Hide/Delete/
    GetVisible/Get/SetAnimeCategories)
  - Domain model: `domain/.../category/model/Category.kt` (single shared
    `Category` class; anime/manga distinction is at the repo level)
  - Preferences: `LibraryPreferences.defaultAnimeCategory()`,
    `animeUpdateCategories()`, `animeUpdateCategoriesExclude()`,
    `lastUsedAnimeCategory()`, `hideHiddenCategoriesSettings()`
- **Status**: `modify`
  - The shared `CategoriesTab` is currently a swipeable anime/manga pager —
    in an anime-only redesign the manga tab should be dropped.
- **Dependencies**:
  - Depends on: anime DB (`categories`, `animes_categories` tables),
    `SetAnimeCategories` (used when assigning anime from library or browse
    screens).
  - Depended on by: `AnimeLibraryTab` (category pager + change-category
    dialog), `BrowseAnimeSourceScreenModel.addFavorite` (default category
    resolution), `AnimeHistoryScreenModel.addFavorite` (same), library-update
    job (include/exclude category sets).
- **Notes**:
  - One `Category` class is shared across anime and manga — see
    `DOCS/architecture/10-data-models.md` §7. Removing the manga UI does not
    require touching this class.
  - `CategoriesTab.options.index = 7u` — it's not in `NavStyle.tabs`, so it's
    reachable only by being pushed. Already gated correctly post-Phase 1.
  - System "Default" category (`_id = 0`) is seeded by the `.sq` file and
    protected from deletion by a SQL trigger; the screen-model filters it out
    via `Category::isSystemCategory`.

---

### Updates Feed (New Episodes)

- **Description**: The "Updates" bottom-nav tab. Shows recently-fetched new
  episodes for favorited anime, grouped by day, with cover, episode number,
  download-state badge and unseen indicator. Multi-select supports bookmark,
  fillermark, mark-seen, download, delete; a single tap launches the player.
  The "View upcoming" action jumps to a calendar of scheduled future episodes.
  Also surfaces the "new updates" count badge that the library-update job
  increments.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/ui/updates/UpdatesTab.kt`
    (Voyager `Tab`, swipeable anime/manga pager via `TabbedScreen`)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/updates/anime/AnimeUpdatesTab.kt`
    (TabContent: screen wiring, action-bar items, dialogs)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/updates/anime/AnimeUpdatesScreenModel.kt`
    (subscribes to last-3-months window of `GetAnimeUpdates`; download-state
    provider; selection; mark seen/bookmark/fillermark; download/delete;
    `resetNewUpdatesCount()` on dispose)
  - `app/src/main/java/eu/kanade/presentation/updates/anime/AnimeUpdateScreen.kt`
  - DB view: `data/src/main/sqldelightanime/view/animeupdatesView.sq`
    (`favorite=1 AND date_fetch > date_added`, ordered by `date_fetch DESC`)
  - Repository/interactor: `data/.../updates/anime/AnimeUpdatesRepositoryImpl.kt`,
    `domain/.../updates/anime/interactor/GetAnimeUpdates.kt`
  - Domain model: `domain/.../updates/anime/model/AnimeUpdatesWithRelations.kt`
  - Calendar: `mihon.feature.upcoming.anime.UpcomingAnimeScreen` (separate
    feature module)
  - Counter prefs: `LibraryPreferences.newAnimeUpdatesCount()`
    (`library_unseen_updates_count`)
- **Status**: `modify`
  - Like other tabs, currently a swipeable anime+manga `TabbedScreen`. In the
    anime-only redesign the manga sub-tab should be dropped (the manga twin
    files `ui/updates/manga/*` are loaded but unreachable post-Phase 1 except
    via this pager).
- **Dependencies**:
  - Depends on: `GetAnimeUpdates.subscribe(limit)` (the `animeupdatesView`),
    `AnimeDownloadManager` + `AnimeDownloadCache` (download badge + actions),
    `SetSeenStatus`, `UpdateEpisode`, `GetEpisode`, `GetAnime`,
    `AnimeLibraryUpdateJob.startNow(...)` (toolbar "update library" action),
    `PlayerPreferences.alwaysUseExternalPlayer()`.
  - Depended on by: `HomeScreen` (one of 6 tabs), `Notifications` tap-through
    (notification → Updates tab), `LibraryPreferences.newAnimeUpdatesCount`
    reset side-effect.
- **Notes**:
  - The Updates window is hard-coded to "now minus 3 months" in
    `AnimeUpdatesScreenModel.init { ZonedDateTime.now().minusMonths(3) }`.
  - `resetNewUpdatesCount()` is called both on enter and on dispose — i.e. the
    "new" badge clears as soon as the user views the Updates tab.
  - `UpdatesTab.onReselect` pushes `DownloadsTab` (long-press the Updates tab
    to jump to the download queue).
  - The `AnimeUpdatesItem` carries `downloadStateProvider` / `downloadProgressProvider`
    as lambdas so the list reflects live download state without recomposing the
    whole row.

---

### Library Update Job (Auto + Manual)

- **Description**: A `WorkManager`-based background job that iterates every
  favorited anime, calls `source.getEpisodeList()` + `syncEpisodesWithSource`,
  optionally refreshes metadata via `source.getAnimeDetails()`, queues
  auto-downloads, and fires notifications for new episodes / errors. Runs on a
  periodic schedule (user-chosen interval, default off) with device
  restrictions (wifi / unmetered / charging) AND per-item restrictions
  (skip completed, skip not-started, skip not-caught-up, skip
  outside-release-period). Manually triggerable per-category or globally from
  the library/updates toolbars.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/data/library/anime/AnimeLibraryUpdateJob.kt`
    (`CoroutineWorker`; `setupTask` enqueues periodic, `startNow` enqueues
    one-time, `stop` cancels, `cancelAllWorks`)
  - `app/src/main/java/eu/kanade/tachiyomi/data/library/anime/AnimeLibraryUpdateNotifier.kt`
    (progress + per-anime new-episode + error notifications)
  - `app/src/main/java/eu/kanade/tachiyomi/data/library/anime/AnimeMetadataUpdateJob.kt`
    (separate worker — only refreshes covers/backgrounds/details when
    `LibraryPreferences.autoUpdateMetadata()` is on; triggered by the
    library-update job)
  - Companion manga twins: `data/library/manga/MangaLibraryUpdateJob.kt` etc.
  - WorkManager tags: `"AnimeLibraryUpdate"` (both), `"AnimeLibraryUpdate-auto"`,
    `"AnimeLibraryUpdate-manual"`.
  - Concurrency: `Semaphore(5)` per source-group, `CopyOnWriteArrayList` for
    currently-updating anime, atomic counters for progress.
  - Settings UI: `app/src/main/java/eu/kanade/presentation/more/settings/screen/SettingsLibraryScreen.kt`
    (interval picker, device restrictions, item restrictions, metadata
    auto-update, categories to include/exclude)
- **Status**: `keep`
- **Dependencies**:
  - Depends on: `AnimeSourceManager.getOrStub(source)`, `GetLibraryAnime`,
    `GetAnime`, `UpdateAnime`, `SyncEpisodesWithSource`,
    `AnimeFetchInterval.getWindow(now)` (release-period gating),
    `FilterEpisodesForDownload` (auto-download selection),
    `GetAnimeSeasonsByParentId` (when `updateSeasonOnLibraryUpdate()` is on),
    `AnimeDownloadManager.startDownloads()` (kicks queue after new episodes),
    `AnimeCoverCache` + `AnimeBackgroundCache` (metadata refresh).
  - Depended on by: `AnimeLibraryTab.onClickRefresh` + `onClickGlobalUpdate`,
    `AnimeUpdatesScreenModel.updateLibrary`, `SettingsLibraryScreen`
    (re-schedules via `setupTask` on interval change), `App.onCreate` /
    `PreferenceChangesObserver` (reschedules on pref change).
- **Notes**:
  - The manga twin (`MangaLibraryUpdateJob`) is a fully parallel worker that
    runs against the manga DB. Post-Phase-1 it's still wired in
    `SettingsLibraryScreen` and will still execute on its schedule if the user
    has manga favorites. Candidate for gating along with the manga library UI.
  - `setupTask` uses `NetworkRequest.Builder` (Android 9+) plus a fallback
    `NetworkType` for older devices — both gated by the
    `autoUpdateDeviceRestrictions()` set.
  - Error log: writes `aniyomi_update_errors.txt` to cache dir grouped by
    error → source → anime; surfaced via a notification with a content URI.
  - `AnimeFetchInterval.getWindow(ZonedDateTime.now())` returns
    `(lowerBound, upperBound)` used by both the skip-outside-release-period
    filter and the per-anime next-update estimator.
  - The "queue size warning" notification fires when any single source has
    more than `ANIME_PER_SOURCE_QUEUE_WARNING_THRESHOLD = 60` anime queued —
    protects against the user accidentally hammering a source.

---

### Library Update Settings (Scheduling + Restrictions)

- **Description**: Settings screen section governing the library update job:
  update interval (off / 1h / 2h / 6h / 12h / 24h / 48h), device restrictions
  (wifi-only / not-metered / charging), per-item skip rules (completed /
  has-unwatched / not-started / outside-release-period), auto-update metadata
  toggle, include/exclude category pickers, and "show updates count badge"
  toggle. Also re-schedules the WorkManager job whenever the interval or
  restrictions change.
- **Location**:
  - `app/src/main/java/eu/kanade/presentation/more/settings/screen/SettingsLibraryScreen.kt`
    (+ its `SettingsLibraryScreenModel`)
  - `domain/.../library/service/LibraryPreferences.kt` — keys: `autoUpdateInterval`
    (`pref_library_update_interval_key`, default 0 = off),
    `autoUpdateDeviceRestrictions` (`library_update_restriction`, default
    `{wifi}`), `autoUpdateItemRestrictions` (`library_update_manga_restriction`,
    default all 4 enabled), `autoUpdateMetadata` (`auto_update_metadata`,
    default false), `animeUpdateCategories` / `animeUpdateCategoriesExclude`,
    `newShowUpdatesCount` (`library_show_updates_count`),
    `updateSeasonOnLibraryUpdate` (`pref_update_season_on_library_update`).
  - Reschedule trigger: `LibraryPreferences.autoUpdateInterval().changes()`
    observed in `SettingsLibraryScreenModel` → `AnimeLibraryUpdateJob.setupTask(context, interval)`.
- **Status**: `TBD`
  - The screen still surfaces manga-side update settings (manga categories,
    manga update interval keys) alongside anime ones. Needs user decision on
    whether to hide manga settings from the UI or fully remove the manga
    schedule worker.
- **Dependencies**:
  - Depends on: `LibraryPreferences`, `AnimeLibraryUpdateJob.setupTask/stop`,
    `GetAnimeCategories` / `GetMangaCategories` (for the include/exclude
    multi-select dialog).
  - Depended on by: every consumer of the auto-update schedule.
- **Notes**:
  - Despite the manga UI being gated in Phase 1, the manga update worker
    (`MangaLibraryUpdateJob.setupTask`) is still scheduled by this screen
    based on the shared `autoUpdateInterval()` preference (the interval
    pref is *not* anime/manga-split). Removing the manga worker is a separate
    code change.
  - The "default category" picker (`defaultAnimeCategory` /
    `defaultMangaCategory`) also lives on this screen — anime version keys
    `DEFAULT_ANIME_CATEGORY_PREF_KEY = "default_anime_category"` (default -1
    = always ask, 0 = "Default" system category).
  - `LibraryPreferences.categoryPreferenceKeys` is a `Set` of all 6
    category-related pref keys (3 anime + 3 manga) used by backup/restore to
    decide which prefs to include.
