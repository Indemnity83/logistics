# Cave Vine Farm

> **Status:** 🚧 Planned — Phase 3 of the farm plan · **Phase:** 2 — Forestry · **Module:** `logistics-forestry` (`forestry` domain)
> **Source:** [`../mods/forestry.md`](../mods/forestry.md) (Farm (single-block)) · **Depends on:** [`0201-farm-framework.md`](0201-farm-framework.md) (`PlantCompat.harvestBerries`) · **Maps to (roadmap):** Phase 2 — Farms

> **Area:** follows the tiered centred-ring model from [0201](0201-farm-framework.md): a 7×7 courtyard and a ring from 11×11 (Basic) to 23×23 (Infernal), raised with Upgrade Kits ([0111](0111-machine-frame-tiers.md)); slots are germlings 0–3, materials 4–7, output 8–16; nothing changes in the world until Start.

Builds a ceiling over its ring, hangs cave vines from it and **picks glow berries** without breaking the vines.

## Requirements

- **Germlings:** glow berries.
- **Materials:** moss blocks, planks, cobblestone or stone, for the ceiling.
- **Ceiling:** one block at **Y+5** over **ring columns only** — never over the courtyard — placed with `PlaceSupport` (50 RF + one block). Ceiling cells are farm structures under [positional ownership](0201-farm-framework.md#positional-ownership): a matching block already there is adopted; a broken cell is rebuilt and its vine re-planted.
- **Planting:** a vine from each ceiling cell (25 RF).
- **Length:** vines are **trimmed to length 3** (`cave_vine_max_length`); growth past it is cleared into the output.
- **Harvest:** **Pick** berries (40 RF); the vine stays.
- **Water:** none.
- **Recipe:** standard farm recipe; valve settled in the farm's PR.

## Design sketch

- A `FarmLogic` on the shared `FarmBlockEntity` using `PlaceSupport`, `Plant` and `Pick`; `clearanceHeight()` stops below the ceiling, and nothing above it is ever touched.
- Picking goes through `PlantCompat.harvestBerries`, which rolls drops only after `setBlock` succeeds.

## Scope & non-goals

- **In:** the farm block and logic, the ceiling, vine planting, trimming and picking.
- **Out:** sweet berries ([0209](0209-berry-farm.md)), weeping and twisting vines.

## Open questions

- Valve choice.

## Done when

- A GameTest builds the ceiling, plants hopper-fed glow berries, and picks berries into a hopper-fed chest on both loaders, leaving every vine in place and no vine longer than 3.
- Ownership tests: breaking a ceiling cell → rebuilt and its vine re-planted; a pre-placed matching block → adopted, no charge; no ceiling material → *No materials*, no world change.

## References

- Breakdown: [`../mods/forestry.md`](../mods/forestry.md) → Farms
- Related: [0201 Farm Framework](0201-farm-framework.md), [0209 Berry Farm](0209-berry-farm.md)
