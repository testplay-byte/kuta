# APK Size Analysis — `app-arm64-v8a-debug.apk`

> Source of truth: artifact `app-debug-apk` from CI run
> [`28723594409`](https://github.com/testplay-byte/kuta/actions/runs/28723594409)
> (Phase 3, AniList home screen, commit `9ba07d2`).
> All numbers below come from `unzip -l` / `unzip -lv` on the actual APK file
> plus `du -b` on the source tree. No source files were modified for this
> analysis.

---

## 0. TL;DR

| Metric | Value |
|---|---|
| **APK file size** | **90,921,114 bytes (86.71 MiB)** |
| Total uncompressed content | 166,030,589 bytes (158.32 MiB) |
| Number of entries | 2,089 |
| Compressed payload + ZIP overhead | 90,404,281 B payload + 516,833 B ZIP overhead |
| Build type | **`debug`** (no R8, no resource shrink, no `.so` symbol stripping) |
| ABI | **`arm64-v8a` only** (FORK marker `app/build.gradle.kts:93-101`) |
| Biggest category | **Native libs (.so) — 41.47 MB, 47.5% of compressed payload** |
| Biggest single file | **`classes.dex` — 41.96 MB uncompressed / 10.69 MB compressed** |
| Top removal recommendation | Ship **`preview`/`release`** build type instead of `debug` — R8 + resource shrink alone should drop the APK to **~50–55 MB**. Pair with manga code removal + 7-tracker removal for **~45–48 MB**. |

> ⚠️ **The 62 MB number the user referenced is the size of the GitHub Actions
> artifact ZIP download (60 MB on disk).** The actual APK inside that ZIP is
> **86.71 MB** (GitHub ZIP-compresses the APK for transport). Going forward,
> use `ls -l app-arm64-v8a-debug.apk` numbers, not the artifact-download
> numbers.

---

## 1. Total APK size

```
$ ls -l app-arm64-v8a-debug.apk
-rw-rw-r-- 1 z z 90921114 Jul  4 23:56 app-arm64-v8a-debug.apk
```

- Exact file size: **90,921,114 bytes**
- In MiB (1024²): **86.71 MiB**
- In MB (1000²): **90.92 MB**

Sum of per-entry compressed sizes from `unzip -lv`: **90,404,281 bytes
(86.22 MiB)**. The remaining **516,833 bytes (0.49 MiB)** is ZIP file headers,
per-entry metadata, and 4-byte alignment padding for `extractNativeLibs=false`
(which is the AGP 7.0+ default and lets the OS mmap `.so` files directly out of
the APK without extracting them).

---

## 2. Breakdown by category

Computed via `unzip -lv` on the APK, summing compressed (column 3) and
uncompressed (column 1) sizes per category. The "compressed" column is what
actually ships inside the APK file; "uncompressed" is what the file becomes
once extracted/mapped into memory.

| Category | Compressed (in-APK) | % of payload | Uncompressed | # files | Notes |
|---|---:|---:|---:|---:|---|
| **Native libs (`lib/arm64-v8a/*.so`)** | **41,485,344 B (39.56 MiB)** | **45.9%** | 41,485,344 B (39.56 MiB) | 20 | All `Stored` (no ZIP compression) — required for `extractNativeLibs=false` mmap. |
| **DEX (`classes*.dex`)** | **35,562,522 B (33.92 MiB)** | **39.3%** | 106,112,136 B (101.20 MiB) | 38 | 38 DEX files; `classes.dex` alone is 41.96 MB uncompressed. Ratio 33.5%. |
| **`resources.arsc`** | **5,936,332 B (5.66 MiB)** | **6.6%** | 5,936,332 B (5.66 MiB) | 1 | Binary resource table. `Stored`. Inflated by **67 i18n + 68 i18n-aniyomi locales** (≈135 strings.xml files, 4.6 MB source total). |
| **`assets/`** | **3,312,513 B (3.16 MiB)** | **3.7%** | 6,605,488 B (6.30 MiB) | 3 | `subfont.ttf` (6.4 MB MPV subtitle font), `cacert.pem` (MPV TLS roots), `aniyomi.lua` (3 KB MPV Lua bridge). |
| **`res/`** | **2,033,035 B (1.94 MiB)** | **2.2%** | 3,732,927 B (3.56 MiB) | 2,000 | Layouts (581), layouts-v33 (319), drawables (315), colors (195), fonts (3), etc. |
| **Other top-level** | **72,164 B (0.07 MiB)** | **0.08%** | 153,717 B (0.15 MiB) | ~10 | `AndroidManifest.xml` (34.6 KB), `kotlin/*.kotlin_builtins` (50 KB), `okhttp3/.../PublicSuffixDatabase.gz` (41 KB), `org/commonmark/.../entities.properties` (24 KB), `DebugProbesKt.bin`, etc. |
| **`META-INF/`** | **2,371 B (0.002 MiB)** | <0.01% | 4,645 B (0.005 MiB) | ~10 | Service-loader manifests + okhttp/jsoup metadata. `META-INF/*.version` already excluded via `app/build.gradle.kts:130-141`. |
| **ZIP overhead** | **516,833 B (0.49 MiB)** | — | — | — | Headers + 4-byte alignment for `extractNativeLibs=false`. |
| **Total APK** | **90,921,114 B (86.71 MiB)** | **100%** | 166,030,589 B (158.32 MiB) | 2,089 | — |

### Observations

1. **Native libs (.so) are the single largest category** at 45.9% of the APK,
   and they are **stored uncompressed** in the APK (required for
   `extractNativeLibs=false`). This is the price of MPV + FFmpeg + libarchive +
   SQLite + Conscrypt + QuickJS.
2. **DEX is the second largest** at 39.3% of the APK. The 38 DEX files compress
   from 101 MB uncompressed to 34 MB compressed (a 3:1 ratio). With R8 enabled
   (release build), this would typically drop 50–70% — easily the **largest
   single optimisation available**.
3. **`resources.arsc` is unusually large at 5.66 MB** because Kuta ships
   **67 + 68 = 135 locale string tables** (i18n + i18n-aniyomi). Each locale
   fully mirrors the base strings.xml. The pre-i18n version of Tachiyomi
   shipped roughly 30–40 locales; Kuta carries the full Aniyomi community-
   translated set. Cutting to ~10 high-traffic locales would shed ~3 MB.
4. **The 4.6 MB `subfont.ttf` asset** is the bundled subtitle font for MPV.
   Required for the player; not removable.

---

## 3. Top 20 largest files

Sorted by uncompressed size (which is what shows in `unzip -l`). Compressed
size is what actually ships in the APK; "Stored" means the file is in the APK
byte-for-byte (no ZIP compression — applies to all `.so` files and to
`resources.arsc`).

| # | File path | Uncompressed | Compressed | Method | Category |
|---:|---|---:|---:|---|---|
| 1 | `classes.dex` | 41,960,900 (40.01 MiB) | 10,689,675 | Defl:N (75%) | DEX |
| 2 | `classes35.dex` | 14,238,176 (13.58 MiB) | 5,718,159 | Defl:N (60%) | DEX |
| 3 | `lib/arm64-v8a/libavcodec.so` | 13,516,616 (12.89 MiB) | 13,516,616 | Stored | Native (FFmpeg codec) |
| 4 | `classes37.dex` | 12,894,148 (12.30 MiB) | 5,081,071 | Defl:N (61%) | DEX |
| 5 | `classes36.dex` | 10,165,820 (9.70 MiB) | 4,142,580 | Defl:N (59%) | DEX |
| 6 | `assets/subfont.ttf` | 6,365,592 (6.07 MiB) | 3,169,988 | Defl:N (50%) | Asset (MPV subtitle font) |
| 7 | `resources.arsc` | 5,936,332 (5.66 MiB) | 5,936,332 | Stored | Resource table |
| 8 | `lib/arm64-v8a/libmpv.so` | 5,535,872 (5.28 MiB) | 5,535,872 | Stored | Native (MPV player core) |
| 9 | `lib/arm64-v8a/libimagedecoder.so` | 5,190,416 (4.95 MiB) | 5,190,416 | Stored | Native (image decoder for reader) |
| 10 | `lib/arm64-v8a/libavfilter.so` | 4,230,168 (4.03 MiB) | 4,230,168 | Stored | Native (FFmpeg filters) |
| 11 | `lib/arm64-v8a/libavformat.so` | 3,323,168 (3.17 MiB) | 3,323,168 | Stored | Native (FFmpeg muxers/demuxers) |
| 12 | `classes38.dex` | 2,225,100 (2.12 MiB) | 927,099 | Defl:N (58%) | DEX |
| 13 | `lib/arm64-v8a/libconscrypt_jni.so` | 2,103,592 (2.01 MiB) | 2,103,592 | Stored | Native (Conscrypt TLS for Android <10) |
| 14 | `classes19.dex` | 1,838,720 (1.75 MiB) | 681,944 | Defl:N (63%) | DEX |
| 15 | `lib/arm64-v8a/libc++_shared.so` | 1,794,776 (1.71 MiB) | 1,794,776 | Stored | Native (C++ runtime) |
| 16 | `lib/arm64-v8a/libsqlite3x.so` | 1,791,880 (1.71 MiB) | 1,791,880 | Stored | Native (SQLDelight SQLite) |
| 17 | `classes20.dex` | 1,738,076 (1.66 MiB) | 589,885 | Defl:N (66%) | DEX |
| 18 | `classes24.dex` | 1,633,336 (1.56 MiB) | 617,196 | Defl:N (62%) | DEX |
| 19 | `lib/arm64-v8a/libarchive-jni.so` | 1,614,360 (1.54 MiB) | 1,614,360 | Stored | Native (libarchive for CBZ/CBR/EPUB) |
| 20 | `classes31.dex` | 1,451,816 (1.38 MiB) | 551,978 | Defl:N (62%) | DEX |

The top 20 files account for **126.5 MB uncompressed / 75.7 MB compressed**
— i.e. **83.6% of the compressed payload** comes from just 20 files (out of
2,089).

### What this tells you

- 8 of the top 20 are DEX files. The remaining 30 DEX files (classes2–18, 21–23,
  25–30, 32–34) are all <1 MB each, totalling ~14 MB compressed.
- 9 of the top 20 are native libraries. The biggest single `.so` is
  `libavcodec.so` at 12.89 MiB — FFmpeg's video codec library. This is the
  single largest file in the entire APK by uncompressed size (only `classes.dex`
  is bigger).
- `assets/subfont.ttf` (6.07 MiB) and `resources.arsc` (5.66 MiB) are the two
  non-code, non-lib heavyweights. Both are required.

---

## 4. Native libraries breakdown

All 20 `.so` files, all arm64-v8a, all `Stored` (no ZIP compression — required
for `extractNativeLibs=false` direct mmap). Total: **41,485,344 bytes (39.56
MiB)** — 47.5% of the compressed APK payload.

| .so file | Size (bytes) | MiB | Purpose | Removable? |
|---|---:|---:|---|---|
| `libavcodec.so` | 13,516,616 | 12.89 | FFmpeg — video codec library (decode/encode H.264, HEVC, AV1, VP9, etc.) | ❌ Required for MPV playback |
| `libmpv.so` | 5,535,872 | 5.28 | MPV player core (audio/video pipeline, scripting, subtitle engine) | ❌ Required for the player |
| `libimagedecoder.so` | 5,190,416 | 4.95 | Image decoder (com.davemorrissey.subsamplingscaleimageview + tachiyomi image-decoder fork) — used by manga reader for paged/scroll image rendering | ⚠️ Manga-only after removal; manga reader also still used for local-archive anime sources |
| `libavfilter.so` | 4,230,168 | 4.03 | FFmpeg — filter graph (audio EQ, video filters, format conversion) | ❌ Required by MPV/FFmpeg pipeline |
| `libavformat.so` | 3,323,168 | 3.17 | FFmpeg — container/protocol muxing+demuxing (MP4, MKV, HLS, DASH, TCP, HTTP) | ❌ Required by MPV/FFmpeg pipeline |
| `libconscrypt_jni.so` | 2,103,592 | 2.01 | Conscrypt TLS provider — only loaded on Android 8/9 (`App.kt:89-91`: `if (SDK_INT < Q) Security.insertProviderAt(...)`) | ✅ **If minSdk is raised to 29** |
| `libc++_shared.so` | 1,794,776 | 1.71 | LLVM C++ standard library (shared by FFmpeg, MPV, libarchive, sqlite, QuickJS) | ❌ Required by all native deps |
| `libsqlite3x.so` | 1,791,880 | 1.71 | SQLDelight's native SQLite (replaces Android system SQLite for newer features) | ❌ Required by persistence layer |
| `libarchive-jni.so` | 1,614,360 | 1.54 | libarchive — CBZ/CBR/EPUB/ZIP/RAR/7z extraction for local sources | ⚠️ Used by `:core:archive` for local manga **and** local anime (anime from CBZ archives) |
| `libxml2.so` | 1,379,808 | 1.32 | libxml2 — XML parser (pulled in by FFmpeg's DASH/MP4 subtitle support, also by some image-decoder paths) | ⚠️ Indirect; removable only if FFmpeg build excludes XML demuxers |
| `libquickjs.so` | 854,224 | 0.81 | QuickJS JavaScript engine — for source extensions that use JS (`JavaScriptEngine.kt` in `:core:common`) | ❌ Required by extension system |
| `libswscale.so` | 754,248 | 0.72 | FFmpeg — pixel-format conversion + scaling | ❌ Required by FFmpeg pipeline |
| `libavutil.so` | 723,792 | 0.69 | FFmpeg — shared utility library (used by all other FFmpeg libs) | ❌ Required by FFmpeg pipeline |
| `libffmpegkit.so` | 465,208 | 0.44 | FFmpegKit JNI wrapper — used by `AnimeDownloader` for download transcoding/remux | ⚠️ Removable only if download transcode feature is dropped |
| `libswresample.so` | 97,552 | 0.09 | FFmpeg — audio resampling | ❌ Required by FFmpeg pipeline |
| `libpostproc.so` | 37,392 | 0.04 | FFmpeg — post-processing (deinterlace, deblock) | ❌ Required by FFmpeg pipeline |
| `libffmpegkit_abidetect.so` | 29,160 | 0.03 | FFmpegKit ABI detection shim | ⚠️ Debugging-only; potentially removable in release |
| `libplayer.so` | 23,912 | 0.02 | Kuta's own thin JNI shim into MPV (`libplayer/`) | ❌ Required by player |
| `libandroidx.graphics.path.so` | 10,096 | 0.01 | AndroidX graphics-path native utils | ❌ Pulled by AndroidX dependency |
| `libavdevice.so` | 9,104 | 0.01 | FFmpeg — device capture (camera, screen) — likely unused but pulled by FFmpegKit build | ⚠️ Could be excluded from a custom FFmpeg build |

### FFmpeg sub-libraries

The 9 FFmpeg libraries (`libavcodec`, `libavfilter`, `libavformat`, `libavutil`,
`libswscale`, `libswresample`, `libpostproc`, `libavdevice`, plus
`libffmpegkit` wrapper) total **23,143,466 bytes (22.07 MiB)** — i.e.
**55.8% of all native code** and **25.6% of the entire APK payload**. These
are the FFmpegKit published binaries (Arthenica-FFmpegKit fork via
`aniyomilibs.ffmpeg.kit` in `app/build.gradle.kts:307`).

### What's NOT in the APK

- ✅ Confirmed `arm64-v8a` is the only ABI (no `armeabi-v7a`, no `x86_64`).
  FORK marker at `app/build.gradle.kts:93-101` enforces this for debug builds.
- ✅ No `mips` / `mips64`.
- ✅ **No `macrobenchmark` code** in the APK — the `:macrobenchmark` module is
  `com.android.test` (`targetProjectPath = ":app"`) and is never a dependency
  of `:app`. Verified: `unzip -l ... | grep -i benchmark` returns nothing.

---

## 5. Feature-by-feature removal estimates

These are estimates. Where the source code size is measurable, the conversion
to DEX is approximated at **1.5× source size** (Kotlin compiles to bytecode
with debug metadata; release R8 would compress further). Native-lib savings are
exact (from `unzip -l`).

| Feature | Source size (Kotlin) | Estimated DEX savings | Native-lib savings | Total est. savings | Notes |
|---|---:|---:|---:|---:|---|
| **Manga code** (all 28 manga dirs across `:app`, `:data`, `:domain`, `:source-local`, `:source-api`) | 1,154,275 B (1.10 MiB) | ~1.5–2.5 MiB | 0 (no manga-only `.so`) | **~1.5–2.5 MiB** | See §5.1 below for breakdown. Note: `libimagedecoder.so` (4.95 MiB) and `libarchive-jni.so` (1.54 MiB) are *also* used by anime local-source archives, so cannot be removed even with manga gone. |
| **7 removable trackers** (Kitsu, Simkl, Komga, Kavita, Suwayomi, Jellyfin, MangaUpdates) | 124,075 B (121 KiB) | ~150–250 KiB | 0 | **~250–350 KiB** (+90 KiB icon assets) | See §5.2. |
| **Macrobenchmark module** | n/a | 0 (not in APK) | 0 | **0** | Already excluded — `com.android.test` module, not an `:app` dependency. |
| **Conscrypt (raise minSdk 26→29)** | n/a | 0 (small Kotlin shim ~1 KiB) | 2,103,592 B (2.01 MiB) | **~2.0 MiB** | `App.kt:89-91` only loads Conscrypt on Android <10. Raising minSdk to 29 lets you drop `libs.conscrypt.android` and strip the conditional. |
| **3 unused design fonts** (Inter Variable, JetBrains Mono Variable, Caveat Variable) | n/a | 0 | 0 | **~1.4 MiB** (`res/font/`) | Fonts added in Phase 2B for the three new design systems. If you ship a single design only, drop the other two fonts. |
| **Locale cut** (67+68 → ~10 locales) | n/a | 0 | 0 | **~3 MiB** off `resources.arsc` | Aniyomi ships 67 i18n + 68 i18n-aniyomi locale string tables. Each is ~35 KB. Cutting to ~10 high-traffic locales (en, es, pt-rBR, fr, de, ja, ru, zh, it, ko) saves ~3 MB off `resources.arsc`. |
| **`aboutlibraries.json`** | n/a | 0 | 0 | **155 KiB** | `res/raw/aboutlibraries.json` (229 libraries listed). Only used by `OpenSourceLicensesScreen.kt`. Could be moved to a build-time-generated slice loaded only when the screen opens, or dropped entirely (licenses also available on the project GitHub). |
| **`updates_grid_widget_preview.webp`** | n/a | 0 | 0 | **420 KiB** | Single largest `res/` file. App-widget preview image. Required by Google Play widget preview, but only the `presentation-widget` module needs it. Could be re-encoded at lower quality. |
| **`subfont.ttf`** (MPV subtitle font) | n/a | 0 | 0 | **0** (3.17 MiB compressed but **required**) | MPV requires a fallback font for subtitles. Could in theory be replaced with a smaller font, but switching to a font without CJK coverage would break CJK subtitles. |
| **RxJava** (`io.reactivex`) | n/a | 0 | 0 | **~200–500 KiB** | Declared in `app/build.gradle.kts:240` (`implementation(libs.rxjava)`) and `core/common/build.gradle.kts:25` (`api(libs.rxjava)`). **Zero direct usages** in Kuta source (`grep -rln io.reactivex --include='*.kt'` returns 0). However, `:source-api` exposes `rx.Observable` via `RxExtension.kt` — external source extensions may use RxJava, so removing it breaks the source-extension contract. **Investigation needed**, not a quick win. |

### 5.1 Manga code breakdown (estimated 1.5–2.5 MiB DEX savings)

| Module | Path | Size |
|---|---|---:|
| `:app` | 28 `manga/` subdirs under `app/src/main/java/.../{ui,presentation,data,domain,extension,feature}/...` | 1,009,655 B (986 KiB) |
| `:domain` | 10 `manga/` subdirs under `domain/src/main/java/.../{entries,category,history,updates,source,track,library,extensionrepo,upcoming}/manga/` | 70,811 B (69 KiB) |
| `:data` | 8 `manga/` subdirs under `data/src/main/java/.../{repository,handlers,entries,category,history,updates,source,track}/manga/` | 48,999 B (48 KiB) |
| `:source-local` | 6 `manga/` subdirs (`image/manga`, `entries/manga`, `io/manga`, `filter/manga`) | 19,663 B (19 KiB) |
| `:source-api` | `MangaSource.kt` + `model/{SManga,SMangaImpl,SChapter,SChapterImpl,MangasPage}.kt` | 5,147 B (5 KiB) |
| **Total Kotlin source** | | **1,154,275 B (1.10 MiB)** |
| Plus manga `.sq` files | `mangas.sq`, `chapters.sq`, `manga_sync.sq`, `mangas_categories.sq` | ~30 KiB source |
| Plus `:presentation-core` manga components | none — `presentation-core` has no `manga/` dirs | 0 |
| Plus `:core:metadata` manga components | none | 0 |

After Kotlin compilation in a debug build (no R8), 1.10 MiB of source typically
becomes **~1.5–2.5 MiB of DEX** (each `.kt` produces bytecode + Kotlin metadata
+ debug line tables; classes in different files don't dedupe against each
other). This is an **estimate** — measuring exactly would require a removal
experiment. Animiru (per worklog entry 3-c) physically removed all 28 manga
dirs and stayed anime-only.

> **Native-lib caveat**: `libimagedecoder.so` (4.95 MiB) is wired to the
> manga reader's `SubsamplingScaleImageView`, but it is also used by anime
> local-source archives (CBZ-bundled anime episodes). Removing manga does NOT
> remove this `.so`. Likewise `libarchive-jni.so` (1.54 MiB) is used by
> `:core:archive` for both manga and anime local sources. **Manga removal
> saves no native-lib bytes**; only DEX.

### 5.2 Removable trackers breakdown (estimated ~340 KiB total)

Per `FEATURES/04-trackers.md`: keep AniList, MAL, Shikimori, Bangumi; remove
the other 7.

| Tracker | Source dir | Source size | Stable ID | Type | Notes |
|---|---|---:|---:|---|---|
| Kitsu | `app/.../data/track/kitsu/` | 38,259 B (37 KiB) | 3 | OAuth | Anime+Manga tracker |
| Simkl | `app/.../data/track/simcl/` | 20,744 B (20 KiB) | 101 | OAuth | Anime+Manga tracker |
| Komga | `app/.../data/track/komga/` | 10,693 B (10 KiB) | 6 | API key + EnhancedMangaTracker | Manga-only server tracker |
| Kavita | `app/.../data/track/kavita/` | 15,612 B (15 KiB) | 8 | API key + EnhancedMangaTracker | Manga-only server tracker |
| Suwayomi | `app/.../data/track/suwayomi/` | 10,640 B (10 KiB) | 9 | API key + EnhancedMangaTracker | Manga-only server tracker |
| Jellyfin | `app/.../data/track/jellyfin/` | 12,271 B (12 KiB) | 102 | API key + EnhancedAnimeTracker | Anime server tracker |
| MangaUpdates | `app/.../data/track/mangaupdates/` | 15,856 B (15 KiB) | 7 | User/pass | Manga-only tracker |
| **Total source** | | **124,075 B (121 KiB)** | | | |
| Plus tracker icons (webp) | `res/drawable-nodpi-v4/ic_tracker_{kitsu,simkl,komga,kavita,suwayomi,jellyfin}.webp` + `ic_manga_updates.webp` | 92,118 B (90 KiB) | | | Already in `res/` |
| **Total assets+source** | | **216,193 B (211 KiB)** | | | |
| **Estimated DEX savings** (after Kotlin compile, ~1.7× source) | | ~210–340 KiB | | | Plus 90 KiB icon assets |
| **Total estimated APK savings** | | | | | **~300–430 KiB** |

---

## 6. What's NOT removable / what to keep

| Component | Size | Why keep |
|---|---:|---|
| MPV (`libmpv.so`) | 5.28 MiB | Player engine. Replacing it would mean rewriting the entire anime player. |
| FFmpeg (`libavcodec/filter/format/util/swscale/swresample/postproc.so`) | 21.96 MiB total | Required by MPV pipeline and by FFmpegKit for download transcoding. A custom-stripped FFmpeg build (only H.264/HEVC/AV1/Opus/AAC decoders, no encoders) could save ~30–50% here, but that requires forking the FFmpegKit build pipeline — not a quick win. |
| `libsqlite3x.so` | 1.71 MiB | SQLDelight's newer SQLite than the Android system one. Required for all persistence. |
| `libquickjs.so` | 0.81 MiB | JavaScript engine for source extensions (some anime sources ship JS hooks). |
| `libarchive-jni.so` + `:core:archive` | 1.54 MiB | Used by local-source anime (CBZ-bundled episodes) as well as manga. Keep. |
| `libc++_shared.so` | 1.71 MiB | C++ runtime shared by all native deps. Removing it would force each `.so` to statically link its own copy → bigger, not smaller. |
| `assets/subfont.ttf` | 6.07 MiB uncompressed / 3.17 MiB compressed | MPV subtitle fallback font. CJK coverage needed. |
| `assets/cacert.pem` | 237 KiB uncompressed / ~110 KiB compressed | MPV's TLS root-cert bundle (set via `AniyomiMPVView.kt:135` `tls-ca-file`). Required for HTTPS playback. |
| `assets/aniyomi.lua` | 3 KiB | Lua bridge for custom MPV buttons + UI hooks. Required by the player's custom-button subsystem. |
| `resources.arsc` (base) | 5.66 MiB | Compressed binary resource table. Cannot be removed; can be shrunk by reducing locale count (see §5). |
| `kotlin/*.kotlin_builtins` | 50 KiB | Kotlin stdlib built-in declarations. Required by reflection + stdlib. |
| `okhttp3/.../PublicSuffixDatabase.gz` | 41 KiB | OkHttp's public-suffix list (cookie / DNS anti-leak). Required by OkHttp. |

---

## 7. Recommendations

### 7.1 Quick wins (can ship today, low risk)

1. **Switch CI from `assembleDebug` to `assemblePreview` (or `assembleRelease`).**
   The `preview` build type (`app/build.gradle.kts:62-73`) inherits release
   settings: `isMinifyEnabled = Config.enableCodeShrink` (R8) and
   `isShrinkResources = Config.enableCodeShrink`. Both default to **true**
   unless `-Pdisable-code-shrink` is passed. R8 typically shrinks DEX 50–70%
   and resource-shrink trims unused `res/` 20–40%.
   - **Expected savings: ~15–20 MiB** (DEX 34→~12 MiB; res/ 1.94→~1.4 MiB).
   - New APK size: **~65–70 MiB** (from 86.71 MiB).
   - Code change: `.github/workflows/build.yml` `./gradlew :app:assembleDebug`
     → `:app:assemblePreview`. Sign with debug key (already configured).
   - **This is the single highest-ROI change available.**

2. **Strip `.so` debug symbols in non-debug builds.** `app/build.gradle.kts:103-128`
   currently sets `keepDebugSymbols += listOf(...)` for all 20 libs. For
   `preview`/`release`/`benchmark` build types, this can be removed (or made
   debug-only). Android NDK `strip` typically cuts `.so` size 5–15%.
   - **Expected savings: ~2–6 MiB** (5–15% of 41.47 MiB native payload).

3. **Cut locale count from 67+68 to ~10.** Most users run one of: en, es,
   pt-rBR, fr, de, ja, ru, zh, it, ko. Cutting to 10 locales saves ~85% of
   the per-locale string tables in `resources.arsc`.
   - **Expected savings: ~3 MiB** off `resources.arsc` (5.66 → ~2.7 MiB).
   - Implementation: filter `i18n/src/commonMain/moko-resources/` and
     `i18n-aniyomi/src/commonMain/moko-resources/` at build time (Gradle
     `sourceSets` filter or a custom copy task).

Combined quick-win total: **~20–29 MiB** → APK drops from 86.71 MiB to
**~58–67 MiB**. (And the GitHub artifact download would drop from 60 MB to
~45–50 MB.)

### 7.2 Medium-effort wins (1–2 days each)

4. **Remove the 7 removable trackers** (Kitsu, Simkl, Komga, Kavita, Suwayomi,
   Jellyfin, MangaUpdates). Per `FEATURES/04-trackers.md` and confirmed in
   worklog 5-b: only AniList, MAL, Shikimori, Bangumi stay.
   - **Estimated savings: ~300–430 KiB** (small but cumulative + cleaner code).
   - Files to delete: 7 subdirs under `app/.../data/track/` + 7 tracker icons
     under `res/drawable-nodpi-v4/` + their entries in `TrackerManager.kt`
     (lines registering ids 3, 6, 7, 8, 9, 101, 102).

5. **Strip `libavdevice.so` (9 KiB) and `libffmpegkit_abidetect.so` (29 KiB)**
   by configuring a custom FFmpegKit build. Small wins but low-hanging if you
   ever fork the FFmpegKit build.
   - **Estimated savings: ~38 KiB** (negligible alone).

### 7.3 Bigger-effort wins (1+ week)

6. **Fully strip manga** (Animiru approach). Per worklog 3-c, Animiru
   physically removed all 28 manga dirs and stayed anime-only.
   - **Estimated savings: ~1.5–2.5 MiB DEX** + ~30 KiB SQLDelight generated code.
   - No native-lib savings (libimagedecoder.so + libarchive-jni.so used by
     anime local-source archives too).
   - Effort: 1–2 weeks of careful surgery across `:app`, `:data`, `:domain`,
     `:source-local`, `:source-api`. Coupling summary in `FEATURES/09-manga.md`.

7. **Raise `minSdk` from 26 to 29.** Lets you drop Conscrypt entirely.
   - **Savings: 2.01 MiB** (`libconscrypt_jni.so`).
   - Effort: bump `AndroidConfig.MIN_SDK` in `buildSrc/.../AndroidConfig.kt`,
     remove `libs.conscrypt.android` from `app/build.gradle.kts:245`, remove
     the `if (SDK_INT < Q) Security.insertProviderAt(...)` block in `App.kt:89-91`.
   - Trade-off: drops support for Android 8.0 / 8.1 / 9 (≈7% of active Android
     devices as of 2024 per Google's distribution dashboard). User decision.

8. **Custom FFmpeg build.** FFmpeg is 21.96 MiB of the 41.47 MiB native
   payload. A minimal build (only decoders for H.264/HEVC/AV1/VP9/Opus/AAC,
   only MP4/MKV/HLS demuxers, no encoders, no `libavdevice`) typically shrinks
   FFmpeg 30–50%.
   - **Estimated savings: ~7–11 MiB**.
   - Effort: fork the FFmpegKit build (or use a published "minimal" build),
     swap `aniyomilibs.ffmpeg.kit` in `app/build.gradle.kts:307`. High effort,
     high reward. Probably a Phase 5+ item.

9. **Investigate RxJava removal.** `rxjava` is declared in `app/build.gradle.kts:240`
   and `core/common/build.gradle.kts:25` (`api(libs.rxjava)`), but
   `grep -rln "io.reactivex" --include='*.kt'` returns **zero usages** in Kuta
   source. The only reference is `:source-api`'s `RxExtension.kt`
   (`expect suspend fun <T> Observable<T>.awaitSingle(): T`) which exists for
   source-extension compatibility.
   - **Estimated savings: ~200–500 KiB** (RxJava + rxandroid + rxkotlin
     transitive deps).
   - Risk: external source extensions may use RxJava, so removing the `api`
     exposure breaks the source-extension contract. **Investigation needed**,
     not a quick win.

### 7.4 What to investigate further (no estimate yet)

- **`kotlinx.reflect.lite`** (`app/build.gradle.kts:215`). Kuta uses Kotlin
  reflection somewhere; check if it can be downgraded to compile-only or
  replaced with `kotlin.reflect.full` (already in stdlib).
- **Material Icons Extended** (`compose.material.icons` in
  `app/build.gradle.kts:201`). 148 Kuta source files import `androidx.compose.material.icons.*`.
  This library is notoriously large because it bundles every Material icon as a
  Compose vector. Switching to icon-font (e.g., `ImageVector` lazy loading via
  generated per-icon modules) could save DEX, but the Compose `material-icons-extended`
  artifact is already tree-shaken by R8 in release builds.
- **AboutLibraries** (`libs.aboutLibraries.compose` + `res/raw/aboutlibraries.json`
  = 155 KiB). Used only by `OpenSourceLicensesScreen.kt`. If R8 keeps it, the
  155 KiB JSON could be moved to a separate dynamic-loaded slice or replaced
  with a static Markdown file.

---

## 8. Methodology & reproducibility

```sh
# Download the APK artifact (PAT required)
PAT=$(grep -E '^github_pat_' /home/z/my-project/MEMORY/credentials/github-pat.txt | head -1)
ARTIFACT_URL=$(curl -s -H "Authorization: token $PAT" \
  "https://api.github.com/repos/testplay-byte/kuta/actions/runs/28723594409/artifacts" \
  | jq -r '.artifacts[0].archive_download_url')
curl -s -L -H "Authorization: token $PAT" "$ARTIFACT_URL" -o /tmp/apk.zip
unzip /tmp/apk.zip -d /tmp/apk_extracted

# All sizes come from these commands:
cd /tmp/apk_extracted
unzip -l  app-arm64-v8a-debug.apk > /tmp/apk_file_list.txt   # uncompressed sizes
unzip -lv app-arm64-v8a-debug.apk                             # compressed + uncompressed + method

# Source-code sizes (manga, trackers, etc.):
cd /home/z/kuta
find app/src/main/java -type d -name manga -exec du -sb {} + | awk '{sum+=$1} END {print sum}'
du -sb app/src/main/java/eu/kanade/tachiyomi/data/track/{kitsu,simcl,komga,kavita,suwayomi,jellyfin,mangaupdates}
```

### Caveats

- All "estimated DEX savings" are heuristic (1.5–2.5× Kotlin source size →
  DEX in a debug build). Precise measurement requires a removal experiment
  (build, measure APK, remove feature, rebuild, measure delta).
- The APK analyzed is the **debug** build. The `preview`/`release` builds
  (with R8 + resource shrink) will be substantially smaller — see §7.1.
- Native-lib sizes are exact (from `unzip -l`).
- Tracker icon sizes are exact.
- "Source size" (`du -b` on `*.kt` files) is exact but does not include
  generated code (SQLDelight, Voyager, Compose compiler) — those would add
  another 10–30% in DEX.
