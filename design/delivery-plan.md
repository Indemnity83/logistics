# Delivery Plan

> The **detailed, contributor-facing** phased build plan. For the short, high-level, user-facing direction, see the root [`ROADMAP.md`](../ROADMAP.md) — that's the canonical public roadmap; this file is its in-depth companion.

The phased plan from day zero to "complete." Phases are sequenced to **build on existing strength first**: the BuildCraft / Logistics Pipes / Thermal Expansion automation core is largely in place, so we round it out before opening the Forestry and Railcraft frontiers.

This file is the human-readable mirror of **GitHub Project #4 "Logistics Roadmap."** It's authored here first; settled sections get decomposed into the board (see [Mapping to the board](#mapping-to-the-board)).

> Per-phase contents are populated from the [`mods/`](mods/) breakdowns. Until a breakdown is finalized, its phase rows are provisional.

## Phase overview

| Phase                   | Theme                                                                                                  | Source mods                            | State          |
|-------------------------|--------------------------------------------------------------------------------------------------------|----------------------------------------|----------------|
| **0 — Foundation**      | Materials, tools, energy, the pipe network                                                             | Logistics Pipes, base of BuildCraft/TE | ✅ Largely done |
| **1 — Automation core** | Round out engines, ore processing, RF machines, and the quarry                                         | BuildCraft + Thermal Expansion         | 🚧 Nearly complete — engines, ore-processing machines, and the fuel chain shipped; remaining: tiered batteries, fluid logistics, and the loader-parity/polish push (firewall pipe deferred) |
| **2 — Forestry**        | Industrial Forestry: farms, processing, power, electronics *(tree harvesting only; no genetics/breeding)* | Forestry                               | — Not started  |
| **3 — Transport**       | Rails, advanced carts, tanks, signals, bulk processing                                                 | Railcraft                              | — Not started  |

---

## Versioning & 1.0

**1.0 is a stability promise, not the finish line.** It does *not* mean all four mods are done — that's the long-term vision. 1.0 means a coherent, self-contained slice is complete and we'll stand behind it.

**1.0 = the Automation core (Phase 1) complete and stable, on both loaders.** That's BuildCraft + Logistics Pipes + Thermal Expansion — a complete, recognizable mod on its own. Forestry (`logistics-forestry`) and Railcraft (`logistics-transport`) are **post-1.0** modules that ship as later 1.x minors.

### Definition of done for 1.0

1. **Phase 1 feature-complete** — the BC/TE gaps filled. Shipped: fluids foundation ✅, the combustion-tier **Fuel Engine** ✅, the **Steam / Magmatic / Reaction** engines ✅ (dynamo tiers unified into the engine line), **Alloy Smelter** ✅, **Sawmill** ✅, **Crucible** ✅, **Refinery** + oil/fuel chain ✅, macerator byproduct outputs ✅, and the **Sequential Fabricator** ✅ (the end-game manufacturer). Logistics Pipes is already done. **Still open for the bar:** tiered batteries and fluid logistics (liquid provider/supplier/request + a fluid↔item step). *(The **firewall pipe** and **machine upgrades** are **not** part of the 1.0 bar — firewall is deferred pending a solid use case; machine upgrades are exploratory — see [ROADMAP.md](../ROADMAP.md) Exploring/RFC.)*
2. **Loader parity** — NeoForge shipped, not "in progress." Fabric + NeoForge both at the bar.
3. **Format stability** — block/BE NBT, data components, and config settled; **no save-breaking changes expected**. This is the real meaning of 1.0.
   - **Content availability** is part of this promise: a 1.0 world must stay fully playable through later phases — no post-1.0 material may become *unobtainable* on an existing save. New materials are sourced without new worldgen ore where possible; any genuinely-new ore commits to retrogen, not pre-seeding. See [RFC 0004](rfcs/0004-worldgen-stability.md).
4. **Polish** — JEI/recipe coverage, a balance pass, current docs, no known crashes.

### Path to 1.0 — decided sequence (Jul 2026)

With the engine and machine lines shipped, the call is to **close the 1.0 bar rather than open Phase 2** (Forestry seeds already leaked in, but farms wait for 1.0). Ordered by ascending risk/effort:

1. **Tiered Batteries** ([`features/0107-tiered-batteries.md`](features/0107-tiered-batteries.md)) — the last self-contained Phase-1 power gap (Copper/Gold/Ender storage line with configurable I/O). Low design risk; the clear first win.
2. **Fluid logistics** — the one remaining *big* Phase-1 system: the **Fluid Transposer** (fluid↔item packaging) first, then liquid provider/supplier/request over the network. Spike/brief before building.

Then: confirm **NeoForge parity**, run a **balance pass**, refresh docs, and gate on `1.0.0-pre.N`.

