# 09 — Preferences

> Scope: how user/app settings are stored, accessed, and surfaced in the Kuta /
> Aniyomi fork. **Research documentation only** — no source files were modified.

## TL;DR

- Aniyomi uses a **custom `PreferenceStore` abstraction** sitting on top of
  Android `SharedPreferences`. It is *not* DataStore, *not* the raw
  `androidx.preference` API at runtime (the `androidx.preference` lib is only
  used to obtain the default shared-prefs file).
- Each feature area owns a small `*Preferences` helper class that registers
  typed keys (`getBoolean`, `getString`, `getEnum`, `getObject`, …) against the
  single injected `PreferenceStore`.
- All `*Preferences` helpers are wired in `PreferenceModule.kt` (Injekt DI).
- Settings UIs are Jetpack Compose screens under
  `app/src/main/java/eu/kanade/presentation/more/settings/screen/`.

---

## 1. The `PreferenceStore` abstraction

### Core interface

**File:** `core/common/src/main/java/tachiyomi/core/common/preference/PreferenceStore.kt`

```kotlin
interface PreferenceStore {
    fun getString(key: String, defaultValue: String = ""): Preference<String>
    fun getLong(key: String, defaultValue: Long = 0): Preference<Long>
    fun getInt(key: String, defaultValue: Int = 0): Preference<Int>
    fun getFloat(key: String, defaultValue: Float = 0f): Preference<Float>
    fun getBoolean(key: String, defaultValue: Boolean = false): Preference<Boolean>
    fun getStringSet(key: String, defaultValue: Set<String> = emptySet()): Preference<Set<String>>
    fun <T> getObject(
        key: String,
        defaultValue: T,
        serializer: (T) -> String,
        deserializer: (String) -> T,
    ): Preference<T>
    fun getAll(): Map<String, *>
}

inline fun <reified T : Enum<T>> PreferenceStore.getEnum(
    key: String, defaultValue: T,
): Preference<T> = getObject(key, defaultValue, { it.name }, { ... })
```

### The `Preference<T>` value object

**File:** `core/common/src/main/java/tachiyomi/core/common/preference/Preference.kt`

```kotlin
interface Preference<T> {
    fun key(): String
    fun get(): T
    fun set(value: T)
    fun isSet(): Boolean
    fun delete()
    fun defaultValue(): T
    fun changes(): Flow<T>            // reactive observations
    fun stateIn(scope: CoroutineScope): StateFlow<T>

    companion object {
        fun isPrivate(key: String): Boolean = key.startsWith(PRIVATE_PREFIX)
        fun privateKey(key: String): String = "${PRIVATE_PREFIX}$key"
        fun isAppState(key: String): Boolean = key.startsWith(APP_STATE_PREFIX)
        fun appStateKey(key: String): String = "${APP_STATE_PREFIX}$key"

        private const val APP_STATE_PREFIX = "__APP_STATE_"
        private const val PRIVATE_PREFIX   = "__PRIVATE_"
    }
}
```

Two key-prefix conventions matter:
- `__PRIVATE_<key>` — sensitive (e.g. tracker passwords/tokens); excluded from
  backups unless the user explicitly opts in.
- `__APP_STATE_<key>` — internal app state, not a real user preference; also
  excluded from backups.

Helpers in the same file: `Preference<T>.getAndSet`, `deleteAndGet`,
`plusAssign` / `minusAssign` for set-typed prefs, and
`Preference<Boolean>.toggle()`.

### Android implementation

**File:** `core/common/src/main/java/tachiyomi/core/common/preference/AndroidPreferenceStore.kt`

```kotlin
class AndroidPreferenceStore(
    context: Context,
    private val sharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context),
) : PreferenceStore {

    private val keyFlow = sharedPreferences.keyFlow   // wraps OnSharedPreferenceChangeListener

    override fun getBoolean(key: String, defaultValue: Boolean) =
        BooleanPrimitive(sharedPreferences, keyFlow, key, defaultValue)
    /* ...getString / getLong / getInt / getFloat / getStringSet / getObject... */

    override fun getAll(): Map<String, *> = sharedPreferences.all ?: emptyMap()
}

// Keyed reactivity comes from a callbackFlow:
private val SharedPreferences.keyFlow
    get() = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key -> trySend(key) }
        registerOnSharedPreferenceChangeListener(listener)
        awaitClose { unregisterOnSharedPreferenceChangeListener(listener) }
    }
```

