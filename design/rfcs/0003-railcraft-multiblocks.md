# RFC 0003: Railcraft Multiblocks & the Steam Chain

> **Status:** 🟡 Open (multiblocks) — but the **steam-power sub-question is now largely settled by shipped reality:** a first-party single-block **Steam Engine** shipped in v0.8.4 (boiler/pressure/turbine model in `power/engine/steam`), i.e. option **(i)** below, *not* the Create-compat leaning. What remains open is the multiblock form of the Coke Oven / Blast Furnace / Bulk Tank. · **Scope:** Phase 3 (Transport) · **Decides:** maintainer + community
> **Affects:** [`../mods/railcraft.md`](../mods/railcraft.md) § Steam/steel/bulk (Coke Oven, Blast Furnace, Boiler, Tank) · **Blocks:** scheduling the Phase 3 steam/steel chain — not Phase 3 tracks/carts/signals

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
| **Steam Boiler** | Up to 36-block multiblock | **Create compat, single-block tier, *or* cut** | Tied to the steam-power decision below. Leaning: lean on **Create** for steam rather than build our own boiler; otherwise a single-block boiler tier, or drop it. |
| **Bulk Tank** | Multiblock | **Tileable tank blocks** (auto-merge into one logical tank) | The one genuine "scale is the feature" case — but tileable, not a rigid schematic. The valid exception. |

**Steam-power scope:** *(largely resolved — see below)* three sub-options —
- **(i) Steam is a real tier** ✅ **shipped** — a single-block **Steam Engine** (v0.8.4) with a boiler/pressure/turbine model, unified into the engine line (consistent with the "unify everything into one engine line" thread in [`../mods/buildcraft.md`](../mods/buildcraft.md)/[`../mods/thermal-expansion.md`](../mods/thermal-expansion.md)). This preempted the Create-compat leaning: Logistics has its own steam power tier now. *(A Create rotational-power **compat** layer could still be added on top later, but it's no longer the primary plan.)*
- **(ii) Simplified** — ship coke/creosote/steel and treated-wood tracks **without** a steam power tier; carts run on the existing RF/engine power.
- **(iii) Create compat instead of our own steam** *(maintainer leaning, Jun 2026)* — don't reimplement a steam/boiler subsystem at all. **Create already nails steam**, and its multiblocks read intuitively. Instead, build a thin **compatibility layer**: accept Create steam/rotational power to drive a Logistics engine (or convert Create rotation → the Logistics reciprocal), so players who run Create get the satisfying steam progression there while Logistics avoids the multiblock + steam-design burden. Steam becomes an *optional, integration-gated* tier rather than core content.

**Leaning:** hold single-block for Coke Oven + Blast Furnace; allow **tileable** Bulk Tanks as the lone scale-is-the-feature exception. **Steam power is now settled — option (i):** a first-party single-block Steam Engine shipped (v0.8.4), so Logistics has its own steam tier and (ii)/(iii) are no longer live paths (a Create rotational-power compat layer could still be a future add-on, but it is not the plan). The remaining open choices are the **multiblock forms** of the Coke Oven, Blast Furnace, and Bulk Tank.

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
- Roadmap: [`../delivery-plan.md`](../delivery-plan.md) → Phase 3 → Steam & steel chain (notes the TBDs)
- Related: engine-line unification thread in [`../mods/buildcraft.md`](../mods/buildcraft.md) / [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md); Steel pairs with [`../features/0105-alloy-smelter.md`](../features/0105-alloy-smelter.md)
