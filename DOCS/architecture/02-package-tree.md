# 02 — Package Tree

This document maps the package structure of the Kuta fork (an Aniyomi fork). The
app is split across multiple Gradle modules; each module owns one or more
package roots. The Android namespace (set in `app/build.gradle.kts:17`) is
`eu.kanade.tachiyomi`, but the forked `applicationId` is `app.kuta`
(`app/build.gradle.kts:20`). The historic `eu.kanade.tachiyomi` / `tachiyomi.*`
package names were intentionally kept to minimise merge churn with upstream
Aniyomi/Mihon.

Source roots used:
- `app/src/main/java/` — the application module (Kotlin + Java).
- `data/src/main/java/` — SQLDelight-backed repository implementations.
- `domain/src/main/java/` — pure-Kotlin domain models, interactors, repository interfaces.
- `source-api/src/commonMain/kotlin/` — multiplatform source/extension API.
- `core/common/src/main/java/`, `core/archive/`, `core-metadata/` — shared core utilities.
- `presentation-widget/src/main/java/` — Glance home-screen widgets.
- `i18n/`, `i18n-aniyomi/` — Moko Resources string catalogs.

## Top-level package roots

```
eu.kanade.tachiyomi        ← main app namespace (app module)
eu.kanade.presentation     ← Compose-only UI pieces (app module, no Android activities)
eu.kanade.domain           ← app-side domain glue: DomainModule + preferences (app module)
eu.kanade.core             ← small app-core utilities (app module)
eu.kanade.test             ← test helpers (app module)

tachiyomi.core.common      ← core/common module
tachiyomi.data             ← data module (repository impls)
tachiyomi.domain           ← domain module (models, interactors, repo interfaces)
tachiyomi.presentation.*   ← presentation-widget + shared presentation modules
tachiyomi.mi               ← SQLDelight-generated anime database (data module)

mihon.data                 ← data module (extension-repo repository impls)
mihon.domain               ← domain module (extension-repo domain layer)
mihon.core.archive         ← core/archive module
mihon.core.migration       ← core/common module (migration framework)

aniyomi.domain             ← domain module (anime-only domain additions)

eu.kanade.tachiyomi.source        ← source-api module (manga sources)
eu.kanade.tachiyomi.animesource   ← source-api module (anime sources)
eu.kanade.tachiyomi.network       ← core/common module (OkHttp, interceptors)
eu.kanade.tachiyomi.util          ← core/common module (misc utils)
```

## `eu.kanade.tachiyomi.*` (app module, top 3 levels)

