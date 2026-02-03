# Branch and Version Management

This document explains how branches, versions, and releases are managed in this multi-version Minecraft mod project.

## Branch Structure

### Active Branches

- **mc/1.21.11** - Stable releases for Minecraft 1.21.11
- **mc/26.1** - Beta releases for Minecraft 26.1 snapshots

### Branch Purpose

**mc/1.21.11:**
- Production-ready code for Minecraft 1.21.11
- Stable releases managed by release-please
- Current development focus (where players are)
- Uses Java 21 (MC 1.21.11 requirement)
- Uses `remapJar` (MC 1.21.11 is obfuscated)

**mc/26.1:**
- Beta releases for Minecraft 26.1 snapshots
- Automated beta builds via release-please
- Prepares for when MC 26.1 goes stable
- Uses Java 25+ (MC 26.1+ requirement)
- Uses `jar` task (MC 26.1+ is NOT obfuscated)

## Version Management

### This Branch (mc/1.21.11)

**Managed by:** release-please (automatic)

**Version format:**
- Releases: `0.3.0`, `0.3.1`, `0.4.0`
- Git tags: `logistics-mc1.21.11-v0.3.0`
- Artifacts: `logistics-1.21.11-0.3.0.jar`
- Published as: `mc1.21.11-0.3.0`

**How it works:**
1. Push commits with conventional commit messages (`fix:`, `feat:`, etc.)
2. Release-please automatically creates a release PR
3. Merge the PR to create a GitHub release
4. Release workflow builds and publishes to Modrinth/CurseForge

**When versions bump:**
- `fix:` commits → patch version (0.3.0 → 0.3.1)
- `feat:` commits → minor version (0.3.0 → 0.4.0)
- `feat!:` or breaking changes → major version (0.3.0 → 1.0.0)

### mc/26.1 Branch

**Managed by:** release-please with `prerelease: true` (automatic betas)

**Version format:**
- Beta releases: `0.3.0-beta.1`, `0.3.0-beta.2`, `0.4.0-beta.1`
- Git tags: `logistics-mc26.1-v0.3.0-beta.1`
- Artifacts: `logistics-26.1-0.3.0-beta.1.jar`
- Published as: `mc26.1-0.3.0-beta.1`

**How it works:**
1. Push commits with conventional commit messages
2. Release-please creates beta release PRs
3. Merge to trigger automated beta publishing
4. Published to Modrinth as beta (not featured)

## Naming Conventions

### Git Tags

Each branch gets unique tag prefixes to avoid collisions:

- mc/1.21.11: `logistics-mc1.21.11-v0.3.0`
- mc/26.1: `logistics-mc26.1-v0.3.0-beta.1`

### Artifact Names

Format: `logistics-{mc-version}-{mod-version}.jar`

Examples:
- `logistics-1.21.11-0.3.0.jar`
- `logistics-26.1-0.3.0-beta.1.jar`

### Published Versions

Format: `mc{mc-version}-{mod-version}`

Examples:
- `mc1.21.11-0.3.0`
- `mc26.1-0.3.0-beta.1`

**Display names:**
- "Logistics 0.3.0 for mc1.21.11"
- "Logistics 0.3.0-beta.1 for mc26.1"

## Release Workflows

### Release Process (mc/1.21.11 - this branch)

1. **Development**: Push commits with conventional commit messages
   ```bash
   git commit -m "fix: correct pipe rendering issue"
   git commit -m "feat: add heat indicators to engines"
   ```

2. **Release PR**: Release-please automatically creates PR with:
   - Updated CHANGELOG.md
   - Version bump in .release-please-manifest.json
   - Tag: `logistics-mc1.21.11-v0.3.1`

3. **Release**: Merge PR → release-please creates GitHub release

4. **Build**: `build-release.yml` workflow:
   - Checks out release tag
   - Runs `./gradlew remapJar`
   - Publishes to Modrinth/CurseForge/GitHub

### Beta Process (mc/26.1)

1. **Development**: Push commits to mc/26.1
2. **Release PR**: Release-please creates beta release PR
3. **Publish**: Merge PR → automated beta build and publish
4. **Distribution**: Published to Modrinth as beta (not featured)

### PR Builds

Pull requests to any `mc/**` branch:
- Run `check-code.yml` (formatting checks)
- Run `build-pr.yml` (build and upload artifact)
- Artifacts available in GitHub Actions for testing

## Development Strategy

### Which Branch Should I Develop On?

**Current Phase: Pre-1.0, MC 26.1 in Snapshot**

Develop on **mc/1.21.11** (this branch):
- Players are on stable MC 1.21.11
- Racing to 1.0 before MC 26.1 releases
- Focus where users actually are

Port to **mc/26.1** occasionally:
- Keeps mc/26.1 current with MC 26.1 snapshots
- Manual re-implementation (not cherry-picks)
- Prepares for when MC 26.1 goes stable

**Future Phase: After MC 26.1 Releases**

Develop on **mc/26.1**:
- Latest MC features and APIs
- Active development happens here

Port to **mc/1.21.11** when needed:
- Critical fixes only
- Plan for eventual end-of-life

### Bug Fixes Across Branches

For bugs that affect both branches:

