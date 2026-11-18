# AGENTS.md

This file provides guidance to codex when working with code in this repository.

## Branch Strategy (IMPORTANT!)

**FIRST:** check your current branch with `git branch --show-current`, or from the working
directory path (`../logistics-mc-1.21.11/` is the mc/1.21.11 branch).

Each Minecraft release has its own long-lived branch:

- **`mc/26.3`** — **main/default branch** (the one `origin/HEAD` tracks): newest *stable* MC
  release. New features, refactors and fixes start here.
- **`mc/26.2`**, **`mc/26.1`**, **`mc/1.21.11`** — maintenance and backport targets.
- **`mc/1.21.1`** — maintenance and backport target; many tech-mod players are still on it.

**Roles advance over time:** when a newer MC release ships *and is stable*, main moves to that
`mc/2x.y` branch and the previous one becomes a backport target. A pre-release branch does
**not** become main and does not shift the backport chain until it ships — it trails main as a
forward-port target. There is no pre-release branch at present. Never hardcode which branch is
main: confirm with `git rev-parse --abbrev-ref origin/HEAD`. Cutting, building or pushing to a
pre-release branch: read `docs/agents/pre-release-branch.md`.

**Porting is `git cherry-pick`**, with occasional conflict resolution for API differences. The
domain architecture keeps version-specific code isolated so commits apply across all five
branches. Sibling worktrees, when present, let you read another version's code directly:
`../logistics-mc-1.21.1/`, `../logistics-mc-1.21.11/`, `../logistics-mc-26.1/`,
`../logistics-mc-26.2/`, `../logistics-mc-26.3/`.

### Cross-Version Workflow

**When fixing bugs:**
1. Fix on **mc/26.3** (main) when the bug exists there.
2. Check whether the bug exists on the other branches.
3. Cherry-pick the fix to each affected branch, resolving conflicts for that branch's API, and
   test on each target after the cherry-pick.
4. Port in order: mc/26.3 → mc/26.2 → mc/26.1 → mc/1.21.11 → mc/1.21.1.
5. **Legacy-only bugs** (don't reproduce on mc/26.x — e.g. 26.x auto-derives the cutout render
   layer from sprite transparency, so manual `BlockRenderLayerMap` registrations only matter on
   1.21.x) are the exception: originate the fix on the highest *affected* branch (mc/1.21.11) and
   cherry-pick down to mc/1.21.1.

**When adding features:** develop on mc/26.3, then backport to every branch the feature applies
to. Internal and infra work is backported too: keep all branches as close to in sync as
possible, since closer branches make every future cherry-pick apply cleanly. Coordinate large
refactorings across all branches.

### Writing Cherry-Pick-Friendly Code

- **Compare implementations** in the sibling worktrees before committing, and match their
  method names, signatures, file layout and formatting.
- **Isolate version-specific code:** keep Minecraft API calls in specific methods or `*Compat`
  classes, so the version difference lives in one place.
- Record intentional divergences in the commit message. When a cherry-pick conflicts, consider
  whether the code structure could converge.

### Branch Protection Rules (CRITICAL)

**New work never goes directly to an `mc/*` branch.** These are protected branches (Git Town
perennials). Writing a fix or a feature — including in auto mode — means a feature branch and a
PR:

