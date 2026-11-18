# Gourd Farm

> **Status:** 🚧 In review — tend model ([ADR 0001](../../docs/adr/0001-tend-only-farms.md)) · **Phase:** 2 — Forestry · **Module:** `logistics-forestry` (`forestry` domain)
> **Source:** [`../mods/forestry.md`](../mods/forestry.md) (Farm (single-block)) · **Depends on:** the tend framework (`TendLogic`, `TendFarmComponent`) · **Maps to (roadmap):** Phase 2 — Farms

Harvests the pumpkins and melons that the player's stems grow around it, and only the fruit
(Forestry's Gourmand farm). The player lays out the stems and leaves their fruit spaces clear; the
farm supplies the labour.

## Requirements

- **Reach:** the shared tend Reach, 7×7 around the farm and one level up or down, Basic tier only.
- **Plots:** each pumpkin or melon stem standing on `logistics:forestry/gourd_farm_ground`
  (farmland, dirt, grass) is a Plot, its germling the stem's seeds. A growing stem is taken on
  before its first fruit, so the farm pays for it from the moment it sees it.
- **Ripe:** a stem attached to its own fruit (an attached pumpkin stem pointing at a pumpkin, an
  attached melon stem at a melon).
- **Harvest:** breaks the fruit only. The stem is never broken; vanilla turns it back into a grown
  stem, which grows the next fruit by itself. No replant, so no seed is spent.
- **Replant:** a stem that is broken or uprooted leaves an Empty Plot. Once seeds of that gourd
  are in the inventory, the farm re-tills the Plot's dirt or grass into farmland and plants a new
  stem. A fruit harvest never yields seeds, so the player supplies them.
- **Never:** tills fruit spaces, builds a lane pattern, irrigates, or places water. Irrigation and
  the water tank were removed by ADR 0001.
- **Output:** the shared 15-slot inventory; overflow drops at the stem (the Plot).
- **Costs:** a fruit harvest is `rf_harvest_crop` (100 RF); a replant is `rf_till` + `rf_plant`
  as for the Crop Farm. Upkeep is `rf_upkeep_gourd_farm`, 0.75 RF/t per stem.
- **Recipe:** tin valves, an iron hoe, pumpkin seeds and melon seeds around a machine core, over
  copper gears and a redstone reception coil.

## Decisions

- **The Plot is the stem, not the fruit.** The framework remembers a Plot only for a harvest with a
  germling, and charges upkeep per Plot. Keying the Plot on the stem gives it a germling (the
  seeds) and a ground (farmland), and `ripe(stem)` returns a harvest whose blocks are the fruit
  only. After the harvest the stem still stands on the Plot, so the framework's replant finds the
  spot occupied and does nothing, and `isPlanted` (overridden to accept the stem attached or not)
  keeps the Plot planted. **No framework change was needed**; `TendLogic.Harvest`'s Javadoc now
  documents that a harvest may leave its plot's plant standing.
- **Broken stems are replanted**, the Crop-Farm path: farmland ground, seeds as germling, dirt or
  grass re-tilled first. A trampled stem therefore comes back once the player puts seeds in.
  Forgetting a stem still follows ADR 0001: put something else there, or make the ground something
  other than farmland, dirt or grass.
- **A fruit outside the Reach is harvested when its stem is inside.** The fruit is the stem's
  produce wherever the stem points, the same way the Arboretum fells a whole tree. A fruit inside
  the Reach whose stem is outside is left alone: the farm only tends stems in its Reach.
- **Only the stem's own fruit is broken.** A pumpkin placed beside an unattached stem, or a carved
  pumpkin, is never touched. The gourds are a code-defined pair list (pumpkin, melon) rather than a
  tag, because a stem's fruit and seeds are private to the vanilla block. A modded gourd needs one
  more entry.
- **Upkeep is 0.75 RF/t per stem.** A Crop Farm field is about 48 Plots at 0.25, about 13 RF/t.
  A gourd field has fewer, more valuable Plots: a stem lane between two fruit lanes fits about 16–20
  stems in the Reach once water is in. At 0.75 that is 12–15 RF/t, the same whole-farm target: more
  than one Stirling engine at 10 RF/t peak, and well within one warm Fuel engine at 10–40.
- **The harvest reuses `rf_harvest_crop`** rather than a new cost key: a pumpkin is a crop block,
  and one key keeps the config smaller.
- **The tin valve,** as on the Crop Farm. The Gourd Farm is another field farm at the same tier.

## Done when

- A GameTest harvests pumpkins and melons into a hopper-fed chest on both loaders without ever
  breaking a stem, checked every tick.
- A GameTest spills a full farm's pumpkin at its stem, and another re-tills and replants a
  trampled stem from inventory seeds.
- JUnit: fruit only, no seed spent; the stem stays a planted Plot through the next fruit; the fruit
  just outside the Reach; a stem outside the Reach, a loose pumpkin, and a stem on other ground are
  left alone; upkeep per stem.

## References

- Breakdown: [`../mods/forestry.md`](../mods/forestry.md) → Farms
- Related: [ADR 0001](../../docs/adr/0001-tend-only-farms.md), [0204 Cropland](0204-crop-farm.md)
