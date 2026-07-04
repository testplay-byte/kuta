# 06 — Database Schema (Both SQLDelight DBs)

Kuta ships **two independent SQLite databases** in the `:data` module:

- **`tachiyomi.db`** (the manga DB, package `tachiyomi.data`) — 9 tables +
  3 views, schema version 32 (migrations `1.sqm` … `32.sqm`).
- **`tachiyomi.animedb`** (the anime DB, package `tachiyomi.mi.data`) — 9
  tables + 8 views, schema version 135 (migrations `113.sqm` … `135.sqm`).

The anime DB's migration sequence starts at 113 (not 1), strongly suggesting
the anime schema was forked from the manga schema at the point where manga's
schema was at version ~112; the two have diverged since. Both DBs enforce
`foreign_keys = ON`, `journal_mode = WAL`, `synchronous = NORMAL` on every
`onOpen`. Both use SQL triggers to maintain `version` and `last_modified_at`
columns on the core entity tables (gated by an `is_syncing` flag for sync-
driven writes). `Category` is the only domain model shared by both sides
(there are two `categories` tables, one per DB, both with the same shape and
a synthetic `_id = 0` "Default" row protected by a
`system_category_delete_trigger`).

The two ER diagrams below are intentionally separate — there are **no
cross-database foreign keys** (SQLite cannot enforce them), so the two
graphs are completely disjoint.

## 6.1 — Anime DB (`tachiyomi.animedb`)

```mermaid
erDiagram
    animes ||--o{ episodes : "1 anime → N episodes"
    animes ||--o{ animehistory : "via episodes"
    episodes ||--o{ animehistory : "1 episode → N watch events"
    animes ||--o{ animes_categories : ""
    categories ||--o{ animes_categories : ""
    animes ||--o{ anime_sync : "1 anime → N tracker rows"
    animes ||--o{ animeseasons : "parent → child season rows (self-ref via parent_id)"
    animes }o--|| animesources : "cached source metadata"

    animes {
        INTEGER _id PK
        INTEGER source FK "extension/source id"
        TEXT    url
        TEXT    title
        TEXT    artist
        TEXT    author
        TEXT    description
        TEXT    genre "comma-joined (StringListColumnAdapter)"
        INTEGER status "0=UNKNOWN 1=ONGOING 2=COMPLETED …"
        TEXT    thumbnail_url
        TEXT    background_url "anime-only"
        INTEGER favorite "0/1"
        INTEGER fetch_type "anime-only (Episodes vs Seasons)"
        INTEGER parent_id "anime-only (parent anime for season rows)"
        REAL    season_number "anime-only"
        INTEGER season_flags "anime-only bitmask"
        INTEGER episode_flags "per-anime filter/sort/display"
        INTEGER viewer_flags "skip-intro length, next-airing"
        INTEGER update_strategy "AnimeUpdateStrategy enum"
        INTEGER initialized "0/1"
        INTEGER date_added
        INTEGER last_update
        INTEGER next_update
        INTEGER fetch_interval
        INTEGER cover_last_modified
        INTEGER background_last_modified "anime-only"
        INTEGER last_modified_at "bumped by SQL trigger"
        INTEGER favorite_modified_at
        INTEGER version "bumped by SQL trigger (gated by is_syncing)"
    }

    episodes {
        INTEGER _id PK
        INTEGER anime_id FK "ON DELETE CASCADE"
        TEXT    url
        TEXT    name
        REAL    episode_number
        INTEGER date_upload
        INTEGER date_fetch
        INTEGER source_order
        TEXT    scanlator "sub-group / release name"
        TEXT    summary "anime-only (episode synopsis)"
        TEXT    preview_url "anime-only"
        INTEGER seen "0/1"
        INTEGER bookmark "0/1"
        INTEGER fillermark "anime-only"
        INTEGER last_second_seen "resume position (ms)"
        INTEGER total_seconds "episode length"
        INTEGER last_modified_at
        INTEGER version
    }

    categories {
        INTEGER _id PK "0 = synthetic Default (protected)"
        TEXT    name
        INTEGER sort "order"
        INTEGER flags "per-category display/sort"
    }

    animes_categories {
        INTEGER _id PK
        INTEGER anime_id FK "ON DELETE CASCADE"
        INTEGER category_id FK "ON DELETE CASCADE"
    }

    animehistory {
        INTEGER _id PK
        INTEGER episode_id FK "ON DELETE CASCADE"
        INTEGER last_seen "Date (DateColumnAdapter)"
    }

    anime_sync {
        INTEGER _id PK
        INTEGER anime_id FK "ON DELETE CASCADE"
        INTEGER sync_id "tracker id (1=MAL 2=AniList 4=Shiki 5=Bangumi …)"
        INTEGER remote_id "tracker-side media id"
        TEXT    title
        REAL    last_episode_seen
        INTEGER total_episodes
        INTEGER status "tracker status enum"
        REAL    score
        TEXT    remote_url
        INTEGER start_date
        INTEGER finish_date
        INTEGER private "0/1"
    }
    %% UNIQUE(anime_id, sync_id)

    animesources {
        INTEGER _id PK
        INTEGER lang
        TEXT    name
    }

    animeseasons {
        INTEGER _id PK "view row"
        INTEGER parent_id "FK to animes._id"
        INTEGER child_id "FK to animes._id"
    }

    extension_repos {
        INTEGER _id PK
        TEXT    baseUrl
        TEXT    name
        TEXT    shortName
        TEXT    website
        TEXT    signingKeyFingerprint
    }

    custom_buttons {
        INTEGER _id PK "1 = seeded '+85 s' skip button"
        TEXT    name
        TEXT    icon
        TEXT    lua "Lua source for MPV user-data/aniyomi events"
        INTEGER visible
        INTEGER order
        INTEGER backgroundColor
        INTEGER textColor
    }
```

