# Repo info

- **Repo**: https://github.com/testplay-byte/kuta
- **Account**: `testplay-byte` (created specifically for this project)
- **Visibility**: Public
- **Default branch**: `main`
- **Origin**: forked from Aniyomi (https://github.com/aniyomiorg/aniyomi), full git
  history preserved for attribution (Apache 2.0) and future upstream syncs.

## Remotes (how this repo is set up locally)

- `origin` → `https://github.com/testplay-byte/kuta.git` (our fork; push here)
- `upstream` → `https://github.com/aniyomiorg/aniyomi.git` (Aniyomi upstream; pull
  updates from here)

Verify with `git remote -v`.

## How to clone (fresh)

```sh
git clone https://github.com/testplay-byte/kuta.git
cd kuta
git remote add upstream https://github.com/aniyomiorg/aniyomi.git
```

You need a PAT to push (public repo, but pushes require auth). See
`pat-requirements.md`.

## How to push

Pushes authenticate via a fine-grained PAT for `testplay-byte/kuta` (see
`pat-requirements.md`). Configure once per machine:

```sh
git config --global credential.helper store
# then either push once and enter testplay-byte / <PAT> at the prompt,
# or pre-seed ~/.git-credentials:
#   echo "https://testplay-byte:<PAT>@github.com" > ~/.git-credentials
#   chmod 600 ~/.git-credentials
```

Commit identity is configured globally as
`Kuta Coder <testplay-byte@users.noreply.github.com>`.

## Syncing upstream changes (later)

```sh
git fetch upstream
git merge upstream/main   # or rebase
# resolve conflicts, then push to origin
git push origin main
```

## Sharing with other GitHub accounts

The `testplay-byte` account owns this repo. To grant another GitHub account push
access: repo **Settings → Collaborators → Add people**, enter the other account's
username. No code change needed. (Noted here so future agents know the option
exists.)
