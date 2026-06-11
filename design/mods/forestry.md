# Forestry

*The "endgame variety" layer. For our purposes the **farms** are the headline — followed closely by the **processing machines and the power** that feed them — then trees and electronics. **Bees are deferred** (the vanilla breeding model diverges sharply from Forestry's; see below) and butterflies are skipped. Groundwork is already seeded: apatite for fertilizer, and the logic-chip/core/valve components resemble electron-tube electronics. This is Phase 2.*

**Source era:** 1.7.10–1.12.2 (Forestry).
**Logistics module:** `logistics-forestry` (new).
**Phase:** 2.

See [`../principles.md`](../principles.md) for the table legend. Forestry is large; the calls below favor **the older single-block designs** and **avoiding heavy multiblocks** (see the multiblock stance).

## Farms (agriculture) — the headline

| Feature | What it did (1.7.10–1.12.2) | Decision | Modern take / balance notes | Status | Maps to |
|---|---|---|---|---|---|
| Farm (single-block) | Automated planting/harvesting of a farm type | Port | Target the **older single-block Forestry farms**, *not* the later multiblock Multifarm. One block per farm + a farm-type selection; range/speed upgrades | — | Phase 2 — farms |
| Multifarm (later multiblock) | Large modular managed-farm structure | Skip | Deliberately not porting the multiblock direction Forestry migrated to — keep farms as single blocks | ❌ | — |
| Peat bog / peat | Grow peat → solid fuel | Port | Nice early-fuel loop; pairs with apatite fertilizer | — | Phase 2 — farms/fuel |
| Fertilizer / humus / compost | Apatite-based fertilizer speeds growth | Modernize | **Apatite already implemented** — build the fertilizer chain on it | 🚧 Planned | `core` / Apatite → fertilizer |

## Processing machines & power

*The chain that feeds the farms and turns their output into fuel and components — a close second to farms in priority.*

| Feature | What it did | Decision | Modern take / balance notes | Status | Maps to |
|---|---|---|---|---|---|
| Carpenter | Liquid-assisted crafting (circuits, impregnated items, etc.) | Modernize | Key machine, but the original was **unintuitive** — redesign into a clearer crafting machine while keeping its role | — | Phase 2 — Carpenter |
| Squeezer | Seeds/fruit/honey → liquids (seed oil, juice) | Port | Feeds biofuel + Carpenter | — | Phase 2 — processing |
| Fermenter | Saplings/biomass + fertilizer → biomass liquid | Port | Biofuel chain step 1 | — | Phase 2 — biofuel |
| Still | Biomass → ethanol/biofuel | Port | Biofuel chain step 2; a **parallel fuel** for the combustion engine (couples to BuildCraft) | — | Phase 2 — biofuel |
| Bottler / Can filling | Fill containers with fluids | Port | Depends on the fluid layer | — | Phase 2 — fluids |
| Engines (peat / biogas / biofuel / electrical) | Forestry's own engine line | Modernize | **Unify with the engine line**: peat-fired + biofuel engine tiers rather than a parallel system. This is "the power that went with the farms" | — | `power` / engines |
| Moistener | Make mycelium/mossy blocks | Modernize | Minor; low priority | — | — |

## Trees (arboriculture)

| Feature | What it did | Decision | Modern take / balance notes | Status | Maps to |
|---|---|---|---|---|---|
| Arboriculture | Breed tree species; saplings/pollen; grafter; treealyzer | Port | Yields wood variety + fruit; could start with a simpler species set before deep genetics | — | Phase 2 — trees |
| Tree products (fruit/wood/sapling) | Specialty woods, fruits, latex | Modernize | Align with vanilla wood types + **resin** (not "sap") | — | Phase 2 — trees |

## Electronics (electron tubes & circuits)

| Feature | What it did | Decision | Modern take / balance notes | Status | Maps to |
|---|---|---|---|---|---|
| Thermionic Fabricator | Molten glass + ingots → **electron tubes** | Modernize | Like the Carpenter, the original was **unintuitive** — redesign for clarity, ideally **unify with the Carpenter** into one approachable components machine | — | Phase 2 — electronics |
| Electron tubes (copper/tin/bronze/iron/gold/diamond/blaze/apatite/lapis/ender/etc.) | Typed components for circuit programming | Modernize | **Already seeded**: Logistics has *logic chips*, *cores* (13), and *valves* (13) that mirror this tube set | 🚧 Planned | `core` / cores, valves, logic chips |
| Circuit boards + Soldering | Program machine/farm behavior via tube layouts | TBD | **Unify** circuits with TE-style augments + BC gates into one "programmable behavior" system across the mod | — | Phase 1/2 — electronics (RFC) |

## Bees (apiculture) — deferred

> **Bees are not a Phase 2 headline and may be skipped entirely for v1.** Modern vanilla already breeds bees by feeding them flowers — a model that diverges sharply from Forestry's princess/drone/queen lifecycle and Mendelian genetics. Reconciling the two (extend vanilla? full parallel system? skip?) needs a deliberate design pass before any of this is scheduled — flag for an Ideas Discussion.

| Feature | What it did | Decision | Modern take / balance notes | Status | Maps to |
|---|---|---|---|---|---|
| Apiary | Single-block bee housing; bees produce combs | Modernize | **Vanilla already has the beehive/bee nest** — extend its behavior rather than add a parallel block | — | — |
| Bee lifecycle (princess/drone/queen) | Breed queens; offspring; decay | TBD | Fundamentally different from vanilla flower-breeding; the crux of whether bees happen at all | — | — |
| Bee genetics / mutations | Mendelian traits + species discovery | TBD | Depends on the lifecycle decision; if pursued, simplify the trait matrix | — | — |
| Bee products + Centrifuge | Combs → centrifuge → honey/wax/jelly | TBD | Coupled to bees; deferred with them | — | — |
| Frames / Alveary / bee tools | Output modifiers; advanced multiblock; analysis | TBD | Deferred; Alveary would also fight the multiblock stance | — | — |

## Out of scope

| Feature | What it did | Decision | Modern take / balance notes | Status | Maps to |
|---|---|---|---|---|---|
| Butterflies (lepidopterology) | Breed butterflies; cocoons; serum | Skip | Straight skip for now — decorative, low gameplay-per-complexity | ❌ | — |
| Backpacks (forester/miner/digger/etc.) | Portable categorized storage | Skip | Storage is other mods' job | ❌ | — |
| Crates | Compact storage | Skip | Out of scope | ❌ | — |
| Mail / Letters / Trade Stations | In-game postal + auto-trade | Skip | Niche; out of scope | ❌ | — |

> TODO: confirm the exact electron-tube → circuit-board recipe model, and whether to merge it with TE augments + BuildCraft gates into a single "programmable behavior" subsystem (likely an Ideas Discussion — it spans three source mods). See [RFC 0001 — Programmable Behavior](../rfcs/0001-programmable-behavior.md).
> TODO: settle the bees question (extend vanilla / parallel system / skip for v1) before any Phase 2 bee work is scheduled. See [RFC 0002 — Forestry Bees](../rfcs/0002-forestry-bees.md).
