# 07 — Extension Lifecycle

The anime extension subsystem has five distinct stages: **Install**, **Load**,
**Register**, **Update**, and **Uninstall**. The diagram below traces each
stage from its entry point through the manager singletons
(`AnimeExtensionManager`, `AndroidAnimeSourceManager`) and the loader
(`AnimeExtensionLoader`) down to the point where an `AnimeSource` instance is
available to the player / browse / search pipelines. The install path is
shared by all three trigger points (manual install from the Extensions tab,
an update, or an `aniyomi://add-repo` deep link); only the upstream "where
did the APK come from" differs. The loader is the security-sensitive stage —
it validates the lib version range (12..16), runs the SHA-256 signature
trust check via `TrustAnimeExtension`, and instantiates the source class
through a parent-last `ChildFirstPathClassLoader` (falling back to a plain
`PathClassLoader` on `LinkageError`).

```mermaid
graph TD
    classDef trigger fill:#fff7e6,stroke:#aa8800,color:#000
    classDef api fill:#e8f0ff,stroke:#3b6fb5,color:#000
    classDef installer fill:#eafaf0,stroke:#2e8b57,color:#000
    classDef loader fill:#f3eaff,stroke:#7a3fb5,color:#000
    classDef manager fill:#fff4e0,stroke:#cc8a00,color:#000
    classDef source fill:#ffe6e6,stroke:#cc3333,color:#000

    %% ---------- Triggers ----------
    subgraph TRIG["Install Triggers"]
        AddRepoUI["Extensions tab → Add repo<br/>(Settings → Extensions)"]
        AddRepoDeepLink["aniyomi://add-repo deep link<br/>(MainActivity.handleIntentAction)"]
        TapInstall["Tap 'Install' on Available extension<br/>(AnimeExtensionsScreen)"]
        TapUpdate["Tap 'Update' on outdated Installed extension"]
    end
    class AddRepoUI,AddRepoDeepLink,TapInstall,TapUpdate trigger

    %% ---------- INSTALL stage ----------
    subgraph INSTALL["Stage 1 — Install"]
        RepoInsert["CreateAnimeExtensionRepo.await(indexUrl)<br/>— regex ^https://.*/index.min.json$<br/>— fetch repo.json for signingKeyFingerprint<br/>— INSERT into extension_repos"]
        ApiFetch["AnimeExtensionApi.findExtensions()<br/>— aggregates index.min.json across all repos<br/>— produces List<AnimeExtension.Available>"]
        ApkDownload["AnimeExtensionApi.getApkUrl<br/>→ download {baseUrl}/apk/{apkName}"]
        Installer["AnimeExtensionInstaller.install(apkFile, pkgName)<br/>— dispatches to:<br/>  PackageInstallerInstallerAnime (default)<br/>  ShizukuInstallerAnime (if Shizuku bound)"]
        InstallReceiver["AnimeExtensionInstallReceiver<br/>(PACKAGE_ADDED / REPLACED / REMOVED broadcast)"]
    end
    class RepoInsert,ApiFetch,ApkDownload,Installer,InstallReceiver installer

    AddRepoUI --> RepoInsert
    AddRepoDeepLink --> RepoInsert
    RepoInsert --> ApiFetch
    TapInstall --> ApkDownload
    TapUpdate --> ApkDownload
    ApkDownload --> Installer
    Installer -- "PackageInstaller / Shizuku session" --> InstallReceiver

    %% ---------- LOAD stage ----------
    subgraph LOAD["Stage 2 — Load (per installed APK)"]
        Discover["AnimeExtensionLoader.loadExtensions(context)<br/>— PackageManager.getInstalledPackages<br/>— scan filesDir/exts/*.ext (private, setReadOnly())<br/>— isPackageAnExtension: reqFeatures contains<br/>  'tachiyomi.animeextension'<br/>— dedupe shared vs private (higher versionCode wins)"]
        LibVersionCheck{"Lib version in 12..16?"}
        RejectLibVer["REJECT → AnimeLoadResult.Error"]
        SigCheck{"SHA-256 signature<br/>trusted?<br/>(TrustAnimeExtension)"}
        UntrustedResult["AnimeLoadResult.Untrusted<br/>— UI prompts 'Trust?' → persist sig → reload"]
        NsfwCheck{"NSFW + showNsfwSource()<br/>off?"}
        SkipNsfw["Skip extension"]
        CreateLoader["ChildFirstPathClassLoader(<br/>  appInfo.sourceDir, null, context.classLoader)<br/>— parent-last (extension's OkHttp/Jsoup wins)<br/>— fallback: plain PathClassLoader on LinkageError"]
        Instantiate["Class.forName(meta 'tachiyomi.animeextension.class',<br/>  false, classLoader).newInstance()<br/>— semicolon-separated FQNs<br/>— leading '.' → prefix with extension pkg name"]
        TypeSwitch{"Instance type?"}
        SourceInstance["List<AnimeSource> (single source)"]
        FactoryInstance["AnimeSourceFactory.createSources()<br/>→ List<AnimeSource>"]
        RejectType["REJECT → 'Unknown source class type'"]
        WrapInstalled["Wrap as AnimeExtension.Installed(<br/>  name, pkgName, versionName, versionCode,<br/>  libVersion, lang, isNsfw, sources, pkgFactory,<br/>  icon, isShared)"]
    end
    class Discover,LibVersionCheck,RejectLibVer,SigCheck,UntrustedResult,NsfwCheck,SkipNsfw,CreateLoader,Instantiate,TypeSwitch,SourceInstance,FactoryInstance,RejectType,WrapInstalled loader

    InstallReceiver -- "PACKAGE_ADDED / REPLACED" --> Discover
    Discover --> LibVersionCheck
    LibVersionCheck -- "no" --> RejectLibVer
    LibVersionCheck -- "yes" --> SigCheck
    SigCheck -- "no" --> UntrustedResult
    SigCheck -- "yes" --> NsfwCheck
    UntrustedResult -- "user trusts → trust() → re-load" --> Discover
    NsfwCheck -- "filter out" --> SkipNsfw
    NsfwCheck -- "ok" --> CreateLoader
    CreateLoader --> Instantiate
    Instantiate --> TypeSwitch
    TypeSwitch -- "AnimeSource" --> SourceInstance
    TypeSwitch -- "AnimeSourceFactory" --> FactoryInstance
    TypeSwitch -- "else" --> RejectType
    SourceInstance --> WrapInstalled
    FactoryInstance --> WrapInstalled

    %% ---------- REGISTER stage ----------
    subgraph REGISTER["Stage 3 — Register"]
        ExtManager["AnimeExtensionManager<br/>— installedExtensionsMapFlow:<br/>  Map<String, AnimeExtension.Installed><br/>— registered as AnimeExtensionInstallReceiver<br/>  listener (keeps maps in sync)<br/>— availableExtensionsMapFlow (from index)<br/>— untrustedExtensionsMapFlow"]
        SourceManager["AndroidAnimeSourceManager<br/>— subscribes to installedExtensionsFlow<br/>— rebuilds sourcesMapFlow:<br/>  ConcurrentHashMap<Long, AnimeSource><br/>— seeds LocalAnimeSource.ID → LocalAnimeSource<br/>— upserts StubAnimeSource into animesources<br/>  (so source remains known after uninstall)"]
        Lookup["Lookup API:<br/>get(sourceKey) / getOrStub /<br/>getOnlineSources / getCatalogueSources /<br/>catalogueSources: Flow<List<AnimeCatalogueSource>>"]
    end
    class ExtManager,SourceManager,Lookup manager

    WrapInstalled --> ExtManager
    ExtManager -- "installedExtensionsFlow emits" --> SourceManager
    SourceManager --> Lookup

    %% ---------- Player / Browse / Search consumers ----------
    subgraph CONSUME["Consumers"]
        PlayerVM["PlayerViewModel.init<br/>sourceManager.getOrStub(anime.source)"]
        BrowseSrc["BrowseAnimeSourceScreen<br/>sourceManager.getOnlineSources()"]
        GlobalSearch["GlobalAnimeSearchScreenModel<br/>sourceManager.getCatalogueSources()"]
        AnimeScreenModel["AnimeScreenModel<br/>sourceManager.getOrStub(anime.source)"]
    end
    class PlayerVM,BrowseSrc,GlobalSearch,AnimeScreenModel source

    Lookup -- "getOrStub(id)" --> PlayerVM
    Lookup -- "getOnlineSources()" --> BrowseSrc
    Lookup -- "getCatalogueSources()" --> GlobalSearch
    Lookup -- "getOrStub(id)" --> AnimeScreenModel

    %% ---------- UPDATE stage ----------
    subgraph UPDATE["Stage 4 — Update"]
        UpdateCheck["AnimeExtensionApi.checkForUpdates<br/>— compare availableExtensionsMapFlow<br/>  versionCode vs installed versionCode"]
        UpdateAvailable["Newer versionCode found"]
        UpdateFlow["Same as Install Stage 1:<br/>ApkDownload → Installer → InstallReceiver"]
    end
    class UpdateCheck,UpdateAvailable,UpdateFlow installer

    ExtManager -- "findAvailableExtensions()" --> ApiFetch
    ApiFetch --> UpdateCheck
    UpdateCheck --> UpdateAvailable
    UpdateAvailable --> TapUpdate
    UpdateFlow --> Discover

    %% ---------- UNINSTALL stage ----------
    subgraph UNINSTALL["Stage 5 — Uninstall"]
        UninstallCall["AnimeExtensionManager.uninstallExtension(ext)<br/>— package uninstall Intent<br/>  (FLAG_ACTIVITY_NEW_TASK)"]
        UninstallBroadcast["PACKAGE_REMOVED broadcast"]
        RemoveFromMaps["AnimeInstallationListener removes ext<br/>from installedExtensionsMapFlow"]
        RebuildSources["SourceManager rebuilds sourcesMapFlow<br/>— affected AnimeSource entries removed<br/>— StubAnimeSource rows remain in animesources table"]
    end
    class UninstallCall,UninstallBroadcast,RemoveFromMaps,RebuildSources manager

    UninstallCall --> UninstallBroadcast
    UninstallBroadcast --> RemoveFromMaps
    RemoveFromMaps --> RebuildSources
```

