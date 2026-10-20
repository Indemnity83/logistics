# Peat Bog

> **Status:** 🚧 Planned — Phase 3 of the farm plan · **Phase:** 2 — Forestry · **Module:** `logistics-forestry` (`forestry` domain); Bog Earth stays in `core`
> **Source:** [`../mods/forestry.md`](../mods/forestry.md) (Peat bog / peat) · **Depends on:** [`0201-farm-framework.md`](0201-farm-framework.md), [`0202-fertilizer-and-soils.md`](0202-fertilizer-and-soils.md) (Bog Earth maturity) · **Maps to (roadmap):** Phase 2 — Farms (peat bog → peat fuel)

> **Area:** follows the tiered centred-ring model from [0201](0201-farm-framework.md): a 7×7 courtyard and a ring from 11×11 (Basic) to 23×23 (Infernal), raised with Upgrade Kits ([0111](0111-machine-frame-tiers.md)); slots are germlings 0–3, materials 4–7, output 8–16; nothing changes in the world until Start.

The peat farm (Forestry's Peat Bog / Turbary): lays Bog Earth in its ring, keeps it wet, and digs it up once it has matured into Peat (+ dirt back).

## Requirements

- **Germlings:** none; the farm places Bog Earth from its material slots.
- **Soil:** Bog Earth (one item per block placed, 50 RF).
- **Harvest:** matured bog earth → Peat (+ dirt back), 50 RF per block.
- **Water:** the plan's water budget gives the Peat Bog **no tank water**; how bog earth stays wet is the open question below.
- **Recipe:** standard farm recipe; proposed valve: **rubber**.

## Design sketch

- A `FarmLogic` on the shared `FarmBlockEntity`, region-relative through `PlotLayout` like every logic ([0201 → Multifarm seam](0201-farm-framework.md#multifarm-seam-contract-only)).
- Relies on the Bog Earth `maturity` property from [0202](0202-fertilizer-and-soils.md); Bog Earth gets a real block class (today a plain `new Block(...)` in `LogisticsCore`), with a default state so shipped worlds load unchanged.

## Scope & non-goals

- **In:** the farm block and logic, laying and harvesting Bog Earth.
- **Out:** a peat-fired engine tier (deferred), changes to the existing Bog Earth smelting route.

## Open questions

- How the farm keeps bog earth wet: a tank and water per op (Forestry charged 20 mB × `h`), or requiring water nearby. The plan currently budgets no water for this farm.
- Valve choice (rubber is a proposal).

## Done when

- A GameTest lays Bog Earth, keeps it wet, and harvests Peat + dirt into a hopper-fed chest on both loaders.

## References

- Breakdown: [`../mods/forestry.md`](../mods/forestry.md) → Peat bog / peat
- Related: [0201 Farm Framework](0201-farm-framework.md), [0202 Fertilizer & Soils](0202-fertilizer-and-soils.md)
