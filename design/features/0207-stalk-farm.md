# Stalk Farm *(name TBD)*

> **Status:** 🚧 Planned — wave 2 · **Phase:** 2 — Forestry · **Module:** `logistics-forestry` (`forestry` domain)
> **Source:** [`../mods/forestry.md`](../mods/forestry.md) (Farm (single-block)) · **Depends on:** [`0201-farm-framework.md`](0201-farm-framework.md) · **Maps to (roadmap):** Phase 2 — Farms

> **Area:** follows the centred-ring model from [0201](0201-farm-framework.md): the farm sits at the centre of a free 9×9 and works (and clears) a ring of plots around it; slots are germlings 0–3, soil 4–7, output 8–16. The ring size is settled when this farm is built.

Harvests tall-growing plants — sugar cane, cactus, bamboo — leaving the base block to regrow (Forestry's succulent / reeds farms).

## Requirements

- **Germlings:** sugar cane, cactus, bamboo.
- **Soil:** sand / dirt — keeps it, does not make it.
- **Harvest:** everything above the base block, top-down; never the base.
- **Water:** none.
- **Recipe:** standard farm recipe; proposed valve: **iron**.

## Design sketch

- A `FarmLogic` on the shared `FarmBlockEntity`, breaking top-down through the framework's `getDrops` + `destroyBlock` path.
- The cactus flower exists only on 26.x; handle it harmlessly so the same logic cherry-picks to every branch.

## Scope & non-goals

- **In:** the farm block and logic for the three stalk plants.
- **Out:** kelp and other underwater plants, chorus fruit.

## Open questions

- **The name.** "Stalk Farm" is a placeholder.
- Valve choice (iron is a proposal for this wave).

## Done when

- A GameTest harvests grown sugar cane, cactus and bamboo into a hopper-fed chest on both loaders and leaves every base block in place.

## References

- Breakdown: [`../mods/forestry.md`](../mods/forestry.md) → Farms
- Related: [0201 Farm Framework](0201-farm-framework.md)
