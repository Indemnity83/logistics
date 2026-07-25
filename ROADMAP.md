    # Logistics Roadmap

Logistics is a modern classic-tech mod inspired by the feel of BuildCraft, Logistics Pipes, Thermal Expansion, Forestry, and Railcraft-era automation.

This roadmap describes the project’s current direction. It is not a promise of exact dates. Priorities may change based on Minecraft updates, loader support, technical risk, and community feedback.

The goal is not to clone every old mod exactly. The goal is to capture the feel of classic automation in a modern Minecraft mod.

## Current Focus

### Phase 1: Automation Core

The first major goal is to make Logistics feel like a complete early-to-midgame tech mod.

This phase focuses on the core automation loop:

- Item logistics network polish
- Fluid pipes and tanks
- Combustion-era power generation
- Core processing machines
- Material progression
- Loader parity for Fabric and NeoForge

## Recently shipped

The automation core has come a long way. Since the fluid foundation landed, most of Phase 1 has shipped:

- **Machines** — Alloy Smelter, Sawmill, Crucible, Refinery, and the Sequential Fabricator, plus macerator byproduct outputs.
- **Power** — the Fuel (combustion), Steam, Magmatic, and Reaction engines join the Redstone/Stirling line; the Power Junction powers logistics networks; cables/batteries no longer power pipes directly.
- **Fuel chain** — crude oil worldgen and the oil → fuel refining chain; biomass/biofuel fluids seeded.
- **Materials** — the Bronze alloy line, chipsets, the reworked valve lineup, rubber, and an expanded dust/byproduct chain.

## Now

Work currently active or near-term — the remaining run at the 1.0 bar (Phase 1 complete, on both loaders, polished).

| Area                 | Status  | Notes                                                                     |
|----------------------|---------|---------------------------------------------------------------------------|
| Tiered batteries     | Planned | Copper/Gold/Ender energy-storage line with configurable I/O               |
| Fluid logistics      | Planned | Liquid provider/supplier/request over the network (via a fluid↔item step) |
| Loader parity        | Ongoing | Keep Fabric and NeoForge aligned; hold both to the 1.0 bar                |

## Next

Likely after the current automation core is stable.

| Area                      | Status    | Notes                                                                                                               |
|---------------------------|-----------|---------------------------------------------------------------------------------------------------------------------|
| Crafting logistics        | Planned   | A Logistics crafting table or related crafting-request system may become the next major logistics-network expansion |
| Firewall Pipe             | Deferred  | No solid use case yet — parked until network segmentation earns its place; also needs routing/boundary design work   |
| Pipe based power delivery | Exploring | The Power Junction is a first step; the broader goal is one connection that feeds a machine both items and power     |

## Later

Important to the long-term vision, but not part of the immediate core milestone.

*These are in no specific order.*

| Area                  | Status        | Notes                                                                                                                 |
|-----------------------|---------------|-----------------------------------------------------------------------------------------------------------------------|
| Facades / pipe hiding | Planned later | Cosmetic integration for hiding pipes cleanly in builds                                                               |
| Forestry-style farms  | Planned later | Single-block farms plus the processing/biofuel chain, modernized for current Minecraft (not the multiblock multifarm) |
| Rail transport        | Planned later | Tracks, carts, routing, and train logistics                                                                           |

## Exploring / RFC

These ideas are under discussion and may change significantly.

- Programmable pipe gates / pipe signaling
- Machine upgrades
- Remote Orderer (handheld network access; really wants an Ender Chest-style companion to shine)
- Railcraft-style multiblock steam boilers/engines
- Optional split of Logistics domains into independent mods (core, automation, forestry, transport)

## Not Currently Planned

These are intentionally not part of the current direction.

- Full BuildCraft builder/filler clone
- Obsidian / vacuum pickup pipe (vanilla hoppers fill this role now)
- Full classic Forestry bee, tree or butterfly genetics
- Full Thermal Expansion item/fluid/storage replacement
- Railcraft style multiblock fluid tanks
- Forestry style multiblock farms
- Deep magic-style teleport logistics (tesseracts)
- Exact one-to-one ports of legacy mods
- Chunk loading

## How to Give Feedback

Feedback is welcome, especially around whether a feature fits the classic-tech feel.

Good feedback:

- “This feature feels essential because…”
- “This old mechanic was fun because…”
- “This part was tedious and should be modernized because…”
- “This would help modpacks because…”

Please keep in mind that Logistics is not trying to recreate every old system exactly as it was. Some features will be ported closely, some will be modernized, and some will be skipped.
