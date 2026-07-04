# Architecture Diagrams

Visual map of the **Kuta** (Aniyomi fork) codebase, post-Phase-1. All diagrams
are written in [Mermaid](https://mermaid.js.org/) so they render inline on
GitHub and in any Markdown viewer that supports it.

> These diagrams are **documentation only**. They describe the codebase as it
> exists today (commit `cbe42a1e`, post-Phase-1) and were generated from the
> prose in `/home/z/kuta/DOCS/architecture/` plus the feature inventory in
> `/home/z/kuta/FEATURES/`. No source files were modified to produce them.

## Index

| # | File | What it shows |
|---|------|---------------|
| 1 | [`01-navigation-flow.md`](01-navigation-flow.md) | Post-Phase-1 screen graph: PlaceholderHomeScreen → tab navigator → pushed screens, with manga screens shown as still-existing-but-unreachable. |
| 2 | [`02-module-dependencies.md`](02-module-dependencies.md) | The 13 Gradle modules and their `implementation` / `api` project dependencies, layered top-down. |
| 3 | [`03-feature-relationships.md`](03-feature-relationships.md) | Feature-level dependency graph (Player ↔ Extensions ↔ Trackers, Downloads ↔ Player, Backup ↔ everything, etc.) grouped by subgraph. |
| 4 | [`04-data-flow.md`](04-data-flow.md) | The extension → source → anime → episode → hoster → video → MPV `loadfile` pipeline, with the `SAnime`/`SEpisode`/`Hoster`/`Video` models at each stage. |
| 5 | [`05-current-ui-structure.md`](05-current-ui-structure.md) | The current Material 3 UI hierarchy (tabs, screens, dialogs/sheets) — the baseline for the design overhaul. |
| 6 | [`06-db-schema.md`](06-db-schema.md) | SQLDelight ER diagram for both databases (`tachiyomi.db` manga + `tachiyomi.animedb` anime) with FK relationships and key columns. |
| 7 | [`07-extension-lifecycle.md`](07-extension-lifecycle.md) | Extension lifecycle: install (repo fetch → APK download → installer), load (PackageManager discovery → manifest check → classloader → instantiate), register, update, uninstall. |

## Reading notes

- All Voyager navigations flow through a **single root `Navigator`** whose root
  screen is now `PlaceholderHomeScreen` (Phase 1). The tab navigator
  (`HomeScreen` → `TabNavigator`) is pushed on demand from the placeholder's
  "Library" button. See diagram 1.
- Manga UI is **gated out, not removed**: dashed nodes in diagram 1 mark manga
  screens that still compile and exist in the codebase but are no longer
  reachable from the visible navigation graph. Their data layer (DB tables,
  repos, mappers) is fully intact — see diagram 6.
- The player is **MPV**, not Media3/ExoPlayer. The full pipeline from
  `AnimeScreen.openEpisode` to `MPVLib.command(loadfile)` is in diagram 4.
- Two SQLite databases (`tachiyomi.db` for manga, `tachiyomi.animedb` for anime)
  coexist in the `:data` module. The anime DB has 9 tables, the manga DB has
  9 tables — see diagram 6.
