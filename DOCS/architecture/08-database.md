# 08 — Database Layer (SQLDelight)

> Scope: persistence layer for the Kuta / Aniyomi fork. This is **research
> documentation only** — no source files were modified.

## TL;DR

- **Library: SQLDelight `2.0.2`**, *not* Room. The user brief assumed Room; that
  assumption is incorrect.
- Aniyomi ships **two separate SQLite databases**, each its own generated
  `Database` interface:
  - `Database` (manga side) → file `tachiyomi.db`, package `tachiyomi.data`
  - `AnimeDatabase` (anime side) → file `tachiyomi.animedb`, package `tachiyomi.mi.data`
- Schema definitions live in `.sq` files under `data/src/main/sqldelight/`
  (manga) and `data/src/main/sqldelightanime/` (anime).
- Migrations are SQL files (`.sqm`) named by version number.
  - Manga: 32 migration files (`1.sqm` … `32.sqm`)
  - Anime: 23 migration files (`113.sqm` … `135.sqm`)
- Drivers / handlers / mappers all live in the `data/` Gradle module.

---

## 1. SQLDelight dependency (confirmation)

File: `gradle/libs.versions.toml`

```toml
sqldelight = "2.0.2"
...
sqldelight-android-driver = { module = "app.cash.sqldelight:android-driver", version.ref = "sqldelight" }
sqldelight-coroutines       = { module = "app.cash.sqldelight:coroutines-extensions-jvm", version.ref = "sqldelight" }
sqldelight-android-paging   = { module = "app.cash.sqldelight:androidx-paging3-extensions", version.ref = "sqldelight" }
sqldelight-dialects-sql     = { module = "app.cash.sqldelight:sqlite-3-38-dialect", version.ref = "sqldelight" }
...
sqldelight = ["sqldelight-android-driver", "sqldelight-coroutines", "sqldelight-android-paging"]
```

Group `app.cash.sqldelight`, version **2.0.2**, dialect SQLite 3.38. No Room
(`androidx.room`) dependency is present anywhere in the data layer.

The SQLDelight Gradle plugin is applied in `data/build.gradle.kts`, where **two**
databases are declared:

```kotlin
// data/build.gradle.kts
sqldelight {
    databases {
        create("Database") {            // manga side
            packageName.set("tachiyomi.data")
            dialect(libs.sqldelight.dialects.sql)
            schemaOutputDirectory.set(project.file("./src/main/sqldelight"))
            srcDirs.from(project.file("./src/main/sqldelight"))
        }
        create("AnimeDatabase") {       // anime side
            packageName.set("tachiyomi.mi.data")
            dialect(libs.sqldelight.dialects.sql)
            schemaOutputDirectory.set(project.file("./src/main/sqldelightanime"))
            srcDirs.from(project.file("./src/main/sqldelightanime"))
        }
    }
}
```

At build time SQLDelight generates:
- `tachiyomi.data.Database` (an interface) + per-table data classes & query
  extensions, in package `tachiyomi.data` / sub-packages `data.*`
  (e.g. `data.Mangas`, `data.History`, `data.Categories`).
- `tachiyomi.mi.data.AnimeDatabase` + per-table data classes in
  `tachiyomi.mi.data` / `dataanime.*` (e.g. `dataanime.Animes`,
  `dataanime.Animehistory`).

---

## 2. Where the database is constructed

Both drivers are created in the app DI graph:

**File:** `app/src/main/java/eu/kanade/tachiyomi/di/AppModule.kt`

