# Cropland (Crop Farm)

> **Status:** ✅ **Shipped** (#1408) — field crops on a ring around the farm · 🚧 **Planned:** tiered ring (11×11 at Basic around a 7×7 courtyard), water per till × climate, reconciled costs · **Phase:** 2 — Forestry · **Module:** `logistics-forestry` (`forestry` domain)
> **Source:** [`../mods/forestry.md`](../mods/forestry.md) (Farm (single-block)) · **Depends on:** [`0201-farm-framework.md`](0201-farm-framework.md) · **Maps to (roadmap):** Phase 2 — Farms

The field-crop farm (Forestry's Managed Farm / Combine): clears a ring around itself, lays dirt, hoes it with water from its tank, plants, and harvests mature crops, replanting from the harvest first. Gourds have their own farm ([0212](0212-gourd-farm.md)).

## Problem & goal

Field crops are the most common thing players automate. **Goal:** a single block that keeps the ring around it tilled, watered and replanted, and never loses a harvest.

## Requirements

### Functional
- **Germlings:** seeds and crop items whose block is a crop — wheat, carrot, potato, beetroot, torchflower, pitcher.
- **Area:** the farm sits at the centre of the 7×7 courtyard and works the tiered ring from [0201](0201-farm-framework.md): **11×11 at Basic** (2 wide, 72 plots); Industrial restores the earlier 15×15. It lays dirt level with itself and clears each plot's column from Y+1 up to Y+5.
- **Slots:** seeds 0–3, dirt 4–7 (materials), output 8–16.
- **Soil:** dirt, farmland and the blocks in `logistics:forestry/tillable` (grass and dirt paths) count as prepared; tillable blocks are hoed into farmland where they stand, so grass that spreads onto laid dirt is simply hoed. Anything else in the soil layer (the farm's own Y) is broken into the output and replaced with dirt from the material slots.
- **Cycle:** lay dirt → **hoe** into dry farmland for `rf_till` RF, no water → plant → harvest → replant on the same farmland. **A harvest leaves the farmland as it is**, at its moisture; only empty farmland that dries out back to dirt is hoed again.
- **Irrigation:** while the farm works, each random tick of a ring plot's farmland that finds no water nearby asks the farm, which keeps it wet for `water_per_irrigation_mb` (100 mB) × climate factor (0.8–2: hot or dry biomes up to twice). Vanilla's moisture is all-or-nothing for growth (any level above 0 grows crops at the wet rate) and only level 7 looks wet, so the farm pays per check rather than per level. Rain and real water are free. Upkeep is about one charge per plot per 68 s: roughly 100 mB/s × climate for a 72-plot Basic ring and 700 mB/s × climate for a 480-plot Infernal one.
- **Harvest:** mature crops only; replants from the drops before using the germling slots.
- **Water:** **yes** — an input-only tank, spent only on irrigation. With no water the farm still tills, plants and harvests; its farmland dries and its crops grow at the dry rate. No water-source placement.
- **States:** reports *Too dark* (crops need light ≥ 8) and *Blocked* (a protected block in the column) instead of retrying forever.
- **Recipe:** standard farm recipe with the **tin valve**.

### Balance
Costs from [0201](0201-farm-framework.md): place dirt 50 RF, till 25 RF, plant 25 RF, harvest 50 RF, plus irrigation water. A fully mature plot costs ≈100 RF over three actions; a full Basic pass takes ≈54 s and 7.2k RF (≈6.7 RF/t). Growing crops idle near 0.2 RF/t at Basic.

## Design sketch

- A `FarmLogic` implementation (e.g. `CropFarmLogic`) on the shared `FarmBlockEntity` with `FluidBuilder.inputOnly(c:water)`.
- Farmland via `Blocks.FARMLAND` + `BlockStateProperties.MOISTURE`; the farm never writes moisture, vanilla does on its random ticks. Never reference the farmland class (`FarmBlock` on 1.21.1, `FarmlandBlock` on 26.x).
- Our own `logistics:forestry/tillable` tag, because 1.21.1 has no `#turns_into_farmland`.
- Water per till goes through the pure `forestry/farm/Hydration` function.

## Scope & non-goals

- **In:** dirt laying, tilling, irrigation, planting, harvest + replant, the light / blocked states.
- **Out:** gourds ([0212](0212-gourd-farm.md)), sugar cane and bamboo ([0207](0207-plantation.md)), cactus ([0213](0213-desert-farm.md)), berries ([0209](0209-berry-farm.md)), cocoa ([0210](0210-cocoa-plantation.md)), nether wart ([0208](0208-nether-wart-farm.md)).

## Open questions

- Whether the block's display name becomes "Cropland" (the lineup name) or stays "Crop Farm".

## Done when

- A GameTest (in a `setAreaOverride(1, 5)` ring) irrigates from a Glass Tank or fluid pipe, plants hopper-fed seeds, harvests a mature crop into a hopper-fed chest, leaving the farmland in place, and replants, on both loaders.
- *Too dark* and *Blocked* show instead of an endless retry.
- JUnit: water per till follows `Hydration`; a till with too little water changes nothing.

## References

- Breakdown: [`../mods/forestry.md`](../mods/forestry.md) → Farms
- Related: [0201 Farm Framework](0201-farm-framework.md), [0202 Fertilizer & Soils](0202-fertilizer-and-soils.md), [0212 Gourd Farm](0212-gourd-farm.md)
