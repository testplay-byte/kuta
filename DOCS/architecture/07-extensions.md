# 07 — Extension / Source System

> Investigation of the Kuta (Aniyomi fork) extension system: the `Source`
> interface hierarchy (anime vs manga), runtime extension loading via
> `PathClassLoader`, the extension-index JSON schema, and the manager
> singletons.
>
> No source files were modified. All paths are relative to the repo root
> (`/home/z/kuta`).

---

## 1. The `Source` interface — there are **two parallel hierarchies**

Aniyomi inherits Tachiyomi's manga side and adds an anime side. There is **no
single `Source` interface**; instead there is a pair of independent hierarchies
that mirror each other:

| Side | Base interface | File |
|---|---|---|
| **Anime** | `eu.kanade.tachiyomi.animesource.AnimeSource` | `source-api/src/commonMain/kotlin/eu/kanade/tachiyomi/animesource/AnimeSource.kt` |
| **Manga** | `eu.kanade.tachiyomi.source.MangaSource` | `source-api/src/commonMain/kotlin/eu/kanade/tachiyomi/source/MangaSource.kt` |

This matters for an anime-only fork: every file under
`source-api/.../animesource/` is the one to keep; everything under
`source-api/.../source/` (the manga `MangaSource`, `SChapter`, `SManga`,
`Page`, `MangasPage`, `CatalogueSource`, `HttpSource`, `ParsedHttpSource`,
`SourceFactory`) is dead weight once manga support is removed.

### 1a. `AnimeSource` — full method list (quoted verbatim)

`source-api/src/commonMain/kotlin/eu/kanade/tachiyomi/animesource/AnimeSource.kt`:

```kotlin
interface AnimeSource {

    val id: Long                                              // Unique source id
    val name: String                                          // Display name
    val lang: String get() = ""                               // ISO 639-1 (default empty)

    /** Get the updated details for an anime. (ext-lib 1.5) */
    suspend fun getAnimeDetails(anime: SAnime): SAnime {
        return fetchAnimeDetails(anime).awaitSingle()
    }

    /** Get all the available episodes for an anime. (ext-lib 1.5) */
    suspend fun getEpisodeList(anime: SAnime): List<SEpisode> {
        return fetchEpisodeList(anime).awaitSingle()
    }

    /** Get all the available seasons for an anime. (ext-lib 16) */
    suspend fun getSeasonList(anime: SAnime): List<SAnime>

    /** Get the list of hosters for an episode. First hoster is preferred. (ext-lib 16) */
    suspend fun getHosterList(episode: SEpisode): List<Hoster> =
        throw IllegalStateException("Not used")

    /** Get the list of videos for a hoster. (ext-lib 16) */
    suspend fun getVideoList(hoster: Hoster): List<Video> =
        throw IllegalStateException("Not used")

    /** Get the list of videos an episode has (legacy). (ext-lib 1.5) */
    suspend fun getVideoList(episode: SEpisode): List<Video> {
        return fetchVideoList(episode).awaitSingle()
    }

    // --- Deprecated RxJava stubs (kept for back-compat with old extensions) ---
    @Deprecated("Use the non-RxJava API instead", ReplaceWith("getAnimeDetails"))
    fun fetchAnimeDetails(anime: SAnime): Observable<SAnime> =
        throw IllegalStateException("Not used")

    @Deprecated("Use the non-RxJava API instead", ReplaceWith("getEpisodeList"))
    fun fetchEpisodeList(anime: SAnime): Observable<List<SEpisode>> =
        throw IllegalStateException("Not used")

    @Deprecated("Use the non-RxJava API instead", ReplaceWith("getVideoList"))
    fun fetchVideoList(episode: SEpisode): Observable<List<Video>> =
        throw IllegalStateException("Not used")
}
```

### 1b. Anime source hierarchy

