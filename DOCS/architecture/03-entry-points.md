# 03 — Entry Points

This document identifies the application's startup entry points: the
`Application` subclass, the launcher `Activity`, and what each one initialises.
There is **no `androidx.startup` Initializer** and **no Hilt/Dagger graph** in
this fork — startup is driven manually from `App.onCreate()` and `MainActivity.onCreate()`.

## AndroidManifest.xml — the relevant entries

File: `app/src/main/AndroidManifest.xml`

### `<application>` declaration (lines 36–50)

```xml
<application
    android:name=".App"
    android:allowBackup="false"
    android:enableOnBackInvokedCallback="true"
    android:hardwareAccelerated="true"
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name"
    android:largeHeap="true"
    android:localeConfig="@xml/locales_config"
    android:networkSecurityConfig="@xml/network_security_config"
    android:preserveLegacyExternalStorage="true"
    android:requestLegacyExternalStorage="true"
    android:roundIcon="@mipmap/ic_launcher_round"
    android:supportsRtl="true"
    android:theme="@style/Theme.Tachiyomi">
```

- `android:name=".App"` → resolves to `eu.kanade.tachiyomi.App`
  (file: `app/src/main/java/eu/kanade/tachiyomi/App.kt`).

### Launcher activity (lines 52–117)

```xml
<activity
    android:name=".ui.main.MainActivity"
    android:exported="true"
    android:launchMode="singleTop"
    android:windowSoftInputMode="adjustResize"
    android:theme="@style/Theme.Tachiyomi.SplashScreen">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>

    <!-- Deep link to add manga/anime repos: tachiyomi://add-repo and aniyomi://add-repo -->
    <intent-filter android:label="@string/action_add_repo">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="tachiyomi" />
        <data android:host="add-repo" />
    </intent-filter>
    <intent-filter android:label="@string/action_add_repo">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="aniyomi" />
        <data android:host="add-repo" />
    </intent-filter>

    <!-- Open .tachibk backup files -->
    <intent-filter android:label="@string/pref_restore_backup">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="file" />
        <data android:scheme="content" />
        <data android:host="*" />
        <data android:mimeType="*/*" />
        <data android:pathPattern=".*\\.tachibk" />
        <!-- …more pathPattern variants for nested-dot filenames… -->
    </intent-filter>

    <meta-data android:name="android.app.shortcuts" android:resource="@xml/shortcuts" />
</activity>
```

### Other declared activities (for context)

| Activity | File | Purpose |
|---|---|---|
| `.crash.CrashActivity` | `app/src/main/java/eu/kanade/tachiyomi/crash/CrashActivity.kt` | Shown by `GlobalExceptionHandler`; runs in `:error_handler` process |
| `.ui.deeplink.anime.DeepLinkAnimeActivity` | `app/…/ui/deeplink/anime/DeepLinkAnimeActivity.kt` | Handles `ACTION_SEARCH` / `SEND` to global anime search; `Theme.NoDisplay` |
| `.ui.deeplink.manga.DeepLinkMangaActivity` | `app/…/ui/deeplink/manga/DeepLinkMangaActivity.kt` | Same, for manga |
| `.ui.reader.ReaderActivity` | `app/…/ui/reader/ReaderActivity.kt` | Manga reader (full-screen, singleTask) |
| `.ui.player.PlayerActivity` | `app/…/ui/player/PlayerActivity.kt` | Anime player (MPV; PiP-capable, singleTask) |
| `.ui.security.UnlockActivity` | `app/…/ui/security/UnlockActivity.kt` | App-lock unlock screen |
| `.ui.webview.WebViewActivity` | `app/…/ui/webview/WebViewActivity.kt` | In-app WebView |
| `.ui.setting.track.TrackLoginActivity` | `app/…/ui/setting/track/TrackLoginActivity.kt` | OAuth callback target (`aniyomi://*-auth`) for trackers |
| `.extension.{manga,anime}.util.*ExtensionInstallActivity` | `app/…/extension/…/util/` | Translucent activity used to confirm extension installs |

