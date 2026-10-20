# Cocoa Plantation

> **Status:** 🚧 Planned — Phase 3 of the farm plan · **Phase:** 2 — Forestry · **Module:** `logistics-forestry` (`forestry` domain)
> **Source:** [`../mods/forestry.md`](../mods/forestry.md) (Farm (single-block)) · **Depends on:** [`0201-farm-framework.md`](0201-farm-framework.md) · **Maps to (roadmap):** Phase 2 — Farms

> **Area:** follows the tiered centred-ring model from [0201](0201-farm-framework.md): a 7×7 courtyard and a ring from 11×11 (Basic) to 23×23 (Infernal), raised with Upgrade Kits ([0111](0111-machine-frame-tiers.md)); slots are germlings 0–3, materials 4–7, output 8–16; nothing changes in the world until Start.

Builds its own **jungle-log support columns** and grows cocoa on their sides (Forestry's cocoa plantation), picking ripe pods without touching the logs.

## Requirements

- **Germlings:** cocoa beans.
- **Materials:** jungle logs, for the supports.
- **Supports:** columns of jungle logs **3 high** at fixed anchor plots, placed with `PlaceSupport` (50 RF + one log per block). Supports are farm structures under [positional ownership](0201-farm-framework.md#positional-ownership): a matching log already at an anchor is adopted; a removed log is rebuilt; a wrong block there is cleared, then the log placed.
- **Planting:** cocoa is **side-mounted** on the support faces (25 RF per pod). Every face cell must satisfy `region.contains(cell)` ([0201 → Multifarm seam](0201-farm-framework.md#multifarm-seam-contract-only)).
- **Harvest:** ripe pods only, with **Pick** (40 RF); the farm replants the face from the drops. Supports are never harvested.
- **Water:** none.
- **Recipe:** standard farm recipe; valve settled in the farm's PR.

## Design sketch

- A `FarmLogic` on the shared `FarmBlockEntity` using `PlaceSupport`, `Plant` and `Pick`.
- Anchor and face positions are defined per lane from the inside out, so the layout is lane-stable across tier upgrades.

## Scope & non-goals

- **In:** the farm block and logic, support columns, cocoa planting and picking.
- **Out:** growing real jungle trees for cocoa (the [Arboretum](0203-arboretum.md) grows trees), other side-mounted plants.

## Open questions

- Anchor spacing per ring width.
- Valve choice.

## Done when

- A GameTest builds the supports, plants hopper-fed cocoa, and picks ripe pods into a hopper-fed chest on both loaders, leaving every log in place.
- Ownership tests (JUnit over `FakeFarmWorld` + GameTest): removing a support → rebuilt, log and RF charged once; replacing a support with stone → stone to output, support rebuilt; a pre-placed jungle log at an anchor → adopted, no charge; no logs → *No materials*, no world change.

## References

- Breakdown: [`../mods/forestry.md`](../mods/forestry.md) → Farms
- Related: [0201 Farm Framework](0201-farm-framework.md), [0209 Berry Farm](0209-berry-farm.md)