```
eu.kanade.tachiyomi/
├── App.kt, AppInfo.kt                     ← Application class + version metadata
├── crash/                                  ← CrashActivity + GlobalExceptionHandler
├── data/                                   ← App-side data layer (jobs, caches, downloads, backup, track)
│   ├── backup/                             ← Backup create/restore, format models
│   │   ├── create/, restore/, full/, models/
│   ├── cache/                              ← AnimeCoverCache, MangaCoverCache, ChapterCache, AnimeBackgroundCache
│   ├── coil/                               ← Coil3 ImageLoader components (fetchers, keyers, decoders)
│   ├── database/                           ← Legacy DB model interfaces (AnimeTrack, Episode, Chapter, …)
│   │   ├── anime/, manga/
│   ├── download/                           ← Download managers, queues, caches, providers, jobs
│   │   ├── anime/, manga/
│   ├── export/                             ← LibraryExporter
│   ├── library/                            ← Library update jobs & notifiers
│   │   ├── anime/, manga/
│   ├── notification/                       ← NotificationReceiver, NotificationHandler, channels
│   ├── preference/                         ← SharedPreferencesDataStore
│   ├── saver/                              ← ImageSaver
│   ├── track/                              ← Tracker implementations (MAL, Anilist, Kitsu, Bangumi, …)
│   └── updater/                            ← AppUpdateChecker, AppUpdateDownloadJob
├── di/                                     ← Injekt DI modules
│   ├── AppModule.kt                        ← Infrastructure singletons (DB, network, caches, download mgrs)
│   └── PreferenceModule.kt                 ← PreferenceStore + every *Preferences class
├── extension/                              ← Extension loading/installation
│   ├── anime/                              ← AnimeExtensionManager, loader, installer (Shizuku/PackageInstaller)
│   └── manga/                              ← MangaExtensionManager, loader, installer
├── source/                                 ← Source managers (wrappers around extension sources)
│   ├── anime/                              ← AndroidAnimeSourceManager
│   └── manga/                              ← AndroidMangaSourceManager
├── ui/                                     ← Voyager screens, tabs, activities, viewmodels
│   ├── base/                               ← BaseActivity, activity delegates (Secure, Theming)
│   │   ├── activity/, delegate/
│   ├── browse/                             ← BrowseTab + anime/manga source/extension/migration screens
│   │   ├── anime/, manga/
│   ├── category/                           ← CategoriesTab + anime/manga category screens
│   ├── deeplink/                           ← Deep-link activities + screens (anime/manga search intents)
│   ├── download/                           ← DownloadsTab + anime/manga download queue screens
│   ├── entries/                            ← AnimeScreen / MangaScreen (entry detail pages)
│   │   ├── anime/, manga/
│   ├── history/                            ← HistoriesTab + anime/manga history screens
│   │   ├── anime/, manga/
│   ├── home/                               ← HomeScreen (root screen + TabNavigator + bottom nav)
│   ├── library/                            ← AnimeLibraryTab / MangaLibraryTab
│   │   ├── anime/, manga/
│   ├── main/                               ← MainActivity (launcher activity, hosts root Navigator)
│   ├── more/                               ← MoreTab, OnboardingScreen, NewUpdateScreen
│   ├── player/                             ← PlayerActivity (MPV) + controls/sheets/settings/loader
│   │   ├── controls/, loader/, settings/, utils/
│   ├── reader/                             ← ReaderActivity (manga reader)
│   │   ├── loader/, model/, setting/, viewer/
│   ├── security/                           ← UnlockActivity (app lock)
│   ├── setting/                            ← SettingsScreen + track login activities
│   │   └── track/
│   ├── stats/                              ← StatsTab (anime/manga)
│   │   ├── anime/, manga/
│   ├── storage/                            ← StorageTab (anime/manga disk usage)
│   │   ├── anime/, manga/
│   ├── updates/                            ← UpdatesTab (anime/manga)
│   │   ├── anime/, manga/
│   └── webview/                            ← WebViewScreen / WebViewActivity
├── util/                                   ← App-level utilities (extensions)
│   ├── chapter/, episode/, lang/, storage/, system/, view/
└── widget/                                 ← Legacy Android View widgets (TextInputEditText, etc.)
    └── listener/
```

### `eu.kanade.presentation.*` (app module — Compose UI, no activities)

```
eu.kanade.presentation/
├── util/        ← Navigator.kt: Screen/Tab base classes, DefaultNavigatorScreenTransition, ioCoroutineScope
├── components/  ← Reusable composables (AppBar, AdaptiveSheet, Banners, TabbedScreen, …)
├── theme/       ← TachiyomiTheme
├── library/     ← Library screen composables (anime/manga lists, grids, settings dialogs)
├── entries/     ← Anime/manga entry-detail composables
├── browse/, category/, history/, updates/, more/, track/, webview/, player/, reader/
└── crash/
```

### `eu.kanade.domain.*` (app module — wires domain to Injekt)

```
eu.kanade.domain/
├── DomainModule.kt     ← Injekt module: ~200+ addFactory/addSingletonFactory bindings for interactors & repos
├── SYDomainModule.kt   ← "SY" extra bindings (TachiyomiSY-derived extensions)
├── base/               ← BasePreferences
├── ui/                 ← UiPreferences, NavStyle, StartScreen, theme models
├── source/             ← SourcePreferences, source interactors (incognito, toggle, …)
├── track/              ← TrackPreferences, delayed tracking stores
├── entries/            ← Entry interactors (UpdateAnime/Manga, viewer flags, sync)
├── items/              ← Chapter/episode interactors (SetReadStatus, SyncChaptersWithSource, …)
├── extension/          ← Extension interactors (GetExtensionsByType, TrustExtension, …)
└── download/           ← DeleteEpisodeDownload / DeleteChapterDownload
```

