<div align="center">

![Logistics](assets/art/logo.png)

# Logistics: Automation

**A modern Minecraft logistics and pipe mod with authentic in-pipe item motion**

[![GitHub](https://img.shields.io/badge/GitHub-indemnity83%2Flogistics-blue?logo=github)](https://github.com/indemnity83/logistics)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21-brightgreen.svg)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.16.7-orange.svg)](https://fabricmc.net/)

</div>

---

## ⚠️ Early Development

**Logistics is in active development.** Version 0.1.0 represents Phase 1 of the roadmap—core pipe transport works, but expect rough edges, missing features, and the occasional bug. Report issues on [GitHub](https://github.com/indemnity83/logistics/issues) if something breaks.

---

## About

Logistics is a Fabric mod inspired by BuildCraft and Logistics Pipes, bringing authentic item pipe systems to modern Minecraft. Items travel smoothly through thin pipes with visible motion, integrating seamlessly with other mods via Fabric's Transfer API.

**Design Principles:**
- **Authentic Visuals** - Items travel continuously through pipes with visible speed
- **Mod Interoperability** - Works with any mod using Fabric Transfer API (ItemStorage)
- **Modular Systems** - Item transport first, fluids and power systems planned for future phases
- **Classic Ergonomics** - Simple placement, visible connections, easy to understand

---

## Pipe Types

**Cobblestone Transport Pipe**
Cobblestone pipes are basic transport pipes that move items through your network. When an item reaches a junction with multiple valid outputs, it randomly selects one direction to continue.
*Recipe: cobblestone + glass + cobblestone → 8 pipes*

**Stone Transport Pipe**
Stone pipes are basic transport pipes that function identically to cobblestone pipes. In future versions, these may transport items faster.
*Recipe: stone + glass + stone → 8 pipes*

**Wooden Transport Pipe**
Wooden pipes are extraction pipes that actively pull items from adjacent inventories into the pipe network. The face to extract from is indicated by an opaque connector and can be changed by right-clicking with a wrench. Only one face can be active at a time. Wooden pipes extract one item per operation; future pipe types will allow for faster extraction.
*Recipe: planks + glass + planks → 8 pipes*

**Gold Transport Pipe**
Gold pipes are acceleration pipes that speed up items traveling through them when powered by redstone. Items passing through a powered gold pipe move faster than normal pipe transit speed.
*Recipe: gold ingot + glass + gold ingot → 8 pipes*

**Iron Transport Pipe**
Iron pipes are merger pipes that take items from any input direction and route them all to a single common output direction, indicated by an opaque connector. The output direction can be changed by right-clicking with a wrench. Items cannot enter through the output face—if you try to route items back in through the output, they will be rejected and fall out of the pipe network.
*Recipe: iron ingot + glass + iron ingot → 8 pipes*

**Copper Transport Pipe**
Copper pipes are distribution pipes that evenly split items across multiple outputs using round-robin routing. When an item reaches the pipe, it sends the item to the next output in rotation, ensuring balanced distribution.
*Recipe: copper ingot + glass + copper ingot → 8 pipes*

**Diamond Transport Pipe**
Diamond pipes are filtering pipes that route items based on configurable per-side filters. Right-click the pipe to open a GUI where you can set which item types should be routed to which connected face. Filtering is basic item type matching, ignoring NBT data. Items that don't match any filter are routed to unfiltered outputs. If an item matches no filters and there are no unfiltered directions available, the item will be rejected and fall out of the pipe network.
*Recipe: diamond + glass + diamond → 8 pipes*

**Quartz Transport Pipe**
Quartz pipes are sensor pipes that emit a redstone comparator signal based on how many items are currently traveling through the pipe. The signal strength scales with the number of items relative to the pipe's capacity.
*Recipe: quartz + glass + quartz → 8 pipes*

**Void Transport Pipe**
Void pipes are deletion pipes that destroy any items that enter them. Items are deleted when they reach the center of the pipe, making them useful for overflow management or disposing of unwanted items.
*Recipe: obsidian + glass + ender pearl → 8 pipes*

**Wrench**
The wrench is a tool used to configure pipe behavior. Right-click on pipes to cycle output directions on iron pipes, select extraction faces on wooden pipes, and interact with other configurable pipe types.
*Recipe: 4 iron ingots in wrench shape → 1 wrench*

---

## Installation

### Requirements
- Minecraft 1.21
- Fabric Loader 0.16.7+
- Fabric API
- Java 21+

### Steps
1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21
2. Download [Fabric API](https://modrinth.com/mod/fabric-api)
3. Download Logistics from:
   - [GitHub Releases](https://github.com/indemnity83/logistics/releases) (includes dev builds)
   - [Modrinth](https://modrinth.com/mod/logistics) (stable releases)
   - [CurseForge](https://www.curseforge.com/minecraft/mc-mods/logistics) (stable releases)
4. Place the Fabric API and Logistics `.jar` files in your `.minecraft/mods` folder
5. Launch Minecraft with the Fabric profile

---

## Getting Started

1. **Craft Pipes** — Start with cobblestone or stone pipes for basic transport
2. **Connect to Inventories** — Pipes automatically connect to adjacent chests and other inventories
3. **Extract Items** — Use wooden pipes to pull items from inventories (wrench to select face)
4. **Control Flow** — Iron pipes for directional routing, copper pipes for even distribution
5. **Filter Items** — Diamond pipes route specific items to specific destinations
6. **Speed Things Up** — Gold pipes accelerate items when powered by redstone

**Tips:**
- Items drop if they can't find a valid destination
- Void pipes delete overflow items
- Quartz pipes output comparator signals based on item count
- The wrench is your friend

---

## Roadmap

### ✅ Phase 1: Pipes + Items (v0.1.0 - Complete!)
- Thin pipe blocks with 6-way connections
- Server-side traveling item simulation with continuous progress
- Client-side smooth visual rendering
- Extraction from and insertion into adjacent inventories
- Routing with filters, priorities, and flow control

### 🚧 Phase 2: Logistics Basics (Next)
- Request Table block with GUI
- Provider pipes that expose inventory contents
- Request flow: request → routing → provider → dispatch
- Autocrafting support via vanilla Crafter integration

### 🔮 Future Phases
- Fluid pipes with Transfer API integration
- Power/cost system for logistics operations
- Additional pipe types, upgrades, and advanced logistics features

See [`docs/DESIGN.md`](docs/DESIGN.md) for architecture details and technical design.

---

## Contributing

Contributions welcome! When reporting bugs:
- Use [GitHub Issues](https://github.com/indemnity83/logistics/issues)
- Include version info (Minecraft, Fabric Loader, mod version)
- Provide steps to reproduce
- Check existing issues first

For code contributions, see the development docs in `docs/`.

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Acknowledgments

Inspired by:
- **BuildCraft** — Classic pipe mechanics and visual style
- **Logistics Pipes** — Request/provider logistics system design
- The Fabric community for excellent modding tools and APIs

---

<div align="center">

[Report an Issue](https://github.com/indemnity83/logistics/issues) • [Technical Design](docs/DESIGN.md)

</div>
