# CI info

CI is defined in `.github/workflows/build.yml`.

## What it does

- **Triggers**: on push to any branch, and on pull requests targeting `main`.
- **Runner**: `ubuntu-latest`.
- **Steps**:
  1. Checkout the repo.
  2. Set up JDK 21 (Temurin). Gradle's foojay toolchain resolver auto-provisions
     whatever `jvmToolchain(N)` the project declares.
  3. Restore/save cache for `~/.gradle/caches` and `~/.gradle/wrapper` (key based on
     gradle scripts, the wrapper, and all version catalogs under `gradle/*.versions.toml`).
  4. Run `./gradlew :app:assembleDebug --no-daemon`.
  5. If the build succeeds, upload `app/build/outputs/apk/debug/app-debug.apk` as a
     workflow artifact named `app-debug-apk`.
- **Env**: `GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.parallel=true -Dorg.gradle.caching=true"`.

## Expected behavior

- A clean build takes ~15–25 minutes on GitHub's runners (longer the first time due
  to no cache + foojay downloading the toolchain JDK).
- The APK artifact is downloadable from the run's summary page on GitHub Actions.

## Why debug-only

We're not setting up release signing yet. Debug builds are the sanity check that
the toolchain + project config are intact. Release signing comes in a later phase.

## Upstream workflows (removed)

Aniyomi's upstream repo shipped `.github/workflows/build_push.yml` and
`build_pull_request.yml`. These were **removed** in the initial setup commit because
they build `assembleRelease` (heavy, with minify/shrink), run `spotlessCheck` + unit
tests, and would run in parallel with our debug build on every push — creating noise
and risk of misleading failures. They remain in git history if we ever want them
back.