```kotlin
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory
...
val sqlDriverManga = AndroidSqliteDriver(
    schema = Database.Schema,
    context = app,
    name = "tachiyomi.db",
    factory = if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        FrameworkSQLiteOpenHelperFactory()   // for Android Studio DB inspector
    } else {
        RequerySQLiteOpenHelperFactory()
    },
    callback = object : AndroidSqliteDriver.Callback(Database.Schema) {
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            setPragma(db, "foreign_keys = ON")
            setPragma(db, "journal_mode = WAL")
            setPragma(db, "synchronous = NORMAL")
        }
        ...
    },
)

val sqlDriverAnime = AndroidSqliteDriver(
    schema = AnimeDatabase.Schema,
    context = app,
    name = "tachiyomi.animedb",
    factory = /* same conditional as above */,
    callback = object : AndroidSqliteDriver.Callback(AnimeDatabase.Schema) { /* same pragmas */ },
)
```

Then the generated `Database` / `AnimeDatabase` instances are created with
custom `ColumnAdapter`s (typed column ↔ Kotlin mapping):

```kotlin
addSingletonFactory {
    Database(
        driver = sqlDriverManga,
        historyAdapter = History.Adapter(last_readAdapter = DateColumnAdapter),
        mangasAdapter = Mangas.Adapter(
            genreAdapter = StringListColumnAdapter,
            update_strategyAdapter = MangaUpdateStrategyColumnAdapter,
        ),
    )
}
addSingletonFactory {
    AnimeDatabase(
        driver = sqlDriverAnime,
        animehistoryAdapter = Animehistory.Adapter(last_seenAdapter = DateColumnAdapter),
        animesAdapter = Animes.Adapter(
            genreAdapter = StringListColumnAdapter,
            update_strategyAdapter = AnimeUpdateStrategyColumnAdapter,
            fetch_typeAdapter = FetchTypeColumnAdapter,
        ),
    )
}
```

Custom adapters live in `data/src/main/java/tachiyomi/data/DatabaseAdapter.kt`:

| Adapter                            | Maps                              |
|------------------------------------|-----------------------------------|
| `DateColumnAdapter`                | `Long` ↔ `java.util.Date`         |
| `StringListColumnAdapter`          | `String` (`, `-joined) ↔ `List<String>` |
| `MangaUpdateStrategyColumnAdapter` | `Long` ↔ `UpdateStrategy` enum    |
| `AnimeUpdateStrategyColumnAdapter` | `Long` ↔ `AnimeUpdateStrategy` enum|
| `FetchTypeColumnAdapter`           | `Long` ↔ `FetchType` enum         |

SQLite pragmas set on every `onOpen`: `foreign_keys = ON`,
`journal_mode = WAL`, `synchronous = NORMAL`.

---

## 3. Database access pattern (handlers)

A thin `*DatabaseHandler` abstraction wraps each generated DB so the rest of the
app depends on an interface, not on the generated code directly.

| File | Purpose |
|------|---------|
| `data/.../handlers/manga/MangaDatabaseHandler.kt`           | Interface: `await`, `awaitList`, `awaitOne`, `subscribeToList`, `subscribeToPagingSource`, etc., all parameterised on `Database`. |
| `data/.../handlers/manga/AndroidMangaDatabaseHandler.kt`    | Implementation; holds `Database` + `SqlDriver`, dispatches on `Dispatchers.IO`, supports transactions via `MangaTransactionContext.kt`. |
| `data/.../handlers/anime/AnimeDatabaseHandler.kt`           | Mirror interface for `AnimeDatabase`. |
| `data/.../handlers/anime/AndroidAnimeDatabaseHandler.kt`    | Implementation; uses `AnimeTransactionContext.kt`. |
| `data/.../handlers/*/QueryPaging*Source.kt`                 | `androidx.paging.PagingSource` bridge for SQLDelight queries. |

Repositories in `data/.../entries/`, `data/.../items/`, `data/.../history/`,
`data/.../track/`, `data/.../category/`, `data/.../updates/`,
`data/.../source/` are the concrete implementations of the domain repository
interfaces. They receive a `*DatabaseHandler` via DI and translate generated
row objects into domain models through `*Mapper` objects (e.g.
`AnimeMapper`, `AnimeTrackMapper`, `AnimeHistoryMapper`).

---

## 4. `data` module layout