```
AnimeSource                                   (base — animesource/AnimeSource.kt)
   │
   ├── AnimeCatalogueSource : AnimeSource     (animesource/AnimeCatalogueSource.kt)
   │       Adds: lang (concrete), supportsLatest,
   │              getPopularAnime(page), getSearchAnime(page, q, filters),
   │              getLatestUpdates(page), getFilterList()
   │
   ├── AnimeHttpSource : AnimeCatalogueSource (animesource/online/AnimeHttpSource.kt)
   │       Concrete abstract base for HTTP/HTML sources.
   │       Adds: baseUrl, client (OkHttp), headers, versionId, generateId(),
   │             popularAnimeRequest/Parse, searchAnimeRequest/Parse,
   │             latestUpdatesRequest/Parse, animeDetailsRequest/Parse,
   │             episodeListRequest/Parse, seasonListRequest/Parse,
   │             hosterListRequest/Parse, videoListRequest(hoster)/Parse,
   │             videoListRequest(episode)/Parse (legacy),
   │             videoUrlRequest/Parse (legacy),
   │             resolveVideo(video), List<Hoster>.sortHosters(),
   │             List<Video>.sortVideos(), getAnimeUrl(), getEpisodeUrl(),
   │             prepareNewEpisode(), getVideo() / getVideoSize() / videoRequest().
   │
   ├── ParsedAnimeHttpSource : AnimeHttpSource (animesource/online/ParsedAnimeHttpSource.kt)
   │       Jsoup-based helper that supplies default *Parse() implementations
   │       driven by abstract selector methods.
   │
   ├── ResolvableAnimeSource (interface)      (animesource/online/ResolvableAnimeSource.kt)
   │       Sources that can resolve a deep-link URL back to an anime/episode
   │       (getAnimeUrl(getAnimeSourceData), etc.).
   │
   ├── ConfigurableAnimeSource (interface)    (animesource/ConfigurableAnimeSource.kt)
   │       Sources that expose a preferences UI via setupPreferenceScreen().
   │
   ├── UnmeteredSource (interface)            (animesource/UnmeteredSource.kt)
   │       Marker for sources whose network usage is unmetered (e.g. local).
   │
   └── (factories)
       AnimeSourceFactory                     (animesource/AnimeSourceFactory.kt)
           interface { fun createSources(): List<AnimeSource> }
           — for extensions that ship multiple sources in one APK.
```

### 1c. `MangaSource` (for contrast — to be removed in anime-only fork)

`source-api/src/commonMain/kotlin/eu/kanade/tachiyomi/source/MangaSource.kt`:

```kotlin
interface MangaSource {
    val id: Long
    val name: String
    val lang: String get() = ""

    suspend fun getMangaDetails(manga: SManga): SManga
    suspend fun getChapterList(manga: SManga): List<SChapter>
    suspend fun getPageList(chapter: SChapter): List<Page>

    // + deprecated Rx fetchMangaDetails / fetchChapterList / fetchPageList
}
```

Parallel `CatalogueSource` / `HttpSource` / `ParsedHttpSource` /
`SourceFactory` / `ConfigurableSource` / `UnmeteredSource` /
`ResolvableSource` mirror the anime side under `source/.../`. The model classes
differ accordingly:

| Anime | Manga |
|---|---|
| `SAnime` | `SManga` |
| `SEpisode` | `SChapter` |
| `Video` | `Page` |
| `Hoster` | (none) |
| `AnimesPage` | `MangasPage` |
| `AnimeFilterList` | `FilterList` |
| `TimeStamp` / `ChapterType` | (none) |
| `Track` (sub/audio) | (none) |

---

## 2. How extensions are loaded at runtime

### 2a. Class loader — `ChildFirstPathClassLoader` (a `PathClassLoader` subclass)

The task brief asks to confirm `DexClassLoader` usage. Strictly speaking the
loader used is **`dalvik.system.PathClassLoader`** (which itself extends
`DexClassLoader`), wrapped in a custom parent-last class loader:

`app/src/main/java/eu/kanade/tachiyomi/util/system/ChildFirstPathClassLoader.kt`:

```kotlin
class ChildFirstPathClassLoader(
    dexPath: String,
    librarySearchPath: String?,
    parent: ClassLoader,
) : PathClassLoader(dexPath, librarySearchPath, parent) {

    override fun loadClass(name: String?, resolve: Boolean): Class<*> {
        var c = findLoadedClass(name)
        if (c == null && systemClassLoader != null) {
            try { c = systemClassLoader.loadClass(name) } catch (_: ClassNotFoundException) {}
        }
        if (c == null) {
            c = try { findClass(name) }
                     catch (_: ClassNotFoundException) { super.loadClass(name, resolve) }
        }
        if (resolve) resolveClass(c)
        return c
    }
    // … similarly parent-last for getResource / getResources / getResourceAsStream
}
```