The manifest also declares the `NotificationReceiver`, two extension-install
`Service`s (foreground, `shortService`), the merged WorkManager
`SystemForegroundService` (`dataSync`), a `FileProvider`, and the
`ShizukuProvider`. There is **no `androidx.startup.InitializationProvider`**
entry — WorkManager uses its default eager initialization.

---

## Application class — `App`

File: `app/src/main/java/eu/kanade/tachiyomi/App.kt`

```kotlin
class App : Application(), DefaultLifecycleObserver, SingletonImageLoader.Factory {

    private val basePreferences: BasePreferences by injectLazy()
    private val networkPreferences: NetworkPreferences by injectLazy()

    private val disableIncognitoReceiver = DisableIncognitoReceiver()

    @SuppressLint("LaunchActivityFromNotification")
    override fun onCreate() {
        super<Application>.onCreate()
        patchInjekt()                                    // 1. Injekt Compose-patch

        GlobalExceptionHandler.initialize(applicationContext, CrashActivity::class.java)  // 2. Crash handler

        // 3. TLS 1.3 backport for Android < 10
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Security.insertProviderAt(Conscrypt.newProvider(), 1)
        }

        // 4. WebView per-process data dir (Android P+ crash workaround)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val process = getProcessName()
            if (packageName != process) WebView.setDataDirectorySuffix(process)
        }

        // 5. Injekt DI graph assembly (see 05-di.md)
        Injekt.importModule(PreferenceModule(this))
        Injekt.importModule(AppModule(this))
        Injekt.importModule(DomainModule())
        // SY -->
        Injekt.importModule(SYDomainModule())
        // SY <--

        setupNotificationChannels()                      // 6. Notification channels

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)  // 7. Process lifecycle hooks

        // 8. Incognito-mode broadcast receiver + notification
        basePreferences.incognitoMode().changes().onEach { … }.launchIn(...)

        // 9. Hardware bitmap threshold default
        basePreferences.hardwareBitmapThreshold().let { … }

        // 10. Theme mode → AppCompat delegate
        setAppCompatDelegateThemeMode(Injekt.get<UiPreferences>().themeMode().get())

        // 11. Glance home-screen widgets
        with(MangaWidgetManager(Injekt.get(), Injekt.get())) { init(...) }
        with(AnimeWidgetManager(Injekt.get(), Injekt.get())) { init(...) }

        // 12. Verbose logcat
        if (!LogcatLogger.isInstalled && networkPreferences.verboseLogging().get()) {
            LogcatLogger.install(AndroidLogcatLogger(LogPriority.VERBOSE))
        }

        initializeMigrator()                             // 13. Version→version migration
    }
    ...
}
```

### What `App.onCreate()` actually does, in order

1. **`patchInjekt()`** — applies `dev.mihon.injekt.patchInjekt`, the Mihon fork's
   Compose-related patch to the Injekt registry. Must run before any `injectLazy`
   delegate is read.
2. **`GlobalExceptionHandler.initialize(..., CrashActivity::class.java)`** —
   installs an uncaught-exception handler that routes crashes to `CrashActivity`
   (which runs in the `:error_handler` process declared in the manifest).
3. **Conscrypt provider** for TLS 1.3 on Android < 10.
4. **WebView data-dir suffix** to avoid the P+ crash when a non-main process
   (e.g. `:error_handler`) touches WebView.
5. **Injekt graph assembly** — four modules are imported in order:
   `PreferenceModule` → `AppModule` → `DomainModule` → `SYDomainModule`. Order
   matters: `AppModule` reads preferences that `PreferenceModule` registers, and
   `DomainModule` reads the `Database`/`AnimeDatabase` that `AppModule` provides.
   See `05-di.md`.
6. **`Notifications.createChannels(this)`** — registers all notification channels.
7. **`ProcessLifecycleOwner` observer** — `App` is its own
   `DefaultLifecycleObserver`; `onStart`/`onStop` forward to
   `SecureActivityDelegate.onApplicationStart/Stopped` (app-lock trigger).
8. **Incognito mode** — registers/unregisters a `BroadcastReceiver` and posts
   an ongoing notification while incognito is on.
