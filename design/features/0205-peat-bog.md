# Peat Bog

> **Status:** 🚧 Planned — wave 2 · **Phase:** 2 — Forestry · **Module:** `logistics-forestry` (`forestry` domain); Bog Earth stays in `core`
> **Source:** [`../mods/forestry.md`](../mods/forestry.md) (Peat bog / peat) · **Depends on:** [`0201-farm-framework.md`](0201-farm-framework.md), [`0202-fertilizer-and-soils.md`](0202-fertilizer-and-soils.md) (Bog Earth maturity) · **Maps to (roadmap):** Phase 2 — Farms (peat bog → peat fuel)

> **Area:** follows the centred-ring model from [0201](0201-farm-framework.md): the farm sits at the centre of a free 9×9 and works (and clears) a ring of plots around it; slots are germlings 0–3, soil 4–7, output 8–16. The ring size is settled when this farm is built.

The peat farm (Forestry's Peat Bog / Turbary): lays Bog Earth, keeps it wet, and digs it up once it has matured into Peat (+ dirt back).

## Requirements

- **Germlings:** none; the farm places Bog Earth from its soil slots.
- **Soil:** Bog Earth.
- **Harvest:** matured bog earth → Peat (+ dirt back).
- **Water:** **yes** — bog earth matures only while wet.
- **Recipe:** standard farm recipe; proposed valve: **rubber**.

## Design sketch

- A `FarmLogic` on the shared `FarmBlockEntity` with an input-only water tank.
- Relies on the Bog Earth `maturity` property from [0202](0202-fertilizer-and-soils.md); Bog Earth gets a real block class (today a plain `new Block(...)` in `LogisticsCore`), with a default state so shipped worlds load unchanged.

## Scope & non-goals

- **In:** the farm block and logic, laying and harvesting Bog Earth.
- **Out:** a peat-fired engine tier (deferred), changes to the existing Bog Earth smelting route.

## Open questions

- Valve choice (rubber is a proposal for this wave).
- How the farm keeps bog earth wet: spending water from its tank, or requiring adjacent water.

## Done when

- A GameTest lays Bog Earth, keeps it wet from a real tank, and harvests Peat + dirt into a hopper-fed chest on both loaders.

## References

- Breakdown: [`../mods/forestry.md`](../mods/forestry.md) → Peat bog / peat
- Related: [0201 Farm Framework](0201-farm-framework.md), [0202 Fertilizer & Soils](0202-fertilizer-and-soils.md)
