# Fertilizer & Soils

> **Status:** ✅ Fertilizer **shipped** (#1402) · ✅ Humus **shipped** (#1404) · 🚧 bog-earth maturity planned (wave 2) · **Phase:** 2 — Forestry · **Module:** `logistics-forestry` (`forestry` domain); Bog Earth stays in `core`
> **Source:** [`../mods/forestry.md`](../mods/forestry.md) (Fertilizer, Humus / compost, Peat bog / peat) · **Depends on:** [`0201-farm-framework.md`](0201-farm-framework.md) (fertilizer slot, charge model) · **Maps to (roadmap):** Phase 2 — Farms (fertilizer chain on existing Apatite)

The consumables and soils the farms run on: a **Fertilizer** item spent as growth charges, a **Humus** soil block that speeds saplings, and a wet-only **maturity** state for the existing Bog Earth that the Peat Bog farm harvests.

## Problem & goal

Apatite (ore, gem, dust, valve), Saltpeter, Sawdust and Pulped Biomass already exist, but nothing turns them into farming inputs: Apatite Dust has no consumer, and Bog Earth only smelts into Peat.

**Goal:** close the farming loop with a minimal set of new items that reuse existing materials, without new worldgen ([RFC 0004](../rfcs/0004-worldgen-stability.md)).

## Requirements

### Functional
- **Fertilizer** (item, farm consumable):
  - Bench recipe: Apatite + 2 Sand → 4.
  - Richer bench recipe: Apatite Dust + Saltpeter + Pulped Biomass → 8. This gives Apatite Dust its first consumer.
  - Each Fertilizer item gives the farm *N* growth charges; **bone meal is accepted at 1 charge**.
  - FERTILIZE spends one charge plus RF to apply one bonemeal-equivalent growth step to an immature plant in the plot. Without fertilizer, plants grow at the vanilla rate.
- **Humus** (block, soil):
  - Recipe: dirt + pulped biomass + sawdust → Humus. Pulped biomass plays the role of Forestry's mulch.
  - Saplings on Humus get extra growth ticks.
  - Each tree grown has a chance to degrade the Humus back to dirt.
  - The [Arboretum](0203-arboretum.md) re-lays Humus from its soil slots.
- **Bog Earth maturity** (wave 2):
  - Adds a `maturity` property with a default, so shipped worlds load unchanged.
  - Bog earth matures only while wet. Mature bog earth drops Peat and dirt.
  - The existing smelting route stays.

### Balance
- *N* charges per Fertilizer, the Humus boost and its degradation chance are config keys; anchor them so Fertilizer is clearly better than bone meal per item and Humus is a worthwhile but consumable upgrade over dirt.

## Design sketch

- **Fertilizer:** a plain item registered in `LogisticsForestry`; the charge count lives in the farm's fertilizer handling in `FarmComponent` ([0201](0201-farm-framework.md)), so the charge math is pure and JUnit-tested.
- **Humus:** goes in **`#minecraft:dirt`**. That works on both versions: on 26.x `dirt` ⊂ `substrate_overworld` ⊂ `supports_vegetation`, and trunk placers keep it. The boost needs no mixins: Humus random-ticks itself and calls `SaplingBlock.advanceTree` (public on both versions) on the sapling above.
- **Bog Earth:** today a plain `new Block(...)` in `LogisticsCore`. Maturity needs a real block class, blockstate variants and a loot-table update; its default state keeps old worlds loading.

## Scope & non-goals

- **In:** Fertilizer + both recipes, the charge model, Humus + recipe + sapling boost + degradation, Bog Earth maturity.
- **Out:** compost bins or a Fermenter route to fertilizer (the processing chain is deferred), new ores, a Moistener.

## Open questions

- Values of *N* charges per Fertilizer and the Humus boost / degradation chance — tune in playtest.
- How fast wet Bog Earth matures, and how many maturity stages it has.

## Done when

- Both Fertilizer recipes craft; a farm spends charges from Fertilizer and from bone meal at 1 charge (JUnit for the charge math).
- Humus is in `#minecraft:dirt`, trees grow on it on both loaders, saplings on it grow faster, and it can degrade to dirt.
- Old worlds with Bog Earth load unchanged; wet bog earth matures and drops Peat + dirt; smelting still works.

## References

- Breakdown: [`../mods/forestry.md`](../mods/forestry.md) → Fertilizer, Humus / compost, Peat bog / peat
- Roadmap: [`../delivery-plan.md`](../delivery-plan.md) → Phase 2 → Farms
- Code: Apatite, Saltpeter, Sawdust, Pulped Biomass and Bog Earth registrations in `LogisticsCore`
- Related: [0201 Farm Framework](0201-farm-framework.md), [0203 Arboretum](0203-arboretum.md), [0205 Peat Bog](0205-peat-bog.md)
