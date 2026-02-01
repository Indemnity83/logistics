# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew build              # Build the mod JAR
./gradlew runClient          # Launch Minecraft client for testing
./gradlew runServer          # Launch Minecraft server
./gradlew spotlessApply      # Format code (Palantir formatter)
./gradlew check              # Run checkstyle + spotless checks
```

**Requirements:** Java 21+, Minecraft 1.21.11, Fabric API

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

- **Formatting:** Spotless with Palantir formatter (auto-applied on build)
- **Linting:** Checkstyle enforces naming, imports, nesting depth (max 3)
- **Single-line if/for allowed** but braces preferred for multi-line
- Use `@SuppressWarnings` to suppress checkstyle only when needed

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

- `docs/DESIGN.md` - Technical architecture and vision (read this first for deep understanding)
- `docs/PIPE_TYPES.md` - Detailed pipe behavior specifications
