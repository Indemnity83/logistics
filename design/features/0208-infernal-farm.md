# Infernal Farm

> **Status:** 🚧 Planned — wave 3 · **Phase:** 2 — Forestry · **Module:** `logistics-forestry` (`forestry` domain)
> **Source:** [`../mods/forestry.md`](../mods/forestry.md) (Farm (single-block)) · **Depends on:** [`0201-farm-framework.md`](0201-farm-framework.md) · **Maps to (roadmap):** Phase 2 — Farms

Plants and harvests nether wart on soul sand (Forestry's infernal farm).

## Requirements

- **Germlings:** nether wart.
- **Soil:** soul sand.
- **Harvest:** mature wart only; replants.
- **Water:** none.
- **Recipe:** standard farm recipe; proposed valve: **blazing**.

## Design sketch

- A `FarmLogic` on the shared `FarmBlockEntity`; a simple single-block crop with no tilling and no water.

## Scope & non-goals

- **In:** the farm block and logic for nether wart.
- **Out:** other nether plants (fungi are in [0206](0206-mushroom-farm.md)).

## Open questions

- Whether the farm lays soul sand from its soil slots or only keeps existing soul sand.
- Valve choice (blazing is a proposal for this wave).

## Done when

- A GameTest plants hopper-fed nether wart, harvests it when mature into a hopper-fed chest and replants, on both loaders.

## References

- Breakdown: [`../mods/forestry.md`](../mods/forestry.md) → Farms
- Related: [0201 Farm Framework](0201-farm-framework.md)
