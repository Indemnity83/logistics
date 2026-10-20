# Desert Farm

> **Status:** 🚧 Planned — Phase 2 of the farm plan · **Phase:** 2 — Forestry · **Module:** `logistics-forestry` (`forestry` domain)
> **Source:** [`../mods/forestry.md`](../mods/forestry.md) (Farm (single-block)) · **Depends on:** [`0201-farm-framework.md`](0201-farm-framework.md) · **Maps to (roadmap):** Phase 2 — Farms

> **Area:** follows the tiered centred-ring model from [0201](0201-farm-framework.md): a 7×7 courtyard and a ring from 11×11 (Basic) to 23×23 (Infernal), raised with Upgrade Kits ([0111](0111-machine-frame-tiers.md)); slots are germlings 0–3, materials 4–7, output 8–16; nothing changes in the world until Start.

Grows **cactus** in a checkerboard on sand and harvests it from the second block up, so the base regrows (Forestry's succulent farm). Split out of the old "Stalk Farm"; sugar cane and bamboo went to the [Plantation](0207-plantation.md).

## Requirements

- **Germlings:** cactus.
- **Soil:** sand (one item per block placed, 50 RF).
- **Layout:** a **checkerboard** — cactus breaks when anything is beside it, so no two cactus cells share a face.
- **Harvest:** from **Y+2 up**, top-down, one block per action (50 RF); the base block at Y+1 stays.
- **Water:** none.
- **Recipe:** standard farm recipe; valve settled in the farm's PR.

## Design sketch

- A `FarmLogic` on the shared `FarmBlockEntity`, breaking through the framework's `getDrops` + `destroyBlock` path.
- The cactus flower exists only on 26.x; handle it harmlessly so the same logic cherry-picks to every branch.
- The checkerboard is anchored region-relative, so it stays stable across tier upgrades.

## Scope & non-goals

- **In:** the farm block and logic for cactus.
- **Out:** sugar cane and bamboo ([0207](0207-plantation.md)).

## Open questions

- Valve choice.

## Done when

- A GameTest grows and harvests cactus into a hopper-fed chest on both loaders, leaving every base block in place.

## References

- Breakdown: [`../mods/forestry.md`](../mods/forestry.md) → Farms
- Related: [0201 Farm Framework](0201-farm-framework.md), [0207 Plantation](0207-plantation.md)
