# 10 — Onboarding

The first-run onboarding flow that runs once on a fresh install (and can be
re-triggered from Settings → Advanced → "Onboarding guide"). It walks the
user through 4 steps (theme, storage, permissions, guides/restore) inside an
`InfoScreen`-hosted pager with a `materialSharedAxisX` slide transition.

> See `app/src/main/java/eu/kanade/presentation/more/onboarding/` for the
> step composables and `app/src/main/java/eu/kanade/tachiyomi/ui/more/OnboardingScreen.kt`
> for the Voyager `Screen` wrapper.

---

### Onboarding launcher & shell

- **Description**: Voyager `Screen` that decides whether onboarding should
  run. `MainActivity.ShowOnboarding()` (a `@Composable` side-effect inside
  the root `Navigator` block) checks `BasePreferences.shownOnboardingFlow()`;
  if false and the current screen isn't already `OnboardingScreen`, it pushes
  `OnboardingScreen`. The Voyager `OnboardingScreen` wraps the actual
  presentation composable, sets `shownOnboardingFlow = true` on completion,
  and provides an `onRestoreBackup` callback that closes onboarding and
  pushes `SettingsScreen(Destination.DataAndStorage)` with
  `SearchableSettings.highlightKey` set to the restore-preference key (so
  the restore row auto-scrolls + highlights when the settings screen opens).
  Back is intercepted while onboarding isn't complete (prevents exiting).
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/ui/more/OnboardingScreen.kt` —
    Voyager `Screen` wrapper.
  - `app/src/main/java/eu/kanade/presentation/more/onboarding/OnboardingScreen.kt` —
    presentation composable (4-step pager, `InfoScreen` chrome,
    `materialSharedAxisX` transitions).
  - `app/src/main/java/eu/kanade/presentation/more/onboarding/OnboardingStep.kt` —
    `interface OnboardingStep { val isComplete: Boolean; @Composable fun Content() }`.
  - `MainActivity.ShowOnboarding()` at `app/src/main/java/eu/kanade/tachiyomi/ui/main/MainActivity.kt:403-411`.
  - State: `BasePreferences.shownOnboardingFlow()` (key
    `__APP_STATE_onboarding_complete`).
  - Re-trigger: `SettingsAdvancedScreen` "Onboarding guide" row →
    `navigator.push(OnboardingScreen())`.
- **Status**: `keep` — first-run flow is core to a good install experience.
- **Dependencies**: `BasePreferences`, `SettingsScreen`, `SettingsDataScreen`
  (for the `restorePreferenceKeyString` and `storageLocationPicker` /
  `storageLocationText` helpers reused by `StorageStep`).
- **Notes**: `BackHandler(enabled = !shownOnboardingFlow)` in the Voyager
  wrapper means a first-time user can't back out of onboarding before
  completing it; a returning user re-triggering from Settings can.

---

### Theme Step

- **Description**: First onboarding step. Lets the user pick a theme mode
  (light / dark / system) via `AppThemeModePreferenceWidget` and an app-theme
  color via `AppThemePreferenceWidget`. AMOLED pure-black is read but not
  directly toggleable here (it's set later via Settings → Appearance). The
  theme mode is applied immediately to `AppCompatDelegate` via
  `setAppCompatDelegateThemeMode(it)`. `isComplete = true` always (the step
  is optional).
- **Location**:
  - `app/src/main/java/eu/kanade/presentation/more/onboarding/ThemeStep.kt`.
  - Back-end: `UiPreferences.themeMode()`, `appTheme()`, `themeDarkAmoled()`
    (keys `pref_theme_mode_key`, `pref_app_theme`, `pref_theme_dark_amoled_key`).
  - Widgets: `app/src/main/java/eu/kanade/presentation/more/settings/widget/AppThemeModePreferenceWidget.kt`,
    `AppThemePreferenceWidget.kt`.
- **Status**: `keep`.
- **Dependencies**: `UiPreferences` (Injekt), `AppCompatDelegate`.
- **Notes**: Mirrors the first group of `SettingsAppearanceScreen` — both
  use the same widgets so theme selection is consistent across onboarding +
  settings.

---

### Storage Step

- **Description**: Second step. Asks the user to pick a download location via
  SAF (`ActivityResultContracts.OpenDocumentTree`). Shows the current path
  via `SettingsDataScreen.storageLocationText` (or "No location set"). On
  TV boxes (where SAF isn't usable) shows a "Create folder" button that
  initializes the default `AndroidStorageFolderProvider.directory()` path
  instead. A help-text + "Learn more" button links to
  `SettingsDataScreen.HELP_URL` (`https://aniyomi.org/docs/faq/storage`).
  `isComplete` becomes true once `storagePref.isSet()` is true.
- **Location**:
  - `app/src/main/java/eu/kanade/presentation/more/onboarding/StorageStep.kt`.
  - Reused helpers: `SettingsDataScreen.storageLocationPicker` /
    `storageLocationText` in
    `app/src/main/java/eu/kanade/presentation/more/settings/screen/SettingsDataScreen.kt`.
  - Back-end: `StoragePreferences.baseStorageDirectory()` (key
    `__APP_STATE_storage_dir`), `AndroidStorageFolderProvider` in
    `core/common/src/main/java/tachiyomi/core/common/storage/AndroidStorageFolderProvider.kt`.
  - TV-box detection: `eu.kanade.tachiyomi.util.system.isTvBox`.
