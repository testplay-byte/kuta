# 06 — Settings

Inventory of every user-facing Settings screen in Kuta. All settings screens live
under `app/src/main/java/eu/kanade/presentation/more/settings/screen/` and are
hosted inside a single Voyager `Navigator` rooted at `SettingsMainScreen` (or at
a specific destination like `About` / `DataAndStorage` / `Tracking`) — see
`app/src/main/java/eu/kanade/tachiyomi/ui/setting/SettingsScreen.kt`. Two-pane
(tablet) mode renders the main list alongside the selected screen via
`TwoPanelBox`. Each screen implements `SearchableSettings` so it is reachable
from `SettingsSearchScreen` (magnifier icon in the main list's app bar).

> Status legend: `keep` = unchanged, `modify` = kept but redesigned,
> `remove` = dropped, `TBD` = pending user decision.

---

### Settings Framework, Search & Two-pane Shell

- **Description**: The wrapper that hosts all settings screens. `SettingsScreen`
  is a Voyager `Screen` that creates its own `Navigator` rooted at
  `SettingsMainScreen` (or `AboutScreen` / `SettingsDataScreen` /
  `SettingsTrackingScreen` when launched via a `Destination`). On tablets it
  switches to a two-pane layout (`TwoPanelBox` with `SettingsMainScreen.Content(twoPane = true)`
  on the left). `SettingsMainScreen` is a hardcoded list of 11 category items
  (Appearance, Library, Reader, Player, Downloads, Tracking, Browse, Data &
  Storage, Security, Advanced, About). A magnifier action pushes
  `SettingsSearchScreen`, which indexes every `SearchableSettings.getTitleRes()`
  and `getPreferences()` result; `SearchableSettings.highlightKey` allows
  external callers (e.g. the onboarding restore button) to deep-link into a
  specific preference row.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/ui/setting/SettingsScreen.kt` —
    Voyager `Screen` shell (one-pane vs two-pane).
  - `app/src/main/java/eu/kanade/presentation/more/settings/screen/SettingsMainScreen.kt` —
    the 11-item category list.
  - `app/src/main/java/eu/kanade/presentation/more/settings/screen/SettingsSearchScreen.kt` —
    searchable index.
  - `app/src/main/java/eu/kanade/presentation/more/settings/screen/SearchableSettings.kt` —
    base class with `highlightKey` mechanism.
  - `app/src/main/java/eu/kanade/presentation/more/settings/screen/Commons.kt` —
    shared setting item composables.
  - Entry from UI: `MoreTab.Content` → `navigator.push(SettingsScreen())`.
- **Status**: `modify` — keep the shell + search; we will redesign the visual
  language (cards/sections/typography) and likely remove the Manga-specific
  category sub-items from several screens.
- **Dependencies**: Depends on `UiPreferences`, `BasePreferences`, etc. (each
  category screen pulls its own preferences via `Injekt.get`). `MoreTab` and
  `OnboardingScreen` (restore path) push into it.
- **Notes**: Because each `*Preferences` class co-locates anime + manga keys
  (see `09-preferences.md`), removing manga UI does **not** require touching
  `*Preferences` classes — manga keys simply become dead entries.

---

### Appearance & General Settings

- **Description**: The first category in the main list. Two groups: **Theme**
  (light/dark/system mode via `AppThemeModePreferenceWidget`, app theme color,
  AMOLED pure-black toggle — applied immediately via
  `setAppCompatDelegateThemeMode` and `ActivityCompat.recreate`) and **Display**
  (app language → `AppLanguageScreen`, tablet UI mode, start screen, nav style
  — `NavStyle` enum — date format, relative-time format). The `NavStyle`
  preference (4 variants: move Manga / Updates / History / Browse into More) is
  the lever Phase 1 used to gate the manga tab — `NavStyle.tabs` always omits
  `MangaLibraryTab` in this fork.
- **Location**:
  - `app/src/main/java/eu/kanade/presentation/more/settings/screen/SettingsAppearanceScreen.kt`
  - `app/src/main/java/eu/kanade/presentation/more/settings/screen/appearance/AppLanguageScreen.kt`
  - `app/src/main/java/eu/kanade/domain/ui/UiPreferences.kt` — back-end (theme
    mode, app theme, AMOLED, tablet UI, start screen, nav style, date format,
    relative time).
  - `app/src/main/java/eu/kanade/domain/ui/model/NavStyle.kt` — the four
    nav-style enum values.
  - `app/src/main/java/eu/kanade/domain/ui/model/StartScreen.kt`,
    `TabletUiMode.kt`, `ThemeMode.kt`.
- **Status**: `modify` — visual redesign; remove the `MOVE_MANGA_TO_MORE`
  variant once manga is fully gone (Phase 1 already forces its removal).
- **Dependencies**: Used by `HomeScreen` (default tab + bottom-nav order),
  `App.onCreate` (theme mode application), `MangaLibraryTab` /
  `AnimeLibraryTab` / `UpdatesTab` / `HistoriesTab` (for `fromMore` UX logic).
- **Notes**: `NavStyle.MOVE_MANGA_TO_MORE` is currently a no-op (Phase 1 FORK
  marker forces manga out always). `MoreTab.onClickAlt` is gated to redirect
  to `AnimeLibraryTab` when `navStyle.moreTab` is `MangaLibraryTab`.

---

### Library & Updates Settings

- **Description**: Three groups tied to library background sync and per-item
  behavior. **Categories** group: edit categories (→ `CategoriesTab`), pick
  default anime + default manga category, "categorized display settings",
  "hide hidden categories". **Library update** group: auto-update interval
  (off/12h/24h/48h/72h/weekly), device restrictions (Wi-Fi / non-metered /
  charging), per-category included/excluded lists (separate dialogs for anime
  and manga), refresh-metadata, smart-update restrictions (only completely
  read / only started / non-completed / in release period), show update-count
  badge on tab. **Behavior** groups: season auto-update on refresh/update
  (anime-only), episode swipe start/end actions (anime), chapter swipe
  start/end actions (manga), and "mark duplicate episode/chapter as seen/read"
  multi-select.
- **Location**:
  - `app/src/main/java/eu/kanade/presentation/more/settings/screen/SettingsLibraryScreen.kt`
  - `domain/src/main/java/tachiyomi/domain/library/service/LibraryPreferences.kt` —
    back-end (~30 keys, anime + manga co-located).
  - Library-update workers: `app/src/main/java/eu/kanade/tachiyomi/data/library/anime/`
    (`AnimeLibraryUpdateJob`, `AnimeMetadataUpdateJob`) and `.../manga/`
    (`MangaLibraryUpdateJob`, `MangaMetadataUpdateJob`).
  - Constants: `LibraryPreferences.Companion.DEVICE_*`, `ENTRY_*`,
    `MARK_DUPLICATE_*`.
- **Status**: `modify` — keep; trim the manga-specific sub-items (manga
  categories dialog, manga swipe actions, default manga category) once manga
  is decided. The `MangaLibraryUpdateJob.setupTask` call in the interval
  `onValueChanged` is the only thing keeping manga library updates active.
- **Dependencies**: `CategoriesTab` (for editing categories); the two
  `*LibraryUpdateJob`s schedule WorkManager periodic work that reads
  `LibraryPreferences`.
- **Notes**: This is one of the heaviest anime/manga-coupled screens — every
  preference group has a parallel anime + manga branch.

---

### Player & Reader Settings Screens

- **Description**: Two distinct settings hubs reached from the main list.
  **Reader** (`SettingsReaderScreen`) is the manga page-reader settings
  (default reading mode, double-tap speed, page transitions, display, E-Ink,
  reading, paged, webtoon, navigation, actions) backed by `ReaderPreferences`
  (~80 keys). **Player** (`PlayerSettingsScreen`) is the MPV anime player
  settings — its own `Navigator` rooted at `PlayerSettingsMainScreen`, listing
  8 sub-screens: Player, Gestures, Decoder, Subtitle, Audio, Custom Button,
  Editor (mpv.conf-style Lua editor), Advanced. Each sub-screen pulls from
  `PlayerPreferences`, `GesturePreferences`, `DecoderPreferences`,
  `SubtitlePreferences`, `AudioPreferences`, `AdvancedPlayerPreferences`.
- **Location**:
  - Reader: `app/src/main/java/eu/kanade/presentation/more/settings/screen/SettingsReaderScreen.kt`
    + `app/src/main/java/eu/kanade/tachiyomi/ui/reader/setting/ReaderPreferences.kt`.
  - Player: `app/src/main/java/eu/kanade/tachiyomi/ui/setting/PlayerSettingsScreen.kt`
    (Voyager shell with two-pane support) + `app/src/main/java/eu/kanade/presentation/more/settings/screen/player/`
    (`PlayerSettingsMainScreen.kt`, `PlayerSettingsPlayerScreen.kt`,
    `PlayerSettingsGesturesScreen.kt`, `PlayerSettingsDecoderScreen.kt`,
    `PlayerSettingsSubtitleScreen.kt`, `PlayerSettingsAudioScreen.kt`,
    `PlayerSettingsAdvancedScreen.kt`, `PlayerSettingsEditorScreen.kt`,
    `PlayerSettingsCustomButtonScreen.kt`).
  - Back-end: `app/src/main/java/eu/kanade/tachiyomi/ui/player/settings/*Preferences.kt`
    (6 classes).
- **Status**: `modify` — keep both (player is core to Kuta; reader's fate
  follows the manga decision — see `09-manga.md`). Visual redesign.
- **Dependencies**: Player settings also consume `custom_buttons.sq` (anime DB)
  for the Lua-defined mpv buttons. Reader settings feed `ReaderActivity` /
  `ReaderViewModel` / `ViewerConfig`.
- **Notes**: See `DOCS/architecture/06-player.md` for the player pipeline.
  Custom-button settings persist in SQLite, not in `SharedPreferences`, so they
  back up via the DB backup creator — not via the preference backup creator.

---

### Downloads Settings

- **Description**: Per-download preference screen. Top-level toggles: Wi-Fi
  only, download speed limit (KiB/s, dialog), save chapter as CBZ, split tall
  images, number of simultaneous download slots (1-5). Sub-groups: **Delete
  chapters/episodes** (remove after marked as read, remove after N-th-to-last
  read, remove bookmarked, download fillermarked, excluded anime categories,
  excluded manga categories); **Auto-download** (download new episodes /
  chapters, only-unseen-only, per-category included/excluded for both anime and
  manga); **Download ahead** (auto-download while watching / while reading, 0
  / 2 / 3 / 5 / 10 episodes-or-chapters); **External downloader** (use
  external downloader, package picker — IDM, IDM+, ADM, ADM Lite).
- **Location**:
  - `app/src/main/java/eu/kanade/presentation/more/settings/screen/SettingsDownloadScreen.kt`
  - `domain/src/main/java/tachiyomi/domain/download/service/DownloadPreferences.kt` —
    back-end (~20 keys, anime + manga co-located).
- **Status**: `modify` — keep core (auto-download while watching, external
  downloader, Wi-Fi, slots, speed limit); trim the manga-side auto-download +
  delete options once manga is decided.
- **Dependencies**: Read by `AnimeDownloadManager` / `MangaDownloadManager`,
  `AnimeDownloader` / `MangaDownloader`, `AnimeDownloadJob` / `MangaDownloadJob`.
- **Notes**: External-downloader package list is hardcoded to
  `idm.internet.download.manager*` and `com.dv.adm` — discovered via
  `packageManager.getInstalledPackages(0)` filter at composition time.

---

### Tracking Settings

- **Description**: Tracker account management and sync behavior. Top toggles:
  auto-update tracker on mark-read, track on add-to-library, show next-episode
  airing time, auto-update on mark-read mode (`AutoTrackState` —
  always / only-wifi / never). **Services** group: 7 standard trackers (MAL,
  Anilist, Kitsu, MangaUpdates, Shikimori, Simkl, Bangumi). Each opens an
  OAuth-in-browser flow (or username/password dialog for Kitsu + MangaUpdates).
  **Enhanced services** group: auto-detected enhanced trackers bound to a
  specific source class (Komga, Kavita, Suwayomi — manga; Jellyfin — anime).
  A help icon links to `https://aniyomi.org/help/guides/tracking/`.
- **Location**:
  - `app/src/main/java/eu/kanade/presentation/more/settings/screen/SettingsTrackingScreen.kt`
  - `app/src/main/java/eu/kanade/domain/track/service/TrackPreferences.kt` —
    back-end (per-tracker `__PRIVATE_track_token_<id>`, usernames, passwords,
    auto-update flags).
  - `app/src/main/java/eu/kanade/tachiyomi/data/track/` — tracker
    implementations (`TrackerManager.kt`, `BaseTracker.kt`, `anilist/`,
    `myanimelist/`, `kitsu/`, `mangaupdates/`, `shikimori/`, `bangumi/`,
    `simkl/`, `komga/`, `kavita/`, `suwayomi/`, `jellyfin/`).
  - OAuth callback target: `app/src/main/java/eu/kanade/tachiyomi/ui/setting/track/TrackLoginActivity.kt`
    (handles `aniyomi://*-auth` for 5 services).
- **Status**: `modify` — keep all standard trackers (they serve both anime
  and manga, so they remain useful even if manga UI is removed). Enhanced
  manga-only trackers (Komga, Kavita, Suwayomi) follow manga's fate.
- **Dependencies**: `TrackerManager` is consumed by `AnimeTrackInfoDialog` /
  `MangaTrackInfoDialog` on entry screens, plus `*TrackPreferences`.
- **Notes**: Tracker credential keys are prefixed `__PRIVATE_` so they are
  excluded from backups unless the user explicitly opts in (see BackupOptions).

---

### Browse & Sources Settings

- **Description**: Sources / extensions configuration. **Sources** group: hide
  in-anime-library items, hide in-manga-library items, manage anime extension
  repos (→ `AnimeExtensionReposScreen`), manage manga extension repos (→
  `MangaExtensionReposScreen`); each repo screen shows a count via
  `GetAnimeExtensionRepoCount` / `GetMangaExtensionRepoCount`. **NSFW content**
  group: show NSFW sources (requires biometric auth via
  `FragmentActivity.authenticate`), with parental-controls info banner.
- **Location**:
  - `app/src/main/java/eu/kanade/presentation/more/settings/screen/SettingsBrowseScreen.kt`
  - Repo screens: `app/src/main/java/eu/kanade/presentation/more/settings/screen/browse/AnimeExtensionReposScreen.kt`,
    `MangaExtensionReposScreen.kt`, plus shared `components/ExtensionReposScreen.kt`.
  - Back-end: `app/src/main/java/eu/kanade/domain/source/service/SourcePreferences.kt`
    (`source_languages`, `show_nsfw_source`, `anime_extension_repos`,
    `extension_repos`, `__APP_STATE_trusted_extensions`, `hidden_anime_catalogues`,
    `hidden_catalogues`, `hide_in_anime_library_items`,
    `hide_in_manga_library_items`, DataSaver).
- **Status**: `modify` — keep anime side; trim manga repos item once manga is
  decided.
- **Dependencies**: Repo lists are persisted in SQLDelight
  (`anime_extension_repos` / `manga_extension_repos` tables in the
  `mihon-domain` module). Extension install UI lives in `BrowseTab`'s
  extensions sub-tabs.
- **Notes**: Repo addition is also reachable via the `aniyomi://add-repo`
  (anime) and `tachiyomi://add-repo` (manga) deep links — see `11-misc.md`.

---

### Data & Storage Settings

- **Description**: A combined screen for storage, backup/restore, caches, and
  library export. **Storage location** picker (SAF `OpenDocumentTree`,
  persists URI via `takePersistableUriPermission`, falls back gracefully on
  InkBook/Samsung devices that don't support persistable grants). **Backup &
  restore** group: segmented button for "Create backup" (→ `CreateBackupScreen`
  with 11 options incl. library entries, categories, chapters, tracking,
  history, settings, extension repos, custom buttons, source settings, private
  settings, extensions) / "Restore backup" (file picker →
  `RestoreBackupScreen`); automatic backup interval (off/6h/12h/24h/48h/weekly)
  via `BackupCreateJob.setupTask`. **Storage usage** group: `StorageInfo`
  composable (per-mount free/total + progress bar), link to `StorageTab` (the
  More-tab storage breakdown), clear chapter cache, auto-clear chapter cache
  on launch. **Export** group: library list → CSV via `LibraryExporter`.
- **Location**:
  - `app/src/main/java/eu/kanade/presentation/more/settings/screen/SettingsDataScreen.kt`
  - `app/src/main/java/eu/kanade/presentation/more/settings/screen/data/CreateBackupScreen.kt`,
    `RestoreBackupScreen.kt`, `StorageInfo.kt`.
  - Back-end: `domain/.../backup/service/BackupPreferences.kt`,
    `domain/.../storage/service/StoragePreferences.kt`,
    `core/common/.../storage/AndroidStorageFolderProvider.kt`.
  - Backup engine: `app/src/main/java/eu/kanade/tachiyomi/data/backup/`
    (`create/BackupCreateJob.kt`, `create/BackupCreator.kt`,
    `create/BackupOptions.kt`, `restore/BackupRestoreJob.kt`,
    `restore/BackupRestorer.kt`, `restore/RestoreOptions.kt`,
    `restore/restorers/*`, `models/*`, `BackupFileValidator.kt`,
    `BackupDecoder.kt`, `BackupNotifier.kt`).
  - CSV export: `app/src/main/java/eu/kanade/tachiyomi/data/export/LibraryExporter.kt`.
- **Status**: `keep` — backup/restore + storage + cache are core. Visual
  redesign only.
- **Dependencies**: `BackupCreateJob` / `BackupRestoreJob` are WorkManager
  workers; `RestoreBackupScreen` is also the destination for `.tachibk` deep
  links (see `11-misc.md`). `StorageTab` lives under `MoreTab`.
- **Notes**: `BackupOptions` includes both anime and manga categories /
  history / tracking — manga-side options will become no-ops if manga is
  removed but the engine itself is shared.

---

### Security Settings

- **Description**: Four items only. **Lock with biometrics** (requires
  `isAuthenticationSupported` — fingerprint/face/Iris); **Lock when idle**
  (always / 1 / 2 / 5 / 10 min / never — each change requires biometric
  confirmation); **Hide notification content**; **Secure screen** mode
  (`ALWAYS` / `INCOGNITO` / `NEVER`) — when `INCOGNITO`, the screen is
  flagged secure only while incognito mode is on.
- **Location**:
  - `app/src/main/java/eu/kanade/presentation/more/settings/screen/SettingsSecurityScreen.kt`
  - Back-end: `core/common/src/main/java/eu/kanade/tachiyomi/core/security/SecurityPreferences.kt`
    (`use_biometric_lock`, `lock_app_after`, `secure_screen_v2`,
    `hide_notification_content`, `__APP_STATE_last_app_closed`).
  - Enforcement: `app/src/main/java/eu/kanade/tachiyomi/ui/base/delegate/SecureActivityDelegate.kt`,
    `app/src/main/java/eu/kanade/tachiyomi/ui/security/UnlockActivity.kt`.
- **Status**: `keep`.
- **Dependencies**: `BaseActivity` registers every activity with
  `SecureActivityDelegate`; `App` (ProcessLifecycleOwner) forwards
  `onApplicationStart` / `onApplicationStopped` to the delegate to compute
  `requireUnlock`.
- **Notes**: Unlock enforcement lives in `SecureActivityDelegateImpl.onResume`
  — it launches `UnlockActivity` (a BiometricPrompt-only activity) and
  overrides the activity transition to be instantaneous.

---

### Advanced Settings

- **Description**: Catch-all category for diagnostics + power-user tweaks.
  Top actions: dump crash logs (writes a `.txt` via `CrashLogUtil.dumpLogs`),
  verbose logging (restart required), debug info screen (→ `DebugInfoScreen`),
  re-trigger onboarding, manage notifications (system settings).
  **Background activity** group: disable battery optimization (intent to
  `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`), Don't-kill-my-app link.
  **Data** group: invalidate download cache (both anime + manga),
  clear manga DB (→ `ClearDatabaseScreen`), clear anime DB (→
  `ClearAnimeDatabaseScreen`). **Network** group: clear cookies, clear WebView
  data, DoH provider (14 providers), user-agent string + reset. **Library**
  group: refresh library covers (starts both `AnimeLibraryUpdateJob` +
  `MangaLibraryUpdateJob` + both metadata jobs), reset viewer flags. **Reader**
  group: hardware bitmap threshold (texture-limit options), always decode
  long-strip with SSIV, display profile picker. **Extensions** group: extension
  installer (`PACKAGE` / `SHIZUKU` / `PRIVATE` — `PRIVATE` hidden in release
  builds), revoke extension trust (both anime + manga). **Data saver** group
  (SY-inherited): manga-only bandwidth saver.
- **Location**:
  - `app/src/main/java/eu/kanade/presentation/more/settings/screen/SettingsAdvancedScreen.kt`
  - Sub-screens: `advanced/ClearDatabaseScreen.kt`, `advanced/ClearAnimeDatabaseScreen.kt`,
    `debug/DebugInfoScreen.kt`, `debug/WorkerInfoScreen.kt`,
    `debug/BackupSchemaScreen.kt`.
  - Back-ends: `core/common/.../network/NetworkPreferences.kt`,
    `app/.../domain/base/BasePreferences.kt`,
    `domain/.../source/service/SourcePreferences.kt`.
- **Status**: `modify` — keep; trim manga-specific items (`ClearDatabaseScreen`,
  `MangaMetadataUpdateJob.startNow`, `ResetMangaViewerFlags`,
  `trustMangaExtension.revokeAll`, manga Data Saver) once manga is decided.
- **Dependencies**: `CrashLogUtil` writes to cache dir; `NetworkHelper`
  applies DoH on next client build; `TrustMangaExtension` /
  `TrustAnimeExtension` persist trusted signing keys.
- **Notes**: The Shizuku installer option shows an AlertDialog if Shizuku
  isn't installed, with a download link. `DebugInfoScreen` dumps device info,
  workers, and a backup schema view — useful for support tickets.

---

### About Screen

- **Description**: End-of-tree screen showing app identity + version + build
  date (copies debug info to clipboard on tap), "check for updates" (if
  `updaterEnabled` — gated by the `enable-updater` gradle property),
  "what's new" (links to `RELEASE_URL` = `https://github.com/aniyomiorg/aniyomi/releases/tag/...`
  — still upstream-branding), help-translate link, open-source licenses (→
  `OpenSourceLicensesScreen` powered by `aboutlibraries`), privacy-policy
  link, and three footer links (website, Discord, GitHub). Phase 1 added a
  "Kuta / Based on Aniyomi" attribution row at the top.
- **Location**:
  - `app/src/main/java/eu/kanade/presentation/more/settings/screen/about/AboutScreen.kt`
    (+ `OpenSourceLicensesScreen.kt`, `OpenSourceLibraryLicenseScreen.kt`).
  - Update checker: `app/src/main/java/eu/kanade/tachiyomi/data/updater/AppUpdateChecker.kt`,
    `AppUpdateNotifier.kt`, `AppUpdateDownloadJob.kt`.
  - Update UI: `app/src/main/java/eu/kanade/tachiyomi/ui/more/NewUpdateScreen.kt`.
- **Status**: `modify` — Phase 1 already replaced the headline with "Kuta /
  Based on Aniyomi". Links still point to `aniyomi.org` / `aniyomiorg/aniyomi`
  GitHub — should be repointed to the Kuta fork's own URLs.
- **Dependencies**: `BuildConfig.COMMIT_SHA`, `COMMIT_COUNT`, `BUILD_TIME`,
  `VERSION_NAME`; `enable-updater` gradle property gates the update-check row.
- **Notes**: `AppUpdateChecker.GITHUB_REPO` is still hardcoded to
  `aniyomiorg/aniyomi` (or `aniyomiorg/aniyomi-preview` for preview builds) —
  this is a fork-level deviation that needs updating for Kuta's release
  channel.
