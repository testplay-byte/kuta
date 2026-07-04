# 03 — Player UI, Gestures, Skip, Subtitles, Resume

> Feature inventory of the Kuta video player. The player is **MPV** (via the
> `aniyomi-mpv-lib` JNI bindings to `libmpv`), hosted in a classic Android
> `Activity` (`PlayerActivity`) with a Compose controls overlay. There is **no**
> Media3/ExoPlayer dependency. Source-state at time of writing: no source files
> were modified. Paths are relative to the repo root (`/home/z/kuta`).
>
> Full architecture details: `DOCS/architecture/06-player.md`.

---

## Feature 1 — Player Activity & MPV Engine Core

- **Description**: The full-screen video playback experience. Hosts the MPV
  surface (`AniyomiMPVView` extends `is.xyz.mpv.BaseMPVView`), drives playback
  through `MPVLib.command(loadfile …)`, observes MPV events, and overlays a
  Compose controls UI. Launched from the episode list via Intent extras only
  (`animeId`, `episodeId`, optional JSON-serialized `hostList`, `hostIndex`,
  `vidIndex`). Also owns Picture-in-Picture params, the Android `MediaSession`
  (lock-screen / Bluetooth controls), audio-focus handling, the noisy-audio
  receiver, and the external-player fallback path.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/ui/player/PlayerActivity.kt` (~1318 lines, the Activity)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/player/PlayerViewModel.kt` (~2060 lines, the ViewModel)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/player/AniyomiMPVView.kt` (MPV surface View)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/player/PlayerObserver.kt` (`MPVLib.EventObserver` / `LogObserver` bridge)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/player/PlayerEnums.kt` (`PlayerOrientation`, `VideoAspect`, `Debanding`, `SingleActionGesture`, `CustomKeyCodes`)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/player/PipActions.kt` (PiP remote-action constants + intent factory)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/player/ExternalIntents.kt` (external-player fallback; builds `Intent.ACTION_VIEW` for chosen `Video`)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/player/controls/PlayerControls.kt` + siblings in `controls/` (Compose overlay)
  - `app/src/main/res/layout/player_layout.xml` (View-binding root containing `<AniyomiMPVView>`)
  - Native libs: `libmpv.so`, `libplayer.so`, FFmpeg suite (see `app/build.gradle.kts` `jniLibs.keepDebugSymbols`)
  - Dep: `gradle/aniyomi.versions.toml` → `aniyomilibs.aniyomi.mpv` = `com.github.aniyomiorg:aniyomi-mpv-lib:1.18.n`
- **Status**: `modify` — engine stays MPV; the Compose controls overlay and the
  settings UI will be redesigned for the Kuta rebrand. The Intent-extras launch
  contract and the `PlayerViewModel.init()` hoster-fetch flow are stable.
- **Dependencies**:
  - Depends on: `AnimeSourceManager` + `AnimeSource.getHosterList/getVideoList/resolveVideo`
    (extensions, see `07-extensions.md`); `Episode.last_second_seen` (DB); `PlayerPreferences`
    + sibling *Preferences classes; `StorageManager` (for MPV config / fonts / scripts dirs);
    `AnimeDownloadManager.isEpisodeDownloaded` (downloaded-episode branch in `EpisodeLoader`).
  - Depended on by: `AnimeScreen.openEpisode`, `AnimeHistoryTab` "resume" tap,
    `AnimeUpdatesTab` "play" action, deep-link `DeepLinkAnimeActivity` → all
    funnel through `MainActivity.startPlayerActivity(...)`.