The concrete primitive wrappers (`BooleanPrimitive`, `StringPrimitive`, …,
`Object`) live in `AndroidPreference.kt` in the same package.

There is also an `InMemoryPreferenceStore` for tests / previews.

### DI wiring

**File:** `app/src/main/java/eu/kanade/tachiyomi/di/PreferenceModule.kt`

```kotlin
class PreferenceModule(val app: Application) : InjektModule {
    override fun InjektRegistrar.registerInjectables() {
        addSingletonFactory<PreferenceStore> { AndroidPreferenceStore(app) }
        addSingletonFactory { NetworkPreferences(get(), isDebugBuildType) }
        addSingletonFactory { SourcePreferences(get()) }
        addSingletonFactory { SecurityPreferences(get()) }
        addSingletonFactory { LibraryPreferences(get()) }
        addSingletonFactory { ReaderPreferences(get()) }
        addSingletonFactory { PlayerPreferences(get()) }
        addSingletonFactory { GesturePreferences(get()) }
        addSingletonFactory { DecoderPreferences(get()) }
        addSingletonFactory { SubtitlePreferences(get()) }
        addSingletonFactory { AudioPreferences(get()) }
        addSingletonFactory { AdvancedPlayerPreferences(get()) }
        addSingletonFactory { TrackPreferences(get()) }
        addSingletonFactory { DownloadPreferences(get()) }
        addSingletonFactory { BackupPreferences(get()) }
        addSingletonFactory { StoragePreferences(get<AndroidStorageFolderProvider>(), get()) }
        addSingletonFactory { UiPreferences(get()) }
        addSingletonFactory { BasePreferences(app, get()) }
    }
}
```

Anything that wants settings just `Injekt.get<FooPreferences>()`.

---

## 2. Feature-specific `*Preferences` classes

All of these are plain classes whose constructor takes a `PreferenceStore`
(and sometimes a `Context` / `FolderProvider`). Each public `fun` returns a
`Preference<T>`; the `fun` name is the accessor.

