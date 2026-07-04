# New-session checklist

A step-by-step for a new agent (fresh session, no memory) to get access to the
kuta repo and verify they can push. **Read `NEW_SESSION_START_HERE.md` at the
repo root first** — it's the complete orientation. This file is the
step-by-step checklist.

## 1. Read the orientation doc

```
cat /home/z/kuta/NEW_SESSION_START_HERE.md   # or read it on GitHub
```

This tells you: what the project is, current state, where the code lives, what's
next. ~5 min read.

## 2. Check for private memory

If `/home/z/my-project/MEMORY/` exists (gitignored, persists across sessions in
the same sandbox), read it:
- `MEMORY/README.md` — entry point
- `MEMORY/01-environment/` — sandbox specs + toolchain setup
- `MEMORY/03-aniyomi-reference/` — architecture headline facts
- `MEMORY/04-session-log/` — chronological logs (read the latest)

If not, you'll build it as you go.

## 3. Pull the repo

```sh
cd /home/z/kuta   # if it exists
git pull origin main

# OR clone fresh:
cd ~ && git clone https://github.com/testplay-byte/kuta.git && cd kuta
git remote add upstream https://github.com/aniyomiorg/aniyomi.git
git remote -v   # origin → kuta, upstream → aniyomi
```

## 4. Set up the Android toolchain (if not already)

A portable toolchain should already be at:
- `/home/z/jdk/jdk-21.0.11+10` (Temurin JDK 21)
- `/home/z/Android/Sdk/` (cmdline-tools + platform-tools)
- Sourced via `~/.android-env.sh`

Verify: `source ~/.android-env.sh && javac -version` → 21.0.11.

If missing, see `MEMORY/01-environment/02-toolchain-setup.md` for the rebuild
recipe (or the "How to get up and running" section of NEW_SESSION_START_HERE.md).

**You do NOT need to build locally.** CI is the build system.

## 5. Get the GitHub PAT

A fine-grained PAT for `testplay-byte/kuta` is needed to push.

If `/home/z/my-project/MEMORY/credentials/github-pat.txt` exists:
```sh
PAT=$(grep -E '^github_pat_' /home/z/my-project/MEMORY/credentials/github-pat.txt | head -1)
git config --global credential.helper store
echo "https://testplay-byte:$PAT@github.com" > ~/.git-credentials
chmod 600 ~/.git-credentials
unset PAT
```

If not, ask the user. See `pat-requirements.md` for scopes (fine-grained,
single repo `testplay-byte/kuta`, all permissions R/W, 90-day expiration).

## 6. Set git identity (if not global already)

```sh
git config --global user.name "Kuta Coder"
git config --global user.email "testplay-byte@users.noreply.github.com"
```

## 7. Verify push access + CI

```sh
git push --dry-run origin main   # should succeed silently

# latest CI run
PAT=$(grep -E '^github_pat_' /home/z/my-project/MEMORY/credentials/github-pat.txt | head -1)
curl -s -H "Authorization: token $PAT" \
  "https://api.github.com/repos/testplay-byte/kuta/actions/runs?per_page=1" \
  | jq '.workflow_runs[0] | {status, conclusion, html_url}'
unset PAT
```

## 8. Read the context docs

Per the "Read the context docs" table in `NEW_SESSION_START_HERE.md`. The key
ones for design system work:
- `DOCS/design-system/00-shared-architecture.md` (the architecture)
- `DOCS/design-system/01-neon.md` / `02-notebook.md` / `03-brutalist.md` (specs)
- `SETUP/current-state.md` (detailed current state)

## 9. You're ready

You should now be able to:
- Push code to `origin main`
- Watch CI build it
- Download the APK from the Actions page
- Read the design specs and the `kuta/` package to understand the codebase

**Constraints reminder**: every modified upstream file gets a `// FORK:` marker;
don't change applicationId (`app.kuta`) or namespace (`eu.kanade.tachiyomi`);
don't delete existing M3 code; don't build locally; push incrementally.
