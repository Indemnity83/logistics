# Mushroom Farm

> **Note:** written for the ring model, which [ADR 0001](../../docs/adr/0001-tend-only-farms.md) replaced with farms that tend the player's own field. Revisit this brief against the tend model before building it.

> **Status:** 🚧 Planned — Phase 3 of the farm plan · **Phase:** 2 — Forestry · **Module:** `logistics-forestry` (`forestry` domain)
> **Source:** [`../mods/forestry.md`](../mods/forestry.md) (Farm (single-block)) · **Depends on:** [`0201-farm-framework.md`](0201-farm-framework.md), [`0203-arboretum.md`](0203-arboretum.md) (`TreeFeller`) · **Maps to (roadmap):** Phase 2 — Farms

> **Area:** follows the tiered centred-ring model from [0201](0201-farm-framework.md): a 7×7 courtyard and a ring from 11×11 (Basic) to 23×23 (Infernal), raised with Upgrade Kits ([0111](0111-machine-frame-tiers.md)); slots are germlings 0–3, materials 4–7, output 8–16; nothing changes in the world until Start.

Grows and fells **giant mushrooms and huge nether fungi** (Forestry's shroom farm). It grows them with bone meal or Fertilizer; small mushrooms are only a by-product of felling.

## Requirements

- **Germlings:** red and brown mushrooms, crimson and warped fungus.
- **Soil:** mycelium / podzol / nylium — keeps it, does not make it.
- **Fertilize:** bone meal or [Fertilizer](0202-fertilizer-and-soils.md) in the **material slots** grows the planted germling into its huge form (`Fertilize` action, 25 RF). Like vanilla bone meal, one charge is consumed per valid attempt even if the growth roll fails ([0201 → Action atomicity](0201-farm-framework.md#action-atomicity)).
- **Harvest:** huge mushrooms and huge fungi only, felled like trees (25 RF per stem block, 5 RF per cap / wart block). Small mushrooms are never farmed on their own; any that drop or spread are collected as a by-product.
- **Water:** none.
- **Recipe:** standard farm recipe; proposed valve: **obsidian**.

## Design sketch

- A `FarmLogic` on the shared `FarmBlockEntity`; felling reuses `forestry/logic/TreeFeller` through the incremental `FellPlanner`, whose canopy rules already cover `#wart_blocks` and shroomlight.
- Growth goes through `PlantCompat.fertilize` (fungi: `NetherFungusBlock` with `Feature` keys on 26.x, `FungusBlock` with `ConfiguredFeature` keys on 1.21.1).
- Planting spacing follows the Arboretum's Chebyshev rule so huge caps don't merge.

## Scope & non-goals

- **In:** the farm block and logic for giant mushrooms and huge fungi; Fertilize from the material slots.
- **Out:** farming small mushrooms, making mycelium or nylium (a Moistener-style job), a Yield augment (deferred).

## Open questions

- Valve choice (obsidian is a proposal).

## Done when

- GameTests grow (with bone meal) and fell a huge mushroom and a huge fungus into a hopper-fed chest on both loaders; `TreeFeller` JUnit covers a huge-fungus shape.
- JUnit: a failed growth roll still consumes one charge; an invalid target consumes nothing.

## References

- Breakdown: [`../mods/forestry.md`](../mods/forestry.md) → Farms
- Related: [0201 Farm Framework](0201-farm-framework.md), [0203 Arboretum](0203-arboretum.md), [0202 Fertilizer & Soils](0202-fertilizer-and-soils.md)