| Class                         | File | Module | Examples of keys |
|-------------------------------|------|--------|------------------|
| `BasePreferences`             | `app/.../eu/kanade/domain/base/BasePreferences.kt` | app | `__APP_STATE_pref_downloaded_only`, `__APP_STATE_incognito_mode`, `__APP_STATE_onboarding_complete`, `pref_display_profile_key`, `pref_hardware_bitmap_threshold`, `pref_always_decode_long_strip_with_ssiv` |
| `UiPreferences`               | `app/.../eu/kanade/domain/ui/UiPreferences.kt` | app | `pref_theme_mode_key`, `pref_app_theme`, `pref_theme_dark_amoled_key`, `relative_time_v2`, `app_date_format`, `tablet_ui_mode`, `start_screen`, `bottom_rail_nav_style` |
| `SourcePreferences`           | `app/.../eu/kanade/domain/source/service/SourcePreferences.kt` | app | `source_languages`, `show_nsfw_source`, `pref_migration_sorting`, `anime_extension_repos`, `extension_repos`, `__APP_STATE_trusted_extensions`, `hidden_anime_catalogues`, `hidden_catalogues` |
| `TrackPreferences`            | `app/.../eu/kanade/domain/track/service/TrackPreferences.kt` | app | `__PRIVATE_pref_mangasync_username_<id>`, `__PRIVATE_pref_mangasync_password_<id>`, `__PRIVATE_track_token_<id>`, `anilist_score_type`, `pref_auto_update_manga_sync_key` |
| `SecurityPreferences`         | `core/common/.../eu/kanade/tachiyomi/core/security/SecurityPreferences.kt` | core-common | `use_biometric_lock`, `lock_app_after`, `secure_screen_v2`, `hide_notification_content`, `__APP_STATE_last_app_closed` |
| `NetworkPreferences`          | `core/common/.../eu/kanade/tachiyomi/network/NetworkPreferences.kt` | core-common | `verbose_logging`, `doh_provider`, `default_user_agent` |
| `LibraryPreferences`          | `domain/.../tachiyomi/domain/library/service/LibraryPreferences.kt` | domain | `pref_display_mode_library`, `library_sorting_mode`, `animelib_sorting_mode`, `__APP_STATE_library_update_last_timestamp`, `pref_library_update_interval_key`, `library_update_restriction`, `display_category_tabs`, `display_download_badge`, `display_unread_badge`, `default_anime_category`, `default_category`, `pref_filter_animelib_downloaded_v2`, `pref_filter_library_unread_v2`, `pref_episode_swipe_end_action`, `pref_chapter_swipe_end_action`, … |
| `DownloadPreferences`         | `domain/.../tachiyomi/domain/download/service/DownloadPreferences.kt` | domain | `pref_download_only_over_wifi_key`, `use_external_downloader`, `external_downloader_selection`, `save_chapter_as_cbz`, `split_tall_images`, `auto_download_while_reading`, `auto_download_while_watching`, `remove_after_read_slots`, `download_new`, `download_new_episode`, `pref_download_fillermarked` |
| `BackupPreferences`           | `domain/.../tachiyomi/domain/backup/service/BackupPreferences.kt` | domain | `backup_interval`, `__APP_STATE_last_auto_backup_timestamp`, `backup_flags` |
| `StoragePreferences`          | `domain/.../tachiyomi/domain/storage/service/StoragePreferences.kt` | domain | `__APP_STATE_storage_dir` (single key; resolves to `FolderProvider.path()` by default) |
| `ReaderPreferences`           | `app/.../ui/reader/setting/ReaderPreferences.kt` | app | `pref_enable_transitions_key`, `pref_reader_flash`, `pref_reader_flash_duration`, `pref_reader_flash_mode`, `pref_double_tap_anim_speed`, `pref_show_page_number_key`, `pref_default_reading_mode_key`, `fullscreen`, `pref_keep_screen_on_key`, … (256 lines, ~80 keys) |
| `PlayerPreferences`           | `app/.../ui/player/settings/PlayerPreferences.kt` | app | `pref_preserve_watching_position`, `pref_progress_preference`, `pref_default_player_orientation_type_key`, `pref_allow_gestures_in_panels`, `pref_remember_brightness`, `player_brightness_value`, `pref_enable_skip_intro`, `pref_enable_auto_skip_ani_skip`, `pref_enable_pip`, `pref_always_use_external_player`, … |
| `GesturePreferences`          | `app/.../ui/player/settings/GesturePreferences.kt` | app | player gesture / seek / pinch prefs |
| `DecoderPreferences`          | `app/.../ui/player/settings/DecoderPreferences.kt` | app | mpv decoder / GPU / HDR prefs |
| `SubtitlePreferences`         | `app/.../ui/player/settings/SubtitlePreferences.kt` | app | subtitle style / language prefs |
| `AudioPreferences`            | `app/.../ui/player/settings/AudioPreferences.kt` | app | audio output / pitch / tempo prefs |
| `AdvancedPlayerPreferences`   | `app/.../ui/player/settings/AdvancedPlayerPreferences.kt` | app | mpv advanced / `mpv.conf`-style prefs |
| `BackupPreference`            | `app/.../data/backup/models/BackupPreference.kt` | app | Backup serializer model (not a real preference store) |
| `BackupExtensionPreferences`  | `app/.../data/backup/models/BackupExtensionPreferences.kt` | app | Backup serializer model for per-extension prefs |

> `BackupPreference` / `BackupExtensionPreferences` are **data classes used by
> the backup serializer**, not `PreferenceStore` accessors. Listed for
> disambiguation.

---

## 3. How to add a new preference

The pattern is intentionally uniform. To add e.g. a new boolean
"Auto-skip outro" pref on the player:

1. **Pick the right `*Preferences` class.** Player-related → `PlayerPreferences`.
2. **Add an accessor** that registers the key against the injected
   `preferenceStore`:

   ```kotlin
   // in PlayerPreferences.kt
   fun autoSkipOutro() = preferenceStore.getBoolean("pref_auto_skip_outro", false)
   ```

   - Choose a stable string key (convention: `pref_<thing>[_key]`).
   - For enums use `getEnum`; for complex objects use `getObject` with a
     serialiser pair.
   - Prefix with `Preference.appStateKey(...)` if it's internal app state, or
     `Preference.privateKey(...)` if it's sensitive (tokens, passwords).
3. **Read / write it** where needed:

   ```kotlin
   val playerPrefs: PlayerPreferences = Injekt.get()
   if (playerPrefs.autoSkipOutro().get()) { ... }
   playerPrefs.autoSkipOutro().set(true)
   ```
4. **Observe reactively** if useful: `playerPrefs.autoSkipOutro().changes()`
   returns a `Flow<Boolean>`; `.stateIn(scope)` gives a `StateFlow<Boolean>`.
