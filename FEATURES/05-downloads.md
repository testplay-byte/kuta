# 05 — Download Manager & Offline Viewing

> Feature inventory of the Kuta download system. Anime downloads are a core
> keep feature; manga downloads are gated out (the manga Library tab is
> already removed — see Task 4 worklog entry) and marked for UI removal / data
> layer TBD. Source-state at time of writing: no source files were modified.
> Paths are relative to the repo root (`/home/z/kuta`).
>
> The anime downloader is **FFmpegKit-based**: it fetches the chosen `Video`
  URL via the source, then runs `FFmpegKit.executeWithArgumentsAsync(...)` to
  mux/transcode the stream to a `.mp4` (or `.mkv`) file on disk.

---

## Feature 1 — Anime Download Manager (Queue / Pause / Resume / Cancel)

- **Description**: The core anime download engine. Manages a persistent queue
  of `AnimeDownload` items (one per episode), downloads them sequentially
  (configurable concurrent slot count), supports pause / resume / cancel per
  item or per series, reorder by drag, "move to top/bottom", and "start now"
  to jump a specific episode to the front. Uses WorkManager
  (`AnimeDownloadJob`) for foreground-service lifecycle, FFmpegKit for the
  actual media fetch/mux, and a `MutableStateFlow<List<AnimeDownload>>` for
  the queue observable by the UI.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/data/download/anime/AnimeDownloadManager.kt` (~467 lines) — public API: `startDownloads()`, `pauseDownloads()`, `clearQueue()`, `downloadEpisodes(anime, episodes, autoStart, alt, video)`, `addDownloadsToStartOfQueue(...)`, `startDownloadNow(episodeId)`, `reorderQueue(...)`, `cancelQueuedDownloads(...)`, `deleteEpisodes(...)`, `deleteAnime(...)`, `isEpisodeDownloaded(...)`, `getDownloadCount(...)`, `getDownloadSize(...)`, `buildVideo(source, anime, episode)` (constructs a `Video` for the player from a downloaded file)
  - `app/.../data/download/anime/AnimeDownloader.kt` (~880 lines) — the actual download worker. FFmpegKit (`FFmpegKit.executeWithArgumentsAsync`), `FFprobeKit` for stream probing, builds the ffmpeg command from `Video.mpvArgs` / `ffmpegStreamArgs` / `ffmpegVideoArgs`. Supports an "alt" downloader mode (per-`AnimeDownload.changeDownloader` flag toggled when `useExternalDownloader` pref mismatches the current mode). Can delegate to an external downloader app via `Intent.ACTION_VIEW` (see `externalDownloaderSelection` pref).
  - `app/.../data/download/anime/AnimeDownloadJob.kt` — `CoroutineWorker` registered via WorkManager `OneTimeWorkRequestBuilder`. Calls `setForegroundSafely()` (data-sync foreground service type on Android Q+). Watches `networkStateFlow` + `downloadOnlyOverWifi().changes()` and stops the downloader with a notification reason if Wi-Fi is required and lost.
  - `app/.../data/download/anime/AnimeDownloadStore.kt` — persists the queue across app restarts (JSON file in app storage).
  - `app/.../data/download/anime/AnimeDownloadPendingDeleter.kt` — defers episode-file deletion until a trigger fires.
  - `app/.../data/download/anime/AnimeDownloadCache.kt` — in-memory map of which episodes are downloaded (avoids disk scans on every library render).
  - `app/.../data/download/anime/AnimeDownloadProvider.kt` — provides `UniFile` directories for source/anime/episode; path scheme `/<downloads root>/<source name>/<anime title>/<episode name>`.
  - `app/.../data/download/anime/model/AnimeDownload.kt`, `AnimeDownloadPart.kt` — queue item models (in-memory only; not DB-persisted directly, serialized via `AnimeDownloadStore`).
- **Status**: `keep` — core anime feature.
- **Dependencies**:
  - Depends on: `AnimeSourceManager` (to resolve the source for fetching
    videos); `EpisodeLoader` / `HosterLoader` (to get the `Video` to download);
    `AnimeDownloadProvider` + `AnimeDownloadCache` + `StorageManager`;
    `DownloadPreferences`; `AnimeDownloadNotifier`; WorkManager
    (`androidx.work.*`); FFmpegKit (`com.arthenica.ffmpegkit.*`).
  - Depended on by: `AnimeDownloadQueueScreenModel` (UI); `AnimeScreenModel`
    (per-episode download menu); `AnimeLibraryUpdateJob` (auto-download new
    episodes); `PlayerViewModel` (downloaded-episode branch in `EpisodeLoader.getHosters`).
- **Notes**:
  - `buildVideo(source, anime, episode)` is what the player calls when it
    detects an episode is already downloaded — returns a `Video` whose
    `videoUrl` is the local `content://` URI. The player then `loadfile`s it
    directly with no network.
  - The "alt downloader" toggle (`AnimeDownload.changeDownloader`,
    `useExternalDownloader` pref) lets the user retry a failed download with
    the opposite mode (internal FFmpeg vs external app). If the current mode
    matches the pref, the download is marked `changeDownloader=true` to retry
    with the other mode on next failure.
  - `downloadNextEpisodes()` is called from `PlayerViewModel.onSecondReached()`
    once playback passes 35% — auto-queues the next episode for download if
    `autoDownloadWhileWatching > 0`.
  - Known coupling: `AnimeDownloader` imports `EpisodeLoader` and
    `HosterLoader` from the `ui/player/loader/` package — the download
    subsystem is not fully decoupled from the player subsystem.

