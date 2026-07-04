# 07 — Global Search, Source Search, Search History

> Feature inventory of search-related UI: global anime search (across all
  installed sources), source-level search (within one source), the library
  in-place filter search, and search history. Research/documentation only —
  no source files modified. All paths relative to `/home/z/kuta`.

---

### Global Anime Search

- **Description**: A full-screen search experience that runs the user's query
  in parallel against *every* enabled `AnimeCatalogueSource` and shows
  per-source result cards (sorted: pinned sources first, then by name+lang;
  empty results can be hidden via a "filter results" toggle). A
  source-filter chip row (`All` / `Pinned only`) and an optional
  `extensionFilter` (used by extension-details deep-link) restrict the
  search. Tap a result → `AnimeScreen(fromSource = true)`; tap a source
  header → `BrowseAnimeSourceScreen` with the query pre-filled. Reachable
  from the Library toolbar's "global search" hint card, the Anime Sources
  sub-tab overflow, and `BrowseTab.onReselect`.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/anime/source/globalsearch/GlobalAnimeSearchScreen.kt`
    (Voyager `Screen`; handles single-source-single-result auto-redirect:
    if `extensionFilter != null && total == 1`, shows a `LoadingScreen`
    and `navigator.replace(AnimeScreen)` directly on success)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/anime/source/globalsearch/GlobalAnimeSearchScreenModel.kt`
    (subclass of `AnimeSearchScreenModel`; overrides `getEnabledSources` to
    apply the `PinnedOnly` filter; auto-fires `search()` on init when a
    query or filter is provided)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/anime/source/globalsearch/AnimeSearchScreenModel.kt`
    (abstract base: holds `State` with `items: PersistentMap<AnimeCatalogueSource,
    AnimeSearchItemResult>`; `search()` runs a 5-thread fixed pool
    (`Executors.newFixedThreadPool(5)`) — one `async` per source calling
    `source.getSearchAnime(1, query, source.getFilterList())`; results
    upserted live; `sortComparator` orders by (empty-first, pinned-first,
    name-asc); `toggleFilterResults` flips
    `SourcePreferences.globalSearchFilterState()`)
  - `app/src/main/java/eu/kanade/presentation/browse/anime/GlobalAnimeSearchScreen.kt`
    (Compose UI; per-source cards)
  - `app/src/main/java/eu/kanade/presentation/browse/GlobalSearchResultItems.kt`
    (shared anime+manga result-card composables)
  - `app/src/main/java/eu/kanade/presentation/browse/GlobalSerachCard.kt`
    (sic — `GlobalSerachCard.kt` typo in source — per-source card shell)
- **Status**: `modify`
  - Core anime feature, definitely staying. The UI is a candidate for the
    redesign; the back-end `AnimeSearchScreenModel` stays.
- **Dependencies**:
  - Depends on: `AnimeSourceManager.getCatalogueSources()`,
    `AnimeExtensionManager.installedExtensionsFlow` (for `extensionFilter`
    resolution), `NetworkToLocalAnime.await(...)` (inserts each remote
    result into the anime DB so `GetAnime.subscribe(url, source)` can later
    refresh it), `SourcePreferences.enabledLanguages()` /
    `disabledAnimeSources()` / `pinnedAnimeSources()` /
    `globalSearchFilterState()`.
  - Depended on by: `BrowseTab.onReselect`, `AnimeSourcesTab` overflow
    action, `AnimeLibraryTab` (the global-search hint card at the top of
    library search results — see `GlobalSearchItem.kt`), `MainActivity`
    deep-link / search-intent handler (search intents push
    `GlobalAnimeSearchScreen(query)`).
- **Notes**:
  - The 5-thread fixed pool (`Executors.newFixedThreadPool(5)`) is created
    per `AnimeSearchScreenModel` instance and never explicitly shut down —
    it's tied to `ioCoroutineScope` which is cancelled on screen-model
    disposal. With ~30+ sources this serializes some searches; user-visible
    as staggered card population.
  - "Reuse previous results if possible": if the user re-submits the same
    query (e.g. toggles source filter), already-completed sources keep
    their results; only previously-unsearched sources start fresh.
  - The single-source-single-result auto-redirect is used by the
    "extension filter" deep-link (e.g. `AnimeExtensionDetailsScreen` →
    tap source → search opens here with `extensionFilter = pkgName`).
  - There is **no** dedicated "search history" UI here — see the
    "Search history" feature entry below.

---

### Source-level Search (Within a Single Source)

- **Description**: Search inside one source's catalog. Reached by tapping a
  source from the Anime Sources sub-tab, then typing into the source's
  toolbar search box (or by tapping a genre chip on `AnimeScreen`, or by
  tapping a source header in `GlobalAnimeSearchScreen`). Supports the
  source's full `AnimeFilterList` via a filter sheet (source-defined
  genres, sorts, etc.). Paged via `androidx.paging.Pager(pageSize = 25)`.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/anime/source/browse/BrowseAnimeSourceScreen.kt`
    (toolbar search box; `FilterChip` row for Popular/Latest/Search; filter
    sheet dialog)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/anime/source/browse/BrowseAnimeSourceScreenModel.kt`
    (`Listing.Search(query, filters)`; `search(query, filters)` /
    `searchGenre(name)`; `setFilters`; `resetFilters`; pager re-keys on
    listing change)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/anime/source/browse/SourceFilterAnimeDialog.kt`
    (filter sheet UI rendering `AnimeFilterList`)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/anime/migration/search/AnimeSourceSearchScreen.kt`
    (migration-specific variant — pushed from "Open in source" inside
    `MigrateAnimeSearchScreen`)
  - Source API:
    `source-api/.../animesource/AnimeCatalogueSource.kt` — declares
    `getSearchAnime(page: Int, query: String, filters: AnimeFilterList):
    AnimesPage`; default `AnimeFilterList()` returned by `getFilterList()`.
  - Paging source: `data/.../source/anime/AnimeSourcePagingSource.kt` →
    calls `source.getSearchAnime(page, query, filters)` per page.
  - Domain interactor: `domain/.../source/anime/interactor/GetRemoteAnime.kt`.
- **Status**: `keep`
- **Dependencies**:
  - Depends on: `AnimeSourceManager.getOrStub(sourceId)`,
    `GetRemoteAnime.subscribe(sourceId, query, filters)` (PagingSource),
    `NetworkToLocalAnime` + `GetAnime.subscribe` (per-row refresh),
    `SourcePreferences.hideInAnimeLibraryItems()` (client-side filter),
    `SourcePreferences.sourceDisplayMode()`.
  - Depended on by: `AnimeSourcesTab`, `GlobalAnimeSearchScreen` ("Open in
    source" action), `AnimeScreen` (genre chip → `searchGenre`),
    `MigrateAnimeSearchScreen` (per-source drill-down).
- **Notes**:
  - `BrowseAnimeSourceScreen.search(query)` and `searchGenre(name)` are
    exposed as `suspend fun`s on the `Screen` instance itself — they send
    to a `companion object` `Channel<SearchType>` shared across all
    instances. This means only one `BrowseAnimeSourceScreen` can drive
    search at a time (acceptable since only one is on-screen).
  - `searchGenre(name)` walks the source's filter list looking for a
    matching `TriState` / `CheckBox` / `Select` filter; falls back to a
    text search if no match — used by `AnimeScreen` genre chips.
  - Filters state survives screen recomposition because `State.filters`
    is held in the screen-model; resetting returns to `source.getFilterList()`
    (a fresh instance from the source).

---

### Library In-place Search (Filter Library by Text)

- **Description**: The search box in the Library tab toolbar. Does *not*
  navigate away — filters the current category (or all categories when
  `categoryTabs` is off / when search is active) in-place by case-insensitive
  title match. Debounced (`SEARCH_DEBOUNCE_MILLIS`, defined in
  `eu.kanade.presentation.components`). While a search query is active, the
  category pager shows all matching anime across all categories (pager
  becomes single-page). Clearing the query restores the per-category pager.
  A "global search" hint card at the top of the results offers to jump to
  `GlobalAnimeSearchScreen` with the same query.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/ui/library/anime/AnimeLibraryTab.kt`
    (`LibraryToolbar(searchQuery = state.searchQuery, onSearchQueryChange =
    screenModel::search, ...)`; `onGlobalSearchClicked = { navigator.push(
    GlobalAnimeSearchScreen(state.value.searchQuery ?: "")) }`)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/library/anime/AnimeLibraryScreenModel.kt`
    (`fun search(query: String?)` updates `State.searchQuery`; the `init`
    flow chains `state.map { it.searchQuery }.debounce(SEARCH_DEBOUNCE_MILLIS)`
    into the filter+sort pipeline; `AnimeLibraryItem.matches(query)` does
    the per-item title matching)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/library/anime/AnimeLibraryItem.kt`
    (`matches(query: String): Boolean` — title, author, artist substring)
  - `app/src/main/java/eu/kanade/presentation/library/components/LibraryToolbar.kt`
    (search box composable)
  - `app/src/main/java/eu/kanade/presentation/library/components/GlobalSearchItem.kt`
    (the "Search globally" hint card shown above in-library search results)
  - Cross-tab channel: `AnimeLibraryTab.queryEvent: Channel<String>` +
    `suspend fun search(query: String)` — `HomeScreen.search()` and
    `MainActivity.handleIntentAction` use this to drive library search from
    outside Compose (e.g. from a search-intent deep link).
