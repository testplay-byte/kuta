# 03 — Feature Relationships

This diagram maps the major user-facing features of Kuta to one another by
*runtime dependency* — i.e. "Feature A depends on Feature B" means A calls
into B's code or cannot function without B. The five subgraphs group features
by responsibility: **Content Consumption** (the playback path),
**Discovery** (browsing/searching sources), **Organization** (the user's
library), **Integration** (sync with the outside world), and **System**
(onboarding, settings). Edge labels spell out the specific call or contract
that creates the dependency. Several non-obvious transitivities jump out
when laid out this way: AniSkip → Trackers (because skip-intro needs the MAL
id from a linked tracker), Downloads → Player (because the downloader uses
the same `Video` model and FFmpegKit), and Backup → *everything* (because
backup serialises Library + History + Categories + Trackers).

```mermaid
graph TD
    classDef consume fill:#ffe6e6,stroke:#cc3333,color:#000
    classDef discover fill:#e8f0ff,stroke:#3b6fb5,color:#000
    classDef organize fill:#eafaf0,stroke:#2e8b57,color:#000
    classDef integrate fill:#f3eaff,stroke:#7a3fb5,color:#000
    classDef system fill:#fff7e6,stroke:#aa8800,color:#000

    %% ---------- Content Consumption ----------
    subgraph CONSUME["Content Consumption"]
        Player["Player<br/>(MPV + Compose overlay)"]
        History["History<br/>(watch log + resume)"]
        Downloads["Downloads<br/>(FFmpegKit mux)"]
        AniSkip["AniSkip<br/>(skip intro/outro)"]
    end
    class Player,History,Downloads,AniSkip consume

    %% ---------- Discovery ----------
    subgraph DISCOVER["Discovery"]
        Browse["Browse Tab<br/>(sources list)"]
        Search["Global / Source Search"]
        Extensions["Extensions<br/>(installed APKs)"]
        Migration["Migration<br/>(anime → source)"]
    end
    class Browse,Search,Extensions,Migration discover

    %% ---------- Organization ----------
    subgraph ORGANIZE["Organization"]
        Library["Library<br/>(favorited anime)"]
        Categories["Categories"]
        Updates["Updates Feed"]
    end
    class Library,Categories,Updates organize

    %% ---------- Integration ----------
    subgraph INTEGRATE["Integration"]
        Trackers["Trackers<br/>(AniList / MAL / Shiki / Bangumi)"]
        Backup["Backup / Restore"]
    end
    class Trackers,Backup integrate

    %% ---------- System ----------
    subgraph SYS["System"]
        Settings["Settings"]
        Onboarding["Onboarding"]
    end
    class Settings,Onboarding system

    %% ---------- Edges ----------
    Player -- "getHosterList / getVideoList / resolveVideo" --> Extensions
    Player -- "Episode.last_second_seen<br/>(resume position)" --> History
    Player -- "EpisodeLoader.isDownloaded branch" --> Downloads
    Player -- "skip-times/{malId}/{epNum}" --> AniSkip

    AniSkip -- "needs MAL id<br/>(MAL direct OR AniList → getMalIdFromAL)" --> Trackers

    Downloads -- "fetches Video via source" --> Extensions
    Downloads -- "buildVideo() returns local Video<br/>for Player" --> Player

    History -- "records watch events" --> Player
    History -- "resume from library<br/>uses GetNextEpisodes" --> Library

    Browse -- "lists installed sources" --> Extensions
    Browse -- "tap source → BrowseAnimeSourceScreen" --> Search
    Migration -- "target source search" --> Browse

    Library -- "add anime flow" --> Browse
    Library -- "organize via categories" --> Categories
    Updates -- "derived from Library favorites<br/>(AnimeLibraryUpdateJob)" --> Library

    Trackers -- "only useful if anime is in Library<br/>(TrackService binds from AnimeScreen)" --> Library
    Backup -- "serialises animes + episodes +<br/>categories + animehistory + anime_sync" --> Library
    Backup -- "includes animehistory rows" --> History
    Backup -- "includes animes_categories" --> Categories
    Backup -- "includes anime_sync rows" --> Trackers

    Settings -- "configures all of the above<br/>via *Preferences classes" --> Player
    Settings -- "configures" --> Downloads
    Settings -- "configures" --> Trackers
    Settings -- "configures" --> Library

    Onboarding -- "restore-from-backup entry<br/>on first run" --> Backup
    Onboarding -- "Theme / Storage / Permission steps<br/>seed Settings + Downloads" --> Settings
```

## Notes

- **AniSkip → Trackers transitivity** is the most non-obvious edge in the
  graph: AniSkip queries `api.aniskip.com/v2/skip-times/{malId}/{epNum}` and
  gets the `malId` from a linked MAL tracker (directly) or from AniList via
  `getMalIdFromAL`. If no tracker is logged in, skip-intro silently does
  nothing — there is no fallback. So "skip intro" is gated on the user
  having linked at least one tracker.
- **Backup is a leaf of every persistent feature.** The backup serialiser
  walks `animes`, `episodes`, `animes_categories`, `categories`,
  `animehistory`, `anime_sync`, `extension_repos`, and `custom_buttons`.
  Removing any of those DB tables breaks backup — and the same is true in
  reverse for restore.
- **Migration depends on Browse** (not on Extensions directly) because
  migration re-uses `BrowseAnimeSourceScreen`'s search pipeline to find the
  target anime in the new source — it doesn't talk to the source itself.
- **Updates is downstream of Library, not upstream.** `AnimeLibraryUpdateJob`
  (a `CoroutineWorker` scheduled by `autoUpdateInterval()`) iterates over
  favorited anime and refreshes their episode lists; the Updates tab is just
  a feed view over the resulting `animeupdatesView` SQL view.
- **Settings isn't a hard dependency of the runtime features** in the
  compile-time sense — it's drawn here because every feature reads its
  `*Preferences` object (e.g. `PlayerPreferences`, `DownloadPreferences`,
  `LibraryPreferences`, `TrackPreferences`) and the Settings UI is the only
  way to mutate them. The `*Preferences` objects themselves are
  Injekt-injected singletons available app-wide.
- **Not shown** (to keep the graph readable): Onboarding → Settings is the
  only System-to-System edge; the Settings screens push into onboarding-like
  flows (e.g. tracker OAuth via `BaseOAuthLoginActivity`) but those are
  sub-flows of Settings, not separate top-level features.