### `tachiyomi.domain.*` (domain module — pure Kotlin)

```
tachiyomi.domain/
├── category/{anime,manga,model}/         ← Category model + interactors + repo interfaces
├── entries/{anime,manga}/                ← Anime/Manga models, interactors, repositories
├── items/{chapter,episode,season}/       ← Chapter/Episode/Season models + interactors + repos
├── history/{anime,manga}/                ← History models + interactors + repos
├── library/{anime,manga,model,service}/  ← Library sort modes, LibraryPreferences
├── source/{anime,manga}/                 ← Source models (Source, StubSource, Pin) + repos + interactors
├── track/{anime,manga}/                  ← Track models + interactors + repos
├── updates/{anime,manga}/                ← Updates-with-relations models + repos + interactors
├── release/                              ← App-release checking (GetApplicationRelease)
├── backup/, download/, storage/, custombuttons/
```

### `tachiyomi.data.*` and `mihon.data.*` (data module)

```
tachiyomi.data/
├── handlers/{anime,manga}/   ← AndroidSqliteDriver-backed DatabaseHandler + paging sources
├── category/{anime,manga}/   ← CategoryRepositoryImpl
├── entries/{anime,manga}/    ← Anime/MangaRepositoryImpl + mappers
├── items/{chapter,episode}/  ← Chapter/EpisodeRepositoryImpl + sanitizers
├── history/{anime,manga}/    ← HistoryRepositoryImpl + mappers
├── source/{anime,manga}/     ← SourceRepositoryImpl + StubSourceRepositoryImpl + PagingSource
├── track/{anime,manga}/      ← TrackRepositoryImpl + mappers
├── updates/{anime,manga}/    ← UpdatesRepositoryImpl
├── release/                  ← ReleaseServiceImpl
├── custombutton/             ← CustomButtonRepositoryImpl
└── DatabaseAdapter.kt        ← SQLDelight column adapters

mihon.data/
└── repository/{anime,manga}/ ← AnimeExtensionRepoRepositoryImpl / MangaExtensionRepoRepositoryImpl

# SQLDelight-generated DB schemas:
tachiyomi.mi.data.AnimeDatabase   ← generated from data/src/main/sqldelightanime/
data.Database                     ← generated from data/src/main/sqldelight/
```

### `source-api` module (Kotlin Multiplatform)

```
eu.kanade.tachiyomi/
├── animesource/        ← AnimeSource, AnimeHttpSource, AnimeCatalogueSource, model/ (SAnime, SEpisode, Video, Hoster, …)
├── source/             ← MangaSource, HttpSource, CatalogueSource, model/ (SManga, SChapter, Page, …)
└── util/               ← JsoupExtensions, JsonExtensions, RxExtension
```

### `core/common` module

```
tachiyomi.core.common/
├── preference/   ← PreferenceStore, AndroidPreference, InMemoryPreferenceStore, TriState, CheckboxState
├── storage/      ← FolderProvider, AndroidStorageFolderProvider, UniFile(Temp|Extensions)
├── i18n/         ← Localize.kt (Moko resources bridge)
└── util/system/  ← ImageUtil, DeviceUtil, WebViewUtil, GLUtil, LogcatExtensions, …

eu.kanade.tachiyomi/
├── network/            ← NetworkHelper, AndroidCookieJar, DohProviders
│   └── interceptor/    ← Cloudflare, RateLimit, UserAgent, WebView, UncaughtException, …
└── util/               ← StringExtensions, Hash, FileExtensions, FFmpegUtils, DiskUtil

mihon.core.migration/   ← Migrator + per-version migrations (used by App.onCreate)
```

## Notes
- The `aniyomi` and `mihon` package prefixes coexist because Aniyomi forked Mihon (which forked Tachiyomi). New anime-specific domain code lives under `aniyomi.domain.*` and `tachiyomi.domain.*.{anime,episode,season}`; manga code stays under the original `tachiyomi.*` paths.
- The `// SY -->` / `// SY <--` comment markers in source files denote code inherited from TachiyomiSY (a now-archived Tachiyomi fork that added extra features). These are kept as merge aids, not active features.
- `eu.kanade.test.DummyTracker` is a test-only tracker used in previews/instrumentation.
