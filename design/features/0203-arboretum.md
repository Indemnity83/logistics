# Arboretum

> **Status:** ✅ **Shipped** (#1409) — tree farm working the ring around a free 7×7, 11×11 at Basic · **Phase:** 2 — Forestry · **Module:** `logistics-forestry` (`forestry` domain)
> **Source:** [`../mods/forestry.md`](../mods/forestry.md) (Farm (single-block); Tree products) · **Depends on:** [`0201-farm-framework.md`](0201-farm-framework.md), [`0202-fertilizer-and-soils.md`](0202-fertilizer-and-soils.md) (Humus) · **Maps to (roadmap):** Phase 2 — Farms

The tree farm (Forestry's Arboretum / Lumberjack): clears a ring around itself, lays Humus, plants saplings and fells whole grown trees into its output — logs, leaf drops (saplings, sticks, apples), mangrove roots, and the sand its trees leave behind. Wood only; no tree breeding.

## Problem & goal

Wood is the base of the Sawmill, Kiln and biomass chains, but automated tree farming today means a vanilla contraption. **Goal:** a single block that keeps the ring around it planted and felled, safely — it must never chew through a player's build or the 7×7 courtyard the player builds in around it.

## Requirements

### Functional
- **Area:** the farm sits at the centre of a free **7×7** courtyard and works the ring around it out to its tier's size: **11×11** at Basic, 15×15, 19×19 and 23×23 above ([0201](0201-farm-framework.md)). Lanes are numbered from the courtyard out. It lays Humus level with itself and clears each plot's column from Y+1 up to Y+10.
- **Slots:** saplings 0–3, Humus 4–7 (materials), output 8–16.
- **Germlings:** `#minecraft:saplings`, including mangrove propagule and azalea, but not the `logistics:forestry/requires_2x2` ones (dark oak, pale oak), which the germling slots refuse.
- **Planting pattern:** one sapling per spot, and any two spots at least 2 apart in Chebyshev distance, so there is always a block between trunks, diagonals included. Spots are chosen greedily from lane 1 outward, walking each lane in step order; a plot becomes a spot iff it keeps that spacing from every spot already chosen, measured on real positions, so the ring's corners are handled too. That works out as every other step of every odd lane: **20 trees at Basic**, 48, 84 and 128 at the higher tiers.
- **Lane 0 stays bare:** the lane beside the courtyard is never planted at any tier. It still gets Humus, and its column is still cleared and felled.
- **Upgrades move no tree:** a lane's spots depend only on the lanes inside it, and a higher tier only adds outer lanes, so each tier's spots include every spot of the tier below.
- **Canopy:** an oak's canopy reaches 2 blocks from its trunk, so at Basic it overhangs the outside of the farm by 1–2 blocks, and the lane-1 trees' lowest leaves reach the courtyard's outermost row. Felling a tree takes the leaves whose nearest log is that tree's, as vanilla judges a leaf's distance (face steps through leaves to any log), ties going with the tree felled first; a leaf nearer another log is left to it and never crawled through. So of two trees whose canopies overlap, each loses only its own leaves and the other is still felled as a tree, and a natural tree beside the ring keeps every leaf nearest its own trunk. The distances are worked out from the logs found while planning, not read from the leaves' `distance`, which vanilla updates only ticks after a nearby block changes.
- **Courtyard exception:** the farm breaks a felled tree's own natural leaves that hang into the courtyard, so they never decay into it. Nothing else there is ever touched: no wood, no player-placed leaves, no other block, no placement.
- **Trees off the spots:** from a plot it does not plant, the farm fells only a tree standing on that plot. A plan seeded at a branch hanging over a plot would miss the trunk below and leave it bare.
- **Soil:** **Humus only.** Anything else in the soil layer (the farm's own Y), dirt and grass included, is replaced with Humus. Without Humus it neither replaces soil nor plants.
- **Humus → sand:** a grown tree turns the Humus under it to sand, and a felling turns every trunk cell's Humus to sand (all four for a 2×2 tree). The next pass digs the sand into the output and lays fresh Humus. The output never voids: when full, the farm stalls with *Output full*.
- **Harvest:** whole trees — logs, leaves (→ saplings / sticks / apples), mangrove roots.
- **Water:** none.
- **No 2×2 trees for now:** 2×2 saplings are not accepted and nothing plants a 2×2 group. The `logistics:forestry/requires_2x2` tag stays for later. A 2×2 tree that grows in the ring anyway, naturally or by a player, is felled like any other.
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
  3. **Canopy:** only from wood or other canopy, through the 6 face neighbours, at most 6 steps. Covers non-`PERSISTENT` `#leaves`, `#wart_blocks`, shroomlight, vines, cocoa and hanging propagules. Never steps from canopy back to wood. The canopy is explored twice as far, any other wood met is noted, and each canopy block goes to its nearest wood; only this tree's are taken. In the keep-out columns (the courtyard) only natural leaves may be taken.
  4. **Never touch:** the `logistics:forestry/never_fell` tag (beehives, creaking heart) and any block entity.
  5. **Bounds:** the farm's outer square plus a 3-block canopy margin, y−3 to y+32; wood must sit inside the outer square. Courtyard columns are `NEVER_TOUCH`.
  6. **Cap:** 1024 blocks. If the cap is hit or there are no natural leaves, it's a player build: skip it.
  7. **Planning and breaking:** `FellPlanner` runs the crawl incrementally on the check budget, then the harvest lane breaks top-down, one block per action, re-validating each target ([0201 → Scheduler](0201-farm-framework.md#scheduler)). Only the base position persists; re-plan after a reload or when the world changes under the plan.
- Breaking uses the framework's `getDrops(…, ItemStack.EMPTY)` + `destroyBlock` path; no fake player.
- **Foreign trees:** a natural tree just outside the outer square with leaves over the ring loses only those leaves (cleared as obstructions); its wood is never planned.

## Scope & non-goals

- **In:** the Arboretum block, logic, the spaced planting pattern, `TreeFeller` / `FellPlanner`, tags, recipe, textures (`front` `_off` / `_on` on the shared machine shell), GameTests.
- **Out:** tree breeding, specialty woods, resin tapping (separate), planting 2×2 trees.

## Open questions

- The canopy step limit — tune against large jungle trees.
- **Fancy oaks reaching into the courtyard:** about one oak in ten grows fancy, and its branches can reach over lane 0 into the courtyard. The farm fells the rest of the tree but never wood in the courtyard, so the branch logs stay there with the leaves nearest them, and those leaves can later decay into litter. In the Basic GameTest this happened in one of three cycles (10 logs, about 44 leaves).

## Done when

- **Basic viability (PR 1G):** a new larger empty GameTest structure (about 15×18×15, one per loader) grows real oak saplings with `PlantCompat.fertilize` on every planting cell of a Basic ring for three full fell → replant cycles: every cell regrows, no trunks merge, every felled tree is replanted, output holds logs, saplings and sticks. **Acceptance:** at least 90% of planting cells stay productive over 3 cycles with zero courtyard writes; if not, 1G adjusts the pattern before Phase 2.
- GameTests in the same structure: two neighbouring farm trees with overlapping canopies are both felled and nothing outside the bounds is touched; a natural tree outside the outer square keeps its wood; canopy test blocks placed above the courtyard are never broken; a mixed 2×2 dark oak fells at Industrial.
- JUnit: the spacing rule as a property test for every tier, lane-stable across upgrades.

- JUnit covers `TreeFeller`: 2×2 trees, mangroves (roots in, muddy roots out), persistent leaves left alone, `never_fell` and block entities skipped, the 1024 cap, and the no-natural-leaves "player build" case.
- A GameTest grows and fells a tree in a `setAreaOverride(1, 5)` ring with real power, hopper-fed saplings and Humus and hopper-to-chest output, turns the trunk's Humus to sand, then swaps the sand for Humus and replants.
- In the 15×18×15 `logistics_gametest:arboretum_field` structure, a real Basic farm keeps all 20 spots productive over three fell-and-replant cycles without touching its courtyard, fells two neighbouring trees whose canopies overlap, and never plans or breaks the wood of a natural tree just outside its ring.

## References

- Breakdown: [`../mods/forestry.md`](../mods/forestry.md) → Farms; Trees (wood via farms only)
- Code precedent: `automation/laserquarry/entity/QuarryBlockBreaker` (drops + `destroyBlock`)
- Related: [0201 Farm Framework](0201-farm-framework.md), [0202 Fertilizer & Soils](0202-fertilizer-and-soils.md), [0206 Mushroom Farm](0206-mushroom-farm.md) (reuses `TreeFeller`)
