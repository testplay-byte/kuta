# 04 — Data Flow: Extension → Source → Anime → Episode → Player

The end-to-end pipeline that turns "user taps an episode in the list" into
"MPV opens a video stream". Each stage carries a typed data model from the
`:source-api` module — `SAnime` → `SEpisode` → `Hoster` → `Video` — and each
arrow is a real method call on the `AnimeSource` interface (or its concrete
`AnimeHttpSource` / `ParsedAnimeHttpSource` subclass). The pipeline has two
forks: the **new ext-lib 16 hoster API** (`getHosterList(episode)` →
`getVideoList(hoster)`) and the **legacy ext-lib 1.5 flat API**
(`getVideoList(episode)`, wrapped into a single pseudo-hoster by
`List<Video>.toHosterList()`). Both terminate in `HosterLoader.getBestVideo`
→ `source.resolveVideo(video)` → `PlayerActivity.setVideo` →
`MPVLib.command(loadfile)`.

```mermaid
graph TD
    classDef user fill:#fff7e6,stroke:#aa8800,color:#000
    classDef ui fill:#e8f0ff,stroke:#3b6fb5,color:#000
    classDef loader fill:#eafaf0,stroke:#2e8b57,color:#000
    classDef source fill:#f3eaff,stroke:#7a3fb5,color:#000
    classDef model fill:#fff4e0,stroke:#cc8a00,color:#000
    classDef player fill:#ffe6e6,stroke:#cc3333,color:#000

    %% ---------- Discovery / browse ----------
    User([User]):::user
    BrowseScreen["BrowseAnimeSourceScreen"]:::ui
    ExtensionLoader["AnimeExtensionLoader<br/>(PackageManager + filesDir/exts)"]:::loader
    SourceManager["AndroidAnimeSourceManager<br/>ConcurrentHashMap<Long, AnimeSource>"]:::loader
    Source["AnimeSource<br/>(AnimeHttpSource / ParsedAnimeHttpSource)"]:::source

    User -- "taps source in Browse tab" --> BrowseScreen
    ExtensionLoader -- "loadExtensions(context)<br/>ChildFirstPathClassLoader → instantiate" --> SourceManager
    SourceManager -- "getOrStub(sourceId)" --> Source
    BrowseScreen -- "source.getSearchAnime(page, query, filters)" --> Source

    SAnime["<b>SAnime</b><br/>url, title, description,<br/>genre, status, thumbnail_url,<br/>initialized"]:::model
    Source -- "returns List<SAnime> wrapped in AnimesPage" --> SAnime

    %% ---------- Anime detail ----------
    AnimeScreen["AnimeScreen(animeId)"]:::ui
    AnimeScreenModel["AnimeScreenModel"]:::ui

    User -- "taps anime cover" --> AnimeScreen
    AnimeScreen -- "rememberScreenModel" --> AnimeScreenModel
    AnimeScreenModel -- "source.getAnimeDetails(SAnime)" --> Source
    Source -- "enriched SAnime (description, genre, …)" --> SAnime
    AnimeScreenModel -- "source.getEpisodeList(SAnime)" --> Source

    SEpisode["<b>SEpisode</b><br/>url, name, episode_number,<br/>date_upload, scanlator"]:::model
    Source -- "returns List<SEpisode>" --> SEpisode

    %% ---------- Episode tap → player launch ----------
    PlayerActivity["PlayerActivity<br/>(BaseActivity, not Voyager Screen)<br/>Intent extras: animeId, episodeId,<br/>hostList?, hostIndex?, vidIndex?"]:::player
    MainActivity["MainActivity.startPlayerActivity(...)"]:::ui
    PlayerViewModel["PlayerViewModel.init(animeId, episodeId, …)"]:::player

    User -- "taps episode row" --> MainActivity
    MainActivity -- "startActivity(PlayerActivity.newIntent)" --> PlayerActivity
    PlayerActivity -- "onNewIntent → viewModel.init" --> PlayerViewModel

    %% ---------- EpisodeLoader ----------
    EpisodeLoader["EpisodeLoader.getHosters(episode, anime, source)"]:::loader
    PlayerViewModel -- "if hostList blank:<br/>re-fetch hosters" --> EpisodeLoader

    EpisodeLoader -- "branch: isDownloaded?" --> DownloadsBranch{"Downloaded?"}
    DownloadsBranch -- "yes → AnimeDownloadManager.buildVideo<br/>(local content:// URI)" --> PlayerActivity
    DownloadsBranch -- "no → source is AnimeHttpSource?<br/>source is LocalAnimeSource?" --> HttpBranch{"Source type"}

    Hoster["<b>Hoster</b><br/>name, url, videoPrefix,<br/>videos: List<Video>?,<br/>sortable via sortHosters()"]:::model

    HttpBranch -- "AnimeHttpSource + ext-lib 16<br/>checkHasHosters(source) == true" --> NewApi["source.getHosterList(SEpisode)"]
    HttpBranch -- "AnimeHttpSource + legacy<br/>checkHasHosters(source) == false" --> LegacyApi["source.getVideoList(SEpisode)<br/>→ wrapped via .toHosterList()"]
    HttpBranch -- "LocalAnimeSource" --> LocalApi["getHostersOnLocal(episode)"]

    NewApi:::source -- "List<Hoster>" --> Hoster
    LegacyApi:::source -- "single pseudo-hoster<br/>containing all Videos" --> Hoster
    LocalApi:::source --> Hoster

    %% ---------- HosterLoader ----------
    HosterLoader["HosterLoader.getBestVideo(source, hosterList)"]:::loader
    PlayerViewModel -- "currentHosterList" --> HosterLoader

    HosterLoader -- "EpisodeLoader.loadHosterVideos(source, hoster)" --> LoadVideos["source.getVideoList(hoster)"]
    LoadVideos:::source -- "List<Video>" --> Video

    Video["<b>Video</b><br/>videoUrl, videoTitle, resolution,<br/>headers, preferred,<br/>subtitleTracks, audioTracks,<br/>timestamps (ChapterType),<br/>mpvArgs / ffmpeg*Args,<br/>initialized, State"]:::model

    HosterLoader -- "selectBestVideo:<br/>preferred first, else first non-empty url" --> Video
    HosterLoader -- "getResolvedVideo:<br/>source.resolveVideo(video) (AnimeHttpSource)" --> ResolveVideo["AnimeHttpSource.resolveVideo(video)"]
    ResolveVideo:::source -- "Video with initialized=true<br/>and resolved videoUrl" --> Video

    %% ---------- Player handoff ----------
    SetVideo["PlayerActivity.setVideo(video, position)"]:::player
    MPVLib["MPVLib.command(['loadfile', url, 'replace', '0', opts])<br/>+ setOptionString('http-header-fields', headers)<br/>+ command(['set','start', resumePos])"]:::player
    Playback([libmpv opens stream → playback]):::player

    PlayerViewModel -- "currentVideo" --> SetVideo
    SetVideo -- "setHttpOptions(video)" --> MPVLib
    SetVideo -- "MPVLib.command(loadfile, …)" --> MPVLib
    MPVLib --> Playback
```

