# Build System & Build Variants

This document explains the **Kuta** (Aniyomi fork) build system: the
`buildSrc/` convention-plugin setup, the resolved SDK/JVM values, build types,
flavors, ABI splits, and version catalogs.

> All values below were read directly from source files. No guessing.
> Repository root: `/home/z/kuta`.

---

## 1. `buildSrc/` overview

This fork uses **`buildSrc/`** (not a `build-logic/` composite build). It is a
standard Gradle `kotlin-dsl` project that ships both:

1. **Precompiled script plugins** (`*.gradle.kts` files at the root of
   `buildSrc/src/main/kotlin/`) — these become Gradle plugin IDs at
   `mihon.*`.
2. **Kotlin helper classes** under the `mihon.buildlogic` package.

### File map

| Path | Role |
|---|---|
| `buildSrc/build.gradle.kts` | Declares `kotlin-dsl` plugin; pulls AGP, Kotlin, Compose-compiler, Spotless into the buildSrc classpath via the version catalogs. |
| `buildSrc/settings.gradle.kts` | Re-declares all 5 version catalogs (`libs`, `androidx`, `compose`, `kotlinx`, `aniyomilibs`) so buildSrc can resolve them. |
| `buildSrc/src/main/kotlin/mihon.android.application.gradle.kts` | Precompiled script plugin — `mihon.android.application`. |
| `buildSrc/src/main/kotlin/mihon.android.application.compose.gradle.kts` | Precompiled script plugin — `mihon.android.application.compose`. |
| `buildSrc/src/main/kotlin/mihon.library.gradle.kts` | Precompiled script plugin — `mihon.library`. |
| `buildSrc/src/main/kotlin/mihon.library.compose.gradle.kts` | Precompiled script plugin — `mihon.library.compose`. |
| `buildSrc/src/main/kotlin/mihon.benchmark.gradle.kts` | Precompiled script plugin — `mihon.benchmark`. |
| `buildSrc/src/main/kotlin/mihon.code.lint.gradle.kts` | Precompiled script plugin — `mihon.code.lint` (Spotless/ktlint). |
| `buildSrc/src/main/kotlin/mihon/buildlogic/AndroidConfig.kt` | Central SDK / NDK / JVM constants. |
| `buildSrc/src/main/kotlin/mihon/buildlogic/BuildConfig.kt` | Project properties → boolean flags (updater, code-shrink, dependency-info). |
| `buildSrc/src/main/kotlin/mihon/buildlogic/Commands.kt` | Git command helpers (`getCommitCount`, `getGitSha`, `getBuildTime`). |
| `buildSrc/src/main/kotlin/mihon/buildlogic/ProjectExtensions.kt` | `configureAndroid`, `configureCompose`, `configureTest` functions + catalog accessors. |
| `buildSrc/src/main/kotlin/mihon/buildlogic/tasks/LocalesConfigTask.kt` | `generateLocalesConfig` task — emits `locales_config.xml` from moko-resources. |

### `buildSrc/build.gradle.kts`

```kotlin
plugins { `kotlin-dsl` }

dependencies {
    implementation(androidx.gradle)                       // AGP
    implementation(kotlinx.gradle)                        // kotlin-gradle-plugin
    implementation(kotlinx.compose.compiler.gradle)       // compose-compiler-gradle-plugin
    implementation(libs.spotless.gradle)
    implementation(gradleApi())

    // Workaround to expose the catalog JARs to the kotlin-dsl classpath
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
    implementation(files(androidx.javaClass.superclass.protectionDomain.codeSource.location))
    implementation(files(compose.javaClass.superclass.protectionDomain.codeSource.location))
    implementation(files(kotlinx.javaClass.superclass.protectionDomain.codeSource.location))
}
```