- **Notes**:
  - `PlayerActivity` is **not** a Voyager `Screen` — it is a standalone
    `BaseActivity` registered in `AndroidManifest.xml`. It is excluded from
    Voyager navigation entirely.
  - PiP (`enterPictureInPictureMode`, `createPipParams`, `onPictureInPictureModeChanged`,
    `PipActions` receiver) and `MediaSession` (`setupMediaSession`, `MediaSession.Callback`
    wired to `SingleActionGesture` enums for play/pause/prev/next) live inside this file.
    The `noisyReceiver` pauses playback on `ACTION_AUDIO_BECOMING_NOISY` (headphone unplug).
  - External-player fallback: `PlayerPreferences.alwaysUseExternalPlayer()` routes
    `MainActivity.startPlayerActivity(extPlayer=true)` through
    `ExternalIntents.newIntent(...)` and `externalPlayerResult` (`registerForActivityResult`).
    Result is reported back via `ExternalIntents.externalIntents.onActivityResult(...)`.
  - `fileLoaded()` runs `setMpvOptions()`, `setMpvMediaTitle()`, `setupPlayerOrientation()`,
    `setupChapters()`, `setupTracks()`, and the AniSkip merge (`ChapterUtils.mergeChapters`).
  - Known bug comment in source: `ConcurrentModificationException` in `fileLoaded()`
    (marked "MAY HAVE BEEN FIXED") at line ~1178.

---

## Feature 2 — Video Quality & Hoster Selection

- **Description**: Lets the user pick which "hoster" (mirror) and which `Video`
  (resolution / stream variant) to play. Sources can expose either the new
  ext-lib-16 hoster API (`getHosterList(episode)` → `getVideoList(hoster)`) or
  the legacy ext-lib-1.5 flat API (`getVideoList(episode)`, wrapped into a
  single pseudo-hoster). `HosterLoader.selectBestVideo` prefers
  `Video.preferred = true`, then the first non-empty resolved URL.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/ui/player/loader/EpisodeLoader.kt` (`getHosters`, `getHostersOnHttp`, `loadHosterVideos`)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/player/loader/HosterLoader.kt` (`getBestVideo`, `selectBestVideo`, `getResolvedVideo`)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/player/controls/components/sheets/QualitySheet.kt` (bottom-sheet UI)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/player/PlayerViewModel.kt` (`_hosterState`, `_hosterExpandedList`, `_selectedHosterVideoIndex`, `currentVideo`)
  - `source-api/src/commonMain/kotlin/eu/kanade/tachiyomi/animesource/model/Hoster.kt` (`Hoster` + `SerializableHoster.serialize()`)
  - `source-api/src/commonMain/kotlin/eu/kanade/tachiyomi/animesource/model/Video.kt` (`Video` model with `preferred`, `resolution`, `bitrate`)
  - Prefs: `PlayerPreferences.showFailedHosters()`, `showEmptyHosters()`
- **Status**: `keep` — core anime feature.
- **Dependencies**:
  - Depends on: `AnimeSource` interface (extensions); `AnimeHttpSource.resolveVideo`
    for lazy URL resolution; `PlayerPreferences`.
  - Depended on by: `PlayerActivity.setVideo()` (consumes the chosen `Video`).
- **Notes**:
  - The `hostList` is JSON-serialized across the launch Intent
    (`Hoster.serialize()` / `String.toHosterList()`) — used when the user pre-picks
    a hoster/video before launching, or for cross-process handoff.
  - When launching from the episode list, `hostList` is empty and the player
    re-fetches hosters at runtime via `EpisodeLoader.getHosters(...)`.

---

## Feature 3 — Subtitle & Audio Track Switching

- **Description**: Discovers external subtitle/audio tracks supplied by the
  source via `Video.subtitleTracks` / `Video.audioTracks` (list of `Track(url, lang)`),
  pushes them into MPV with `sub-add` / `audio-add` commands, and exposes
  bottom-sheets to switch tracks at runtime. Includes a full subtitle
  typography/color/border/delay customization panel and an audio-delay panel.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/ui/player/PlayerActivity.kt::setupTracks()` (issues `sub-add` / `audio-add` to MPV)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/player/controls/components/sheets/SubtitleTracksSheet.kt`
  - `app/src/main/java/eu/kanade/tachiyomi/ui/player/controls/components/sheets/AudioTracksSheet.kt`
  - `app/src/main/java/eu/kanade/tachiyomi/ui/player/controls/components/sheets/GenericTracksSheet.kt` (shared sheet chrome)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/player/controls/components/panels/SubtitleSettingsPanel.kt` (+ `SubtitleSettingsTypographyCard`, `SubtitleSettingsColorsCard`, `SubtitleSettingsMiscellaneousCard`)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/player/controls/components/panels/SubtitleDelayPanel.kt`
  - `app/src/main/java/eu/kanade/tachiyomi/ui/player/controls/components/panels/AudioDelayPanel.kt`
  - `app/src/main/java/eu/kanade/tachiyomi/ui/player/utils/TrackSelect.kt`
  - Prefs: `settings/SubtitlePreferences.kt` (font, size, scale, border, colors, justification, position, ASS override, delay, secondary delay, whitelist/blacklist/preferred languages, screenshot-subs toggle), `settings/AudioPreferences.kt` (preferred audio languages, pitch correction, `AudioChannels` enum → maps to MPV `audio-channels` / `af`, volume-boost cap, audio delay)
  - Source model: `source-api/.../animesource/model/Video.kt` (`Track`, `subtitleTracks`, `audioTracks`)