The parent-last ordering is critical: extension APKs ship their own copy of
common dependencies (OkHttp, Jsoup, etc.) and we want those to win over the
app's, so that an extension built against an older lib still loads.

### 2b. Loader flow — `AnimeExtensionLoader`

`app/src/main/java/eu/kanade/tachiyomi/extension/anime/util/AnimeExtensionLoader.kt`:

1. **Discovery** (`loadExtensions(context)`):
   - Query `PackageManager.getInstalledPackages(...)` for shared (system
     installed) extensions.
   - Scan `context.filesDir/exts/*.ext` for *private* (side-loaded) extensions
     (Android 14+ requires these be read-only; the loader calls
     `setReadOnly()` defensively).
   - For each candidate, `isPackageAnExtension(pkgInfo)` checks
     `pkgInfo.reqFeatures` for the feature string `"tachiyomi.animeextension"`.
   - De-duplicate by package name (shared wins by default; private wins if its
     `versionCode` is higher — see `selectExtensionPackage`).
   - Load each extension concurrently via `runBlocking { … async { loadExtension } … }`.

2. **Per-extension load** (`loadExtension(context, extensionInfo)`):
   - Read `versionName`, parse the lib version (the part before the last `.`).
     Reject if outside `[LIB_VERSION_MIN=12, LIB_VERSION_MAX=16]`.
   - Compute SHA-256 of each signing certificate and check
     `TrustAnimeExtension.isTrusted(pkgInfo, signatures)`. Untrusted → returns
     `AnimeLoadResult.Untrusted` (the UI prompts the user to trust it; on
     approval the signature is persisted and the extension is reloaded).
   - Honour NSFW filtering (`METADATA_NSFW == 1` and `preferences.showNsfwSource()` is off).
   - **Create the class loader**:
     ```kotlin
     val classLoader = ChildFirstPathClassLoader(appInfo.sourceDir, null, context.classLoader)
     ```
     On `LinkageError` it falls back to a plain `dalvik.system.PathClassLoader`.
   - **Instantiate the source class(es)** from the manifest metadata
     `tachiyomi.animeextension.class` (semicolon-separated list of fully-qualified
     class names; leading `.` is prefixed with the extension's package name):
     ```kotlin
     when (val obj = Class.forName(it, false, classLoader)
                              .getDeclaredConstructor().newInstance()) {
         is AnimeSource       -> listOf(obj)
         is AnimeSourceFactory -> obj.createSources()
         else -> throw Exception("Unknown source class type: ${obj.javaClass}")
     }
     ```
   - Wrap the result in `AnimeExtension.Installed(name, pkgName, versionName,
     versionCode, libVersion, lang, isNsfw, sources, pkgFactory, icon, isShared)`.

### 2c. Extension manifest contract (what an extension APK must declare)

From `AnimeExtensionLoader` constants:

| Manifest item | Value | Purpose |
|---|---|---|
| `<uses-feature android:name="tachiyomi.animeextension">` | (feature flag) | Marks the APK as an anime extension |
| `<meta-data android:name="tachiyomi.animeextension.class">` | `com.example.MySource` or `com.example.MyFactory;com.example.OtherSource` | Entry-point class(es) implementing `AnimeSource` or `AnimeSourceFactory` |
| `<meta-data android:name="tachiyomi.animeextension.factory">` | (string) | Optional factory id |
| `<meta-data android:name="tachiyomi.animeextension.nsfw">` | `0`/`1` | NSFW flag |
| `<meta-data android:name="tachiyomi.animeextension.hasReadme">` | `0`/`1` | Show "Readme" button |
| `<meta-data android:name="tachiyomi.animeextension.hasChangelog">` | `0`/`1` | Show "Changelog" button |
| `application android:label="@string/extension_name">` | `Aniyomi: <Name>` | The loader strips the `"Aniyomi: "` prefix to get `extName` |
| `versionName` | `"<libVersion>.<patch>.<build>"` (e.g. `16.0.1`) | lib version is parsed from the prefix before the last `.` |
| `versionCode` | long | Used for update detection |
| APK signing | must be SHA-256-trusted | Verified against `TrustAnimeExtension` |

**Package naming convention**: extension packages are named
`eu.kanade.tachiyomi.animeextension.<lang>.<name>`. This is confirmed by
`AnimeExtensionDetailsScreen.kt` which strips the prefix
`"eu.kanade.tachiyomi.animeextension."` from the package name for display, and
by the `Jellyfin.kt` tracker which explicitly matches the source class
`"eu.kanade.tachiyomi.animeextension.all.jellyfin.Jellyfin"`.

So the **entry-point class name an extension must implement** is any class that
implements `eu.kanade.tachiyomi.animesource.AnimeSource` (directly, or via
`AnimeHttpSource` / `ParsedAnimeHttpSource`) **or**
`eu.kanade.tachiyomi.animesource.AnimeSourceFactory` (which returns a
`List<AnimeSource>` from `createSources()`). The class is declared via the
`tachiyomi.animeextension.class` meta-data in the extension's
`AndroidManifest.xml`.

---

## 3. Extension index / repo URL

### 3a. No hard-coded default repo

A grep for `raw.githubusercontent`, `aniyomi-extensions`, `defaultRepo`,
`DEFAULT_REPO`, `seedRepo`, etc. across the Kotlin sources finds **no
hard-coded default extension-repo URL** in this fork. Repos are entirely
user-managed:

- Stored in the SQLDelight table `extension_repos` (anime:
  `data/src/main/sqldelightanime/dataanime/extension_repos.sq`; manga:
  `data/src/main/sqldelight/data/extension_repos.sq`).
- Added via the **Settings → Extensions → Add repo** UI, or via deep links
  `aniyomi://add-repo` / `tachiyomi://add-repo` handled in
  `app/src/main/java/eu/kanade/tachiyomi/ui/main/MainActivity.kt` (lines 543,
  549).
- One-time migration `ExternalRepoMigration.kt` (migration version 114f)
  rewrites legacy user-configured repos to the
  `https://raw.githubusercontent.com/<owner>/<repo>/repo` shape, but only for
  repos the user had already added.

> The upstream Aniyomi default repo
> (`https://raw.githubusercontent.com/aniyomiorg/aniyomi-extensions/repo`) is
> **not** baked into this fork's source. If a default is desired for a
> derived anime-only fork, it must be added explicitly (e.g. seeded into
> `extension_repos` on first run or via a `Preference` default).

### 3b. Repo URL shape and validation

`domain/src/main/java/mihon/domain/extensionrepo/anime/interactor/CreateAnimeExtensionRepo.kt`:

```kotlin
private val repoRegex = """^https://.*/index\.min\.json$""".toRegex()

suspend fun await(indexUrl: String): Result {
    val formattedIndexUrl = indexUrl.toHttpUrlOrNull()?.toString()
        ?.takeIf { it.matches(repoRegex) }
        ?: return Result.InvalidUrl

    val baseUrl = formattedIndexUrl.removeSuffix("/index.min.json")
    return service.fetchRepoDetails(baseUrl)?.let { insert(it) } ?: Result.InvalidUrl
}
```

So a repo is identified by a URL of the form
`https://<host>/<path>/index.min.json`. The `baseUrl` is everything except the
trailing `/index.min.json`.

### 3c. Network endpoints used

`app/src/main/java/eu/kanade/tachiyomi/extension/anime/api/AnimeExtensionApi.kt`:

| Endpoint | Purpose |
|---|---|
| `GET {baseUrl}/index.min.json` | Fetch the list of available extensions (parsed as `List<AnimeExtensionJsonObject>`) |
| `GET {baseUrl}/repo.json` | Fetch repo metadata (`ExtensionRepoMetaDto`) — see `ExtensionRepoService.kt` |
| `GET {baseUrl}/icon/{pkg}.png` | Per-extension icon URL (constructed client-side, not fetched by the API) |
| `GET {baseUrl}/apk/{apkName}` | Per-extension APK download URL (`AnimeExtensionApi.getApkUrl`) |

`domain/src/main/java/mihon/domain/extensionrepo/service/ExtensionRepoService.kt`:

```kotlin
client.newCall(GET("$repo/repo.json")) …
```

### 3d. Index JSON schema (`index.min.json`)

Parsed by `AnimeExtensionApi`:

```kotlin
@Serializable
private data class AnimeExtensionJsonObject(
    val name: String,            // "Aniyomi: <Name>"
    val pkg:  String,            // package name, e.g. eu.kanade.tachiyomi.animeextension.en.gogoanime
    val apk:  String,            // APK filename, e.g. "en-gogoanime-v16.0.1.apk"
    val lang: String,            // ISO 639-1 (or "all")
    val code: Long,              // versionCode
    val version: String,         // "<libVersion>.<patch>" e.g. "16.0.1"
    val nsfw: Int,               // 0 or 1
    val sources: List<AnimeExtensionSourceJsonObject>?,  // nullable — present for stub metadata
)

@Serializable
private data class AnimeExtensionSourceJsonObject(
    val id: Long,                // source id (MD5-derived, see AnimeHttpSource.generateId)
    val lang: String,
    val name: String,
    val baseUrl: String,
)
```

So `index.min.json` is a JSON array of `AnimeExtensionJsonObject`. Each entry
maps to an `AnimeExtension.Available` via `toExtensions(repoUrl)`:

```kotlin
AnimeExtension.Available(
    name        = it.name.substringAfter("Aniyomi: "),
    pkgName     = it.pkg,
    versionName = it.version,
    versionCode = it.code,
    libVersion  = it.extractLibVersion(),         // version.substringBeforeLast('.').toDouble()
    lang        = it.lang,
    isNsfw      = it.nsfw == 1,
    sources     = it.sources?.map(extensionAnimeSourceMapper).orEmpty(),
    apkName     = it.apk,
    iconUrl     = "$repoUrl/icon/${it.pkg}.png",
    repoUrl     = repoUrl,
)
```

### 3e. Repo metadata JSON schema (`repo.json`)

`domain/src/main/java/mihon/domain/extensionrepo/service/ExtensionRepoDto.kt`:

```kotlin
@Serializable
data class ExtensionRepoMetaDto(
    val meta: ExtensionRepoDto,
)

@Serializable
data class ExtensionRepoDto(
    val name: String,
    val shortName: String?,
    val website: String,
    val signingKeyFingerprint: String,
)
```

Parsed into the domain model
`domain/src/main/java/mihon/domain/extensionrepo/model/ExtensionRepo.kt`:

```kotlin
data class ExtensionRepo(
    val baseUrl: String,
    val name: String,
    val shortName: String?,
    val website: String,
    val signingKeyFingerprint: String,
)
```

---

## 4. Manager singletons — discovery / registration

### 4a. `AnimeExtensionManager`

`app/src/main/java/eu/kanade/tachiyomi/extension/anime/AnimeExtensionManager.kt`:

- Singleton (Injekt-injected).
- On construction calls `AnimeExtensionLoader.loadExtensions(context)` and
  populates three `MutableStateFlow<Map<String, AnimeExtension.*>>`:
  - `installedExtensionsMapFlow` — `AnimeExtension.Installed` keyed by package
    name. Exposed as `installedExtensionsFlow: StateFlow<List<AnimeExtension.Installed>>`.
  - `availableExtensionsMapFlow` — `AnimeExtension.Available` (from the repo
    index). Exposed as `availableExtensionsFlow`.
  - `untrustedExtensionsMapFlow` — `AnimeExtension.Untrusted`.
- Registers an `AnimeExtensionInstallReceiver` (listens for
  `PACKAGE_ADDED`/`REPLACED`/`REMOVED` broadcasts) wired to
  `AnimeInstallationListener` which keeps the maps in sync.
- `findAvailableExtensions()` → calls `AnimeExtensionApi.findExtensions()`
  (aggregates across all configured repos) and updates
  `availableExtensionsMapFlow`.
- `installExtension(ext)` / `updateExtension(ext)` / `uninstallExtension(ext)`
  delegate to `AnimeExtensionInstaller` (which dispatches to either
  `PackageInstallerInstallerAnime` or `ShizukuInstallerAnime`).
- `trust(ext)` for untrusted extensions: persists the signature via
  `TrustAnimeExtension.trust(...)`, then re-loads via
  `AnimeExtensionLoader.loadExtensionFromPkgName(...)`.

### 4b. `AndroidAnimeSourceManager` (implements `AnimeSourceManager`)

`app/src/main/java/eu/kanade/tachiyomi/source/anime/AndroidAnimeSourceManager.kt`:

- Singleton (Injekt-injected). Holds
  `sourcesMapFlow: MutableStateFlow<ConcurrentHashMap<Long, AnimeSource>>`.
- On init, subscribes to `AnimeExtensionManager.installedExtensionsFlow`. Each
  time installed extensions change, it rebuilds the map:
  - Seeds `LocalAnimeSource.ID → LocalAnimeSource`.
  - For each extension, for each `AnimeSource` it produces, stores
    `mutableMap[source.id] = source` and upserts a `StubAnimeSource` into the
    DB (so the source remains known even if the extension is later uninstalled).
- Lookup API: `get(sourceKey)`, `getOrStub(sourceKey)`,
  `getOnlineSources()`, `getCatalogueSources()`, `getStubSources()`,
  `catalogueSources: Flow<List<AnimeCatalogueSource>>`.
- The domain interface `AnimeSourceManager` is at
  `domain/src/main/java/tachiyomi/domain/source/anime/service/AnimeSourceManager.kt`.

This is the layer the player ultimately calls: `PlayerViewModel` does
`sourceManager.getOrStub(anime.source)` to get the `AnimeSource` it then invokes
`getHosterList` / `getVideoList` on.

### 4c. Manga side (parallel, for reference — to be removed)

- `app/src/main/java/eu/kanade/tachiyomi/extension/manga/MangaExtensionManager.kt`
- `app/src/main/java/eu/kanade/tachiyomi/extension/manga/util/MangaExtensionLoader.kt`
- `app/src/main/java/eu/kanade/tachiyomi/extension/manga/api/MangaExtensionApi.kt`
- `app/src/main/java/eu/kanade/tachiyomi/source/manga/AndroidMangaSourceManager.kt`

The manga loader uses the same `ChildFirstPathClassLoader`, but checks the
feature flag `tachiyomi.extension` (without the `anime` prefix) and metadata
`tachiyomi.extension.class`. Manga extension packages follow the convention
`eu.kanade.tachiyomi.extension.<lang>.<name>`.

---

## 5. Summary cheatsheet

| Question | Answer |
|---|---|
| Where is `AnimeSource` defined? | `source-api/src/commonMain/kotlin/eu/kanade/tachiyomi/animesource/AnimeSource.kt` |
| Key anime source methods | `getAnimeDetails`, `getEpisodeList`, `getSeasonList`, `getHosterList(episode)`, `getVideoList(hoster)` (new), `getVideoList(episode)` (legacy) |
| Concrete HTTP base class | `animesource/online/AnimeHttpSource.kt` (abstract) → `ParsedAnimeHttpSource.kt` (Jsoup helper) |
| Multi-source entry point | `animesource/AnimeSourceFactory.kt` — `fun createSources(): List<AnimeSource>` |
| Extension class loader | `ChildFirstPathClassLoader extends PathClassLoader` (parent-last), at `app/src/main/java/eu/kanade/tachiyomi/util/system/ChildFirstPathClassLoader.kt`; fallback to plain `dalvik.system.PathClassLoader` |
| Extension loader | `app/src/main/java/eu/kanade/tachiyomi/extension/anime/util/AnimeExtensionLoader.kt` |
| Extension manifest feature flag | `tachiyomi.animeextension` |
| Extension manifest class metadata | `tachiyomi.animeextension.class` (semicolon list of `AnimeSource` / `AnimeSourceFactory` FQNs) |
| Extension package convention | `eu.kanade.tachiyomi.animeextension.<lang>.<name>` |
| Extension lib version range | `12 … 16` (`AnimeExtensionLoader.LIB_VERSION_MIN`/`MAX`) |
| Extension manager singleton | `app/src/main/java/eu/kanade/tachiyomi/extension/anime/AnimeExtensionManager.kt` |
| Source manager singleton | `app/src/main/java/eu/kanade/tachiyomi/source/anime/AndroidAnimeSourceManager.kt` |
| Extension repo index URL | `<baseUrl>/index.min.json` (must match `^https://.*/index\.min\.json$`) |
| Repo metadata URL | `<baseUrl>/repo.json` |
| APK download URL | `<baseUrl>/apk/<apkName>` |
| Icon URL | `<baseUrl>/icon/<pkg>.png` |
| Index JSON schema | `List<{name, pkg, apk, lang, code: Long, version, nsfw: Int, sources: List<{id, lang, name, baseUrl}>?}>` |
| Repo meta JSON schema | `{meta: {name, shortName?, website, signingKeyFingerprint}}` |
| Default repo URL hard-coded? | **No.** Repos are user-added (UI or `aniyomi://add-repo` deep link). |