1. Fix on the branch where it was reported
2. Check if bug exists on other branch
3. **Re-implement** the fix (don't cherry-pick):
   - Understand the root cause
   - Write equivalent fix for each branch's API
   - Test independently

**Why not cherry-pick?** MC 26.1 has extensive API changes. Almost every Minecraft method call is different. Cherry-picking will fail or produce broken code.

## Branch Independence

### Each Branch is Separate

The branches are **independent codebases** that share:
- Project structure
- Design patterns
- Core concepts
- Documentation approach

But NOT:
- Actual code
- Git commits
- Feature parity

### Version Independence

Versions can and will diverge:

```
mc/1.21.11: 0.3.0 → 0.3.1 → 0.4.0 → 1.0.0
mc/26.1:    0.3.0-beta.1 → 0.3.0-beta.2 → 0.4.0-beta.1
```

**This is normal and expected.** Each branch:
- Releases independently
- Has different maturity levels
- Serves different player bases
- Has different feature sets

## GitHub Workflows

### On This Branch (mc/1.21.11)

- `prepare-release-mc-1.21.11.yml` - Creates release PRs
- `build-release.yml` - Builds releases
- `build-pr.yml` - Builds PRs
- `check-code.yml` - Code quality checks

### On mc/26.1 Branch

- `prepare-release-mc-26.1.yml` - Creates beta release PRs
- `build-release.yml` - Builds beta releases
- `build-pr.yml` - Builds PRs
- `check-code.yml` - Code quality checks

## Version Strategy

### Pre-1.0 (Current)

Both features and minor versions bump the second digit:

```
0.3.0 → 0.3.1 (bug fix)
0.3.1 → 0.4.0 (new feature)
```

### Post-1.0 (Recommended)

**Use major version bumps for new Minecraft versions:**

```
mc/1.21.11: 1.0.0 → 1.0.1 → 1.0.2 (patches only)
mc/26.1:    2.0.0 (when MC 26.1 goes stable, major bump for API changes)
```

**Why major versions?** MC 26.1 IS a breaking change:
- Almost every Minecraft method changed
- Package structures reorganized
- New/removed APIs
- Un-obfuscated code
- Java 25 required

This deserves a major version bump per SemVer.

## MC 26.1 Changes

The mc/26.1 branch targets Minecraft 26.1+, which introduces major changes:

### No More Obfuscation

**MC 1.21.11 and earlier (this branch):**
- Minecraft ships obfuscated (meaningless names)
- Mods use `remapJar` to deobfuscate
- Build: `./gradlew remapJar`

**MC 26.1 and later:**
- Minecraft ships un-obfuscated (readable names)
- No remapping needed
- Build: `./gradlew jar`

### Java 25 Required

- **mc/1.21.11 (this branch)**: Java 21
- **mc/26.1**: Java 25

### API Changes

Almost every Minecraft API changed:
- Method signatures
- Package structures
- Class hierarchies
- Registration systems
- Rendering pipeline

**This is why branches must be independent.**

## Conventional Commits

Use conventional commit messages for automatic changelog generation:

**Bug fixes:**
```bash
git commit -m "fix: correct pipe rendering in dark mode"
```

**New features:**
```bash
git commit -m "feat: add quantum pipes"
```

**Breaking changes:**
```bash
git commit -m "feat!: redesign storage API"
# or
git commit -m "feat: redesign storage API

BREAKING CHANGE: Storage API methods renamed"
```

**Other types:**
- `docs:` - Documentation only
- `style:` - Formatting changes
- `refactor:` - Code changes that neither fix bugs nor add features
- `perf:` - Performance improvements
- `test:` - Adding or updating tests
- `chore:` - Build process or tooling changes

## FAQ

### Q: Why are there multiple branches?

**A:** Different Minecraft versions have incompatible APIs. MC 26.1 changed almost every method call. Supporting both requires separate codebases.

### Q: Can I use the same code on both branches?

**A:** No. The APIs are too different. You must re-implement features for each branch.

### Q: Will all features be on all versions?

**A:** No. Feature parity is not realistic. Each version will have different capabilities based on:
- MC API availability
- Development priorities
- Player demand

### Q: How do I create a release?

**A:**
1. Push commits with conventional commit format
2. Wait for release-please to create a PR
3. Review and merge the PR
4. Release-please creates the GitHub release
5. `build-release.yml` automatically builds and publishes

### Q: How do I update versions?

**A:** You don't manually update versions. Release-please handles it based on your commit messages.

### Q: What if I need to manually set a version?

**A:** Edit `.release-please-manifest.json` and commit the change. Release-please will use that as the base for the next version.

### Q: When should I merge release-please PRs?

**A:** When you're ready to publish a release. Merging the PR creates the release and triggers publishing.

### Q: How long will mc/1.21.11 be supported?

**A:** Maintenance timeline:
- Active development: 6-12 months (current phase)
- Maintenance mode: 12-24 months (after MC 26.1 releases)
- Archived: When player base moves to newer MC versions

### Q: Why beta releases on mc/26.1?

**A:** MC 26.1 is still in snapshot (unstable). Beta releases indicate the mod is tested but the underlying Minecraft version may have bugs or API changes.

## References

For implementation details, see:
- `.github/workflows/prepare-release-mc-1.21.11.yml` - Release automation
- `.github/workflows/build-release.yml` - Build and publish process
- `release-please-config.json` - Release-please configuration
- `.release-please-manifest.json` - Current version

---

**Key Takeaway:** Each branch is an independent project. Don't expect code sharing or synchronized releases. Focus development where your players are (currently mc/1.21.11).
