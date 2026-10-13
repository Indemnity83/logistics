# Orchard

> **Status:** 🚧 Planned — wave 3 · **Phase:** 2 — Forestry · **Module:** `logistics-forestry` (`forestry` domain)
> **Source:** [`../mods/forestry.md`](../mods/forestry.md) (Farm (single-block); Tree products) · **Depends on:** [`0201-farm-framework.md`](0201-farm-framework.md) (`PlantCompat.harvestBerries`) · **Maps to (roadmap):** Phase 2 — Farms

> **Area:** follows the centred-ring model from [0201](0201-farm-framework.md): the farm sits at the centre of a free 9×9 and works (and clears) a ring of plots around it; slots are germlings 0–3, soil 4–7, output 8–16. The ring size is settled when this farm is built.

Picks fruit without breaking the plant: sweet berries, glow berries and cocoa (Forestry's Orchard / Cocoa plantation).

## Requirements

- **Germlings:** sweet berries, glow berries, cocoa — it plants cocoa on jungle logs.
- **Soil:** n/a.
- **Harvest:** fruit only; resets the plant's age, never breaks it.
- **Water:** none.
- **Recipe:** standard farm recipe; proposed valve: **emerald**.

## Design sketch

- A `FarmLogic` on the shared `FarmBlockEntity`; the soil slot row is hidden on the shared screen since the farm lays no soil.
- Berry picking goes through `PlantCompat.harvestBerries(level, pos, state)` — 26.x reads harvest loot tables where 1.21.1 hard-codes counts.

## Scope & non-goals

- **In:** the farm block and logic for berries and cocoa.
- **Out:** apples (they come from leaves in the [Arboretum](0203-arboretum.md)), modded fruit trees beyond what the tags cover.

## Open questions

- Valve choice (emerald is a proposal for this wave).

## Done when

- A GameTest picks sweet berries, glow berries and cocoa into a hopper-fed chest on both loaders, leaving every plant in place with its age reset.

## References

- Breakdown: [`../mods/forestry.md`](../mods/forestry.md) → Farms
- Related: [0201 Farm Framework](0201-farm-framework.md)
