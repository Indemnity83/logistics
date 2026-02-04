# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Branch Strategy (IMPORTANT!)

**You are on: `mc/1.21.11` - Stable Minecraft 1.21.11 branch**

This repository uses a multi-version strategy to support different Minecraft releases:
- **`mc/1.21.11`** (this branch) - Stable releases for MC 1.21.11, maintenance focus
- **`main`** - Snapshot development for MC 26.1+, active feature development

### Critical Understanding

**MC 26.1 introduced massive API changes.** Almost every Minecraft method has changed. This means:
- ❌ **Cannot cherry-pick** commits between branches
- ❌ **Cannot share code** between versions
- ❌ **Cannot expect feature parity** across versions
- ✅ **Can re-implement** fixes/features manually on other branch

**Porting = manual re-implementation, not git operations.**

### Development Strategy (Phased)

**Current Phase (Pre-1.0):**
- **This branch (mc/1.21.11)**: Primary development target (where players are!)
- **`main` branch**: Port features occasionally to stay current with MC 26.1

**Future Phase (After MC 26.1 releases):**
- **`main` branch**: Primary development target (latest MC)
- **`mc/26.1` branch**: Stable MC 26.1 releases
- **This branch (mc/1.21.11)**: Critical bug fixes only, eventual EOL

**See `BRANCH_DIVERGENCE_REALITY.md` for full details.**

### Working on This Branch

**Targeting fixes/features for mc/1.21.11:**
- This is the **stable, production branch**
- Focus on **bug fixes** and **essential features**
- Keep changes **minimal and tested**
- Avoid refactoring unless necessary

**Cross-version bugs:**
1. Fix on this branch first (where reported)
2. Check if bug exists on `main` branch
3. **Re-implement** the fix for `main`'s API (don't cherry-pick)
4. Test independently on both branches

**New features:**
- Develop here during Pre-1.0 phase
- Port to `main` occasionally (manual re-implementation)
- After MC 26.1 releases, focus shifts to `main` branch

## Build Commands

```bash
./gradlew build              # Build the mod JAR
./gradlew remapJar           # Build with obfuscation remapping (MC 1.21.11 is obfuscated)
./gradlew runClient          # Launch Minecraft client for testing
./gradlew runServer          # Launch Minecraft server
./gradlew check              # Run checkstyle checks
```

**Requirements:**
- **Java 21** (MC 1.21.11 requirement)
- Minecraft 1.21.11
- Fabric API

**Build output:** `build/libs/logistics-{version}.jar`
- Local: `logistics-dev-local.jar`
- CI: `logistics-0.4.0+mc1.21.11.fabric.jar` (SemVer build metadata format)

### Version Management

This branch uses **release-please** for automated versioning with **SemVer build metadata**:
- Push conventional commits: `feat:`, `fix:`, etc.
- Release-please creates PR with version bump and changelog
- Merge PR → automatic GitHub release
- Tags: `mc1.21.11-v0.4.0`
- Version format: `0.4.0+mc1.21.11.fabric` (SemVer 2.0.0 build metadata)
  - The `0.4.0` is the semantic version
  - The `+mc1.21.11.fabric` is build metadata (platform and loader identifiers)

**Do NOT manually edit version numbers.** Let release-please manage it.

## Architecture

This is a Fabric mod organized into **independent domains** for maintainability and potential future modular packaging.

### Domain Structure

```
src/main/java/com/logistics/
├── LogisticsMod.java        # Entry point, initializes all domains
├── core/                    # Shared interfaces and utilities
│   ├── bootstrap/           # Domain initialization system
│   └── lib/                 # Interfaces that other domains may import
├── pipe/                    # Item transport pipes
├── power/                   # Energy generation (engines)
└── automation/              # Machines (quarry, etc.)

src/client/java/com/logistics/
├── LogisticsModClient.java  # Client entry point
├── core/                    # Client-side core utilities
├── pipe/                    # Pipe rendering
└── power/                   # Engine rendering
```

### Domain Isolation Rules

- **Domains must not import from each other** (no `pipe` → `power` imports)
- **Exception:** All domains may import from `core.lib` for shared interfaces
- This keeps domains decoupled and allows splitting into separate JARs later

### Bootstrap System

Each domain implements `DomainBootstrap` for initialization. The main mod class discovers and initializes all domains via service loader.

### Domain Details

See `docs/DESIGN.md` for full architectural vision. Key patterns per domain:

**Pipe Domain:** Module composition system where `Pipe` composes `Module` instances. Modules control routing, acceptance, and speed. `TravelingItem` represents items in transit with progress-based movement.

**Power Domain:** Engine hierarchy with `AbstractEngineBlockEntity` base class. Engines have heat stages (COLD → COOL → WARM → HOT → OVERHEAT) and integrate with Team Reborn Energy API.

## Code Style

- **Formatting:** Manual - prioritize minimal diffs across branches over strict style
- **Linting:** Checkstyle enforces naming, imports, nesting depth (max 3)
- **Single-line if/for allowed** but braces preferred for multi-line
- Use `@SuppressWarnings` to suppress checkstyle only when needed

**Note:** Spotless was removed to maintain minimal diffs between mc/1.21.11 and mc/26.1 branches. This makes cross-version maintenance and comparison easier.

## Commit Messages

Output a SINGLE-LINE commit subject only:
- No conventional-commit prefix (no "feat:", "fix:", etc.)
- No scope, no body, no co-author trailer
- Imperative mood ("Add", "Fix", "Refactor")
- Aim for <= 72 characters
- Be specific about what changed

## Pull Requests

Use conventional commit format for PR title (no scope):
```
<type>: <description>
```

Valid types: `feat`, `fix`, `refactor`, `perf`, `test`, `docs`, `build`, `ci`, `chore`, `revert`

PR body should read like release notes:
- Focus on WHAT changed and WHY it matters
- Use short sections: Summary / Changes / Notes
- Bullet points, grouped and scannable
- No low-level implementation details unless they affect behavior or compatibility
- Add `BREAKING CHANGE: ...` footer only if applicable

## Tools

Python 3 with PIL is available for graphics/texture development:
```bash
scripts/venv/bin/python
```

## Documentation

**Multi-Version Strategy:**
- `BRANCH_DIVERGENCE_REALITY.md` - Why branches diverge, development strategy
- `VERSION_STRATEGY.md` - Version management approach
- `IMPORTANT_JAVA_VERSIONS.md` - Java 21 vs Java 25 requirements
- `MC26_GRADLE_CHANGES.md` - Why MC 26.1 doesn't need remapJar

**Architecture:**
- `docs/DESIGN.md` - Technical architecture and vision (read this first for deep understanding)
- `docs/PIPE_TYPES.md` - Detailed pipe behavior specifications