## Notes

- **Two APIs, one pipeline.** The `AnimeSource` interface declares both
  `getHosterList(episode)` (ext-lib 16, the new hoster abstraction) and
  `getVideoList(episode)` (ext-lib 1.5, the legacy flat list).
  `EpisodeLoader.getHostersOnHttp` calls `checkHasHosters(source)` to decide
  which API to use; if the source only implements the legacy API, the result
  is wrapped into a single pseudo-hoster via `List<Video>.toHosterList()` so
  the rest of the pipeline is identical.
- **Downloaded-episode short-circuit.** If `EpisodeLoader.isDownload(episode,
  anime)` returns true (file exists on disk via `AnimeDownloadProvider`),
  the player gets a `Video` whose `videoUrl` is a local `content://` URI
  produced by `AnimeDownloadManager.buildVideo(...)`. No network call, no
  hoster fetch — MPV just `loadfile`s the local URI.
- **Intent-only handoff.** `AnimeScreen.openEpisode` →
  `MainActivity.startPlayerActivity` → `PlayerActivity.newIntent`. Only
  `animeId` and `episodeId` are required; the full `Video` list is *not*
  passed when launching from the episode list (the player re-fetches hosters
  / videos at runtime). `hostList` *can* be supplied as a JSON-serialized
  `List<Hoster>` (see `SerializableHoster.serialize()`) for the case where
  the user pre-picked a hoster/video from a dialog.
- **`resolveVideo` is lazy.** A `Video` from `getVideoList(hoster)` may have
  `initialized = false` and a `videoUrl` that is only a *page* URL, not a
  playable stream URL. `HosterLoader.getResolvedVideo` calls
  `AnimeHttpSource.resolveVideo(video)` (only for `AnimeHttpSource` + un-
  initialized videos) to resolve the page URL to the actual stream URL.
  The first video whose resolved `videoUrl` is non-empty short-circuits the
  search.
- **Resume position** is pushed to MPV via `MPVLib.command(["set", "start",
  "<seconds>"])` *before* `loadfile`. The position comes from
  `episode.last_second_seen` (unless the episode is marked seen and
  `preserveWatchingPosition()` is false, in which case it's 0).
- **HTTP headers** are pushed via `MPVLib.setOptionString("http-header-fields",
  ...)` from `video.headers ?: source.headers`. Subtitle/audio tracks from
  `video.subtitleTracks` / `video.audioTracks` are pushed via `sub-add` /
  `audio-add` after `loadfile`. Skip-intro timestamps from
  `video.timestamps` (typed by `ChapterType`) drive the auto-skip UX.