- **Status**: `modify` — UI overhaul; preferences stay.
- **Dependencies**:
  - Depends on: MPV `sub-add` / `audio-add` / `sub-select` / `audio-select`
    commands; `Video.subtitleTracks` / `Video.audioTracks` from extensions;
    `SubtitlePreferences` + `AudioPreferences`; `StorageManager.getFontsDirectory()`
    (custom subtitle fonts copied to MPV `sub-fonts-dir` in `PlayerActivity`).
  - Depended on by: `PlayerActivity.fileLoaded()` → `setupTracks()`.
- **Notes**:
  - Subtitle preferences are pushed into MPV as `sub-*` options (e.g.
    `sub-ass-override`, `sub-font-size`, `sub-scale`, `sub-border-size`,
    `sub-border-color`, `sub-color`, `sub-back-color`, `sub-align-x/y`,
    `sub-pos`, `sub-delay`).
  - Audio channel mapping uses the `AudioChannels` enum which directly emits
    MPV option strings (`audio-channels=stereo`, `af=pan=[stereo|c0=c1|c1=c0]`
    for reverse-stereo, etc.).
  - `preferredSubLanguages` / `subtitleWhitelist` / `subtitleBlacklist` /
    `preferredAudioLanguages` are comma-separated language-code lists used to
    auto-pick a track on load.

---

## Feature 4 — Skip Intro/Outro (AniSkip + Chapter Timestamps)

- **Description**: Two independent skip mechanisms: (1) **AniSkip** — online
  lookup of community-contributed skip-times for the MAL-id + episode number,
  returning `TimeStamp` segments typed `Opening`/`Ending`/`Recap`/`MixedOp`;
  supports manual skip button, auto-skip, and "Netflix-style" countdown skip.
  (2) **In-band chapter timestamps** — `Video.timestamps` supplied by the
  source itself, rendered as MPV chapters with a chapters sheet and a
  current-chapter widget.
- **Location**:
  - AniSkip API: `app/src/main/java/eu/kanade/tachiyomi/ui/player/utils/AniSkipApi.kt`
    (`https://api.aniskip.com/v2/skip-times/{malId}/{episodeNumber}?types[]=ed…`)
  - Merge logic: `app/src/main/java/eu/kanade/tachiyomi/ui/player/utils/ChapterUtils.kt`
    (`mergeChapters(currentChapters, stamps, duration)`)
  - ViewModel: `PlayerViewModel.kt` — `aniSkipResponse(malId, epNum, duration)`,
    `getMalIdFromTrack()`, `setChapter(position)`, `onSkipIntro()`,
    `introSkipEnabled`, `autoSkip`, `netflixStyle`, `waitingSkipIntro`,
    `getAnimeSkipIntroLength()` / `setAnimeSkipIntroLength()` (writes `anime.skipIntroLength`)
  - Activity: `PlayerActivity.fileLoaded()` runs the AniSkip fetch + merge
  - UI: `controls/BottomRightPlayerControls.kt` (`skipIntroButton`), `controls/components/sheets/ChaptersSheet.kt`, `controls/components/CurrentChapter.kt`
  - Chapter model: `source-api/.../animesource/model/Video.kt` (`TimeStamp`, `ChapterType` enum: `Opening`, `Ending`, `Recap`, `MixedOp`, `Other`)
  - Prefs: `PlayerPreferences.aniSkipEnabled()`, `enableSkipIntro()`, `autoSkipIntro()`, `enableNetflixStyleIntroSkip()`, `waitingTimeIntroSkip()`, `disableAniSkipOnChapters()`; `GesturePreferences.defaultIntroLength()` (default 85s)
