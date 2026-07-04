# New-session checklist

A step-by-step for a new agent (fresh session, no memory) to get access to the kuta
repo and verify they can push.

## 1. Locate your private memory folder

If a previous session left one (e.g. `/home/z/my-project/MEMORY/`), read it first —
it has sandbox specs, toolchain setup, project understanding, and session logs. If
not, you'll build it as you go.

## 2. Set up the Android toolchain (if not already)

See your memory's environment notes. Summary: portable Temurin JDK 21 + Android
command-line tools + platform-tools under `$HOME`, sourced via `~/.android-env.sh`.
You do NOT need a full SDK platform for editing/git — only CI builds the APK.

## 3. Get the repo

If `/home/z/kuta` already exists from a prior session, `cd` there and `git pull`.
Otherwise:

```sh
cd ~
git clone https://github.com/testplay-byte/kuta.git
cd kuta
git remote add upstream https://github.com/aniyomiorg/aniyomi.git
git remote -v   # origin → kuta, upstream → aniyomi
```

## 4. Get a PAT and authenticate

Ask the user for a fine-grained PAT for `testplay-byte/kuta` (see
`pat-requirements.md` for what scopes). Then:

```sh
git config --global credential.helper store
echo "https://testplay-byte:<PAT>@github.com" > ~/.git-credentials
chmod 600 ~/.git-credentials
```

Store the PAT in your private (gitignored) memory folder too, for reuse.

## 5. Set commit identity (if not global already)

```sh
git config --global user.name "Kuta Coder"
git config --global user.email "testplay-byte@users.noreply.github.com"
```

## 6. Verify push access

```sh
git push --dry-run origin main
```

Should succeed silently. If it 401s, the PAT expired or lacks permissions.

## 7. Verify CI

```sh
# latest run status
curl -s -H "Authorization: token <PAT>" \
  https://api.github.com/repos/testplay-byte/kuta/actions/runs | jq '.workflow_runs[0] | {status,conclusion,html_url}'
```

## 8. Read project context

Read `NOTICE` (attribution) and your memory's project overview. You're working on a
fork of Aniyomi — anime-only, AniList as the front door, extensions hidden, rebuilt
player UI. Don't modify Aniyomi source beyond what the current task requires.
