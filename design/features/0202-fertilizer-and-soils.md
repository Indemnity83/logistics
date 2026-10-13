# Fertilizer & Soils

> **Status:** ✅ Fertilizer **shipped** (#1402) · ✅ Humus **shipped** (#1404) · 🚧 bog-earth maturity planned (wave 2) · **Phase:** 2 — Forestry · **Module:** `logistics-forestry` (`forestry` domain); Bog Earth stays in `core`
> **Source:** [`../mods/forestry.md`](../mods/forestry.md) (Fertilizer, Humus / compost, Peat bog / peat) · **Depends on:** [`0201-farm-framework.md`](0201-farm-framework.md) · **Maps to (roadmap):** Phase 2 — Farms (fertilizer chain on existing Apatite)

The consumables and soils around the farms: a **Fertilizer** item kept for a future machine augmentation, a **Humus** soil block that speeds saplings and the Arboretum lays, and a wet-only **maturity** state for the existing Bog Earth that the Peat Bog farm harvests.

## Problem & goal

Apatite (ore, gem, dust, valve), Saltpeter, Sawdust and Pulped Biomass already exist, but nothing turns them into farming inputs: Apatite Dust has no consumer, and Bog Earth only smelts into Peat.

**Goal:** close the farming loop with a minimal set of new items that reuse existing materials, without new worldgen ([RFC 0004](../rfcs/0004-worldgen-stability.md)).

## Requirements

### Functional
- **Fertilizer** (item):
  - Bench recipe: Apatite + 2 Sand → 4.
  - Richer bench recipe: Apatite Dust + Saltpeter + Pulped Biomass → 8. This gives Apatite Dust its first consumer.
  - **Kept as an item for a future machine augmentation and is not used by farms.** Farms have no fertilizer slot; plants in them grow at the vanilla rate. The item, its recipes and its tag stay so it can return as a speed augmentation with [0106](0106-machine-upgrades.md).
- **Humus** (block, soil):
  - Recipe: dirt + pulped biomass + sawdust → Humus. Pulped biomass plays the role of Forestry's mulch.
  - Saplings on Humus get extra growth ticks.
  - **Humus turns to sand once a tree grows on it:** when its own boost grows the tree, on any random tick that finds wood above it, and under every trunk cell the Arboretum fells (all four for a 2×2 tree).
  - The [Arboretum](0203-arboretum.md) accepts Humus only: it digs the sand out into its output and lays fresh Humus from its soil slots.
- **Bog Earth maturity** (wave 2):
  - Adds a `maturity` property with a default, so shipped worlds load unchanged.
  - Bog earth matures only while wet. Mature bog earth drops Peat and dirt.
  - The existing smelting route stays.

### Balance
- The Humus boost is a config key (`humus_growth_chance`); Humus is consumed once per tree, so it is a worthwhile but consumable soil.

## Design sketch

- **Fertilizer:** a plain item registered in `LogisticsForestry`, with no consumer yet.
- **Humus:** goes in **`#minecraft:dirt`**. That works on both versions: on 26.x `dirt` ⊂ `substrate_overworld` ⊂ `supports_vegetation`, and trunk placers keep it. The boost needs no mixins: Humus random-ticks itself and calls `SaplingBlock.advanceTree` (public on both versions) on the sapling above.
- **Bog Earth:** today a plain `new Block(...)` in `LogisticsCore`. Maturity needs a real block class, blockstate variants and a loot-table update; its default state keeps old worlds loading.

## Scope & non-goals

- **In:** Fertilizer + both recipes, Humus + recipe + sapling boost + humus→sand, Bog Earth maturity.
- **Out:** compost bins or a Fermenter route to fertilizer (the processing chain is deferred), new ores, a Moistener.

## Open questions

- The Humus boost — tune in playtest. How Fertilizer works as an augmentation ([0106](0106-machine-upgrades.md)).
- How fast wet Bog Earth matures, and how many maturity stages it has.

## Done when

- Both Fertilizer recipes craft.
- Humus is in `#minecraft:dirt`, trees grow on it on both loaders, saplings on it grow faster, and it turns to sand under a grown tree.
- Old worlds with Bog Earth load unchanged; wet bog earth matures and drops Peat + dirt; smelting still works.

## References

- Breakdown: [`../mods/forestry.md`](../mods/forestry.md) → Fertilizer, Humus / compost, Peat bog / peat
- Roadmap: [`../delivery-plan.md`](../delivery-plan.md) → Phase 2 → Farms
- Code: Apatite, Saltpeter, Sawdust, Pulped Biomass and Bog Earth registrations in `LogisticsCore`
- Related: [0201 Farm Framework](0201-farm-framework.md), [0203 Arboretum](0203-arboretum.md), [0205 Peat Bog](0205-peat-bog.md)
