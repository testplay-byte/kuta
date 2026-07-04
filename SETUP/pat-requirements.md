# PAT requirements

A GitHub Personal Access Token (PAT) is required to push to this repo. The repo is
public (so clone/read needs no auth), but writes require authentication.

## ⚠️ NEVER commit a PAT to this repo

This file contains **instructions only** — no actual token. If you need a token,
the user provides it out-of-band. Store it in your private memory folder (which is
gitignored), never in this repo.

## How to generate a PAT (the user does this)

1. Go to https://github.com/settings/tokens (logged in as `testplay-byte`).
2. **Generate new token → Fine-grained token** (NOT classic).
3. Settings:
   - **Token name**: e.g. `kuta-push`
   - **Expiration**: 90 days
   - **Repository access**: Only select repositories → `testplay-byte/kuta`
   - **Permissions** → Repository permissions → set ALL to **Read and write**
     (contents, metadata, actions, workflows, etc.). At minimum you need
     **Contents: Read and write** and **Workflows: Read and write** (to push CI
     changes).
4. Generate, copy the token immediately (GitHub won't show it again).

## How a new agent uses the PAT

1. Get the PAT from the user (out-of-band — pasted into the session).
2. Store it in your private memory folder (e.g. `MEMORY/credentials/github-pat.txt`),
   which MUST be gitignored. Verify with `git status` before any commit.
3. Seed git credentials:
   ```sh
   git config --global credential.helper store
   echo "https://testplay-byte:<PAT>@github.com" > ~/.git-credentials
   chmod 600 ~/.git-credentials
   ```
4. Verify: `git push --dry-run origin main` should succeed without prompting.

## Token rotation

Fine-grained tokens expire (90 days). Before expiry, generate a new one and update
`~/.git-credentials` (and your private memory copy). A failed push with a 401 means
the token expired or lacks permissions.