9. **Hardware bitmap threshold** — initialises a default from `GLUtil.DEVICE_TEXTURE_LIMIT`.
10. **Theme mode** — applies the user's light/dark/system choice to `AppCompatDelegate`.
11. **Glance widgets** — `MangaWidgetManager` and `AnimeWidgetManager` are
    initialised and bound to the process lifecycle scope.
12. **Logcat** — installs verbose logging if the user opted in.
13. **`Migrator.initialize(...)`** — runs the migration framework
    (`mihon.core.migration.migrations`) from the previous `last_version_code`
    preference to `BuildConfig.VERSION_CODE`. The migrator is **initialised** in
    `App.onCreate()` but **awaited** in `MainActivity.onCreate()` via
    `Migrator.awaitAndRelease()` (see below).

`App` also implements `SingletonImageLoader.Factory.newImageLoader(...)`,
which builds the Coil3 `ImageLoader` with OkHttp fetcher, custom decoders
(`TachiyomiImageDecoder`), manga/anime cover fetchers, and the keyers from
`data/coil/`.

`App.getPackageName()` is overridden to spoof the package name when called from
Chromium (`org.chromium.base.BuildInfo`) — used to change the
`X-Requested-With` header sent by in-app WebViews.

---

## Launcher activity — `MainActivity`

File: `app/src/main/java/eu/kanade/tachiyomi/ui/main/MainActivity.kt` (629 lines)

```kotlin
class MainActivity : BaseActivity() {

    private val libraryPreferences: LibraryPreferences by injectLazy()
    private val preferences: BasePreferences by injectLazy()
    private val animeDownloadCache: AnimeDownloadCache by injectLazy()
    private val downloadCache: MangaDownloadCache by injectLazy()
    private val chapterCache: ChapterCache by injectLazy()
    private val getAnimeIncognitoState: GetAnimeIncognitoState by injectLazy()
    private val getMangaIncognitoState: GetMangaIncognitoState by injectLazy()

    var ready = false                       // gates the splash screen
    private var navigator: Navigator? = null

    init { registerSecureActivity(this) }   // app-lock wiring

    override fun onCreate(savedInstanceState: Bundle?) {
        val isLaunch = savedInstanceState == null
        val splashScreen = if (isLaunch) installSplashScreen() else null
        super.onCreate(savedInstanceState)
        val didMigration = Migrator.awaitAndRelease()   // block until migrations finish

        if (!isTaskRoot) { finish(); return }            // ignore launcher-recreate quirk

        setComposeContent {                              // wrapper that wraps in TachiyomiTheme
            ...
            Navigator(
                screen = HomeScreen,
                disposeBehavior = NavigatorDisposeBehavior(
                    disposeNestedNavigators = false,
                    disposeSteps = true,
                ),
            ) { navigator ->
                ...
                Scaffold(
                    topBar = { AppStateBanners(...) },
                    contentWindowInsets = scaffoldInsets,
                ) { contentPadding ->
                    Box {
                        DefaultNavigatorScreenTransition(navigator, ...)
                        // nav-bar scrim if needed
                    }
                }
                HandleOnNewIntent(context, navigator)
                CheckForUpdates()
                ShowOnboarding()
            }
            ...
        }

        splashScreen?.setKeepOnScreenCondition {
            val elapsed = System.currentTimeMillis() - startTime
            elapsed <= SPLASH_MIN_DURATION || !ready && elapsed <= SPLASH_MAX_DURATION
        }
        setSplashScreenExitAnimation(splashScreen)

        if (isLaunch && libraryPreferences.autoClearItemCache().get()) {
            lifecycleScope.launchIO { chapterCache.clear() }
        }

        externalPlayerResult = registerForActivityResult(StartActivityForResult()) { ... }
    }
}
```

### What `MainActivity.onCreate()` does, in order

