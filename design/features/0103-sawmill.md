# Sawmill

> **Status:** 🚧 Planned · **Phase:** 1 — Automation core · **Module:** `logistics-automation` (`automation` domain)
> **Source:** [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md) (Sawmill) · **Depends on:** [`0102-macerator-secondary-outputs.md`](0102-macerator-secondary-outputs.md) (`ChanceResult` for sawdust)
> **Maps to (roadmap):** Phase 1 — Sawmill

A single-input RF machine: logs → extra planks + a chance of sawdust. The lowest-complexity new machine in Phase 1 — effectively the Macerator pattern with a wood-flavored recipe set. Wood Pulp / Flour dusts already exist; this gives them a primary production machine.

## Problem & goal

Vanilla gives 4 planks per log. The classic Sawmill gave *more* (≈6) plus sawdust, making it the early wood-efficiency machine and the entry point to a wood-byproduct chain (sawdust → paper/compressed blocks/etc. later).

**Goal:** a cheap, early RF machine that improves log→plank yield and produces sawdust, reusing the existing machine and secondary-output mechanisms with minimal new surface.

## Requirements

### Functional
- **One input slot** (logs / `#minecraft:logs`-style tag) + **one or two output slots** (planks + optional sawdust).
- **Custom recipe type** `logistics:sawmill`: one ingredient, one primary result (planks, configurable count), optional `ChanceResult` sawdust, a saw time, energy cost.
- Tag-driven recipes so *all* wood types work from a handful of recipes (input via `#minecraft:logs`, output the matching plank) — or a generated recipe per wood. Prefer tag-based to avoid per-wood file sprawl (see Open questions).
- RF-powered with internal buffer; JEI category; progress + energy GUI (reuse Macerator screen).

### Balance
- Yield anchor: log → **6 planks** (vs vanilla 4) + ~50% sawdust, tuned so it's worth the power but not a plank printer.
- Cheapest new machine to build and run — it's an early-game efficiency unlock. Energy draw at/below the Macerator.

## Design sketch

Structurally the **simplest instance of the machine pattern** — closest sibling to the Kiln (single input) but with a custom recipe like the Macerator.

```
common/src/main/java/com/logistics/automation/sawmill/
├── SawmillBlock.java               # extends MachineBlock; FACING + LIT
├── SawmillBlockEntity.java         # extends BaseBlockEntity; HasItemStorage, HasEnergyStorage,
│                                   #   WorldlyContainer, MenuBehavior.HasMenu
├── SawmillRecipe.java              # Recipe<SingleRecipeInput>; ingredient, result, ChanceResult sawdust, time
├── SawmillRecipeSerializer.java
├── SawmillProcessingPlan.java      # pure logic, unit tested
├── SawmillScreenHandler.java
└── (client) SawmillScreen.java + jei/{Category,Plugin}
```

- Register in `LogisticsAutomation` alongside Kiln.
- Reuse the shared `ChanceResult` (from [`0102-macerator-secondary-outputs.md`](0102-macerator-secondary-outputs.md)) for sawdust.
- Recipe JSON (tag input, single recipe covers a wood family if outputs are resolved by tag — otherwise one per wood):
  ```json
  {
    "type": "logistics:sawmill",
    "ingredient": "#minecraft:oak_logs",
    "result": { "id": "minecraft:oak_planks", "count": 6 },
    "sawdust": { "id": "logistics:core/sawdust", "count": 1, "chance": 0.5 },
    "sawtime": 160
  }
  ```

## Scope & non-goals

- **In:** the machine, log→plank recipes for vanilla woods, sawdust byproduct, JEI, GUI.
- **Out:** stripping/processing non-log wood, treated wood / creosote (that's Railcraft, Phase 3 — though sawdust may feed it), upgrades (separate doc), modded-wood auto-coverage beyond what tags give.

## Open questions

- **Per-wood recipes vs. one tag-driven recipe.** A single recipe keyed on `#minecraft:logs` can't statically know which plank to output. Options: (a) generate one JSON per vanilla wood (simple, finite, what the snippet shows); (b) a smarter recipe/serializer that maps log→plank via a data map. **Lean: generate per vanilla wood for v1; revisit a data-map approach if modded-wood coverage matters.**
- Final yield numbers (6 planks? scaling with upgrades later?) — pick a balance anchor.
- Define `sawdust` item + its downstream use, or reuse existing Wood Pulp / Flour dust. **Lean: reuse the existing wood dust rather than add a new item.**

## Done when

- Logs of each vanilla wood saw into the configured planks + chance sawdust on both loaders.
- Sawdust chance is unit-tested in the processing plan.
- JEI lists sawmill recipes; GUI shows progress + energy.

## References

- Roadmap: [`../roadmap.md`](../roadmap.md) → Phase 1 → Sawmill
- Breakdown: [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md) → Sawmill row (notes existing Wood Pulp/Flour dusts)
- Code pattern: `automation/kiln/*`, `core/macerator/*`; registration in `LogisticsAutomation.java`
- Shared mechanism: [`0102-macerator-secondary-outputs.md`](0102-macerator-secondary-outputs.md) `ChanceResult`
