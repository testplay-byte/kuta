# 04 — Tracking Integrations

> Feature inventory of the tracker subsystem in Kuta (Aniyomi fork). Trackers
> sync anime/manga progress (status, episode count, score, dates) with remote
> services via per-service OkHttp clients + OAuth. The user has decided to
> **keep only AniList, MyAnimeList, Shikimori, Bangumi**; the remaining seven
> trackers (Kitsu, Simkl, Komga, Kavita, Suwayomi, Jellyfin, MangaUpdates)
> will be removed.
>
> Source-state at time of writing: no source files were modified. Paths are
> relative to the repo root (`/home/z/kuta`).

---

## Feature 1 — Tracker Infrastructure

- **Description**: The shared plumbing that all trackers sit on: the `Tracker`
  interface and `BaseTracker` abstract class, the `TrackerManager` registry
  (one singleton instance per tracker, indexed by stable id), the
  `TrackPreferences` credential/OAuth store, the per-tracker OkHttp
  interceptors that add auth headers and refresh expired tokens, the
  `anime_sync` / `manga_sync` DB tables that persist linked tracks, and the
  shared UI scaffolding (track search dialog, status/score/date selectors,
  settings screen) used by every tracker regardless of service.
- **Location**:
  - Interface: `app/src/main/java/eu/kanade/tachiyomi/data/track/Tracker.kt`
    (`id`, `name`, `client`, `isLoggedIn`, `login`, `logout`, `getScoreList`,
    `getCompletionStatus`, `animeService`/`mangaService` cast helpers)
  - Base: `app/.../data/track/BaseTracker.kt` (holds `TrackPreferences` +
    `NetworkHelper`; `isLoggedIn` = username+password both non-empty;
    `isLoggedInFlow` combines the two pref flows; `logout()` clears credentials)
  - Sub-interfaces: `AnimeTracker.kt` (`getStatusListAnime`, `getWatchingStatus`,
    `getRewatchingStatus`, `searchAnime`, `bind`, `update`, `refresh`,
    `setRemoteAnimeStatus`/`setRemoteLastEpisodeSeen`/`setRemoteScore`/
    `setRemoteStartDate`/`setRemoteFinishDate`/`setRemotePrivate`),
    `MangaTracker.kt` (mirror for manga),
    `EnhancedAnimeTracker.kt` (auto-match: `accept(source)`, `match(anime)`,
    `isTrackFrom`, `migrateTrack` — never prompts the user; used by Komga/Kavita/Suwayomi/Jellyfin),
    `EnhancedMangaTracker.kt`, `DeletableAnimeTracker.kt` (`delete(track)`),
    `DeletableMangaTracker.kt`
  - Registry: `app/.../data/track/TrackerManager.kt` — instantiates all 11
    trackers with stable ids (MAL=1, AniList=2, Kitsu=3, Shikimori=4, Bangumi=5,
    Komga=6, MangaUpdates=7, Kavita=8, Suwayomi=9, Simkl=101, Jellyfin=102),
    exposes `trackers: List<Tracker>`, `loggedInTrackers()`,
    `loggedInTrackersFlow()`, `get(id)`, `getAll(ids)`
  - Prefs: `app/.../domain/track/service/TrackPreferences.kt` — `trackUsername(tracker)` /
    `trackPassword(tracker)` (private-key prefs `pref_mangasync_username_<id>` /
    `pref_mangasync_password_<id>`), `trackAuthExpired(tracker)`, `trackToken(tracker)`,
    `setCredentials(...)`, `anilistScoreType()`, `autoUpdateTrack()` (default true),
    `autoUpdateTrackOnMarkRead()` (`AutoTrackState` enum: ALWAYS / ONLY_FRESH / NEVER),
    `trackOnAddingToLibrary()`, `showNextEpisodeAiringTime()`
  - OAuth base activity: `app/.../ui/setting/track/BaseOAuthLoginActivity.kt`
    (custom-scheme deep-link receiver used by AniList/MAL/Shikimori/Bangumi/Kitsu/Simkl;
    shows `LoadingScreen` while `handleResult(data: Uri?)` runs)
  - Plain login activity: `app/.../ui/setting/track/TrackLoginActivity.kt`
    (username/password form for trackers without OAuth — MangaUpdates, Komga/Kavita via API key)
  - DB tables: `data/src/main/sqldelightanime/dataanime/anime_sync.sq` (`anime_sync`,
    UNIQUE(anime_id, sync_id), FK→animes ON DELETE CASCADE) +
    `data/src/main/sqldelight/data/manga_sync.sq` (mirror for manga)
  - Domain interactors:
    - `app/.../domain/track/anime/interactor/AddAnimeTracks.kt` (`bind(tracker, item, animeId)` + `bindEnhancedTrackers(anime, source)`)
    - `app/.../domain/track/anime/interactor/TrackEpisode.kt` (called from player on episode-seen)
    - `app/.../domain/track/anime/interactor/SyncEpisodeProgressWithTrack.kt`
    - `app/.../domain/track/anime/interactor/RefreshAnimeTracks.kt` (pull latest from remote)
    - `domain/.../track/anime/interactor/{GetAnimeTracks, GetTracksPerAnime, InsertAnimeTrack, DeleteAnimeTrack}.kt`
  - Search models: `app/.../data/track/model/AnimeTrackSearch.kt`, `MangaTrackSearch.kt`