1. **Splash screen** — `installSplashScreen()` only on a fresh launch (not on
   config changes). `setKeepOnScreenCondition` keeps the splash visible until
   either `SPLASH_MIN_DURATION` elapses or `ready` becomes true (whichever is
   later, capped by `SPLASH_MAX_DURATION`). `ready` is flipped to `true` by
   individual screens once they have composed their first frame — e.g.
   `BrowseTab` sets `(context as? MainActivity)?.ready = true` inside its
   `Content()` (`BrowseTab.kt:103-105`).
2. **`Migrator.awaitAndRelease()`** — blocks onCreate until any pending
   migrations (initialised in `App.onCreate()`) have completed. The returned
   `didMigration` flag drives the "what's new" changelog dialog.
3. **`isTaskRoot` guard** — finishes early if Android tries to recreate the
   launcher task in a non-root position (a long-standing Android launcher bug).
4. **`setComposeContent { … }`** — extension in
   `app/src/main/java/eu/kanade/tachiyomi/util/view/ViewExtensions.kt:26` that
   wraps `ComponentActivity.setContent` in `TachiyomiTheme` + small typography
   `CompositionLocal` overrides.
5. **Edge-to-edge + status bar colors** — `enableEdgeToEdge(...)` is called
   inside a `LaunchedEffect` that re-evaluates the system-bar style based on
   the current app-state banner (indexing / download-only / incognito).
6. **Root `Navigator(screen = HomeScreen, ...)`** — the single Voyager `Navigator`
   that owns the back stack for the whole app. `disposeNestedNavigators = false`
   keeps child navigators alive across tab switches. See `04-navigation.md`.
7. **`Scaffold` with `AppStateBanners`** top bar — the
   "downloaded only" / "incognito" / "indexing" banners shown above the nav host.
8. **`DefaultNavigatorScreenTransition(navigator, …)`** — renders the current
   screen with a shared-axis-X Material motion transition (defined in
   `app/src/main/java/eu/kanade/presentation/util/Navigator.kt:67`).
9. **`HandleOnNewIntent` / `CheckForUpdates` / `ShowOnboarding`** — side-effect
   composables that route new intents (shortcuts, deep links, search), check
   for app updates, and show the onboarding screen on first run.
10. **`handleIntentAction(intent, navigator)`** — on first launch, reads the
    launching intent and pushes an appropriate `HomeScreen.Tab`/screen (e.g.
    `SHORTCUT_ANIME` + `ANIME_EXTRA` → `navigator.push(AnimeScreen(id))`).
11. **Auto-clear chapter cache** if the user opted in.
12. **External player `ActivityResultLauncher`** registered for the
    `PlayerActivity`/external-player flow.

---

## Startup initializers

- **No `androidx.startup` Initializers** are declared. The manifest has no
  `<provider android:name="androidx.startup.InitializationProvider">` entry.
- **WorkManager** uses its default on-demand initialization (the merged
  `SystemForegroundService` with `foregroundServiceType="dataSync"` is the only
  WorkManager-specific manifest entry). Jobs are scheduled from
  `data/library/`, `data/backup/`, `data/download/`, `data/updater/` etc.
- **Crash handling** is installed manually via
  `GlobalExceptionHandler.initialize(...)` in `App.onCreate()`, not via a
  startup initializer.
- **Coil3 `ImageLoader`** is provided lazily via `App` implementing
  `SingletonImageLoader.Factory` — Coil calls `newImageLoader(...)` on first
  image request, not at startup.
- **Migrations** (`mihon.core.migration.Migrator`) are *initialised* in
  `App.onCreate()` and *awaited* in `MainActivity.onCreate()`; this lets the
  splash screen cover the migration work without blocking the Application
  constructor itself.
- **Asynchronous warm-up**: `AppModule.registerInjectables()` ends with
  `ContextCompat.getMainExecutor(app).execute { get<NetworkHelper>();
  get<MangaSourceManager>(); get<AnimeSourceManager>(); get<Database>();
  get<AnimeDatabase>(); get<MangaDownloadManager>(); get<AnimeDownloadManager>(); }`
  (`AppModule.kt:230-242`) — these Injekt singletons are eagerly constructed
  on the main thread after `onCreate()` returns, to improve cold-start latency
  for the first screen.
