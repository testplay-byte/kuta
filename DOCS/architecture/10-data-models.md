# 10 — Core Data Models

> Scope: the domain-level data classes the app passes around. For each model:
> file path, declaration, key fields, and purpose. **Research documentation
> only** — no source files were modified.

## TL;DR

- Domain models live in the **`domain/` module** under
  `tachiyomi.domain.*` (and a few under `aniyomi.domain.*`).
- They are plain Kotlin `data class`es — *not* ORM entities. There is **no
  Room `@Entity` / no SQLDelight annotation** on them.
- Each model has a parallel `*Update` data class (nullable fields) used by
  repositories for partial updates.
- DB ↔ domain mapping is done by hand in `*Mapper` objects in the `data/`
  module — SQLDelight generates row data classes (e.g. `dataanime.Animes`)
  which the mappers turn into the domain `Anime`.
- **Anime and manga are fully parallel**: `Anime`/`Manga`, `Episode`/`Chapter`,
  `AnimeTrack`/`MangaTrack`, `AnimeHistory`/`MangaHistory`,
  `AnimeHistoryWithRelations`/`MangaHistoryWithRelations`,
  `LibraryAnime`/`LibraryManga`, `AnimeDownload`/`MangaDownload`. Field shapes
  are near-identical; anime adds player/season-specific fields.

---

## Conventions

- All `id`s are `Long` (SQLDelight row primary keys).
- Date/time epoch values are `Long` (epoch millis) on the domain model; the
  `Date`-typed `last_read` / `last_seen` columns in the DB are converted via
  `DateColumnAdapter` only at the SQLDelight layer.
- "Flags" columns (`viewerFlags`, `episodeFlags`, `chapterFlags`,
  `seasonFlags`, `flags`) pack many boolean/enum states into one `Long` using
  bitmask constants declared in the model's `companion object`.
- Each model has a `fun create()` factory in its companion for an "empty"
  placeholder (id = `-1`).

---

## 1. `Anime`

**File:** `domain/src/main/java/tachiyomi/domain/entries/anime/model/Anime.kt`
**Package:** `tachiyomi.domain.entries.anime.model`
**Purpose:** A single anime entry in the user's library or cache (one row in the
`animes` table of the anime DB).

```kotlin
@Immutable
data class Anime(
    val id: Long,
    val source: Long,                  // extension/source id
    val favorite: Boolean,             // in library?
    val lastUpdate: Long,
    val nextUpdate: Long,
    val fetchInterval: Int,            // estimated days between updates
    val dateAdded: Long,
    val viewerFlags: Long,             // skip-intro length, next-airing info, …
    val episodeFlags: Long,            // per-anime filter/sort/display flags
    val coverLastModified: Long,
    val backgroundLastModified: Long,  // anime-only: background art
    val url: String,
    val title: String,
    val artist: String?,
    val author: String?,
    val description: String?,
    val genre: List<String>?,
    val status: Long,                  // 0=UNKNOWN,1=ONGOING,2=COMPLETED,… (SAnime)
    val thumbnailUrl: String?,
    val backgroundUrl: String?,        // anime-only
    val updateStrategy: AnimeUpdateStrategy,
    val initialized: Boolean,
    val lastModifiedAt: Long,          // bumped by SQL trigger on every UPDATE
    val favoriteModifiedAt: Long?,
    val version: Long,                 // bumped by SQL trigger when fields change
    val fetchType: FetchType,          // anime-only: Episodes vs Seasons
    val parentId: Long?,               // anime-only: parent anime for season rows
    val seasonFlags: Long,             // anime-only: season filter/sort/display
    val seasonNumber: Double,
    val seasonSourceOrder: Long,
) : Serializable
```

The companion object declares a large set of bitmask constants
(`EPISODE_SHOW_UNSEEN`, `EPISODE_SORTING_MASK`, `SEASON_SORT_DESC`,
`ANIME_INTRO_MASK`, `ANIME_AIRING_EPISODE_MASK`, …) used by derived getters
(`sorting`, `displayMode`, `unseenFilter`, `skipIntroLength`,
`nextEpisodeToAir`, `seasonDisplayGridMode`, `seasonContinueOverlay`, …).

**DB mapping:** `data/.../entries/anime/AnimeMapper.kt` (`mapAnime`,
`mapLibraryAnime`, …) turns the generated `dataanime.Animes` row into
`Anime`/`LibraryAnime`.