- **Status**: `keep` — but `modify` to remove the 7 dropped trackers from
  `TrackerManager.trackers`, delete their subdirectories, and prune the
  `manga_sync` table path (since manga UI is gated out, see `02-package-tree.md`).
- **Dependencies**:
  - Depends on: `NetworkHelper` (shared `OkHttpClient`); `PreferenceStore`
    (private-key prefs for credentials/tokens); `anime_sync`/`manga_sync` SQLDelight tables;
    `AnimeSourceManager`/`MangaSourceManager` (for `EnhancedTracker.accept(source)`).
  - Depended on by: `PlayerViewModel.updateTrackEpisodeSeen()` (called on
    episode-seen threshold); `AnimeScreenModel` (track-info dialog);
    `AnimeScreen.addToLibrary` flow (`trackOnAddingToLibrary` triggers
    `bindEnhancedTrackers`); `SettingsTrackingScreen`; backup/restore jobs.
- **Notes**:
  - Per-tracker OAuth token refresh is implemented in each tracker's
    `*Interceptor.kt` (e.g. `MyAnimeListInterceptor.refreshToken(chain)` calls
    `MyAnimeListApi.refreshTokenRequest(oauth)` and persists the new token via
    `myanimelist.saveOAuth(it)`; on 401 it calls `setAuthExpired()` and throws
    `MALTokenExpired`). AniList's interceptor is simpler — no refresh-token
    flow, just logs out when expired (`AnilistInterceptor` line ~33).
  - `autoUpdateTrack()` gates whether the player / reader pushes updates to
    trackers at all. `autoUpdateTrackOnMarkRead()` controls the same for the
    "mark as seen" UI action (vs. only on actual playback completion).
  - `EnhancedAnimeTracker` / `EnhancedMangaTracker` never prompt for search —
    they auto-bind when an anime from an accepted source is added to the
    library. Komga/Kavita/Suwayomi/Jellyfin all use this. Removing those
    trackers also removes the only `Enhanced*Tracker` implementations.

---

## Feature 2 — AniList

