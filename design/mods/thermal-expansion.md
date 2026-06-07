# Thermal Expansion

*RF machines and ore processing — the backbone of the classic mid-game. Logistics has the pulverizer (Macerator) and furnace (Kiln); the gaps are the alloying/sawmill/fluid machines, secondary ore outputs, machine upgrades, and a tiered energy-storage line.*

**Source era:** 1.7.10–1.12.2 (Thermal Expansion / CoFH).
**Logistics module:** `logistics-automation` (automation + power domains) + base materials in `logistics-core`.
**Phase:** 0 (done parts) / 1 (gaps).

See [`../principles.md`](../principles.md) for the table legend.

## Core machines

| Feature | What it did (1.7.10–1.12.2) | Decision | Modern take / balance notes | Status | Maps to |
|---|---|---|---|---|---|
| Pulverizer | Ore → 2 dust + chance secondary | Modernize | **Macerator** (RF grinder, custom recipes, JEI). *Secondary/byproduct outputs not yet implemented* | ✅ Done (base) | `core` / Macerator |
| — secondary/byproduct outputs | Chance bonus dust per recipe | Modernize | Add chance-based second output (GT-style); high-value, balances ore yield | 🚧 Planned | Phase 1 — macerator outputs |
| Redstone Furnace | RF-powered smelting | Modernize | **Kiln** (reuses vanilla smelting recipes) | ✅ Done | `automation` / Kiln |
| Sawmill | Logs → extra planks + sawdust | Port | We already have Wood Pulp/Flour dusts; needs the machine | 🚧 Planned | Phase 1 — Sawmill |
| Induction Smelter | Alloying: 2 inputs → alloy (+ slag) | Port | Key machine — makes Bronze/Invar/Electrum etc. coherent; pairs with the alloy material set | 🚧 Planned | Phase 1 — Alloy Smelter |
| Magma Crucible | Solids → molten fluid | Port | Depends on the fluid layer | — | Phase 1 — fluids |
| Fluid Transposer | Fill/empty containers; fluid+item recipes | Port | Depends on the fluid layer | — | Phase 1 — fluids |
| Cyclic Assembler | Machine autocrafting | Skip | Covered by vanilla Crafter + Crafting Logistics Pipe | ❌ | [`logistics-pipes.md`](logistics-pipes.md) |
| Phytogenic Insolator | RF + fertilizer → grow crops/trees | Modernize | Overlaps Forestry farms; defer and unify there | — | Phase 2 — Forestry farms |
| Energetic Infuser / Charge Bench | Charge powered items | TBD | Only if we add powered handheld items | TBD | — |
| Glacial Precipitator / Aqueous Accumulator | Make ice/snow / water | TBD | Minor utility; low priority | TBD | — |

## Machine upgrades & augments

| Feature | What it did | Decision | Modern take / balance notes | Status | Maps to |
|---|---|---|---|---|---|
| Augments (speed / efficiency / secondary / auto-output) | Slot-in machine modifiers | Modernize | Strong system worth adopting as a unified **machine upgrade** mechanic across Macerator/Kiln/etc. | 🚧 Planned | Phase 1 — machine upgrades |
| Machine frames / tiers (Basic→Resonant) | Crafted tiers gating machine power | Modernize | Map onto vanilla metal ladder; keep tier count modest | TBD | Phase 1 — machine tiers |

## Power generation & storage (Dynamos / Energy Cells)

| Feature | What it did | Decision | Modern take / balance notes | Status | Maps to |
|---|---|---|---|---|---|
| Dynamos (Steam/Magmatic/Compression/Reactant/etc.) | Modular RF generators from various fuels | Modernize | Overlaps BuildCraft engines — **unify** under the engine line rather than a parallel dynamo line; magmatic/compression = fluid-fueled engine tiers | TBD | Phase 1 — engines |
| Energy Cells (Leadstone→Resonant) | Tiered RF storage with configurable I/O | Modernize | **Battery** exists; expand into a tiered storage line with I/O config | 🚧 Planned | `power` / Battery (tiers) |
| Energy conduits (Fluxducts) | RF transport | Modernize | Covered by **cables** (copper/gold/ender) | ✅ Done | `power` / cables |
| Itemducts / Fluiducts (+ servo/filter/retriever) | Item/fluid transport network | Skip | Covered by the Logistics pipe system | ❌ | [`logistics-pipes.md`](logistics-pipes.md) |
| Tesseract | Wireless cross-dimension item/fluid/energy | Skip | Out of scope (Ender-Storage-like); modern replacements exist | ❌ | — |

## Materials

| Feature | What it did | Decision | Modern take / balance notes | Status | Maps to |
|---|---|---|---|---|---|
| Base metals (copper/tin/silver/lead/nickel) | Ore + ingots feeding alloys | Modernize | Copper is vanilla now; **Tin** implemented; add others only as alloys require — keep the set small | 🚧 Partial | `core` / materials |
| Alloys (bronze/invar/electrum/constantan) | Smelter alloys for tiers | Port | **Bronze** done; add a tight set tied to the Alloy Smelter | 🚧 Partial | `core` / Bronze (+ alloys) |
| High-tier alloys (signalum/lumium/enderium) | Gate TE's top tiers | TBD | Only if endgame machine tiers need them; risk of bloat | TBD | — |
| Gears / plates / components | Crafting intermediates | Modernize | **Gears** (9), dusts, logic chips, valves, cores implemented | ✅ Done | `core` / components |
| Hardened glass / Rockwool (16 colors) | Decorative/utility | Skip | Decorative bloat; out of scope | ❌ | — |
| Cured rubber | Component for cables/machines | Port | **Rubber Chunk / Rubber Mix** implemented | ✅ Done | `power` / rubber |
| Strongboxes / Caches / Portable Tanks / Satchel | Portable storage | Skip | Storage is other mods' job (Drawers/Iron Chests) | ❌ | — |

> TODO: confirm exact pulverizer secondary-output rates we want to mirror for balance (TE used per-recipe chance + a secondary item) — ties into the macerator-outputs epic.
> TODO: decide whether dynamos collapse fully into the engine line or keep a couple of distinct generator blocks (e.g. a fluid-fuel "magmatic" generator) — couples to the BuildCraft combustion-engine call.
