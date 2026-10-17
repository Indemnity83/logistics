# Arboretum

> **Status:** ✅ **Shipped** (#1409) — tree farm working a 13×13 ring around a free 9×9 · **Phase:** 2 — Forestry · **Module:** `logistics-forestry` (`forestry` domain)
> **Source:** [`../mods/forestry.md`](../mods/forestry.md) (Farm (single-block); Tree products) · **Depends on:** [`0201-farm-framework.md`](0201-farm-framework.md), [`0202-fertilizer-and-soils.md`](0202-fertilizer-and-soils.md) (Humus) · **Maps to (roadmap):** Phase 2 — Farms

The tree farm (Forestry's Arboretum / Lumberjack): clears a ring around itself, lays Humus, plants saplings and fells whole grown trees into its output — logs, leaf drops (saplings, sticks, apples), mangrove roots, and the sand its trees leave behind. Wood only; no tree breeding.

## Problem & goal

Wood is the base of the Sawmill, Kiln and biomass chains, but automated tree farming today means a vanilla contraption. **Goal:** a single block that keeps the ring around it planted and felled, safely — it must never chew through a player's build or the 9×9 the player builds in around it.

## Requirements

### Functional
- **Area:** the farm sits at the centre of a free **9×9** and works the 2-wide ring out to **13×13** (88 plots). It lays Humus level with itself and clears each plot's column from Y+1 up to Y+10 ([0201](0201-farm-framework.md)).
- **Slots:** saplings 0–3, Humus 4–7, output 8–16.
- **Germlings:** `#minecraft:saplings`, including mangrove propagule and azalea. Single saplings go on every even step of each lane; 2×2 saplings go in aligned groups across both lanes, a group at every corner and two-cell groups along each edge.
- **Soil:** **Humus only.** Anything else in the soil layer (the farm's own Y), dirt and grass included, is replaced with Humus. Without Humus it neither replaces soil nor plants.
- **Humus → sand:** a grown tree turns the Humus under it to sand, and a felling turns every trunk cell's Humus to sand (all four for a 2×2 tree). The next pass digs the sand into the output and lays fresh Humus. The output never voids: when full, the farm stalls with *Output full*.
- **Harvest:** whole trees — logs, leaves (→ saplings / sticks / apples), mangrove roots.
- **Water:** none.
- **2×2 saplings:** planted as aligned 2×2 groups from the `logistics:forestry/requires_2x2` tag. A group is planted all or none, only once four of the same sapling are on hand; a 2×2 sapling is never planted alone.
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
- A GameTest grows and fells a tree in a `setAreaOverride(1, 5)` ring with real power, hopper-fed saplings and Humus and hopper-to-chest output, turns the trunk's Humus to sand, then swaps the sand for Humus and replants.

## References

- Breakdown: [`../mods/forestry.md`](../mods/forestry.md) → Farms; Trees (wood via farms only)
- Code precedent: `automation/laserquarry/entity/QuarryBlockBreaker` (drops + `destroyBlock`)
- Related: [0201 Farm Framework](0201-farm-framework.md), [0202 Fertilizer & Soils](0202-fertilizer-and-soils.md), [0206 Mushroom Farm](0206-mushroom-farm.md) (reuses `TreeFeller`)