---

## Feature 2 — Anime Download Queue UI

- **Description**: The download queue screen, presented as a tab inside
  `DownloadsTab` (which also hosts the manga download queue tab — see
  Feature 7). Shows a grouped list (grouped by source), each item with
  progress bar, pause/cancel menu, drag-to-reorder. Top app bar has a sort
  menu (by upload date asc/desc, by episode number asc/desc) and "Cancel all"
  overflow. A FAB toggles pause/resume for the whole queue. Empty state shows
  `MR.strings.information_no_downloads`.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/ui/download/DownloadsTab.kt` — Voyager `Tab` with a 2-page `HorizontalPager` (anime page 0, manga page 1). Each page has its own actions (sort, cancel all) and FAB (pause/resume).
  - `app/.../ui/download/anime/AnimeDownloadQueueTab.kt` — Voyager sub-tab wrapper for the anime page
  - `app/.../ui/download/anime/AnimeDownloadQueueScreen.kt` — Compose screen hosting an `AndroidView` with `DownloadListBinding` (legacy RecyclerView-based adapter wrapped in Compose)
  - `app/.../ui/download/anime/AnimeDownloadQueueScreenModel.kt` — Voyager `ScreenModel` exposing `state: StateFlow<List<AnimeDownloadHeaderItem>>`, `isDownloaderRunning: StateFlow<Boolean>`, methods `reorder(...)`, `reorderQueue(selector, desc)`, `cancel(downloads)`, `clearQueue()`, `startDownloads()`, `pauseDownloads()`, `getDownloadStatusFlow()`, `getDownloadProgressFlow()`
  - `app/.../ui/download/anime/AnimeDownloadAdapter.kt` — RecyclerView adapter (drag-handle enabled, `SectionedRecyclerViewAdapter` base). Items: `AnimeDownloadHeaderItem` (per source) + `AnimeDownloadItem` (per episode).
  - `app/.../ui/download/anime/AnimeDownloadHeaderItem.kt`, `AnimeDownloadHeaderHolder.kt`, `AnimeDownloadItem.kt`, `AnimeDownloadHolder.kt` — view-binding items
- **Status**: `modify` — UI to be re-skinned. The `DownloadsTab` pager must
  also be modified to remove the manga page (see Feature 7).
- **Dependencies**:
  - Depends on: `AnimeDownloadManager` (queue state + control methods);
    `DownloadPreferences` (for `numberOfDownloads` etc.); `Notifications`
    (notification channel id for the downloader).
  - Depended on by: `MoreTab` (Downloads entry) and the in-player download
    shortcut.
- **Notes**:
  - The `AndroidView` + `DownloadListBinding` pattern is legacy — the rest of
    the app has migrated to pure Compose. This is one of the few remaining
    RecyclerView screens. A future re-skin could replace it with a pure
    Compose `LazyColumn`.
  - The `AnimeDownloadAdapter.isHandleDragEnabled = true` enables drag-to-reorder
    via `ItemTouchHelper`. The `onItemReleased(position)` callback rebuilds
    the queue order and calls `reorder(downloads)`.
  - Sort menu uses `NestedMenuItem` for "by upload date" / "by episode
    number" submenus.

---

## Feature 3 — Download Storage & Provider

- **Description**: Where downloads live on disk and how the app maps an
  `(anime, episode, source)` triple to a directory. Path scheme:
  `/<base storage>/downloads/<source name>/<anime title>/<episode name>`.
  The base storage directory is user-configurable (SAF URI in
  `StoragePreferences.baseStorageDirectory()`); on change, the
  `StorageManager` re-creates the standard subdirs (`downloads`, `local`,
  `localAnime`, `autobackup`, `mpv/{fonts,scripts,script-opts,shaders}`).
  A `.nomedia` file is placed in the downloads dir to prevent Android
  Gallery scanning.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/data/download/anime/AnimeDownloadProvider.kt` — `getAnimeDir(title, source)`, `findSourceDir(source)`, `findAnimeDir(title, source)`, `findEpisodeDir(name, scanlator, title, source)`, `findEpisodeDirs(episodes, anime, source)`, plus `getSourceDirName(source)` / `getAnimeDirName(title)` / `getValidEpisodeDirNames(name, scanlator)` helpers
  - `app/.../data/download/anime/AnimeDownloadCache.kt` — in-memory dir cache; `isEpisodeDownloaded(name, scanlator, title, sourceId, skipCache)`, `getDownloadCount(anime)`, `getDownloadSize(anime)`, `removeEpisodes(episodes, anime)`, `getTotalDownloadCount()`, `getTotalDownloadSize()`. Rooted at `StorageManager.getDownloadsDirectory()`.
  - `domain/src/main/java/tachiyomi/domain/storage/service/StorageManager.kt` — `getDownloadsDirectory()`, `getLocalAnimeSourceDirectory()`, `getMPVConfigDirectory()`, `getFontsDirectory()`, `getScriptsDirectory()`, `getShadersDirectory()`. Listens to `StoragePreferences.baseStorageDirectory().changes()` and rebuilds `baseDir` + standard subdirs.
  - Prefs: `domain/.../storage/service/StoragePreferences.kt` (likely — referenced as `storagePreferences.baseStorageDirectory()`)
  - Filesystem abstraction: `com.hippo.unifile.UniFile` (SAF-aware) throughout
