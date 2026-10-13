# Crop Farm

> **Status:** ✅ **Shipped** (#1408) — field crops on a 15×15 ring around a free 9×9 · 🚧 gourd row pattern planned (wave 3) · **Phase:** 2 — Forestry · **Module:** `logistics-forestry` (`forestry` domain)
> **Source:** [`../mods/forestry.md`](../mods/forestry.md) (Farm (single-block)) · **Depends on:** [`0201-farm-framework.md`](0201-farm-framework.md) · **Maps to (roadmap):** Phase 2 — Farms

The field-crop farm (Forestry's Managed Farm / Combine): clears a ring around itself, lays dirt, hoes it with water from its tank, plants, and harvests mature crops, replanting from the harvest first. Later it also handles gourds with a row pattern, replacing Forestry's separate Gourmand farm.

## Problem & goal

Field crops are the most common thing players automate. **Goal:** a single block that keeps the ring around it tilled, watered and replanted, and never loses a harvest.

## Requirements

### Functional
- **Germlings:** seeds and crop items whose block is a crop — wheat, carrot, potato, beetroot, torchflower, pitcher.
- **Area:** the farm sits at the centre of a free **9×9** and works the 3-wide ring out to **15×15** (144 plots). It clears each plot's column up to Y+4 ([0201](0201-farm-framework.md)).
- **Slots:** seeds 0–3, dirt 4–7, output 8–16.
- **Soil:** dirt and farmland count as prepared; anything else at Y-1, grass included, is replaced with dirt from the soil slots.
- **Cycle:** lay dirt → **hoe**, spending `water_per_till_mb` (250 mB) from the tank and setting moisture to 7 → plant → keep planted farmland at moisture 7 for `rf_moisten` RF only (the water paid at hoeing covers the whole crop) → harvest. **A harvest turns the plot back to plain dirt**, so the next hoe costs water again.
- **Harvest:** mature crops only; replants from the drops before using the germling slots.
- **Water:** **yes** — an input-only tank, spent per hoe. With no water the farm still harvests but stops tilling and reports *No water*. No water-source placement.
- **States:** reports *Too dark* (crops need light ≥ 8) and *Blocked* (a protected block in the column) instead of retrying forever.
- **Gourds (wave 3):** pumpkin / melon seeds planted in a row pattern on farmland; harvests the fruit blocks only, the stems stay.
- **Recipe:** standard farm recipe with the **tin valve**.

## Design sketch

- A `FarmLogic` implementation (e.g. `CropFarmLogic`) on the shared `FarmBlockEntity` with `FluidBuilder.inputOnly(c:water)`.
- Farmland via `Blocks.FARMLAND` + `BlockStateProperties.MOISTURE`, moisture written with flag 2; never reference the farmland class (`FarmBlock` on 1.21.1, `FarmlandBlock` on 26.x).
- Our own `logistics:forestry/tillable` tag, because 1.21.1 has no `#turns_into_farmland`.

## Scope & non-goals

- **In:** dirt laying, tilling with water, moistening, planting, harvest + replant, the light / blocked states, the later gourd row pattern.
- **Out:** a separate gourd farm, stalk crops (see [0207](0207-stalk-farm.md)), berries and cocoa (see [0209](0209-orchard.md)), nether wart (see [0208](0208-infernal-farm.md)).

## Open questions

- The exact gourd row pattern (stem rows vs. fruit rows) inside the 3-wide ring.

## Done when

- A GameTest (in a `setAreaOverride(1, 5)` ring) tills with water from a Glass Tank or fluid pipe, plants hopper-fed seeds, harvests a mature crop into a hopper-fed chest, returning the plot to dirt, and replants, on both loaders.
- *Too dark* and *Blocked* show instead of an endless retry.
- Wave 3: pumpkins and melons are harvested without breaking stems.

## References

- Breakdown: [`../mods/forestry.md`](../mods/forestry.md) → Farms
- Related: [0201 Farm Framework](0201-farm-framework.md), [0202 Fertilizer & Soils](0202-fertilizer-and-soils.md)