5. **Wire up a UI toggle.** Add a `SwitchItem` / `ListItem` to the matching
   settings Compose screen (see §4). The screen typically does
   `pref.changes().collectAsState()` and calls `pref.set(...)` on toggle.

No DI registration change is needed for an existing `*Preferences` class — only
new *classes* require a new `addSingletonFactory { ... }` line in
`PreferenceModule.kt`.

---

## 4. Settings UI wiring

The Compose settings screens live in:

```
app/src/main/java/eu/kanade/presentation/more/settings/screen/
├── SettingsMainScreen.kt
├── SettingsAppearanceScreen.kt
├── SettingsLibraryScreen.kt
├── SettingsReaderScreen.kt
├── SettingsDownloadScreen.kt
├── SettingsTrackingScreen.kt
├── SettingsBrowseScreen.kt
├── SettingsSecurityScreen.kt
├── SettingsDataScreen.kt
├── SettingsAdvancedScreen.kt
├── SettingsSearchScreen.kt
├── SearchableSettings.kt               # base class w/ search indexing
├── Commons.kt                          # shared setting item composables
├── appearance/AppLanguageScreen.kt
├── advanced/{ClearDatabaseScreen, ClearAnimeDatabaseScreen}.kt
├── data/{CreateBackupScreen, RestoreBackupScreen, StorageInfo}.kt
├── browse/{AnimeExtensionReposScreen, MangaExtensionReposScreen, …}.kt
├── debug/{DebugInfoScreen, WorkerInfoScreen, BackupSchemaScreen}.kt
└── player/
    ├── PlayerSettingsMainScreen.kt
    ├── PlayerSettingsPlayerScreen.kt
    ├── PlayerSettingsGesturesScreen.kt
    ├── PlayerSettingsAudioScreen.kt
    ├── PlayerSettingsSubtitleScreen.kt
    ├── PlayerSettingsDecoderScreen.kt
    ├── PlayerSettingsAdvancedScreen.kt
    ├── PlayerSettingsEditorScreen.kt
    └── custombutton/PlayerSettingsCustomButtonScreen.kt   # backs custom_buttons table
```

The entry point is `SettingsScreen` (in
`app/src/main/java/eu/kanade/tachiyomi/ui/setting/SettingsScreen.kt`), a
Voyager `Screen` that hosts a `Navigator` and routes to the screens above.
Each screen reads its `*Preferences` via a Voyager `ScreenModel` (e.g.
`SettingsLibraryScreenModel`) and renders `PreferenceItem` / `SwitchItem` /
`SliderItem` composables from `presentation-core/.../components/SettingsItems.kt`.

Player sub-screens consume `PlayerPreferences`, `GesturePreferences`,
`DecoderPreferences`, `SubtitlePreferences`, `AudioPreferences`, and
`AdvancedPlayerPreferences` — all registered in `PreferenceModule.kt`.

---

## 5. Surprises / things worth knowing

1. **One shared prefs file.** All `*Preferences` classes share the default
   `SharedPreferences` file (from `PreferenceManager.getDefaultSharedPreferences`).
   There is no per-feature file. The keys are namespaced only by string prefix.
2. **`__APP_STATE_` vs `__PRIVATE_` prefixes are enforced by convention.**
   Helpers exist (`Preference.appStateKey`, `Preference.privateKey`,
   `Preference.isAppState`, `Preference.isPrivate`), and the backup system
   filters on these prefixes. Adding a "real" user preference that begins with
   either prefix will silently exclude it from backups.
3. **Anime and manga preferences are co-located, not split.** Unlike the
   database layer (which has separate `Database` / `AnimeDatabase`),
   `LibraryPreferences`, `DownloadPreferences`, `SourcePreferences` etc. hold
   *both* anime and manga keys in the same class — keys are disambiguated by
   string only (e.g. `default_anime_category` vs `default_category`,
   `pref_filter_animelib_downloaded_v2` vs `pref_filter_library_downloaded_v2`).
   When disabling the manga UI, these classes can stay as-is; the manga-side
   keys simply become dead entries.
4. **Custom player buttons live in SQLite, not preferences.**
   `custom_buttons.sq` (anime DB) persists Lua-defined mpv buttons — so they
   back up with the database, not with `SharedPreferences`.
5. **No DataStore migration.** Despite `androidx.datastore` being a common
   modern replacement, this codebase has not adopted it; everything still
   flows through `SharedPreferences` via the `PreferenceStore` shim.