**Not on the path:**
- **Firewall pipe** — **deferred** (no solid use case yet; see below). Not a 1.0 item.
- **Recipe-book display parity** — the Refinery and Sequential Fabricator already have **JEI categories + Jade HUD** (#744/#745). They lack only the optional *vanilla* recipe-book display type the other machines register — a minor nicety, not a 1.0 gate. Fold into the balance/polish pass if wanted.

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
- ✅ Base materials: tin, bronze, apatite, dusts, gears, chipsets, valves; tools (wrench)
- ✅ Engines (redstone / stirling / creative) with heat stages; cables (copper/gold/ender); battery
- ✅ Macerator (RF grinding + custom recipes + JEI), Kiln (RF furnace), Laser Quarry (marker-bounded)

> Detail and any remaining gaps live in [`mods/logistics-pipes.md`](mods/logistics-pipes.md) and the Phase-0 portions of [`mods/buildcraft.md`](mods/buildcraft.md) and [`mods/thermal-expansion.md`](mods/thermal-expansion.md).

## Phase 1 — Automation core 🚧

*Finish the BuildCraft + Thermal Expansion layer the classic tech experience was built on. Derived from [`mods/buildcraft.md`](mods/buildcraft.md), [`mods/thermal-expansion.md`](mods/thermal-expansion.md), and the gaps in [`mods/logistics-pipes.md`](mods/logistics-pipes.md).*

**🔑 Fluids foundation** ✅ *(shipped v0.7.3 — prerequisite for much of this phase + later)*
- ✅ Fluid transport layer (fluid pipes) on the platform fluid API
- ✅ Fluid storage (tanks) + machine fluid I/O
- ✅ Pump (world fluid → network)
- *Fast-follow:* Iron mid-tier pipe and a fluid filter pipe (not gated for the foundation)

**Engines & power**
- ✅ Combustion-tier **Fuel Engine** (fluid fuel + coolant, manage-or-explode tension) *(v0.8.4)*
- ✅ Dynamos unified into the engine line — **Magmatic** (lava/fluid-fuel), **Steam** (boiler/pressure), and **Reaction** (custom reactant recipes) engines *(v0.8.4)*
- ✅ **Power Junction** — powers a logistics network from cables/batteries; engines power extraction pipes directly *(v0.8.0)*
- Battery → tiered energy-storage line with configurable I/O — **still open** ([`features/0107-tiered-batteries.md`](features/0107-tiered-batteries.md))

**Ore processing & machines**
- ✅ Macerator secondary/byproduct outputs (chance-based) *(v0.8.0)*
- ✅ Alloy Smelter — Bronze *(v0.8.2; only the Bronze alloy exists today — Invar/Electrum not yet added)*
- ✅ Sawmill (logs → planks + sawdust) *(v0.8.0)*
- ✅ Magma Crucible (solids → molten fluid) *(shipped as the **Crucible**, v0.8.2)*
- ✅ **Sequential Fabricator** — the consolidated end-game manufacturer (chipset/electronics crafting; see below) *(v0.8.3)*
- Fluid Transposer (fluid↔item container fill/empty) — **still open** *(the Refinery covers fluid→fluid; the transposer is the item-packaging bridge fluid logistics needs)*

*Exploratory (not committed for 1.0 — [ROADMAP.md](../ROADMAP.md) Exploring/RFC):*
- Machine upgrades / augments (speed / efficiency / secondary / auto-output) — see [`features/0106-machine-upgrades.md`](features/0106-machine-upgrades.md)

**Pipes & logistics QoL**
- ✅ Pipe operation power gating *(done — #464/#465/#469)*
- Firewall pipe (network segmentation) — **deferred**, no solid use case yet; not a 1.0 item ([`features/0110-firewall-pipe.md`](features/0110-firewall-pipe.md))
- Fluid logistics: liquid provider/supplier/request — **still open** *(needs the Fluid Transposer step above)* — the one remaining big Phase-1 system

*Reclassified (not committed for 1.0 — [ROADMAP.md](../ROADMAP.md)):*
- Remote Orderer (handheld network access) — **exploratory**; leaned on an Ender Chest-style companion mod. See [`features/0109-remote-orderer.md`](features/0109-remote-orderer.md).
- Obsidian vacuum pipe (world item pickup) — **not planned**; vanilla hoppers cover this. See [`features/0108-obsidian-vacuum-pipe.md`](features/0108-obsidian-vacuum-pipe.md).

**Fuels** *(couples to the Fuel Engine + Forestry biofuel)*
- ✅ Oil sourcing settled and the fuel chain built — crude oil worldgen (oil sand/shale), the **Refinery** (crude → fuel oil), and biomass/biofuel fluids *(v0.8.2–v0.8.3)*

**Sequential Fabricator — a cross-mod consolidation (shipped)**
- One end-game manufacturing machine now fills the role of **three** source-mod blocks: BuildCraft's **Assembly Table + Laser**, and Forestry's **Thermionic Fabricator** (electron tubes) / **Carpenter**. It sequentially crafts the electronics tier (chipsets, valves). This resolves several previously-TBD rows across [`mods/buildcraft.md`](mods/buildcraft.md) and [`mods/forestry.md`](mods/forestry.md) — see those files.

**Programmable behavior — explicitly post-1.0 (deferred)**
- The unified gates + circuits system (BuildCraft **gates** · TE programmable **augments** · Forestry **circuits** → one "programmable behavior" layer) is **not a 1.0 item.** Deferred until Forestry actually needs it — i.e. circuit boards to program Forestry blocks (Phase 2) — or later. See [RFC 0001](rfcs/0001-programmable-behavior.md).
- *Distinct from **machine upgrades** (speed/efficiency/auto-output modifiers) — that's a modifier system, not the programmable-logic layer. It's separately **exploratory** (not committed for 1.0; see [ROADMAP.md](../ROADMAP.md) Exploring/RFC) and is unaffected by this deferral either way.*

## Phase 2 — Forestry —

*Industrial Forestry — farms, processing, power, and electronics. Derived from [`mods/forestry.md`](mods/forestry.md). Apatite (fertilizer) and the logic-chip/core/valve "electronics" are already seeded. The genetics/biology axis (bees, tree-breeding, butterflies) is **out of scope** — see [RFC 0002](rfcs/0002-forestry-bees.md).*

**Farms** *(headline)*
- Single-block farms (the older Forestry design, **not** the later multiblock multifarm) + farm-type selection/upgrades
- Peat bog → peat fuel
- Fertilizer chain on existing Apatite

**Processing & power** *(close second)*
- Carpenter (modernized — clearer than the original)
- Squeezer → Fermenter → Still biofuel chain *(parallel fuel for the combustion engine)*
- Bottler *(needs fluids)*
- Peat / biofuel engines unified into the engine line

**Trees** *(wood via farms only — no breeding)*
- Farms harvest vanilla trees for wood + resin; **no arboriculture/breeding genetics** (out of scope — [RFC 0002](rfcs/0002-forestry-bees.md))

**Electronics**
- Thermionic Fabricator (modernized, ideally unified with the Carpenter) → electron tubes (map to existing cores/valves/logic chips)
- Circuit boards + soldering → ties into the (post-1.0) programmable-behavior RFC, and is the natural trigger to revisit it

**Bees / genetics** *(out of scope — separate-mod territory)*
- Bees, tree-breeding, and butterflies are not built here; the genetics axis diverges from vanilla and would dominate the module — see [RFC 0002](rfcs/0002-forestry-bees.md)

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
- Steam power (boiler/turbine) — *TBD; leaning toward a **Create compat layer** over a homegrown boiler (see [RFC 0003](rfcs/0003-railcraft-multiblocks.md)); electric locomotives/tracks are out of scope*
- Bulk fluid Tanks — *TBD (valid multiblock exception?)*

> **Cross-cutting dependencies:** the **Fluids foundation** (Phase 1) unblocks fluid logistics, several TE machines, biofuel bottling (Phase 2), and tank carts/tanks (Phase 3). The **programmable-behavior RFC** spans Phases 1–2. Schedule both early within their phases.

---

## Mapping to the board

When a breakdown section settles, decompose it into Project #4:

| Doc concept                                  | Project #4 representation                                                                     |
|----------------------------------------------|-----------------------------------------------------------------------------------------------|
| **Phase** (0–3)                              | **Milestone** (e.g. "Phase 1 — Automation core")                                              |
| **Mod-area / epic** (e.g. "Engines & power") | **Epic issue** (parent issue)                                                                 |
| **Feature row** in a `mods/*.md` table       | **Sub-issue** under the epic (uses the board's *Parent issue* / *Sub-issues progress* fields) |
| **Decision** (Port/Modernize/Skip)           | Label: `port` / `modernize` / `wontport` *(to be created)* + existing scope labels            |
| **Status** (✅/🚧/—/❌)                        | Board **Status** field; ❌ rows are not filed                                                  |

**Decomposition rules:**
- Reconcile with issues already on the board (e.g. *"Forestry-Inspired Farming & Electronics Roadmap"*, *"BuildCraft compatibility shim"*) — extend/link them, don't duplicate.
- Only file accepted rows (Port/Modernize). Skip rows stay documented here as the rationale.
- Do a **mapping dry-run on one mod file** before any bulk issue creation.
- This step happens **only with explicit approval** — the markdown is authored and reviewed first.
