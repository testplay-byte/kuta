# Feature Inventory — Aniyomi (Kuta fork)

Comprehensive inventory of every user-facing feature in the Aniyomi codebase as it
exists in this fork. Generated 2026-07-04 by reading the source at `/home/z/kuta`
(Aniyomi upstream commit `2f5cf775c`).

## Status definitions

| Status | Meaning |
|--------|---------|
| `keep` | Keeping as-is |
| `remove` | Planning to remove (manga-specific UI, non-kept trackers) |
| `modify` | Keeping functionality but changing UI/UX (most UI — design overhaul) |
| `TBD` | Needs user decision (manga features, some misc features) |

## Status summary (at a glance)

**Total features: 92** across 11 category files.

| Status | Count | % |
|--------|-------|---|
| `keep` | 35 | 38% |
| `modify` | 34 | 37% |
| `remove` | 9 | 10% |
| `TBD` | 15 | 16% |

**Key takeaways:**
- **34 features will be redesigned** (`modify`) — the design overhaul touches most of the UI.
- **9 features marked `remove`**: 7 non-kept trackers (Kitsu, Simkl,, Komga, Kavita, Suwayomi, Jellyfin, MangaUpdates) + manga download UI + manga browse sub-tabs.
- **15 features marked `TBD`**: all 10 manga features (user hasn't decided keep vs remove) + manga Glance widget + a few misc items (search history is greenfield, AniList front door is Phase 2).
- **35 features marked `keep`**: core anime functionality (library, history, player engine, 4 trackers, anime downloads, backup, app lock, onboarding, etc.).

## Feature → Status → Category table

### 01 — Library (6 features)

| Feature | Status | Note |
|---------|--------|------|
| Anime Library Tab | `modify` | Main library screen; redesign for new design language |
| Filter/Sort/Display Sheet | `modify` | 3-tab settings sheet; redesign |
| Categories (Anime) | `modify` | CRUD + assignment; redesign |
| Updates Feed | `modify` | New episode detection feed; redesign |
| Library Update Job | `keep` | Background worker (WorkManager); functional, keep |
| Library Update Settings | `modify` | Settings screen; redesign |

### 02 — Browse (7 features)

| Feature | Status | Note |
|---------|--------|------|
| Browse Tab (pager) | `modify` | Swipeable sub-tabs; prune manga sub-tabs + redesign |
| Anime Sources Tab | `keep` | Source browsing entry point; functional |
| Source Browsing | `modify` | Popular/Latest/Search listings; redesign |
| Extension Manager | `keep` | Install/update/uninstall/trust; functional (UI = modify) |
| Extension Repos | `modify` | Repo URL management; redesign |
| Migration | `TBD` | Anime source migration; user decision on keeping |
| Manga-side sub-tabs | `remove` | Manga sources/ext/migration sub-tabs; remove UI |

### 03 — Player (9 features)

| Feature | Status | Note |
|---------|--------|------|
| Player Engine (MPV) | `keep` | Core playback engine; keep (not Media3) |
| Video Quality / Hoster Selection | `modify` | Quality picker UI; redesign |
| Subtitle & Audio Track Switching | `modify` | Track selection UI; redesign |
| Skip Intro/Outro (AniSkip) | `keep` | Depends on linked tracker for MAL id; keep |
| Resume / Seek Position | `keep` | Position memory; functional |
| Gestures | `modify` | Seek/brightness/volume/double-tap; redesign |
| Player Settings Suite | `modify` | 6 prefs + 7 screens; redesign |
| Custom Lua MPV Buttons | `keep` | Full subsystem (DB + Lua bridge); keep |
| Screenshot / PiP / MediaSession | `keep` | Auxiliary player features; keep |

### 04 — Trackers (13 features)

| Feature | Status | Note |
|---------|--------|------|
| Tracking Infrastructure | `keep` | TrackerManager, BaseTracker, OAuth, anime_sync DB |
| AniList Tracker | `keep` | Front door tracker (Phase 2); keep |
| MyAnimeList Tracker | `keep` | Keep (has refresh-token) |
| Shikimori Tracker | `keep` | Keep |
| Bangumi Tracker | `keep` | Keep |
| Track Search (link anime) | `modify` | UI for finding anime on tracker; redesign |
| Track Status Sync | `keep` | Progress/score/status sync; functional |
| Track Settings Screen | `modify` | Settings UI; redesign |
| Kitsu Tracker | `remove` | Not in kept set |
| Simkl Tracker | `remove` | Not in kept set |
| Komga Tracker | `remove` | Manga-only; remove |
| Kavita Tracker | `remove` | Manga-only; remove |
| Suwayomi Tracker | `remove` | Manga-only EnhancedMangaTracker; remove |

*(Jellyfin and MangaUpdates also marked `remove` — manga-only / not in kept set. See 04-trackers.md for full detail.)*

### 05 — Downloads (7 features)

| Feature | Status | Note |
|---------|--------|------|
| Anime Download Manager | `keep` | Queue/pause/resume (FFmpegKit-based); functional |
| Download Queue UI | `modify` | Legacy RecyclerView-in-Compose; re-skin |
| Download Storage/Provider/Cache | `keep` | Storage management; functional |
| Download Preferences | `modify` | Shared anime+manga prefs; prune manga keys |
| Download Notifications + Service | `keep` | Foreground service; functional |
| Downloaded-only + Incognito | `keep` | Modes; functional |
| Manga Download Manager | `remove` | UI remove; data layer TBD |

### 06 — Settings (11 features)

| Feature | Status | Note |
|---------|--------|------|
| Settings Framework | `modify` | Settings navigation + screens; redesign |
| Appearance / General Settings | `modify` | Theme, dark mode, start screen; redesign |
| Library / Updates Settings | `modify` | Update interval, categories; redesign |
| Player / Reader Settings | `modify` | Player prefs; redesign |
| Download Settings | `modify` | Auto-download, storage; redesign |
| Tracking Settings | `modify` | Tracker login management; redesign |
| Browse / Sources Settings | `modify` | Extension repos; redesign |
| Data & Storage Settings | `keep` | Backup/restore, cache; functional (UI = modify) |
| Security Settings | `keep` | App lock, incognito; functional (UI = modify) |
| Advanced Settings | `modify` | Crash logs, debug; redesign |
| About Screen | `modify` | Already has Kuta branding; redesign |

### 07 — Search (6 features)

| Feature | Status | Note |
|---------|--------|------|
| Global Anime Search | `modify` | Multi-source search; redesign |
| Source-level Search | `modify` | Within-source search; redesign |
| Library In-place Search | `modify` | Library toolbar search; redesign |
| History Tab Search | `modify` | History filtering; redesign |
| Search History | `TBD` | DOES NOT EXIST — greenfield add; user decision |
| Source Filters / Pinned Sources | `modify` | Filter/pin management; redesign |

### 08 — History (4 features)

| Feature | Status | Note |
|---------|--------|------|
| History Tab | `modify` | Recently watched; redesign |
| Resume / Continue Watching | `keep` | Resume position; functional |
| History Tracking (data) | `keep` | animehistory DB; functional |
| History Clearing / Management | `keep` | Clear history; functional |

### 09 — Manga (10 features, all TBD)

| Feature | Status | Note |
|---------|--------|------|
| Manga Library | `TBD` | UI gated; data intact. Keep vs remove? |
| Manga Reader | `TBD` | Page-based reader. Keep vs remove? |
| Manga Browse / Sources | `TBD` | Sub-tabs still visible (Phase 2 cleanup gap). |
| Manga Extensions | `TBD` | Manga extension manager. |
| Manga Downloads | `TBD` | Manga download manager. |
| Manga History | `TBD` | Manga history tab. |
| Manga Tracking | `TBD` | Manga tracker integrations. |
| Manga Categories | `TBD` | Manga category management. |
| Manga Migration | `TBD` | Manga source migration. |
| Manga Updates Feed | `TBD` | Manga updates sub-tab. |

**Cross-cutting coupling**: BrowseTab, HistoriesTab, CategoriesTab, StatsTab, StorageTab, DownloadsTab, UpdatesTab all still expose manga sub-tabs (Phase 1 only gated the Library tab). See `09-manga.md` for the full coupling summary.

### 10 — Onboarding (5 features)

| Feature | Status | Note |
|---------|--------|------|
| Onboarding Screen | `keep` | Welcome + steps; functional (UI = modify) |
| Storage Setup | `keep` | Download location chooser; functional |
| Permission Requests | `keep` | Storage, notifications; functional |
| Theme Selection Step | `keep` | First-run theme pick; functional |
| Guides Step | `keep` | Guide links; functional |

### 11 — Misc (14 features)

| Feature | Status | Note |
|---------|--------|------|
| Crash Logs / Error Reporting | `keep` | GlobalExceptionHandler + CrashActivity; functional |
| Deep Links (aniyomi://) | `modify` | add-repo, tracker auth; rebrand scheme? |
| OAuth Callbacks | `keep` | Tracker auth redirects; functional |
| App Shortcuts | `modify` | Launcher shortcuts; has duplicate-shortcutId bug |
| Updates Widget (Glance) | `keep` | presentation-widget module; functional |
| Backup & Restore | `keep` | Full app data backup; functional (UI = modify) |
| Incognito Mode | `keep` | Session-only history; functional |
| External Intents | `keep` | Open video URLs from other apps; functional |
| App Lock / Biometric | `keep` | SecureActivityDelegate + UnlockActivity; functional |
| Storage Usage Screen | `modify` | Storage breakdown; redesign |
| Stats Screen | `modify` | Watch/read stats; redesign |
| Categories Management | `modify` | Category CRUD screen; redesign |
| App Update Checker | `modify` | Points to aniyomiorg/aniyomi — branding leak; fix |
| CSV Export | `modify` | Library export; filename says "aniyomi_library.csv" — branding leak |

## How to use this inventory

- **For each prompt**: check which features the task touches, read the corresponding category file for file paths and dependencies.
- **For the design overhaul**: all `modify` features need new UI. Start with the ones that have the most leverage (shared components, settings framework).
- **For manga removal decision**: read `09-manga.md` — it has the full coupling summary showing every shared file that needs surgery.
- **For tracker pruning**: read `04-trackers.md` — 7 trackers to remove, with file paths for each.

## Files

| File | Features | Category |
|------|----------|----------|
| [01-library.md](01-library.md) | 6 | Anime library, categories, updates |
| [02-browse.md](02-browse.md) | 7 | Browse tab, sources, extensions, migration |
| [03-player.md](03-player.md) | 9 | Player UI, gestures, skip, subtitles, resume |
| [04-trackers.md](04-trackers.md) | 13 | All 11 trackers + infra + UI |
| [05-downloads.md](05-downloads.md) | 7 | Download manager, offline viewing |
| [06-settings.md](06-settings.md) | 11 | All settings screens |
| [07-search.md](07-search.md) | 6 | Global search, search history (doesn't exist) |
| [08-history.md](08-history.md) | 4 | Watch history, continue watching |
| [09-manga.md](09-manga.md) | 10 | Manga features (all TBD) + coupling summary |
| [10-onboarding.md](10-onboarding.md) | 5 | Welcome, storage, permissions, restore |
| [11-misc.md](11-misc.md) | 14 | Crash logs, deep links, shortcuts, widgets, backup, etc. |