- **Status**: `keep` — choosing a download location up front avoids the
  SAF permission issues that plague mid-session location changes.
- **Dependencies**: `StoragePreferences`, `AndroidStorageFolderProvider`
  (both Injekt singletons), `UniFile` for SAF URI handling.
- **Notes**: `takePersistableUriPermission` is wrapped in try/catch because
  some Samsung / InkBook devices don't implement SAF persistable grants
  properly — on those devices the URI still works but isn't revokable.

---

### Permission Step

- **Description**: Third step. Three permission rows, each shown with a
  "Grant" button (or a green check once granted). (1) **Install unknown
  apps** (`REQUEST_INSTALL_PACKAGES`) — required to install extensions from
  repos; uses `rememberRequestPackageInstallsPermissionState()` +
  `launchRequestPackageInstallsPermission()`. (2) **Notifications**
  (`POST_NOTIFICATIONS`, Android 13+ only) — uses
  `ActivityResultContracts.RequestPermission`; pre-13 always shows as
  granted. (3) **Ignore battery optimizations** — launches
  `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` so library updates /
  downloads / backups survive doze. Permission state is re-checked on every
  `onResume` via a `DefaultLifecycleObserver` so returning from the system
  settings screen updates the granted check. `isComplete = true` always
  (all three are optional).
- **Location**:
  - `app/src/main/java/eu/kanade/presentation/more/onboarding/PermissionStep.kt`.
  - Install-permission helper: `eu.kanade.presentation.util.rememberRequestPackageInstallsPermissionState`
    + `eu.kanade.tachiyomi.util.system.launchRequestPackageInstallsPermission`.
- **Status**: `keep`.
- **Dependencies**: Android system settings intents; manifest-declared
  permissions (`REQUEST_INSTALL_PACKAGES`, `POST_NOTIFICATIONS`).
- **Notes**: The "manage notifications" row in `SettingsAdvancedScreen`
  duplicates the system-notifications settings as a fallback for users who
  skipped onboarding or want to revoke later.

---

### Guides Step & First-launch Backup Restore

- **Description**: Fourth and final step. Two paths. **New users**: a
  "Getting Started" button opens `https://aniyomi.org/docs/guides/getting-started`
  (the `GETTING_STARTED_URL` constant exported from this file and reused by
  `MangaLibraryTab`'s empty-library state). **Returning users**: a "Restore
  Backup" button calls `onRestoreBackup` (provided by the Voyager
  `OnboardingScreen` wrapper) — this finishes onboarding
  (`shownOnboardingFlow = true`) and pushes `SettingsScreen(Destination.DataAndStorage)`
  with `SearchableSettings.highlightKey` set to the restore-preference string,
  so the user lands on the Data & Storage screen with the restore row
  auto-highlighted. `isComplete = true` always.
- **Location**:
  - `app/src/main/java/eu/kanade/presentation/more/onboarding/GuidesStep.kt`
    (exports `GETTING_STARTED_URL`).
  - Restore-target screen:
    `app/src/main/java/eu/kanade/presentation/more/settings/screen/SettingsDataScreen.kt`
    (defines `restorePreferenceKeyString = MR.strings.label_backup`).
  - Restore flow:
    `app/src/main/java/eu/kanade/presentation/more/settings/screen/data/RestoreBackupScreen.kt`
    → `app/src/main/java/eu/kanade/tachiyomi/data/backup/restore/BackupRestoreJob.kt`
    (WorkManager `CoroutineWorker`).
- **Status**: `keep` — both paths are useful; only the URL needs updating
  to the Kuta fork's own docs site.
- **Dependencies**: `SearchableSettings.highlightKey` (static mutable
  String? — consumed by `SettingsSearchScreen` / settings items for
  auto-scroll + emphasize).
- **Notes**: There's also a `.tachibk` file-open intent-filter on
  `MainActivity` (see `11-misc.md`), so opening a backup file from a file
  manager jumps directly into `RestoreBackupScreen` without going through
  onboarding. The onboarding restore path is for users who want to be guided
  rather than open a file directly.

---

### Background work that fires on first launch

Not strictly part of onboarding, but runs in parallel during/after the
first-run flow:

- **`Migrator.awaitAndRelease()`** in `MainActivity.onCreate` blocks the
  splash screen until any version migrations finish (see `03-entry-points.md`).
- **`CheckForUpdates`** composable in `MainActivity` — fires
  `AppUpdateChecker().checkForUpdate(context)` (if `updaterEnabled`) and
  `AnimeExtensionApi().checkForUpdates(context)` +
  `MangaExtensionApi().checkForUpdates(context)` in parallel `LaunchedEffect`s.
- **`handleIntentAction(intent, navigator)`** — if the launcher intent has a
  shortcut / deep-link / search action, it's routed before the user sees the
  first tab.
- **`libraryPreferences.autoClearItemCache()`** — if the user opted in,
  `chapterCache.clear()` runs in `lifecycleScope.launchIO` on first launch.

These run regardless of whether onboarding has been completed.
