# SETUP — shared project setup (committed to repo)

This folder is the **shared brain** for project setup. It is committed to the repo
so that ANY new agent (in a new session) can onboard themselves by reading here.
It is separate from any agent's private memory folder (which is gitignored).

## What's in here

| File | Purpose |
|------|---------|
| `current-state.md` | Detailed current-state snapshot (what's done, what's next, known gaps) — **read this after NEW_SESSION_START_HERE.md** |
| `repo-info.md` | Repo URL, account, visibility, how to clone & push, upstream sync |
| `pat-requirements.md` | How to generate the GitHub PAT needed to push (NO actual token here) |
| `ci-info.md` | What the CI workflow (`.github/workflows/build.yml`) does |
| `new-session-checklist.md` | Step-by-step onboarding for a new agent/session |

## Quick start for a new agent

1. Read `NEW_SESSION_START_HERE.md` at the repo root (the complete orientation).
2. Then read `current-state.md` here for the detailed snapshot.
3. Then follow `new-session-checklist.md` to get set up.

## What does NOT go here

- Actual PATs / secrets (see `pat-requirements.md` for how to get one).
- Any agent's private notes (those live in that agent's own memory folder,
  outside the repo and gitignored).
