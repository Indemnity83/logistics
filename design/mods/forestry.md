# Forestry

*The "endgame variety" layer. For our purposes the **farms** are the headline — followed closely by the **processing machines and the power** that feed them — then electronics. **The entire genetics/biology axis is out of scope** for this mod: no bees, no tree-breeding (arboriculture), no butterflies (maintainer + contributor call, Jun 2026 — see [RFC 0002](../rfcs/0002-forestry-bees.md)). Forestry's fit here is its *industrial* side. This is Phase 2 — but several pieces landed early during Phase 1: **peat + bog earth** (v0.8.2), the **biomass/biofuel/fuel-oil fluids** (v0.8.2–v0.8.3), and the **Sequential Fabricator** already fills the electron-tube/Carpenter manufacturing role (see Electronics below). Apatite (fertilizer) and the chipset/valve electronics are also seeded.*

**Source era:** 1.7.10–1.12.2 (Forestry).
**Logistics module:** `logistics-forestry` (new).
**Phase:** 2.

See [`../principles.md`](../principles.md) for the table legend. Forestry is large; the calls below favor **the older single-block designs** and **avoiding heavy multiblocks** (see the multiblock stance).

## Farms (agriculture) — the headline

| Feature | What it did (1.7.10–1.12.2) | Decision | Modern take / balance notes | Status | Maps to |
|---|---|---|---|---|---|
| Farm (single-block) | Automated planting/harvesting of a farm type | Port | Target the **older single-block Forestry farms**, *not* the later multiblock Multifarm. One block per farm + a farm-type selection; range/speed upgrades | — | Phase 2 — farms |
| Multifarm (later multiblock) | Large modular managed-farm structure | Skip | Deliberately not porting the multiblock direction Forestry migrated to — keep farms as single blocks | ❌ | — |
| Peat bog / peat | Grow peat → solid fuel | Port | Shipped early (v0.8.2) — **bog earth** + **peat** solid fuel. The bog-growth/fertilizer loop pairs with apatite | ✅ Done | `worldgen` / bog earth + peat |
| Fertilizer / humus / compost | Apatite-based fertilizer speeds growth | Modernize | **Apatite already implemented** — build the fertilizer chain on it | 🚧 Planned | `core` / Apatite → fertilizer |

## Processing machines & power

*The chain that feeds the farms and turns their output into fuel and components — a close second to farms in priority.*

| Feature | What it did | Decision | Modern take / balance notes | Status | Maps to |
|---|---|---|---|---|---|
| Carpenter | Liquid-assisted crafting (circuits, impregnated items, etc.) | Modernize | Its manufacturing role is now filled by the **Sequential Fabricator** (v0.8.3) — the consolidated end-game manufacturer. A separate Carpenter is unlikely; revisit only if a distinct liquid-assisted recipe class emerges | — | see Electronics / Sequential Fabricator |
| Squeezer | Seeds/fruit/honey → liquids (seed oil, juice) | Port | Feeds biofuel + Carpenter | — | Phase 2 — processing |
| Fermenter | Saplings/biomass + fertilizer → biomass liquid | Port | Biofuel chain step 1 | — | Phase 2 — biofuel |
| Still | Biomass → ethanol/biofuel | Port | Biofuel chain step 2; a **parallel fuel** for the Fuel Engine (couples to BuildCraft). The **biomass/biofuel fluids already exist** (v0.8.3) — the squeezer→fermenter→still *machines* that produce them are the pending work | — | Phase 2 — biofuel |
| Bottler / Can filling | Fill containers with fluids | Port | Fluid layer shipped (v0.7.3) — now unblocked | — | Phase 2 — fluids |
| Engines (peat / biogas / biofuel / electrical) | Forestry's own engine line | Modernize | **Unify with the engine line**: peat-fired + biofuel engine tiers rather than a parallel system. This is "the power that went with the farms" | — | `power` / engines |
| Moistener | Make mycelium/mossy blocks | Modernize | Minor; low priority | — | — |

## Trees (arboriculture) — out of scope

> **Tree breeding/arboriculture is genetics, and genetics is out of scope for Logistics** (Jun 2026 — see [RFC 0002](../rfcs/0002-forestry-bees.md)). Logistics farms can already *grow and harvest* vanilla trees for wood; what we skip is the **breeding/species-discovery** layer (saplings/pollen genetics, grafter, treealyzer, specialty bred woods). If breeding is ever wanted, it's a separate, dedicated mod's job.

