# Roadmap

The phased plan from day zero to "complete." Phases are sequenced to **build on existing strength first**: the BuildCraft / Logistics Pipes / Thermal Expansion automation core is largely in place, so we round it out before opening the Forestry and Railcraft frontiers.

This file is the human-readable mirror of **GitHub Project #4 "Logistics Roadmap."** It's authored here first; settled sections get decomposed into the board (see [Mapping to the board](#mapping-to-the-board)).

> Per-phase contents are populated from the [`mods/`](mods/) breakdowns. Until a breakdown is finalized, its phase rows are provisional.

## Phase overview

| Phase | Theme | Source mods | State |
|---|---|---|---|
| **0 — Foundation** | Materials, tools, energy, the pipe network | Logistics Pipes, base of BuildCraft/TE | ✅ Largely done |
| **1 — Automation core** | Round out engines, ore processing, RF machines, quarry, gates/automation | BuildCraft + Thermal Expansion | 🚧 In progress |
| **2 — Forestry** | Biological automation: bees, trees, farms, electronics | Forestry | — Not started |
| **3 — Transport** | Rails, advanced carts, tanks, signals, bulk processing | Railcraft | — Not started |

---

## Versioning & 1.0

**1.0 is a stability promise, not the finish line.** It does *not* mean all five mods are done — that's the long-term vision. 1.0 means a coherent, self-contained slice is complete and we'll stand behind it.

**1.0 = the Automation core (Phase 1) complete and stable, on both loaders.** That's BuildCraft + Logistics Pipes + Thermal Expansion — a complete, recognizable mod on its own. Forestry (`logistics-forestry`) and Railcraft (`logistics-transport`) are **post-1.0** modules that ship as later 1.x minors.

### Definition of done for 1.0

1. **Phase 1 feature-complete** — the BC/TE gaps filled (fluids foundation, combustion-tier engine, alloy smelter, sawmill, macerator secondary outputs, machine upgrades). Logistics Pipes is already done.
2. **Loader parity** — NeoForge shipped, not "in progress." Fabric + NeoForge both at the bar.
3. **Format stability** — block/BE NBT, data components, and config settled; **no save-breaking changes expected**. This is the real meaning of 1.0.
4. **Polish** — JEI/recipe coverage, a balance pass, current docs, no known crashes.

### Released in lockstep

**1.0 is declared only when *every* supported MC version branch (mc/1.21.1, mc/1.21.11, mc/26.1, …) and *both* loaders all meet the bar simultaneously.** No branch goes 1.0 ahead of the others. This is the stronger, slower promise: a player on any supported version gets the same stable 1.0 core. (Pre-1.0, branches still version independently on their own lines.)

### Release mechanics

- **Pre-1.0 (now, 0.6.x):** `feat:` → patch, `feat!:` → minor (per release-please config). Burn down Phase 1 + NeoForge as 0.7–0.9 minors.
- **The flip at 1.0:** afterward `feat:` → **minor** and `feat!:` → **major** — so breaking changes become loud. Don't declare 1.0 until we're willing to either avoid breaking changes or accept major bumps.
- **Gate:** publish `1.0.0-pre.N` via the pre-release workflow for final cross-version/cross-loader testing before declaring `1.0.0`.

---

## Phase 0 — Foundation ✅

*The groundwork. Mostly complete — listed so the roadmap starts from day zero.*

- ✅ Loader-agnostic energy abstraction (RF; Team Reborn / NeoForge adapters)
- ✅ Item transport + traveling-item simulation and rendering
- ✅ Three-tier pipe model (Mechanical → Smart → Network)
- ✅ Full logistics network: provider / requester / supplier / crafting / process / satellite + chassis MkI–V & modules
- ✅ Base materials: tin, bronze, apatite, dusts, gears, logic chips; tools (wrench, probe)
- ✅ Engines (redstone / stirling / creative) with heat stages; cables (copper/gold/ender); battery
- ✅ Macerator (RF grinding + custom recipes + JEI), Kiln (RF furnace), Laser Quarry (marker-bounded)

> Detail and any remaining gaps live in [`mods/logistics-pipes.md`](mods/logistics-pipes.md) and the Phase-0 portions of [`mods/buildcraft.md`](mods/buildcraft.md) and [`mods/thermal-expansion.md`](mods/thermal-expansion.md).

## Phase 1 — Automation core 🚧

*Finish the BuildCraft + Thermal Expansion layer the classic tech experience was built on. Derived from [`mods/buildcraft.md`](mods/buildcraft.md), [`mods/thermal-expansion.md`](mods/thermal-expansion.md), and the gaps in [`mods/logistics-pipes.md`](mods/logistics-pipes.md).*

**🔑 Fluids foundation** *(prerequisite for much of this phase + later)*
- Fluid transport layer (fluid pipes) on the platform fluid API
- Fluid storage (tanks) + machine fluid I/O
- Pump (world fluid → network)

