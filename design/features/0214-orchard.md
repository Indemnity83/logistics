# Orchard

> **Status:** 🚧 In progress — Phase 2 of the farm plan · **Phase:** 2 — Forestry · **Module:** `logistics-forestry` (`forestry` domain)
> **Source:** [`../mods/forestry.md`](../mods/forestry.md) (Farm (single-block)) · **Depends on:** [`0201-farm-framework.md`](0201-farm-framework.md), [ADR 0001](../../docs/adr/0001-tend-only-farms.md) · **Maps to (roadmap):** Phase 2 — Farms

Picks apples off the living trees around it. It is the first farm that never breaks a block: no harvest, no replant, no tilling. The trees are the player's, and the Orchard only takes the fruit. The apples are meant to feed a future biofuel chain.

## Requirements

- **Scope:** apples only. Sweet berries and glow berries were deliberately left out for now; the [Berry Farm](0209-berry-farm.md) and [Cave Vine Farm](0211-cave-vine-farm.md) cover them.
- **Reach:** the shared 7×7 footprint, but from one block below the farm to **seven above it** (Y-1..Y+7), so it sees the trees it picks from. It is the only farm so far that reaches higher than one block.
- **Plot = one tree:**
  - A tree's **trunk base** is the lowest log of its trunk. It must lie in the 7×7 at Y-1..Y+1.
  - The tree needs at least one qualifying leaf attached.
  - All of the tree's attached canopy counts, even leaves above or beyond the Reach.
  - Canopy of a tree rooted outside the Reach is ignored.
  - Leaves belong to the nearest log, by vanilla's rule, as the Arboretum already reads them.
  - Logs side by side (a dark oak trunk) are one tree.
- **Qualifying leaves:**
  - natural (`persistent=false`);
  - still attached (`distance` < 7);
  - in the block tag `#logistics:forestry/orchard/apple_leaves`, which holds `minecraft:oak_leaves` and `minecraft:dark_oak_leaves`. Vanilla has no such tag; only those two leaves' loot tables roll apples.
  - Any tree counts, whether it stands on Humus, grass, dirt or anything else.
- **Pure picker:** the Orchard never plants, fells or repairs, and its trees need no germling. A tree that is gone (felled, burnt, or with no qualifying leaves left) is forgotten as a Plot.
- **Ripening:**
  - While the farm runs (powered, enabled and its upkeep paid), each tracked tree gets a simulated random tick each game tick with probability `randomTickSpeed / 4096`.
  - The gamerule is read live; at 0 nothing ripens.
  - Each simulated random tick rolls the fruit's `chance`. A success picks one apple at once. There is no stored ripe state.
- **Pick:** costs `rf_harvest_crop` (100 RF). The apple goes into the shared 15-slot inventory. When that is full, it spills at the trunk base and the farm keeps working ([ADR 0001](../../docs/adr/0001-tend-only-farms.md)). A few leaf particles rustle at one of the tree's apple leaves.
- **Status:** Jade and the farm screen's Plots line show the number of trees tracked.
- **Recipe:** the standard farm recipe (valve, tool, two germlings, machine core, copper gears, redstone reception coil) with the **iron valve**, **shears** and two **apples**.

### Fruit definitions are data

One file per fruit, at `data/<namespace>/logistics/orchard/fruit/<name>.json`, loaded on every resource reload:

```json
{"leaves": "#logistics:forestry/orchard/apple_leaves", "fruit": "minecraft:apple", "chance": 0.02}
```

- `leaves` is a block tag (`#namespace:path`) or one block id.
- `chance` is the chance, in (0, 1], that one simulated random tick picks one `fruit`. The file carries no rate; the rate comes from random ticks.
- If one tree's leaves match several fruits, the first in a stable load order wins: by definition id, namespace then path.
- A file that fails to parse is logged and skipped.
- We ship `data/logistics/logistics/orchard/fruit/apple.json`.

## Balance

- **Where 0.02 comes from:** the Orchard should match a player who grows, fells and lets oaks decay by hand.
  - A sapling needs 2 growth stages at 1/7 per random tick, so about 14 random ticks per cycle.
  - An average small oak has about 56 leaves × 0.5% apple chance ≈ 0.28 apples per cycle.
  - 0.28 / 14 = **0.02 per random tick**.
  - At the default `randomTickSpeed` of 3, that is 3/4096 × 0.02 × 72 000 ticks ≈ **1.05 apples per tree per hour**.