- **Description**: AniList (https://anilist.co) tracking — anime + manga,
  deletable, supports reading dates and private tracking. OAuth2 via custom
  scheme; supports 5 score types (`POINT_100`, `POINT_10`, `POINT_10_DECIMAL`,
  `POINT_5`, `POINT_3`) configured via `TrackPreferences.anilistScoreType()`.
  Status codes (anime): WATCHING=11, COMPLETED=2, ON_HOLD=3, DROPPED=4,
  PLAN_TO_WATCH=15, REWATCHING=16.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/data/track/anilist/Anilist.kt` (BaseTracker + MangaTracker + AnimeTracker + DeletableMangaTracker + DeletableAnimeTracker)
  - `app/.../data/track/anilist/AnilistApi.kt` (GraphQL API)
  - `app/.../data/track/anilist/AnilistInterceptor.kt` (Bearer-token auth; expires→logout)
  - `app/.../data/track/anilist/AnilistUtils.kt` (`toApiScore()` based on `anilistScoreType`)
  - DTOs: `app/.../data/track/anilist/dto/` (`ALOAuth`, `ALUser`, `ALSearch`, `ALSearchItem`, `ALAnime`, `ALManga`, `ALAddEntry`, `ALUserList`, `ALFuzzyDate`)
  - Logo: `R.drawable.ic_tracker_anilist`, color `Color.rgb(18, 25, 35)`
  - Stable id: `TrackerManager.ANILIST = 2L`
- **Status**: `keep`
- **Dependencies**: Tracker Infrastructure (Feature 1); AniList is also the
  primary source of MAL-ids for the AniSkip skip-intro lookup
  (`AniSkipApi.getMalIdFromAL(track.remoteId)`).
- **Notes**: AniList OAuth returns the access token without milliseconds;
  the interceptor pre-expires it by 60 seconds (`expires * 1000 - 60 * 1000`).
  There is **no refresh-token flow** for AniList — when the token expires the
  user must re-login.

---

## Feature 3 — MyAnimeList (MAL)

- **Description**: MyAnimeList (https://myanimelist.com) tracking — anime +
  manga, deletable, supports reading dates. OAuth2 via custom scheme with
  **refresh-token** flow. Score is 0–10 integer. Anime status codes:
  WATCHING=11, COMPLETED=2, ON_HOLD=3, DROPPED=4, PLAN_TO_WATCH=16, REWATCHING=17.
  Search supports two prefixes: `id:<malId>` and `my:<query>` (searches the
  user's own list).
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/data/track/myanimelist/MyAnimeList.kt` (BaseTracker + MangaTracker + AnimeTracker + DeletableMangaTracker + DeletableAnimeTracker)
  - `app/.../data/track/myanimelist/MyAnimeListApi.kt` (REST API; `refreshTokenRequest(oauth)`)
  - `app/.../data/track/myanimelist/MyAnimeListInterceptor.kt` (Bearer auth + synchronized `refreshToken(chain)`; throws `MALTokenExpired` / `MALTokenRefreshFailed`)
  - `app/.../data/track/myanimelist/MyAnimeListUtils.kt`
  - DTOs: `app/.../data/track/myanimelist/dto/` (`MALOAuth`, `MALUser`, `MALSearch`, `MALAnime`, `MALManga`, `MALList`, `MALUserListSearch`)
  - Logo: `R.drawable.ic_tracker_mal`, color `Color.rgb(46, 81, 162)`
  - Stable id: `1L`
- **Status**: `keep`
- **Dependencies**: Tracker Infrastructure (Feature 1). MAL is the **preferred
  source of MAL-ids** for the AniSkip skip-intro lookup
  (`track.remoteId` used directly as the MAL id).
- **Notes**: MAL's API requires a registered client id/secret; the
  `User-Agent` header line is commented out (MAL rejects Aniyomi-style UAs).
  `setAuthExpired()` flips `trackAuthExpired` to true; the user sees a
  "Login has expired" prompt and must re-authenticate.

---

## Feature 4 — Shikimori

- **Description**: Shikimori (https://shikimori.one) tracking — anime + manga,
  deletable, no reading-date support. OAuth2 via custom scheme with
  refresh-token flow. Score 0–10. Status codes (anime): READING=1, COMPLETED=2,
  ON_HOLD=3, DROPPED=4, PLAN_TO_READ=5, REREADING=6 (same set shared between
  anime/manga in the codebase; `getStatusListAnime()` exists but constants
  are manga-named).
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/data/track/shikimori/Shikimori.kt` (BaseTracker + MangaTracker + AnimeTracker + DeletableMangaTracker + DeletableAnimeTracker)
  - `app/.../data/track/shikimori/ShikimoriApi.kt`
  - `app/.../data/track/shikimori/ShikimoriInterceptor.kt`
  - `app/.../data/track/shikimori/ShikimoriUtils.kt`
  - DTOs: `app/.../data/track/shikimori/dto/` (`SMOAuth`, `SMUser`, `SMEntry`, `SMUserListEntry`, `SMAddEntryResponse`)
  - Logo: `R.drawable.ic_tracker_shikimori`
  - Stable id: `4L`
- **Status**: `keep`
- **Dependencies**: Tracker Infrastructure (Feature 1).
- **Notes**: Shikimori shares its MAL-id namespace — Shikimori entries often
  have the same remote id as the MAL entry. Could be a secondary source of
  MAL-ids for AniSkip if desired (not currently used that way).

---

## Feature 5 — Bangumi

- **Description**: Bangumi (https://bgm.tv) tracking — anime + manga,
  non-deletable (no `Deletable*Tracker` interface), supports private tracking.
  OAuth2 via custom scheme with refresh-token flow. Score 0–10 integer.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/data/track/bangumi/Bangumi.kt` (BaseTracker + MangaTracker + AnimeTracker)
  - `app/.../data/track/bangumi/BangumiApi.kt`
  - `app/.../data/track/bangumi/BangumiInterceptor.kt`
  - `app/.../data/track/bangumi/BangumiUtils.kt`
  - DTOs: `app/.../data/track/bangumi/dto/` (`BGMOAuth`, `BGMUser`, `BGMSearch`, `BGMCollectionResponse`)
  - Logo: `R.drawable.ic_tracker_bangumi`
  - Stable id: `5L`
- **Status**: `keep`
- **Dependencies**: Tracker Infrastructure (Feature 1).
- **Notes**: The only kept tracker that does **not** implement
  `DeletableAnimeTracker` — the user cannot remove a tracked entry from the
  Bangumi list from inside Kuta, only update it. `SettingsTrackingScreen`
  conditionally hides the "delete" button based on this interface check.

---

## Feature 6 — Kitsu

- **Description**: Kitsu (https://kitsu.io) tracking — anime + manga,
  deletable, supports reading dates and private tracking. OAuth2 with
  refresh-token flow. Anime status codes: WATCHING=11, COMPLETED=2, ON_HOLD=3,
  DROPPED=4, PLAN_TO_WATCH=15.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/data/track/kitsu/Kitsu.kt` + `KitsuApi.kt`, `KitsuInterceptor.kt`, `KitsuUtils.kt`, `KitsuDateHelper.kt`
  - DTOs: `app/.../data/track/kitsu/dto/` (`KitsuOAuth`, `KitsuUser`, `KitsuSearch`, `KitsuSearchItemCover`, `KitsuListSearch`, `KitsuAddEntry`)
  - Logo: `R.drawable.ic_tracker_kitsu`
  - Stable id: `TrackerManager.KITSU = 3L`
- **Status**: `remove`
- **Dependencies**: Tracker Infrastructure (Feature 1).
- **Notes**: Removing Kitsu = delete the entire `data/track/kitsu/` package
  (~12 files), remove the `kitsu` field from `TrackerManager`, drop its entry
  from the `trackers` list, and remove its UI entry from
  `SettingsTrackingScreen`. The `anime_sync` rows with `sync_id=3` will become
  orphans; a migration to delete them is recommended.

---

## Feature 7 — Simkl

- **Description**: Simkl (https://simkl.com) tracking — anime only (no manga),
  non-deletable, no reading dates. OAuth2 with refresh-token flow. Anime
  status codes: WATCHING=1, COMPLETED=2, ON_HOLD=3, NOT_INTERESTING=4,
  PLAN_TO_WATCH=5. Score 0–10.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/data/track/simkl/Simkl.kt` + `SimklApi.kt`, `SimklInterceptor.kt`, `SimklUtils.kt`
  - DTOs: `app/.../data/track/simkl/dto/` (`SimklOAuth`, `SimklUser`, `SimklSearch`, `SimklSyncItem`, `SimklSyncWatched`)
  - Logo: `R.drawable.ic_tracker_simkl`
  - Stable id: `TrackerManager.SIMKL = 101L`
- **Status**: `remove`
- **Dependencies**: Tracker Infrastructure (Feature 1).
- **Notes**: Implements only `AnimeTracker` (not `MangaTracker`), so removing
  it has no manga-side coupling. Drop the `data/track/simkl/` package
  (~10 files), remove `simkl` from `TrackerManager.trackers`, delete the
  `SIMKL` constant.

---

## Feature 8 — Komga

- **Description**: Komga (https://github.com/gotson/komga) tracking — manga
  only, self-hosted comic server. `EnhancedMangaTracker` (auto-binds when a
  manga from the Komga source is added to library; never prompts the user to
  search). No OAuth — uses API key auth via `KomgaInterceptor`. Status codes:
  UNREAD=1, READING=2, COMPLETED=3.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/data/track/komga/Komga.kt` + `KomgaApi.kt`, `KomgaModels.kt`
  - Uses `Dns.SYSTEM` (bypasses DoH to allow IP-based local-server access)
  - Logo: `R.drawable.ic_tracker_komga`
  - Stable id: `6L`
- **Status**: `remove` (manga-only, and an Enhanced tracker tied to a manga source)
- **Dependencies**: Tracker Infrastructure (Feature 1); `MangaSourceManager`
  (for `accept(source)`).
- **Notes**: One of three `EnhancedMangaTracker` implementations (Komga,
  Kavita, Suwayomi). Removing all three lets the `EnhancedMangaTracker`
  interface itself be deleted. The `getAcceptedSources()` list points at
  hard-coded manga-source class names — these become dead references once
  removed.

---

## Feature 9 — Kavita

- **Description**: Kavita (https://github.com/Kareadita/Kavita) tracking —
  manga only, self-hosted comic server. `EnhancedMangaTracker` (auto-bind).
  Per-source API-key authentication via `KavitaInterceptor`. Status codes:
  UNREAD=1, READING=2, COMPLETED=3.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/data/track/kavita/Kavita.kt` + `KavitaApi.kt`, `KavitaInterceptor.kt`, `KavitaModels.kt`
  - Depends on `MangaSourceManager` (for `ConfigurableSource` prefs lookup)
  - Logo: `R.drawable.ic_tracker_kavita`
  - Stable id: `TrackerManager.KAVITA = 8L`
- **Status**: `remove` (manga-only Enhanced tracker)
- **Dependencies**: Tracker Infrastructure (Feature 1); `MangaSourceManager`.
- **Notes**: See Komga notes — same removal pattern. `authentications: OAuth?`
  field on the tracker instance is a non-obvious bit of mutable state shared
  between the interceptor and the tracker; whole file goes when removed.

---

## Feature 10 — Suwayomi

- **Description**: Suwayomi (https://github.com/Suwayomi/Suwayomi-Server)
  tracking — manga only, self-hosted manga server. `EnhancedMangaTracker`
  (auto-bind). Status codes: UNREAD=1, READING=2, COMPLETED=3.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/data/track/suwayomi/Suwayomi.kt` + `SuwayomiApi.kt`, `SuwayomiModels.kt`
  - Logo: `R.drawable.ic_tracker_suwayomi`
  - Stable id: `9L`
- **Status**: `remove` (manga-only Enhanced tracker; **not listed in the
  user's keep/remove set** but matches the manga-removal pattern)
- **Dependencies**: Tracker Infrastructure (Feature 1); `MangaSourceManager`.
- **Notes**: The user's tracker list omitted Suwayomi — discovered during
  inventory. It is a manga-only Enhanced tracker and should be removed with
  Komga/Kavita. Confirm with the user before deletion (in case they want to
  keep it for a future anime variant). Logo color has a `// TODO` comment.

---

## Feature 11 — Jellyfin

- **Description**: Jellyfin (https://jellyfin.org) tracking — anime only,
  self-hosted media server. `EnhancedAnimeTracker` (auto-binds when an anime
  from a Jellyfin source is added to library; never prompts search). No score
  list. Status codes: UNSEEN=1, WATCHING=2, COMPLETED=3.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/data/track/jellyfin/Jellyfin.kt` + `JellyfinApi.kt`, `JellyfinInterceptor.kt`
  - DTOs: `app/.../data/track/jellyfin/dto/JFItem.kt`
  - Uses `Dns.SYSTEM` (bypasses DoH for IP-based local access)
  - Logo: `R.drawable.ic_tracker_jellyfin`
  - Stable id: `TrackerManager.JELLYFIN = 102L`
- **Status**: `remove` (anime-only Enhanced tracker — the user explicitly
  listed Jellyfin for removal even though it's anime-side)
- **Dependencies**: Tracker Infrastructure (Feature 1); `AnimeSourceManager`.
- **Notes**: Removing Jellyfin = drop `data/track/jellyfin/` (~4 files), drop
  `jellyfin` from `TrackerManager.trackers`, delete the `JELLYFIN` constant.
  This is the **only anime-only tracker being removed** — Komga/Kavita/Suwayomi
  are all manga-only. `getRewatchingStatus()` returns `-1` (Jellyfin has no
  rewatch concept).

---

## Feature 12 — MangaUpdates

- **Description**: MangaUpdates (https://www.mangaupdates.com) tracking —
  manga only, deletable. Username/password login (no OAuth). Score 0.0–10.0
  in 0.1 increments. Status codes: READING_LIST=0, WISH_LIST=1,
  COMPLETE_LIST=2, UNFINISHED_LIST=3, ON_HOLD_LIST=4 (uses MangaUpdates'
  "lists" concept instead of statuses).
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/data/track/mangaupdates/MangaUpdates.kt` + `MangaUpdatesApi.kt`, `MangaUpdatesInterceptor.kt`
  - DTOs: `app/.../data/track/mangaupdates/dto/` (`MUContext`, `MULoginResponse`, `MUSearch`, `MUSeries`, `MUListItem`, `MUStatus`, `MURating`, `MURecord`, `MUImage`, `MUUrl`)
  - Logo: `R.drawable.ic_manga_updates`
  - Stable id: `7L`
- **Status**: `remove` (manga-only)
- **Dependencies**: Tracker Infrastructure (Feature 1).
- **Notes**: Largest DTO surface of any tracker (~9 dto files). Removing
  MangaUpdates = drop the `data/track/mangaupdates/` package (~12 files),
  drop `mangaUpdates` from `TrackerManager.trackers`. No anime-side coupling
  whatsoever — purely manga.

---

## Feature 13 — Track UI & Settings

- **Description**: The user-facing track UI: per-anime "Tracking" info dialog
  (shows all linked trackers with status/score/progress, "Add tracking" button
  that opens the per-tracker search dialog), the tracker search list (top-N
  remote matches), and the global tracker settings screen under
  Settings → Tracking (login/logout buttons, "auto-update on progress" toggle,
  "auto-update on mark read" enum, "track on adding to library" toggle,
  "show next episode airing time" toggle, AniList score-type picker).
- **Location**:
  - Per-anime dialog: `app/.../ui/entries/anime/track/AnimeTrackInfoDialog.kt` (875-line Voyager `Screen` + ScreenModel), `AnimeTrackItem.kt`; manga mirror at `app/.../ui/entries/manga/track/`
  - Search UI: `app/.../presentation/track/anime/AnimeTrackerSearch.kt`, `AnimeTrackInfoDialogHome.kt` (+ preview providers); manga mirror in `presentation/track/manga/`
  - Shared selectors: `app/.../presentation/track/TrackInfoDialogSelector.kt`, plus `TrackItemSelector`, `TrackStatusSelector`, `TrackScoreSelector`, `TrackDateSelector` (in `presentation/track/`)
  - Logo/icon component: `app/.../presentation/track/components/TrackLogoIcon.kt`, `TrackLogoIconPreviewProvider.kt`
  - Settings screen: `app/.../presentation/more/settings/screen/SettingsTrackingScreen.kt` (417 lines — lists every tracker from `TrackerManager.trackers`, conditionally shows login UI for OAuth vs username/password, renders `Enhanced*Tracker` info, opens browser for OAuth via `openInBrowser`, returns to settings via `BaseOAuthLoginActivity`)
  - OAuth deep-link receivers: per-tracker `*LoginActivity` classes that extend `BaseOAuthLoginActivity` (registered in `AndroidManifest.xml` with custom intent-filters)
- **Status**: `modify` — UI will be re-skinned; the `SettingsTrackingScreen`
  must be pruned to only show the 4 kept trackers (this happens automatically
  if `TrackerManager.trackers` is trimmed, since the screen iterates it).
- **Dependencies**:
  - Depends on: `TrackerManager` (the source of truth for which trackers
    exist); `TrackPreferences`; `AnimeTrackSearch` model;
    `AddAnimeTracks` / `TrackEpisode` / `RefreshAnimeTracks` / `SyncEpisodeProgressWithTrack`
    interactors; `BaseOAuthLoginActivity` (OAuth callback); per-anime
    `AnimeScreenModel` (which owns the dialog state).
  - Depended on by: `AnimeScreen` (renders the tracking section in the info
    sheet); `SettingsScreen` (parent of `SettingsTrackingScreen`).
- **Notes**:
  - The settings screen has a hard-coded list of which trackers use OAuth
    (AniList, MAL, Shikimori, Bangumi, Kitsu, Simkl) vs username/password
    (MangaUpdates, Komga, Kavita). Removing non-kept trackers will simplify
    this conditional significantly.
  - `AnimeTrackerSearch` performs the search on the IO dispatcher and shows
    results in a paginated list; tapping a result calls
    `AnimeTracker.register(item, animeId)` → `AddAnimeTracks.bind(...)`.
  - After the 7 dropped trackers are removed, the manga-side track UI
    (`MangaTrackInfoDialog`, `MangaTrackerSearch`, `MangaTrackInfoDialogHome`)
    can also be deleted — the manga Library tab is already gated out
    (Phase 1, Task 4), so these UIs are unreachable.
