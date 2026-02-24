# Logistics Wiki

Welcome to the **Logistics** wiki! A modern Minecraft logistics and pipe mod with authentic in-pipe item motion, built on Fabric.

## Getting Started

New to Logistics? Start here:

- **[Installation](getting-started/install.md)** - Get the mod installed and running
- **[Your First Pipe Network](getting-started/first-network.md)** - Build a basic transport system
- **[Understanding Tiers](getting-started/understanding-tiers.md)** - Learn the progression system

## Core Concepts

Understand the fundamentals:

- **[Pipe Networks](core/pipe-networks.md)** - How pipes connect and form networks
- **[Tier System](core/tier-system.md)** - Mechanical → Smart → Network progression
- **[Item Transport](core/item-transport.md)** - How items move through pipes
- **[Connectivity](core/connectivity.md)** - Connection rules and behaviors
- **[Routing](core/routing.md)** - How items choose paths at junctions

## Pipes

The core of Logistics - transporting items through your network.

### Tier 1: Mechanical Pipes
- **[Stone Transport Pipe](pipes/stone-transport-pipe.md)** - Very slow transport
- **[Copper Transport Pipe](pipes/copper-transport-pipe.md)** - Standard transport backbone
- **[Item Extractor Pipe](pipes/item-extractor-pipe.md)** - Pull from inventories
- **[Item Merger Pipe](pipes/item-merger-pipe.md)** - Converge to single output
- **[Golden Transport Pipe](pipes/golden-transport-pipe.md)** - Speed boost with redstone
- **[Item Passthrough Pipe](pipes/item-passthrough-pipe.md)** - Bypass inventories
- **[Item Void Pipe](pipes/item-void-pipe.md)** - Delete items

### Tier 2: Smart Pipes
- **[Item Filter Pipe](pipes/item-filter-pipe.md)** - Route by item type
- **[Item Insertion Pipe](pipes/item-insertion-pipe.md)** - Prefer inventories with space

[Browse all pipes →](pipes/index.md)

## Power

Engines generate RF energy to power automation machines.

- **[RF Energy](power/rf-energy.md)** - Understanding the power system
- **[Redstone Engine](power/redstone-engine.md)** - Basic power from redstone
- **[Stirling Engine](power/stirling-engine.md)** - High power from fuel

[Browse all power →](power/index.md)

## Automation

Machines that automate tasks - some use RF power, others use fuel and temperature.

- **[Laser Quarry](automation/laser-quarry.md)** - Automated 16×16 mining (RF powered)
- **[Kiln](automation/kiln.md)** - Temperature-controlled crafting for valves

[Browse all automation →](automation/index.md)

## Tools

Configuration and network management tools.

- **[Wrench](tools/wrench.md)** - Configure pipes, engines, and machines
- **[Marking Fluid](tools/marking-fluid.md)** - Color-code copper pipes to segment networks

[Browse all tools →](tools/index.md)

## Materials

Gears, valves, ores, and crafting components.

**Gears (Engines & Machines):**
- **[Wooden Gear](materials/wooden-gear.md)** - Basic | **[Stone Gear](materials/stone-gear.md)** - Stone
- **[Iron Gear](materials/iron-gear.md)** | **[Copper Gear](materials/copper-gear.md)** | **[Tin Gear](materials/tin-gear.md)** | **[Bronze Gear](materials/bronze-gear.md)** - Mid tier
- **[Gold Gear](materials/gold-gear.md)** | **[Diamond Gear](materials/diamond-gear.md)** - Advanced
- **[Netherite Gear](materials/netherite-gear.md)** - End tier

**Valves ([Kiln](automation/kiln.md)-Crafted):**
- **Tier 1:** [Copper](materials/valve-copper.md) | [Tin](materials/valve-tin.md) (Coal fuel)
- **Tier 2:** [Iron](materials/valve-iron.md) | [Bronze](materials/valve-bronze.md) (Coal at limit/fails)
- **Tier 3:** [Gold](materials/valve-gold.md) | [Apatite](materials/valve-apatite.md) (Blaze rod/lava)
- **Tier 4-6:** [Diamond](materials/valve-diamond.md) | [Ender](materials/valve-ender.md) | [Emerald](materials/valve-emerald.md) | [Lapis](materials/valve-lapis.md) | [Obsidian](materials/valve-obsidian.md) | [Netherite](materials/valve-netherite.md) | [Blazing](materials/valve-blazing.md) (Lava required)

**Ores & Metals:**
- **[Tin Ore](materials/tin-ore.md)** → **[Tin Ingot](materials/tin-ingot.md)** - Common metal
- **[Bronze Ingot](materials/bronze-ingot.md)** - Copper + tin alloy
- **[Apatite Ore](materials/apatite-ore.md)** → **[Apatite](materials/apatite.md)** - Gem material

[Browse all materials →](materials/index.md)

## Quick Reference

### Common Tasks
- **Building a basic network:** [Your First Pipe Network](getting-started/first-network.md)
- **Sorting items:** [Item Filter Pipe](pipes/item-filter-pipe.md)
- **Powering machines:** [Stirling Engine](power/stirling-engine.md) + [RF Energy](power/rf-energy.md)
- **Automated mining:** [Laser Quarry](automation/laser-quarry.md)
- **Extracting items:** [Item Extractor Pipe](pipes/item-extractor-pipe.md)
- **Controlling flow:** [Item Merger Pipe](pipes/item-merger-pipe.md)
- **Crafting valves:** [Kiln](automation/kiln.md) + fuel + molten glass

### By Material
- **Stone:** [Stone Transport Pipe](pipes/stone-transport-pipe.md), [Stone Gear](materials/stone-gear.md)
- **Copper:** [Copper Transport Pipe](pipes/copper-transport-pipe.md), [Copper Gear](materials/copper-gear.md)
- **Wood:** [Item Extractor Pipe](pipes/item-extractor-pipe.md), [Wooden Gear](materials/wooden-gear.md)
- **Iron:** [Item Merger Pipe](pipes/item-merger-pipe.md), [Iron Gear](materials/iron-gear.md)
- **Gold:** [Golden Transport Pipe](pipes/golden-transport-pipe.md)
- **Diamond:** [Item Filter Pipe](pipes/item-filter-pipe.md), [Diamond Gear](materials/diamond-gear.md)
- **Quartz:** [Item Insertion Pipe](pipes/item-insertion-pipe.md)

## About Logistics

**Early Development:** Logistics is in active development. Core features (Tier 1-2) are implemented and functional. Tier 3 network logistics is planned for future updates.

**Mod Version:** 1.21.11
**Mod Loader:** Fabric 0.18.4+
**License:** MIT

[GitHub Repository](https://github.com/indemnity83/logistics) | [Report Issues](https://github.com/indemnity83/logistics/issues)
