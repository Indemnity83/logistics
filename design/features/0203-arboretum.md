# Arboretum

> **Status:** 🚧 Planned — design settled, wave 1 · **Phase:** 2 — Forestry · **Module:** `logistics-forestry` (`forestry` domain)
> **Source:** [`../mods/forestry.md`](../mods/forestry.md) (Farm (single-block); Tree products) · **Depends on:** [`0201-farm-framework.md`](0201-farm-framework.md), [`0202-fertilizer-and-soils.md`](0202-fertilizer-and-soils.md) (Humus) · **Maps to (roadmap):** Phase 2 — Farms

The tree farm (Forestry's Arboretum / Lumberjack): plants saplings, lays Humus, fertilizes, and fells whole grown trees into its output — logs, plus leaf drops (saplings, sticks, apples) and mangrove roots. Wood only; no tree breeding.

## Problem & goal

Wood is the base of the Sawmill, Kiln and biomass chains, but automated tree farming today means a vanilla contraption. **Goal:** a single block that keeps a 9×9 plot planted and felled, safely — it must never chew through a player's build.

## Requirements

### Functional
- **Germlings:** `#minecraft:saplings`, including mangrove propagule and azalea.
- **Soil:** the dirt family; prefers **Humus**, which it re-lays from its soil slots.
- **Harvest:** whole trees — logs, leaves (→ saplings / sticks / apples), mangrove roots.
- **Water:** none.
- **2×2 saplings:** planted as aligned 2×2 groups from the `logistics:forestry/requires_2x2` tag. The farm does its own 2×2 check before spending fertilizer, because a lone dark oak is a "valid" bonemeal target on 1.21.1 and then silently fails.
- **Recipe:** standard farm recipe with the **copper valve**.

### Balance
- Within the shared 10–20 RF/t anchor ([0201](0201-farm-framework.md)); felling is spread across ticks (K blocks per tick), never one huge burst.

## Design sketch

- A `FarmLogic` implementation (e.g. `ArboretumLogic`) on the shared `FarmBlockEntity`.
- **Tags, not classes:** germlings, soils and `requires_2x2` are tags under `logistics:forestry/…`, so modded saplings work. 26.x-only content (pale oak, poplar) appears as `"required": false` entries, so one JSON file cherry-picks everywhere.
- **`forestry/logic/TreeFeller`** — pure code over a tiny `TreeView` (`state`, `isLoaded`), unit-tested with a HashMap:
  1. **Seed:** the plot, or the first wood within 4 above it.
  2. **Wood:** `#logs` plus `mangrove_roots` through the 26 surrounding blocks, excluding `muddy_mangrove_roots`.
  3. **Canopy:** only from wood or other canopy, through the 6 face neighbours, at most 6 steps. Covers non-`PERSISTENT` `#leaves`, `#wart_blocks`, shroomlight, vines, cocoa and hanging propagules. Never steps from canopy back to wood.
  4. **Never touch:** the `logistics:forestry/never_fell` tag (beehives, creaking heart) and any block entity.
  5. **Bounds:** plot ±8 horizontally, y−3 to y+32.
  6. **Cap:** 1024 blocks. If the cap is hit or there are no natural leaves, it's a player build: skip it.
  7. **Breaking:** top-down, K blocks per tick. Only the base position persists; re-plan after a reload.
- Breaking uses the framework's `getDrops(…, ItemStack.EMPTY)` + `destroyBlock` path; no fake player.

## Scope & non-goals

- **In:** the Arboretum block, logic, 2×2 planting, `TreeFeller`, tags, recipe, textures (`front` `_off` / `_on` on the shared machine shell), GameTests.
- **Out:** tree breeding, specialty woods, resin tapping (separate), an area preview.

## Open questions

- K (blocks broken per tick) and the canopy step limit — tune against large jungle / dark oak trees.

## Done when

- JUnit covers `TreeFeller`: 2×2 trees, mangroves (roots in, muddy roots out), persistent leaves left alone, `never_fell` and block entities skipped, the 1024 cap, and the no-natural-leaves "player build" case.
- A GameTest grows and fells a tree in a 3×3 override plot with real power, hopper-fed saplings and hopper-to-chest output, then replants.

## References

- Breakdown: [`../mods/forestry.md`](../mods/forestry.md) → Farms; Trees (wood via farms only)
- Code precedent: `automation/laserquarry/entity/QuarryBlockBreaker` (drops + `destroyBlock`)
- Related: [0201 Farm Framework](0201-farm-framework.md), [0202 Fertilizer & Soils](0202-fertilizer-and-soils.md), [0206 Mushroom Farm](0206-mushroom-farm.md) (reuses `TreeFeller`)