## Notes

- **No hard-coded default repo URL.** This fork does **not** bake in the
  upstream Aniyomi default repo (`raw.githubusercontent.com/aniyomiorg/aniyomi-extensions/repo`).
  Repos are entirely user-managed: added via the Settings → Extensions →
  "Add repo" UI or via the `aniyomi://add-repo` / `tachiyomi://add-repo`
  deep links handled in `MainActivity`. URLs must match
  `^https://.*/index\.min\.json$` (validated by `CreateAnimeExtensionRepo`).
- **Two installer backends.** `AnimeExtensionInstaller` dispatches to either
  `PackageInstallerInstallerAnime` (the Android system `PackageInstaller`
  API, default) or `ShizukuInstallerAnime` (if Shizuku is bound, which lets
  the app install APKs without the system install dialog). The choice is
  transparent to the rest of the pipeline.
- **`ChildFirstPathClassLoader` is parent-last.** Extension APKs ship their
  own copy of common dependencies (OkHttp, Jsoup, etc.) and the parent-last
  ordering ensures the extension's bundled versions win over the app's. This
  is what lets an extension built against an older lib still load. On
  `LinkageError`, the loader falls back to a plain `dalvik.system.PathClassLoader`
  (parent-first).
- **Lib version range 12..16.** Parsed from the part of `versionName` before
  the last `.` (e.g. `16.0.1` → lib version `16`). Outside the range, the
  extension is rejected with `AnimeLoadResult.Error`. Current min = 12,
  current max = 16.
