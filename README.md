<div align="center">

![Logistics](assets/art/logo.png)

# Logistics: Automation

**A modern Minecraft logistics and pipe mod with authentic in-pipe item motion**

[![GitHub](https://img.shields.io/badge/GitHub-indemnity83%2Flogistics-blue?logo=github)](https://github.com/indemnity83/logistics)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![codecov](https://codecov.io/gh/Indemnity83/logistics/branch/mc%2F26.1/graph/badge.svg)](https://codecov.io/gh/Indemnity83/logistics)
[![Minecraft](https://img.shields.io/badge/Minecraft-26.1-brightgreen.svg)](https://www.minecraft.net/)
[![Loaders](https://img.shields.io/badge/Loaders-Fabric%20%26%20NeoForge-orange.svg)](https://fabricmc.net/)
[![Discord](https://img.shields.io/badge/Discord-Join-5865F2?logo=discord&logoColor=white)](https://discord.gg/94DP3CVNVt)

</div>


## ⚠️ Early Development

**Logistics is in active development.** Core systems work, but expect rough edges, missing features, and the occasional bug. Report issues on [GitHub](https://github.com/indemnity83/logistics/issues), or [join the Discord](https://discord.gg/94DP3CVNVt) for help and discussion.

While in pre-release, save compatibility reaches back one major version (the second version digit), so load your world in the **latest release of each major in turn** before jumping ahead. See the [release notes](https://github.com/indemnity83/logistics/releases) whenever you update.


## About

Logistics is a multiloader (Fabric & NeoForge) mod inspired by BuildCraft and Logistics Pipes, bringing authentic item pipe systems to modern Minecraft. Items travel smoothly through thin pipes with visible motion, integrating with other mods' item, fluid, and energy storage on both loaders.

**What's inside:**

- **Pipes** — item transport, extraction, filtering, and full network logistics (providers, requesters, suppliers, crafting, chassis + modules)
- **Fluids** — a standalone fluid pipe/tank/pump system, separate from the item network
- **Power** — RF engines (redstone, stirling, creative) and power cables
- **Automation** — machines like the macerator, kiln, laser quarry, refinery, and sequential fabricator

The full, always-current catalog of blocks, items, machines, and mechanics lives in the wiki — this README stays focused on the repository itself.

📖 **[Read the documentation →](https://indemnity83.github.io/logistics/)**


## Install (players)

**Requirements:** Minecraft 26.1 • Fabric (with Fabric API) or NeoForge • Java 25+

The mod bundles its libraries — Team Reborn Energy, [Configory](https://modrinth.com/mod/configory), and (when crash reporting is enabled) the Sentry SDK — inside the jar, so there's nothing extra to install beyond the loader and Fabric API.

Download from:
- [Modrinth](https://modrinth.com/mod/logistics) — stable releases
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/logistics-automation) — stable releases
- [GitHub Releases](https://github.com/indemnity83/logistics/releases) — includes dev/RC builds

[Full installation guide →](https://indemnity83.github.io/logistics/getting-started/install/)


## Building from source

**Requirements:** JDK 25 and Git. The Gradle wrapper (`./gradlew`) handles everything else.

```bash
git clone https://github.com/indemnity83/logistics.git
cd logistics
./gradlew build            # compile both loaders + run unit tests and gametests
```

Built jars land in `fabric/build/libs/` and `neoforge/build/libs/`.

**Run in a dev environment:**

```bash
./gradlew :fabric:runClient      # or :neoforge:runClient
./gradlew :fabric:runServer      # or :neoforge:runServer
```

**Test & format:**

```bash
./gradlew test                   # unit tests
./gradlew :fabric:runClientGameTest   # in-game tests (Fabric)
./gradlew spotlessApply          # auto-format; spotlessCheck verifies in CI
./gradlew installGitHooks        # one-time: pre-commit formatting hook
```

### Project layout

Multiloader split — shared code plus thin loader adapters:

```
common/     # loader-agnostic server/common + client code (no Fabric/NeoForge imports)
fabric/     # Fabric adapter and client wiring
neoforge/   # NeoForge adapter and client wiring
```

Common code is organized into decoupled **domains** (`pipe`, `power`, `automation`, `core`) that depend only on abstractions in `core.lib`. See [`CLAUDE.md`](CLAUDE.md) for the full architecture, multiloader rules, and bootstrap flow. To diagnose performance, see [`PROFILING.md`](PROFILING.md).

### Branches

The repo supports several Minecraft versions in parallel. New work starts on the main branch and is cherry-picked to the maintenance branches.

| Branch | Role |
|---|---|
| `mc/26.2` | Main / default — active development |
| `mc/26.1`, `mc/1.21.11`, `mc/1.21.1` | Maintenance / backport targets |

Details and the cross-version workflow are in [`CLAUDE.md`](CLAUDE.md).


## Contributing

Contributions welcome! Report issues on [GitHub Issues](https://github.com/indemnity83/logistics/issues); for code, read [`CLAUDE.md`](CLAUDE.md) first.

Pull request **titles** use Conventional Commit format — release notes are generated from them, so prefer wording that describes the player-visible impact:

```text
type(scope): short description
```

Player-facing types appear in the changelog: `feat` (Added), `fix` (Fixed), `balance`/`change`/`perf` (Changed), `remove` (Removed). Internal types are hidden: `refactor`, `test`, `build`, `ci`, `chore`, `docs`. The full type/scope tables live in [`CLAUDE.md`](CLAUDE.md).


## Crash reporting & privacy

Logistics can optionally send **sanitized** crash diagnostics to help fix bugs. It is **off by default** and opt-in per install via `/logistics diagnostics enable`. It never intentionally sends player names, UUIDs, IPs, server addresses, chat, or world data. See [CRASH_REPORTING.md](CRASH_REPORTING.md) for exactly what is and isn't collected.


## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

Some textures are licensed under CC BY 4.0 or CC BY-NC-SA 4.0 — see [CREDITS.md](CREDITS.md) for attribution details.


## Acknowledgments

Inspired by:
- **BuildCraft** — Classic pipe mechanics and visual style
- **Logistics Pipes** — Request/provider logistics system design
- **Forestry** — machines and progressive automation
- The Fabric and NeoForge communities for excellent modding tools and APIs

**Textures:**
- Some textures used, adapted, or inspired from [Unused Textures](https://github.com/malcolmriley/unused-textures) by Malcolm Riley, licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/)
- Some textures used, adapted, or inspired from [TextureRepository](https://github.com/Futureazoo/TextureRepository) by Futureazoo, licensed under [CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/)


<div align="center">

[Report an Issue](https://github.com/indemnity83/logistics/issues) • [Documentation](https://indemnity83.github.io/logistics/) • [Discord](https://discord.gg/94DP3CVNVt)

</div>
