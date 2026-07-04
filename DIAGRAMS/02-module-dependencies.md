# 02 — Gradle Module Dependencies

The Kuta fork inherits Aniyomi's 13-module Gradle layout (declared in
`settings.gradle.kts`). The graph is a clean DAG — no cycles, no composite
builds. `:app` is the only `com.android.application` module and pulls in 11 of
the other 12 modules directly; `:macrobenchmark` is a `com.android.test`
module that targets `:app` via `targetProjectPath = ":app"` (not an
`implementation(project(...))` line). The leaf-most modules (`:i18n`,
`:i18n-aniyomi`, `:core:archive`) are depended-on by many but depend on
nothing project-internal. Arrows point from the dependent to its dependency
(i.e. `A --> B` means "A depends on B"). The four KMP modules (`:i18n`,
`:i18n-aniyomi`, `:source-api`, `:source-local`) only declare their project
deps under `androidMain` (or split commonMain / androidMain for `:source-local`).

```mermaid
graph BT
    classDef app fill:#ffe6e6,stroke:#cc3333,color:#000
    classDef core fill:#e8f0ff,stroke:#3b6fb5,color:#000
    classDef domain fill:#eafaf0,stroke:#2e8b57,color:#000
    classDef data fill:#fff4e0,stroke:#cc8a00,color:#000
    classDef source fill:#f3eaff,stroke:#7a3fb5,color:#000
    classDef i18n fill:#fff7e6,stroke:#aa8800,color:#000
    classDef presentation fill:#eafafa,stroke:#2e8585,color:#000
    classDef test fill:#eee,stroke:#999,color:#000

    %% Leaves
    i18n[":i18n<br/>Moko resources (core strings)"]
    i18nAniyomi[":i18n-aniyomi<br/>Aniyomi-specific strings<br/>(AYMR generator)"]
    coreArchive[":core:archive<br/>libarchive wrapper<br/>(CBZ/CBR/EPUB)"]
    class i18n,i18nAniyomi,coreArchive i18n

    %% Core utility layer
    coreCommon[":core:common<br/>OkHttp / RxJava / prefs /<br/>quickjs / FFmpeg-kit"]
    class coreCommon core
    coreCommon --> i18n

    coreMetadata[":core-metadata<br/>metadata parsing"]
    class coreMetadata core
    coreMetadata --> sourceApi

    %% Source contract layer
    sourceApi[":source-api (KMP)<br/>AnimeSource / MangaSource /<br/>SAnime / Video / Hoster"]
    class sourceApi source
    sourceApi -- "androidMain" --> coreCommon

    %% Domain layer
    domain[":domain<br/>models + repos + use-cases"]
    class domain domain
    domain --> sourceApi
    domain --> coreCommon

    %% Data layer
    data[":data<br/>SQLDelight (2 DBs) +<br/>handlers + mappers + repos"]
    class data data
    data --> sourceApi
    data --> domain
    data --> coreCommon

    %% Source impl layer
    sourceLocal[":source-local (KMP)<br/>reads anime/manga from disk"]
    class sourceLocal source
    sourceLocal -- "commonMain" --> sourceApi
    sourceLocal -- "commonMain api" --> i18n
    sourceLocal -- "commonMain api" --> i18nAniyomi
    sourceLocal -- "androidMain" --> coreArchive
    sourceLocal -- "androidMain" --> coreCommon
    sourceLocal -- "androidMain" --> coreMetadata
    sourceLocal -- "androidMain" --> domain

    %% Presentation layer
    presentationCore[":presentation-core<br/>forked M3 components +<br/>AdaptiveSheet / Pill / etc."]
    class presentationCore presentation
    presentationCore -- "api" --> coreCommon
    presentationCore -- "api" --> i18n

    presentationWidget[":presentation-widget<br/>Glance home-screen widgets"]
    class presentationWidget presentation
    presentationWidget --> coreCommon
    presentationWidget --> domain
    presentationWidget --> presentationCore
    presentationWidget -- "api" --> i18n
    presentationWidget -- "api" --> i18nAniyomi

    %% App layer
    app[":app<br/>application shell — UI, player (MPV),<br/>downloads, trackers, extensions, DI"]
    class app app
    app --> i18n
    app --> i18nAniyomi
    app --> coreArchive
    app --> coreCommon
    app --> coreMetadata
    app --> sourceApi
    app --> sourceLocal
    app --> data
    app --> domain
    app --> presentationCore
    app --> presentationWidget

    %% Test layer
    macrobenchmark[":macrobenchmark<br/>com.android.test<br/>baseline profiles"]
    class macrobenchmark test
    macrobenchmark -- "targetProjectPath = :app" --> app
```

## Notes

- **No `includeBuild`**: all 13 modules live in the single root Gradle build.
  `buildSrc/` (not a composite `build-logic/`) hosts the convention plugins
  under `mihon.buildlogic.*`.
- **`TYPESAFE_PROJECT_ACCESSORS`** preview is enabled, which is why the
  `build.gradle.kts` files write `projects.core.common` / `projects.i18nAniyomi`
  instead of `project(":core:common")`.
- **KMP modules**: `:i18n`, `:i18n-aniyomi`, `:source-api`, `:source-local`
  use `kotlin("multiplatform")` but only target `androidTarget()`. Their
  project dependencies are scoped under `androidMain` (or split
  commonMain / androidMain for `:source-local`), which is why some arrows are
  labelled with the source set.
- **Five version catalogs**: `libs` (default) + `kotlinx` + `androidx` +
  `compose` + `aniyomilibs`. The latter four are declared in
  `settings.gradle.kts` and re-declared in `buildSrc/settings.gradle.kts` so
  the `kotlin-dsl` buildSrc project can resolve them too.
- **Fork marker**: `app/build.gradle.kts:93–101` restricts `splits.abi` to
  `arm64-v8a` only (upstream ships multiple ABIs). `applicationId` is
  `app.kuta` (forked from Aniyomi's `xyz.jmir.tachiyomi.mi`); the package
  namespace `eu.kanade.tachiyomi` and `rootProject.name = "Aniyomi"` are
  unchanged.
- **Layering** (top-down): contract (`:source-api`) → core utility
  (`:core:common`, `:core:archive`, `:core-metadata`) → domain (`:domain`)
  → data (`:data`) → source impl (`:source-local`) → i18n (leaves) →
  presentation (`:presentation-core`, `:presentation-widget`) → app (`:app`)
  → test (`:macrobenchmark`).