- **Status**: `keep`
- **Dependencies**:
  - Depends on: `StorageManager` + `StoragePreferences`; `UniFile` (SAF);
    `DiskUtil` (`.nomedia` creation, filename sanitization).
  - Depended on by: `AnimeDownloadManager`, `AnimeDownloader` (writes files
    here), `AnimeDownloadCache`, `AnimeLibraryUpdateNotifier` (for size
    display), `StorageTab`/`AnimeStorageScreenModel` (storage usage stats).
- **Notes**:
  - Anime and manga downloads **share** the same `downloads/` root dir
    (`StorageManager.getDownloadsDirectory()` returns the same dir for both).
    The separation is by `<source name>` subdir — anime source names and
    manga source names don't collide. Removing the manga downloader won't
    require any storage-path changes.
  - `getValidEpisodeDirNames(name, scanlator)` returns multiple candidate
    directory names (with/without scanlator prefix) to handle legacy
    naming — the cache and provider look for any of them.
  - On invalid download dir (e.g. SAF URI revoked), `getAnimeDir` throws with
    a localized `MR.strings.invalid_location` message showing the
    `displayablePath` of the configured dir.

---

## Feature 4 — Download Preferences & Auto-download

- **Description**: All download-related preferences, in a single
  `DownloadPreferences` class (shared between anime and manga). Covers
  Wi-Fi-only downloads, external downloader toggle + selection, concurrent
  download slot count, download speed limit, auto-download while watching
  (after N episodes), auto-download new episodes on library update, category
  include/exclude lists for auto-download, auto-delete after read (with
  slots), delete bookmarked chapters/episodes, download fillermarked items,
  download only unseen episodes. Most prefs are anime/manga parallel; the
  manga-specific ones become dead once manga UI is removed.
