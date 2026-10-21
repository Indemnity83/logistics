# Nether Wart Farm

> **Note:** written for the ring model, which [ADR 0001](../../docs/adr/0001-tend-only-farms.md) replaced with farms that tend the player's own field. Revisit this brief against the tend model before building it.

> **Status:** 🚧 Planned — Phase 2 of the farm plan · **Phase:** 2 — Forestry · **Module:** `logistics-forestry` (`forestry` domain)
> **Source:** [`../mods/forestry.md`](../mods/forestry.md) (Farm (single-block)) · **Depends on:** [`0201-farm-framework.md`](0201-farm-framework.md) · **Maps to (roadmap):** Phase 2 — Farms

> **Area:** follows the tiered centred-ring model from [0201](0201-farm-framework.md): a 7×7 courtyard and a ring from 11×11 (Basic) to 23×23 (Infernal), raised with Upgrade Kits ([0111](0111-machine-frame-tiers.md)); slots are germlings 0–3, materials 4–7, output 8–16; nothing changes in the world until Start.

Plants and harvests nether wart on soul sand (Forestry's infernal farm). Renamed from "Infernal Farm" because **Infernal** is now the top [machine tier](0111-machine-frame-tiers.md).

## Requirements

- **Germlings:** nether wart.
- **Soil:** soul sand.
- **Harvest:** mature wart only (50 RF); replants from the drops first (25 RF).
- **Water:** none.
- **Recipe:** standard farm recipe; proposed valve: **blazing**.

## Design sketch

- A `FarmLogic` on the shared `FarmBlockEntity`; a simple single-block crop with no tilling and no water.

## Scope & non-goals

- **In:** the farm block and logic for nether wart.
- **Out:** other nether plants (fungi are in [0206](0206-mushroom-farm.md)).

## Open questions

- Whether the farm lays soul sand from its material slots (50 RF per block) or only keeps existing soul sand.
- Valve choice (blazing is a proposal).

## Done when

- A GameTest plants hopper-fed nether wart, harvests it when mature into a hopper-fed chest and replants, on both loaders.

## References

- Breakdown: [`../mods/forestry.md`](../mods/forestry.md) → Farms
- Related: [0201 Farm Framework](0201-farm-framework.md)