| Feature | What it did | Decision | Modern take / balance notes | Status | Maps to |
|---|---|---|---|---|---|
| Arboriculture (tree breeding/genetics) | Breed tree species; saplings/pollen; grafter; treealyzer | Skip | Genetics axis — out of scope for this mod; belongs in a dedicated genetics mod | ❌ | — |
| Tree products (fruit/wood/sapling) | Specialty woods, fruits, latex | Modernize | No *bred* specialty woods; cover wood/resin via the **farms** harvesting vanilla trees (resin, not "sap") rather than an arboriculture system | — | Phase 2 — farms |

## Electronics (electron tubes & circuits)

| Feature | What it did | Decision | Modern take / balance notes | Status | Maps to |
|---|---|---|---|---|---|
| Thermionic Fabricator | Molten glass + ingots → **electron tubes** | Modernize | Shipped as the **Sequential Fabricator** (v0.8.3) — one machine unifies this with BuildCraft's Assembly Table (see [`buildcraft.md`](buildcraft.md)). It sequentially crafts the electronics tier (chipsets, valves) | ✅ Done | `automation` / Sequential Fabricator |
| Electron tubes (copper/tin/bronze/iron/gold/diamond/blaze/apatite/lapis/ender/etc.) | Typed components for circuit programming | Modernize | Realized as the **valve** line (15 valves, reworked v0.8.3 with distinct electron-tube textures, bench + Fabricator recipes) and **chipsets** (7). The earlier "cores" idea folded into these — there is no separate cores item | ✅ Done | `core` / valves + chipsets |
| Circuit boards + Soldering | Program machine/farm behavior via tube layouts | TBD | **Unify** circuits with TE-style augments + BC gates into one "programmable behavior" system across the mod. The valves/chipsets are the *components*; the programmable **logic layer** is still deferred (RFC 0001) | — | Phase 1/2 — electronics (RFC) |

## Bees (apiculture) — out of scope

> **Bees are out of scope for Logistics** (decided Jun 2026 — see [RFC 0002](../rfcs/0002-forestry-bees.md)). Modern vanilla already breeds bees by feeding them flowers — a model that diverges sharply from Forestry's princess/drone/queen lifecycle and Mendelian genetics — and the whole genetics axis (bees/trees/butterflies) is deliberately not this mod's job. The rows below are retained as the rationale for that call; they are **not** on the roadmap.

| Feature | What it did | Decision | Modern take / balance notes | Status | Maps to |
|---|---|---|---|---|---|
| Apiary | Single-block bee housing; bees produce combs | Skip | Vanilla already has the beehive/bee nest; genetics axis is out of scope | ❌ | — |
| Bee lifecycle (princess/drone/queen) | Breed queens; offspring; decay | Skip | Fundamentally different from vanilla flower-breeding; genetics — out of scope (separate-mod territory) | ❌ | — |
| Bee genetics / mutations | Mendelian traits + species discovery | Skip | The genetics axis we're explicitly not building | ❌ | — |
| Bee products + Centrifuge | Combs → centrifuge → honey/wax/jelly | Skip | Coupled to bees; out with them (source any needed products another way) | ❌ | — |
| Frames / Alveary / bee tools | Output modifiers; advanced multiblock; analysis | Skip | Out with bees; Alveary would also fight the multiblock stance | ❌ | — |

## Out of scope

| Feature | What it did | Decision | Modern take / balance notes | Status | Maps to |
|---|---|---|---|---|---|
| Butterflies (lepidopterology) | Breed butterflies; cocoons; serum | Skip | Straight skip for now — decorative, low gameplay-per-complexity | ❌ | — |
| Backpacks (forester/miner/digger/etc.) | Portable categorized storage | Skip | Storage is other mods' job | ❌ | — |
| Crates | Compact storage | Skip | Out of scope | ❌ | — |
| Mail / Letters / Trade Stations | In-game postal + auto-trade | Skip | Niche; out of scope | ❌ | — |

> Partly resolved: the electron-tube components (valves, chipsets) ship, crafted at the bench and the Sequential Fabricator. What's still owed is the **circuit-board / programmable-behavior** layer that would *consume* them — deferred, and still the biggest open cross-mod call. See [RFC 0001 — Programmable Behavior](../rfcs/0001-programmable-behavior.md).
> Decided (Jun 2026): the genetics axis — bees, tree-breeding, butterflies — is **out of scope** for Logistics; it would be a separate, dedicated mod. See [RFC 0002 — Forestry Genetics](../rfcs/0002-forestry-bees.md).
