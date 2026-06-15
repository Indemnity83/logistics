# Vision

## The goal

Recreate the gameplay of a **classic 1.7.10-era tech modpack** in a modern Minecraft client — with a single coherent mod (or small family of mods) that *feels* classic rather than bolting old content onto new versions.

Think of it as a **parallel to Create, but classic**: where Create leans into kinetic contraptions and a bespoke aesthetic, Logistics leans into the BuildCraft/Thermal/Forestry lineage — pipes you can see items move through, engines that overheat, RF machines, ore doubling, automated farms, and rails that tie a base together.

> *Personal note: this grew out of a 1.7.10 pack I used to run. The target is the genre that pack represented, not any one pack's exact contents.*

## Source material

That era's tech identity came from a handful of mods, **all now abandoned**. Their gameplay is the target:

| Source mod | What it contributed | Status in Logistics |
|---|---|---|
| **BuildCraft** | Pipes, engines, the quarry, gates/automation | Largely covered (pipes, engines, laser quarry) |
| **Logistics Pipes** | Request/provider/supplier network logistics — *the* glue that tied a base together | **Done** (3-tier pipe network, chassis + modules) |
| **Thermal Expansion** | RF machines, ore processing (pulverizer/dusts), energy cells, fluid handling | Partially covered (macerator, kiln, dusts, cables) |
| **Forestry** | Bees, trees, butterflies, farms/multifarms, electron tubes, the worktable | Not started — **industrial side only** (farms, processing, electronics); genetics (bees/trees/butterflies) is out of scope |
| **Railcraft** | Rails, advanced minecarts, tanks, signals, coke ovens, boilers | Not started |

See each breakdown in [`mods/`](mods/).

### Adjacent pack mods (context, mostly out of scope)

Packs of this style also leaned on storage and quality-of-life mods. Most have living modern equivalents and are **not** Logistics' job:

- **Storage** — Iron Chests, Iron Tanks, Storage Drawers all exist in modern form; **Ender Storage** is abandoned but has modern replacements. Out of scope (Logistics provides the *pipes*, not the *boxes*).
- **World/flair** — Biomes O' Plenty, Binnie's Mods (extends Forestry). Out of scope, though Forestry-style genetics may overlap.
- **Magic** — Soul Shards. Out of scope.
- **Tools/QoL** — NEI, Waila, Schematica, Chisel, Carpenter's Blocks. Out of scope (modern equivalents exist; we integrate with JEI/Jade where it helps).

## Design principles

Extends the principles already stated in the project [`README.md`](../README.md):

- **Material-based identity** — each block reads at a glance from its vanilla materials (stone, copper, gold, diamond, obsidian…).
- **Layered progression** — the three-tier pipe model (Mechanical → Smart → Network), and more broadly: cheap-and-simple early, powerful-and-abstract late.
- **Authentic visuals** — items visibly travel through pipes; engines visibly heat; machines have state you can see.
- **Mod interoperability** — speak the platform energy/item/fluid APIs so any mod's inventories and machines participate.
- **Classic ergonomics** — simple placement, visible connections, learnable without a wiki.

Two more that guide *this* effort specifically:

- **Classic feel, modern vanilla.** When the old mod's assumptions clash with modern Minecraft (copper now exists; trees drop resin, not "sap"; the Crafter block exists), we **modernize to fit vanilla** rather than reproduce dated mechanics. See [`principles.md`](principles.md).
- **Balance is the deliverable.** The end state must feel like a coherent, balanced 1.7.10-era progression — not a pile of imported features. Every port/modernize decision is made with the whole curve in mind.

## What "complete" looks like

A player can install Logistics (or its module family) on a modern client and play a progression that recognizably mirrors the classic 1.7.10-era tech arc:

1. Early mechanical pipes + redstone/stirling engines.
2. Ore processing and RF machines (the Thermal layer).
3. Logistics-network automation tying storage and crafting together.
4. Forestry-style farming and processing automation (farms, biofuel, electronics) for endgame variety.
5. Railcraft-style transport and bulk processing for inter-base logistics.

…all while letting players install **only the parts they want** (see [`architecture.md`](architecture.md)).

## Non-goals

- Not a 1:1 clone of any single source mod's recipes or numbers.
- Not a kitchen-sink port of every block — features earn their place against balance and the "classic feel" test.
- Generally **not** monolithic multiblock structures (see the multiblock stance in [`principles.md`](principles.md)).
- Not storage blocks, world-gen flair, or magic — those are other mods' jobs.
