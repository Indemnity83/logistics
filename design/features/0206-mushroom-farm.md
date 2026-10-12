# Mushroom Farm

> **Status:** 🚧 Planned — wave 2 · **Phase:** 2 — Forestry · **Module:** `logistics-forestry` (`forestry` domain)
> **Source:** [`../mods/forestry.md`](../mods/forestry.md) (Farm (single-block)) · **Depends on:** [`0201-farm-framework.md`](0201-farm-framework.md), [`0203-arboretum.md`](0203-arboretum.md) (`TreeFeller`) · **Maps to (roadmap):** Phase 2 — Farms

Grows and fells huge mushrooms and huge nether fungi (Forestry's shroom farm).

## Requirements

- **Germlings:** mushrooms, crimson and warped fungus.
- **Soil:** mycelium / podzol / nylium — keeps it, does not make it.
- **Harvest:** huge mushrooms and huge fungi, felled like trees.
- **Water:** none.
- **Recipe:** standard farm recipe; proposed valve: **obsidian**.

## Design sketch

- A `FarmLogic` on the shared `FarmBlockEntity`; felling reuses `forestry/logic/TreeFeller`, whose canopy rules already cover `#wart_blocks` and shroomlight.
- Fungus growth goes through `PlantCompat` (`NetherFungusBlock` with `Feature` keys on 26.x, `FungusBlock` with `ConfiguredFeature` keys on 1.21.1).

## Scope & non-goals

- **In:** the farm block and logic for mushrooms and nether fungi.
- **Out:** making mycelium or nylium (a Moistener-style job), small-mushroom spreading.

## Open questions

- Valve choice (obsidian is a proposal for this wave).

## Done when

- GameTests grow and fell a huge mushroom and a huge fungus into a hopper-fed chest on both loaders; `TreeFeller` JUnit covers a huge-fungus shape.

## References

- Breakdown: [`../mods/forestry.md`](../mods/forestry.md) → Farms
- Related: [0201 Farm Framework](0201-farm-framework.md), [0203 Arboretum](0203-arboretum.md)
