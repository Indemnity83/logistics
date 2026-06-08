# Sawmill

> **Status:** 🚧 Planned — **design settled, ready to build** · **Phase:** 1 — Automation core · **Module:** `logistics-automation` (`automation` domain)
> **Source:** [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md) (Sawmill) · **Depends on:** [`0102-macerator-secondary-outputs.md`](0102-macerator-secondary-outputs.md) (`ChanceResult` for sawdust)
> **Maps to (roadmap):** Phase 1 — Sawmill

A single-input RF machine: logs → extra planks + a chance of sawdust. The lowest-complexity new machine in Phase 1 — effectively the Macerator pattern with a wood-flavored recipe set. Wood Pulp / Flour dusts already exist; this gives them a primary production machine.

## Problem & goal

Vanilla gives 4 planks per log. The classic Sawmill gave *more* (≈6) plus sawdust, making it the early wood-efficiency machine and the entry point to a wood-byproduct chain (sawdust → paper/compressed blocks/etc. later).

**Goal:** a cheap, early RF machine that improves log→plank yield and produces sawdust, reusing the existing machine and secondary-output mechanisms with minimal new surface.

## Requirements

### Functional
- **Slots (inherits 0102):** one input (a log) + **primary output (planks)** + **dedicated secondary output (Wood Pulp)** = 3 slots. Same `ChanceResult` + **pause-until-clear** semantics as the Macerator — a full Wood Pulp slot stalls processing, no byproduct lost. Top/sides → input (slot 0); bottom → both outputs (slots 1 & 2).
- **Custom recipe type** `logistics:sawmill`: one ingredient, one primary result (planks, configurable count), optional `ChanceResult` **Wood Pulp** byproduct, a saw time, energy cost.
- **One recipe per vanilla wood** — explicit JSON keyed on each wood's log tag (`#minecraft:oak_logs` → oak planks, etc.), covering oak…cherry plus crimson/warped stems (~9 recipes). Using the per-wood log *tag* as the ingredient neatly covers stripped/wood variants. Modded woods work only if that mod/pack ships its own `logistics:sawmill` recipe — it's just data.
- **Byproduct reuses the existing Wood Pulp item** (`logistics:core/wood_pulp`) — no new sawdust item.
- RF-powered with internal buffer; JEI category; progress + energy GUI (reuse Macerator screen).

### Balance
- Yield anchor: log → **6 planks** (vs vanilla 4) + a **modest Wood Pulp chance (~25–50%)**, tuned so it's worth the power but not a plank printer. *Numbers approximate — TE has no public source (wiki/knowledge); tune in playtest.* Upgrade-scaling is [`0105-machine-upgrades.md`](0105-machine-upgrades.md)'s job.
- Cheapest new machine to build and run — it's an early-game efficiency unlock. Energy draw at/below the Macerator.

## Design sketch

Structurally the **simplest instance of the machine pattern** — closest sibling to the Kiln (single input) but with a custom recipe like the Macerator.

```text
common/src/main/java/com/logistics/automation/sawmill/
├── SawmillBlock.java               # extends MachineBlock; FACING + LIT
├── SawmillBlockEntity.java         # extends BaseBlockEntity; HasItemStorage, HasEnergyStorage,
│                                   #   WorldlyContainer, MenuBehavior.HasMenu; 3 slots (in/planks/wood-pulp)
├── SawmillRecipe.java              # Recipe<SingleRecipeInput>; ingredient, result, ChanceResult wood pulp, time
├── SawmillRecipeSerializer.java
├── SawmillProcessingPlan.java      # pure logic, unit tested
├── SawmillScreenHandler.java
└── (client) SawmillScreen.java + jei/{Category,Plugin}
```

- Register in `LogisticsAutomation` alongside Kiln. Adopt the same 3-slot + completion-gate shape resolved for the Macerator.
- Reuse the shared `ChanceResult` (from [`0102-macerator-secondary-outputs.md`](0102-macerator-secondary-outputs.md)) for the Wood Pulp byproduct.
- Recipe JSON — one per vanilla wood, keyed on the wood's log tag:
  ```json
  {
    "type": "logistics:sawmill",
    "ingredient": "#minecraft:oak_logs",
    "result": { "id": "minecraft:oak_planks", "count": 6 },
    "byproduct": { "id": "logistics:core/wood_pulp", "count": 1, "chance": 0.5 },
    "sawtime": 160
  }
  ```

## Scope & non-goals

- **In:** the machine, one log→plank recipe per vanilla wood, the Wood Pulp byproduct (dedicated slot + pause-until-clear), JEI, GUI.
- **Out:** stripping/processing non-log wood, treated wood / creosote (that's Railcraft, Phase 3 — though Wood Pulp may feed it), upgrades (separate doc), automatic modded-wood coverage (modded woods need their own data-pack recipe).

## Decisions

All start-blocking questions are settled:

- **Recipe model** — **one explicit JSON per vanilla wood**, keyed on the wood's log tag (covers stripped/wood variants). Modded woods are covered only via their own data-pack recipes — no runtime derivation or datamap in v1.
- **Byproduct item** — **reuse the existing Wood Pulp** (`logistics:core/wood_pulp`); no new sawdust item.
- **Secondary-output mechanics** — **inherit 0102 verbatim**: dedicated secondary slot + `ChanceResult` + pause-until-clear.
- **Yield** — anchor **6 planks + ~25–50% Wood Pulp**, approximate (TE wiki/knowledge), tune in playtest; upgrade-scaling handled by `0105`.

> Remaining choices are implementation details: the final per-wood plank count / Wood Pulp %, saw time, and the recipe-field name (`byproduct`) shared with the `ChanceResult` helper.

## Done when

- Logs of each vanilla wood saw into the configured planks + chance Wood Pulp on both loaders.
- The Wood Pulp roll (both branches) and **pause-until-clear** (full byproduct slot stalls, nothing lost) are unit-tested in the processing plan.
- JEI lists sawmill recipes; GUI shows progress + energy and the secondary slot + chance.

## References

- Roadmap: [`../roadmap.md`](../roadmap.md) → Phase 1 → Sawmill
- Breakdown: [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md) → Sawmill row (notes existing Wood Pulp/Flour dusts)
- **TE sourcing note:** TE has **no public source** — yields are wiki/knowledge-based, approximate; tune in playtest. Existing items confirmed in `LogisticsCore.java`: `WOOD_PULP` (`logistics:core/wood_pulp`), `FLOUR`.
- Code pattern: `automation/kiln/*`, `core/macerator/*`; registration in `LogisticsAutomation.java`
- Shared mechanism: [`0102-macerator-secondary-outputs.md`](0102-macerator-secondary-outputs.md) `ChanceResult`
