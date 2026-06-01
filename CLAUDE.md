# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Branch Strategy (IMPORTANT!)

**FIRST:** Always check your current branch using `git branch --show-current` or by checking the working directory path (e.g., `../logistics-mc-1.21.11/` indicates mc/1.21.11 branch).

This repository uses a multi-version strategy to support different Minecraft releases:
- **`mc/1.21.1`** - Stable releases for MC 1.21.1, maintenance focus
- **`mc/1.21.11`** - Stable releases for MC 1.21.11, active feature development
- **`mc/26.1`** - Snapshot development for MC 26.1 snapshots, forward-port for testing

### Critical Understanding

**Domain architecture enables cross-version cherry-picking.** After significant refactoring to isolate version-specific code:
- ✅ **Can cherry-pick** commits between branches (mc/1.21.1 ↔ mc/1.21.11 ↔ mc/26.1)
- ✅ **Can share code** across versions via git operations
- ✅ **Can maintain feature parity** with minimal manual intervention
- ✅ **Cherry-pick is the primary porting mechanism**

**Porting = git cherry-pick, with occasional conflict resolution for API differences.**

### Recommended Worktree Setup

For easier cross-version development, consider setting up git worktrees for each mc/* branch in sibling directories:
- `../logistics-mc-1.21.1/` - mc/1.21.1 branch worktree
- `../logistics-mc-1.21.11/` - mc/1.21.11 branch worktree
- `../logistics-mc-26.1/` - mc/26.1 branch worktree

The current working directory path indicates which branch you're on.

**Benefits:**
- Reference code across versions without switching branches
- Compare implementations when cherry-picking
- Resolve conflicts by viewing other version's code directly

If worktrees are detected at these paths, they may be referenced when working on cross-version changes.

### Development Strategy

- **`mc/1.21.11`**: Primary development target (latest stable release, most players)
- **`mc/26.1`**: Port features to keep up with snapshot releases
- **`mc/1.21.1`**: Backport features/fixes (many tech mod users still on this version)

### Branch Protection Rules (CRITICAL)

**Never push or commit directly to `mc/*` branches** (`mc/26.1`, `mc/1.21.11`, `mc/1.21.1`). These are protected primary branches. All work — including in auto mode — must go through a feature branch and PR.

**Required workflow for any new work:**
1. Create a feature branch first: `git checkout -b descriptive-branch-name`
2. Make commits on the feature branch
3. Push the feature branch: `git push origin descriptive-branch-name`
4. Open a PR targeting the appropriate `mc/*` branch

**The only exception** is cherry-picking between `mc/*` branches for porting already-merged commits. Even then, confirm with the user before pushing.

**In auto mode:** Still pause and confirm before any `git push` when the current branch is `mc/*` or when no feature branch has been created yet. A wrong push to a protected branch is very hard to undo cleanly.

### Cross-Version Workflow

**When fixing bugs:**
1. Fix on the branch where reported (typically mc/1.21.11 during Pre-1.0)
2. Check if bug exists on other branches
3. **Cherry-pick** the fix to affected branches (resolve conflicts if needed)
4. Test on each target branch after cherry-pick
5. Priority order for porting: mc/1.21.11 → mc/26.1 → mc/1.21.1

**When adding features:**
- **During Pre-1.0 phase**: Develop on mc/1.21.11, cherry-pick to mc/26.1 and mc/1.21.1
- **After MC 26.1 releases**: Develop on mc/26.1, backport to mc/1.21.11 if needed
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

When a critical bug needs a patch release *after* feature development has already started on `mc/1.21.11`, use this process to bypass release-please and publish a clean hotfix.

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

**Three tools to manage this:**

**1. "Up Next" label** — Applied to PRs that are approved but intentionally held past the current patch release.
- Rule: If a release-please PR for the current version is still open, apply `up next` to any `feat:` PR instead of merging it.
- Transition: When the patch release ships, strip `up next` — those PRs are now in scope for the next minor.

**2. Snapshot testing** (`build-snapshot.yml`) — After feature PRs merge, leave the release-please PR open and publish snapshot builds for community testing. Bugs found here accumulate as `fix:` commits in the upcoming release.

**3. Pre-release gate** (`build-prerelease.yml`) — When ready to commit to the release, publish `v0.6.0-pre.1`. This signals "final testing phase." When satisfied, merge the release-please PR.

**Full cycle:**
```
fix: PRs merge → release-please opens vX.Y.Z patch PR
Apply "up next" label to any feat: PRs that should wait
Merge patch release-please PR → vX.Y.Z ships → cherry-pick to other branches
Strip "up next" from held PRs
Merge feat: PRs → release-please opens vX.Y+1.0 PR (leave it open)
Publish snapshots via build-snapshot.yml → community testing
Fix bugs (accumulate in vX.Y+1.0 release notes)
Publish vX.Y+1.0-pre.1 via build-prerelease.yml → final gate
Merge vX.Y+1.0 release-please PR → ships → cherry-pick
```

**Use the Hotfix Workflow (above) only when:** v0.6.0 is already out AND new features have already merged, making a v0.5.x backport necessary.

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
- NeoForge custom cable blockstate models use loader-specific blockstate JSON in `neoforge/src/main/resources`, leaving the common Fabric blockstate JSON untouched.

### Small PR Strategy For NeoForge Work

Prefer PRs that are reviewable by one concern, but do not split a commit that only works with its pair:
- **Bootstrap/SPI PR:** common bootstrap timing, `BlockEntityTypeFactory`, `PlatformService`, resource reload, creative tab registrar, and smoke tests.
- **Runtime capabilities PR:** energy/item/fluid adapters plus capability lookup registration.
- **Networking/events PR:** packet registration, server/client payload handlers, pipe/cable tick hooks, unload cleanup, commands, and loot modifiers.
- **Client rendering PR:** NeoForge client setup, block entity renderers, model loader wiring, shared client renderers/models, and render-only resources.
- **Build/CI/docs PR:** Gradle wiring, non-blocking CI toggles, `.gitignore`, and architecture documentation.

If the branch already contains all of these, open a draft PR first and explain that it is the integration branch. If maintainers want smaller PRs, split from the branch with stacked branches or interactive cherry-picks in the order above. The safest target for NeoForge implementation work is `mc/26.1`; backports should be cherry-picked after the implementation PR lands.

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
feat(pipes): add priority routing mode
fix(neoforge): fix startup crash on NeoForge
perf(automation): reduce idle machine tick cost
refactor(common): simplify platform service lookup
test(pipes): add routing tests
build(fabric): update publishing task
ci(build): update Gradle cache settings
chore(release): update release metadata
chore(update): update dependencies
```

### Changelog-visible types

Only these types should appear in generated player-facing release notes:

| Type | Changelog section | Use for |
|---|---|---|
| `feat` | Added | New player-visible behavior |
| `fix` | Fixed | Player-visible bug fixes |
| `perf` | Improved | Player-visible performance improvements |

### Internal types

These types are allowed, but should be treated as internal/developer-facing and should not be included in Release Please changelog sections:

| Type | Use for |
|---|---|
| `refactor` | Code cleanup without player-visible behavior changes |
| `test` | Test coverage |
| `build` | Gradle, publishing, or build system changes |
| `ci` | GitHub Actions or automation changes |
| `chore` | Maintenance work |
| `docs` | Documentation-only changes |
| `revert` | Revert a previous commit/PR |

### Allowed scopes

Use the scope that best describes the player-facing area of the mod affected:

| Scope | Use for |
|---|---|
| `core` | Shared mod behavior, base blocks/items, recipes, world behavior |
| `automation` | Machines, upgrades, automation logic |
| `pipes` | Pipes, routing, extraction, insertion, pipe networks |
| `energy` | Energy transfer and storage behavior |
| `storage` | Inventories, storage blocks, adapters |
| `ui` | Screens, tooltips, overlays, visible rendering/UX |
| `compat` | Integration with other mods |
| `fabric` | Fabric-specific user-visible behavior |
| `neoforge` | NeoForge-specific user-visible behavior |
| `docs` | README, wiki, or user documentation |
| `release` | Release metadata |
| `update` | Dependency and automated update PRs |
| `common` | Shared/internal code changes with no better user-facing scope |
| `build` | Build tooling or CI support scope |

Prefer player-facing scopes for `feat`, `fix`, and `perf`.

Good:

```text
fix(pipes): fix items getting stuck when a route becomes invalid
```

Avoid for player-facing changes:

```text
fix(common): invalidate pipe graph cache on neighbor update
```

The second title may be technically accurate, but the first produces better release notes.

### Guidance for agents

When creating or modifying PRs:

- Use scoped Conventional Commit PR titles.
- Think about the generated changelog before choosing the PR title.
- Prefer player-facing wording for `feat`, `fix`, and `perf`.
- Use internal types like `refactor`, `test`, `build`, `ci`, and `chore` for non-player-facing work.
- Do not add `refactor`, `test`, `build`, `ci`, or `chore` to the Release Please changelog sections.
- Do not attempt to group changelog entries by scope using nested headings unless the release strategy is intentionally changed later.

## Documentation

**Architecture & Design:**
- [Documentation](https://indemnity83.github.io/logistics/) - Detailed information on pipes, power, automation, and technical design

**Development:**
- `CLAUDE.md` (this file) - Primary development guidance for Claude Code
- `README.md` - Project overview and user-facing documentation
- `CHANGELOG.md` - Auto-generated release notes

**Version Management:**
- `.release-please-manifest.json` - Current version per branch
- `release-please-config.json` - Release-please configuration
- `.github/workflows/prepare-release.yml` - Release automation workflow
- `.github/workflows/build-release.yml` - Build and publish workflow
