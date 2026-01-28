# Branch Management

This document describes how we manage branches for supporting multiple Minecraft versions.

## Branch Overview

| Branch | Purpose | Minecraft Version | Releases |
|--------|---------|-------------------|----------|
| `main` | Stable development | Current stable MC | Stable releases via release-please |
| `next` | Snapshot development | Upcoming MC snapshots | Snapshot releases (alpha) |

## The `next` Branch

The `next` branch contains a working copy of the mod for the next major Minecraft version (currently MC 26.1 snapshots). This branch:

- Targets Minecraft snapshot/pre-release versions
- Uses Java 25 (required for MC 26.1+)
- Uses official Mojang mappings (MC 26.1+ is unobfuscated)
- Automatically publishes snapshot builds to Modrinth as alpha releases

### Key Differences from `main`

1. **No yarn mappings** - MC 26.1+ is unobfuscated, so we use `net.fabricmc.fabric-loom` (no remapping) instead of `net.fabricmc.fabric-loom-remap`
2. **Java 25** - Required by MC 26.1+
3. **Mojang class names** - Code uses official Minecraft class/method names instead of Yarn mappings
4. **Different Fabric API** - Some APIs have been renamed (e.g., `ServerWorldEvents` → `ServerLevelEvents`)

## Workflow

### Daily Development

**For stable Minecraft features:**
1. Develop on `main` branch
2. Create PRs against `main`
3. Releases are managed by release-please

**For snapshot Minecraft features:**
1. Develop on `next` branch
2. Create PRs against `next`
3. Pushes to `next` automatically publish snapshot builds

### Cherry-picking Changes

When a change on `main` should also go to `next`:

```bash
# On the next branch
git checkout next
git cherry-pick <commit-hash>

# Resolve any mapping conflicts (Yarn → Mojang names)
# Test the build
./gradlew build -x checkstyleClient -x checkstyleMain -x spotlessCheck

git push
```

Common mapping translations needed:
- `getWorld()` → `getLevel()`
- `getPos()` → `getBlockPos()`
- `getCachedState()` → `getBlockState()`
- `ServerWorldEvents` → `ServerLevelEvents`
- `ScreenHandlerType` → `MenuType`
- `Identifier` → `ResourceLocation` (in some contexts)

### When a Minecraft Version Becomes Stable

When the Minecraft version on `next` becomes stable (e.g., 26.1 releases):

1. **Prepare `next` for merge:**
   ```bash
   git checkout next
   # Update gradle.properties with stable MC version
   # Update fabric.mod.json version constraints
   # Update README.md version badges
   git commit -m "chore: prepare for stable release"
   ```

2. **Merge `next` into `main`:**
   ```bash
   git checkout main
   git merge next --no-ff -m "feat: update to Minecraft 26.1"
   git push
   ```

3. **Let release-please handle the release:**
   - Release-please will create a PR with version bump
   - Merge the release PR to trigger the stable release

4. **Create new `next` branch for the next MC version:**
   ```bash
   git checkout main
   git checkout -b next
   # Update to next snapshot version when available
   git push -u origin next
   ```

### Handling Breaking Changes

If the new Minecraft version has significant breaking changes:

1. Consider keeping `main` on the old MC version for patch releases
2. Create a maintenance branch (e.g., `1.21.x`) if long-term support is needed
3. Update `next` → `main` only when ready to drop old MC support

## CI/CD Behavior

### `main` Branch
- PRs trigger build validation
- Merges with release-please commits trigger stable releases
- Publishes to Modrinth, CurseForge, and GitHub Releases

### `next` Branch
- PRs trigger build validation (linting disabled due to Java 25 incompatibility)
- Every push triggers a snapshot build
- Publishes to Modrinth as alpha with version `X.Y.Z-snapshot.<sha>`

## Local Development

### Switching Between Branches

When switching from `main` to `next` (or vice versa), update your global Gradle config:

```bash
# For next branch (Java 25)
# In ~/.gradle/gradle.properties:
org.gradle.java.home=/path/to/jdk-25

# For main branch (Java 21)
# In ~/.gradle/gradle.properties:
org.gradle.java.home=/path/to/jdk-21
```

On macOS, find paths with:
```bash
/usr/libexec/java_home -v 25
/usr/libexec/java_home -v 21
```

### Running the Client

Mods built for obfuscated Minecraft (1.21.11 and earlier) will **not** work with unobfuscated Minecraft (26.1+). Remove any incompatible mods from `run/mods/` before launching.

## Version Numbering

- **Stable releases** (from `main`): `0.2.3`, `0.3.0`, etc.
- **Snapshot builds** (from `next`): `0.3.0-snapshot.abc1234`

The snapshot version uses the next planned version from `version.txt` with a `-snapshot.<sha>` suffix.