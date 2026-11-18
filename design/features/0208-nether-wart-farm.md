# Nether Wart Farm

> **Status:** 🚧 In progress — Phase 2 of the farm plan · **Phase:** 2 — Forestry · **Module:** `logistics-forestry` (`forestry` domain)
> **Source:** [`../mods/forestry.md`](../mods/forestry.md) (Farm (single-block)) · **Depends on:** [`0201-farm-framework.md`](0201-farm-framework.md), [ADR 0001](../../docs/adr/0001-tend-only-farms.md) · **Maps to (roadmap):** Phase 2 — Farms

Tends the nether wart a player plants on soul sand around it (Forestry's infernal farm). Renamed from "Infernal Farm" because **Infernal** is now the top [machine tier](0111-machine-frame-tiers.md).

It is the simplest farm and a near copy of the Crop Farm: one block, one plant, no tilling and no water.

## Requirements

- **Reach:** the shared 7×7 Reach, from one block below the farm to one above it ([ADR 0001](../../docs/adr/0001-tend-only-farms.md)). Basic tier only.
- **Tends:** `logistics:forestry/nether_wart_farm_tends`, which holds `minecraft:nether_wart`.
- **Ground:** `logistics:forestry/nether_wart_farm_ground`, which holds `minecraft:soul_sand`. Wart on any other ground is left alone.
- **Ripe:** wart at its highest `age` (3).
- **Harvest:** breaks ripe wart with vanilla's loot, then replants one wart from the drops first and the inventory second. Ripe wart always drops 2–4, so a Plot is only left empty when something else takes the drops.
- **Plots:** growing wart on soul sand is taken on as a Plot before its first harvest. A Plot whose soul sand is replaced is forgotten. An Empty Plot waits for nether wart to arrive in the inventory.
- **Soul sand:** never laid, repaired or replaced. A Plot never costs a till.
- **Water:** none.
- **Output:** the shared 15-slot inventory. When it is full, the overflow spills at the plot and the farm keeps working.
- **Recipe:** the standard farm recipe (valve, tool, two germlings, machine core, copper gears, redstone reception coil) with the **blazing valve**, an iron hoe and two nether wart.

## Balance

- **Actions:** the shared `rf_harvest_crop` (100 RF) per harvest and `rf_plant` (50 RF) per replant.
- **Upkeep:** `rf_upkeep_nether_wart_farm`, 0.25 RF/t per Plot, the same as the Crop Farm.
  - A full field is the 48 positions around the farm on one level, so it costs 12 RF/t. A Crop Farm's full field is about 13 RF/t.
  - That is more than one Stirling engine supplies at its 10 RF/t peak, and well inside one warm Fuel engine's 10–40 RF/t.
  - Nether wart takes about 34 minutes to ripen on average (a 10% chance per random tick, three stages), close to wheat on hydrated farmland. The harvest and replant cost, 150 RF per Plot per cycle, adds well under 0.01 RF/t per Plot, so upkeep sets the running cost.

## Design sketch

- `NetherWartTendLogic` (a `TendLogic`) on the shared `FarmBlockEntity`, driven by `TendFarmComponent`, the same shape as `CropTendLogic`.
- Ripeness comes from the block's `age` property through `GrowthAge`. `plotGround` returns the soul sand as it is, or `null` for anything else.
- `NetherWartFarmBlock` registers through `LogisticsForestry.BLOCK.registerFarm`. Loader wiring (capabilities, the screen, Jade) is shared by every farm block and needs nothing new.
- Body: the glass case with a soul sand bed and four wart crosses, painted from `nether_wart_farm_body.png`.

## Scope & non-goals

- **In:** the farm block, its tend logic, its tags, recipe, model, upkeep and tests.
- **Out:** other nether plants (fungi are in [0206](0206-mushroom-farm.md)). Laying or repairing soul sand. Higher tiers.

## Decisions

- **Tend only:** the farm keeps existing soul sand and never lays any, so it has no material slots and no per-block soul sand cost.
- **Valve:** the blazing valve. It needs blaze rods, so the farm sits after a first Nether trip, where wart is found anyway.
- **Recipe tool:** an iron hoe, the same as the Crop Farm.
- **Costs:** the shared crop harvest and plant costs rather than new wart-only keys. The brief's original 50 RF harvest dates from the ring model's cost table.
- **Upkeep:** its own config key at the Crop Farm's 0.25 RF/t per Plot, so it can be tuned on its own.

## Done when

- A GameTest harvests a ripe wart field on soul sand, replants every plot and the hopper under the farm pulls the harvest into a chest, on both loaders.
- A GameTest refills an Empty Plot with wart a hopper on top of the farm fed it, then harvests and replants it when ripe, on both loaders.
- JUnit: unripe wart and wart off soul sand are left alone; a Plot whose soul sand is replaced is forgotten and never repaired; a harvest costs a harvest and a plant, never a till; the farm pays its own upkeep rate.

## References

- Breakdown: [`../mods/forestry.md`](../mods/forestry.md) → Farms
- Related: [0201 Farm Framework](0201-farm-framework.md), [0204 Crop Farm](0204-crop-farm.md), [ADR 0001](../../docs/adr/0001-tend-only-farms.md)