- **Upkeep:** `rf_upkeep_orchard`, **1.5 RF/t per tree**, the same as the Arboretum per Plot.
  - A full Orchard of 6–9 trees costs about 9–13 RF/t, near the one-engine target of the other farms.
  - A pick (100 RF about once an hour per tree) adds next to nothing; upkeep sets the running cost.

## Design sketch

- `OrchardTendLogic` (a `TendLogic`) on the shared `FarmBlockEntity`, driven by `TendFarmComponent`; `OrchardBlock` registers through `LogisticsForestry.BLOCK.registerFarm`.
- **Framework additions**, kept apart from the harvest path so the Crop Farm and Arboretum are untouched:
  - `TendLogic#reachAbove()` (default 1): a per-logic vertical Reach. `TendFarmComponent` builds its scan offsets from it.
  - The picking seam: `TendLogic#picks()`, `#pickPlot(world, pos)` (which Plot a log belongs to) and `#bearing(world, plot)` (what a Plot bears now, as a `TendLogic.Bearing`: produce, chance, cost and the leaves it comes from).
  - `PickPlots`: the component's Picking Plots, saved as a `PickPlots` long array. They pay upkeep and count in `plotsTotal()`, but never go through the replant path.
  - `TendFarmComponent#ripen` gives each Picking Plot its random tick after the farm's actions, and `#pick` charges, stores, spills and rustles.
  - `FarmWorldAccess#randomTickSpeed()` and `#rustle(pos)`. `LevelFarmWorld` reads the gamerule through `GameRuleCompat`.
  - Test seam: `setPickOverride(chancePerTick)`, the counterpart of `setRateOverride`.
- **Trees** are read with `FarmWorld#fellPlan` (`TreeFeller`): wood within two columns of the trunk base and no lower, canopy within the feller's margin and limits, by its nearest-wood rule.
- **Fruit:** `OrchardFruit` (one definition) and `OrchardFruits` (the loaded list), registered on the common `ResourceReloadRegistrar` SPI, which has Fabric and NeoForge implementations.
- **Body:** the glass case with the mini tree on a grass bed and red apples in the foliage ("orchard_tree", alternative A of the farm-body concepts), painted from `orchard_body.png`.

## Scope & non-goals

- **In:** the farm block, its tend logic, the picking seam, fruit definitions, tags, recipe, model, upkeep and tests.
- **Out:** sweet berries and glow berries. Planting, felling or repairing trees. Higher tiers.
- **No coordination with the Arboretum.** The Arboretum fells every grown tree on Humus in its Reach. Keep Orchard trees off Humus; the JEI page says so.

## Decisions

- **Pure picker:** the Orchard owns no blocks, so it needs no germling, no claim on the ground and no replant.
- **Random ticks, not a ripe state:** a pick is a roll per simulated random tick, so the rate follows the gamerule the way a player's own oaks would, with nothing stored per tree.
- **Fruit as data:** a pack can add fruit for modded leaves without code.
- **Valve:** the iron valve. Tin, copper, bronze and blazing are taken by the other farms; apples are early game, and iron is the next step.
- **Recipe tool:** shears, the picking tool; the germlings are two apples.

## Done when

- A GameTest grows an oak on grass in the Reach, powers the Orchard, and a hopper under the farm pulls picked apples into a chest; not one log or leaf is broken, on both loaders.
- A GameTest shows an unpowered Orchard picks nothing, on both loaders.
- JUnit: trees are found by their trunk base in the Reach's lowest levels; canopy beyond the Reach counts and canopy of a tree rooted outside it does not; placed or detached leaves are ignored; a gone tree is forgotten; the roll follows `randomTickSpeed / 4096` then the fruit's chance and does nothing at 0; each pick charges `rf_harvest_crop`; a full inventory spills at the trunk base; fruit definitions parse and load in a stable order.

## References

- Breakdown: [`../mods/forestry.md`](../mods/forestry.md) → Farms
- Related: [0201 Farm Framework](0201-farm-framework.md), [0203 Arboretum](0203-arboretum.md), [0209 Berry Farm](0209-berry-farm.md), [ADR 0001](../../docs/adr/0001-tend-only-farms.md)
