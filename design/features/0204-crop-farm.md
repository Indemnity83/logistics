# Crop Farm

> **Status:** 🚧 Planned — design settled, wave 1 (gourd row pattern in wave 3) · **Phase:** 2 — Forestry · **Module:** `logistics-forestry` (`forestry` domain)
> **Source:** [`../mods/forestry.md`](../mods/forestry.md) (Farm (single-block)) · **Depends on:** [`0201-farm-framework.md`](0201-farm-framework.md), [`0202-fertilizer-and-soils.md`](0202-fertilizer-and-soils.md) (Fertilizer) · **Maps to (roadmap):** Phase 2 — Farms

The field-crop farm (Forestry's Managed Farm / Combine): tills, hydrates, plants, fertilizes and harvests mature crops, replanting from the harvest first. Later it also handles gourds with a row pattern, replacing Forestry's separate Gourmand farm.

## Problem & goal

Field crops are the most common thing players automate. **Goal:** a single block that keeps a 9×9 plot tilled, watered and replanted, and never loses a harvest.

## Requirements

### Functional
- **Germlings:** seeds and crop items whose block is a crop — wheat, carrot, potato, beetroot, torchflower, pitcher.
- **Soil:** tills dirt / grass → farmland, from the `logistics:forestry/tillable` tag.
- **Harvest:** mature crops only; replants from the drops before using the germling slots.
- **Water:** **yes** — the input-only tank hydrates farmland (IRRIGATE costs mB).
- **States:** reports *Too dark* (crops need light ≥ 8) and *Blocked* (farmland reverts under solid blocks) instead of retrying forever.
- **Gourds (wave 3):** pumpkin / melon seeds planted in a row pattern on farmland; harvests the fruit blocks only, the stems stay.
- **Recipe:** standard farm recipe with the **tin valve**.

## Design sketch

- A `FarmLogic` implementation (e.g. `CropFarmLogic`) on the shared `FarmBlockEntity` with `FluidBuilder.inputOnly(c:water)`.
- Farmland via `Blocks.FARMLAND` + `BlockStateProperties.MOISTURE`, moisture written with flag 2; never reference the farmland class (`FarmBlock` on 1.21.1, `FarmlandBlock` on 26.x).
- Our own `logistics:forestry/tillable` tag, because 1.21.1 has no `#turns_into_farmland`.
- Growth via `PlantCompat.canFertilize` / `fertilize`.

## Scope & non-goals

- **In:** tilling, hydration, planting, harvest + replant, fertilizing, the light / blocked states, the later gourd row pattern.
- **Out:** a separate gourd farm, stalk crops (see [0207](0207-stalk-farm.md)), berries and cocoa (see [0209](0209-orchard.md)), nether wart (see [0208](0208-infernal-farm.md)).

## Open questions

- The exact gourd row pattern (stem rows vs. fruit rows) inside a 9×9 / 13×13 / 17×17 plot.

## Done when

- A GameTest tills, waters (from a Glass Tank or fluid pipe), plants hopper-fed seeds, harvests a mature crop into a hopper-fed chest and replants, on both loaders.
- *Too dark* and *Blocked* show instead of an endless retry.
- Wave 3: pumpkins and melons are harvested without breaking stems.

## References

- Breakdown: [`../mods/forestry.md`](../mods/forestry.md) → Farms
- Related: [0201 Farm Framework](0201-farm-framework.md), [0202 Fertilizer & Soils](0202-fertilizer-and-soils.md)
