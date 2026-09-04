# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Branch Strategy (IMPORTANT!)

**FIRST:** Always check your current branch using `git branch --show-current` or by checking the working directory path (e.g., `../logistics-mc-1.21.11/` indicates mc/1.21.11 branch).

This repository uses a multi-version strategy to support different Minecraft releases:
- **`mc/26.3`** - **Pre-release.** Newest MC release, not yet stable. Forward-port target — receives cherry-picks from `mc/26.2`, does not originate new work
- **`mc/26.2`** - **Main/default branch** (the branch `origin/HEAD` tracks). Newest *stable* MC release; new work starts here
- **`mc/26.1`** - Maintenance + backport target for MC 26.1
- **`mc/1.21.11`** - Maintenance + backport target for MC 1.21.11
- **`mc/1.21.1`** - Maintenance + backport target for MC 1.21.1

**Roles advance over time:** when a newer MC release ships *and is stable*, `main` moves to that `mc/2x.y` branch and the previous one becomes a backport target. A pre-release branch (like `mc/26.3` today) does **not** become main and does not shift the backport chain until it ships — it just trails main as a forward-port target. Don't hardcode which branch is main — confirm with `git rev-parse --abbrev-ref origin/HEAD`.

### Critical Understanding

**Domain architecture enables cross-version cherry-picking.** After significant refactoring to isolate version-specific code:
- ✅ **Can cherry-pick** commits between branches (mc/1.21.1 ↔ mc/1.21.11 ↔ mc/26.1 ↔ mc/26.2 ↔ mc/26.3)
- ✅ **Can share code** across versions via git operations
- ✅ **Can maintain feature parity** with minimal manual intervention
- ✅ **Cherry-pick is the primary porting mechanism**

**Porting = git cherry-pick, with occasional conflict resolution for API differences.**

### Recommended Worktree Setup