1. Create a feature branch: `git town hack descriptive-branch-name`
2. Commit on it.
3. Push it: `git town sync --push` (the explicit `--push` guarantees the branch reaches the
   remote whatever the user's Git Town push defaults; or let `propose` do it).
4. Open a PR against the appropriate `mc/*` branch: `git town propose`.

**Porting an already-merged commit pushes directly.** `git push origin HEAD:mc/1.21.1` after a
cherry-pick is the *sanctioned* way to keep the version branches in parity, and the
`version-branches` ruleset permits it. Confirm with the user before pushing. The default branch
is the one place this does not apply (see below).

**Issue each push as its own command.** Permission rules match a single command; a `for` loop or
an `&&` chain that ports to several branches at once matches no rule and falls through to the
permission classifier, which then has to judge a compound string containing a push to a protected
branch. Batch the cherry-picks if you like, but run one `git -C <worktree> push origin
HEAD:<branch>` per branch.

**In auto mode:** still pause and confirm before any push when the current branch is `mc/*` or no
feature branch exists yet. A wrong push to a protected branch is very hard to undo cleanly.

The tag-based hotfix branch is the other direct-push exception; see `docs/agents/releases.md`.

#### How the rulesets enforce this

| Ruleset | Targets | Bypass |
|---|---|---|
| `default-branch` | `~DEFAULT_BRANCH` | **none** |
| `version-branches` | `refs/heads/mc/**` | repository admin |

Both require a PR, the same required status checks, and resolution of every review thread. The
default branch matches both, and bypassing one ruleset does not exempt you from the other — so
**nothing can be pushed directly to the default branch, by anyone, including admins and agent
sessions.** The other `mc/*` branches match only `version-branches`, where the admin bypass keeps
the cherry-pick-and-push porting flow working. `~DEFAULT_BRANCH` resolves dynamically, so the
roles follow when main moves.

**Port *up* into the default branch through a PR.** Porting down or sideways stays a direct push.
A fix that originates on a non-default branch and must reach the default branch needs a normal PR
for that hop: the adapted commit is different code from what was reviewed, and the default
branch is where review happens. So if a change affects several versions, propose it on the
default branch first. Legacy-only fixes are unaffected: they are cherry-picked *down* and never
touch the default branch.

**Direct pushes run checks locally first.** The `pre-push` hook (installed by `./gradlew
installGitHooks`) runs `spotlessCheck` on every push, and `./gradlew test` whenever the
destination is an `mc/*` branch, because a port has no PR and nothing else has tested the adapted
code before it lands. Game tests stay in CI. Pre-release branches skip the hook. Bypass with
`git push --no-verify` only in an emergency.

### Git Town

Git Town is configured by [`.git-town.toml`](.git-town.toml) in the repo root — main branch
`mc/26.3`, `^mc/` perennial, `^release-please--` observed, ship via the GitHub API. It is
committed, so a fresh clone is already configured. Git Town runs headlessly here — no interactive
prompts, safe from scripts and agents. Prefer it over plain `git` for the feature-branch flow:

| Instead of… | Use |
|---|---|
| `git checkout -b <branch>` (off main) | `git town hack <branch>` |
| `git checkout -b <branch>` (off current branch, stacked) | `git town append <branch>` |
| `git pull` / manually merging main into a feature branch | `git town sync` |
| Opening a PR by hand | `git town propose` |
| Squash-merging a PR | `python3 tools/ship_pr.py <N>` (runs `git town ship` for you — see below) |
| Deleting a merged/obsolete feature branch | `git town delete` |
| `git checkout <branch>` when you don't remember the name | `git town switch` |

**Ship with `python3 tools/ship_pr.py <N>`.** Squash merges here keep only the PR title
(`squash_merge_commit_message: BLANK`), so changelog lines in a PR body never reach the commit on
their own. The script writes them in from the PR's override block (see `docs/agents/changelog.md`),
makes the subject `<title> (#N)`, and ships through `git town ship` (or `gh pr merge` when the
branch is checked out in another worktree). `--dry-run` prints the message without merging. A
hand-typed `git town ship -m` replaces the *whole* message, which is how a headline entry or its
`(#N)` link gets lost.

**Change repo-level settings in the file, never with `git config`.** Local git metadata silently
wins over `.git-town.toml`, and list settings like `perennials` *merge* rather than replace.
`git config --local --list | grep '^git-town\.'` should come back empty. Branch lineage
(`git-town-branch.<name>.parent`, `.branchtype`) is the exception: it describes *your* in-flight
branches and belongs in local metadata, where Git Town writes it.

A worktree on a branch that predates the file has *no* config, and Git Town hard-errors with "no
main branch configured". Fix it with `git town sync`, which picks the file up from the parent.

**Plain `git` stays for:** cherry-picks between `mc/*` branches, the tag-based hotfix branch, and
read-only inspection (`git status`, `git log`, `git branch --show-current`, `git diff`).

### Parallel sessions

Several sessions can work different PRs at once, each in its own worktree. **Shipping and porting
are serialised behind the ship lock**: before `tools/ship_pr.py` or a port, read
`docs/agents/parallel-sessions.md` for the lock script and its release rules. Hard constraints,
whatever else you do:

- **A branch can be checked out in only one worktree.** `git worktree add` refuses a branch that
  is checked out anywhere (`fatal: '<branch>' is already used by worktree at ...`). Look before
  you create.
- **Run commands only in worktrees you own** — no build, checkout or `pkill` in another session's
  worktree; it is probably mid-operation.
- **`pkill -f` matches prefixes.** `pkill -f "logistics-mc-1.21.1"` also kills
  `logistics-mc-1.21.11`. Anchor with a trailing slash, or inspect with `pgrep -fl` first. Generic
  patterns (`GradleWorkerMain`, `devlaunchinjector`) hit every worktree — never use them.
- **Minecraft dev servers collide on port 25565.** A second `runServer` fails to bind and dies,
  which reads as a broken port rather than a busy one. Serialise them, or give each worktree its
  own `server-port`.

## Build Commands

```bash
./gradlew build                  # Build the mod jars (<loader>/build/libs/)
./gradlew remapJar               # mc/1.21.1 and mc/1.21.11 only: obfuscation remapping; mc/26.1+ uses build
./gradlew :fabric:runClient      # Launch the Fabric client
./gradlew :neoforge:runClient    # Launch the NeoForge client
./gradlew :fabric:runServer      # Launch the Fabric server
./gradlew :neoforge:runServer    # Launch the NeoForge server
./gradlew spotlessApply          # Format; spotlessCheck checks without changing
./gradlew installGitHooks        # One-time: pre-commit formatting and pre-push checks
```

**Name the loader on the run tasks.** Loom and ModDev each register `runClient`/`runServer`, so
the root build refuses an unqualified `./gradlew runClient` rather than launching two clients, or
two servers racing for port 25565. `:common` has no mod to launch.

Toolchain and Minecraft versions are in `gradle.properties`.

### Where a dependency version goes

Dependabot's Gradle parser resolves `${…}` **only** against quoted assignments in
`build.gradle`/`build.gradle.kts`. It never reads `gradle.properties` — that file is fetched into
the update snapshot but never consulted for values, and its entries are unquoted besides. So the
file a version lives in decides whether it is ever bumped again.

| Kind of version | Where it goes | Why |
|---|---|---|
| Floating library (`junit`, `assertj`, `sentry`) | `ext { }` in the root `build.gradle` | Quoted assignment in the top-level buildfile, so `${rootProject.x}` resolves for both Gradle *and* Dependabot; still one central declaration |
| Floating plugin (`spotless`, `moddev`, `io.sentry.jvm.gradle`) | literal in `settings.gradle` | Settings is evaluated before the root project, so `ext` is not visible there. A version catalog is no help either: catalogs are evaluated after settings plugins, so `libs` accessors do not exist in `pluginManagement { plugins { … } }` ([gradle/gradle#36437](https://github.com/gradle/gradle/issues/36437)) |
| Minecraft-coupled pin (`minecraft_version`, `fabric_version`, `loom_version`, `neoforge_version`, `jei_*`, `jade_*`, `spark_*`) | `gradle.properties` | Per-branch policy, not "latest wins". Being invisible to Dependabot is the desired behavior — an automated bump would break the branch. `gradle.properties` is also the variance sink that keeps `settings.gradle` and the module build files branch-identical and cheap to cherry-pick |

**Keep floating versions out of `gradle.properties`.** One moved there silently stops being
updated, with no signal — the dashboard still looks healthy because the literal-versioned deps
keep flowing. See #1161, and #1018 where exactly that was proposed for Spotless and declined.

## Releases

Every `mc/*` branch releases through **Release Please**: the squash commit's conventional subject
becomes a changelog line, and merging a branch's release PR publishes to Modrinth and CurseForge.
Release Please owns the version numbers. Versioning and naming, the release cycle (`up next`,
alpha and RC builds) or a hotfix for a shipped version: read `docs/agents/releases.md`.

## Backward Compatibility Policy

Compatibility bridges reach back **one major only**. During pre-1.0 the "major" is the **second
version digit**: `0.7.x` keeps bridges for `0.6.x`, nothing older. Two kinds of bridge:

- **Registry ID aliases** — old block/item/block-entity/menu/data-component IDs mapped to current
  ones, in the `ALIAS` static classes of the `Logistics<Domain>` bootstraps (`registerItemAlias`,
  `registerBlockAlias`, `registerBlockEntityAlias`, `PlatformService.registerAlias`).
- **NBT save migrations** — `loadLegacyData` overrides on block entities; the live format is read
  by `loadLogisticsData`.

Keep a bridge only if its old form was the **live (written) form in some release of the previous
major**; when a new major opens, prune the ones that fall outside. Content that only ever shipped
in an alpha owes no bridge. Players must load worlds in the latest release of each major in
sequence before skipping ahead, so say so in the release notes whenever bridges are pruned.

## Architecture

A multiloader mod of **independent domains**, each depending on the abstractions in `core.lib`
rather than on each other (dependency inversion). Follow SOLID throughout. Startup wiring, how to
add a domain, and where each domain's main classes live: read `docs/agents/architecture.md`.

### Multiloader Architecture Rules

```
common/src/main/      # Shared server/common code; no Fabric or NeoForge imports
common/src/client/    # Shared vanilla client code; Minecraft client imports allowed, no Fabric/NeoForge imports
common/src/gametest/  # GameTest bodies shared by both loaders
fabric/src/main/      # Fabric server/common adapter code
fabric/src/client/    # Fabric client adapter code and Fabric-only client wiring
neoforge/src/main/    # NeoForge adapter code; client-only code must stay under com.logistics.neoforge.client
```

**Hard rules:**
- `common/src/main` and `common/src/client` stay loader-agnostic: no `net.fabricmc.*`, Fabric
  API, `net.neoforged.*` or NeoForge API imports.
- Loader APIs belong behind small adapter classes in `fabric/` or `neoforge/`.
- Common code talks to loader services only through abstractions in `core.lib`
  (`PlatformService`, `BlockEntityTypeFactory`, `EnergyCapabilityLookup`, `ItemStorageLookup`,
  `FluidStorageLookup`, `PipeConnectionLookup`, `ServerNetworking`, `ClientNetworking`, etc.).
- Any new loader service needs a Fabric implementation, a NeoForge implementation, and
  `META-INF/services/...` entries where the SPI uses `ServiceLoader`.
- Client-only Minecraft classes are allowed in `common/src/client`; loader-specific client APIs
  stay in loader source sets.
- In NeoForge, client-only classes and imports stay under `com.logistics.neoforge.client`. Shared
  NeoForge classes such as packet/capability/bootstrap registration import no
  `net.minecraft.client.*` and no common client screens or renderers.
- NeoForge clientbound payloads register the payload type/codec in common packet registration and
  the client handler via `RegisterClientPayloadHandlersEvent`.
- Loader-specific capability wrappers are thin adapters: they preserve simulation/transaction
  semantics and leave the common storage contracts alone.
- When moving code from Fabric to shared client code, remove Fabric-only helper types first (for
  example Fabric `ItemVariant`) so NeoForge can compile the same renderer.

### Domain Isolation Rules

Domains are `core`, `pipe`, `power`, `automation` and `forestry`, under `com.logistics`.

- **Domains never import from each other** (no `pipe` → `power` imports).
- Shared interfaces live in `core.lib`; concrete implementations live in domains. A feature that
  needs two domains to cooperate gets a contract in `core.lib`.

## Code Style

- **Formatting:** Spotless, with deliberately minimal rules (unused imports, trailing whitespace,
  final newline) so diffs stay small and cherry-picks clean. The pre-commit hook applies it; CI
  enforces `spotlessCheck`.
- Single-line `if`/`for` allowed; braces for multi-line.
- Keep nesting shallow (prefer max 3 levels).
- **Comments: terse, and about the code — not the change.** State the non-obvious *what* a
  future reader needs. Change rationale, version history and context around the edit go in the
  commit or PR.

## Commit Messages

Output a SINGLE-LINE commit subject only:
- No conventional-commit prefix (no "feat:", "fix:", etc.)
- No scope, no body, no co-author trailer
- Imperative mood ("Add", "Fix", "Refactor")
- Aim for <= 72 characters
- Be specific about what changed

The PR title carries the conventional format; the squash merge turns it into the commit
Release Please reads.

## PR titles

```text
<type>(<scope>): <description>
```

Release notes are written for **players**, so the title names the player-visible effect in 4–8
words after the colon.

- **Changelog types:** `feat`, `balance`, `change`, `perf`, `deprecate`, `remove`, `fix`,
  `revert`, `security`. Most common: `feat`, `balance`, `change`, `fix`, `remove`.
- **Hidden types** (non-player-facing work): `refactor`, `test`, `build`, `ci`, `chore`, `docs`.
- **Product scopes:** `macerator`, `kiln`, `quarry`, `sawmill`, `pump`, `transport`, `routing`,
  `fluids`, `pipes`, `energy`, `storage`, `crafting`, `farming`, `worldgen`, `ui`.
- **Framework scopes:** `core`, `automation`, `api`, `common`. **Platform:** `fabric`,
  `neoforge`, `compat`. **Project:** `build`, `ci`, `release`. There is no `docs` scope.

Choosing between types or scopes, wording examples, or a PR with several player-facing changes
(each gets a line in the PR body's override block): read `docs/agents/changelog.md`.

**PR body reads like release notes:** short Summary / Changes / Notes sections, bullet points,
WHAT changed and WHY it matters, implementation detail only where it affects behavior or
compatibility.

## Agent skills

### Issue tracker

Two surfaces. Standalone work (bugs, findings, feature requests) goes to GitHub Issues. Specs and the implementation tickets they break into stay local under `.scratch/<feature>/` (gitignored) so they don't flood the backlog. See `docs/agents/issue-tracker.md`.

### Triage labels

Default vocabulary (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`): labels on GitHub, `Status:` lines in `.scratch/` files. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `GLOSSARY.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.

## Documentation

- **Player wiki:** <https://logistics.fandom.com/>. Its source lives on the `docs` branch: one
  MediaWiki page per `wiki/<Page Title>.txt`, pushed by `tools/upload_to_fandom.py`. Read
  `claude.md` on that branch before editing pages (`git show origin/docs:claude.md`).
- **`CLAUDE.md` and `AGENTS.md`** are the same document apart from their opening lines; CI job
  `lint (docs)` enforces it. Edit `CLAUDE.md`, then regenerate:
  `{ sed -n '1,3p' AGENTS.md; tail -n +4 CLAUDE.md; } > /tmp/a && mv /tmp/a AGENTS.md`.
- **`docs/agents/`** holds the procedures and reference this file points at.
- `CHANGELOG.md` is generated by Release Please.
