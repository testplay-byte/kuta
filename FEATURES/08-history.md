# 08 — Watch History, Continue Watching

> Feature inventory of the watch-history surface: the History tab, the
  resume/continue-watching action, what's stored and how, and history
  clearing. Research/documentation only — no source files modified. All
  paths relative to `/home/z/kuta`.

---

### History Tab (Recently Watched List)

- **Description**: The "History" bottom-nav tab. A swipeable anime/manga
  pager showing recently watched episodes grouped by date (today, yesterday,
  earlier…). Each row shows anime cover, title, episode number, and "time
  last seen" (relative or absolute per `UiPreferences`). Tap cover →
  `AnimeScreen`; tap "Resume" button → next-episode resolver → player.
  A search box filters the list by title. An overflow action "Clear
  history" wipes all rows (with confirmation). Long-press re-select on the
  tab resumes the *globally* last-watched episode.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/ui/history/HistoriesTab.kt`
    (Voyager `Tab`; swipeable anime/manga `TabbedScreen`; hoists both
    sub-tab screen models so the search bar survives sub-tab swaps;
    `onReselect` sends to `resumeLastEpisodeSeenEvent`)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/history/anime/AnimeHistoryTab.kt`
    (TabContent: wires `AnimeHistoryScreen`, delete / delete-all / duplicate
    / change-category / migrate dialogs, the `resumeLastEpisodeSeenEvent`
    `Channel<Unit>` listener, AppBar "Clear history" action)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/history/anime/AnimeHistoryScreenModel.kt`
    (`State` with `list: List<AnimeHistoryUiModel>?`; `subscribe(query)`
    → `getHistory.subscribe(query)` → `toAnimeHistoryUiModels()` which
    inserts date-header separators; `removeFromHistory`,
    `removeAllFromHistory(animeId)`, `removeAllHistory()`, `getNextEpisode`,
    `getNextEpisodeForAnime`, `addFavorite` with default-category
    resolution, `showMigrateDialog`)
  - `app/src/main/java/eu/kanade/presentation/history/anime/AnimeHistoryScreen.kt`
    (Compose UI)
  - DB view: `data/src/main/sqldelightanime/view/animehistoryView.sq`
    — joins `animes` ⋈ `episodes` ⋈ `animehistory` plus a max-last-seen
    sub-query to dedupe to "the most-recently-watched episode per anime".
    The `animehistory` query filters `seenAt > 0 AND maxSeenAtEpisodeId =
    episodeId AND title LIKE '%query%'`.
  - DB table: `data/src/main/sqldelightanime/dataanime/animehistory.sq`
    (`animehistory(_id, episode_id FK→episodes, last_seen DATE)`; FK CASCADE)
  - Repository/interactor:
    `data/.../history/anime/AnimeHistoryRepositoryImpl.kt` +
    `AnimeHistoryMapper.kt`;
    `domain/.../history/anime/interactor/GetAnimeHistory.kt`,
    `RemoveAnimeHistory.kt`, `UpsertAnimeHistory.kt`, `GetNextEpisodes.kt`
  - Domain models:
    `domain/.../history/anime/model/AnimeHistory.kt` (bare row),
    `AnimeHistoryWithRelations.kt` (enriched view row),
    `AnimeHistoryUpdate.kt` (patch DTO)
- **Status**: `modify`
  - The swipeable anime/manga pager should collapse to anime-only in the
    redesign; the underlying anime history machinery stays.
- **Dependencies**:
  - Depends on: `GetAnimeHistory.subscribe(query)`,
    `RemoveAnimeHistory`, `GetNextEpisodes`, `GetAnime`,
    `GetDuplicateLibraryAnime`, `GetAnimeCategories`, `SetAnimeCategories`,
    `UpdateAnime`, `AddAnimeTracks`, `AnimeSourceManager`,
    `LibraryPreferences.defaultAnimeCategory()`,
    `PlayerPreferences.alwaysUseExternalPlayer()` (resume launch),
    `MainActivity.startPlayerActivity(...)`.
  - Depended on by: `HomeScreen` (one of 6 tabs), `AnimeScreen` (no direct
    dep — but `AnimeHistoryScreenModel.addFavorite` is invoked from history
    when an unfavorited anime's row is tapped).
- **Notes**:
  - The view's `max_last_seen` sub-query is what gives "one row per anime"
    — without it every watched episode of the same anime would appear.
    `maxSeenAtEpisodeId` then pins which episode to show ("the most
    recently watched episode of that anime").
  - Search query is a SQL-side `LIKE '%query%'` against `lower(title)` —
    see `animehistoryView.sq`.
  - `AnimeHistoryUiModel` is a sealed type with `Item` and `Header` — the
    `insertSeparators` helper groups items by `seenAt.toLocalDate()`.
  - `HistoriesTab` hoists *both* `AnimeHistoryScreenModel` and
    `MangaHistoryScreenModel` even though only one is visible at a time —
    this is so the search-bar state survives a sub-tab swap.

---

### Resume / Continue Watching

- **Description**: Two distinct resume entry points: (1) per-anime "Resume"
  button on each history row → opens the *next* episode after the one last
  watched (or the same episode if not seen to end); (2) "Resume last
  watched" triggered by re-selecting the History tab (long-press / tap
  when already selected) → opens the next episode of the globally
  last-watched anime. The library tab also has an optional "Continue
  watching" button on each anime card (gated by
  `LibraryPreferences.showContinueViewingButton()`) that opens the next
  unseen episode of that anime.
- **Location**:
  - History row "Resume": `AnimeHistoryScreenModel.getNextEpisodeForAnime(
    animeId, episodeId)` → calls `GetNextEpisodes.await(animeId, episodeId,
    onlyUnseen = false)` → sends `Event.OpenEpisode(episode)` → tab's
    `openEpisode(context, episode)` → `MainActivity.startPlayerActivity(...)`
  - History tab re-select: `HistoriesTab.onReselect` →
    `resumeLastEpisodeSeenEvent.send(Unit)` → `AnimeHistoryTab` listens →
    `screenModel.getNextEpisode()` → `GetNextEpisodes.await(onlyUnseen =
    false)` which reads `historyRepository.getLastAnimeHistory()` then
    resolves the next episode.
  - Library "Continue watching" button: `AnimeLibraryTab.onContinueWatchingClicked`
    → `screenModel.getNextUnseenEpisode(anime)` (in `AnimeLibraryScreenModel`)
    → `openEpisode(episode)` → `MainActivity.startPlayerActivity(...)`
  - Interactor: `domain/.../history/anime/interactor/GetNextEpisodes.kt`
    — three overloads:
      - `await(onlyUnseen: Boolean = true)` — uses last history row
        globally.
      - `await(animeId, onlyUnseen)` — all episodes of one anime.
      - `await(animeId, fromEpisodeId, onlyUnseen)` — episodes *after*
        `fromEpisodeId`, with logic: if `fromEpisode` is not fully seen,
        include it (resume mid-episode); else drop it.
  - Last-history lookup: `AnimeHistoryRepository.getLastAnimeHistory()`
    → `getLatestAnimeHistory` SQL query (`ORDER BY seenAt DESC LIMIT 1`).
  - Player-side resume (where the actual playback position lives): see
    `DOCS/architecture/06-player.md` — `Episode.lastSecondSeen` and
    `Episode.totalSeconds` are read by `PlayerViewModel` to issue
    `set start=<seconds>` to MPV. `PlayerPreferences.preserveWatchingPosition()`
    gates whether the position is saved on exit.
- **Status**: `keep`
- **Dependencies**:
  - Depends on: `GetNextEpisodes`, `AnimeHistoryRepository.getLastAnimeHistory()`,
    `AnimeLibraryScreenModel.getNextUnseenEpisode` (for library variant),
    `MainActivity.startPlayerActivity(context, animeId, episodeId, extPlayer)`,
    `PlayerPreferences.alwaysUseExternalPlayer()`.
  - Depended on by: `HistoriesTab` (re-select), `AnimeHistoryTab` (row
    "Resume" button), `AnimeLibraryTab` (continue-watching button).
- **Notes**:
  - **Anime vs manga asymmetry** (per `DOCS/architecture/10-data-models.md`
    §6): `AnimeHistory` has no `time_read` / `readDuration` field; anime
    watch duration is reconstructed from `Episode.last_second_seen` /
    `total_seconds`. `MangaHistory` *does* have `readDuration` and the
    manga `history` table has a `time_read` column. This means anime
    history's "time spent" is per-episode only, not per-watch-session.
  - The "next episode" logic is subtle: if the user stopped mid-episode
    (not seen), `GetNextEpisodes` returns the *same* episode so they
    resume it; if they finished it (`seen = true`), it returns the next
    one. See inline comment in `GetNextEpisodes.kt`.
  - `resumeLastEpisodeSeenEvent` is a top-level `Channel<Unit>` declared
    in `AnimeHistoryTab.kt` (not on a singleton object) — it survives
    because `AnimeHistoryTab` is a `data object`/singleton composable-scoped.
  - `PlayerPreferences.preserveWatchingPosition()` (key
    `pref_preserve_watching_position`) gates whether playback position
    updates are persisted on exit. Default is on.

---

### History Tracking (What's Stored + Resume Position)

- **Description**: The data layer that backs the History tab and resume.
  Two tables/columns cooperate: (1) the `animehistory` table records
  "user watched episode X at time Y" (one row per watch event, deduped in
  the view to most-recent-per-anime); (2) the `episodes` table's
  `last_second_seen` and `total_seconds` columns record the playback
  position within an episode. The player's `PlayerViewModel` writes both
  on playback progress and on exit. There is no separate "watch session"
  or "watch duration per session" table.
- **Location**:
  - Table: `data/src/main/sqldelightanime/dataanime/animehistory.sq` —
    `animehistory(_id INTEGER PK, episode_id INTEGER FK→episodes(_id) ON
    DELETE CASCADE, last_seen DATE AS Date)`. `last_seen` uses
    `DateColumnAdapter` (Long ↔ `java.util.Date`).
  - Episode columns: `data/src/main/sqldelightanime/dataanime/episodes.sq`
    — `last_second_seen INTEGER` (resume offset in seconds), `total_seconds
    INTEGER` (episode duration in seconds). Both written by the player.
  - View: `data/src/main/sqldelightanime/view/animehistoryView.sq`
    (described above).
  - Mapper: `data/.../history/anime/AnimeHistoryMapper.kt` (maps the
    generated `dataanime.AnimehistoryView` row to the domain
    `AnimeHistoryWithRelations`).
  - Repository: `data/.../history/anime/AnimeHistoryRepositoryImpl.kt`
    — `getAnimeHistory(query)`, `getLastAnimeHistory()`,
    `getHistoryByAnimeId(animeId)`, `upsertAnimeHistory(update)`,
    `resetAnimeHistory(historyId)`, `resetHistoryByAnimeId(animeId)`,
    `deleteAllAnimeHistory()`.
  - Domain interactors:
    `domain/.../history/anime/interactor/{GetAnimeHistory,GetNextEpisodes,RemoveAnimeHistory,UpsertAnimeHistory}.kt`
  - Domain models:
    `domain/.../history/anime/model/{AnimeHistory,AnimeHistoryWithRelations,AnimeHistoryUpdate}.kt`
  - Writers (player side):
    `app/.../ui/player/PlayerViewModel.kt` writes `EpisodeUpdate(
    lastSecondSeen = ..., totalSeconds = ...)` periodically + on exit,
    and `UpsertAnimeHistory.await(AnimeHistoryUpdate(episodeId, seenAt =
    Date()))` on episode start/finish.
  - Tracker bridge: `AnimeHistory` rows trigger tracker updates via
    `AddAnimeTracks` / `TrackPreferences` auto-update — see
    `DOCS/architecture/10-data-models.md` §5.
- **Status**: `keep`
- **Dependencies**:
  - Depends on: anime DB (`animehistory`, `episodes` tables, `animehistoryView`
    view), the player pipeline for writes.
  - Depended on by: History tab, resume action, library continue-watching,
    tracker auto-update (when an episode is marked seen, the tracker is
    notified), backup/restore (the `animehistory` table is included in
    backups).
- **Notes**:
  - The `animehistory` table is essentially a log of "watch events"; the
    view dedupes to the most-recent-per-anime. Old watch events are *not*
    auto-pruned — they accumulate (this is how the "watched at" date is
    preserved for old episodes).
  - Resume *position* lives on `episodes.last_second_seen`, not on
    `animehistory`. This is why removing all history (`deleteAllAnimeHistory`)
    does *not* reset playback positions — those live on the episode rows.
  - The `seen` flag on `episodes` (set when playback crosses the
    `PlayerPreferences.progressPreference()` threshold, default 95%) is
    separate from `last_second_seen` — `seen` is a binary "watched" flag,
    `last_second_seen` is the precise offset.
  - There's an `animehistorystatsView` SQL view
    (`data/src/main/sqldelightanime/view/animehistorystatsView.sq`) that
    drives a "last-seen aggregate per anime" used by the Stats tab — not
    part of the History feature per se but worth knowing about.

---

### History Clearing / Management

- **Description**: Three granularities of history deletion, all surfaced
  from the History tab. (1) Single row: swipe or tap the row's delete icon
  → delete just that one watch event (the `animehistory` row), with a
  dialog offering "delete all of this anime's history" as an option. (2)
  Per-anime: delete every `animehistory` row for one anime (the dialog's
  "all" option). (3) All: the overflow "Clear history" action wipes every
  `animehistory` row. None of these reset `episodes.last_second_seen`
  (playback position) — that lives on the episode row and is only reset
  when the episode's `seen` flag is flipped off (via `SetSeenStatus`).
- **Location**:
  - Single / per-anime: `AnimeHistoryScreenModel.removeFromHistory(
    history: AnimeHistoryWithRelations)` → `RemoveAnimeHistory.await(history)`
    → `repository.resetAnimeHistory(history.id)`;
    `AnimeHistoryScreenModel.removeAllFromHistory(animeId)` →
    `RemoveAnimeHistory.await(animeId)` →
    `repository.resetHistoryByAnimeId(animeId)`.
  - All: `AnimeHistoryScreenModel.removeAllHistory()` →
    `RemoveAnimeHistory.awaitAll()` → `repository.deleteAllAnimeHistory()`
    → on success emits `Event.HistoryCleared` → snackbar.
  - UI: `app/src/main/java/eu/kanade/presentation/history/HistoryDeleteDialog.kt`
    (single-row dialog with "this episode" / "all of this anime" toggle,
    `isManga: Boolean` flag), `HistoryDeleteAllDialog.kt` (global
    confirmation).
  - Overflow action: `AnimeHistoryTab.kt` — `AppBar.Action(title =
    R.string.pref_clear_history, icon = DeleteSweep, onClick = {
    screenModel.setDialog(Dialog.DeleteAll) })`.
  - Repository methods: `AnimeHistoryRepositoryImpl.resetAnimeHistory(id)`,
    `resetHistoryByAnimeId(animeId)`, `deleteAllAnimeHistory()`.
- **Status**: `keep`
- **Dependencies**:
  - Depends on: `RemoveAnimeHistory` interactor, `AnimeHistoryRepository`.
  - Depended on by: `AnimeHistoryTab` (overflow action + row action).
- **Notes**:
  - **Subtle UX quirk**: deleting history does NOT reset the resume
    position (`episodes.last_second_seen`). So a user who deletes history
    and then taps "Resume" on the library's continue-watching button will
    still resume mid-episode. This is by design (history ≠ playback
    position) but can confuse users.
  - The "Clear history" overflow action uses icon `Icons.Outlined.DeleteSweep`
    and string `R.string.pref_clear_history` — the same string is used in
    Settings for the "Clear database" action, which is a different (more
    destructive) operation that *does* reset playback positions and
    non-favorite anime. The overlap is a known string-reuse wart.
  - The "all of this anime" delete option in the single-row dialog is the
    only way to bulk-delete one anime's history without nuking everything —
    there is no multi-select on the History tab (unlike the Updates tab).
  - `Event.HistoryCleared` is the only `Event` that surfaces a snackbar
    after a clear — per-row and per-anime deletes are silent.
