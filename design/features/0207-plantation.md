# Plantation

> **Note:** written for the ring model, which [ADR 0001](../../docs/adr/0001-tend-only-farms.md) replaced with farms that tend the player's own field. Revisit this brief against the tend model before building it.

> **Status:** 🚧 Planned — Phase 2 of the farm plan · **Phase:** 2 — Forestry · **Module:** `logistics-forestry` (`forestry` domain)
> **Source:** [`../mods/forestry.md`](../mods/forestry.md) (Farm (single-block)) · **Depends on:** [`0201-farm-framework.md`](0201-farm-framework.md) · **Maps to (roadmap):** Phase 2 — Farms

> **Area:** follows the tiered centred-ring model from [0201](0201-farm-framework.md): a 7×7 courtyard and a ring from 11×11 (Basic) to 23×23 (Infernal), raised with Upgrade Kits ([0111](0111-machine-frame-tiers.md)); slots are germlings 0–3, materials 4–7, output 8–16; nothing changes in the world until Start.

Grows **sugar cane and bamboo** beside water channels it builds itself, and harvests everything above the base block so the plant regrows (Forestry's reeds farm). Cactus, which needs the opposite conditions, has its own [Desert Farm](0213-desert-farm.md). Replaces the placeholder "Stalk Farm".

## Requirements

- **Germlings:** sugar cane, bamboo.
- **Soil:** sand or dirt — the ground sugar cane and bamboo accept.
- **Water channels:** some lanes are **channel lanes** of real water sources at the soil layer, so every sugar-cane base touches water. The farm places each source itself (`PlaceWater`: 50 RF + **1000 mB** from its tank). Channels are farm structures under [positional ownership](0201-farm-framework.md#positional-ownership): flowing water from a broken channel is drained, and a channel cell the player fills in is cleared and re-placed for another 1000 mB.
- **Harvest:** everything **above the base block**, top-down, one block per action (50 RF); never the base.
- **Water:** **yes** — an input-only tank, spent only when a channel source is placed.
- **Recipe:** standard farm recipe; valve settled in the farm's PR.

## Design sketch

- A `FarmLogic` on the shared `FarmBlockEntity` with `FluidBuilder.inputOnly(c:water)`.
- Channel and planting lanes are defined per lane from the inside out, so the layout is lane-stable across tier upgrades and region-relative through `PlotLayout`.
- Channel ownership: a water source in a channel cell is the farm's; anything else there is an obstruction.

## Scope & non-goals

- **In:** the farm block and logic for sugar cane and bamboo, channel placement and upkeep.
- **Out:** cactus ([0213](0213-desert-farm.md)), kelp and other underwater plants, chorus fruit.

## Open questions

- The exact channel / planting lane pattern per ring width (lane-stable).
- Valve choice.

## Done when

- A GameTest harvests grown sugar cane and bamboo into a hopper-fed chest on both loaders and leaves every base block in place.
- Ownership tests (JUnit over `FakeFarmWorld` + GameTest): filling a channel cell → cleared, source re-placed, 1000 mB charged; flowing water drained; a channel placed with too little water changes nothing.

## References

- Breakdown: [`../mods/forestry.md`](../mods/forestry.md) → Farms
- Related: [0201 Farm Framework](0201-farm-framework.md), [0213 Desert Farm](0213-desert-farm.md)
