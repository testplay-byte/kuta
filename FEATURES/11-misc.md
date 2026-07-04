# 11 — Misc Features

Catch-all for cross-cutting and standalone features that don't fit cleanly
into the other inventory files. Several are infrastructure pieces (crash
logs, deep links, app shortcuts, backup engine) that the rest of the app
builds on; others are leaf user-facing screens (stats, storage usage,
categories management) reached from the `MoreTab`.

> Status legend: `keep` = unchanged, `modify` = kept but redesigned,
> `remove` = dropped, `TBD` = pending user decision.

---

### Crash Logging & Error Reporting

- **Description**: A global `Thread.UncaughtExceptionHandler` that captures
  any uncaught exception, serializes it to JSON (`ThrowableSerializer`),
  launches `CrashActivity` in a separate `:error_handler` process (declared
  in the manifest), then re-throws to the default handler so the OS still
  kills the process. `CrashActivity` renders a Compose `CrashScreen` with
  the stack trace and a "Restart app" button (`finishAffinity()` + relaunch
  `MainActivity`). Separately, `CrashLogUtil.dumpLogs()` (invoked from
  Settings → Advanced → "Dump crash logs") writes a human-readable log file
  to external cache; `CrashLogUtil.getDebugInfo()` (invoked from the
  About-screen version row tap) returns a clipboard-friendly debug summary.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/crash/GlobalExceptionHandler.kt`
    (installed in `App.onCreate` via
    `GlobalExceptionHandler.initialize(applicationContext, CrashActivity::class.java)`).
  - `app/src/main/java/eu/kanade/tachiyomi/crash/CrashActivity.kt`.
  - Manifest: `AndroidManifest.xml` line ~120 —
    `<activity android:name=".crash.CrashActivity" android:process=":error_handler" android:exported="false"/>`.
  - `CrashLogUtil` (used by `SettingsAdvancedScreen` + `AboutScreen`) —
    located in `app/src/main/java/eu/kanade/tachiyomi/util/`.
  - Compose UI: `app/src/main/java/eu/kanade/presentation/crash/CrashScreen.kt`.
- **Status**: `keep`.
- **Dependencies**: App process split: `:error_handler` runs in a separate
  process so a main-process crash can still show the UI. `WebView.setDataDirectorySuffix`
  is set in `App.onCreate` for non-main processes specifically to avoid the
  Android P+ crash when `:error_handler` touches WebView.
- **Notes**: This is **not** ACRA / Bugsnag / Sentry — it's a self-contained
  local crash handler. There's no automatic remote upload; the user must
  manually share the dump file.

---

### Deep Links

- **Description**: Three deep-link families routed through `MainActivity`.
  (1) **`aniyomi://add-repo?url=<URL>`** — opens `AnimeExtensionReposScreen`
  pre-filled with the supplied repo URL. (2) **`tachiyomi://add-repo?url=<URL>`**
  — opens `MangaExtensionReposScreen` (note: the legacy `tachiyomi://` scheme
  is manga-side). Both schemes are registered as intent-filters on
  `MainActivity`. (3) **`.tachibk` file open** — `ACTION_VIEW` with
  `pathPattern=".*\\.tachibk"` (plus several nested-dot variants because
  Android's path matcher is per-segment) → `RestoreBackupScreen(intent.data.toString())`.
  Plus the OAuth callback scheme (see Tracker auth below).
- **Location**:
  - Manifest intent-filters: `AndroidManifest.xml` lines ~50-110 (add-repo
    for both schemes + `.tachibk` file open).
  - Routing: `MainActivity.handleIntentAction(...)` at lines 539-559 in
    `app/src/main/java/eu/kanade/tachiyomi/ui/main/MainActivity.kt`.
  - Target screens:
    `app/src/main/java/eu/kanade/presentation/more/settings/screen/browse/AnimeExtensionReposScreen.kt`,
    `MangaExtensionReposScreen.kt`,
    `app/src/main/java/eu/kanade/presentation/more/settings/screen/data/RestoreBackupScreen.kt`.
- **Status**: `modify` — keep `aniyomi://` (anime) and `.tachibk`;
  `tachiyomi://add-repo` (manga) follows the manga decision.
- **Dependencies**: `SearchableSettings.highlightKey` (used by restore path
  but not by deep-link path — deep links push directly into
  `RestoreBackupScreen`).
- **Notes**: Android's `pathPattern` matcher doesn't cross literal `.`s, so
  the manifest has 7 escalating patterns (`.*\\.tachibk`, `.*\\..*\\.tachibk`,
  …) to cover nested-dot filenames like `My.backup.tachibk`.

---

### Tracker OAuth Callbacks

- **Description**: Separate from the add-repo deep links, OAuth flow for
  tracker login. The browser flow redirects to
  `aniyomi://<service>-auth?code=<CODE>` (or `#access_token=<TOKEN>` for
  Anilist). `TrackLoginActivity` (a `BaseOAuthLoginActivity` subclass)
  consumes the URI, extracts the code/token, calls the relevant tracker's
  `login(...)` suspend function, and returns to the previous screen. Handles
  5 services: `myanimelist-auth`, `anilist-auth`, `bangumi-auth`,
  `shikimori-auth`, `simkl-auth`.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/ui/setting/track/TrackLoginActivity.kt`.
  - `app/src/main/java/eu/kanade/tachiyomi/ui/setting/track/BaseOAuthLoginActivity.kt`.
  - Manifest: `AndroidManifest.xml` lines ~231-249 — single activity with 5
    `<data android:host="*-auth"/>` entries under `android:scheme="aniyomi"`.
  - Triggered from: `SettingsTrackingScreen` tracker-login rows open
    `<service>Api.authUrl()` in the browser via `context.openInBrowser(...,
    forceDefaultBrowser = true)`.
- **Status**: `keep` — both anime and manga use these trackers; the callback
  scheme is `aniyomi://` (still upstream-branding — keep as-is for compat).
- **Dependencies**: `TrackerManager` (Injekt) —
  `trackerManager.{aniList, myAnimeList, bangumi, shikimori, simkl}.login(...)`.
- **Notes**: Kitsu and MangaUpdates don't use OAuth — they use a
  username/password dialog (`TrackingLoginDialog` in
  `SettingsTrackingScreen`). Komga/Kavita/Suwayomi/Jellyfin are enhanced
  trackers with their own login (source-bound, no OAuth).

---

### App Shortcuts (launcher long-press)

- **Description**: Five static launcher shortcuts defined in `shortcuts.xml`,
  processed by the `com.github.zellius.shortcut-helper` Gradle plugin (which
  rewrites `android:shortcutId` / `android:action` strings to be
  locale-stable). Shortcuts: (1) "Manga Library" —
  `eu.kanade.tachiyomi.SHOW_LIBRARY` (gated — see below); (2) "Anime
  Library" — `SHOW_ANIMELIB`; (3) "Recently Updated" — `SHOW_RECENTLY_UPDATED`;
  (4) "Recently Read" — `SHOW_RECENTLY_READ` (note: duplicate
  `shortcutId="show_recently_updated"` is a bug upstream — both IDs match,
  Android silently drops the duplicate); (5) "Browse" — `SHOW_CATALOGUES`.
  All target `MainActivity` with custom actions, routed in
  `MainActivity.handleIntentAction` via the `Constants.SHORTCUT_*` map.
- **Location**:
  - `app/shortcuts.xml` (root of `:app` module — the shortcut-helper plugin
    reads it via `shortcutHelper.setFilePath("./shortcuts.xml")` in
    `app/build.gradle.kts:14`).
  - Manifest meta-data: `AndroidManifest.xml` line ~115 —
    `<meta-data android:name="android.app.shortcuts" android:resource="@xml/shortcuts"/>`.
    (The `@xml/shortcuts` resource is generated by the shortcut-helper
    plugin from `app/shortcuts.xml`.)
  - Routing: `MainActivity.handleIntentAction(...)` lines 467-492 —
    `Constants.SHORTCUT_ANIMELIB`, `SHORTCUT_LIBRARY` (manga),
    `SHORTCUT_ANIME`, `SHORTCUT_MANGA`, `SHORTCUT_UPDATES`,
    `SHORTCUT_HISTORY`, `SHORTCUT_SOURCES`, `SHORTCUT_EXTENSIONS`,
    `SHORTCUT_ANIMEEXTENSIONS`, `SHORTCUT_DOWNLOADS`, `SHORTCUT_ANIME_DOWNLOADS`.
  - `Constants` class lives in `core/common/src/main/java/tachiyomi/core/common/Constants.kt`.
- **Status**: `modify` — keep; drop the "Manga Library" shortcut
  (`show_library` / `SHORTCUT_LIBRARY` / `SHORTCUT_MANGA`) once manga is
  decided. Fix the duplicate `shortcutId` bug while we're here.
- **Dependencies**: `MainActivity.handleIntentAction` +
  `HomeScreen.openTab(...)` (the actual tab-switch happens via the
  process-singleton `HomeScreen.openTabEvent` channel).
- **Notes**: `SHORTCUT_ANIME` and `SHORTCUT_MANGA` (with an `ANIME_EXTRA` /
  `MANGA_EXTRA` long) open a specific entry's detail screen — used when the
  user pins a library item to the home screen (long-press → "Add to home
  screen" from `MangaScreen` / `AnimeScreen`).

---

### Widgets (Glance home-screen widgets)

- **Description**: A separate `:presentation-widget` Gradle module shipping
  four Glance app widgets — `AnimeUpdatesGridGlanceWidget`,
  `AnimeUpdatesGridCoverScreenGlanceWidget` (lock-screen / cover-screen
  variant), `MangaUpdatesGridGlanceWidget`,
  `MangaUpdatesGridCoverScreenGlanceWidget`. Each shows a 4×2 grid of
  recent-unseen updates' cover art; tapping an item opens the corresponding
  entry. Two widget-info XMLs declare the home-screen + lock-screen +
  Samsung cover-screen placement options. When app-lock is enabled, a
  "Locked*" widget variant is shown instead (covers blurred + a lock icon),
  so sensitive covers aren't visible while the app is locked.
- **Location**:
  - Module: `presentation-widget/` (declared in `settings.gradle.kts`).
  - Receivers: `presentation-widget/src/main/java/tachiyomi/presentation/widget/entries/anime/`
    (`AnimeUpdatesGridGlanceReceiver.kt`,
    `AnimeUpdatesGridCoverScreenGlanceReceiver.kt`,
    `AnimeUpdatesGridGlanceWidget.kt`,
    `AnimeUpdatesGridCoverScreenGlanceWidget.kt`,
    `BaseAnimeUpdatesGridGlanceWidget.kt`, `AnimeWidgetManager.kt`) and
    the parallel `entries/manga/` tree.
  - Managers wired in `App.onCreate` lines 151-158:
    `MangaWidgetManager(Injekt.get(), Injekt.get()).init(...)` and
    `AnimeWidgetManager(Injekt.get(), Injekt.get()).init(...)`.
  - Locked variants: `components/anime/LockedAnimeWidget.kt`,
    `components/manga/LockedMangaWidget.kt`.
  - Widget-info XMLs: `presentation-widget/src/main/res/xml/`
    `updates_grid_homescreen_widget_info.xml`,
    `updates_grid_lockscreen_widget_info.xml`,
    `updates_grid_samsung_cover_widget_info.xml`.
- **Status**: `TBD` — anime widgets `keep`; manga widgets follow the manga
  decision. `AnimeWidgetManager` / `MangaWidgetManager` both depend on
  `SecurityPreferences.useAuthenticator()` to switch to the locked variant.
- **Dependencies**: `GetAnimeUpdates` / `GetMangaUpdates` interactors;
  `SecurityPreferences.useAuthenticator()` for the locked variant;
  `AnimeDownloadCache` / `MangaDownloadCache` for cover-bitmap fetching.
- **Notes**: Widget updates are driven by a `combine(getUpdates, useAuth)`
  flow in each `*WidgetManager.init` — the widget refreshes whenever unseen
  updates change OR the user toggles app-lock.

---

### Backup & Restore

- **Description**: Full-app backup engine. **Create**: `BackupCreateJob`
  (WorkManager periodic + OneTime) → `BackupCreator` → writes a
  `.tachibk` file (protobuf-wrapped JSON) at the user's chosen SAF URI. 11
  options (`BackupOptions`): library entries, categories, chapters,
  tracking, history, read/non-library entries, app settings, extension-repo
  settings, custom buttons (player), source settings, private settings
  (sensitive — opt-in), and extensions (also opt-in). 12 backup-creator
  classes under `data/backup/create/creators/` (one per option, split
  anime/manga/custom-button/preference/extension). **Restore**:
  `BackupRestoreJob` (WorkManager OneTime) → `BackupRestorer` → dispatches
  to per-domain restorers under `data/backup/restore/restorers/`. Backup
  format validated by `BackupFileValidator` (magic bytes + version);
  `BackupDecoder` unwraps optional gzip. Progress + completion/error
  surfaced via `BackupNotifier` (notification channel
  `Notifications.CHANNEL_BACKUP_RESTORE`).
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/data/backup/` (full tree).
  - Engine: `create/BackupCreateJob.kt`, `create/BackupCreator.kt`,
    `create/BackupOptions.kt`, `restore/BackupRestoreJob.kt`,
    `restore/BackupRestorer.kt`, `restore/RestoreOptions.kt`,
    `BackupFileValidator.kt`, `BackupDecoder.kt`, `BackupNotifier.kt`,
    `BackupDetector.kt`.
  - Creators: `create/creators/AnimeBackupCreator.kt`, `MangaBackupCreator.kt`,
    `AnimeCategoriesBackupCreator.kt`, `MangaCategoriesBackupCreator.kt`,
    `AnimeSourcesBackupCreator.kt`, `MangaSourcesBackupCreator.kt`,
    `AnimeExtensionRepoBackupCreator.kt`, `MangaExtensionRepoBackupCreator.kt`,
    `ExtensionsBackupCreator.kt`, `PreferenceBackupCreator.kt`,
    `CustomButtonBackupCreator.kt`.
  - Restorers: parallel `restore/restorers/Anime*`, `Manga*`, `Extensions*`,
    `Preference*`, `CustomButton*`.
  - Models: `models/Backup.kt` (aggregate), `BackupAnime.kt`, `BackupManga.kt`,
    `BackupChapter.kt`, `BackupEpisode.kt`, `BackupTracking.kt`,
    `BackupAnimeTracking.kt`, `BackupHistory.kt`, `BackupAnimeHistory.kt`,
    `BackupCategory.kt`, `BackupSource.kt`, `BackupAnimeSource.kt`,
    `BackupExtension.kt`, `BackupExtensionRepos.kt`,
    `BackupExtensionPreferences.kt`, `BackupPreference.kt`,
    `BackupCustomButtons.kt`.
  - Settings: `domain/.../backup/service/BackupPreferences.kt` —
    `backup_interval`, `__APP_STATE_last_auto_backup_timestamp`, `backup_flags`.
- **Status**: `keep`.
- **Dependencies**: WorkManager (foreground `dataSync` service);
  `AndroidStorageFolderProvider` for default location; `.tachibk` file
  intent-filter on `MainActivity`; `RestoreBackupScreen` reachable from
  Settings → Data & Storage + onboarding Guides step.
- **Notes**: Backup format is **not** compatible with Mihon/Tachiyomi
  (different backup model classes), but it is forward-compatible across
  Aniyomi versions. `BackupOptions.extensions` and `BackupOptions.privateSettings`
  default to `false` — users must explicitly opt in.

---

### Incognito Mode

- **Description**: A process-wide flag (`BasePreferences.incognitoMode()`,
  key `__APP_STATE_incognito_mode`) that, when on, suppresses all history
  writes (anime + manga) and forces the secure-screen flag (`secureScreen =
  INCOGNITO` → screenshots blocked, recents redacted). Toggled from
  `MoreTab` (the "Incognito mode" switch) or from the quick-toggle in
  `BaseActivity`'s app-state banner. While on, an ongoing notification
  (`Notifications.ID_INCOGNITO_MODE`, channel `CHANNEL_INCOGNITO_MODE`) is
  shown; tapping it broadcasts `ACTION_DISABLE_INCOGNITO_MODE` which is
  received by an inner-class `BroadcastReceiver` in `App` that flips the
  preference off. `MainActivity.onCreate` resets incognito to false on
  every fresh launch.
- **Location**:
  - State: `BasePreferences.incognitoMode()` (key
    `__APP_STATE_incognito_mode`).
  - Notification + receiver: `App.kt` lines 79, 112-139, 246-252 —
    `disableIncognitoReceiver` (inner `BroadcastReceiver`), registered /
    unregistered dynamically based on the preference value.
  - Toggle UI: `MoreTab.Content` → `screenModel.incognitoMode`
    (`MoreScreenModel`); app-state banner via `AppStateBanners` in
    `MainActivity`'s `Scaffold`.
  - Enforcement: `SecureActivityDelegateImpl.setSecureScreen` combines
    `secureScreen` + `incognitoMode` flows; `ReaderViewModel` /
    `PlayerViewModel` check `incognitoMode` before upserting history.
- **Status**: `keep`.
- **Dependencies**: `SecurityPreferences.secureScreen()` (the INCOGNITO
  mode of `SecureScreenMode`), `Notifications` channel registry.
- **Notes**: The broadcast-receiver approach is unusual (a static
  `Intent(ACTION_DISABLE_INCOGNITO_MODE)` PendingIntent attached to the
  notification) — it lets the user kill incognito from the notification
  shade without bringing the app to the foreground.

---

### External Intents (open video URLs in other apps)

- **Description**: Lets the user open an anime episode's resolved video in
  an external player (MX Player, VLC, etc.) instead of the in-app MPV
  player. `ExternalIntents.getExternalIntent(context, animeId, episodeId,
  chosenVideo)` resolves the video via `EpisodeLoader.getHosters` +
  `HosterLoader.getBestVideo`, resolves the URL via
  `source.resolveVideo`, and returns an `ACTION_VIEW` intent with the MIME
  type derived from the URL. If the user has picked a specific external
  player package (`PlayerPreferences.externalPlayerPreference()`), the
  intent is `setPackage(...)`-scoped to that app. The result (resume
  position, completed) is reported back via `MainActivity`'s
  `externalPlayerResult` `ActivityResultLauncher` and written to the
  episode's progress + history + tracking.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/ui/player/ExternalIntents.kt`
    (~600 lines — main `ExternalIntents` class + `newIntent` factory +
    `getVideoUrl` / `getMime` / `addVideoHeaders` helpers).
  - Launcher: `MainActivity.externalPlayerResult` (registered in
    `MainActivity.onCreate`), invoked via
    `MainActivity.startPlayerActivity(context, animeId, episodeId, extPlayer = true, ...)`.
  - Player preference: `PlayerPreferences.externalPlayerPreference()`
    (key `pref_always_use_external_player` + `external_player_selection`).
  - Track sync: `DelayedAnimeTrackingStore` queues progress updates while
    the external player is running; `DelayedAnimeTrackingUpdateJob` flushes
    them when the external player result returns.
- **Status**: `keep`.
- **Dependencies**: `AnimeSource.resolveVideo`, `EpisodeLoader.getHosters`,
  `HosterLoader.getBestVideo`, `TrackerManager` for track sync.
- **Notes**: This is anime-only — there is no equivalent for manga
  chapters (an external manga reader would make little sense). External
  player position is reported via the
  `android.intent.extra.PLAYER_STATE` / `POSITION` extras that MX Player
  + VLC send back in the result intent.

---

### App Lock / Biometric Unlock

- **Description**: Two-piece app-lock system. (1) **`SecureActivityDelegate`**
  is a process-singleton gate that tracks `requireUnlock` (true on process
  start, true after `lockAppAfter` idle time elapses since `lastAppClosed`).
  `App` forwards `ProcessLifecycleOwner` `onStart` / `onStop` callbacks to
  `SecureActivityDelegate.onApplicationStart` / `onApplicationStopped`;
  every `BaseActivity` subclass registers itself with the delegate, which
  on `onResume` checks `requireUnlock` and launches `UnlockActivity` if
  locked. (2) **`UnlockActivity`** is a transparent activity that shows a
  `BiometricPrompt` (with `confirmationRequired = false`); on success it
  calls `SecureActivityDelegate.unlock()` (flips `requireUnlock = false`)
  and finishes; on error it calls `finishAffinity()` to bounce the user
  back to the launcher. The activity transition is overridden to be
  instantaneous so the prompt appears without a visible activity swap.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/ui/base/delegate/SecureActivityDelegate.kt`
    (interface + `SecureActivityDelegateImpl`).
  - `app/src/main/java/eu/kanade/tachiyomi/ui/security/UnlockActivity.kt`.
  - Settings: `SettingsSecurityScreen` (see `06-settings.md`).
  - Back-end: `core/common/.../core/security/SecurityPreferences.kt`
    (`use_biometric_lock`, `lock_app_after`, `__APP_STATE_last_app_closed`).
- **Status**: `keep`.
- **Dependencies**: `androidx.biometric.BiometricPrompt`,
  `AuthenticatorUtil.isAuthenticationSupported` (device must have a
  fingerprint / face / class-3 biometric enrolled), `BaseActivity`
  (registers every activity).
- **Notes**: `lockAppAfter` values: 0 = always lock on background, -1 =
  never lock (once unlocked), N = lock if N minutes have elapsed since
  `lastAppClosed`. If `isAuthenticationSupported` is false on a device,
  `SecureActivityDelegateImpl.setAppLock` auto-disables the preference.

---

### Storage Usage Screen

- **Description**: A two-tab pager (`StorageTab`) showing per-category
  on-disk size breakdown for anime (page 0) and manga (page 1). Each row
  shows a category name, total size of downloaded episodes/chapters in
  that category, and a delete button to reclaim space. The settings screen
  also has a smaller `StorageInfo` composable (under Settings → Data &
  Storage → Storage usage) that shows per-mount free/total space with a
  progress bar.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/ui/storage/StorageTab.kt` —
    shared Voyager `Tab` (reached from `MoreTab.onClickStorage` and from
    `SettingsDataScreen.getDataGroup()`).
  - `app/src/main/java/eu/kanade/tachiyomi/ui/storage/anime/AnimeStorageTab.kt`,
    `AnimeStorageScreenModel.kt`,
    `app/src/main/java/eu/kanade/tachiyomi/ui/storage/manga/MangaStorageTab.kt`,
    `MangaStorageScreenModel.kt`,
    `app/src/main/java/eu/kanade/tachiyomi/ui/storage/CommonStorageScreenModel.kt`.
  - Settings widget:
    `app/src/main/java/eu/kanade/presentation/more/settings/screen/data/StorageInfo.kt`.
  - Disk helpers: `eu.kanade.tachiyomi.util.storage.DiskUtil`
    (`getExternalStorages`, `getAvailableStorageSpace`,
    `getTotalStorageSpace`).
- **Status**: `modify` — keep anime side; trim manga sub-tab once manga is
  decided.
- **Dependencies**: `AnimeDownloadProvider` / `MangaDownloadProvider`
  resolve on-disk paths; `GetAnimeFavorites` / `GetMangaFavorites` for
  library grouping.
- **Notes**: The size computation walks the download folder recursively
  per category — can be slow on libraries with thousands of episodes; the
  result is cached in `*StorageScreenModel` state.

---

### Stats Screen

- **Description**: A two-tab pager (`StatsTab`) showing library statistics
  for anime (page 0) and manga (page 1). Computed on first composition by
  `AnimeStatsScreenModel` / `MangaStatsScreenModel` (heavy queries over
  the anime/manga DBs). Shows total entries, watched/unwatched count,
  total runtime (anime) / total chapters (manga), mean score, score
  distribution, tracker coverage breakdown, top-N by score, etc.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/ui/stats/StatsTab.kt` — shared
    Voyager `Tab` (reached from `MoreTab.onClickStats`).
  - `app/src/main/java/eu/kanade/tachiyomi/ui/stats/anime/AnimeStatsTab.kt`,
    `AnimeStatsScreenModel.kt`,
    `app/src/main/java/eu/kanade/tachiyomi/ui/stats/manga/MangaStatsTab.kt`,
    `MangaStatsScreenModel.kt`.
  - Compose UI: `app/src/main/java/eu/kanade/presentation/more/stats/`
    (`AnimeStatsScreenContent.kt`, `MangaStatsScreenContent.kt`,
    `StatsScreenState.kt`).
  - Computation: `domain/.../stats/anime/` and `domain/.../stats/manga/`
    interactors.
- **Status**: `modify` — keep anime side; trim manga sub-tab once manga is
  decided.
- **Dependencies**: Anime DB / manga DB (separate SQLDelight databases);
  `GetAnimeFavorites` / `GetMangaFavorites`; `GetAnimeHistory` /
  `GetMangaHistory`.
- **Notes**: Stats computation runs on the IO dispatcher; the screen shows
  a `LoadingScreen` until the first computation completes (state is
  cached in the `*ScreenModel` so re-entering the tab is instant).

---

### Categories Management

- **Description**: CRUD screen for library categories — create, rename,
  hide, delete, reorder (drag-and-drop). Reached from `MoreTab` →
  Categories (pushes `CategoriesTab`). `CategoriesTab` is a shared
  two-tab pager (anime page 0 + manga page 1); `CategoriesTab.showMangaCategory()`
  is a `Channel<Unit>` API that other screens (e.g. `MangaLibraryTab`,
  `SettingsLibraryScreen`) use to programmatically switch to page 1. The
  `Category` model is **shared** between anime and manga — a single
  `categories` table with a `manga_not_anime` boolean column.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/ui/category/CategoriesTab.kt` —
    shared Voyager `Tab`.
  - `app/src/main/java/eu/kanade/tachiyomi/ui/category/anime/AnimeCategoryTab.kt`,
    `AnimeCategoryScreenModel.kt`,
    `app/src/main/java/eu/kanade/tachiyomi/ui/category/manga/MangaCategoryTab.kt`,
    `MangaCategoryScreenModel.kt`.
  - Compose UI: `app/src/main/java/eu/kanade/presentation/category/`
    (`AnimeCategoryScreen.kt`, `MangaCategoryScreen.kt`, shared
    `CategoryCreateDialog.kt`, `CategoryRenameDialog.kt`,
    `CategoryDeleteDialog.kt`).
  - Domain: `domain/.../category/anime/` and `domain/.../category/manga/`
    (separate interactors, shared `Category` model in
    `domain/.../category/model/Category.kt`).
  - DB: `categories` table (shared) + `mangas_categories` link table +
    `anime_categories` link table.
- **Status**: `modify` — keep; trim manga page once manga is decided.
  If manga is fully removed, the `categories` table needs a migration to
  drop manga rows (and the `manga_not_anime` column).
- **Dependencies**: `LibraryPreferences.defaultAnimeCategory()` /
  `defaultMangaCategory()`, `LibraryPreferences.animeUpdateCategories()` /
  `mangaUpdateCategories()`, `DownloadPreferences.downloadNewEpisodeCategories()`
  / `downloadNewChapterCategories()`, `DownloadPreferences.removeExcludeCategories()`
  / `removeExcludeAnimeCategories()`.
- **Notes**: A category can have a sort order (`changeOrder`); hidden
  categories are filtered from the library pager unless
  `LibraryPreferences.hideHiddenCategoriesSettings()` is off. The "Default
  category" preference value `-1` means "Always ask" (the user is prompted
  when adding a new entry to the library).

---

### App Update Checker

- **Description**: Checks GitHub releases for a newer APK. On every app
  start, `MainActivity.CheckForUpdates` (a `@Composable` side-effect)
  fires `AppUpdateChecker().checkForUpdate(context)` if the `enable-updater`
  gradle property is set (default: off). If a new version is found,
  `AppUpdateNotifier.promptUpdate(...)` posts a notification, and tapping
  it pushes `NewUpdateScreen` (shows version, changelog, release link,
  download link). `AppUpdateDownloadJob` handles APK download + install
  via the system package installer. The About screen has a manual "Check
  for updates" button that calls the same checker with `forceCheck = true`.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/data/updater/AppUpdateChecker.kt`
    — checks `GITHUB_REPO` = `aniyomiorg/aniyomi` (stable) or
    `aniyomiorg/aniyomi-preview` (preview build type) against
    `BuildConfig.COMMIT_COUNT` / `VERSION_NAME`.
  - `app/src/main/java/eu/kanade/tachiyomi/data/updater/AppUpdateNotifier.kt`,
    `AppUpdateDownloadJob.kt`.
  - UI: `app/src/main/java/eu/kanade/tachiyomi/ui/more/NewUpdateScreen.kt`.
  - Domain: `domain/.../release/interactor/GetApplicationRelease.kt`.
- **Status**: `modify` — `GITHUB_REPO` is still upstream-branding
  (`aniyomiorg/aniyomi`); must be repointed to the Kuta fork's release
  channel before enabling the updater. The `enable-updater` flag is
  currently off (default), so no updates are checked.
- **Dependencies**: `BuildConfig.UPDATER_ENABLED` (set from
  `Config.enableUpdater` gradle property), `GetApplicationRelease`,
  `RELEASE_URL` / `RELEASE_TAG` constants in `AppUpdateChecker.kt`.
- **Notes**: Preview builds use `r${COMMIT_COUNT}` as the version tag;
  stable builds use `v${VERSION_NAME}`. The check compares against
  `BuildConfig.COMMIT_COUNT.toInt()` for preview and semantic versioning
  for stable. In Miui-with-optimizations-disabled builds, a warning banner
  is shown on the About / CreateBackup screens.

---

### Library CSV Export

- **Description**: A small utility under Settings → Data & Storage →
  Export → "Library list" that exports the user's library (anime +
  manga combined) as a CSV file. A dialog lets the user pick which
  columns to include (Title, Type, Author, Artist). Uses
  `ActivityResultContracts.CreateDocument("text/csv")` to let the user
  choose a save location (default filename `aniyomi_library.csv` —
  upstream-branding, should be `kuta_library.csv`).
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/data/export/LibraryExporter.kt`
    — `exportToCsv(context, uri, favorites, options, onExportComplete)`.
  - `app/src/main/java/eu/kanade/tachiyomi/data/export/ExportEntry.kt` —
    `ExportEntry` + `toExportEntry()` extension on `Anime` / `Manga`.
  - UI: `SettingsDataScreen.getExportGroup()` + `ColumnSelectionDialog`.
  - Source data: `GetAnimeFavorites` + `GetMangaFavorites`.
- **Status**: `modify` — keep; trim manga side once decided; rename the
  default filename from `aniyomi_library.csv` to `kuta_library.csv`.
- **Dependencies**: `GetAnimeFavorites`, `GetMangaFavorites`,
  `ActivityResultContracts.CreateDocument`.
- **Notes**: This is the only "export library as text" path — backups are
  binary/protobuf; this is a human-readable CSV for spreadsheet use.
