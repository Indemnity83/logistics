# Alloy Smelter

> **Status:** 🚧 Planned · **Phase:** 1 — Automation core · **Module:** `logistics-automation` (`automation` domain)
> **Source:** [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md) (Induction Smelter) · **Depends on:** [`0102-macerator-secondary-outputs.md`](0102-macerator-secondary-outputs.md) (shares the `ChanceResult` slag mechanism)
> **Maps to (roadmap):** Phase 1 — Alloy Smelter

A two-input RF machine that combines metals into alloys (Bronze, Invar, Electrum, …), optionally with a chance of slag. This is the machine that makes the alloy material set *coherent* — right now Bronze exists but has no dedicated production machine. Pairs directly with the materials work in [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md).

## Problem & goal

The classic mid-game ran on alloys, and alloys need an alloying machine. Without one, every alloy is a crafting-table recipe with no progression weight. The Alloy Smelter gives alloys a machine + energy cost, anchors a tight metal set, and creates demand for the secondary metals the Macerator can drop.

**Goal:** a single-block, two-input RF alloying machine reusing the established machine pattern (Macerator/Kiln), producing a defined alloy set, with optional slag byproduct.

## Requirements

### Functional
- **Two input slots** + **one (or two) output slots** (output + optional slag). Sided access: inputs from top/sides, output from bottom (match Macerator/Kiln `WorldlyContainer` mapping).
- **Custom recipe type** `logistics:alloy_smelter`: two ingredients (each with a count), one primary result, optional `ChanceResult` slag, a smelt time, energy cost.
- Recipe matching is **order-independent** for the two inputs.
- RF-powered with an internal buffer; consumes energy per tick while smelting (same shape as Macerator's `ENERGY_PER_TICK`).
- JEI category showing input A + input B → result (+ slag chance).
- Recipe book / GUI with progress + energy bars (reuse the Macerator screen layout).

### Balance
- Energy/time anchored to the Macerator (10,000 RF buffer, ~128 RF/t intake, ~10 RF/t draw, ~200-tick op) but a touch costlier — alloying is a step up from grinding.
- **Alloy set kept tight** (per [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md)): Bronze (done), Invar, Electrum, maybe Constantan. Add a metal only when an alloy needs it; avoid the full TE metal sprawl. Signalum/Lumium/Enderium are TBD/out for v1.
- Input ratios mirror classic alloying (e.g. Bronze = 3 copper : 1 tin), tuned to vanilla copper availability.

## Design sketch

Follow the machine "shape" exactly as Macerator/Kiln (verified pattern):

```
common/src/main/java/com/logistics/automation/alloysmelter/
├── AlloySmelterBlock.java            # extends MachineBlock; FACING + LIT; particles when active
├── AlloySmelterBlockEntity.java      # extends BaseBlockEntity
│                                     #   implements HasItemStorage, HasEnergyStorage,
│                                     #   WorldlyContainer, MenuBehavior.HasMenu
│                                     #   fields: ItemInventoryComponent (2 in + 1–2 out),
│                                     #   EnergyComponent, ContainerData
├── AlloySmelterRecipe.java           # implements Recipe<…>; two ingredients, result, ChanceResult slag, time
├── AlloySmelterRecipeSerializer.java # MapCodec + StreamCodec
├── AlloySmelterProcessingPlan.java   # pure logic (advance/consume/complete) — unit tested
├── AlloySmelterScreenHandler.java    # extends RecipeBookMenu; ContainerData getters
└── (client) AlloySmelterScreen.java + jei/{Category,Plugin}
```

- Register block/item/BE/menu/recipe-type/serializer in **`LogisticsAutomation`** (same place Kiln registers), following `registerBlockWithItem` / `registerBlockEntity` / `registerMenuType` helpers.
- The two-input recipe input is a custom `RecipeInput` (not vanilla `SingleRecipeInput`) holding both slots; `matches()` tries both input orderings.
- Reuse the shared `ChanceResult` from [`0102-macerator-secondary-outputs.md`](0102-macerator-secondary-outputs.md) for slag — build that first or in lockstep.
- Recipe JSON:
  ```json
  {
    "type": "logistics:alloy_smelter",
    "inputs": [
      { "ingredient": "#c:ingots/copper", "count": 3 },
      { "ingredient": "logistics:core/tin_ingot", "count": 1 }
    ],
    "result": { "id": "logistics:core/bronze_ingot", "count": 4 },
    "slag": { "id": "logistics:core/slag", "count": 1, "chance": 0.1 },
    "smelttime": 200
  }
  ```

## Scope & non-goals

- **In:** the machine, a tight alloy recipe set, optional slag, JEI, GUI.
- **Out:** fluid inputs/outputs (TE's smelter took some fluids — not in v1), the full TE metal roster, high-tier alloys (Signalum/Lumium/Enderium — TBD), machine tiers (separate: [`0105-machine-upgrades.md`](0105-machine-upgrades.md)).
- **Out:** replacing existing crafting-table alloy recipes silently — decide whether the smelter is the *only* alloy source or an efficient alternative (see Open questions).

## Open questions

- **Is the Alloy Smelter the sole alloy source, or a cheaper/faster path alongside crafting?** Sole-source gives it real weight but gates early alloys behind a machine + power; alternative-path is gentler. **Lean: sole-source for new alloys (Invar/Electrum), keep Bronze also craftable early so power isn't a hard gate to enter the tier.**
- Which alloys ship in v1, and their exact ratios/byproducts (needs the materials decision in TE breakdown).
- One output slot or two (separate slag slot)? Tie to the same decision in [`0102-macerator-secondary-outputs.md`](0102-macerator-secondary-outputs.md).
- Does slag have a use yet, or is it a placeholder sink? If no use, maybe omit slag for v1 and add with machine tiers.

## Done when

- Block places, rotates, smelts two inputs → alloy on both loaders, persists across save/load.
- Optional slag drops at configured chance (unit-tested in the processing plan).
- JEI lists alloy recipes; GUI shows progress + energy.
- At least Bronze + Invar + Electrum recipes exist and are reachable on the progression curve.

## References

- Roadmap: [`../roadmap.md`](../roadmap.md) → Phase 1 → Alloy Smelter; materials rows in [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md)
- Code pattern: `automation/kiln/*` and `core/macerator/*` (block, BE, recipe, serializer, processing plan, screen, JEI); registration in `LogisticsAutomation.java`
- Shared mechanism: [`0102-macerator-secondary-outputs.md`](0102-macerator-secondary-outputs.md) `ChanceResult`
