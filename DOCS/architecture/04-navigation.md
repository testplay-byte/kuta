# 04 — Navigation

## Navigation library: Voyager 1.0.1

Confirmed. The version catalog `gradle/libs.versions.toml:10` declares:

```toml
[versions]
voyager = "1.0.1"

[libraries]
voyager-navigator = { module = "cafe.adriel.voyager:voyager-navigator", version.ref = "voyager" }
voyager-screenmodel = { module = "cafe.adriel.voyager:voyager-screenmodel", version.ref = "voyager" }
voyager-tab-navigator = { module = "cafe.adriel.voyager:voyager-tab-navigator", version.ref = "voyager" }
voyager-transitions = { module = "cafe.adriel.voyager:voyager-transitions", version.ref = "voyager" }

[bundles]
voyager = ["voyager-navigator", "voyager-screenmodel", "voyager-tab-navigator", "voyager-transitions"]
```

And `app/build.gradle.kts:281` pulls in the bundle:

```kotlin
implementation(libs.bundles.voyager)
```

So all four Voyager artifacts (navigator, screenmodel, tab-navigator,
transitions) are available to the app. No Jetpack Navigation, no Compose
Destinations, no Router/Decompose.

## Custom `Screen` / `Tab` base classes

Voyager is wrapped by two thin base classes in
`app/src/main/java/eu/kanade/presentation/util/Navigator.kt`:

```kotlin
// Navigator.kt:37-44
interface Tab : cafe.adriel.voyager.navigator.tab.Tab {
    suspend fun onReselect(navigator: Navigator) {}

    @Composable
    fun currentNavigationStyle(): NavStyle = uiPreferences.navStyle().collectAsState().value
}

// Navigator.kt:46-49
abstract class Screen : Screen {                       // cafe.adriel.voyager.core.screen.Screen
    override val key: ScreenKey = uniqueScreenKey
}
```

