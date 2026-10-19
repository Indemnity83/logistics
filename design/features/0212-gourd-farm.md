# Gourd Farm

> **Status:** 🚧 Planned — Phase 2 of the farm plan · **Phase:** 2 — Forestry · **Module:** `logistics-forestry` (`forestry` domain)
> **Source:** [`../mods/forestry.md`](../mods/forestry.md) (Farm (single-block)) · **Depends on:** [`0201-farm-framework.md`](0201-farm-framework.md), [`0204-crop-farm.md`](0204-crop-farm.md) (tilling with water) · **Maps to (roadmap):** Phase 2 — Farms

> **Area:** follows the tiered centred-ring model from [0201](0201-farm-framework.md): a 7×7 courtyard and a ring from 11×11 (Basic) to 23×23 (Infernal), raised with Upgrade Kits ([0111](0111-machine-frame-tiers.md)); slots are germlings 0–3, materials 4–7, output 8–16; nothing changes in the world until Start.

Grows pumpkins and melons in **alternating stem and fruit lanes** and harvests the fruit only (Forestry's Gourmand farm). Replaces the gourd row pattern once planned inside the Crop Farm.

## Requirements

- **Germlings:** pumpkin seeds, melon seeds.
- **Soil:** dirt, tilled like [Cropland](0204-crop-farm.md) — 100 mB × `h` per till.
- **Layout:** stem lanes and fruit lanes alternate, defined per lane from the inside out so the pattern is lane-stable across upgrades. Fruit cells are kept clear for the stems to grow into, and must satisfy `region.contains(cell)`.
- **Harvest:** the **fruit blocks only** (50 RF); stems are never broken.
- **Water:** **yes** — an input-only tank, spent per till.
- **Recipe:** standard farm recipe; valve settled in the farm's PR.

## Design sketch

- A `FarmLogic` on the shared `FarmBlockEntity` with `FluidBuilder.inputOnly(c:water)`, sharing Cropland's till and irrigation handling.

## Scope & non-goals

- **In:** the farm block and logic for pumpkins and melons.
- **Out:** field crops ([0204](0204-crop-farm.md)).

## Open questions

- Which lane parity carries stems at each width (lane 0 stems or fruit).
- Valve choice.

## Done when

- A GameTest grows and harvests pumpkins and melons into a hopper-fed chest on both loaders without breaking a stem.
- JUnit: the lane pattern is stable across tier upgrades.

## References

- Breakdown: [`../mods/forestry.md`](../mods/forestry.md) → Farms
- Related: [0201 Farm Framework](0201-farm-framework.md), [0204 Cropland](0204-crop-farm.md)