## 6.2 — Manga DB (`tachiyomi.db`)

> The manga DB is **still fully present** even though the manga UI has been
> gated out. Its tables, migrations, mappers, repositories, and domain
> models are intact and wired in `AppModule.kt`. Removing the UI did not
> touch this schema.

```mermaid
erDiagram
    mangas ||--o{ chapters : "1 manga → N chapters"
    mangas ||--o{ history : "via chapters"
    chapters ||--o{ history : "1 chapter → N read events"
    mangas ||--o{ mangas_categories : ""
    categories ||--o{ mangas_categories : ""
    mangas ||--o{ manga_sync : "1 manga → N tracker rows"
    mangas ||--o{ excluded_scanlators : "per-manga blocklist"
    mangas }o--|| sources : "cached source metadata"

    mangas {
        INTEGER _id PK
        INTEGER source FK
        TEXT    url
        TEXT    title
        TEXT    artist
        TEXT    author
        TEXT    description
        TEXT    genre "comma-joined"
        INTEGER status
        TEXT    thumbnail_url
        INTEGER favorite
        INTEGER update_strategy "UpdateStrategy enum"
        INTEGER initialized
        INTEGER date_added
        INTEGER last_update
        INTEGER next_update
        INTEGER fetch_interval
        INTEGER cover_last_modified
        INTEGER chapter_flags
        INTEGER viewer_flags
        INTEGER last_modified_at
        INTEGER favorite_modified_at
        INTEGER version
    }

    chapters {
        INTEGER _id PK
        INTEGER manga_id FK "ON DELETE CASCADE"
        TEXT    url
        TEXT    name
        REAL    chapter_number
        INTEGER date_upload
        INTEGER date_fetch
        INTEGER source_order
        TEXT    scanlator
        INTEGER read "0/1"
        INTEGER bookmark "0/1"
        INTEGER last_page_read "page index"
        INTEGER last_modified_at
        INTEGER version
    }

    categories {
        INTEGER _id PK "0 = synthetic Default (protected)"
        TEXT    name
        INTEGER sort
        INTEGER flags
    }

    mangas_categories {
        INTEGER _id PK
        INTEGER manga_id FK "ON DELETE CASCADE"
        INTEGER category_id FK "ON DELETE CASCADE"
    }

    history {
        INTEGER _id PK
        INTEGER chapter_id FK "ON DELETE CASCADE"
        INTEGER last_read "Date"
        INTEGER time_read "total reading time (manga-only)"
    }

    manga_sync {
        INTEGER _id PK
        INTEGER manga_id FK "ON DELETE CASCADE"
        INTEGER sync_id "tracker id"
        INTEGER remote_id
        TEXT    title
        REAL    last_chapter_read
        INTEGER total_chapters
        INTEGER status
        REAL    score
        TEXT    remote_url
        INTEGER start_date
        INTEGER finish_date
        INTEGER private "0/1 — added by migration 32.sqm"
    }
    %% UNIQUE(manga_id, sync_id)

    sources {
        INTEGER _id PK
        INTEGER lang
        TEXT    name
    }

    extension_repos {
        INTEGER _id PK
        TEXT    baseUrl
        TEXT    name
        TEXT    shortName
        TEXT    website
        TEXT    signingKeyFingerprint
    }

    excluded_scanlators {
        INTEGER _id PK
        INTEGER manga_id FK "ON DELETE CASCADE"
        TEXT    scanlator
    }
```

