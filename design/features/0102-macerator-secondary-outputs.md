# Macerator Secondary / Byproduct Outputs

> **Status:** 🚧 Planned · **Phase:** 1 — Automation core · **Module:** `logistics-automation` (`core` domain — Macerator)
> **Source:** [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md) (Pulverizer secondary output) · **Depends on:** nothing
> **Maps to (roadmap):** Phase 1 — macerator outputs · **Reused by:** [`0103-sawmill.md`](0103-sawmill.md) (sawdust), [`0104-alloy-smelter.md`](0104-alloy-smelter.md) (slag)

The classic Pulverizer didn't just double ore — it had a *chance* at a bonus byproduct (e.g. ore → 2 dust + sometimes a second metal's dust or a gem). The Macerator currently produces a single deterministic output. This adds the chance-based second output, and in doing so builds a **reusable chance-output mechanism** the Sawmill and Alloy Smelter also want.

## Problem & goal

Ore doubling is in; the *byproduct economy* that made TE ore processing interesting is not. Adding a secondary output makes processing chains worth optimizing (more uptime = more rare byproducts) and gives a natural sink for cross-metal byproducts.

**Goal:** per-recipe optional secondary output with an independent drop chance, surfaced in JEI, balanced to enrich (not trivialize) ore yield.

## Requirements

### Functional
- A Macerator recipe may declare **zero or one secondary output**: an item + a `chance` in `[0,1]`.
- On completion, the primary output is produced as today; the secondary is rolled independently and, if it succeeds, placed in (or merged into) the output region.
- Output handling must not deadlock: if the secondary can't be placed (output full / mismatched), define behavior — **recommended:** the recipe still completes and the secondary is *lost* unless a dedicated secondary slot is free. (Decide: single shared output slot vs. add a second output slot — see Open questions.)
- JEI shows the secondary with its chance (e.g. "25%").
- Fully backward compatible: recipes without a `secondary` block behave exactly as now.

### Balance
- Secondary chances modest (TE-era anchor: ~5–25% per operation, rare byproducts lower).
- Byproducts should be *useful but not primary sources* — a way to get small amounts of an off-metal, not to replace mining it.
- No change to grinding time or energy cost for having a secondary (keep the knob count low for v1).

## Design sketch

Extend the recipe data model and the pure processing logic — both already isolated for testing.

- **`MaceratorRecipeWrapper`** (`core/macerator/MaceratorRecipeWrapper.java`): add optional fields `ItemStackTemplate secondaryResult` + `float secondaryChance` (default empty/0). Update the `MapCodec`/`StreamCodec` in `MaceratorRecipeSerializer` to read an optional `secondary: { id, count, chance }` object.
- **`MaceratorProcessingPlan`** (`core/macerator/MaceratorProcessingPlan.java`): the *roll* should stay deterministic-testable — pass a random source (or a pre-rolled boolean) in rather than calling `level.random` inside the plan, so unit tests can assert both branches. The plan reports "secondary produced: yes/no"; the BE applies it to the inventory.
- **`MaceratorRecipeDisplay`** + **JEI category** (`core/macerator/jei/MaceratorRecipeCategory.java`): render the secondary slot with a chance label.
- **Recipe JSON** (`data/logistics/recipe/macerator/*.json`): new optional shape —
  ```json
  {
    "type": "logistics:macerator",
    "ingredient": "...",
    "result": { "id": "...", "count": 2 },
    "secondary": { "id": "...", "count": 1, "chance": 0.15 },
    "grindingtime": 200
  }
  ```

**Generalize:** factor the "optional chance-based secondary result" into a small shared record (e.g. `core/lib/recipe/ChanceResult`) so the Sawmill and Alloy Smelter recipe types embed the same field with identical JSON shape and JEI rendering.

## Scope & non-goals

- **In:** one optional secondary output per recipe, independent chance, JEI display, the shared `ChanceResult` helper.
- **Out:** multiple secondaries, fortune/luck scaling, upgrade-modified chances (that's [`0105-machine-upgrades.md`](0105-machine-upgrades.md) — the "secondary" augment hooks here later), per-output sided routing.

## Open questions

- **Output slots:** keep the single output slot (secondary must stack with / wait behind primary) or add a dedicated secondary output slot? A dedicated slot is cleaner and matches TE, but changes the GUI and `WorldlyContainer` face mapping. **Lean: add a second output slot.**
- Confirm the exact byproduct *items* we want (needs the alloy/material set from [`0104-alloy-smelter.md`](0104-alloy-smelter.md) to be settled, or use existing dusts as placeholders).
- Should the roll be per-operation (current plan) or amortized for averaged yields? Per-operation is simpler and more classic.

## Done when

- A recipe with a `secondary` block produces it at the configured chance, verified by unit tests on `MaceratorProcessingPlan` (both branches) and in-game.
- JEI shows the secondary + chance.
- Legacy recipes (no secondary) are unchanged.
- The shared `ChanceResult` helper is in `core.lib` and consumed by at least the Macerator (Sawmill/Alloy adopt it in their docs).

## References

- Roadmap: [`../roadmap.md`](../roadmap.md) → Phase 1 → macerator outputs
- Breakdown: [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md) → "secondary/byproduct outputs" row
- Code: `core/macerator/{MaceratorRecipeWrapper,MaceratorRecipeSerializer,MaceratorProcessingPlan,MaceratorRecipeDisplay}.java`, `core/macerator/jei/MaceratorRecipeCategory.java`, recipes in `data/logistics/recipe/macerator/`