- **Status**: `keep` — core anime feature.
- **Dependencies**:
  - Depends on: tracker linkage (AniSkip needs the anime's MAL id, retrieved
    from the user's MyAnimeList or AniList track via `AniSkipApi.getMalIdFromAL`);
    `Video.timestamps` from extensions; `PlayerPreferences` + `GesturePreferences`.
  - Depended on by: bottom-right player controls.
- **Notes**:
  - `disableAniSkipOnChapters` (default true): if the source already supplies
    chapter timestamps, AniSkip is skipped to avoid duplicate skip points.
  - The default-skip-length button ("+85 s") is actually a built-in
    `CustomButton` (id=1) seeded in `custom_buttons.sq` — see Feature 8.
    It calls `aniyomi.right_seek_by(intro_length)` via the Lua bridge and
    observes `user-data/current-anime/intro-length`.
  - AniSkip only fires when the user has at least one tracker linked (else
    `getMalIdFromTrack` returns null and `aniSkipResponse` returns null).

---

## Feature 5 — Resume & Episode Progress Memory

- **Description**: Remembers the user's playback position per episode
  (`Episode.last_second_seen` in the DB) and resumes from there on next launch.
  On launch, `PlayerActivity.setVideo()` issues
  `MPVLib.command(arrayOf("set", "start", "${resumePosition / 1000F}"))` before
  `loadfile`. While watching, `onSecondReached()` writes the current position
  back to the DB every second; episodes past `progressPreference()` (default
  0.85 = 85%) of duration are auto-marked seen, which triggers tracker updates
  and optional next-episode download.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/ui/player/PlayerActivity.kt::setVideo()` (resume-position handoff)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/player/PlayerViewModel.kt::onSecondReached()` (per-second save + seen-threshold), `saveCurrentEpisodeWatchingProgress()`, `saveWatchingProgress()`, `updateEpisodeProgressOnComplete()`, `updateTrackEpisodeSeen()`, `deleteEpisodeIfNeeded()`, `downloadNextEpisodes()`
  - DB column: `tachiyomi.animedb` → `episodes.last_second_seen` (Long millis)
  - DB write: `EpisodeUpdate` interactor (via `UpdateEpisode` usecase)
  - Prefs: `PlayerPreferences.preserveWatchingPosition()` (keep position on seen episodes), `progressPreference()` (0.85 default), `autoplayEnabled()`
- **Status**: `keep` — core anime feature.
- **Dependencies**:
  - Depends on: `Episode` DB row (`episodes.last_second_seen`, `total_seconds`,
    `seen`); `EpisodeUpdate` interactor; `AnimeHistoryUpdate` (writes
    `animehistory` table for the "continue watching" row); tracker sync
    (`updateTrackEpisodeSeen` → `TrackEpisode` interactor); `AnimeDownloadManager`
    (for `deleteEpisodeIfNeeded` and `downloadNextEpisodes`).
  - Depended on by: `AnimeHistoryTab` (shows resume progress), `AnimeScreen`
    episode-list progress bars, `AnimeUpdatesTab`.
- **Notes**:
  - `preserveWatchingPosition=false` (default): seen episodes resume from 0.
    Set to true to keep resume position even after marking seen.
  - `incognitoMode` (from `BasePreferences`) suppresses both progress writes
    and tracker updates unless the anime has trackers linked (`hasTrackers`).
  - `markDuplicateSeenEpisodeAsSeen` (LibraryPreferences) propagates "seen"
    to duplicate-numbered episodes after completion.

---

## Feature 6 — Player Gestures & Double-tap Actions

- **Description**: Touch gestures on the player surface: horizontal drag =
  seek (with optional smooth-seek), vertical drag on right half = volume,
  vertical drag on left half = brightness (or swapped). Double-tap on left /
  center / right third is user-configurable per-zone (seek, play-pause, switch
  episode, custom MPV keypress, none). Hardware media keys (DPAD, MEDIA_REWIND,
  MEDIA_FAST_FORWARD, MEDIA_NEXT, etc.) and Bluetooth media buttons are routed
  to the same configurable actions via `MediaSession.Callback`.
- **Location**:
  - `app/src/main/java/eu/kanade/tachiyomi/ui/player/controls/GestureHandler.kt` (touch-event interpretation)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/player/controls/components/VerticalSliders.kt` (volume / brightness on-screen sliders)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/player/controls/components/DoubleTapSeekTriangles.kt` (double-tap seek visual feedback)
  - `app/src/main/java/eu/kanade/tachiyomi/ui/player/controls/components/BrightnessOverlay.kt`
  - `app/src/main/java/eu/kanade/tachiyomi/ui/player/controls/components/SeekBar.kt`
  - `app/src/main/java/eu/kanade/tachiyomi/ui/player/PlayerEnums.kt::SingleActionGesture` (enum: `None`, `Seek`, `PlayPause`, `Switch`, `Custom`)
  - `PlayerActivity.onKeyDown` / `setupMediaSession()` (hardware-key routing)
  - Prefs: `settings/GesturePreferences.kt` — `gestureVolumeBrightness()`, `swapVolumeBrightness()`, `gestureHorizontalSeek()`, `showSeekBar()`, `playerSmoothSeek()`, `skipLengthPreference()` (10s default), `defaultIntroLength()` (85s), `leftDoubleTapGesture()`, `centerDoubleTapGesture()`, `rightDoubleTapGesture()`, `mediaPreviousGesture()`, `mediaPlayPauseGesture()`, `mediaNextGesture()`
- **Status**: `modify` — gesture handler logic stays; visual feedback UI will
  be redesigned.
- **Dependencies**:
  - Depends on: `GesturePreferences`; `PlayerViewModel.leftSeek()` /
    `rightSeek()` / `pauseUnpause()` / `changeEpisode(direction)` /
    `handleLeftDoubleTap()` / `handleRightDoubleTap()`; audio manager for
    volume; window brightness for brightness.
  - Depended on by: `PlayerControls` (registers the gesture handler on the
    MPV surface).
- **Notes**:
  - `SingleActionGesture.Custom` mode dispatches an MPV `keypress` for
    `CustomKeyCodes.MediaPlay` / `MediaPrevious` / `MediaNext` — useful for
    user-defined `input.conf` bindings.
  - `skipLengthPreference` controls the seek delta for double-tap and
    media-key seek actions (default 10 seconds).

---

## Feature 7 — Player Settings Suite

- **Description**: The player settings UI, organized into 7 Compose screens
  (Main, Player, Decoder, Subtitle, Audio, Gestures, Advanced) backed by 6
  preference classes. Covers decoder selection (HW decoding, gpu-next,
  debanding, YUV420P, video filters), subtitle/audio typography & delay,
  gesture behavior, AniSkip options, PiP, external player, playback speed
  presets, custom Lua scripts / `mpv.conf` / `input.conf`, and statistics
  page selection.
- **Location**:
  - Prefs classes (all under `app/src/main/java/eu/kanade/tachiyomi/ui/player/settings/`):
    - `PlayerPreferences.kt` — display (fullscreen, hide controls, panel opacity, time-to-disappear, reduce motion, system status bar), hoster visibility, skip-intro & AniSkip, PiP (`enablePip`, `pipOnExit`, `pipEpisodeToasts`, `pipReplaceWithPrevious`), external player (`alwaysUseExternalPlayer`, `externalPlayerPreference`), playback speed (`playerSpeed`, `speedPresets`), aspect ratio, orientation, autoplay, `preserveWatchingPosition`, `progressPreference`, brightness/volume memory
    - `SubtitlePreferences.kt` — font, size, scale, border, bold/italic, text/border/background colors, border style (`SubtitlesBorderStyle`), shadow offset, justification (`SubtitleJustification`), position, ASS override, delay, speed, secondary delay, preferred languages, whitelist, blacklist, screenshot-subs toggle
    - `AudioPreferences.kt` — preferred audio languages, pitch correction, `AudioChannels` enum (auto / auto-safe / mono / stereo / reverse-stereo), volume-boost cap, audio delay
    - `DecoderPreferences.kt` — `tryHWDecoding`, `gpuNext`, `Debanding` enum, `useYUV420P`, video filters (brightness, saturation, contrast, gamma, hue)
    - `GesturePreferences.kt` — see Feature 6
    - `AdvancedPlayerPreferences.kt` — `mpvUserFiles` (load user Lua/scripts), `mpvConf` (raw `mpv.conf` content), `mpvInput` (raw `input.conf` content), `playerStatisticsPage`
  - Settings screens (all under `app/src/main/java/eu/kanade/presentation/more/settings/screen/player/`):
    - `PlayerSettingsMainScreen.kt` (entry list)
    - `PlayerSettingsPlayerScreen.kt` (general player prefs)
    - `PlayerSettingsDecoderScreen.kt`
    - `PlayerSettingsSubtitleScreen.kt`
    - `PlayerSettingsAudioScreen.kt`
    - `PlayerSettingsGesturesScreen.kt`
    - `PlayerSettingsAdvancedScreen.kt`
  - Entry point: `app/src/main/java/eu/kanade/tachiyomi/ui/setting/PlayerSettingsScreen.kt` (Voyager `Screen` wrapper, two-pane on tablets)
- **Status**: `modify` — all 7 settings screens will be re-skinned; the
  underlying 6 *Preferences classes and their pref keys stay (data stability).
- **Dependencies**:
  - Depends on: the 6 *Preferences classes; `PreferenceStore` (see `09-preferences.md`); `StorageManager` (for MPV config / fonts / scripts dirs referenced by Advanced prefs).
  - Depended on by: `PlayerActivity` (reads most prefs at `fileLoaded()` / `setMpvOptions()`), `PlayerViewModel`, `GestureHandler`, `PlayerControls`.
- **Notes**:
  - `mpvUserFiles=true` triggers `PlayerActivity.copyUserFiles()` which copies
    user Lua scripts, fonts, shaders, and `script-opts` from app storage into
    the MPV config dir (`<baseStorage>/mpv/{scripts,fonts,shaders,script-opts}`).
    The bundled `assets/aniyomi.lua` is the Lua/JS bridge that custom buttons
    depend on (see Feature 8).
  - `mpvConf` / `mpvInput` raw strings are written to `<baseStorage>/mpv/mpv.conf`
    and `input.conf` respectively and loaded by libmpv at startup.
  - Playback speed: `PlayerPreferences.playerSpeed()` (Float, default 1.0) and
    `speedPresets()` (default set `0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0, 2.5, 3.0, 3.5, 4.0`)
    are surfaced in `controls/components/sheets/PlaybackSpeedSheet.kt`. Speed
    is pushed to MPV via `MPVLib.setPropertyString("speed", value)`.

---

## Feature 8 — Custom Lua-defined MPV Buttons

- **Description**: User-defined on-screen player buttons backed by Lua scripts
  executed inside MPV. Each `CustomButton` row has `content` (Lua body for tap),
  `longPressContent` (Lua body for long-press), and `onStartup` (Lua body run
  on player start, e.g. to observe an MPV property and update the button
  title). The player generates a `custombuttons.lua` file at runtime that
  `require`s the bundled `aniyomi` Lua module and registers
  `script-message call_button_<id>` / `call_button_<id>_long` handlers per
  button, then `load-script`s it into MPV. One button can be marked
  "primary" (shown inline in the bottom-right controls; others appear in a
  sheet). The built-in default-skip-length button ("+85 s") is seeded as
  id=1 in the DB migration.
- **Location**:
  - DB table: `data/src/main/sqldelightanime/dataanime/custom_buttons.sq`
    (columns: `_id, name, isFavorite, sortIndex, content, longPressContent, onStartup`)
    + migration `129.sqm` (initial CREATE + seed row for the "+85 s" button)
  - Domain model: `domain/src/main/java/tachiyomi/domain/custombuttons/model/CustomButton.kt` (with `getButtonContent(primaryId)`, `getButtonLongPressContent(primaryId)`, `getButtonOnStartup(primaryId)` — all do `$id` / `$isPrimary` token substitution)
  - Repository: `domain/.../custombuttons/repository/CustomButtonRepository.kt` + `data/.../custombutton/CustomButtonRepositoryImpl.kt`
  - Lua bridge: `app/src/main/java/eu/kanade/tachiyomi/ui/player/PlayerActivity.kt::setupCustomButtons(buttons)` (writes `custombuttons.lua` to `<files>/mpv/scripts/`, then `MPVLib.command(arrayOf("load-script", path))`)
  - ViewModel: `PlayerViewModel.kt` — `primaryButton` / `primaryButtonTitle` StateFlows, `handleLuaInvocation(property, value)` (receives `user-data/aniyomi` events from Lua)
  - Settings UI: `app/src/main/java/eu/kanade/presentation/more/settings/screen/player/custombutton/` — `PlayerSettingsCustomButtonScreen.kt`, `PlayerSettingsCustomButtonScreenModel.kt`, `components/CustomButtonScreen.kt`, `components/CustomButtonListItem.kt`, `components/CustomButtonDialogs.kt`
  - Backup/restore: `data/backup/create/creators/CustomButtonBackupCreator.kt`, `data/backup/restore/restorers/CustomButtonRestorer.kt`
  - Bundled Lua module: `app/src/main/assets/aniyomi.lua` (the `require 'aniyomi'` module that exposes `right_seek_by`, `int_picker`, `show_button`, `hide_button`, `set_button_title`, etc.)
- **Status**: `keep` — distinctive MPV-power-user feature; survives rebrand.
  Settings UI for managing buttons = `modify`.
- **Dependencies**:
  - Depends on: MPV `load-script` + `script-message` + `user-data/aniyomi`
    property bridge; `assets/aniyomi.lua` bundled module; `StorageManager`
    (scripts dir); `AdvancedPlayerPreferences.mpvUserFiles()` (gates whether
    user Lua is loaded at all); `custom_buttons` SQLDelight table
    (`tachiyomi.animedb`).
  - Depended on by: `PlayerControls` (renders `primaryButton` inline +
    overflow sheet), backup/restore jobs.
- **Notes**:
  - The primary-button mechanism: exactly one `CustomButton` is the user's
    "primary"; its `onStartup` typically calls `mp.observe_property(...)` and
    `aniyomi.show_button()` / `hide_button()` to make it appear/disappear
    contextually. The default "+85 s" button observes
    `user-data/current-anime/intro-length`.
  - Lua ↔ Kotlin bridge: Lua calls `mp.commandv("set_property", "user-data/aniyomi", "<json>")`,
    which fires `PlayerActivity`'s MPV property observer for
    `user-data/aniyomi`, which dispatches to `PlayerViewModel.handleLuaInvocation(...)`.
    This is how Lua buttons can trigger Kotlin-side effects (e.g. open an int
    picker dialog, change episode).
  - Token substitution: `$id` → button id; `$isPrimary` → `"true"` / `"false"`.
    Lets a single Lua snippet behave differently for primary vs non-primary buttons.

---

## Feature 9 — Screenshot Capture, PiP & MediaSession (Auxiliary Player Features)

- **Description**: Three auxiliary in-player features:
  (a) **Screenshot capture** — `MPVLib.command(arrayOf("screenshot-to-file", path, "subtitles"|"video"))`
  captures the current frame (with or without subs); user can save to Pictures,
  share via Android share-sheet, or set as anime cover / background / thumbnail.
  (b) **Picture-in-Picture** — when the user leaves the player (or auto on
  exit if `pipOnExit` is set), playback continues in a PiP window with custom
  remote actions (play/pause, next/prev episode, +10s seek); title/subtitle
  shown on Android 13+.
  (c) **MediaSession** — registers an `android.media.session.MediaSession`
  for lock-screen, Bluetooth, and Android Auto / Wear OS media controls;
  callbacks are routed through the user's `SingleActionGesture` preferences.
- **Location**:
  - Screenshot:
    - `app/src/main/java/eu/kanade/tachiyomi/ui/player/PlayerViewModel.kt::takeScreenshot(cachePath, showSubtitles)`, `saveImage(...)`, `shareImage(...)`, `setAsImage(...)`
    - `app/src/main/java/eu/kanade/tachiyomi/ui/player/controls/components/sheets/ScreenshotSheet.kt` (UI: Save / Share / Set as Cover / Set as Background / Set as Thumbnail + show-subs toggle)
    - `app/src/main/java/eu/kanade/tachiyomi/ui/player/PlayerActivity.kt::onShareImageResult`, `onSaveImageResult`, `onSetAsArtResult`
    - `app/src/main/java/eu/kanade/tachiyomi/ui/reader/SaveImageNotifier.kt` (reused for screenshot save notifications)
    - `app/src/main/java/eu/kanade/tachiyomi/data/saver/ImageSaver.kt` + `data/saver/...`
    - Pref: `SubtitlePreferences.screenshotSubtitles()`
    - `ArtType` enum (`Cover`, `Background`, `Thumbnail`) in `PlayerEnums.kt` or similar
  - PiP:
    - `PlayerActivity.createPipParams()`, `onPictureInPictureModeChanged(...)`, `pipReceiver` (BroadcastReceiver for PiP remote actions)
    - `app/src/main/java/eu/kanade/tachiyomi/ui/player/PipActions.kt` (`createPipActions(context, isPaused, replaceWithPrevious, playlistCount, playlistPosition)` → `RemoteAction`s)
    - Prefs: `PlayerPreferences.enablePip()`, `pipOnExit()`, `pipEpisodeToasts()`, `pipReplaceWithPrevious()`
  - MediaSession:
    - `PlayerActivity.setupMediaSession()` (creates `MediaSession("PlayerActivity")`, sets `PlaybackState` actions play/pause/stop/skip-to-prev/skip-to-next, registers `ACTION_AUDIO_BECOMING_NOISY` receiver)
    - Callbacks route to `SingleActionGesture` enum lookups from `GesturePreferences.mediaPreviousGesture()` / `mediaPlayPauseGesture()` / `mediaNextGesture()`
    - Audio focus: `AudioFocusRequestCompat` with `USAGE_MEDIA` / `CONTENT_TYPE_MUSIC`, ducking on `AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK`, pause on loss.
- **Status**: `keep` — all three are core anime-watching features. UI for
  screenshot sheet = `modify`.
- **Dependencies**:
  - Screenshot depends on: MPV `screenshot-to-file` command; `ImageSaver`
    (writes to `Location.Pictures(<anime-title>)`); `SaveImageNotifier`;
    cover/background/thumb update interactors for "set as art".
  - PiP depends on: `PackageManager.FEATURE_PICTURE_IN_PICTURE`;
    `PlayerPreferences.enablePip()`; `BasePreferences.deviceHasPip()`.
  - MediaSession depends on: `GesturePreferences` (media-key action mapping);
    `AudioManager` (audio focus); `noisyReceiver`.
  - Depended on by: bottom-right player controls (screenshot button), system
    PiP / media button infrastructure.
- **Notes**:
  - PiP aspect ratio is derived from `player.getVideoOutAspect()` clamped to
    the Android-permitted range (0.42–2.38).
  - `pipReplaceWithPrevious` swaps the "next" PiP action for "previous" —
    useful for binge-watchers who want to go back, not forward, from PiP.
  - `setAsImage` for `ArtType.Thumbnail` is only available when the anime is
    in local source (`isLocalSource` flag in `ScreenshotSheet`).
  - The `MediaSession` is **separate** from any tracker MediaSession — it's
    purely for system media-button routing, not for tracker scrobbling.