- **Every screen in the app extends `eu.kanade.presentation.util.Screen`**
  (not Voyager's `Screen` directly). This gives each instance a stable
  `uniqueScreenKey` automatically, which Voyager uses for state restoration.
- **Every tab extends `eu.kanade.presentation.util.Tab`**, which adds an
  `onReselect(navigator)` hook (called when the user taps the already-selected
  bottom-nav item) and a `currentNavigationStyle()` helper.

## Root nav graph — single `Navigator` rooted at `HomeScreen`

File: `app/src/main/java/eu/kanade/tachiyomi/ui/main/MainActivity.kt:193-285`

The entire app UI lives under **one** Voyager `Navigator`, created in
`MainActivity.onCreate()` via `setComposeContent { … }`:

```kotlin
Navigator(
    screen = HomeScreen,                                            // root screen
    disposeBehavior = NavigatorDisposeBehavior(
        disposeNestedNavigators = false,                            // keep tab state alive
        disposeSteps = true,
    ),
) { navigator ->
    LaunchedEffect(navigator) {
        this@MainActivity.navigator = navigator
        if (isLaunch) {
            handleIntentAction(intent, navigator)                   // route launch intent
            preferences.incognitoMode().set(false)                  // reset incognito on relaunch
        }
    }
    ...
    Scaffold(
        topBar = { AppStateBanners(...) },
        contentWindowInsets = scaffoldInsets,
    ) { contentPadding ->
        Box {
            DefaultNavigatorScreenTransition(navigator, ...)        // renders current screen
            // optional nav-bar scrim
        }
    }
    HandleOnNewIntent(context, navigator)
    CheckForUpdates()
    ShowOnboarding()
}
```

- `HomeScreen` (an `object` — see below) is the single root. Pushed screens form
  a flat back stack under this one Navigator.
- `disposeNestedNavigators = false` is important: `HomeScreen` itself contains a
  `TabNavigator` (the bottom nav), and we do **not** want Voyager to dispose
  tab state when navigating away from `HomeScreen` and back.

## `HomeScreen` — the root screen and the bottom-nav `TabNavigator`

File: `app/src/main/java/eu/kanade/tachiyomi/ui/home/HomeScreen.kt`

```kotlin
object HomeScreen : Screen() {                                       // Navigator.kt:46

    private val librarySearchEvent = Channel<String>()
    private val openTabEvent = Channel<Tab>()
    private val showBottomNavEvent = Channel<Boolean>()

    private const val TAB_FADE_DURATION = 200
    private const val TAB_NAVIGATOR_KEY = "HomeTabs"

    private val uiPreferences: UiPreferences by injectLazy()
    private val defaultTab = uiPreferences.startScreen().get().tab
    private val moreTab = uiPreferences.navStyle().get().moreTab

    @Composable
    override fun Content() {
        val navStyle by uiPreferences.navStyle().collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        TabNavigator(
            tab = defaultTab,
            key = TAB_NAVIGATOR_KEY,
        ) { tabNavigator ->
            CompositionLocalProvider(LocalNavigator provides navigator) {
                Scaffold(
                    startBar = {
                        if (isTabletUi()) {
                            NavigationRail {
                                navStyle.tabs.fastForEach { NavigationRailItem(it) }
                            }
                        }
                    },
                    bottomBar = {
                        if (!isTabletUi()) {
                            AnimatedVisibility(
                                visible = bottomNavVisible && tabNavigator.current != navStyle.moreTab,
                                enter = expandVertically(),
                                exit = shrinkVertically(),
                            ) {
                                NavigationBar {
                                    navStyle.tabs.fastForEach { NavigationBarItem(it) }
                                }
                            }
                        }
                    },
                ) { contentPadding ->
                    AnimatedContent(
                        targetState = tabNavigator.current,
                        transitionSpec = {
                            materialFadeThroughIn(initialScale = 1f, durationMillis = TAB_FADE_DURATION) togetherWith
                                materialFadeThroughOut(durationMillis = TAB_FADE_DURATION)
                        },
                        label = "tabContent",
                    ) {
                        tabNavigator.saveableState(key = "currentTab", it) { it.Content() }
                    }
                }
            }
            ...
        }
    }

    sealed interface Tab {
        data class AnimeLib(val animeIdToOpen: Long? = null) : Tab
        data class Library(val mangaIdToOpen: Long? = null) : Tab
        data object Updates : Tab
        data object History : Tab
        data class Browse(val toExtensions: Boolean = false, val anime: Boolean = false) : Tab
        data class More(val toDownloads: Boolean) : Tab
    }
}
```

### Key points

- `HomeScreen` is a singleton `object` (not a `data class`) — it is the only
  screen in the back stack that is stateless and re-entrant.
- The outer `Navigator` (from `MainActivity`) hosts `HomeScreen`. Inside
  `HomeScreen.Content()`, a **`TabNavigator`** is created with
  `tab = defaultTab` (the user's chosen start screen). This is Voyager's
  bottom-nav primitive.
- The `CompositionLocalProvider(LocalNavigator provides navigator)` line
  re-exposes the **outer** screen `Navigator` to tab contents, so individual
  tabs can `navigator.push(SomeScreen(...))` to navigate above the bottom bar
  (the `TabNavigator` would otherwise shadow `LocalNavigator`).
- The bottom bar (phone) or navigation rail (tablet) is rendered from
  `navStyle.tabs` — see the next section.
- Tab content transitions use `materialFadeThroughIn/Out` (200 ms) from
  `soup.compose.material.motion`.
- Inter-tab communication happens through three `Channel`s exposed as
  `suspend fun`s on `HomeScreen`: `search(query)`, `openTab(tab)`,
  `showBottomNav(show)`. Callers (e.g. `MainActivity.handleIntentAction`,
  `MoreTab`) call these to drive the UI from outside Compose.

## The six top-level destinations — `NavStyle`

File: `app/src/main/java/eu/kanade/domain/ui/model/NavStyle.kt`

```kotlin
enum class NavStyle(
    val titleRes: StringResource,
    val moreTab: Tab,
) {
    MOVE_MANGA_TO_MORE(titleRes = AYMR.strings.pref_bottom_nav_no_manga,    moreTab = MangaLibraryTab),
    MOVE_UPDATES_TO_MORE(titleRes = AYMR.strings.pref_bottom_nav_no_updates, moreTab = UpdatesTab),
    MOVE_HISTORY_TO_MORE(titleRes = AYMR.strings.pref_bottom_nav_no_history, moreTab = HistoriesTab),
    MOVE_BROWSE_TO_MORE(titleRes = AYMR.strings.pref_bottom_nav_no_browse,   moreTab = BrowseTab),
    ;

    val tabs: List<Tab>
        get() = mutableListOf(
            AnimeLibraryTab,
            MangaLibraryTab,
            UpdatesTab,
            HistoriesTab,
            BrowseTab,
            MoreTab,
        ).apply { remove(this@NavStyle.moreTab) }
}
```

There are **six** top-level tab types. The user picks one (via
`UiPreferences.navStyle()`) to "move to More" — i.e. to hide from the bottom
nav and surface only inside the `MoreTab` screen. The five remaining tabs are
rendered in the bottom bar (or rail) in this fixed order:

| Order | Tab object | File | `TabOptions.index` |
|---|---|---|---|
| 1 | `AnimeLibraryTab` | `app/…/ui/library/anime/AnimeLibraryTab.kt` | `0u` |
| 2 | `MangaLibraryTab` | `app/…/ui/library/manga/MangaLibraryTab.kt` | dynamic |
| 3 | `UpdatesTab` | `app/…/ui/updates/UpdatesTab.kt` | dynamic |
| 4 | `HistoriesTab` | `app/…/ui/history/HistoriesTab.kt` | dynamic |
| 5 | `BrowseTab` | `app/…/ui/browse/BrowseTab.kt` | `3u` |
| 6 | `MoreTab` | `app/…/ui/more/MoreTab.kt` | `4u` |

(`TabOptions.index` is a Voyager field used for default sorting; the actual
bottom-bar order is the list order from `NavStyle.tabs`, not the index.)

A `Tab` is a `data object` implementing `eu.kanade.presentation.util.Tab`:

```kotlin
// AnimeLibraryTab.kt:69-93
data object AnimeLibraryTab : Tab {

    override val options: TabOptions
        @Composable get() {
            val isSelected = LocalTabNavigator.current.current is AnimeLibraryTab
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_library_enter)
            return TabOptions(
                index = 0u,
                title = stringResource(AYMR.strings.label_anime_library),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        ...
    }
}
```

The animated-vector-drawable `icon` flips state when the tab is selected, giving
the playful "icon morph" effect on the bottom nav.

## Screen example — pushing a detail screen

File: `app/src/main/java/eu/kanade/tachiyomi/ui/entries/anime/AnimeScreen.kt:78-100`

```kotlin
class AnimeScreen(
    private val animeId: Long,
    val fromSource: Boolean = false,
) : Screen(), AssistContentScreen {                          // eu.kanade.presentation.util.Screen

    private var assistUrl: String? = null
    override fun onProvideAssistUrl() = assistUrl

    @Composable
    override fun Content() {
        if (!ifAnimeSourcesLoaded()) { LoadingScreen(); return }
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        ...
        val screenModel =
            rememberScreenModel { AnimeScreenModel(context, lifecycleOwner.lifecycle, animeId, fromSource) }
        ...
    }
}
```

Screens are pushed from anywhere with access to `LocalNavigator`:

```kotlin
navigator.push(AnimeScreen(animeId, fromSource = true))
navigator.push(MangaScreen(mangaId))
navigator.replace(...)
navigator popUntilRoot()
```

`AssistContentScreen` (defined in `Navigator.kt:63-65`) is an opt-in interface
for screens that can supply a URL to Google Assistant; `MainActivity.onProvideAssistContent`
checks `navigator?.lastItem is? AssistContentScreen` and forwards the URL.

## Screen-transitions and nav helpers

File: `app/src/main/java/eu/kanade/presentation/util/Navigator.kt:67-102`

```kotlin
@Composable
fun DefaultNavigatorScreenTransition(
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val slideDistance = rememberSlideDistance()
    ScreenTransition(
        navigator = navigator,
        transition = {
            materialSharedAxisX(
                forward = navigator.lastEvent != StackEvent.Pop,
                slideDistance = slideDistance,
            )
        },
        modifier = modifier,
    )
}

@Composable
fun ScreenTransition(
    navigator: Navigator,
    transition: AnimatedContentTransitionScope<Screen>.() -> ContentTransform,
    modifier: Modifier = Modifier,
    content: ScreenTransitionContent = { it.Content() },
) {
    AnimatedContent(
        targetState = navigator.lastItem,
        transitionSpec = transition,
        modifier = modifier,
        label = "transition",
    ) { screen ->
        navigator.saveableState("transition", screen) { content(screen) }
    }
}
```

- The default transition is **Material's shared-axis-X** (`materialSharedAxisX`
  from `io.github.fornewid:material-motion-compose-core`), reversed on `Pop`.
  This is what `MainActivity` renders around its root `Navigator`.
- The `materialFadeThroughIn/Out` transition is used **between tabs** inside
  `HomeScreen` (different transition than between pushed screens).
- `ScreenModelStore.getOrPutDependency(...)` is wrapped into a
  `ScreenModel.ioCoroutineScope` extension (`Navigator.kt:55-61`) that gives
  each `ScreenModel` an `IO`-dispatcher `CoroutineScope` auto-cancelled on
  disposal.
- `LocalBackPress` (`Navigator.kt:35`) is a `staticCompositionLocalOf` for
  invoking the host activity's back press, used by screens that need to
  explicitly pop the activity (not the navigator).

## Notes / gotchas

- **One `Navigator`, one `TabNavigator`.** The app does **not** use per-tab
  nested `Navigator`s (a pattern Voyager supports). Pushed detail screens
  (AnimeScreen, MangaScreen, SettingsScreen, …) share the single root
  `Navigator`, and the bottom-nav `TabNavigator` lives only inside
  `HomeScreen.Content()`. This is why `disposeNestedNavigators = false` is set
  on the root — there are no nested navigators to dispose.
- **`HomeScreen` is an `object`.** State on it (the three `Channel`s) is
  process-singleton; calling `HomeScreen.openTab(...)` from any coroutine
  works because there is exactly one `HomeScreen` instance in the back stack.
- **`BrowseTab` flips `MainActivity.ready`** inside its `Content()` so the
  splash screen can dismiss when the user's default tab is Browse. Other tabs
  set `ready` elsewhere in their composition.
- **Deep-link / shortcut routing** is done in
  `MainActivity.handleIntentAction(...)` (lines ~440-560), which translates
  intent actions like `Constants.SHORTCUT_ANIME` into `HomeScreen.Tab.*`
  values and calls `HomeScreen.openTab(...)`; for `SHORTCUT_ANIME`/`SHORTCUT_MANGA`
  it also `navigator.popUntilRoot()` then `navigator.push(AnimeScreen(id))`.
