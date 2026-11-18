# Berry Farm

> **Note:** written for the ring model, which [ADR 0001](../../docs/adr/0001-tend-only-farms.md) replaced with farms that tend the player's own field. Revisit this brief against the tend model before building it.

> **Status:** 🚧 Planned — Phase 2 of the farm plan · **Phase:** 2 — Forestry · **Module:** `logistics-forestry` (`forestry` domain)
> **Source:** [`../mods/forestry.md`](../mods/forestry.md) (Farm (single-block)) · **Depends on:** [`0201-farm-framework.md`](0201-farm-framework.md) (`PlantCompat.harvestBerries`) · **Maps to (roadmap):** Phase 2 — Farms

> **Area:** follows the tiered centred-ring model from [0201](0201-farm-framework.md): a 7×7 courtyard and a ring from 11×11 (Basic) to 23×23 (Infernal), raised with Upgrade Kits ([0111](0111-machine-frame-tiers.md)); slots are germlings 0–3, materials 4–7, output 8–16; nothing changes in the world until Start.

Plants sweet berry bushes and **picks** them without breaking the bush (Forestry's orchard idea, reworked). Glow berries have their own [Cave Vine Farm](0211-cave-vine-farm.md) and cocoa its own [Cocoa Plantation](0210-cocoa-plantation.md).

## Requirements

- **Germlings:** sweet berries.
- **Soil:** dirt / grass — the ground sweet berry bushes accept.
- **Harvest:** a non-destructive **Pick** (40 RF): takes the berries and resets the bush's age; the bush is never broken.
- **Water:** none.
- **Recipe:** standard farm recipe; proposed valve: **emerald**.

## Design sketch

- A `FarmLogic` on the shared `FarmBlockEntity` using the `Pick` action.
- Picking goes through `PlantCompat.harvestBerries(level, pos, state)` — 26.x reads harvest loot tables where 1.21.1 hard-codes counts — which rolls drops only after `setBlock` succeeds, so a failed pick can't duplicate berries ([0201 → Action atomicity](0201-farm-framework.md#action-atomicity)).

## Scope & non-goals

- **In:** the farm block and logic for sweet berries.
- **Out:** glow berries ([0211](0211-cave-vine-farm.md)), cocoa ([0210](0210-cocoa-plantation.md)), apples (they come from leaves in the [Arboretum](0203-arboretum.md)), modded fruit beyond what the tags cover.

## Open questions

- Valve choice (emerald is a proposal, carried over from the Orchard).

## Done when

- A GameTest picks sweet berries into a hopper-fed chest on both loaders, leaving every bush in place with its age reset.
- JUnit: a pick whose `setBlock` fails yields no drops and no charge.

## References

- Breakdown: [`../mods/forestry.md`](../mods/forestry.md) → Farms
- Related: [0201 Farm Framework](0201-farm-framework.md)
