<div align="center">

![Logistics](assets/art/logo.png)

# Logistics: Automation

**A modern Minecraft logistics and pipe mod with authentic in-pipe item motion**

[![GitHub](https://img.shields.io/badge/GitHub-indemnity83%2Flogistics-blue?logo=github)](https://github.com/indemnity83/logistics)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen.svg)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.18.4-orange.svg)](https://fabricmc.net/)

</div>

---

## ⚠️ Early Development

**Logistics is in active development.** Core pipe transport works, but expect rough edges, missing features, and the occasional bug. Report issues on [GitHub](https://github.com/indemnity83/logistics/issues) if something breaks.

---

## About

Logistics is a Fabric mod inspired by BuildCraft and Logistics Pipes, bringing authentic item pipe systems to modern Minecraft. Items travel smoothly through thin pipes with visible motion, integrating seamlessly with other mods via Fabric's Transfer API.

**Design Principles:**
- **Material-Based Identity** - Each pipe uses distinct vanilla materials for visual clarity
- **Layered Progression** - Three tiers: Mechanical pipes (basic operations), Smart pipes (decisions), Network logistics (abstract services)
- **Authentic Visuals** - Items travel continuously through pipes with visible speed
- **Mod Interoperability** - Works with any mod using Fabric Transfer API (ItemStorage)
- **Classic Ergonomics** - Simple placement, visible connections, easy to understand

---

## How It Works

Logistics is built on a **three-tier system** that grows with your world progression. Implemented tiers are listed first, followed by future plans.

### Tier 1: Mechanical Pipes (Implemented)
**Basic routing without item awareness**

Start here. These pipes perform mechanical operations—moving, merging, extracting, deleting—but they don't look at what's flowing through them. They just do their job, every time, regardless of item type.

- **Stone Transport Pipe (Stone)** - Very slow backbone connectivity with random routing
- **Copper Transport Pipe (Copper)** - Backbone connectivity with random routing
- **Item Extractor Pipe (Wood)** - Pull items from adjacent inventories into your network
- **Item Merger Pipe (Iron)** - All inputs converge to a single output
- **Golden Transport Pipe (Gold)** - Speed boost when powered by redstone
- **Item Passthrough Pipe (Sandstone)** - Connects only to pipes; bypasses inventories
- **Item Void Pipe (Obsidian)** - Delete unwanted items

### Tier 2: Smart Pipes (Implemented)
**Item-aware routing decisions**

These pipes are intelligent. They inspect items and change behavior based on what they see. This is where your network becomes conditional and responsive.

- **Item Filter Pipe (Diamond)** - Route specific items to specific destinations (item-aware)
- **Item Insertion Pipe (Quartz)** - Prefer inventories with space; otherwise route to pipes

### Tier 3: Network Logistics (Future)
**System-aware automation and requests**

The ultimate goal. Your inventories become abstract resources, and you request what you need—the network figures out the rest.

- **Request Tables** - Ask for items; the network delivers them
- **Provider Modules** - Advertise what inventories contain
- **Crafting Logistics** - Automated crafting on demand
- **Global Routing** - Smart pathfinding across your entire network

Each tier builds on the previous one—you'll use all three together as your base grows.

---

## Features

Logistics includes a complete system for item transport, power generation, and automation.

### Pipes
Transport items through networks with different behaviors:
- **Basic Transport** - Stone and Copper pipes for backbone connectivity
- **Extraction & Routing** - Wood (extractor), Iron (merger), Diamond (filter), Quartz (insertion) pipes
- **Special Pipes** - Gold (speed boost), Sandstone (passthrough), Obsidian (void)

[View all pipes →](https://indemnity83.github.io/logistics/pipes/)

### Power
RF energy generation with engines:
- **Redstone Engine** - Simple, safe, steady power
- **Stirling Engine** - Fuel-powered with heat management

[Learn about power systems →](https://indemnity83.github.io/logistics/power/)

### Automation
- **[Kiln](https://indemnity83.github.io/logistics/automation/kiln/)** - Temperature-controlled crafting for molten glass and advanced materials
- **[Laser Quarry](https://indemnity83.github.io/logistics/automation/laser-quarry/)** - Automated 16×16 mining with energy-scaled speed

### Tools
- **[Wrench](https://indemnity83.github.io/logistics/tools/wrench/)** - Configuration tool for pipes and machines
- **[Marking Fluid](https://indemnity83.github.io/logistics/tools/marking-fluid/)** - Color-code your pipe networks

---

## Installation

**Requirements:** Minecraft 1.21.11 • Fabric Loader 0.18.4+ • Fabric API • Java 21+

**Download from:**
- [GitHub Releases](https://github.com/indemnity83/logistics/releases) (includes dev builds)
- [Modrinth](https://modrinth.com/mod/logistics) (stable releases)
- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/logistics-automation) (stable releases)

[Full installation guide →](https://indemnity83.github.io/logistics/getting-started/install/)

---

## Quick Start

1. **Craft pipes** - Start with stone or copper transport pipes
2. **Connect to inventories** - Pipes automatically connect to chests and other storage
3. **Extract items** - Use wood extractor pipes (wrench to configure)
4. **Route and filter** - Combine different pipe types to build your network

[Build your first pipe network →](https://indemnity83.github.io/logistics/getting-started/first-network/)

---

## Status

### ✅ Implemented
- Thin pipe blocks with 6-way connections
- Server-side traveling item simulation with continuous progress
- Client-side smooth visual rendering
- Extraction from and insertion into adjacent inventories
- Mechanical and Smart pipe behaviors
- Redstone and Stirling engines with heat management
- Kiln for temperature-controlled crafting
- Laser Quarry with automatic frame construction and energy-scaled mining speed

### 🚧 Future
- Request Table block with GUI
- Provider pipes that expose inventory contents
- Global pathfinding and request fulfillment
- Autocrafting support via vanilla Crafter integration
- Network logistics components
- Fluid pipes with Transfer API integration
- Power/cost system for logistics operations
- Additional pipe upgrades and advanced logistics features

See the [documentation](https://indemnity83.github.io/logistics/) for detailed information on pipes, power, automation, and more.

---

## Development

### Testing

**124 tests** (73 unit + 51 game tests) ensure code quality and prevent regression bugs.

**Run unit tests:**
```bash
./gradlew test
```

**Run game tests** (full Minecraft server environment):
```bash
./gradlew runGameTest
```

**Run specific test class:**
```bash
./gradlew test --tests "com.logistics.power.engine.PIDControllerTest"
```

**Test reports:** After running tests, view detailed results at `build/reports/tests/test/index.html`

**CI Integration:** All tests run automatically on pull requests. Tests must pass before merge.

**Test Coverage:**
- **Unit tests (73):** PID control, item physics, routing logic, cross-version compatibility
- **Game tests (51):**
  - **Core domain (9 tests):**
    - Ore generation feature registration (5 tests) - ✅ Tin and apatite worldgen configured
    - Ore block placement and replacement (4 tests)
  - **Pipe domain (15 tests):**
    - Pipe placement and connections (4 tests)
    - Module placement (7 tests) and routing behavior (4 tests)
  - **Power domain (12 tests):**
    - Engine placement (3 tests) and configuration (2 tests)
    - **Stirling engine inventory access** (4 tests) - ✅ Verified NOT accessible from front face
    - Stirling engine fuel acceptance/rejection (2 tests)
    - Creative sink unlimited drain rate (1 test)
  - **Automation domain (15 tests):**
    - Laser quarry placement, energy acceptance, and item handling (6 tests)
    - Kiln placement, inventory access, and initial state (9 tests)

See `CLAUDE.md` for comprehensive development guidance including testing strategy.

---

## Contributing

Contributions welcome! Report issues on [GitHub Issues](https://github.com/indemnity83/logistics/issues). For code contributions, see `CLAUDE.md` for development guidance.

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

Some textures are licensed under CC BY 4.0 - see [CREDITS.md](CREDITS.md) for attribution details.

---

## Acknowledgments

Inspired by:
- **BuildCraft** — Classic pipe mechanics and visual style
- **Logistics Pipes** — Request/provider logistics system design
- **Forestry** — machines and progressive automation
- The Fabric community for excellent modding tools and APIs

**Textures:**
- Some textures adapted from [Unused Textures](https://github.com/malcolmriley/unused-textures) by Malcolm Riley, licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/)

---

<div align="center">

[Report an Issue](https://github.com/indemnity83/logistics/issues) • [Documentation](https://indemnity83.github.io/logistics/)

</div>
