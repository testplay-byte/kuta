# SETUP — shared project setup (committed to repo)

This folder is the **shared brain** for project setup. It is committed to the repo
so that ANY new agent (in a new session) can onboard themselves by cloning the repo
and reading here. It is separate from any agent's private memory folder.

## What's in here

| File | Purpose |
|------|---------|
| `repo-info.md` | Repo URL, account, visibility, how to clone & push, upstream sync |
| `pat-requirements.md` | How to generate the GitHub PAT needed to push (NO actual token here) |
| `ci-info.md` | What the CI workflow (`.github/workflows/build.yml`) does |
| `new-session-checklist.md` | Step-by-step onboarding for a new agent/session |

## Quick start for a new agent

1. Read `new-session-checklist.md` and follow it.
2. It points you to the other files in this folder as needed.

## What does NOT go here

- Actual PATs / secrets (see `pat-requirements.md` for how to get one).
- Any agent's private notes (those live in that agent's own memory folder, outside
  the repo and gitignored).