**Engines & power**
- Combustion-tier engine (fluid fuel + coolant, manage-or-explode tension)
- Unify dynamos/generators into the engine line (fluid-fuel "magmatic" tier)
- Battery → tiered energy-storage line with configurable I/O

**Ore processing & machines**
- Macerator secondary/byproduct outputs (chance-based)
- Alloy Smelter (Bronze/Invar/Electrum + the alloy material set)
- Sawmill (logs → planks + sawdust)
- Machine upgrades / augments (speed / efficiency / secondary / auto-output)
- Magma Crucible + Fluid Transposer *(needs fluids)*

**Pipes & logistics QoL**
- Pipe operation power gating *(in progress)*
- Remote Orderer (handheld network access)
- Obsidian vacuum pipe (world item pickup)
- Firewall pipe (network segmentation)
- Fluid logistics: liquid provider/supplier/request *(needs fluids)*

**Fuels** *(couples to Combustion Engine + Forestry biofuel)*
- Decide oil/biofuel sourcing; build the fuel chain

**RFC (cross-cutting, needs Discussion before scheduling)**
- Programmable automation: BuildCraft **gates** + TE **augments** + Forestry **circuits** → one unified "programmable behavior" system

## Phase 2 — Forestry —

*Biological automation and endgame variety. Derived from [`mods/forestry.md`](mods/forestry.md). Apatite (fertilizer) and the logic-chip/core/valve "electronics" are already seeded.*

**Farms** *(headline)*
- Single-block farms (the older Forestry design, **not** the later multiblock multifarm) + farm-type selection/upgrades
- Peat bog → peat fuel
- Fertilizer chain on existing Apatite

**Processing & power** *(close second)*
- Carpenter (modernized — clearer than the original)
- Squeezer → Fermenter → Still biofuel chain *(parallel fuel for the combustion engine)*
- Bottler *(needs fluids)*
- Peat / biofuel engines unified into the engine line

**Trees**
- Arboriculture (saplings/pollen, grafter); products aligned to vanilla woods + resin

**Electronics**
- Thermionic Fabricator (modernized, ideally unified with the Carpenter) → electron tubes (map to existing cores/valves/logic chips)
- Circuit boards + soldering → ties into the Phase 1 programmable-behavior RFC

**Bees** *(deferred / uncertain — may be skipped for v1)*
- Vanilla flower-breeding diverges sharply from Forestry's princess/drone/queen; needs a design pass (Discussion) before scheduling

## Phase 3 — Transport —

*Rails, carts, and bulk inter-base logistics. Derived from [`mods/railcraft.md`](mods/railcraft.md). The most separable module (`logistics-transport`).*

**Tracks**
- Track tiers (basic → reinforced → high-speed)
- Switches/junctions, powered/booster, detectors
- Consolidated control tracks; routing track + tickets

**Locomotives & carts**
- Steam locomotive (automated trains)
- Tank / chest / work carts *(tank cart needs fluids)*
- Item & fluid loaders/unloaders; cart dispenser

**Signals**
- Simplified, approachable signal set (block/distant) + tuner

**Steam & steel chain**
- Coke Oven (coke + creosote) — reconsider multiblock
- Blast Furnace → **Steel** (single-block preferred); creosote → treated wood
- Steam power (boiler/turbine) — *TBD, couples to engine line*
- Bulk fluid Tanks — *TBD (valid multiblock exception?)*

> **Cross-cutting dependencies:** the **Fluids foundation** (Phase 1) unblocks fluid logistics, several TE machines, biofuel bottling (Phase 2), and tank carts/tanks (Phase 3). The **programmable-behavior RFC** spans Phases 1–2. Schedule both early within their phases.

---

## Mapping to the board

When a breakdown section settles, decompose it into Project #4:

| Doc concept | Project #4 representation |
|---|---|
| **Phase** (0–3) | **Milestone** (e.g. "Phase 1 — Automation core") |
| **Mod-area / epic** (e.g. "Engines & power") | **Epic issue** (parent issue) |
| **Feature row** in a `mods/*.md` table | **Sub-issue** under the epic (uses the board's *Parent issue* / *Sub-issues progress* fields) |
| **Decision** (Port/Modernize/Skip) | Label: `port` / `modernize` / `wontport` *(to be created)* + existing scope labels |
| **Status** (✅/🚧/—/❌) | Board **Status** field; ❌ rows are not filed |

**Decomposition rules:**
- Reconcile with issues already on the board (e.g. *"Forestry-Inspired Farming & Electronics Roadmap"*, *"BuildCraft compatibility shim"*) — extend/link them, don't duplicate.
- Only file accepted rows (Port/Modernize). Skip rows stay documented here as the rationale.
- Do a **mapping dry-run on one mod file** before any bulk issue creation.
- This step happens **only with explicit approval** — the markdown is authored and reviewed first.
