# Thermal Expansion

*RF machines and ore processing — the backbone of the classic mid-game. Logistics now has the pulverizer (Macerator, with byproduct outputs), furnace (Kiln), **Alloy Smelter**, **Sawmill**, and **Crucible** (magma crucible), and the dynamo family folded into the engine line. Remaining gaps: the **Fluid Transposer**, a **tiered energy-storage** line, and machine upgrades (exploratory).*

**Source era:** 1.7.10–1.12.2 (Thermal Expansion / CoFH).
**Logistics module:** `logistics-automation` (automation + power domains) + base materials in `logistics-core`.
**Phase:** 0 (done parts) / 1 (gaps).

See [`../principles.md`](../principles.md) for the table legend.

## Core machines

| Feature | What it did (1.7.10–1.12.2) | Decision | Modern take / balance notes | Status | Maps to |
|---|---|---|---|---|---|
| Pulverizer | Ore → 2 dust + chance secondary | Modernize | **Macerator** (RF grinder, custom recipes, JEI) with a chance-based byproduct output | ✅ Done | `core` / Macerator |
| — secondary/byproduct outputs | Chance bonus dust per recipe | Modernize | Chance byproduct shipped (v0.8.0) — recipes can drop one optional chance byproduct (resolved as a `ChanceOutput`), tunable per recipe. Also used by the Alloy Smelter's recycling recipes (e.g. slag) | ✅ Done | `automation` / macerator byproducts |
| Redstone Furnace | RF-powered smelting | Modernize | **Kiln** (reuses vanilla smelting recipes) | ✅ Done | `automation` / Kiln |
| Sawmill | Logs → extra planks + sawdust | Port | Shipped (v0.8.0) — logs → planks + sawdust + pulped biomass; recipe type + JEI + Jade | ✅ Done | `automation` / Sawmill |
| Induction Smelter | Alloying: 2 inputs → alloy (+ slag) | Port | Shipped as the **Alloy Smelter** (v0.8.2). Only the **Bronze** alloy exists today; Invar/Electrum are candidates as the tier needs them | ✅ Done | `automation` / Alloy Smelter |
| Magma Crucible | Solids → molten fluid | Port | Shipped as the **Crucible** (v0.8.2) — melts solids into a molten-metal tank | ✅ Done | `automation` / Crucible |
| Fluid Transposer | Fill/empty containers; fluid+item recipes | Port | The fluid↔item bridge fluid logistics needs. **Still open** — the Refinery covers fluid→fluid, but nothing packages fluids into items/containers yet | — | Phase 1 — machines |
| Cyclic Assembler | Machine autocrafting | Skip | Covered by vanilla Crafter + Crafting Logistics Pipe (general autocrafting); the **Sequential Fabricator** covers the bespoke electronics-tier manufacturing | ❌ | [`logistics-pipes.md`](logistics-pipes.md) |
| Phytogenic Insolator | RF + fertilizer → grow crops/trees | Modernize | Overlaps Forestry farms; defer and unify there | — | Phase 2 — Forestry farms |
| Energetic Infuser / Charge Bench | Charge powered items | TBD | Only if we add powered handheld items | — | — |
| Glacial Precipitator / Aqueous Accumulator | Make ice/snow / water | TBD | Minor utility; low priority | — | — |

## Machine upgrades & augments

| Feature | What it did | Decision | Modern take / balance notes | Status | Maps to |
|---|---|---|---|---|---|
| Augments (speed / efficiency / secondary / auto-output) | Slot-in machine modifiers | Modernize | Strong system worth adopting as a unified **machine upgrade** mechanic across Macerator/Kiln/etc. **Exploratory — not committed for 1.0** ([ROADMAP](../../ROADMAP.md) Exploring/RFC) | — | Exploring — machine upgrades |
| Machine frames / tiers (Basic→Resonant) | Crafted tiers gating machine power | Modernize | Map onto vanilla metal ladder; keep tier count modest | — | Phase 1 — machine tiers |

## Power generation & storage (Dynamos / Energy Cells)

| Feature | What it did | Decision | Modern take / balance notes | Status | Maps to |
|---|---|---|---|---|---|
| Dynamos (Steam/Magmatic/Compression/Reactant/etc.) | Modular RF generators from various fuels | Modernize | Unified into the engine line as planned (v0.8.4): **Steam** (boiler/pressure), **Magmatic** (lava/fluid), **Reaction** (custom reactant recipes — the reactant dynamo). Fuel Engine is the liquid-fuel tier | ✅ Done | `power` / engine line |
| Energy Cells (Leadstone→Resonant) | Tiered RF storage with configurable I/O | Modernize | **Battery** exists as a single tier; expand into a Copper/Gold/Ender storage line with I/O config. **Still open** — the clearest remaining Phase-1 power gap | 🚧 Planned | `power` / Battery (tiers) — [`../features/0107-tiered-batteries.md`](../features/0107-tiered-batteries.md) |
| Energy conduits (Fluxducts) | RF transport | Modernize | Covered by **cables** (copper/gold/ender) | ✅ Done | `power` / cables |
| Itemducts / Fluiducts (+ servo/filter/retriever) | Item/fluid transport network | Skip | Covered by the Logistics pipe system | ❌ | [`logistics-pipes.md`](logistics-pipes.md) |
| Tesseract | Wireless cross-dimension item/fluid/energy | Skip | Out of scope (Ender-Storage-like); modern replacements exist | ❌ | — |

## Materials

| Feature | What it did | Decision | Modern take / balance notes | Status | Maps to |
|---|---|---|---|---|---|
| Base metals (copper/tin/silver/lead/nickel) | Ore + ingots feeding alloys | Modernize | Copper is vanilla; **Tin** implemented (ore + raw + ingot/nugget/block). No silver/lead/nickel yet — add only as an alloy requires; keep the set small | ✅ Done | `core` / materials |
| Alloys (bronze/invar/electrum/constantan) | Smelter alloys for tiers | Port | **Bronze** shipped via the Alloy Smelter. Invar/Electrum not yet added — introduce a tight set only as a tier needs them | 🚧 Planned | `core` / Bronze (+ alloys) |
| High-tier alloys (signalum/lumium/enderium) | Gate TE's top tiers | TBD | Only if endgame machine tiers need them; risk of bloat | — | — |
| Gears / plates / components | Crafting intermediates | Modernize | **Gears** (8 — tin gear dropped in v0.8.0), **chipsets** (7, renamed from "chips"), **valves** (15, reworked with electron-tube textures), **silicon wafers**, `machine_core`, dusts | ✅ Done | `core` / components |
| Hardened glass / Rockwool (16 colors) | Decorative/utility | Skip | Decorative bloat; out of scope | ❌ | — |
| Cured rubber | Component for cables/machines | Port | **Rubber Chunk / Rubber Mix** implemented | ✅ Done | `power` / rubber |
| Strongboxes / Caches / Portable Tanks / Satchel | Portable storage | Skip | Storage is other mods' job (Drawers/Iron Chests) | ❌ | — |

> Resolved: macerator byproducts shipped (v0.8.0) as a per-recipe chance byproduct; balance is tunable per recipe.
> Resolved: dynamos collapsed **fully** into the engine line — no separate generator blocks. Steam/Magmatic/Reaction/Fuel are engine tiers (v0.8.4).
> TODO: the **Fluid Transposer** is the last core TE machine gap — it's the fluid↔item packaging step that fluid logistics (liquid provider/supplier/request) depends on. Schedule it with the fluid-logistics epic.