- **Location**:
  - `domain/src/main/java/tachiyomi/domain/download/service/DownloadPreferences.kt` — single class for all download prefs:
    - Network: `downloadOnlyOverWifi()` (default true), `useExternalDownloader()` (default false), `externalDownloaderSelection()` (selected external downloader pkg)
    - Concurrency: `numberOfDownloads()` (default 1, max concurrent downloads), `downloadSpeedLimit()` (default 0 = unlimited)
    - Format: `saveChaptersAsCBZ()` (default true, manga-only), `splitTallImages()` (manga-only)
    - Auto-download: `autoDownloadWhileReading()` (manga), `autoDownloadWhileWatching()` (anime), `downloadNewChapters()` (manga), `downloadNewEpisodes()` (anime), `downloadNewChapterCategories()` / `downloadNewEpisodeCategories()` + `*Exclude()` variants, `downloadNewUnreadChaptersOnly()` (manga), `downloadNewUnseenEpisodesOnly()` (anime)
    - Auto-delete: `removeAfterReadSlots()` (default -1 = disabled), `removeAfterMarkedAsRead()` (default false), `removeBookmarkedChapters()` (default false), `downloadFillermarkedItems()` (default false)
    - Category filters: `removeExcludeCategories()` (manga), `removeExcludeAnimeCategories()` (anime)
  - `domain/.../download/service/DownloadPreferences.categoryPreferenceKeys` — set of pref keys used by category-sync logic
  - Settings UI: `app/.../presentation/more/settings/screen/SettingsDownloadScreen.kt` (likely)
- **Status**: `modify` — keep anime-relevant prefs; prune manga-only prefs
  (`saveChaptersAsCBZ`, `splitTallImages`, `autoDownloadWhileReading`,
  `downloadNewChapters`, `downloadNewChapterCategories*`,
  `downloadNewUnreadChaptersOnly`, `removeExcludeCategories`) once manga is
  fully removed. Shared prefs (`downloadOnlyOverWifi`,
  `useExternalDownloader`, `numberOfDownloads`, `downloadSpeedLimit`,
  `removeAfterReadSlots`, `removeAfterMarkedAsRead`, `removeBookmarkedChapters`)
  stay as-is.
- **Dependencies**:
  - Depends on: `PreferenceStore`.
  - Depended on by: `AnimeDownloadManager`, `AnimeDownloader`,
    `AnimeDownloadJob` (Wi-Fi check), `AnimeLibraryUpdateJob`
    (`downloadNewEpisodes` + category filters), `PlayerViewModel`
    (`autoDownloadWhileWatching`, `removeAfterReadSlots`,
    `downloadFillermarkedItems`), `AnimeScreenModel` (per-episode delete prefs).
- **Notes**:
  - `removeAfterReadSlots` semantics: `-1` = disabled, `0` = delete after
    finishing, `N>0` = delete after reading N more episodes (keeps a sliding
    window of N episodes downloaded).
  - `autoDownloadWhileWatching` is checked in `PlayerViewModel` after the
    35% playback threshold — if `> 0`, queues the next N episodes.
  - The pref keys `pref_download_only_over_wifi_key`,
    `pref_remove_after_marked_as_read_key`, `pref_remove_bookmarked`,
    `pref_download_fillermarked`, `download_slots`, `download_speed_limit`,
    `use_external_downloader`, `external_downloader_selection` are stable
    string keys; renaming them would require a migration.

---

## Feature 5 — Download Notifications & Foreground Service

