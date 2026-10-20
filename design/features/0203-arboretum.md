# Arboretum

> **Status:** ✅ **Shipped** (#1409) — tree farm working a ring around the farm · 🚧 **Planned:** tiered ring (11×11 at Basic around a 7×7 courtyard) with a Chebyshev-spaced, lane-stable planting pattern · **Phase:** 2 — Forestry · **Module:** `logistics-forestry` (`forestry` domain)
> **Source:** [`../mods/forestry.md`](../mods/forestry.md) (Farm (single-block); Tree products) · **Depends on:** [`0201-farm-framework.md`](0201-farm-framework.md), [`0202-fertilizer-and-soils.md`](0202-fertilizer-and-soils.md) (Humus) · **Maps to (roadmap):** Phase 2 — Farms

The tree farm (Forestry's Arboretum / Lumberjack): clears a ring around itself, lays Humus, plants saplings and fells whole grown trees into its output — logs, leaf drops (saplings, sticks, apples), mangrove roots, and the sand its trees leave behind. Wood only; no tree breeding.

## Problem & goal

Wood is the base of the Sawmill, Kiln and biomass chains, but automated tree farming today means a vanilla contraption. **Goal:** a single block that keeps the ring around it planted and felled, safely — it must never chew through a player's build or the 7×7 courtyard the player builds in around it.

## Requirements

### Functional
- **Area:** the farm sits at the centre of the 7×7 courtyard and works the tiered ring from [0201](0201-farm-framework.md): **11×11 at Basic** (2 wide, 72 plots) up to 23×23 at Infernal. It lays Humus level with itself and clears each plot's column from Y+1 up to Y+10.
- **Slots:** saplings 0–3, Humus 4–7 (materials), output 8–16.
- **Germlings:** `#minecraft:saplings`, including mangrove propagule and azalea.
- **Planting pattern** (replaces "every even step of every lane"):
  - Trunk cells, including the 4 cells of a 2×2, are **at least 2 apart in Chebyshev distance**: there is always a gap, even diagonally, so wood from neighbouring trees never merges.
  - Cells are assigned greedily **lane by lane from the inside out**, so the pattern is lane-stable: an upgrade adds trees on the new lanes and never moves existing ones.
  - **At width 2 (Basic) trees go on the outer lane only** — lane 1, every other step, about 20 trees. Lane 0 stays bare Humus, so the courtyard gets no leaf litter or dropped saplings. The canopy overhangs outside the farm by 1–2 blocks. Wider rings keep the lane-0 exclusion and follow the same greedy rule.
  - **2×2 groups** only where the band is at least 3 wide (Industrial and up), or at corners where they fit.
- **Soil:** **Humus only.** Anything else in the soil layer (the farm's own Y), dirt and grass included, is replaced with Humus. Without Humus it neither replaces soil nor plants.
- **Humus → sand:** a grown tree turns the Humus under it to sand, and a felling turns every trunk cell's Humus to sand (all four for a 2×2 tree). The next pass digs the sand into the output and lays fresh Humus. The output never voids: when full, the farm stalls with *Output full*.
- **Harvest:** whole trees — logs, leaves (→ saplings / sticks / apples), mangrove roots.
- **Water:** none.
- **2×2 saplings:** planted as aligned 2×2 groups from the `logistics:forestry/requires_2x2` tag. A group is planted all or none, only once four of the same sapling are on hand; a 2×2 sapling is never planted alone.
- **Recipe:** standard farm recipe with the **copper valve**.

### Balance
- Costs from [0201](0201-farm-framework.md): **25 RF per log**, **5 RF per leaf block**, 50 RF per Humus placed, 25 RF per sapling. An oak (≈65 blocks) costs ≈425 RF; a mega jungle (≈490 blocks) ≈4.3k RF.
- Felling runs on the scheduler's harvest lane, alternating 50/50 with cultivation when both have work, never one huge burst. At Basic an oak takes ≈16 s alone or ≈32 s contended; a mega jungle ≈2 / ≈4 min.

## Design sketch

- A `FarmLogic` implementation (e.g. `ArboretumLogic`) on the shared `FarmBlockEntity`.
- **Tags, not classes:** germlings, soils and `requires_2x2` are tags under `logistics:forestry/…`, so modded saplings work. 26.x-only content (pale oak, poplar) appears as `"required": false` entries, so one JSON file cherry-picks everywhere.
- **`forestry/logic/TreeFeller`** — pure code over a tiny `TreeView` (`state`, `isLoaded`), unit-tested with a HashMap:
  1. **Seed:** the plot, or the first wood within 4 above it.
  2. **Wood:** `#logs` plus `mangrove_roots` through the 26 surrounding blocks, excluding `muddy_mangrove_roots`.
  3. **Canopy:** only from wood or other canopy, through the 6 face neighbours, at most 6 steps. Covers non-`PERSISTENT` `#leaves`, `#wart_blocks`, shroomlight, vines, cocoa and hanging propagules. Never steps from canopy back to wood.
  4. **Never touch:** the `logistics:forestry/never_fell` tag (beehives, creaking heart) and any block entity.
  5. **Bounds:** the farm's outer square plus a 3-block canopy margin, y−3 to y+32; wood must sit inside the outer square. Courtyard columns are `NEVER_TOUCH`.
  6. **Cap:** 1024 blocks. If the cap is hit or there are no natural leaves, it's a player build: skip it.
  7. **Planning and breaking:** `FellPlanner` runs the crawl incrementally on the check budget, then the harvest lane breaks top-down, one block per action, re-validating each target ([0201 → Scheduler](0201-farm-framework.md#scheduler)). Only the base position persists; re-plan after a reload or when the world changes under the plan.
- Breaking uses the framework's `getDrops(…, ItemStack.EMPTY)` + `destroyBlock` path; no fake player.
- **Foreign trees:** a natural tree just outside the outer square with leaves over the ring loses only those leaves (cleared as obstructions); its wood is never planned.

## Scope & non-goals

- **In:** the Arboretum block, logic, the spaced lane-stable pattern, 2×2 planting, `TreeFeller` / `FellPlanner`, tags, recipe, textures (`front` `_off` / `_on` on the shared machine shell), GameTests.
- **Out:** tree breeding, specialty woods, resin tapping (separate).

## Open questions

- The canopy step limit — tune against large jungle / dark oak trees.

## Done when

- **Basic viability (PR 1G):** a new larger empty GameTest structure (about 15×18×15, one per loader) grows real oak saplings with `PlantCompat.fertilize` on every planting cell of a Basic ring for three full fell → replant cycles: every cell regrows, no trunks merge, every felled tree is replanted, output holds logs, saplings and sticks. **Acceptance:** at least 90% of planting cells stay productive over 3 cycles with zero courtyard writes; if not, 1G adjusts the pattern before Phase 2.
- GameTests in the same structure: two neighbouring farm trees with overlapping canopies are both felled and nothing outside the bounds is touched; a natural tree outside the outer square keeps its wood; canopy test blocks placed above the courtyard are never broken; a mixed 2×2 dark oak fells at Industrial.
- JUnit: the spacing rule as a property test for every tier, lane-stable across upgrades.

- JUnit covers `TreeFeller`: 2×2 trees, mangroves (roots in, muddy roots out), persistent leaves left alone, `never_fell` and block entities skipped, the 1024 cap, and the no-natural-leaves "player build" case.
- A GameTest grows and fells a tree in a `setAreaOverride(1, 5)` ring with real power, hopper-fed saplings and Humus and hopper-to-chest output, turns the trunk's Humus to sand, then swaps the sand for Humus and replants.

## References

- Breakdown: [`../mods/forestry.md`](../mods/forestry.md) → Farms; Trees (wood via farms only)
- Code precedent: `automation/laserquarry/entity/QuarryBlockBreaker` (drops + `destroyBlock`)
- Related: [0201 Farm Framework](0201-farm-framework.md), [0202 Fertilizer & Soils](0202-fertilizer-and-soils.md), [0206 Mushroom Farm](0206-mushroom-farm.md) (reuses `TreeFeller`)