```
data/
├── build.gradle.kts                      # declares both SQLDelight databases
├── src/main/
│   ├── AndroidManifest.xml
│   ├── sqldelight/                        # MANGA DB  (package tachiyomi.data)
│   │   ├── data/        *.sq              # table + query definitions
│   │   ├── view/        *.sq              # SQL views (library/history/updates)
│   │   └── migrations/  *.sqm             # 1.sqm … 32.sqm
│   ├── sqldelightanime/                   # ANIME DB  (package tachiyomi.mi.data)
│   │   ├── dataanime/   *.sq
│   │   ├── view/        *.sq
│   │   └── migrations/  *.sqm             # 113.sqm … 135.sqm
│   └── java/
│       ├── tachiyomi/data/
│       │   ├── DatabaseAdapter.kt         # ColumnAdapters
│       │   ├── handlers/{anime,manga}/    # DatabaseHandler + TransactionContext
│       │   ├── entries/{anime,manga}/     # Anime/Manga repos + mappers
│       │   ├── items/{chapter,episode}/   # Chapter/Episode repos + sanitizers
│       │   ├── history/{anime,manga}/     # History repos + mappers
│       │   ├── track/{anime,manga}/       # Track repos + mappers
│       │   ├── category/{anime,manga}/    # Category repos
│       │   ├── updates/{anime,manga}/     # Updates repos
│       │   ├── source/{anime,manga}/      # Source / StubSource repos + paging
│       │   ├── release/                   # GitHub release service
│       │   └── custombutton/              # Custom player buttons repo
│       └── mihon/data/repository/{anime,manga}/  # ExtensionRepo repos
```

---

## 5. Tables / `.sq` files

### 5.1 Manga database (`Database`) — 9 tables + 3 views

`.sq` files live in `data/src/main/sqldelight/data/`:

| `.sq` file                | Table(s) / object                  | Notes |
|---------------------------|------------------------------------|-------|
| `mangas.sq`               | `mangas`                           | Core manga entry. Has triggers `update_last_favorite_at_mangas`, `update_last_modified_at_mangas`, `update_manga_version`. |
| `chapters.sq`             | `chapters`                         | FK → `mangas(_id)` ON DELETE CASCADE. Trigger `update_last_modified_at_chapters`, `update_chapter_and_manga_version`. |
| `categories.sq`           | `categories`                       | User-defined library categories. Inserts a system category `_id=0`; trigger `system_category_delete_trigger` blocks its deletion. |
| `mangas_categories.sq`    | `mangas_categories`                | Join table manga↔category. FK both sides CASCADE. Trigger bumps manga `version` on insert. |
| `history.sq`              | `history`                          | Read history. FK → `chapters(_id)` CASCADE. `last_read` stored as `Date` (via `DateColumnAdapter`). |
| `manga_sync.sq`           | `manga_sync`                       | Tracker sync rows (AniList/MAL/Shikimori/…). FK → `mangas(_id)` CASCADE. UNIQUE(`manga_id`, `sync_id`). |
| `sources.sq`              | `sources`                          | Cached source metadata (`_id`, `lang`, `name`). |
| `extension_repos.sq`      | `extension_repos`                  | Extension repo URLs + signing key fingerprints. |
| `excluded_scanlators.sq`  | `excluded_scanlators`              | Per-manga scanlator blocklist. FK → `mangas(_id)` CASCADE. |

Views in `data/src/main/sqldelight/view/`:

| `.sq` file       | View             | Purpose |
|------------------|------------------|---------|
| `libraryView.sq` | `libraryView`    | `mangas` LEFT JOIN chapter aggregates (total/read/bookmark/latestUpload/lastRead) + category — feeds the library list. |
| `historyView.sq` | `historyView`    | `mangas` ⋈ `chapters` ⋈ `history` + a `max_last_read` sub-query — drives the "Continue reading" / History tab. |
| `updatesView.sq` | `updatesView`    | `mangas` ⋈ `chapters` filtered to `favorite=1 AND date_fetch > date_added` — drives the Updates tab. |