The four `implementation(files(...))` lines are the well-known workaround for
making version-catalog accessors available inside precompiled script plugins
(which compile against the buildSrc classpath, not the root project's).

---

## 2. Convention (precompiled) plugins

Each `*.gradle.kts` file in `buildSrc/src/main/kotlin/` becomes a plugin whose
ID equals its filename minus the `.gradle.kts` suffix. So
`mihon.android.application.gradle.kts` → plugin ID `mihon.android.application`.

### `mihon.android.application`
Source: `buildSrc/src/main/kotlin/mihon.android.application.gradle.kts`
```kotlin
plugins {
    id("com.android.application")
    kotlin("android")
    id("mihon.code.lint")
}
android {
    defaultConfig { targetSdk = AndroidConfig.TARGET_SDK }
    configureAndroid(this)
    configureTest()
}
```

### `mihon.android.application.compose`
Source: `buildSrc/src/main/kotlin/mihon.android.application.compose.gradle.kts`
```kotlin
plugins {
    id("com.android.application")
    kotlin("android")
    id("mihon.code.lint")
}
android { configureCompose(this) }
```
Note: This plugin does **not** apply `mihon.android.application`; it only sets
up Compose. Modules that need both apply them together (the `:app` module does
exactly this — `app/build.gradle.kts:7-8`).

### `mihon.library`
Source: `buildSrc/src/main/kotlin/mihon.library.gradle.kts`
```kotlin
plugins {
    id("com.android.library")
    id("mihon.code.lint")
}
android {
    configureAndroid(this)
    configureTest()
}
```

### `mihon.library.compose`
Source: `buildSrc/src/main/kotlin/mihon.library.compose.gradle.kts`
```kotlin
plugins {
    id("com.android.library")
    id("mihon.code.lint")
}
android { configureCompose(this) }
```

### `mihon.benchmark`
Source: `buildSrc/src/main/kotlin/mihon.benchmark.gradle.kts`
```kotlin
plugins {
    id("com.android.test")
    kotlin("android")
    id("mihon.code.lint")
}
android {
    configureAndroid(this)
    configureTest()
}
```
Used by `:macrobenchmark`.

### `mihon.code.lint`
Source: `buildSrc/src/main/kotlin/mihon.code.lint.gradle.kts`
Applies the Spotless plugin and configures:
- `kotlin { ktlint(libs.ktlint.core.get().version); trimTrailingWhitespace(); endWithNewline() }`
  targeting `**/*.kt` and `**/*.kts` (excluding `**/build/**`).
- An XML format pass that excludes generated moko-resources XML
  (`src/commonMain/moko-resources` minus `/base/**`) and `**/build/**/*.xml`.

Every convention plugin above transitively applies `mihon.code.lint`, so all
modules get consistent formatting.

---

## 3. The `mihon.buildlogic` package (Kotlin helpers)

### `AndroidConfig.kt`  — **the single source of SDK/JVM truth**
Full file (`buildSrc/src/main/kotlin/mihon/buildlogic/AndroidConfig.kt`):
```kotlin
object AndroidConfig {
    const val COMPILE_SDK = 35
    const val TARGET_SDK = 34
    const val MIN_SDK = 26
    const val NDK = "27.1.12297006"
    const val BUILD_TOOLS = "35.0.1"

    val JavaVersion = GradleJavaVersion.VERSION_17
    val JvmTarget   = KotlinJvmTarget.JVM_17
}
```

### `BuildConfig.kt`
Exposes a `Project.Config: BuildConfig` extension that reads Gradle project
properties:
```kotlin
interface BuildConfig {
    val enableUpdater: Boolean
    val enableCodeShrink: Boolean
    val includeDependencyInfo: Boolean
}
val Project.Config: BuildConfig get() = object : BuildConfig {
    override val enableUpdater        = project.hasProperty("enable-updater")
    override val enableCodeShrink     = !project.hasProperty("disable-code-shrink")
    override val includeDependencyInfo = project.hasProperty("include-dependency-info")
}
```
Consumed in `app/build.gradle.kts` to gate minification, the updater flag, and
`dependenciesInfo` blocks.

### `Commands.kt`
Three `Project`-extension helpers that shell out to `git` via
`providers.exec { ... }`:
- `getCommitCount()` → `git rev-list --count HEAD`
- `getGitSha()` → `git rev-parse --short HEAD`
- `getBuildTime(useLastCommitTime: Boolean)` → ISO-8601 UTC timestamp, either
  from `git log -1 --format=%ct` or `LocalDateTime.now(UTC)`.

Used in `app/build.gradle.kts:25-27, 57, 72` to populate `BuildConfig` fields.

### `ProjectExtensions.kt`
The workhorse. Defines:

- Catalog accessors (so the rest of buildSrc can use them tersely):
  ```kotlin
  val Project.androidx get() = the<LibrariesForAndroidx>()
  val Project.compose  get() = the<LibrariesForCompose>()
  val Project.kotlinx  get() = the<LibrariesForKotlinx>()
  val Project.libs     get() = the<LibrariesForLibs>()
  ```
- `configureAndroid(commonExtension)` — sets `compileSdk`, `buildToolsVersion`,
  `defaultConfig.minSdk`, `ndk.version`, `compileOptions` (Java 17 + core
  library desugaring), wires Kotlin compile `jvmTarget = JVM_17`, adds
  `-Xcontext-receivers` and `-opt-in=kotlin.RequiresOptIn`, honors the
  `warningsAsErrors` gradle property, and adds the desugar dependency.
- `configureCompose(commonExtension)` — applies the compose-compiler plugin,
  enables `buildFeatures.compose`, adds the Compose BOM platform dependency,
  sets the `OptimizeNonSkippingGroups` feature flag, and (optionally, behind
  the `enableComposeCompilerMetrics` / `enableComposeCompilerReports` gradle
  properties) writes metrics/reports under `rootProject.buildDir/{compose-metrics,compose-reports}`.
- `configureTest()` — forces `useJUnitPlatform()` and logs PASSED/SKIPPED/FAILED.
- `val Project.generatedBuildDir: File` — `build/generated/mihon`.

### `tasks/LocalesConfigTask.kt`
Registers a `generateLocalesConfig` task that scans
`src/commonMain/moko-resources/*/strings.xml`, skips empty `<resources/>`
files, maps the `base` folder to `en`, normalizes `-r`/`+` locale suffixes,
and writes `<locale-config>` XML to `xml/locales_config.xml` in the supplied
output resource dir. Wired into `preBuild` by `:i18n` and `:i18n-aniyomi`.

---

## 4. Resolved SDK & JVM values  ⭐ (the whole point)

| Setting | Value | Source |
|---|---|---|
| **`compileSdk`** | **35** | `AndroidConfig.COMPILE_SDK` → applied in `ProjectExtensions.kt:28` (`compileSdk = AndroidConfig.COMPILE_SDK`) |
| **`minSdk`** | **26** | `AndroidConfig.MIN_SDK` → applied in `ProjectExtensions.kt:32` (`defaultConfig { minSdk = AndroidConfig.MIN_SDK }`) |
| **`targetSdk`** | **34** | `AndroidConfig.TARGET_SDK` → applied in `mihon.android.application.gradle.kts:14` (`defaultConfig { targetSdk = AndroidConfig.TARGET_SDK }`). *Note: only the application convention plugin sets targetSdk; library modules do not.* |
| **NDK version** | `27.1.12297006` | `AndroidConfig.NDK` → `ProjectExtensions.kt:34` |
| **Build tools** | `35.0.1` | `AndroidConfig.BUILD_TOOLS` → `ProjectExtensions.kt:29` |
| **Java source/target compatibility** | `VERSION_17` | `AndroidConfig.JavaVersion` → `ProjectExtensions.kt:39-40` |
| **Kotlin `jvmTarget`** | `JVM_17` | `AndroidConfig.JvmTarget` → `ProjectExtensions.kt:47` (`jvmTarget.set(AndroidConfig.JvmTarget)`) |
| **`jvmToolchain(N)`** | **Not used anywhere.** | A repo-wide `rg "jvmToolchain"` finds the string only in `SETUP/ci-info.md` (documentation). JVM versioning is done via `jvmTarget` + `sourceCompatibility`/`targetCompatibility`, not via a toolchain. JDK provisioning for *building* still goes through the `foojay-resolver-convention` plugin in `settings.gradle.kts:42`, but the *target* bytecode is fixed at 17. |

> ⚠️ **Note on `targetSdk < compileSdk`:** `compileSdk = 35` while
> `targetSdk = 34`. This is intentional and matches upstream Aniyomi — the app
> builds against the API 35 SDK (to satisfy AGP 8.9 / Build-Tools 35.0.1
> requirements) but still declares runtime behavior compatibility with API 34
> until the app is fully audited against API 35 behavior changes.

---

## 5. Build types  (`app/build.gradle.kts:45–86`)

The `:app` module declares four build types. Three are the standard Android
ones (`debug`, `release`) plus two custom ones (`preview`, `benchmark`).

| Build type | `applicationIdSuffix` | `versionNameSuffix` | minify | shrinkResources | debuggable | signing | profileable | Notes |
|---|---|---|---|---|---|---|---|---|
| `debug` (built-in) | `.dev` | `-${commitCount}` | (false) | (false) | (true, default) | debug keystore (default) | no | `isPseudoLocalesEnabled = true` |
| `release` (built-in) | — | — | `Config.enableCodeShrink` (default **true**; set `-Pdisable-code-shrink` to disable) | same as minify | (false, default) | release keystore (not declared here — supplied via `signing.properties`/CI in practice) | no | `proguardFiles("proguard-android-optimize.txt", "proguard-rules.pro")`; `BUILD_TIME` = last commit time |
| `preview` (custom) | `.debug` | inherits debug's (`-${commitCount}`) | inherits release (so on by default) | inherits release | inherits release (false) | **debug** keystore | no | `initWith(release)` then overrides; `matchingFallbacks = ["release"]`; `BUILD_TIME` = current time |
| `benchmark` (custom) | `.benchmark` | `-benchmark` | inherits release | inherits release | **false** (explicit) | **debug** keystore | **true** (`isProfileable = true`) | `initWith(release)`; `matchingFallbacks = ["release"]`; intended for macrobenchmark runs |

### Source

```kotlin
// app/build.gradle.kts:45-86
buildTypes {
    val debug by getting {
        applicationIdSuffix = ".dev"
        versionNameSuffix = "-${getCommitCount()}"
        isPseudoLocalesEnabled = true
    }
    val release by getting {
        isMinifyEnabled = Config.enableCodeShrink
        isShrinkResources = Config.enableCodeShrink
        proguardFiles("proguard-android-optimize.txt", "proguard-rules.pro")
        buildConfigField("String", "BUILD_TIME", "\"${getBuildTime(useLastCommitTime = true)}\"")
    }
    val commonMatchingFallbacks = listOf(release.name)
    create("preview") {
        initWith(release)
        applicationIdSuffix = ".debug"
        versionNameSuffix = debug.versionNameSuffix
        signingConfig = debug.signingConfig
        matchingFallbacks.addAll(commonMatchingFallbacks)
        buildConfigField("String", "BUILD_TIME", "\"${getBuildTime(useLastCommitTime = false)}\"")
    }
    create("benchmark") {
        initWith(release)
        isDebuggable = false
        isProfileable = true
        versionNameSuffix = "-benchmark"
        applicationIdSuffix = ".benchmark"
        signingConfig = debug.signingConfig
        matchingFallbacks.addAll(commonMatchingFallbacks)
    }
}
```

### Source-set sharing

`preview` and `benchmark` reuse the `debug` source set's resources
(`app/build.gradle.kts:88–91`):
```kotlin
sourceSets {
    getByName("preview").res.srcDirs("src/debug/res")
    getByName("benchmark").res.srcDirs("src/debug/res")
}
```

### Application ID per variant (effective)

| Variant | Application ID |
|---|---|
| `debug` | `app.kuta.dev` |
| `release` | `app.kuta` |
| `preview` | `app.kuta.debug` |
| `benchmark` | `app.kuta.benchmark` |

(Base `applicationId = "app.kuta"` — `app/build.gradle.kts:20`.)

---

## 6. Build flavors  — **none declared**

A repo-wide search for `flavorDimensions` and `productFlavors {` returns **zero
matches** in any `build.gradle.kts`:

```
$ rg -n "flavorDimensions|productFlavors\s*\{" /home/z/kuta \
       --glob '!**/build/**' --glob '!**/.gradle/**' --glob '!**/.idea/**'
(no matches, exit code 1)
```

No `productFlavors` block exists in `app/build.gradle.kts`, in any module, or in
any `buildSrc` convention plugin. There is no `dev/` or `standard/` source set
under `app/src/` (only `debug/`, `main/`, `test/`).

### ⚠️ Stale flavor references (dead code from upstream)

`app/build.gradle.kts:315–329` contains an `androidComponents` block that
references flavors `"default" → "dev"` and `"default" → "standard"`:

```kotlin
androidComponents {
    beforeVariants { variantBuilder ->
        if (variantBuilder.buildType == "benchmark") {
            variantBuilder.enable = variantBuilder.productFlavors.containsAll(
                listOf("default" to "dev"),
            )
        }
    }
    onVariants(selector().withFlavor("default" to "standard")) {
        it.packaging.resources.excludes.add("META-INF/*.version")
    }
}
```

Because no `flavorDimensions`/`productFlavors` are declared, these selectors
**never match any variant** — i.e. this entire `androidComponents` block is a
no-op carried over from upstream Aniyomi. It is worth either re-introducing the
flavors or deleting this block during the fork cleanup.

---

## 7. ABI splits  (`app/build.gradle.kts:93–101`)

```kotlin
// FORK: Only build arm64-v8a for faster debug builds. Revisit for release in Phase 5.
splits {
    abi {
        isEnable = true
        isUniversalApk = false
        reset()
        include("arm64-v8a")
    }
}
```

Key facts:
- ABI splits are **enabled**.
- A **universal APK is NOT produced** (`isUniversalApk = false`).
- `reset()` clears the default ABI list, then `include("arm64-v8a")` adds only
  **arm64-v8a**.
- This applies to **all build types** (there's no per-type override) — so
  release, preview, and benchmark APKs are also arm64-v8a only.
- The `// FORK:` marker is a known fork-level deviation from upstream Aniyomi,
  which historically shipped `armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`. The
  comment explicitly says to revisit for release in "Phase 5".
- `packaging.jniLibs.keepDebugSymbols` (lines 103–128) lists ~20 native libs
  whose `.so` debug symbols should be retained (`libmpv`, `libavcodec`,
  `libffmpegkit`, `libarchive-jni`, `libquickjs`, `libsqlite3x`, etc.).

---

## 8. Other `:app` build features  (`app/build.gradle.kts:150–163`)

```kotlin
buildFeatures {
    viewBinding = true
    buildConfig = true
    aidl = false
    renderScript = false
    shaders = false
}
lint {
    abortOnError = false
    checkReleaseBuilds = false
}
```

`dependenciesInfo` (lines 145–148) — both `includeInApk` and `includeInBundle`
are gated behind `Config.includeDependencyInfo` (i.e. only true when the
`include-dependency-info` gradle property is set).

The `:app` module's `kotlin { compilerOptions { ... } }` block (lines 166–182)
adds a long list of Compose/coroutines/serialization opt-ins globally.

---

## 9. Version catalogs

Five catalogs are in use. Four are declared in `settings.gradle.kts:19–32`; the
fifth (`libs`) is the default catalog auto-loaded from `gradle/libs.versions.toml`.
`buildSrc/settings.gradle.kts` re-declares all five for the buildSrc project.

| Accessor | Gradle type-safe class | TOML file | Headline versions |
|---|---|---|---|
| `libs` | `LibrariesForLibs` | `gradle/libs.versions.toml` | okhttp 5.0.0-alpha.14, sqldelight 2.0.2, sqlite 2.4.0, coil 3.1.0, voyager 1.0.1, aboutlib 11.6.3, leakcanary 2.14, moko 0.24.5, spotless 7.0.2, ktlint-core 1.5.0, shizuku 13.1.0 |
| `kotlinx` | `LibrariesForKotlinx` | `gradle/kotlinx.versions.toml` | **kotlin 2.2.0**, serialization 1.9.0, xml-serialization 0.90.3, coroutines BOM 1.10.1, immutables 0.3.8 |
| `androidx` | `LibrariesForAndroidx` | `gradle/androidx.versions.toml` | **AGP 8.9.0**, lifecycle 2.8.7, paging 3.3.6, work 2.10.0, benchmark-macro 1.3.3, core-ktx 1.15.0, appcompat 1.7.0, splashscreen 1.0.1 |
| `compose` | `LibrariesForCompose` | `gradle/compose.versions.toml` | Compose BOM 2025.03.00, activity-compose 1.10.1, glance 1.1.1 |
| `aniyomilibs` | `LibrariesForAniyomilibs` | `gradle/aniyomi.versions.toml` | aniyomi-mpv-lib 1.18.n, ffmpeg-kit 1.18, arthenica-smartexceptions 0.2.1, constraint-layout 1.1.0, media 1.7.0, seeker 1.2.2, truetypeparser 2.1.4 |

### Plugin entries inside the catalogs

`libs` (`gradle/libs.versions.toml:100`):
```toml
[plugins]
aboutLibraries = { id = "com.mikepenz.aboutlibraries.plugin", version.ref = "aboutlib_version" }
sqldelight      = { id = "app.cash.sqldelight",                  version.ref = "sqldelight" }
moko            = { id = "dev.icerock.mobile.multiplatform-resources", version.ref = "moko" }
```

`kotlinx` (`gradle/kotlinx.versions.toml:29`):
```toml
[plugins]
android           = { id = "org.jetbrains.kotlin.android",          version.ref = "kotlin_version" }
compose-compiler  = { id = "org.jetbrains.kotlin.plugin.compose",   version.ref = "kotlin_version" }
serialization     = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin_version" }
```

`androidx`, `compose`, `aniyomilibs` have **no `[plugins]` section** — they only
declare `[versions]` and `[libraries]`.

### Catalog usage examples in module scripts

- `app/build.gradle.kts:11` — `alias(libs.plugins.aboutLibraries)`
- `data/build.gradle.kts:5` — `alias(libs.plugins.sqldelight)`
- `i18n/build.gradle.kts:8` and `i18n-aniyomi/build.gradle.kts:8` — `alias(libs.plugins.moko)`
- `app/build.gradle.kts:10` — `kotlin("plugin.serialization")` (resolved via
  the root `build.gradle.kts:8` `alias(kotlinx.plugins.serialization) apply false`)
- Convention plugins reference libraries directly via the `the<LibrariesFor...>()`
  accessors (see `ProjectExtensions.kt:21–24`).

---

## 10. Putting it together — how a typical library module is wired

Take `:domain` (`domain/build.gradle.kts`):

```kotlin
plugins {
    id("mihon.library")              // → com.android.library + mihon.code.lint
                                     //   + configureAndroid (compileSdk 35, minSdk 26,
                                     //     Java 17, desugaring, JVM_17) + configureTest
    kotlin("android")                // resolved via root buildscript classpath
    kotlin("plugin.serialization")
}

android {
    namespace = "tachiyomi.domain"
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    implementation(projects.sourceApi)
    implementation(projects.core.common)
    // ... catalog deps
}
```

So a single `id("mihon.library")` line pulls in: the Android Gradle plugin,
Kotlin android, Spotless/ktlint, `compileSdk = 35`, `minSdk = 26`,
`buildToolsVersion = 35.0.1`, NDK pin, Java/Kotlin 17, core-library desugaring,
default compiler args, and JUnit-Platform test config. The module only needs to
declare its namespace, deps, and any module-specific options.

---

## 11. Summary cheat-sheet

- **Toolchain:** Kotlin 2.2.0, AGP 8.9.0, Gradle wrapper 8.13.
- **compileSdk = 35**, **minSdk = 26**, **targetSdk = 34** (all in
  `AndroidConfig.kt`).
- **JVM target = 17** (Java `VERSION_17` + Kotlin `JVM_17`).
- **No `jvmToolchain(N)` calls** anywhere in the build.
- **NDK 27.1.12297006**, **Build-Tools 35.0.1**.
- Build types: `debug`, `release`, `preview`, `benchmark` (4 total).
- Build flavors: **none declared** (the `androidComponents` flavor references in
  `app/build.gradle.kts:315–329` are stale no-ops).
- ABI splits: `arm64-v8a` only (FORK marker); no universal APK.
- Convention plugins live in `buildSrc/` (not `build-logic/`), as precompiled
  script plugins under `buildSrc/src/main/kotlin/`.
- 5 version catalogs: `libs`, `kotlinx`, `androidx`, `compose`, `aniyomilibs`.
- 13 modules total; `:app` is the only application module and depends on 11 of
  the other 12 (`:macrobenchmark` is the only one it does not depend on).