- **SHA-256 signature trust is opt-in per signature.** `TrustAnimeExtension`
  maintains a list of trusted SHA-256 certificate fingerprints. Untrusted
  extensions are returned as `AnimeLoadResult.Untrusted`; the UI prompts
  the user to trust the signature, after which it is persisted and the
  extension is re-loaded.
- **Package naming convention**: `eu.kanade.tachiyomi.animeextension.<lang>.<name>`
  (anime) vs `eu.kanade.tachiyomi.extension.<lang>.<name>` (manga). The
  feature flag in the manifest is `tachiyomi.animeextension` (anime) vs
  `tachiyomi.extension` (manga). The class metadata key is
  `tachiyomi.animeextension.class` (anime) vs `tachiyomi.extension.class`
  (manga).
- **Multi-source extensions** declare a semicolon-separated list of FQNs in
  `tachiyomi.animeextension.class`. A leading `.` is prefixed with the
  extension's package name. If the entry class implements `AnimeSource`
  directly, it's wrapped in a single-element list; if it implements
  `AnimeSourceFactory`, `createSources()` is called to get the list.
- **Stub sources survive uninstall.** When an extension is uninstalled, the
  `AnimeSource` entries are removed from the in-memory
  `ConcurrentHashMap<Long, AnimeSource>`, but the `StubAnimeSource` rows in
  the `animesources` table remain. This is so anime entries that referenced
  the now-uninstalled source can still display "source: <name>" rather than
  crashing.
- **Update flow reuses the install pipeline.** The only difference between
  install and update is the trigger: update is fired by the user tapping
  "Update" on an outdated installed extension (after
  `AnimeExtensionApi.checkForUpdates` detects a newer `versionCode` in the
  index). The actual APK download + installer + broadcast + load flow is
  identical to a fresh install.