### 5.2 Anime database (`AnimeDatabase`) — 9 tables + 8 views

`.sq` files live in `data/src/main/sqldelightanime/dataanime/`:

| `.sq` file              | Table(s) / object     | Notes |
|-------------------------|-----------------------|-------|
| `animes.sq`             | `animes`              | Core anime entry. Adds anime-specific columns: `fetch_type`, `parent_id`, `season_flags`, `season_number`, `season_source_order`, `background_url`, `background_last_modified`. Triggers mirror the manga side (`update_last_favorite_at_animes`, `update_last_modified_at_animes`, `update_anime_version`). |
| `episodes.sq`           | `episodes`            | FK → `animes(_id)` CASCADE. Adds `last_second_seen`, `total_seconds`, `summary`, `preview_url`, `fillermark`. |
| `categories.sq`         | `categories`          | Same shape as manga `categories` (anime-side copy with its own `_id=0` system row + trigger). |
| `animes_categories.sq`  | `animes_categories`   | Join anime↔category. Trigger bumps anime `version`. |
| `animehistory.sq`       | `animehistory`        | Watch history. FK → `episodes(_id)` CASCADE. `last_seen` as `Date`. (No `time_read` column — anime tracks duration via episode `last_second_seen`.) |
| `anime_sync.sq`         | `anime_sync`          | Tracker sync rows for anime. FK → `animes(_id)` CASCADE. UNIQUE(`anime_id`, `sync_id`). |
| `animesources.sq`       | `animesources`        | Cached anime source metadata. |
| `extension_repos.sq`    | `extension_repos`     | Duplicate of the manga-side table (anime extension repos). |
| `custom_buttons.sq`     | `custom_buttons`      | mpv-style custom player buttons (Lua content). Seeds a "+85 s" skip-intro button at `_id=1`. |

Views in `data/src/main/sqldelightanime/view/`:

| `.sq` file                  | View                    | Purpose |
|-----------------------------|-------------------------|---------|
| `animelibView.sq`           | `animelibView`          | Anime library list (analogous to `libraryView`, but branches on `fetch_type` between episode-stats and season-stats). |
| `animehistoryView.sq`       | `animehistoryView`      | Anime history join. |
| `animeupdatesView.sq`       | `animeupdatesView`      | Anime updates tab. |
| `animedeletableView.sq`     | `animedeletableView`    | Anime entries that can be pruned (non-favorite, etc.). |
| `animeseasonsView.sq`       | `animeseasonsView`      | Season grouping (parent anime → child season rows). |
| `animeseasonstatsView.sq`   | `animeseasonstatsView`  | Per-parent season aggregates (child_count, fully_seen_seasons, max_last_seen, …). |
| `animehistorystatsView.sq`  | `animehistorystatsView` | Last-seen aggregate per anime. |
| `episodestatsView.sq`       | `episodestatsView`      | Per-anime episode aggregates (total, seen, bookmark, fillermark, latestUpload, fetchedAt). |

So the totals are: **18 tables across the two databases (9 + 9)** plus **11
views (3 + 8)**.

---

## 6. Schema version

SQLDelight exposes the schema version as a generated constant
`Database.Schema.version` / `AnimeDatabase.Schema.version` (an `Int`). At
runtime `AndroidSqliteDriver` reads this and runs the appropriate `.sqm`
migrations when the on-disk DB file is older.

The on-disk schema version is determined by the highest-numbered `.sqm`
migration file present in each database's `migrations/` directory:

| Database        | Migration files               | Highest `.sqm` | Schema version |
|-----------------|-------------------------------|----------------|----------------|
| `Database`      | `1.sqm` … `32.sqm` (32 files) | `32.sqm`       | **32**         |
| `AnimeDatabase` | `113.sqm` … `135.sqm` (23 files) | `135.sqm`   | **135**        |

