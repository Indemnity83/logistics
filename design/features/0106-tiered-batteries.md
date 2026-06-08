# Tiered Energy-Storage Line (Batteries)

> **Status:** 🚧 Planned · **Phase:** 1 — Automation core · **Module:** `logistics-automation` (`power` domain)
> **Source:** [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md) (Energy Cells: Leadstone→Resonant) · **Depends on:** nothing (extends existing Battery)
> **Maps to (roadmap):** Phase 1 — Battery → tiered energy-storage line

Expand the single existing Battery into a tiered line (≈Leadstone → Resonant) with growing capacity and configurable I/O rates. The codebase already has the abstraction (`AbstractBatteryBlockEntity`) and a tiering precedent (the cable tiers), so this is mostly content + an I/O-config UX.

## Problem & goal

There's one Battery (100,000 RF, 1,000 RF/t). Energy storage is currently flat — no progression, no throttle control. The classic Energy Cell line gave storage *tiers* and, crucially, **configurable input/output rates** so a cell could act as a buffer, a limiter, or a burst source.

**Goal:** a 3-tier battery line with increasing capacity/throughput and per-block (ideally per-side) configurable I/O, reusing the existing battery base and the cable-tier pattern.

## Requirements

### Functional
- **Three tiers** (names TBD; e.g. Basic / Reinforced / Resonant) with increasing capacity and max I/O. The current Battery becomes the mid or base tier.
- Each tier: distinct block + item, shared `BlockEntityType`, capacity + max-I/O set per tier (cable-tier style).
- **Configurable I/O rate** — the player can set max input and max output (independently), at least per-block; per-side is the stretch goal. Persisted, and shown in a small GUI or via wrench cycling.
- Charge level visible in-world (the existing `CHARGE` block-state property, 0–10) and on the item (the existing durability-bar render in `BatteryBlockItem`).
- Energy persists across break/place via the existing `block_entity_data` component path.
- Interops with both the push side (engines/batteries → machines) and the network pull side (`ILogisticsNetwork.consumeEnergy`) exactly as the current Battery does.

### Balance
- Capacity/throughput anchored to the current Battery (100k cap, **1k RF/t** per-side I/O) as the **middle** of the line; e.g. base ≈ 50k / 0.5k, top ≈ 1M / 5k. **Note:** battery I/O is the *direct per-face* transfer limit and already exceeds cable throughput — cables are a separate, much lower network bottleneck (`CableTier`: copper 30 / gold 60 / **ender 120 RF/t**). So don't size batteries "to saturate a cable"; through a cable network the cable is always the smaller pipe. High battery I/O only matters for direct battery↔machine adjacency and multi-connection draw.
- Higher tiers gated behind alloys/components from the [`0104-alloy-smelter.md`](0104-alloy-smelter.md) tier so storage progression couples to the materials curve.
- I/O config can't exceed the tier's hardware max — it throttles down, never up.

## Design sketch

The base already supports tiering via constructor parameters — verified: `AbstractBatteryBlockEntity` takes `(type, pos, state, capacity, maxInsert, maxExtract)` and `BatteryBlockEntity` passes constants. Mirror the **cable tier** approach (`CableTier` enum + per-tier `Block` instances, shared BE type).

```text
common/src/main/java/com/logistics/power/block/
├── BatteryTier.java          # enum {BASIC, REINFORCED, RESONANT} → capacity(), maxIo()
├── BatteryBlock.java         # holds a BatteryTier field (like CableBlock holds CableTier)
└── entity/BatteryBlockEntity.java  # reads tier from block; passes tier.capacity()/maxIo() to super
```

- Register three blocks in `LogisticsPower.BLOCK` (`registerBlockWithItem`), one shared `BlockEntityType` covering all three (`registerBlockEntity(type, factory, BASIC, REINFORCED, RESONANT)`) — exactly the cable registration shape.
- **I/O configuration storage:** two options —
  - **(A) Block-state IntegerProperties** `INPUT_RATE` / `OUTPUT_RATE` cycled by wrench. Simple, no GUI, but coarse (few discrete steps) and bloats blockstates.
  - **(B) Data component / BE NBT** holding `maxInput`/`maxOutput` (and per-side maps for the stretch), edited in a small GUI. Finer control, follows the `PipeDataComponents` precedent, copies onto the dropped item.
  - **Lean: (B)** for real I/O control, falling back to wrench-cycling presets if a GUI is too much for v1.
- The `EnergyComponent` already takes max-insert/max-extract; the configured values feed a wrapper so `insert/extract` clamp to the lower of (tier max, configured).

## Scope & non-goals

- **In:** three tiers, capacity/throughput per tier, configurable I/O (per-block min; per-side stretch), in-world + item charge display.
- **Out:** wireless/cross-dim transfer (Tesseract — out of scope per TE breakdown), energy-cell "frames" cosmetic variants, redstone-controlled output modes (could be a later augment), per-side input/output *enable* toggles if per-side rate config already covers it.

## Open questions

- **I/O config storage: block-state vs data-component+GUI** (above). Drives whether this ships a screen.
- **Per-side vs whole-block I/O** for v1 — per-side is the classic Energy Cell behavior but more UX/render work. **Lean: whole-block in v1, per-side as a fast-follow.**
- Tier names + exact capacity/throughput numbers (pick on their own merits — *not* "to saturate a cable," since cables top out at 120 RF/t while even the base battery does more).
- Does the **existing** Battery become BASIC or REINFORCED? (Affects whether existing worlds' batteries change stats — prefer mapping it so saved batteries keep their current 100k/1k.)

## Done when

- Three battery tiers place, store, and transfer energy with distinct capacity/throughput on both loaders.
- I/O rates are configurable and clamp correctly (never exceed tier max), persisted across save/load and break/place.
- Charge shows in-world and on the item for all tiers.
- Battery I/O rates are independent of cable throughput; a battery feeding through a cable network is correctly limited by the cable (≤120 RF/t on Ender), while direct adjacency uses the battery's full per-face rate.

## References

- Roadmap: [`../roadmap.md`](../roadmap.md) → Phase 1 → Battery (tiers); [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md) → "Energy Cells" row
- Code: `core/lib/power/AbstractBatteryBlockEntity` (tier-ready constructor), `power/block/{BatteryBlock,BatteryBlockItem}`, `power/block/entity/BatteryBlockEntity`; tier precedent `power/cable/{CableTier,CableBlock,CableBlockEntity}`; data-component precedent `pipe/data/PipeDataComponents`; registration in `LogisticsPower.java`
- Related: [`0104-alloy-smelter.md`](0104-alloy-smelter.md) (materials gate), [`0105-machine-upgrades.md`](0105-machine-upgrades.md) (creates the demand for bigger storage)