For easier cross-version development, consider setting up git worktrees for each mc/* branch in sibling directories:
- `../logistics-mc-1.21.1/` - mc/1.21.1 branch worktree
- `../logistics-mc-1.21.11/` - mc/1.21.11 branch worktree
- `../logistics-mc-26.1/` - mc/26.1 branch worktree
- `../logistics-mc-26.2/` - mc/26.2 (main) branch worktree
- `../logistics-mc-26.3/` - mc/26.3 (pre-release) branch worktree

The current working directory path indicates which branch you're on.

**Benefits:**
- Reference code across versions without switching branches
- Compare implementations when cherry-picking
- Resolve conflicts by viewing other version's code directly

If worktrees are detected at these paths, they may be referenced when working on cross-version changes.

### Running Several Sessions in Parallel

Several sessions can work different PRs at once. Most of the work parallelises cleanly; two
steps do not, and the difference is worth knowing before spinning up four or five.

**Independent per session** — each session gets its own worktree and stays in it:

- Editing, committing, and pushing a feature branch.
- Addressing review findings and resolving threads.
- `git town sync` / `propose` on your own branch. Git Town's runstate is keyed by **worktree
  path**, not by repository, so sessions in different worktrees cannot corrupt each other's.
- `git fetch`, and reads generally. Git locks refs internally.
- Git Town's configuration. It is read from `.git-town.toml` in each worktree's own checkout
  rather than from shared git metadata, so a worktree sitting on an older branch can be a
  config revision behind until it syncs. See [Git Town](#git-town).

**Serialised — take the ship lock first:**

- **`git town ship`.** Shipping moves the default branch, and every other session's feature
  branch immediately becomes "not in sync with its parent".
- **Porting.** Cherry-picking uses the shared `mc/*` worktrees; two sessions cherry-picking in
  the same worktree collide outright, and two pushing the same branch race for a
  non-fast-forward rejection.

The lock is one atomic `mkdir` in the shared git dir, so every worktree sees the same lock:

```bash
# --path-format=absolute matters: a bare --git-common-dir prints a *relative*
# `.git` when run from the main worktree, so the lock path would depend on cwd.
LOCK="$(git rev-parse --path-format=absolute --git-common-dir)/ship.lock"

if mkdir "$LOCK" 2>/dev/null; then
  echo "pr-<n>" > "$LOCK/owner"; date +%s > "$LOCK/since"

  # ... git town ship, then port to the other branches ...

  rm -rf "$LOCK"                             # release: only ever on this path
else
  owner=$(cat "$LOCK/owner" 2>/dev/null) || true
  since=$(cat "$LOCK/since" 2>/dev/null) || true
  if [ -n "$since" ]; then age="$(( $(date +%s) - since ))s"; else age="unknown age"; fi
  echo "held by ${owner:-unknown} ($age)"
  # Do not touch the lock. Wait and retry, or go work a different PR.
fi
```

The fallbacks on `owner`/`since` are not cosmetic: a lock directory can exist without its two
files — a session killed between the `mkdir` and the writes leaves exactly that — and
`$(( $(date +%s) - $(cat missing) ))` is a **syntax error** that aborts the whole `echo`, so the
losing session would print nothing at all instead of saying who holds the lock.

**Release only on the path that acquired it.** Putting `rm -rf "$LOCK"` after the `if`/`else`
instead of inside the success branch means the session that *lost* the race deletes the winner's
lock on its way past — the next contender then acquires it while the original holder is still
mid-ship, which is precisely the collision the lock exists to prevent.

Release even when the ship fails, but only if you are the holder. **The release is its own
step** — acquire, ship, port and release are separate commands in an agent session, each in its
own shell, so a `trap ... EXIT` on the acquiring command would fire the moment that command
returns and drop the lock immediately. That also means a ship that fails partway will strand the
lock rather than unwinding it: if a step fails, release explicitly before moving on.

Because a lock can be stranded that way, treat one held by a PR that is clearly finished, or one
that is hours old, as stale — say so, confirm the holder is really gone, then clear it. Waiting
is usually cheap: a ship plus a port is a few minutes.

**Hard constraints, whatever else you do:**

- **A branch can be checked out in only one worktree.** `git worktree add` refuses a branch that
  is already checked out anywhere and fails with `fatal: '<branch>' is already used by worktree
  at ...`. Look before you create.
- **Never run anything in a worktree you do not own** — not a build, not a checkout, not a
  `pkill`. Another session is probably mid-operation in it.
- **`pkill -f` matches prefixes.** `pkill -f "logistics-mc-1.21.1"` also kills
  `logistics-mc-1.21.11`. Anchor with a trailing slash, or inspect with `pgrep -fl` first.
  Generic patterns (`GradleWorkerMain`, `devlaunchinjector`) hit every worktree — never use them.
- **Minecraft dev servers collide on port 25565.** A second `runServer` fails to bind and dies,
  which reads as a broken port rather than a busy one. Serialise them, or give each worktree its
  own `server-port`.
- Gradle daemons are per-worktree but share `~/.gradle`. Four or five concurrent builds are
  heavy; the ship lock already serialises the ones that run during a push.

### Development Strategy

- **`mc/26.2`**: Primary development target (main branch) — new features, refactors, and fixes start here
- **`mc/26.3`**: Pre-release forward-port target — cherry-pick player-facing features/fixes up from mc/26.2 once they're stable there; never originate work here
- **`mc/26.1`**: Backport target (maintenance) — port player-facing features/fixes down from mc/26.2
- **`mc/1.21.11`**: Backport target (maintenance) — port player-facing features/fixes down
- **`mc/1.21.1`**: Backport target (maintenance) — many tech-mod users still on this version

### Branch Protection Rules (CRITICAL)

**New work never goes directly to an `mc/*` branch** (`mc/26.3`, `mc/26.2`, `mc/26.1`, `mc/1.21.11`, `mc/1.21.1`). These are protected branches (configured as Git Town perennial branches — see [Git Town](#git-town) below). Writing a fix or a feature — including in auto mode — means a feature branch and a PR.

**Porting an already-merged commit is different, and is expected to push directly.** `git push origin HEAD:mc/1.21.1` after a cherry-pick is the *sanctioned* way to keep the version branches in parity — not a violation of the rule above, which is about where new work originates. The [Cross-Version Workflow](#cross-version-workflow) depends on it, and the `version-branches` ruleset is configured to permit it. The one place this does not apply is the default branch, which accepts no direct pushes at all; see [How the rulesets enforce this](#how-the-rulesets-enforce-this).

**Required workflow for any new work:**
1. Create a feature branch first: `git town hack descriptive-branch-name`
2. Make commits on the feature branch
3. Sync/push the feature branch: `git town sync --push` (the explicit `--push` guarantees the branch reaches the remote regardless of the user's Git Town push defaults; or let `propose` do it)
4. Open a PR targeting the appropriate `mc/*` branch: `git town propose`

**Issue each push as its own command.** Permission rules match a single command; a `for` loop or
an `&&` chain that ports to several branches at once matches no rule and falls through to the
permission classifier, which then has to judge a compound string containing a push to a protected
branch. One `git -C <worktree> push origin HEAD:<branch>` per branch is both allowable by rule and
easier to read in a transcript — batch the cherry-picks if you like, but not the pushes.

**Exceptions:**
- Cherry-picking between `mc/*` branches for porting already-merged commits. Confirm with the user before pushing.
- The tag-based [Hotfix Workflow](#hotfix-workflow): its branch comes off a release tag, not main, and is pushed/tagged directly with no PR by design. Confirm with the user before pushing there too.

**In auto mode:** Still pause and confirm before any push when the current branch is `mc/*` or when no feature branch has been created yet. A wrong push to a protected branch is very hard to undo cleanly.

#### How the rulesets enforce this

Two rulesets implement the above, and they differ only in who may bypass them:

| Ruleset | Targets | Bypass |
|---|---|---|
| `default-branch` | `~DEFAULT_BRANCH` | **none** |
| `version-branches` | `refs/heads/mc/**` | repository admin |

Both require a PR, the same ten status checks, and resolution of every review thread. The default branch matches both, and bypassing one ruleset does not exempt you from another's rules — so **nothing can be pushed directly to the default branch, by anyone, including admins and agent sessions.** The other `mc/*` branches match only `version-branches`, where the admin bypass keeps the cherry-pick-and-push porting flow working.

`~DEFAULT_BRANCH` resolves dynamically, so when main moves to a newer `mc/2x.y` the roles follow automatically. Nothing here hardcodes which branch is main.

**Consequence — port *up* into the default branch through a PR.** Porting down or sideways (mc/26.2 → mc/26.1, → mc/1.21.11, → mc/1.21.1, → mc/26.3) stays a direct push. But a fix that originates on a non-default branch and needs to reach the default branch cannot be pushed there; open a normal PR for that hop. This is the intended behavior: the adapted commit is different code from what was reviewed, and the default branch is where review happens. It also makes the existing "new work starts on the default branch" rule enforceable rather than aspirational — so if a change affects multiple versions, propose it on the default branch first.

The [legacy-only exception](#cross-version-workflow) is unaffected: those fixes originate on the highest affected branch and are cherry-picked *down*, never touching the default branch.

**Direct pushes to `mc/*` run the unit tests locally first.** The `pre-push` hook (installed by `./gradlew installGitHooks`) runs `./gradlew test` whenever the destination ref is an `mc/*` branch, because a port has no PR and so nothing else has tested the adapted code. Check Code does run on push to `mc/**`, but only after the commit has landed. Game tests stay in CI. Bypass with `git push --no-verify` in an emergency.

**Pre-release branches are exempt from the hook**, decided from `minecraft_version` rather than a branch list: a bare version (`26.2`) is released and enforces the checks, anything carrying a qualifier (`26.3-snapshot-7`, `26.3-pre-1`, `26.3-rc1`) is pre-release and skips them. Such a branch tracks upstream snapshot artifacts that get rotated and deleted, so Gradle often cannot resolve its dependencies — and a failed *configuration* fails every task, not just the tests, which would block every push and train everyone to pass `--no-verify`. The roles rotate on their own: when the pre-release ships and `minecraft_version` becomes bare, that branch starts enforcing the checks, and the next alpha branch is exempt from the day it is created. Nothing to remember, no list to edit.

### Git Town

Git Town is configured by [`.git-town.toml`](.git-town.toml) in the repo root — main branch
`mc/26.2`, `^mc/` perennial, `^release-please--` observed, ship via the GitHub API. It is
committed, so a fresh clone is already configured: nothing to run, and every contributor and
session sees the same branch policy. Git Town runs headlessly here — no interactive prompts, safe
to run from scripts/agents. Prefer these over plain `git` for the standard feature-branch flow:

| Instead of… | Use |
|---|---|
| `git checkout -b <branch>` (off main) | `git town hack <branch>` |
| `git checkout -b <branch>` (off current branch, stacked) | `git town append <branch>` |
| `git pull` / manually merging main into a feature branch | `git town sync` |
| Opening a PR by hand | `git town propose` |
| Squash-merging a PR | `git town ship` |
| Deleting a merged/obsolete feature branch | `git town delete` |
| `git checkout <branch>` when you don't remember the name | `git town switch` |

**Local git metadata overrides the file.** `git config git-town.main-branch …` in a clone silently
wins over `.git-town.toml`, and list settings like `perennials` *merge* rather than replace, so a
stale local key is invisible until behavior diverges. Change repo-level settings by editing the
file, not with `git config`. Check for leftovers with `git config --local --list | grep '^git-town\.'`
— that should come back empty.

**Branch lineage stays local.** `git-town-branch.<name>.parent` and `.branchtype` describe *your*
in-flight branches, not the repo, and belong in local metadata where Git Town writes them.

Because the file is version-controlled it travels with cherry-picks, so every `mc/*` branch carries
the same config, and a fresh clone needs no setup. The flip side: a worktree on a branch that
predates the file has *no* config, and Git Town hard-errors with "no main branch configured". The
fix is `git town sync` to pick the file up from the parent — not a local `git config`.

**Exceptions that stay plain `git`:** cherry-picks between `mc/*` branches (perennial-to-perennial, not a Git Town workflow), the tag-based hotfix branch in [Hotfix Workflow](#hotfix-workflow) below (branches off a tag, not main, so `git town hack` doesn't apply), and read-only inspection commands (`git status`, `git log`, `git branch --show-current`, `git diff`).

### Cross-Version Workflow

**When fixing bugs:**
1. Fix on **mc/26.2** (main) when the bug exists there
2. Check if the bug exists on the other branches, **including the pre-release mc/26.3**
3. **Cherry-pick** the fix to affected branches (resolve conflicts if needed) — down to mc/26.1, mc/1.21.11, mc/1.21.1, and *up* to mc/26.3
4. Test on each target branch after cherry-pick
5. Priority order for porting **when mc/26.2 is the origin** (the common case): mc/26.2 → mc/26.1 → mc/1.21.11 → mc/1.21.1 (backports), and separately mc/26.2 → mc/26.3 (forward-port)
6. **Legacy-only bugs** (don't reproduce on mc/26.x — e.g. 26.x auto-derives the cutout render layer from sprite transparency, so manual `BlockRenderLayerMap` registrations only matter on 1.21.x): this is the exception to #5 — mc/26.2 is not the origin. Originate the fix on the highest *affected* branch (mc/1.21.11) instead and cherry-pick down to mc/1.21.1

**When adding features:**
- Develop on **mc/26.2** (main), then backport to mc/26.1, mc/1.21.11 and mc/1.21.1 if the feature applies, and forward-port to mc/26.3 once it's stable on main
- Internal/infra work is backported (and forward-ported to mc/26.3) too — keep all branches as close to in sync as possible, since closer branches make every future cherry-pick apply cleanly
- Keep changes minimal and tested
- Avoid large refactorings unless coordinated across all branches

### Writing Cherry-Pick-Friendly Code

Since commits will be cherry-picked across branches, write code that minimizes conflicts:

**Before committing:**
1. **Compare implementations** across worktrees (if available):
   - Check `../logistics-mc-1.21.1/` for how mc/1.21.1 implements similar features
   - Check `../logistics-mc-26.1/` for how mc/26.1 handles the same areas
2. **Match structure** where possible:
   - Use similar method names and signatures across versions
   - Keep file organization consistent
   - Align formatting and code structure
3. **Isolate version-specific code**:
   - Keep Minecraft API calls isolated in specific methods/classes
   - Use abstractions to hide version differences
   - Document any version-specific workarounds with comments

**When reviewing commits:**
- Test cherry-picks to other branches before pushing
- If conflicts arise, consider whether the code structure could be improved
- Document any intentional divergences in commit messages

**Goal:** Minimize cherry-pick conflicts by maintaining structural consistency across versions while isolating version-specific API differences.

## Build Commands

```bash
./gradlew build              # Build the mod JAR
./gradlew remapJar           # Build with obfuscation remapping (use for mc/1.21.1 and mc/1.21.11; mc/26.1+ uses build)
./gradlew runClient          # Launch Minecraft client for testing
./gradlew runServer          # Launch Minecraft server
```

**Requirements:** See `gradle.properties` for current versions (`java_version`, `minecraft_version`, `loader_version`, `fabric_version`).

### Third-party mod dependencies on a pre-release branch

JEI, Jade and spark are all compile-only or dev-only, and none of them publish for a Minecraft
pre-release on day one. That used to make a pre-release branch unbuildable — and therefore
untestable — until every one of them caught up, which is weeks after the code itself is ready.

Only **JEI** is genuinely coupled: its *artifact id* embeds a Minecraft version
(`mezz.jei:jei-<mc>-fabric`), so bumping `minecraft_version` changes what Gradle asks for and
resolution fails. `jei_mc_version` in `gradle.properties` is that artifact's Minecraft line, kept
separate so a pre-release branch can compile against the last line JEI shipped for. The API surface
the mod uses is stable across lines. Raise it to match `minecraft_version` once JEI catches up.

Jade and spark need no equivalent — their Minecraft line lives in the version string
(`jade_fabric_version=26.2.11+fabric`), so pointing them at an older build is an ordinary version
change. One spark build usually spans several Minecraft lines.

This decouples the *dependencies* only. A new Minecraft release still needs its own API migration
before the branch compiles.

**Build output:** `build/libs/logistics-{version}.jar`
- Local: `logistics-dev-local.jar`
- CI: `logistics-0.5.5+mc1.21.11.fabric.jar` (SemVer build metadata format)

### Version Management

All branches use **release-please** for automated versioning with **SemVer build metadata**.

**How it works:**
1. Create a feature/fix branch (short, meaningful name - no specific format required)
2. Make commits using imperative mood (non-conventional format)
3. Work freely - squash commits, force push, iterate as needed
4. Create PR with:
   - Title: scoped conventional commit format (`fix(pipes):`, `feat(automation):`, etc.)
   - Body: release notes style
5. PR gets squash-merged into target branch with the conventional commit message
6. Release-please sees the conventional commit and creates a release PR
7. Merge the release PR to create a GitHub release
8. Release workflow builds and publishes to Modrinth/CurseForge

**When versions bump (pre-1.0.0 behavior):**
- `fix:` commits → patch version (0.4.0 → 0.4.1)
- `feat:` commits → patch version (0.4.0 → 0.4.1) ← `bump-patch-for-minor-pre-major` is enabled
- `feat!:` or `BREAKING CHANGE:` → minor version (0.4.0 → 0.5.0) ← `bump-minor-pre-major` is enabled

After 1.0.0, `feat:` → minor and `feat!:` → major (standard SemVer).

**Naming conventions:**
- Git tags: `mc{version}-v{semver}` (e.g., `mc1.21.11-v0.4.0`)
- Artifacts: `logistics-{semver}+mc{version}.{loader}.jar` (e.g., `logistics-0.4.0+mc1.21.11.fabric.jar`)
- Published version: `{semver}+mc{version}.{loader}` (e.g., `0.4.0+mc1.21.11.fabric`)
- Display name: `Logistics v{semver} for {loader} {version}` (e.g., `Logistics v0.4.0 for fabric 1.21.11`)

**Do NOT manually edit version numbers.** Let release-please manage it. If you need to manually set a version, edit `.release-please-manifest.json` and commit the change.

### Hotfix Workflow

When a critical bug needs a patch release *after* feature development has already started on the primary development branch, use this process to bypass release-please and publish a clean hotfix.

This branches off a tag rather than main, so it's the plain-`git` exception noted in [Git Town](#git-town) above — don't use `git town hack` here. It's also the documented exception in [Branch Protection Rules](#branch-protection-rules-critical) — the hotfix branch is pushed and tagged directly, with no feature branch or PR. Still confirm with the user before pushing.

**Steps:**
1. Branch from the last release tag:
   ```bash
   git checkout -b hotfix/X.Y.Z mc{version}-vX.Y.Z
   # e.g. git checkout -b hotfix/0.4.1 mc1.21.11-v0.4.0
   ```
2. Apply the fix and commit (imperative commit message, no prefix)
3. Tag the hotfix:
   ```bash
   git tag mc{version}-vX.Y.Z
   # e.g. git tag mc1.21.11-v0.4.1
   ```
4. Push the branch and tag:
   ```bash
   git push origin hotfix/X.Y.Z
   git push origin mc{version}-vX.Y.Z
   # e.g. git push origin mc1.21.11-v0.4.1
   ```
5. Trigger the build workflow manually on GitHub:
   - Go to **Actions → Build and Publish Release**
   - Set `tag` = `mc1.21.11-v0.4.1`, `publish` = `true`
6. Cherry-pick the fix back to `mc/1.21.11`:
   ```bash
   git checkout mc/1.21.11
   git cherry-pick <fix-commit-sha>
   ```
7. **Bump the manifest** so release-please starts from the correct base:
   - Edit `.release-please-manifest.json`: update version from `X.Y.Z-1` → `X.Y.Z`
   - Commit: `Bump release-please manifest to X.Y.Z after hotfix`
8. Delete the hotfix branch:
   ```bash
   git push origin --delete hotfix/X.Y.Z
   git branch -d hotfix/X.Y.Z
   ```

**Why step 7 matters:** release-please reads `.release-please-manifest.json` to determine the current version. If you skip this, it will try to create a release PR for the hotfix version on the next `fix:` or `feat:` commit — producing a duplicate tag conflict.

### Release Cycle Playbook

**The version boundary problem:** Once any `feat:` PR merges, the next release-please bump is a minor version. This makes "quick patch after features land" impossible without the hotfix workflow above.

**Two tools to manage this:**

**1. "Up Next" label** — Applied to PRs that are approved but intentionally held past the current patch release.
- Rule: If a release-please PR for the current version is still open, apply `up next` to any `feat:` PR instead of merging it.
- Transition: When the patch release ships, strip `up next` — those PRs are now in scope for the next minor.

**2. Release Candidate gate** (`build-rc.yml`) — When ready to commit to the release, publish `v0.6.0-rc.1`. This builds the base branch merged with the open Release Please PR, publishes it as a beta/prerelease, and signals the "Field Test phase" for community testing. When satisfied, merge the release-please PR.

**Full cycle:**
```
fix: PRs merge → release-please opens vX.Y.Z patch PR
Apply "up next" label to any feat: PRs that should wait
Merge patch release-please PR → vX.Y.Z ships → cherry-pick to other branches
Strip "up next" from held PRs
Merge feat: PRs → release-please opens vX.Y+1.0 PR (leave it open)
Publish vX.Y+1.0-rc.1 via build-rc.yml → Field Test gate
Fix bugs found in testing (accumulate in vX.Y+1.0 release notes; re-publish rc.2, rc.3 as needed)
Merge vX.Y+1.0 release-please PR → ships → cherry-pick
```

**Use the Hotfix Workflow (above) only when:** v0.6.0 is already out AND new features have already merged, making a v0.5.x backport necessary.

## Backward Compatibility Policy

To bound technical debt while in pre-release, backward-compatibility support reaches back **one major only**. During pre-release the "major" is the **second version digit** (e.g. `0.7.x`); each major keeps compatibility bridges for the **previous major** (`0.6.x`) but nothing older.

This applies to **two** kinds of bridge:
- **Registry ID aliases** — old block/item/block-entity/menu/data-component IDs mapped to current ones. Registered in the `ALIAS` static classes of `LogisticsCore` / `LogisticsAutomation` / `LogisticsPipe` via `registerItemAlias` / `registerBlockAlias` / `registerBlockEntityAlias` / `PlatformService.registerAlias`.
- **NBT save migrations** — old saved-state formats read on load. Implemented as `loadLegacyData` overrides on block entities (the live format is read by `loadLogisticsData`).

**Rule of thumb:** keep a bridge only if its old form was the **live (written) form in some release of the previous major**. Drop anything whose old form was already gone by the previous major's first release. When a new major opens, prune the bridges that now fall outside the window.

**Consequence for players:** worlds must be loaded in the **latest release of each major in sequence** before skipping ahead (e.g. pre-`0.6` → latest `0.6.x` → `0.7+`). Skipping a major may silently drop blocks/items or saved machine state. Note this in release notes whenever bridges are pruned.

## Architecture

This is a multiloader Minecraft mod organized into **independent domains** following **SOLID principles** to maximize maintainability.

The domain architecture applies the **Dependency Inversion Principle** (DIP) - domains depend on abstractions in `core.lib`, not on each other. This enables:
- Decoupled domains that can be tested and modified independently
- Potential future modular packaging (splitting into separate JARs)
- Clear separation of concerns with minimal cross-domain dependencies

### Multiloader Architecture Rules

The repository is split into shared code plus loader-specific adapters:

```
common/src/main/      # Shared server/common code; no Fabric or NeoForge imports
common/src/client/    # Shared vanilla client code; Minecraft client imports allowed, no Fabric/NeoForge imports
fabric/src/main/      # Fabric server/common adapter code
fabric/src/client/    # Fabric client adapter code and Fabric-only client wiring
neoforge/src/main/    # NeoForge adapter code; client-only code must stay under com.logistics.neoforge.client
```

**Hard rules:**
- `common/src/main` and `common/src/client` must stay loader-agnostic. Do not import `net.fabricmc.*`, Fabric API, `net.neoforged.*`, or NeoForge APIs there.
- Loader APIs belong behind small adapter classes in `fabric/` or `neoforge/`.
- Common code talks to loader services only through abstractions in `core.lib` (`PlatformService`, `BlockEntityTypeFactory`, `EnergyCapabilityLookup`, `ItemStorageLookup`, `FluidStorageLookup`, `PipeConnectionLookup`, `ServerNetworking`, `ClientNetworking`, `ClientModelRegistry`, etc.).
- Any new loader service must have a Fabric implementation, a NeoForge implementation, and `META-INF/services/...` entries where the SPI uses `ServiceLoader`.
- Client-only Minecraft classes are allowed in `common/src/client`, but loader-specific client APIs stay in loader source sets.
- In NeoForge, client-only classes and imports must stay under `com.logistics.neoforge.client`. Shared NeoForge classes such as packet/capability/bootstrap registration must not import `net.minecraft.client.*` or common client screens/renderers.
- NeoForge clientbound payloads should register the payload type/codec in common packet registration and register the actual client handler via `RegisterClientPayloadHandlersEvent`.
- Loader-specific capability wrappers should be thin adapters. They should preserve simulation/transaction semantics and avoid changing common storage contracts to match one loader.
- When moving code from Fabric to shared client code, remove Fabric-only helper types first (for example Fabric `ItemVariant`) so NeoForge can compile the same renderer.

**Current multiloader patterns used in this branch:**
- Server/common domain initialization still uses `LogisticsCommonBootstrap` and `DomainBootstrap` services from `common/src/main/resources/META-INF/services`.
- Fabric client initialization uses `LogisticsClientBootstrap` plus client-domain services from `fabric/src/client/resources/META-INF/services`.
- NeoForge common initialization runs once from `RegisterEvent`, because built-in registries are writable during that event.
- NeoForge deferred services such as creative tabs and resource reload listeners collect registrations through common SPIs, then attach to the correct NeoForge event bus from `LogisticsNeoForge`.
- Shared dynamic renderers and extra model keys live in `common/src/client`; Fabric and NeoForge only provide model-loader/event wiring.
- NeoForge custom cable blockstate models use loader-specific blockstate JSON in `neoforge/src/main/resources`, leaving the common Fabric blockstate JSON untouched. Newer branches share one vanilla blockstate from common, because cables there are drawn by a `CableBlockEntityRenderer` in common client code. On this branch each loader still bakes its own custom model (`fabric/.../CableModel`, `neoforge/.../NeoForgeCableModel`, sharing only `CableGeometry`), so the two blockstates must stay different: NeoForge's points at a `*_cable_dynamic` model carrying `"loader": "logistics:cable_model"`, which Fabric cannot read.

### Small PR Strategy For NeoForge Work

Prefer PRs that are reviewable by one concern, but do not split a commit that only works with its pair:
- **Bootstrap/SPI PR:** common bootstrap timing, `BlockEntityTypeFactory`, `PlatformService`, resource reload, creative tab registrar, and smoke tests.
- **Runtime capabilities PR:** energy/item/fluid adapters plus capability lookup registration.
- **Networking/events PR:** packet registration, server/client payload handlers, pipe/cable tick hooks, unload cleanup, commands, and loot modifiers.
- **Client rendering PR:** NeoForge client setup, block entity renderers, model loader wiring, shared client renderers/models, and render-only resources.
- **Build/CI/docs PR:** Gradle wiring, non-blocking CI toggles, `.gitignore`, and architecture documentation.

If the branch already contains all of these, open a draft PR first and explain that it is the integration branch. If maintainers want smaller PRs, split from the branch with stacked branches or interactive cherry-picks in the order above. The safest target for NeoForge implementation work is `mc/26.2` (main); backports should be cherry-picked after the implementation PR lands.

**Throughout the codebase, follow SOLID principles:**
- **S**ingle Responsibility: Classes have one reason to change
- **O**pen/Closed: Open for extension, closed for modification
- **L**iskov Substitution: Subtypes must be substitutable for their base types
- **I**nterface Segregation: Clients shouldn't depend on interfaces they don't use
- **D**ependency Inversion: Depend on abstractions, not concretions

### Domain Structure

```
src/main/java/com/logistics/
├── LogisticsMod.java        # Entry point, initializes all domains
├── api/                     # Public API surface (LogisticsApi, TransportApi)
├── core/                    # Shared interfaces and utilities
│   ├── bootstrap/           # Domain initialization system
│   └── lib/                 # Interfaces that other domains may import
│       └── network/         # ILogisticsNetwork, IWorldView, Order, etc.
├── pipe/                    # Item transport pipes
├── power/                   # Energy generation (engines)
└── automation/              # Machines (quarry, etc.)

src/client/java/com/logistics/
├── LogisticsModClient.java  # Client entry point
├── core/                    # Client-side core utilities
├── pipe/                    # Pipe rendering
├── power/                   # Engine rendering
└── automation/              # Machine rendering (quarry laser, etc.)
```

### Domain Isolation Rules (Dependency Inversion Principle)

- **Domains must not import from each other** (no `pipe` → `power` imports)
- **All domains depend on abstractions in `core.lib`**, not on concrete implementations
- Shared interfaces live in `core.lib`, concrete implementations live in domains
- This applies DIP: high-level modules (domains) and low-level modules (implementations) both depend on abstractions (core.lib)

**Benefits:**
- Domains remain decoupled and independently testable
- Changes in one domain don't cascade to others
- Enables future modular packaging (split into separate JARs)
- Clear separation of concerns

### Bootstrap System

Domains are initialized using a two-phase pattern (server/common + client):

**Server/Common initialization (ServiceLoader):**
1. **Fabric entry point**: `fabric.mod.json` declares `LogisticsMod` as the main entry point.
2. **NeoForge entry point**: `neoforge.mods.toml`/`@Mod("logistics")` constructs `LogisticsNeoForge`, which calls `LogisticsCommonBootstrap.initialize()` once from `RegisterEvent`.
3. **Discovery**: `LogisticsCommonBootstrap.initialize()` calls `DomainBootstraps.all()` which uses Java's ServiceLoader.
4. **Registration**: Each domain provides a `DomainBootstrap` implementation listed in `META-INF/services/com.logistics.core.bootstrap.DomainBootstrap`:
   - `com.logistics.LogisticsCore`
   - `com.logistics.LogisticsPipe`
   - `com.logistics.LogisticsPower`
   - `com.logistics.LogisticsAutomation`
5. **Initialization**: Each domain's `initCommon()` method is called to register blocks, items, etc.

**Client initialization:**
1. **Fabric entry point**: `fabric.mod.json` declares `LogisticsModClient` as the client entry point.
2. **Fabric discovery**: `LogisticsModClient` calls `LogisticsClientBootstrap.initialize()`, which discovers `ClientDomainBootstrap` implementations via `fabric/src/client/resources/META-INF/services/com.logistics.core.bootstrap.ClientDomainBootstrap`.
3. **Fabric model loading**: after client-domain bootstraps register model keys, `FabricModelLoader.setup()` maps `ClientModelRegistry` entries into Fabric extra models.
4. **NeoForge entry point**: `LogisticsNeoForge` calls `NeoForgeClientSetup.register(modBus)` only when `FMLEnvironment.getDist().isClient()`.
5. **NeoForge client wiring**: `NeoForgeClientSetup` registers screens, BERs, block colors, client payload handlers, and model loaders on NeoForge client/mod events.

**Adding a new domain:**
1. **Create packages**:
   - `common/src/main/java/com/logistics/newdomain/` - loader-agnostic server/common code
   - `common/src/client/java/com/logistics/newdomain/` - loader-agnostic client code, if reusable across loaders
   - loader-specific packages under `fabric/` or `neoforge/` only for loader API wiring
2. **Implement server bootstrap**:
   - Create `LogisticsNewDomain implements DomainBootstrap`
   - Implement `initCommon()` for registration
   - Add to `META-INF/services/com.logistics.core.bootstrap.DomainBootstrap`
3. **Implement Fabric client bootstrap when needed**:
   - Create `LogisticsNewDomainClient implements ClientDomainBootstrap`
   - Implement `initClient()` for Fabric client registration
   - Add it to `fabric/src/client/resources/META-INF/services/com.logistics.core.bootstrap.ClientDomainBootstrap`
4. **Implement NeoForge client wiring when needed**:
   - Add NeoForge-specific client registrations to `NeoForgeClientSetup` or a small helper under `com.logistics.neoforge.client`
   - Keep NeoForge client event handlers and model loader code out of common packages

### Domain Details

**For detailed architecture and design philosophy, see:**
- [Documentation](https://indemnity83.github.io/logistics/) - Vision, three-tier model, pipe specifications, and more
- Source code in `src/main/java/com/logistics/{domain}/` for implementation details

**Current domain patterns:**

**Core Domain** (`com.logistics.core`):
- **Foundation for all domains** - provides shared abstractions via `core.lib`
- **Dependency Inversion**: All domains depend on `core.lib` interfaces/abstracts, not on each other
- Contains core game elements: tools (wrenches), crafting intermediates, shared utilities, and machines that don't belong to a single domain (e.g. Macerator)
- **Key abstractions in `core.lib`**:
  - `AbstractEngineBlockEntity` - base for all engines
  - `DomainBootstrap` - interface for domain initialization
  - `BaseBlockEntity` - base class for block entities with common NBT/tick patterns
  - `HasItemStorage`, `HasEnergyStorage`, `HasFluidStorage` - capability interfaces block entities implement
  - `core.lib.network` - logistics network contracts (`ILogisticsNetwork`, `IWorldView`, `INetworkGraph`, `Order`, `IngredientChecker`, `ProviderCanFulfill`)
  - Shared interfaces that enable cross-domain functionality without coupling
- Think of `core.lib` as the "contract layer" that keeps domains decoupled

**Pipe Domain** (`com.logistics.pipe`):
- Module composition: `Pipe` composes `Module` instances for behavior
- Module types: `ProviderModule`, `SupplierModule`, `RequesterModule`, `SinkModule`, `NetworkRouterModule`, `CraftingModule`
- Module ordering: CraftingModule runs first, NetworkRouterModule runs last
- Network layer: `NetworkRegistry`, `PipeNetwork` (implements `ILogisticsNetwork`), `NetworkController`, `NetworkGraph`
- `TravelingItem` represents items in transit with progress-based movement
- See [documentation](https://indemnity83.github.io/logistics/) for comprehensive pipe architecture

**Power Domain** (`com.logistics.power`):
- Engine hierarchy: `AbstractEngineBlockEntity` base class
- Heat management: COLD → COOL → WARM → HOT → OVERHEAT stages
- Integrates with Team Reborn Energy API
- Types: Redstone Engine, Stirling Engine (with fuel), Creative Engine

**Automation Domain** (`com.logistics.automation`):
- Kiln: RF-powered electric furnace (smelts any vanilla smelting recipe)
- Laser Quarry: Mining machine with frame and laser rendering
- Expandable for future machines

## Code Style

- **Formatting:** Automated via Spotless (minimal rules for consistency)
- **Single-line if/for allowed** but braces preferred for multi-line
- Keep nesting depth reasonable (prefer max 3 levels)
- **Comments: terse, and about the code — not the change.** State the non-obvious *what* a future reader needs; don't narrate why this edit was made, version history, or context around the change (meaningless to the next reader). Put change rationale in the commit/PR, not the code.

### Code Formatting (Spotless)

This project uses Spotless for minimal automated formatting to ensure consistency:
- Remove unused imports
- Trim trailing whitespace
- End files with newline

**Setup (one-time per developer):**
```bash
./gradlew installGitHooks
```

This installs a pre-commit hook that automatically formats code before each commit.

**Manual formatting:**
```bash
./gradlew spotlessApply    # Format all files
./gradlew spotlessCheck    # Check formatting without changes
```

**Skipping the hook:**
```bash
git commit --no-verify     # Skip pre-commit formatting check
```

Use `--no-verify` when making infrastructure commits (like the initial spotless setup) or when you plan to format in a separate commit.

**CI Enforcement:** Pull requests must pass `spotlessCheck` before merging.

**Note:** The formatting rules are intentionally minimal to avoid churny diffs and maintain easy cherry-picking between branches.

## Commit Messages

Output a SINGLE-LINE commit subject only:
- No conventional-commit prefix (no "feat:", "fix:", etc.)
- No scope, no body, no co-author trailer
- Imperative mood ("Add", "Fix", "Refactor")
- Aim for <= 72 characters
- Be specific about what changed

**Note:** While individual commits don't use conventional format, this keeps diffs minimal and makes history easier to read. The PR title will use conventional format for release-please.

## Pull Requests

Use scoped conventional commit format for PR titles:
```text
<type>(<scope>): <description>
```

**PR body should read like release notes:**
- Focus on WHAT changed and WHY it matters
- Use short sections: Summary / Changes / Notes
- Bullet points, grouped and scannable
- No low-level implementation details unless they affect behavior or compatibility

## Release notes and PR title strategy

This repository uses Release Please with Conventional Commit-style PR titles.

Release notes are intended for players/users of the mod, not primarily for developers. When creating PR titles, commit messages, or squash merge titles, prefer wording that describes the player-visible effect of the change.

Use this format:

```text
type(scope): short description
```

Examples:

```text
feat(automation): add the sawmill
balance(automation): increase the laser quarry frame material cost
change(automation): rename Wood Pulp to Sawdust
fix(neoforge): fix startup crash on NeoForge
remove(automation): drop the unused crusher recipe
perf(automation): reduce idle machine tick cost
refactor(common): simplify platform service lookup
build(fabric): update publishing task
chore(release): update release metadata
```

### Commit types and changelog sections

The changelog follows [Keep a Changelog](https://keepachangelog.com). Each type maps to a section:

| Type | Changelog section | Use for |
|---|---|---|
| `feat` | Added | new player-facing capability, block, item, or machine |
| `balance` | Changed | recipe / cost / rate / output / power / progression tuning (buffs and nerfs) |
| `change` | Changed | other player-facing change (rename, restyle, behavior shift) |
| `perf` | Changed | player-visible performance improvement |
| `deprecate` | Deprecated | content/behavior marked for future removal |
| `remove` | Removed | removed content or behavior |
| `fix` | Fixed | corrected broken or unintended behavior |
| `security` | Security | security-sensitive fix |

Prefer `balance` over `change` when the change is gameplay tuning (recipes, costs, rates,
progression); `balance` is honest about both buffs and nerfs without implying an improvement.

### Internal types

These types are allowed but hidden from the changelog (developer-facing):

| Type | Use for |
|---|---|
| `refactor` | code restructuring with no player-visible change |
| `test` | test coverage |
| `build` | Gradle, publishing, or build system changes |
| `ci` | GitHub Actions or automation changes |
| `chore` | maintenance work |
| `docs` | documentation-only changes |
| `revert` | revert a previous commit/PR |

### Multi-change squash commits

When one PR makes several distinct player-facing changes, record each as its own changelog entry by
putting one Conventional Commit line per change in the squash commit BODY (the subject is the first
entry). Release Please parses each `type(scope): description` line into a separate changelog entry:

```text
feat(automation): add the sawmill

change(automation): rename Wood Pulp to Sawdust
balance(automation): move wood processing from the macerator to the sawmill
remove(automation): drop the macerator's wood-pulp recipes
```

### Allowed scopes

Scopes identify the mod's product surface or implementation area.

**Product scopes** — major player-recognizable systems, machines, and feature areas:
`macerator`, `kiln`, `quarry`, `sawmill`, `pump`, `transport`, `routing`, `fluids`, `pipes`,
`energy`, `storage`, `crafting`, `worldgen`, `ui`.

**Framework / internal scopes:** `core`, `automation`, `api`, `common`.
**Platform scopes:** `fabric`, `neoforge`, `compat`.
**Project scopes:** `docs`, `build`, `ci`, `release`.

Pipe-related scope rules:
- `transport` — basic item transport pipes
- `routing` — smart/routed/requested item logistics pipes
- `fluids` — fluid pipes, tanks, handlers, transfer rules, pipe sealant, and general fluid mechanics
- `pipes` — shared pipe framework code used across multiple pipe families (use `transport`/`routing`/`fluids` for one family)

Other rules:
- Use `automation` only for the shared machine framework or behavior that crosses multiple machines
  (shared components, machine base classes, reusable output handling, chunk loading, upgrade seams).
- Use `core` for low-level shared infrastructure that is not specific to machines.
- Use `common` for shared implementation code with no better product/framework scope.
- Use `fabric` and `neoforge` for loader-specific implementation work.
- Repo automation: `build` (Gradle, mappings, dependencies, publishing), `ci` (GitHub Actions /
  validation), `release` (release-please, versions, changelog configuration).
- Do not use `logistics` (too broad — it's the mod name; use `routing` for smart-pipe behavior) or
  `fluid-pipes` (folded into `fluids`).

### Changelog-readable subjects

Let the scope carry the main product noun, but keep enough context that the changelog line stands on
its own — roughly **4–8 words** after the colon. Prefer changelog-readable over ultra-short.

Good:

```text
balance(macerator): standardize recipe around machine components
change(kiln): restyle with machine shell
fix(pump): stop destroying waterlogged blocks
feat(transport): add gold item pipe
fix(routing): respect destination priority
feat(fluids): add void fluid pipe
refactor(pipes): share connection logic
refactor(automation): extract ChunkLoadingComponent
```

Too short (changelog becomes vague):

```text
balance(macerator): rework recipe
change(kiln): restyle machine
fix(automation): stop voiding byproducts
```

Too long / implementation-heavy (the scope already says the machine):

```text
balance(macerator): rework the macerator recipe to use the shared machine components
change(kiln): restyle the kiln to match the shared machine-component visual design
```

For the standardized machine recipes, prefer wording like
`balance(<machine>): standardize recipe around machine components` — these recipes share a consistent
structure (machine frame + redstone coil + machine-specific ingredients, e.g. tanks/buckets for the
pump, a pickaxe for the quarry, flint for the macerator).

### Guidance for agents

When creating or modifying PRs:

- Use scoped Conventional Commit PR titles.
- Think about the generated changelog before choosing the PR title.
- Prefer player-facing wording and a changelog-visible type — most commonly `feat`, `balance`, `change`, `fix`, and `remove` (the type table above lists the full set, including `perf`, `deprecate`, and `security`).
- Use internal types like `refactor`, `test`, `build`, `ci`, and `chore` for non-player-facing work.
- For PRs with several player-facing changes, list each as its own Conventional Commit line in the squash body (see "Multi-change squash commits").
- Do not add `refactor`, `test`, `build`, `ci`, or `chore` to the Release Please changelog sections.
- Do not attempt to group changelog entries by scope using nested headings unless the release strategy is intentionally changed later.

## Documentation

**Architecture & Design:**
- [Documentation](https://indemnity83.github.io/logistics/) - Detailed information on pipes, power, automation, and technical design

**Development:**
- `CLAUDE.md` and `AGENTS.md` - Primary development guidance for coding agents. The two are the same document apart from their opening two lines; change one and mirror the change to the other.
- `README.md` - Project overview and user-facing documentation
- `CHANGELOG.md` - Auto-generated release notes

**Version Management:**
- `.release-please-manifest.json` - Current version per branch
- `release-please-config.json` - Release-please configuration
- `.github/workflows/prepare-release.yml` - Release automation workflow
- `.github/workflows/build-release.yml` - Build and publish workflow