> **Observation / inference (not verified at runtime):** the anime DB's
> migration sequence starts at `113`, not `1`. This strongly suggests the anime
> DB was forked from the manga DB at the point where manga's schema was at
> version ~112, and the two have since diverged: anime continues its own
> migration numbering from 113 upward, while the manga side appears to have had
> its earlier migrations squashed into the `.sq` files and now maintains a
> separate, lower-numbered sequence (1–32). The two `.sq` directories therefore
> represent fully independent schemas that happen to share a common ancestry
> and similar table shapes.

---

## 7. Migration mechanism

Migrations in SQLDelight are plain SQL files named `<version>.sqm`. Each file
contains the statements needed to bring the database from the previous version
to that version. SQLDelight collects every `.sqm` under the database's
`srcDirs`, orders them numerically, and applies the ones newer than the
on-disk version on first open (via the `AndroidSqliteDriver.Callback`).

Examples:

`data/src/main/sqldelight/migrations/1.sqm` (manga):
```sql
ALTER TABLE chapters
ADD COLUMN source_order INTEGER DEFAULT 0;

UPDATE mangas
SET thumbnail_url = replace(thumbnail_url, '93.174.95.110', 'kissmanga.com')
WHERE source = 4;
```

`data/src/main/sqldelight/migrations/32.sqm` (manga):
```sql
import kotlin.Boolean;

-- Add private field for tracking
ALTER TABLE manga_sync ADD COLUMN private INTEGER AS Boolean DEFAULT 0 NOT NULL;
```

`data/src/main/sqldelightanime/migrations/135.sqm` (anime):
```sql
UPDATE animes
SET fetch_type = 1;
```

There is **no Kotlin-side `Migration` object graph** (no analogue of Room's
`Migration(N, N+1)` registry). All schema evolution is declarative in `.sqm`
files; SQLDelight wires them up automatically from the file system.

When the `.sq` files themselves are changed in a way that affects the schema,
the build regenerates `Database.Schema` (and the `.db` schema dump into
`schemaOutputDirectory`); the developer is then expected to add a new
`<next-version>.sqm` file describing how to migrate an existing DB to the new
shape. CI / the SQLDelight Gradle task can verify that `.sqm` files actually
bridge the old and new schemas.

---

## 8. Surprises / things worth knowing

1. **Two databases, not one.** Most forks of Tachiyomi/Mihon keep a single DB.
   Kuta splits anime and manga into separate SQLite files (`tachiyomi.db` and
   `tachiyomi.animedb`) with their own generated `Database` interfaces, own
   handlers, own mappers, and own migration sequences. This doubles the surface
   area of the data layer and is the reason every repository is duplicated
   under `.../anime/` and `.../manga/` packages.
2. **Manga is still fully present.** Even though the project goal is to disable
   the manga UI, the manga database, migrations, mappers, repositories, and
   domain models are intact and wired in `AppModule.kt`. Disabling the UI will
   not touch the data layer.
3. **Anime-specific columns.** `animes` extends the manga shape with
   `fetch_type`, `parent_id`, `season_flags`, `season_number`,
   `season_source_order`, `background_url`, `background_last_modified` — these
   support anime-only features like season grouping and background art.
4. **Anime episodes carry player metadata** (`last_second_seen`,
   `total_seconds`, `summary`, `preview_url`, `fillermark`) that manga
   chapters don't have.
5. **Custom mpv buttons are persisted in the anime DB** (`custom_buttons.sq`),
   not in preferences — so they back up with the anime database.
6. **`animehistory` has no `time_read` column** (manga `history` does). Anime
   watch duration is reconstructed from `episodes.last_second_seen` /
   `total_seconds`.
7. **Version-controlled row mutation.** Both DBs use SQL triggers
   (`update_*_version`, `update_last_modified_at_*`) to automatically bump a
   `version` and `last_modified_at` column on every UPDATE — these power the
   sync feature (`is_syncing` flag exempts sync-driven writes from version
   bumps).
