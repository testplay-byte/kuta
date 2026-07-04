# 06 — Video Player Pipeline

> Investigation of the Kuta (Aniyomi fork) video player: the player library, how
> video URLs flow from the episode list into playback, the `Video` data model, and
> per-feature implementation paths.
>
> Source-state at time of writing: no source files were modified. All paths quoted
> are relative to the repo root (`/home/z/kuta`).

---

## 1. Player library — **MPV**, not Media3/ExoPlayer

**Surprise / important correction up front:** despite the task brief assuming
Media3/ExoPlayer, this fork uses **MPV** as its playback engine. There is **no**
`androidx.media3` / `exoplayer` dependency anywhere in the Gradle catalogs or the
app module's `build.gradle.kts`. The player is built on top of the
[`aniyomi-mpv-lib`](https://github.com/aniyomiorg/aniyomi-mpv-lib) JNA bindings to
`libmpv`, plus bundled FFmpeg shared libraries.

### Dependency declarations

`gradle/aniyomi.versions.toml`:

```toml
aniyomi-mpv-lib = "1.18.n"
aniyomi-mpv = { module = "com.github.aniyomiorg:aniyomi-mpv-lib", version.ref = "aniyomi-mpv-lib" }
```

`app/build.gradle.kts` (only player-related line):

```kotlin
// mpv-android
implementation(aniyomilibs.aniyomi.mpv)
```

`app/build.gradle.kts` (native libraries preserved by the packaging block — note
`libmpv`, `libplayer`, plus the entire FFmpeg suite):

```kotlin
jniLibs {
    keepDebugSymbols += listOf(
        "libmpv", "libplayer",
        "libavcodec", "libavdevice", "libavfilter", "libavformat",
        "libavutil", "libpostproc", "libswresample", "libswscale",
        "libffmpegkit", "libffmpegkit_abidetect",
        // ...
    ).map { "**/$it.so" }
}
```

So at runtime the app talks to `libmpv.so` via JNI through the `is.xyz.mpv.*`
classes shipped by `aniyomi-mpv-lib` (notably `MPVLib`, `BaseMPVView`,
`KeyMapping`).

### Why MPV?

The `PlayerActivity` header explicitly credits the design as *"a mix between
PlayerActivity from mpvKt and the former PlayerActivity from Aniyomi"* — mpvKt
(https://github.com/abdallahmehiz/mpvKt) being an MPV-based Android player. This
matters for any anime-only fork: MPV gives the app native support for soft-subs,
audio-track switching, hardware decoding, custom shaders, and Lua scripts — none
of which require Media3.

---

## 2. Player Activity / Screen / View

There is **no Compose-only player screen**. The player is a classic Android
`Activity` that hosts a custom `View` (the MPV surface) plus a Compose overlay
for controls.

| Component | File path |
|---|---|
| Activity | `app/src/main/java/eu/kanade/tachiyomi/ui/player/PlayerActivity.kt` (~1318 lines) |
| ViewModel | `app/src/main/java/eu/kanade/tachiyomi/ui/player/PlayerViewModel.kt` (~2060 lines) |
| MPV surface view | `app/src/main/java/eu/kanade/tachiyomi/ui/player/AniyomiMPVView.kt` (extends `is.xyz.mpv.BaseMPVView`) |
| MPV event observer | `app/src/main/java/eu/kanade/tachiyomi/ui/player/PlayerObserver.kt` (implements `MPVLib.EventObserver`, `MPVLib.LogObserver`) |
| Compose controls overlay | `app/src/main/java/eu/kanade/tachiyomi/ui/player/controls/PlayerControls.kt` (and siblings in `controls/`) |
| External-player fallback | `app/src/main/java/eu/kanade/tachiyomi/ui/player/ExternalIntents.kt` |
| Player preferences | `app/src/main/java/eu/kanade/tachiyomi/ui/player/settings/PlayerPreferences.kt`, `GesturePreferences.kt`, `SubtitlePreferences.kt`, `AudioPreferences.kt`, `DecoderPreferences.kt`, `AdvancedPlayerPreferences.kt` |
| Episode/hoster loaders | `app/src/main/java/eu/kanade/tachiyomi/ui/player/loader/EpisodeLoader.kt`, `HosterLoader.kt` |

`PlayerActivity` extends `BaseActivity` and uses view binding
(`PlayerLayoutBinding`). The XML layout `player_layout.xml` contains the
`<eu.kanade.tachiyomi.ui.player.AniyomiMPVView>` element; the controls are drawn
on top via Compose (`PlayerControls` composable).

---

## 3. Data flow: episode list → player launch → MPV `loadfile`

### 3a. Episode list → `PlayerActivity` Intent

The episode list lives in `AnimeScreen.kt` (and equivalent tabs for
history/updates/library). Tapping an episode ultimately calls
`MainActivity.startPlayerActivity(...)`:

`app/src/main/java/eu/kanade/tachiyomi/ui/entries/anime/AnimeScreen.kt`:

```kotlin
private suspend fun openEpisode(context: Context, episode: Episode, useExternalPlayer: Boolean) {
    withIOContext {
        MainActivity.startPlayerActivity(
            context,
            episode.animeId,
            episode.id,
            useExternalPlayer,
        )
    }
}
```

`app/src/main/java/eu/kanade/tachiyomi/ui/main/MainActivity.kt` (companion):

```kotlin
suspend fun startPlayerActivity(
    context: Context, animeId: Long?, episodeId: Long?,
    extPlayer: Boolean, video: Video? = null,
    hosterIndex: Int = -1, videoIndex: Int = -1,
    hosterList: List<Hoster>? = null,
) {
    if (extPlayer) {
        val intent = ExternalIntents.newIntent(context, animeId, episodeId, video)
        externalPlayerResult?.launch(intent) ?: return
    } else {
        context.startActivity(
            PlayerActivity.newIntent(context, animeId, episodeId, hosterList, hosterIndex, videoIndex),
        )
    }
}
```

The launch contract is therefore **Intent extras only** (no shared memory, no
URI). See `PlayerActivity.newIntent`:

`PlayerActivity.kt`:

```kotlin
companion object {
    fun newIntent(
        context: Context,
        animeId: Long?,
        episodeId: Long?,
        hostList: List<Hoster>? = null,
        hostIndex: Int? = null,
        vidIndex: Int? = null,
    ): Intent = Intent(context, PlayerActivity::class.java).apply {
        putExtra("animeId", animeId)
        putExtra("episodeId", episodeId)
        hostIndex?.let { putExtra("hostIndex", it) }
        vidIndex?.let { putExtra("vidIndex", it) }
        hostList?.let { putExtra("hostList", it.serialize()) }   // JSON-serialized
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}
```

Key points:

- Only `animeId` and `episodeId` are required. The full `Video` list is **not**
  passed when launching from the episode list — the player re-fetches hosters /
  videos from the source at runtime (see 3b).
- `hostList` *can* be supplied as a JSON-serialized
  `List<Hoster>` (see `SerializableHoster.serialize()` in
  `source-api/.../animesource/model/Hoster.kt`). This is used when the host list
  was already fetched (e.g. the user picked a specific hoster/video from a
  dialog) or for cross-process handoff.
- `hostIndex` / `vidIndex` preselect a specific (hoster, video) pair.

### 3b. `PlayerViewModel.init()` — fetch anime, source, hosters

`PlayerActivity.onNewIntent` extracts the extras and calls
`viewModel.init(animeId, episodeId, hostList, hostIndex, vidIndex)`.

`PlayerViewModel.kt` (abridged):

```kotlin
suspend fun init(animeId, initialEpisodeId, hostList: String, hostIndex, vidIndex)
    : Pair<InitResult, Result<Boolean>> {

    val anime = getAnime.await(animeId)                 // DB
    val source = sourceManager.getOrStub(anime.source)  // AnimeSourceManager

    updateEpisodeList(initEpisodeList(anime))           // sorted, filtered
    val episode = currentPlaylist.value.first { it.id == episodeId }

    // Push metadata into mpv user-data
    MPVLib.setPropertyString("user-data/current-anime/anime-title", anime.title)
    MPVLib.setPropertyInt("user-data/current-anime/intro-length", getAnimeSkipIntroLength())

    if (hostList.isNotBlank()) {
        currentHosterList = hostList.toHosterList()
        qualityIndex = Pair(hostIndex, vidIndex)
    } else {
        // Re-fetch hosters from the live Source via the EpisodeLoader
        currentHosterList = EpisodeLoader.getHosters(
            currentEp.toDomainEpisode()!!, anime, source,
        ).takeIf { it.isNotEmpty() } ?: throw ExceptionWithStringResource(...)
    }

    return Pair(InitResult(currentHosterList, qualityIndex, position = episodePosition),
                Result.success(true))
}
```

### 3c. `EpisodeLoader` — the bridge to the `Source` interface

`app/src/main/java/eu/kanade/tachiyomi/ui/player/loader/EpisodeLoader.kt`:

```kotlin
suspend fun getHosters(episode: Episode, anime: Anime, source: AnimeSource): List<Hoster> {
    val isDownloaded = isDownload(episode, anime)
    return when {
        isDownloaded                -> getHostersOnDownloaded(episode, anime, source)
        source is AnimeHttpSource   -> getHostersOnHttp(episode, source)
        source is LocalAnimeSource  -> getHostersOnLocal(episode)
        else -> error("source not supported")
    }
}

// Online path supports BOTH the new (ext-lib 16) hoster API and the legacy
// (ext-lib 1.5) flat getVideoList(episode) API:
private suspend fun getHostersOnHttp(episode: Episode, source: AnimeHttpSource): List<Hoster> =
    if (checkHasHosters(source)) {
        source.getHosterList(episode.toSEpisode())
            .let { source.run { it.sortHosters() } }
    } else {
        source.getVideoList(episode.toSEpisode())
            .let { source.run { it.sortVideos() } }
            .toHosterList()     // wrapped as a single pseudo-hoster
    }
```

So the **exact methods on `Source` (anime side) the player consumes are**:

- `AnimeSource.getHosterList(episode: SEpisode): List<Hoster>` — new API,
  ext-lib 16
- `AnimeSource.getVideoList(hoster: Hoster): List<Video>` — new API, ext-lib 16
  (called from `EpisodeLoader.getVideosOnHttp`)
- `AnimeSource.getVideoList(episode: SEpisode): List<Video>` — legacy API,
  ext-lib 1.5, wrapped via `List<Video>.toHosterList()`
- `AnimeHttpSource.resolveVideo(video: Video): Video?` — lazy URL resolution

### 3d. `HosterLoader.getBestVideo()` — picks a concrete `Video`

`app/src/main/java/eu/kanade/tachiyomi/ui/player/loader/HosterLoader.kt`:

1. For each `Hoster`, `EpisodeLoader.loadHosterVideos(source, hoster)` calls
   `source.getVideoList(hoster)` and produces a `HosterState.Ready` containing
   the `List<Video>` plus a parallel `List<Video.State>` (QUEUE/READY/…).
2. `selectBestVideo()` prefers the first `Video` with `preferred = true`,
   otherwise the first video whose `videoUrl` is non-empty.
3. For each candidate, `getResolvedVideo(source, video)` calls
   `source.resolveVideo(video)` (only for `AnimeHttpSource` + un-initialized
   videos) and returns the resolved `Video` with `initialized = true`.
4. The first video whose resolved `videoUrl` is non-empty short-circuits the
   search.

### 3e. `PlayerActivity.setVideo()` — hand the URL to MPV

Once `PlayerViewModel` has a `currentVideo`, the activity issues the MPV
`loadfile` command:

`PlayerActivity.kt`:

```kotlin
fun setVideo(video: Video?, position: Long? = null) {
    if (player.isExiting || video == null) return

    setHttpOptions(video)     // push headers into MPV's http-header-fields option

    // Resume position handling
    if (viewModel.isLoadingEpisode.value) {
        viewModel.currentEpisode.value?.let { episode ->
            val preservePos = playerPreferences.preserveWatchingPosition().get()
            val resumePosition = position
                ?: if (episode.seen && !preservePos) 0L else episode.last_second_seen
            MPVLib.command(arrayOf("set", "start", "${resumePosition / 1000F}"))
        }
    } else {
        player.timePos?.let { MPVLib.command(arrayOf("set", "start", "${player.timePos}")) }
    }

    val videoOptions = video.mpvArgs.joinToString(",") { (k, v) -> "$k=\"$v\"" }

    MPVLib.command(arrayOf(
        "loadfile",
        parseVideoUrl(video.videoUrl),   // resolves content:// URIs etc.
        "replace",
        "0",
        videoOptions,
    ))
}
```

`setHttpOptions(video)` (`PlayerActivity.kt:1108`) builds a comma-separated
header string from `video.headers ?: source.headers` and pushes it via:

```kotlin
MPVLib.setOptionString("http-header-fields", httpHeaderString)
```

This is the final handoff: MPV opens the URL itself with the supplied headers,
subtitle tracks, audio tracks, and chapters (see §5).

### 3f. End-to-end summary diagram

```
AnimeScreen.openEpisode(episode)
        │  (CoroutineIO)
        ▼
MainActivity.startPlayerActivity(context, animeId, episodeId, ...)
        │
        ▼
PlayerActivity.newIntent(animeId, episodeId, hostList?, hostIndex?, vidIndex?)
        │  Intent extras only
        ▼
PlayerActivity.onNewIntent  ──►  PlayerViewModel.init(...)
        │                                   │
        │                                   ├─ getAnime.await(animeId)            [DB]
        │                                   ├─ sourceManager.getOrStub(source)    [AnimeSourceManager]
        │                                   ├─ EpisodeLoader.getHosters(episode, anime, source)
        │                                   │       └─ AnimeSource.getHosterList(episode)   [ext-lib 16]
        │                                   │       └─ OR AnimeSource.getVideoList(episode) [legacy] + .toHosterList()
        │                                   └─ returns InitResult(hosterList, qualityIndex, position)
        ▼
HosterLoader.getBestVideo(source, hosterList)
        │
        ├─ EpisodeLoader.loadHosterVideos(source, hoster)
        │       └─ AnimeSource.getVideoList(hoster)        [ext-lib 16]
        ├─ HosterLoader.selectBestVideo(hosterStates)      [preferred first, else first non-empty url]
        └─ AnimeHttpSource.resolveVideo(video)             [lazy URL resolution]
        │
        ▼
PlayerActivity.setVideo(video, position)
        ├─ setHttpOptions(video)  ──► MPVLib.setOptionString("http-header-fields", …)
        ├─ MPVLib.command(arrayOf("set", "start", "<resumePos>"))
        └─ MPVLib.command(arrayOf("loadfile", url, "replace", "0", videoOptions))
                          ▲
                  libmpv opens the stream ──► PlayerObserver fires MPV events ──► UI updates
```

---

## 4. The `Video` model

Defined at `source-api/src/commonMain/kotlin/eu/kanade/tachiyomi/animesource/model/Video.kt`.

```kotlin
data class Video(
    var videoUrl: String = "",
    val videoTitle: String = "",
    val resolution: Int? = null,
    val bitrate: Int? = null,
    val headers: Headers? = null,
    val preferred: Boolean = false,
    val subtitleTracks: List<Track> = emptyList(),
    val audioTracks:   List<Track> = emptyList(),
    val timestamps:    List<TimeStamp> = emptyList(),
    val mpvArgs:           List<Pair<String, String>> = emptyList(),
    val ffmpegStreamArgs:  List<Pair<String, String>> = emptyList(),
    val ffmpegVideoArgs:   List<Pair<String, String>> = emptyList(),
    val internalData: String = "",
    val initialized: Boolean = false,
) {
    @Volatile var status: State = State.QUEUE          // QUEUE / LOAD_VIDEO / READY / ERROR

    // Back-compat shims for ext-lib < 1.6 (deprecated):
    @Deprecated("Use videoTitle instead") val quality: String get() = videoTitle
    @Deprecated("Use videoUrl instead")   val url:     String get() = videoPageUrl
    private var videoPageUrl: String = ""

    enum class State { QUEUE, LOAD_VIDEO, READY, ERROR }
}

@Serializable data class Track(val url: String, val lang: String)

@Serializable
data class TimeStamp(
    val start: Double, val end: Double, val name: String,
    val type: ChapterType = ChapterType.Other,
)

@Serializable
enum class ChapterType { Opening, Ending, Recap, MixedOp, Other }
```

Notes:

- `videoUrl` is the direct/playable stream URL (after `resolveVideo()`).
- `videoTitle` is the human-readable label shown in the quality picker (formerly
  `quality`).
- `preferred` is a hint from the source that this video should be picked first;
  `HosterLoader.selectBestVideo()` honours it.
- `subtitleTracks` / `audioTracks` are external sub/audio tracks the source
  discovered; the player pushes them into MPV via `audio-add` / `sub-add`
  (see §5).
- `timestamps` is the in-band chapter/skip-segment list, typed by `ChapterType`
  (`Opening`, `Ending`, `Recap`, `MixedOp`, `Other`) — this is what powers
  skip-intro/outro.
- `mpvArgs` / `ffmpegStreamArgs` / `ffmpegVideoArgs` allow the source to pass
  arbitrary decoder/ffmpeg options through to MPV.
- `SerializableVideo` (companion in the same file) provides JSON
  encode/decode helpers (`List<Video>.serialize()` /
  `String.toVideoList()`) used when crossing Intent/process boundaries.

The `Hoster` wrapper lives next to it at
`source-api/src/commonMain/kotlin/eu/kanade/tachiyomi/animesource/model/Hoster.kt`:

```kotlin
open class Hoster(
    val hosterUrl: String = "",
    val hosterName: String = "",
    val videoList: List<Video>? = null,   // null ⇒ lazy: fetch via source.getVideoList(hoster)
    val internalData: String = "",
    val lazy: Boolean = false,
)
```

---

## 5. Feature checklist

For each requested feature, the implementation location (or "absent in
upstream"). All paths under `app/src/main/java/eu/kanade/tachiyomi/ui/player/`.

### Quality (hoster / video) selection
- Bottom-sheet UI: `controls/components/sheets/QualitySheet.kt`
- Selection logic: `loader/HosterLoader.kt::selectBestVideo` (preferred-first,
  then first non-empty URL)
- Per-hoster expansion state in `PlayerViewModel` (`_hosterState`,
  `_hosterExpandedList`, `_selectedHosterVideoIndex`).
- `PlayerPreferences.kt`: `showFailedHosters()`, `showEmptyHosters()`.

### Subtitle toggle
- Loading tracks into MPV: `PlayerActivity.setupTracks()`:
  ```kotlin
  subtitleTracks?.forEach { sub ->
      executeMPVCommand(arrayOf("sub-add", sub.url, "auto", sub.lang))
  }
  audioTracks?.forEach { audio ->
      executeMPVCommand(arrayOf("audio-add", audio.url, "auto", audio.lang))
  }
  ```
- UI sheets: `controls/components/sheets/SubtitleTracksSheet.kt`,
  `AudioTracksSheet.kt`, `controls/components/panels/SubtitleSettingsPanel.kt`
  (typography / colors / miscellaneous cards),
  `SubtitleDelayPanel.kt`, `AudioDelayPanel.kt`.
- Preferences: `settings/SubtitlePreferences.kt`, `settings/AudioPreferences.kt`.
- Track model: `Video.subtitleTracks: List<Track>` / `Video.audioTracks`.

### Skip intro / outro
Two independent mechanisms:

1. **AniSkip** (online skip-times lookup): `utils/AniSkipApi.kt` — queries
   `https://api.aniskip.com/v2/skip-times/{malId}/{episodeNumber}?...` and
   returns `List<TimeStamp>` typed `Opening`/`Ending`/`Recap`/`MixedOp`.
   - Preferences: `PlayerPreferences.aniSkipEnabled()`,
     `enableSkipIntro()`, `autoSkipIntro()`,
     `enableNetflixStyleIntroSkip()`, `waitingTimeIntroSkip()`,
     `disableAniSkipOnChapters()`.
   - Per-anime override: `PlayerViewModel.getAnimeSkipIntroLength()` /
     `setAnimeSkipIntroLength()` (writes `anime.skipIntroLength` /
     `skipIntroDisable` viewer flags).
   - Default intro length pref: `GesturePreferences.defaultIntroLength()` (85s).
   - Skip button UI: `controls/BottomRightPlayerControls.kt` (`skipIntroButton`).
   - Auto/Netflix-style skip logic: `PlayerViewModel` around
     `autoSkip`/`netflixStyle` flags (`~line 1939–1961`).

2. **In-band chapter timestamps** supplied by the source via
   `Video.timestamps: List<TimeStamp>`: `PlayerActivity.setupChapters()`
   populates MPV chapters; the chapters sheet is
   `controls/components/sheets/ChaptersSheet.kt`, and the current-chapter widget
   is `controls/components/CurrentChapter.kt`.

### Resume playback
- Position source: `Episode.last_second_seen` (DB) plus
  `PlayerPreferences.preserveWatchingPosition()` to keep position on seen
  episodes.
- Handoff: `PlayerActivity.setVideo()` issues
  `MPVLib.command(arrayOf("set", "start", "${resumePosition / 1000F}"))` before
  `loadfile`.
- Save: `PlayerViewModel.saveCurrentEpisodeWatchingProgress()` writes the
  current MPV `time-pos` back to `EpisodeUpdate` / `AnimeHistoryUpdate`.
- Auto-mark-as-seen threshold: `PlayerPreferences.progressPreference()` (0.85
  default) — episodes past 85% are flagged seen.

### Gestures
- Preferences: `settings/GesturePreferences.kt`
  - `gestureVolumeBrightness()`, `swapVolumeBrightness()`
  - `gestureHorizontalSeek()`, `showSeekBar()`, `playerSmoothSeek()`,
    `skipLengthPreference()` (10s default)
  - `leftDoubleTapGesture()` / `centerDoubleTapGesture()` /
    `rightDoubleTapGesture()` — each an enum `SingleActionGesture` (Seek,
    PlayPause, Switch, …)
  - `mediaPreviousGesture()` / `mediaPlayPauseGesture()` / `mediaNextGesture()`
- Handler: `controls/GestureHandler.kt`
- Visual feedback: `controls/components/DoubleTapSeekTriangles.kt`,
  `controls/components/VerticalSliders.kt` (volume/brightness),
  `controls/components/BrightnessOverlay.kt`.

### Other player features worth noting (not asked, but relevant)
- **Picture-in-Picture**: `PlayerActivity` configures PiP params;
  `PlayerPreferences.enablePip()`, `pipOnExit()`, `pipEpisodeToasts()`,
  `pipReplaceWithPrevious()`; `PipActions.kt` handles PiP media buttons.
- **External player fallback**: `ExternalIntents.kt` builds an `Intent` for an
  external video player when `PlayerPreferences.alwaysUseExternalPlayer()` is
  set; `MainActivity.startPlayerActivity(extPlayer=true)` launches it via
  `externalPlayerResult`.
- **MediaSession**: `PlayerActivity` registers an `android.media.session.MediaSession`
  for lock-screen / Bluetooth controls.
- **Custom Lua scripts / shaders**: `PlayerActivity.copyUserFiles()` copies user
  Lua scripts from app storage into the MPV config dir; the bundled
  `assets/aniyomi.lua` is the JS/Lua bridge for custom buttons
  (`MPVLib.command(arrayOf("script-message", "call_button_$id"))`).
- **Custom on-screen buttons** (`CustomButton`): the Lua bridge dispatches
  `script-message call_button_<id>`; backed by `domain/.../custombutton/`.

---

## 6. Player ↔ Extension interaction summary

The player **never** calls into extension code directly. All calls flow through
the `AnimeSource` interface (see `07-extensions.md` for the full method list):

| Player-side callsite | Source-side method | Purpose |
|---|---|---|
| `EpisodeLoader.getHostersOnHttp` | `AnimeSource.getHosterList(episode: SEpisode): List<Hoster>` (ext-lib 16) | Get the list of hosters for an episode |
| `EpisodeLoader.getHostersOnHttp` (legacy) | `AnimeSource.getVideoList(episode: SEpisode): List<Video>` (ext-lib 1.5) | Legacy flat video list, wrapped as a single hoster |
| `EpisodeLoader.getVideosOnHttp` | `AnimeSource.getVideoList(hoster: Hoster): List<Video>` (ext-lib 16) | Get videos for a specific hoster |
| `HosterLoader.getResolvedVideo` | `AnimeHttpSource.resolveVideo(video: Video): Video?` | Resolve lazy placeholder URLs to playable URLs |
| `EpisodeLoader.parseVideoUrls` (legacy) | `AnimeHttpSource.getVideoUrl(video: Video): String` | Resolve a single video's URL (deprecated path) |
| Sorting hosters / videos | `AnimeHttpSource.List<Hoster>.sortHosters()` / `List<Video>.sortVideos()` | Source-defined quality ordering |

The `AnimeSource` instance itself comes from
`AndroidAnimeSourceManager` (`app/src/main/java/eu/kanade/tachiyomi/source/anime/AndroidAnimeSourceManager.kt`),
which holds a `ConcurrentHashMap<Long, AnimeSource>` populated by observing
`AnimeExtensionManager.installedExtensionsFlow` — i.e. the sources are loaded
from extension APKs by `AnimeExtensionLoader` (see `07-extensions.md`).