---

## 2. `Episode`

**File:** `domain/src/main/java/tachiyomi/domain/items/episode/model/Episode.kt`
**Package:** `tachiyomi.domain.items.episode.model`
**Purpose:** A single episode of an anime (one row in `episodes`).

```kotlin
data class Episode(
    val id: Long,
    val animeId: Long,
    val seen: Boolean,                 // watched to end?
    val bookmark: Boolean,
    val fillermark: Boolean,           // marks filler episodes
    val lastSecondSeen: Long,          // resume position
    val totalSeconds: Long,            // episode length
    val dateFetch: Long,
    val sourceOrder: Long,
    val url: String,
    val name: String,
    val dateUpload: Long,
    val episodeNumber: Double,
    val scanlator: String?,            // actually used for sub-group/release name
    val summary: String?,              // episode synopsis
    val previewUrl: String?,           // episode thumbnail URL
    val lastModifiedAt: Long,
    val version: Long,
)
```

Has `copyFrom(other: Episode)` to merge in freshly-fetched fields without
overwriting local-only ones.

**`EpisodeUpdate`** (same file's sibling `EpisodeUpdate.kt`) is the
all-nullable patch DTO used by `UpdateEpisode`:

```kotlin
data class EpisodeUpdate(
    val id: Long,
    val animeId: Long? = null,
    val seen: Boolean? = null,
    val bookmark: Boolean? = null,
    val fillermark: Boolean? = null,
    val lastSecondSeen: Long? = null,
    val totalSeconds: Long? = null,
    /* …all other fields nullable… */
    val version: Long? = null,
)
fun Episode.toEpisodeUpdate(): EpisodeUpdate
```

The repository turns nullables into SQLDelight `coalesce(:param, col)` calls
(see `episodes.sq` `update:` query), so unset fields are preserved.

**DB mapping:** `data/.../items/episode/EpisodeRepositoryImpl.kt` + `EpisodeSanitizer.kt`.

---

## 3. `Manga` (kept; UI being disabled)

**File:** `domain/src/main/java/tachiyomi/domain/entries/manga/model/Manga.kt`
**Package:** `tachiyomi.domain.entries.manga.model`
**Purpose:** A single manga entry (one row in `mangas`). The codebase is
keeping the model and DB layer even while the UI is being disabled.

```kotlin
@Immutable
data class Manga(
    val id: Long,
    val source: Long,
    val favorite: Boolean,
    val lastUpdate: Long,
    val nextUpdate: Long,
    val fetchInterval: Int,
    val dateAdded: Long,
    val viewerFlags: Long,
    val chapterFlags: Long,            // manga equivalent of episodeFlags
    val coverLastModified: Long,
    val url: String,
    val title: String,
    val artist: String?,
    val author: String?,
    val description: String?,
    val genre: List<String>?,
    val status: Long,
    val thumbnailUrl: String?,
    val updateStrategy: UpdateStrategy,
    val initialized: Boolean,
    val lastModifiedAt: Long,
    val favoriteModifiedAt: Long?,
    val version: Long,
) : Serializable
```

Notice it is *shorter* than `Anime`: no `fetchType`, `parentId`, `seasonFlags`,
`seasonNumber`, `backgroundUrl`, `backgroundLastModified`. The companion
exposes the `CHAPTER_*` mask constants (`CHAPTER_SHOW_UNREAD`,
`CHAPTER_SORTING_NUMBER`, `CHAPTER_DISPLAY_NUMBER`, …).

**DB mapping:** `data/.../entries/manga/MangaMapper.kt` /
`MangaRepositoryImpl.kt`. The `mapAnime`/`mapManga` mappers are
near-line-for-line twins.

---

## 4. `Chapter`

**File:** `domain/src/main/java/tachiyomi/domain/items/chapter/model/Chapter.kt`
**Package:** `tachiyomi.domain.items.chapter.model`
**Purpose:** A single chapter of a manga (one row in `chapters`).

```kotlin
data class Chapter(
    val id: Long,
    val mangaId: Long,
    val read: Boolean,                 // == Episode.seen
    val bookmark: Boolean,
    val lastPageRead: Long,            // == Episode.lastSecondSeen (page index)
    val dateFetch: Long,
    val sourceOrder: Long,
    val url: String,
    val name: String,
    val dateUpload: Long,
    val chapterNumber: Double,         // == Episode.episodeNumber
    val scanlator: String?,
    val lastModifiedAt: Long,
    val version: Long,
)
```

Compared to `Episode`, `Chapter` lacks `fillermark`, `totalSeconds`,
`summary`, `previewUrl`. There is a sibling `ChapterUpdate.kt` with the same
nullable-patch pattern as `EpisodeUpdate`, plus `ChapterSanitizer.kt` in the
`data/` module.

---

## 5. `Track` — split into `AnimeTrack` / `MangaTrack`

There is no single `Track` domain class. Aniyomi keeps two parallel classes,
one per side. Each maps to a row in the corresponding `*_sync` table
(`anime_sync` / `manga_sync`) where `sync_id` identifies the tracker
(AniList, MAL, Shikimori, Bangumi, Kitsu, MangaUpdates, Komga, … — see
`TrackerManager`).

### 5.1 `AnimeTrack`

**File:** `domain/src/main/java/tachiyomi/domain/track/anime/model/AnimeTrack.kt`
**Package:** `tachiyomi.domain.track.anime.model`
**Purpose:** One tracker-binding row for an anime.

```kotlin
data class AnimeTrack(
    val id: Long,                      // _id in anime_sync
    val animeId: Long,
    val trackerId: Long,               // == sync_id column (tracker enum id)
    val remoteId: Long,                // tracker-side media id
    val libraryId: Long?,
    val title: String,
    val lastEpisodeSeen: Double,
    val totalEpisodes: Long,
    val status: Long,                  // tracker status enum (watching/plan-to/…)
    val score: Double,
    val remoteUrl: String,
    val startDate: Long,
    val finishDate: Long,
    val private: Boolean,              // "private" on the tracker
) : Serializable
```

### 5.2 `MangaTrack`

**File:** `domain/src/main/java/tachiyomi/domain/track/manga/model/MangaTrack.kt`
**Package:** `tachiyomi.domain.track.manga.model`
**Purpose:** One tracker-binding row for a manga (shape mirrors `AnimeTrack`).

```kotlin
data class MangaTrack(
    val id: Long,
    val mangaId: Long,
    val trackerId: Long,
    val remoteId: Long,
    val libraryId: Long?,
    val title: String,
    val lastChapterRead: Double,       // == AnimeTrack.lastEpisodeSeen
    val totalChapters: Long,           // == AnimeTrack.totalEpisodes
    val status: Long,
    val score: Double,
    val remoteUrl: String,
    val startDate: Long,
    val finishDate: Long,
    val private: Boolean,
) : Serializable
```

**DB mapping:** `data/.../track/anime/AnimeTrackMapper.kt`,
`AnimeTrackRepositoryImpl.kt` and the manga-side twins. Note the column ↔ field
rename `sync_id` → `trackerId`.

---

## 6. `History` — split into `AnimeHistory` / `MangaHistory` (+ `WithRelations`)

History drives the "Continue watching/reading" cards and the History tab. As
with Track, anime and manga each have their own classes.

### 6.1 `AnimeHistory`

**File:** `domain/src/main/java/tachiyomi/domain/history/anime/model/AnimeHistory.kt`
**Purpose:** A bare history row (one `animehistory` row).

```kotlin
data class AnimeHistory(
    val id: Long,
    val episodeId: Long,
    val seenAt: Date?,
) {
    companion object {
        fun create() = AnimeHistory(id = -1L, episodeId = -1L, seenAt = null)
    }
}
```

### 6.2 `AnimeHistoryWithRelations`

**File:** `domain/src/main/java/tachiyomi/domain/history/anime/model/AnimeHistoryWithRelations.kt`
**Purpose:** The enriched row produced by the `animehistoryView` SQL view —
joins `animes` + `episodes` + `animehistory`. This is what the History UI
actually consumes.

```kotlin
data class AnimeHistoryWithRelations(
    val id: Long,
    val episodeId: Long,
    val animeId: Long,
    val title: String,
    val episodeNumber: Double,
    val seenAt: Date?,
    val coverData: AnimeCover,
)
```

(`AnimeCover` itself is a small `data class(animeId, sourceId, isAnimeFavorite,
url, lastModified)` in `domain/.../entries/anime/model/AnimeCover.kt` — the
image-loader's input.)

### 6.3 `MangaHistory` / `MangaHistoryWithRelations`

**Files:** `domain/src/main/java/tachiyomi/domain/history/manga/model/MangaHistory.kt`
and `.../MangaHistoryWithRelations.kt`
**Purpose:** Manga-side twins.

```kotlin
data class MangaHistory(
    val id: Long,
    val chapterId: Long,
    val readAt: Date?,
    val readDuration: Long,            // total time spent reading this chapter
)

data class MangaHistoryWithRelations(
    val id: Long,
    val chapterId: Long,
    val mangaId: Long,
    val title: String,
    val chapterNumber: Double,
    val readAt: Date?,
    val readDuration: Long,
    val coverData: MangaCover,
)
```

> **Difference worth noting:** `MangaHistory` carries `readDuration` (and the
> `history` table has a `time_read` column to back it). `AnimeHistory` does
> not — anime watch time is reconstructed from `episodes.last_second_seen` /
> `total_seconds`. This asymmetry is mirrored in the `.sq` schemas.

There is also an `AnimeHistoryUpdate` / `MangaHistoryUpdate` data class used by
the `UpsertHistory` interactors.

**DB mapping:** `data/.../history/anime/AnimeHistoryMapper.kt`,
`AnimeHistoryRepositoryImpl.kt` (+ manga twins).

---

## 7. `Category`

**File:** `domain/src/main/java/tachiyomi/domain/category/model/Category.kt`
**Package:** `tachiyomi.domain.category.model`
**Purpose:** A user-defined library category ("Reading", "Watching", "Plan to
read", …). One row in `categories` (anime or manga DB — both have an
identical `categories` table).

```kotlin
data class Category(
    val id: Long,
    val name: String,
    val order: Long,                   // == `sort` column in the .sq file
    val flags: Long,                   // per-category display/sort flags
    val hidden: Boolean,
) : Serializable {

    val isSystemCategory: Boolean = id == UNCATEGORIZED_ID

    companion object {
        const val UNCATEGORIZED_ID = 0L
    }
}
```

`UNCATEGORIZED_ID = 0` is the synthetic "Default" category seeded by the `.sq`
file's `INSERT OR IGNORE INTO categories(_id, name, sort, flags) VALUES (0, "", -1, 0)`
and protected from deletion by the `system_category_delete_trigger`.

There is a sibling `CategoryUpdate.kt` (all-nullable patch DTO). Note: unlike
Anime/Manga, there is just **one** `Category` class shared by both sides
(anime and manga categories share the same Kotlin type; the difference is only
which DB / repository you query through — `AnimeCategoryRepositoryImpl` vs
`MangaCategoryRepositoryImpl`).

---

## 8. `Download` — split into `AnimeDownload` / `MangaDownload`

Downloads are *not* persisted in the database. They are in-memory queue items
owned by the `*DownloadManager` (in the `app/` module) and rehydrated from the
file system on startup. The classes live next to the download manager, not in
`domain/`.

### 8.1 `AnimeDownload`

**File:** `app/src/main/java/eu/kanade/tachiyomi/data/download/anime/model/AnimeDownload.kt`
**Package:** `eu.kanade.tachiyomi.data.download.anime.model`
**Purpose:** A single anime episode in the download queue. Carries live
progress/state flows for the download UI.

```kotlin
data class AnimeDownload(
    val source: AnimeHttpSource,
    val anime: Anime,
    val episode: Episode,
    val changeDownloader: Boolean = false,
    var video: Video? = null,
) : ProgressListener {

    val statusFlow: StateFlow<State>      // NOT part of data ctor; backed by MutableStateFlow
    var status: State                     // NOT part of data ctor
    val progressFlow: StateFlow<Int>
    var progress: Int

    override fun update(bytesRead: Long, contentLength: Long, done: Boolean)

    enum class State(val value: Int) {
        NOT_DOWNLOADED(0),
        QUEUE(1),
        DOWNLOADING(2),
        DOWNLOADED(3),
        ERROR(4),
    }

    companion object {
        suspend fun fromEpisodeId(
            episodeId: Long,
            getEpisode: GetEpisode = Injekt.get(),
            getAnimeById: GetAnime = Injekt.get(),
            sourceManager: AnimeSourceManager = Injekt.get(),
        ): AnimeDownload?
    }
}
```

The `@Transient` flow fields on the JVM serializer are not part of the data
class constructor (which is why they aren't in the snippet above); they are
initialised in the body. They are observable from the Compose download UI.

### 8.2 `MangaDownload`

**File:** `app/src/main/java/eu/kanade/tachiyomi/data/download/manga/model/MangaDownload.kt`
**Package:** `eu.kanade.tachiyomi.data.download.manga.model`
**Purpose:** A single manga chapter in the download queue. Same shape as
`AnimeDownload` but with a `pages: List<Page>?` instead of `video: Video?`.

```kotlin
data class MangaDownload(
    val source: HttpSource,
    val manga: Manga,
    val chapter: Chapter,
) {
    var pages: List<Page>?
    val totalProgress: Int
    val downloadedImages: Int
    val statusFlow: StateFlow<State>
    var status: State
    val progressFlow: StateFlow<Int>
    var progress: Int
    /* …same State enum (NOT_DOWNLOADED, QUEUE, DOWNLOADING, DOWNLOADED, ERROR)… */
}
```

### 8.3 `AnimeDownloadPart`

**File:** `app/src/main/java/eu/kanade/tachiyomi/data/download/anime/model/AnimeDownloadPart.kt`
**Purpose:** A single HTTP range part of a multi-part anime download. Holds the
byte `range`, a `UniFile` for the `.part.tmp` file, and a `completed` flag.
Used when an anime video is downloaded in parallel chunks.

---

## 9. Supporting "view-model" domain classes

The SQL views (`libraryView`, `animelibView`, `historyView`, `updatesView`,
…) produce composite rows that map to extra domain classes. The most
important ones:

### `LibraryAnime`

**File:** `domain/src/main/java/tachiyomi/domain/library/anime/LibraryAnime.kt`
**Purpose:** One row of `animelibView` — an `Anime` plus per-anime aggregate
counts. Drives the anime Library tab.

```kotlin
data class LibraryAnime(
    val anime: Anime,
    val category: Long,
    val totalCount: Long,
    val seenCount: Long,
    val bookmarkCount: Long,
    val fillermarkCount: Long,
    val latestUpload: Long,
    val episodeFetchedAt: Long,
    val lastSeen: Long,
) {
    val id: Long = anime.id
    val unseenCount get() = totalCount - seenCount
    val hasBookmarks get() = bookmarkCount > 0
    val hasStarted = seenCount > 0
}
```

### `LibraryManga`

**File:** `domain/src/main/java/tachiyomi/domain/library/manga/LibraryManga.kt`
Twin of `LibraryAnime` for the manga side (`totalChapters`, `readCount`,
`bookmarkCount`, `latestUpload`, `chapterFetchedAt`, `lastRead`).

### `SeasonAnime`

**File:** `domain/src/main/java/aniyomi/domain/anime/SeasonAnime.kt`
**Package:** `aniyomi.domain.anime` (note: `aniyomi.*`, not `tachiyomi.*`)
**Purpose:** A "season" view of an anime — wraps an `Anime` (the parent row)
plus season-aggregate counts (`totalCount`, `seenCount`, `bookmarkCount`,
`fillermarkCount`, `latestUpload`, `fetchedAt`, `lastSeen`). Convertible to
`LibraryAnime` via `toLibraryAnime()`.

### `AnimeUpdatesWithRelations` / `MangaUpdatesWithRelations`

**Files:** `domain/src/main/java/tachiyomi/domain/updates/{anime,manga}/model/*UpdatesWithRelations.kt`
**Purpose:** Enriched rows from the `*updatesView` views that drive the
Updates tab.

### `AnimeHistoryUpdate` / `MangaHistoryUpdate`

**Files:** `domain/.../history/{anime,manga}/model/*HistoryUpdate.kt`
**Purpose:** Patch DTOs used by `Upsert*History` interactors.

### `AnimeUpdate` / `MangaUpdate` / `EpisodeUpdate` / `ChapterUpdate` / `CategoryUpdate` / `CustomButtonUpdate`

Each persistent model has a sibling `*Update` data class whose fields are all
nullable except `id`. The repositories translate these into SQLDelight
`coalesce(:param, col)` updates so unspecified fields are preserved. This is
the project's equivalent of Room's `@Update` partial-entity behaviour.

---

## 10. Model ↔ DB row ↔ view matrix

| Domain model                  | DB table       | DB package        | SQL view (if any)              | Mapper                                |
|-------------------------------|----------------|-------------------|--------------------------------|---------------------------------------|
| `Anime`                       | `animes`       | `tachiyomi.mi.data` (`dataanime.Animes`) | `animelibView`, `animehistoryView`, `animeupdatesView`, `animedeletableView`, `animeseasonsView` | `AnimeMapper` |
| `Episode`                     | `episodes`     | `dataanime.Episodes` | `episodestatsView`          | (`EpisodeRepositoryImpl` direct)      |
| `Manga`                       | `mangas`       | `tachiyomi.data` (`data.Mangas`) | `libraryView`, `historyView`, `updatesView` | `MangaMapper` |
| `Chapter`                     | `chapters`     | `data.Chapters`   | — (joined inside `libraryView`) | (`ChapterRepositoryImpl` direct)      |
| `AnimeTrack`                  | `anime_sync`   | `dataanime.AnimeSync` | —                         | `AnimeTrackMapper`                    |
| `MangaTrack`                  | `manga_sync`   | `data.MangaSync`  | —                              | `MangaTrackMapper`                    |
| `AnimeHistory` (+WithRelations) | `animehistory` | `dataanime.Animehistory` | `animehistoryView`, `animehistorystatsView` | `AnimeHistoryMapper` |
| `MangaHistory` (+WithRelations) | `history`      | `data.History`    | `historyView`                  | `MangaHistoryMapper`                  |
| `Category`                    | `categories` (both DBs) | `data.Categories` / `dataanime.Categories` | — | (repo direct, both sides) |
| `AnimeDownload` / `MangaDownload` | *(none — in-memory)* | — | — | (built by `*DownloadManager`) |
| `LibraryAnime` / `LibraryManga` | `animes`/`mangas` + joins | — | `animelibView` / `libraryView` | `AnimeMapper.mapLibraryAnime` / `MangaMapper.mapLibraryManga` |
| `SeasonAnime`                 | `animes` + `animeseasonstatsView` | — | `animeseasonsView`, `animeseasonstatsView` | (`AnimeMapper` + repository) |
| `*UpdatesWithRelations`       | `animes`/`mangas` + `episodes`/`chapters` | — | `animeupdatesView` / `updatesView` | (repo direct) |

---

## 11. Surprises / things worth knowing

1. **No annotations on the models.** Unlike Room (`@Entity`, `@PrimaryKey`),
   these `data class`es are pure. All persistence knowledge lives in the
   `.sq` files and the hand-written `*Mapper` objects. The mapping is type-safe
   only because SQLDelight's generated query functions take named parameters
   matching the `*Mapper.map*(...)` argument list.
2. **Anime ↔ manga parallelism is exhaustive.** Every persistent anime concept
   has a manga twin. When the manga UI is disabled, the data layer requires no
   changes; only the UI/screen-model/repository-consumer layer needs gating.
3. **`Category` is the one shared model.** Both anime and manga use the same
   `Category` Kotlin class even though there are two `categories` tables.
   Disambiguation happens at the repository level.
4. **Downloads are intentionally non-persistent.** `AnimeDownload` /
   `MangaDownload` live in the `app/` module (next to the download managers),
   not in `domain/`, and they rehydrate from disk rather than from the DB.
   Their mutable `status` / `progress` fields are `StateFlow`-backed and
   observable from Compose.
5. **`version` and `lastModifiedAt` are trigger-maintained.** Every UPDATE on
   `animes`/`mangas`/`episodes`/`chapters` fires a SQL trigger that bumps
   `last_modified_at`; further triggers bump `version` (unless `is_syncing = 1`).
   The domain model surfaces these as plain `Long` fields and they power the
   sync feature. Don't write them manually.
6. **Anime-only fields are scattered through several models.** `Anime` has 6
   anime-only columns; `Episode` has 3 (`fillermark`, `totalSeconds`,
   `summary`/`previewUrl`); `AnimeDownload` has `video` and the
   `AnimeDownloadPart` companion. None of these exist on the manga side.
7. **`aniyomi.domain.anime.SeasonAnime` lives outside the `tachiyomi.*`
   namespace** — it's one of the few classes in the `aniyomi.*` package, which
   hints at it being a newer/Aniyomi-specific addition layered atop the
   upstream Tachiyomi/Mihon core.
