# RFC 0003: Railcraft Multiblocks & the Steam Chain

> **Status:** 🟡 Open — needs a Discussion before Phase 3 scheduling · **Scope:** Phase 3 (Transport) · **Decides:** maintainer + community
> **Affects:** [`../mods/railcraft.md`](../mods/railcraft.md) § Steam/steel/bulk (Coke Oven, Blast Furnace, Boiler, Tank) + the steam-power TODO · **Blocks:** scheduling the Phase 3 steam/steel chain — not Phase 3 tracks/carts/signals

The single biggest design tension in the Railcraft module: its signature blocks were **multiblocks**, and [`../principles.md`](../principles.md) defaults to **avoiding monolithic multiblocks**. This RFC decides the form of each, plus whether steam is a real power tier.

## Context

Railcraft's processing/power blocks were structures: **Coke Oven** (coal → coke + creosote), **Blast Furnace** (iron + coke → steel), **Steam Boiler** (fuel + water → steam, up to 36 blocks), and **Iron/Steel Tank** (bulk fluid storage). The principles stance: prefer a **single block + optional markers/tileable**; allow a multiblock only when *scale itself is the feature*, and even then prefer **modular/tileable** over a rigid required schematic.

[`../mods/railcraft.md`](../mods/railcraft.md) already leans single-block for the Blast Furnace ("single-block preferred over the 34-block structure") and flags the bulk **Tank** as the one place where "scale is the feature" might justify an exception. The steam-power scope is also open: is steam a real `power` tier (boiler → turbine → RF, unified into the engine line) or simplified away?

This is **Phase 3 / post-1.0**, so it does *not* gate 1.0 — but it defines the whole transport module's feel and should be settled before Phase 3 is scheduled.

## The decision to make

**For each of {Coke Oven, Blast Furnace (Steel), Steam Boiler, Bulk Tank}: single block, tileable/modular, or true multiblock? And: is steam a real power tier, or simplified out?**

## Recommended disposition (the proposal to debate)

| Block | Classic form | Proposed form | Rationale |
|---|---|---|---|
| **Coke Oven** | Multiblock | **Single block** (or tileable) | Produces coke + creosote; scale isn't the point — hold the no-multiblock line. |
| **Blast Furnace → Steel** | 34-block multiblock | **Single-block machine** | Breakdown already prefers this; steel is the module's key material and shouldn't need a schematic. Pairs with the Alloy Smelter pattern. |
| **Steam Boiler** | Up to 36-block multiblock | **Single-block engine-line tier *or* cut** | Tied to the steam-power decision below. If steam stays, a single-block boiler tier; if not, drop it. |
| **Bulk Tank** | Multiblock | **Tileable tank blocks** (auto-merge into one logical tank) | The one genuine "scale is the feature" case — but tileable, not a rigid schematic. The valid exception. |

**Steam-power scope:** two sub-options —
- **(i) Steam is a real tier** — boiler → turbine → RF, unified into the engine line (consistent with the "unify everything into one engine line" thread in [`../mods/buildcraft.md`](../mods/buildcraft.md)/[`../mods/thermal-expansion.md`](../mods/thermal-expansion.md)).
- **(ii) Simplified** — ship coke/creosote/steel and treated-wood tracks **without** a steam power tier; carts run on the existing RF/engine power.

**Leaning:** hold single-block for Coke Oven + Blast Furnace; allow **tileable** Bulk Tanks as the lone scale-is-the-feature exception; treat steam as **(ii) simplified for the transport module's v1**, revisiting a steam engine tier only if it earns its place.

## Sub-questions still open

- Does "tileable tank" need a **controller/valve block**, or do adjacent tank blocks auto-form a logical multiblock with shared contents?
- **Progression coupling:** do tracks/steel *require* the coke→creosote→treated-wood chain, or can the rail tier be reached without the steam chain? (Affects whether cutting steam strands content.)
- If steam is cut (ii), is there any content that *only* made sense with a boiler (e.g. a turbine block)? Confirm nothing is orphaned.
- **World Anchor / chunkloading** is a *separate* sensitive TBD (server-perf) — note it here but resolve in its own Discussion, not this one.

## How we'll decide

Discussion, weighing **tedium vs. the satisfaction of scale** per block, against the multiblock stance. Record the per-block outcome back into the `railcraft.md` rows (flip Modernize/TBD → the concrete form chosen). Settle before Phase 3 scheduling.

## References

- Breakdown: [`../mods/railcraft.md`](../mods/railcraft.md) § Steam/steel/bulk + the multiblock & steam-power TODOs
- Principles: [`../principles.md`](../principles.md) § The multiblock stance
- Roadmap: [`../roadmap.md`](../roadmap.md) → Phase 3 → Steam & steel chain (notes the TBDs)
- Related: engine-line unification thread in [`../mods/buildcraft.md`](../mods/buildcraft.md) / [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md); Steel pairs with [`../features/0104-alloy-smelter.md`](../features/0104-alloy-smelter.md)
