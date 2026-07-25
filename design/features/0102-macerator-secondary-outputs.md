# Macerator Secondary / Byproduct Outputs

> **Status:** ✅ **Shipped** (v0.8.0, #643) — chance-based byproduct output on ore→dust recipes (one byproduct per recipe, resolved as a `ChanceOutput`); shown in the Macerator's secondary-chance slot · **Phase:** 1 — Automation core · **Module:** `logistics-automation` (`core` domain — Macerator)
> **Source:** [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md) (Pulverizer secondary output) · **Depends on:** nothing
> **Maps to (roadmap):** Phase 1 — macerator outputs · **Reused by:** [`0104-sawmill.md`](0104-sawmill.md) (sawdust), [`0105-alloy-smelter.md`](0105-alloy-smelter.md) (slag)

The classic Pulverizer didn't just double ore — it had a *chance* at a bonus byproduct (e.g. ore → 2 dust + sometimes a second metal's dust or a gem). The Macerator currently produces a single deterministic output. This adds the chance-based second output, and in doing so builds a **reusable chance-output mechanism** the Sawmill and Alloy Smelter also want.

## Problem & goal

Ore doubling is in; the *byproduct economy* that made TE ore processing interesting is not. Adding a secondary output makes processing chains worth optimizing (more uptime = more rare byproducts) and gives a natural sink for cross-metal byproducts.

**Goal:** per-recipe optional secondary output with an independent drop chance, surfaced in JEI, balanced to enrich (not trivialize) ore yield.

## Requirements

### Functional
- A Macerator recipe may declare **zero or one secondary output**: an item + a `chance` in `[0,1]`.
- **Dedicated secondary output slot.** The Macerator gains a third slot: input (0) + primary output (1) + **secondary output (2)**. The secondary never shares or blocks the primary.
- **Roll is per-operation.** On completion the primary is produced as today; the secondary's `chance` is rolled independently — success deposits it in slot 2, failure leaves slot 2 untouched.
- **Pause-until-clear (no byproduct lost).** An operation completes only when **both** the primary *and* the secondary slot can accept their results; if the secondary slot is full, processing **pauses** (progress holds, input is not consumed) until it drains. The roll still decides whether the secondary is actually deposited, but the space is required first — so a backed-up secondary stalls primary ore-doubling (the accepted trade for never losing a byproduct).
- JEI shows the secondary with its chance (e.g. "25%").
- **Backward compatible:** recipes without a `secondary` block only use slot 1; slot 2 stays empty and never gates. Existing saved Macerators (2-slot inventory) **resize to 3 slots** on load, the new secondary slot empty.

### Balance
- Secondary chances modest (TE-era anchor: ~5–25% per operation, rare byproducts lower).
- Byproducts should be *useful but not primary sources* — a way to get small amounts of an off-metal, not to replace mining it.
- No change to grinding time or energy cost for having a secondary (keep the knob count low for v1).

## Design sketch

Extend the recipe data model, the slot layout, the GUI, and the pure processing logic — the recipe and plan are already isolated for testing.

- **`MaceratorRecipeWrapper`** (`core/macerator/MaceratorRecipeWrapper.java`): add optional fields `ItemStackTemplate secondaryResult` + `float secondaryChance` (default empty/0). Update the `MapCodec`/`StreamCodec` in `MaceratorRecipeSerializer` to read an optional `secondary: { id, count, chance }` object.
- **Block entity slots** (`MaceratorBlockEntity`): grow `ItemInventoryComponent` from 2 → **3 slots** (in / primary / secondary). `WorldlyContainer`: top + sides → input (slot 0); **bottom → both outputs (slots 1 *and* 2)** for extraction. Handle loading legacy 2-slot NBT (pad to 3).
- **`MaceratorProcessingPlan`** (`core/macerator/MaceratorProcessingPlan.java`): the completion gate's `acceptsOutput` must now check **both** the primary and secondary slots have room (this is what implements pause-until-clear). The *roll* stays deterministic-testable — pass a random source (or a pre-rolled boolean) in rather than calling `level.random` inside the plan, so unit tests assert both branches. The plan reports "secondary produced: yes/no"; the BE applies it to slot 2.
- **GUI** (`MaceratorScreenHandler` + client `MaceratorScreen`): add the second output slot and a **chance bar/label**; this is a visible texture/layout change to the Macerator screen.
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