- **Status**: `modify`
  - Stays, but the UI of the toolbar / hint card is up for redesign.
- **Dependencies**:
  - Depends on: `AnimeLibraryScreenModel.state.searchQuery`, the same
    `GetLibraryAnime` flow that drives the library (search is applied
    client-side after filtering+sorting), `AnimeLibraryItem.matches`.
  - Depended on by: `HomeScreen.search()` (cross-tab driver), search-intent
    deep-link routing in `MainActivity`.
- **Notes**:
  - Search is *not* persisted — `state.searchQuery` lives only in the
    screen-model's `StateScreenModel.State`. Navigating away and back
    clears it.
  - The "global search" hint card (`GlobalSearchItem`) is what bridges
    in-library search to `GlobalAnimeSearchScreen` — it pushes the screen
    with the current query.
  - `BackHandler` is registered when `state.searchQuery != null` so the
    hardware back button clears the query instead of navigating away.

---

### History Tab Search (Filter Recently Watched)

- **Description**: The search box in the History tab (rendered by the shared
  `TabbedScreen` shell). Filters the anime-history list in-place by
  case-insensitive title match. The query is hoisted into
  `HistoriesTab.Content()` (so it survives sub-tab swaps between anime and
  manga history) and forwarded to the active sub-tab's screen model.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/ui/history/HistoriesTab.kt`
    (`animeHistoryScreenModel.query` collected as state; passed to
    `TabbedScreen(animeSearchQuery = ..., onChangeAnimeSearchQuery =
    animeHistoryScreenModel::search, ...)`)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/history/anime/AnimeHistoryScreenModel.kt`
    (`private val _query: MutableStateFlow<String?>` + `fun search(query:
    String?)`; `init { _query.collectLatest { getHistory.subscribe(query ?: "") } }`)
  - DB-side filter: `data/src/main/sqldelightanime/view/animehistoryView.sq`
    — the `animehistory` query has `WHERE lower(title) LIKE ('%' || :query
    || '%')`.
  - `app/src/main/java/eu/kanade/presentation/components/TabbedScreen.kt`
    (shared shell that hosts the search box).
- **Status**: `modify`
- **Dependencies**:
  - Depends on: `GetAnimeHistory.subscribe(query)`, the `animehistoryView`
    SQL view, `TabbedScreen` shell.
  - Depended on by: `HistoriesTab`.
- **Notes**:
  - Search is in-memory only (not persisted).
  - The query is a `LIKE '%query%'` against `lower(title)` — no FTS, no
    indexing; fine for the typical history-list size but would scale
    poorly if history grew unbounded.
  - This is part of the broader History tab feature — see `08-history.md`.

---

### Search History (Does Not Exist)

- **Description**: **There is no persisted search-history feature in Kuta.**
  A grep across the entire repo for `search.?history`, `SearchHistory`,
  `recentQueries`, `recent_search`, and `searchHistory` returns zero hits.
  All search boxes in the app (library, history, browse sources,
  extensions, global search, source-level search, migrate search) are
  stateless text fields backed by an in-memory `MutableStateFlow<String?>`
  or `State.searchQuery` field on the screen model. Submitting a query
  fires the search; navigating away and back clears the field.
- **Location**:
  - N/A — no files implement this.
  - Closest things to "search history" in the codebase:
    - `SourcePreferences.lastUsedAnimeSource()` — remembers the *source*
      last browsed (not the query).
    - `AnimeHistory` table — records *watched* episodes, not *searched*
      terms.
    - `extension_repos` table — extension-repo URLs (not search terms).
- **Status**: `TBD`
  - Whether to add a search-history feature (recent-queries dropdown,
    persisted across launches) is a product decision. Most modern
    consumer apps do have this; the current Aniyomi upstream does not.
  - If added, the natural persistence layer is `SharedPreferences` via a
    new `*Preferences` accessor returning a `Preference<Set<String>>` (the
    existing `PreferenceStore.getStringSet(...)` pattern), keyed e.g.
    `__APP_STATE_recent_anime_searches`.
- **Dependencies**:
  - N/A — feature does not exist.
- **Notes**:
  - This is documented explicitly so the inventory is complete. If the
    orchestrator/user wants this feature, it's a greenfield add rather
    than a modify.
  - Note the in-memory `State.searchQuery` fields are also not restored
    on process death — Voyager's `StateScreenModel` does not survive
    process kill by default (no `SavedStateHandle` integration). A
    search-history feature would naturally fix the "lost query on
    rotation/kill" UX issue as a side effect.

---

### Source Filters / Pinned Sources (Search-relevant Preferences)

- **Description**: Not a single screen, but a cluster of preferences that
  govern *which* sources participate in global search and how they're
  ordered. Includes: enabled-languages set, disabled-sources set,
  pinned-sources set, the global-search "filter results" toggle (hide
  sources with no matches), and the per-source "hide in library items"
  toggle on browse. Surfaced via the Anime Sources filter screen and the
  per-source options dialog.
- **Location**:
  - `app/src/main/java/eu/kanade/domain/source/service/SourcePreferences.kt`
    — keys: `enabledLanguages` (`source_languages`, default =
    `LocaleHelper.getDefaultEnabledLanguages()` = the device locale + en),
    `disabledAnimeSources` (`hidden_anime_catalogues`), `pinnedAnimeSources`
    (`pinned_anime_catalogues`), `globalSearchFilterState`
    (`__APP_STATE_has_filters_toggle_state`),
    `hideInAnimeLibraryItems` (`browse_hide_in_anime_library_items`),
    `showNsfwSource` (`show_nsfw_source`, default true).
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/anime/source/AnimeSourcesFilterScreen.kt`
    + `AnimeSourcesFilterScreenModel.kt` (UI for toggling
    `enabledLanguages` and `disabledAnimeSources`)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/browse/anime/extension/AnimeExtensionFilterScreen.kt`
    + `AnimeExtensionFilterScreenModel.kt` (UI for toggling
    `enabledLanguages` from the extensions sub-tab)
  - `app/src/main/java/eu/kanade/domain/source/anime/interactor/ToggleAnimeSourcePin.kt`,
    `ToggleAnimeSource.kt`, `GetEnabledAnimeSources.kt`.
- **Status**: `keep`
- **Dependencies**:
  - Depends on: `SourcePreferences`, `AnimeSourceManager`.
  - Depended on by: `AnimeSourcesScreenModel`, `AnimeExtensionsScreenModel`,
    `AnimeSearchScreenModel.getEnabledSources()`,
    `BrowseAnimeSourceScreenModel` (hide-in-library filter).
- **Notes**:
  - `globalSearchFilterState` is named confusingly — it's actually the
    "only show sources with results" toggle on the global-search screen
    (not a "filter state" in the search-query sense).
  - `enabledLanguages` defaults to a 2-element set: the device's locale
    language + "en" (see `LocaleHelper.getDefaultEnabledLanguages()`).
    This is why a fresh install only shows English sources + the device's
    local-language sources until the user enables more.
  - Pinned sources are stored as a `Set<String>` of source-id strings —
    a source is "pinned" iff `"${source.id}" in pinnedAnimeSources()`.
  - Disabled sources are excluded from the Anime Sources sub-tab *and*
    from global search (see `AnimeSearchScreenModel.getEnabledSources`:
    `disabledAnimeSources` filtered out).