- **Description**: While downloads are running, a persistent foreground
  notification shows current episode title + progress percentage, with a
  Pause action and a "Show anime" action. Errors (download failed) get a
  separate non-dismissable notification. The foreground service is hosted
  by `AnimeDownloadJob` (a `CoroutineWorker` that promotes itself to
  foreground via `setForegroundSafely()`).
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/data/download/anime/AnimeDownloadNotifier.kt` (~238 lines) — `onProgressChange(download)`, `onError(download, reason)`, `dismissProgress()`. Two `NotificationCompat.Builder`s: `progressNotificationBuilder` (channel `CHANNEL_DOWNLOADER_PROGRESS`, small icon `stat_sys_download`, large icon = app launcher, actions: Pause + Show Anime) and `errorNotificationBuilder` (channel `CHANNEL_DOWNLOADER_ERROR`).
  - `app/.../data/download/anime/AnimeDownloadJob.kt::getForegroundInfo()` — builds the foreground notification (`Notifications.ID_DOWNLOAD_EPISODE_PROGRESS`) with `ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC` on Android Q+.
  - `app/.../data/notification/Notifications.kt` — channel + id constants: `CHANNEL_DOWNLOADER_PROGRESS`, `CHANNEL_DOWNLOADER_ERROR`, `ID_DOWNLOAD_EPISODE_PROGRESS`
  - `app/.../data/notification/NotificationReceiver.kt` — `pauseAnimeDownloadsPendingBroadcast(context)` (Pause action target), `openAnimeEntryPendingActivity(context, animeId)` (Show Anime action target)
  - `app/.../data/notification/NotificationHandler.kt` — `openAnimeDownloadManagerPendingActivity(context)` (notification-tap target — opens `DownloadsTab`)
  - Channel setup: `app/.../App.kt::setupNotificationChannels()` (creates the channels at app startup)
- **Status**: `keep`
- **Dependencies**:
  - Depends on: `Notifications` channel/id constants; `NotificationReceiver`
    (handles Pause broadcast); `NotificationHandler` (handles tap);
    `SecurityPreferences` (for `hideNotificationContent` if applicable —
    imported in `AnimeDownloadNotifier`).
  - Depended on by: `AnimeDownloader` (instantiates and calls
    `notifier.onProgressChange` / `onError`).
- **Notes**:
  - Manga has a parallel `MangaDownloadNotifier` / `MangaDownloadJob`. They
    use the **same** notification channels (`CHANNEL_DOWNLOADER_PROGRESS` /
    `CHANNEL_DOWNLOADER_ERROR`) but different notification IDs — removing
    the manga downloader won't require channel changes.
  - The Pause action sends a broadcast to `NotificationReceiver`, which
    calls `AnimeDownloadManager.pauseDownloads()` — there's no separate
    "Pause" service intent; it goes through the receiver.
  - `setOnlyAlertOnce(true)` on the progress builder prevents the
    notification from chiming on every percent update.
  - On Android 14+ the `FOREGROUND_SERVICE_TYPE_DATA_SYNC` declaration
    requires the `FOREGROUND_SERVICE_DATA_SYNC` permission in
    `AndroidManifest.xml` — verify it's present if it isn't already.

---

## Feature 6 — Downloaded-only Mode & Incognito Mode

- **Description**: Two global app-state toggles (not download-specific):
  (a) **Downloaded-only mode** — when on, the library and browse screens
  filter to only show entries/episodes that are already downloaded; no new
  network fetches for catalogs. Useful for offline viewing. (b) **Incognito
  mode** — when on, playback progress is not saved to the DB and tracker
  updates are suppressed (unless the anime has trackers linked, in which
  case trackers still update — see `PlayerViewModel.onSecondReached`:
  `shouldTrack = !incognitoMode || hasTrackers`).
- **Location**:
  - `app/src/main/java/eu/kanade/domain/base/BasePreferences.kt`:
    - `downloadedOnly()` — `Preference.appStateKey("pref_downloaded_only")`, default false
    - `incognitoMode()` — `Preference.appStateKey("incognito_mode")`, default false
  - Consumers:
    - `AnimeLibraryScreenModel` / `AnimeLibrarySettingsScreenModel` (filter library by downloaded-only)
    - `BrowseAnimeSourceScreenModel` (filter browse list by downloaded-only)
    - `PlayerViewModel.onSecondReached()` / `saveWatchingProgress()` (incognito gating)
    - `ReaderViewModel` (manga mirror)
    - More menu / quick-toggle in `MoreTab` and the in-library toolbar
- **Status**: `keep`
- **Dependencies**:
  - Depends on: `PreferenceStore` (with `appStateKey` prefix so it's not
    backed up); `AnimeDownloadCache` (downloaded-only filter checks the cache).
  - Depended on by: library / browse / player / reader.
- **Notes**:
  - These are `appStateKey` prefs — they are **not** included in backups.
    This is intentional: a restored backup shouldn't restore "incognito" or
    "downloaded-only" state.
  - Incognito is also visually indicated (a banner / icon in the toolbar)
  across library, browse, and player.
  - The `hasTrackers` carve-out for incognito means that if a user is
    tracking an anime, watching it in incognito still updates the tracker
    (but not the local DB) — this is intentional behavior to avoid losing
    tracker progress, but is a known UX surprise.

---

## Feature 7 — Manga Download Manager (Gated — UI Remove / Data TBD)

- **Description**: The manga-side parallel of Feature 1 — manages a queue of
  `MangaDownload` items (one per chapter), downloads page images, packs them
  into CBZ files (`saveChaptersAsCBZ` pref), supports the same
  pause/resume/cancel/reorder operations. The manga Library tab is already
  gated out (Phase 1, Task 4), so this UI is currently reachable only via the
  manga page of `DownloadsTab` and via per-chapter menus in `MangaScreen`
  (also gated out). The data layer (`MangaDownloadManager`, `MangaDownloader`,
  `MangaDownloadJob`, `MangaDownloadProvider`, `MangaDownloadCache`,
  `MangaDownloadStore`, `MangaDownloadPendingDeleter`) is fully intact.
- **Location**:
  - UI (gate out / remove):
    - `app/src/main/java/eu/kanade/tachiyomi/ui/download/manga/MangaDownloadQueueTab.kt`
    - `app/.../ui/download/manga/MangaDownloadQueueScreen.kt` + `MangaDownloadQueueScreenModel.kt`
    - `app/.../ui/download/manga/MangaDownloadAdapter.kt`, `MangaDownloadHeaderItem.kt`, `MangaDownloadHeaderHolder.kt`, `MangaDownloadItem.kt`, `MangaDownloadHolder.kt`
    - `app/.../ui/download/DownloadsTab.kt` (the `HorizontalPager` has 2 pages — page 1 is manga; remove it)
  - Data layer (TBD — keep for now, remove with manga cleanup):
    - `app/.../data/download/manga/MangaDownloadManager.kt`, `MangaDownloader.kt`, `MangaDownloadJob.kt`, `MangaDownloadProvider.kt`, `MangaDownloadCache.kt`, `MangaDownloadStore.kt`, `MangaDownloadPendingDeleter.kt`, `model/MangaDownload.kt`, `MangaDownloadNotifier.kt`
- **Status**: `remove` (UI) / `TBD` (data layer — defer to the broader manga-removal pass)
- **Dependencies**:
  - Depends on: `MangaSourceManager` (gated out, but data layer still
    present); `MangaDownloadProvider` + `MangaDownloadCache` +
    `StorageManager` (shared with anime); `DownloadPreferences` (shared).
  - Depended on by: `MangaScreen` (gated out), `MangaLibraryUpdateJob`
    (gated out), `ReaderViewModel` (gated out), `DownloadsTab` (the manga page).
- **Notes**:
  - The fastest Phase-2 win: modify `DownloadsTab.kt` to remove the manga
    page from the `HorizontalPager` (change `rememberPagerState { 2 }` to
    `{ 1 }` and drop the manga tab + manga actions). This makes the manga
    download UI unreachable without touching the data layer.
  - Full removal (deleting the `data/download/manga/` package) should be
    deferred to the same pass that deletes the rest of the manga data layer
    (manga DB, manga source API, manga extensions) — see
    `DOCS/architecture/12-animiru-diff.md` for the physical-removal
    precedent.
  - `MangaDownloadProvider` shares the same `downloads/` root as
    `AnimeDownloadProvider` — its files would persist on disk after code
    removal unless the user manually deletes them. A migration that scans
    for orphaned manga-source subdirs and prompts the user to delete them
    would be polite but not required.