## Notes

- **Two databases, not one.** Most Tachiyomi/Mihon forks keep a single DB.
  Kuta splits anime and manga into separate SQLite files with their own
  generated `Database` / `AnimeDatabase` interfaces, their own handlers
  (`AndroidMangaDatabaseHandler` / `AndroidAnimeDatabaseHandler`), their own
  mappers, and their own migration sequences. This doubles the data-layer
  surface and is the reason every repository is duplicated under
  `.../anime/` and `.../manga/` packages.
- **`animehistory` has no `time_read` column** (manga `history` does). Anime
  watch duration is reconstructed from `episodes.last_second_seen` /
  `total_seconds` at view time (via `animehistoryView` and
  `animehistorystatsView`). This asymmetry is mirrored in the domain models
  (`AnimeHistory` vs `MangaHistory` — the latter has `readDuration`).
- **Anime-only columns on `animes`**: `fetch_type` (Episodes vs Seasons),
  `parent_id` (parent anime for season rows), `season_flags`,
  `season_number`, `season_source_order`, `background_url`,
  `background_last_modified`. These support season grouping and background
  art — there is no manga equivalent.
- **Anime-only columns on `episodes`**: `last_second_seen` (resume position),
  `total_seconds` (episode length), `summary` (episode synopsis),
  `preview_url` (episode thumbnail), `fillermark`. Manga `chapters` has
  `last_page_read` instead (page index) and no filler / preview / summary.
- **`custom_buttons`** is anime-only (seeds a "+85 s" skip-intro button at
  `_id=1` via migration 129). The buttons back the MPV Lua bridge — Lua
  source is stored in the `lua` column and pushed to MPV's
  `user-data/aniyomi` JSON events at player start.
- **`extension_repos` is duplicated** in both DBs (one for anime extensions,
  one for manga extensions). Both have identical schemas.
- **Schema version divergence**: manga DB is at v32, anime DB is at v135.
  The anime DB started its migration sequence at `113.sqm` (not `1.sqm`),
  strongly suggesting the anime DB was forked from the manga DB at the point
  where manga's schema was at version ~112; the two have diverged since.
  Migrations are plain SQL `.sqm` files — no Kotlin `Migration` registry
  (no Room-style migration graph).
- **SQL views** (not shown in the ER diagrams): manga has 3 (`libraryView`,
  `historyView`, `updatesView`); anime has 8 (`animelibView`,
  `animehistoryView`, `animeupdatesView`, `animedeletableView`,
  `animeseasonsView`, `animeseasonstatsView`, `animehistorystatsView`,
  `episodestatsView`). The views feed the Library / History / Updates tabs
  and the per-anime aggregate counts shown as `LibraryAnime` /
  `LibraryManga`.
- **`version` and `last_modified_at` are trigger-maintained.** Every
  `UPDATE` on `animes` / `mangas` / `episodes` / `chapters` fires a SQL
  trigger that bumps `last_modified_at`; further triggers bump `version`
  (unless `is_syncing = 1`). The domain models surface these as plain
  `Long` fields; they power the sync feature and should never be written
  manually.
- **No cross-database FKs.** SQLite cannot enforce them, so the two ER
  diagrams above are completely disjoint. `Anime ↔ Manga` parallelism is
  exhaustive at the schema level but the two sides never reference each
  other.
