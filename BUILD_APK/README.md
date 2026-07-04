# BUILD_APK — where to find APKs

Debug APKs are produced by GitHub Actions CI (`.github/workflows/build.yml`) and
uploaded as workflow artifacts. **Binary APKs are never committed to this repo.**

## How to download a debug APK

1. Go to https://github.com/testplay-byte/kuta/actions
2. Click the latest **"Build APK"** run (the one with a green ✓).
3. Scroll to the **Artifacts** section at the bottom of the run summary.
4. Click **`app-debug-apk`** to download a ZIP.
5. Unzip it — inside is `app-arm64-v8a-debug.apk`.

## About this APK

- **Architecture**: `arm64-v8a` only. This covers essentially every modern Android
  phone (Android 10+, 64-bit ARM). It will NOT install on old 32-bit ARM devices
  (`armeabi-v7a`) or x86 emulators.
- **Build type**: `debug` (signed with the Android debug key, debuggable). Safe to
  sideload; not for distribution.
- **Installed package name**: `app.kuta.dev` (applicationId `app.kuta` + the debug
  build-type `.dev` suffix).
- **Variant detail**: this repo is configured (in `app/build.gradle.kts`, `splits`
  block) to build arm64-v8a **only** for faster debug iteration. Release builds
  (Phase 5) will revisit ABI coverage and signing.

## Retention

GitHub Actions artifacts are kept for **90 days** by default. Older runs' APKs are
automatically deleted by GitHub. For a specific older build, download it before the
90-day window expires.

## Future: permanent storage

GitHub Releases (for tagged versions) are the plan for permanent APK storage, to be
set up in a later phase alongside release signing. Not configured yet.