**Generalize:** factor the "optional chance-based secondary result" into a small shared record (e.g. `core/lib/recipe/ChanceResult`) so the Sawmill and Alloy Smelter recipe types embed the same field with identical JSON shape and JEI rendering. The **dedicated-secondary-slot + pause-until-clear** shape generalizes too — Sawmill (sawdust) and Alloy Smelter (slag) adopt the same 3-slot layout and both-outputs completion gate.

## Scope & non-goals

- **In:** one optional secondary output per recipe, independent per-operation chance, the dedicated secondary slot + GUI change, pause-until-clear semantics, JEI display, the shared `ChanceResult` helper, legacy 2→3 slot migration.
- **Deferred (not blocking):** the actual **byproduct content map** (which ore gives which off-metal, and at what %) — lands alongside the alloy/material set in [`0105-alloy-smelter.md`](0105-alloy-smelter.md). This feature ships the mechanism plus one or two illustrative secondaries using existing items.
- **Out:** multiple secondaries, fortune/luck scaling, upgrade-modified chances (that's [`0106-machine-upgrades.md`](0106-machine-upgrades.md) — the "secondary" augment hooks here later), per-output sided routing (both outputs share the bottom face).

## Decisions

All mechanism-level questions are settled — the byproduct *content* is the only deferred piece, and it's data, not a blocker:

- **Output slots** — **dedicated secondary slot** (in / primary / secondary = 3 slots), TE-style. Secondary never blocks primary; GUI gains a second output + chance bar; both outputs extract from the bottom face.
- **Slot-full behavior** — **pause-until-clear**: an op completes only when both outputs have room, so a full secondary slot stalls processing rather than dropping the byproduct. No byproduct is ever lost.
- **Roll model** — **per-operation**, independent chance, deterministic-testable (roll passed into `MaceratorProcessingPlan`).
- **Byproduct content** — **deferred to [`0105-alloy-smelter.md`](0105-alloy-smelter.md)**: ship the mechanism + `ChanceResult` + a couple of illustrative secondaries (existing items) now; finalize the per-ore byproduct table with the alloy/material set.

> Remaining choices are implementation details: GUI texture layout for the second slot + chance bar, and the exact illustrative secondaries shipped in v1.

## Done when

- A recipe with a `secondary` block deposits it in slot 2 at the configured chance, verified by unit tests on `MaceratorProcessingPlan` (roll-success and roll-fail branches).
- With the secondary slot full, the Macerator **pauses** (progress holds, input not consumed) and resumes when it drains — no byproduct lost. Unit-tested.
- JEI and the machine GUI show the secondary + chance.
- Legacy recipes (no secondary) and existing saved Macerators load unchanged (inventory pads 2→3 slots).
- The shared `ChanceResult` helper is in `core.lib` and consumed by the Macerator (Sawmill/Alloy adopt it in their docs).

## References

- Roadmap: [`../delivery-plan.md`](../delivery-plan.md) → Phase 1 → macerator outputs
- Breakdown: [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md) → "secondary/byproduct outputs" row
- **TE sourcing note:** Thermal Expansion has **no public source repo** — Pulverizer behavior (dedicated secondary slot, per-op chance, ~5–25% range) is reconstructed from knowledge + the TE wiki. Treat exact chances as *approximate* and tune in playtesting; do not cite source line numbers.
- Code: `core/macerator/{MaceratorRecipeWrapper,MaceratorRecipeSerializer,MaceratorProcessingPlan,MaceratorRecipeDisplay}.java`, `core/macerator/jei/MaceratorRecipeCategory.java`, recipes in `data/logistics/recipe/macerator/`
