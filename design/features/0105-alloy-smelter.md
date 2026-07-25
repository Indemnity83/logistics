# Alloy Smelter

> **Status:** ✅ **Shipped** (v0.8.2, #656) — the Alloy Smelter with a custom `logistics:alloy_smelter` recipe type; produces **Bronze** today (Invar/Electrum not yet added — introduce only as a tier needs them) · **Phase:** 1 — Automation core · **Module:** `logistics-automation` (`automation` domain) + materials in `core`
> **Source:** [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md) (Induction Smelter) · **Depends on:** [`0102-macerator-secondary-outputs.md`](0102-macerator-secondary-outputs.md) (Nickel as an iron-ore Macerator byproduct) · **Companion:** [`0103-hand-grinder.md`](0103-hand-grinder.md) — manual, powerless ore→dust; ungates the dust/alloy economy from energy (built early — prerequisite for the no-power Bronze path)
> **Maps to (roadmap):** Phase 1 — Alloy Smelter; materials rows in [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md)

A two-input RF machine that combines two metals into an alloy. This feature is **machine + a small materials expansion**: it adds **Nickel + Invar** (the first new metal/alloy since Bronze), introduces an **alloy-via-dust crafting path** so alloys feel like *alloying* and aren't hard-gated on power, and gives alloys a machine + energy cost as the automation tier. Right now Bronze exists but has no production machine; this makes the alloy set coherent. Pairs with the materials rows in [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md).

## Problem & goal

The classic mid-game ran on alloys, but alloying should *feel* like combining metals — not a flat crafting-table recipe, and not something that hard-gates an early alloy (Bronze) behind having power first.

The model (decided): **alloys are made by combining two component *dusts*.**
- **By hand:** `component-A dust + component-B dust → alloy dust` in the crafting grid (no ingot+ingot shortcut), then smelt the alloy dust → ingot. Dusts come from the Macerator (powered) **or the Hand Grinder (manual, powerless)** — so Bronze is reachable without energy.
- **Automated:** the **Alloy Smelter** does the combination directly and continuously, accepting either the dust pair or the ingot pair → alloy.

**Goal:** a single-block, two-input RF alloying machine (the automation tier) plus the dust-crafting path and the **Nickel + Invar** materials that give the alloy set somewhere to grow.

## Requirements

### Functional
- **Two input slots + a 3-slot shared output inventory.** Outputs (the alloy *and* any chanced secondary) fill the shared output region wherever there's room — *not* a rigid one-primary/one-secondary split. Sizing rationale: a full input load (e.g. 64 + 64) runs many ops and yields **two stacks of alloy** (≈128), so two slots buffer the primary and the third leaves room for a chanced byproduct. Uses 0102's **`ChanceResult` + pause-until-clear** (an op completes only when the output inventory has room for its results; otherwise progress holds, nothing lost), generalized from a dedicated slot to the shared buffer. **v1 ships no recipe that produces a secondary** (the capability is built/tested, then dormant; see below). Sided access: inputs from top/sides; bottom → the whole output inventory.
- **Custom recipe type** `logistics:alloy_smelter`: two ingredients (each with a count), one result, an **optional `ChanceResult` secondary** (schema present, unused by any v1 recipe), a smelt time, energy cost. Recipe matching is **order-independent** for the two inputs.
- **Accepts dusts or ingots:** the smelter has recipes for both `dust + dust → alloy dust` and `ingot + ingot → alloy ingot` (output in kind). The grid only supports the dust form (below).
- RF-powered with an internal buffer; consumes energy per tick while smelting (same shape as Macerator's `ENERGY_PER_TICK`).
- **Hand path (no machine):** grid recipes `component-A dust + component-B dust → alloy dust` (e.g. copper dust + tin dust → bronze dust; iron dust + nickel dust → invar dust). **No ingot+ingot grid recipe.** Alloy dust smelts to ingot in a furnace/Kiln.
- **New materials:** add **Nickel** (`nickel_dust`, `nickel_ingot`, `nickel_nugget`) and **Invar** (`invar_dust`, `invar_ingot`, `invar_nugget`). Nickel is sourced as a **Macerator byproduct of iron ore** (uses 0102's `ChanceResult` — no new worldgen). Invar = Iron + Nickel. **No slag item in v1** (deferred with its use).
- **Invar's use** (extends existing ladders): **Invar Gear** + **Invar Valve** (the next tier above `bronze_gear` / `bronze_valve`), with the dimensional-stability / "engine valve" flavor pointing at future machine frames. *(Confirm — see Decisions.)*
- **Secondary output is dormant:** the shared output buffer + recipe schema exist so a future byproduct (slag — a TE-style recycle/Rich-Slag loop) is a **pure-JSON + add-the-item** change. No slag item, no slag-producing recipe, in v1.
- JEI category (input A + input B → result, plus the secondary chance *when a recipe defines one*); recipe-book GUI with progress + energy bars (reuse the Macerator screen layout, widened to the 3-slot output inventory).

### Balance
- **Bronze is not energy-gated:** Hand Grinder → dusts → grid-craft bronze dust → smelt. The Alloy Smelter is the automation/efficiency tier, not the only door.
- **Invar is (acceptably) energy-gated:** Nickel comes from the powered Macerator's iron-ore byproduct, so Invar — a mid-tier alloy — sits behind having power. Fine for its tier.
- Energy/time anchored to the Macerator (10,000 RF buffer, ~128 RF/t intake, ~10 RF/t draw, ~200-tick op) but a touch costlier — alloying is a step up from grinding.
- **Alloy set kept tight:** Bronze (exists) + Invar (new). Electrum/Silver, Constantan, and high-tier alloys (Signalum/Lumium/Enderium) are deferred/out for v1.
- Ratios (anchors; **approximate — TE has no public source**, tune in playtest): Bronze = 3 copper : 1 tin; Invar ≈ 2 iron : 1 nickel.

## Design sketch

Follow the machine "shape" exactly as Macerator/Kiln (verified pattern):

```text
common/src/main/java/com/logistics/automation/alloysmelter/
├── AlloySmelterBlock.java            # extends MachineBlock; FACING + LIT; particles when active
├── AlloySmelterBlockEntity.java      # extends BaseBlockEntity
│                                     #   implements HasItemStorage, HasEnergyStorage,
│                                     #   WorldlyContainer, MenuBehavior.HasMenu
│                                     #   fields: ItemInventoryComponent (2 in + 3-slot shared output),
│                                     #   EnergyComponent, ContainerData
├── AlloySmelterRecipe.java           # implements Recipe<…>; two ingredients, result, optional ChanceResult secondary, time
├── AlloySmelterRecipeSerializer.java # MapCodec + StreamCodec
├── AlloySmelterProcessingPlan.java   # pure logic (advance/consume/complete) — unit tested
├── AlloySmelterScreenHandler.java    # extends RecipeBookMenu; ContainerData getters
└── (client) AlloySmelterScreen.java + jei/{Category,Plugin}
```

- Register block/item/BE/menu/recipe-type/serializer in **`LogisticsAutomation`**; register the new **Nickel/Invar items** (+ Invar gear/valve) in `LogisticsCore` alongside the existing tin/bronze items. Use `registerBlockWithItem` / `registerBlockEntity` / `registerMenuType` / `registerItem`.
- The two-input recipe input is a custom `RecipeInput` (not vanilla `SingleRecipeInput`) holding both slots; `matches()` tries both input orderings.
- **Secondary output** reuses the shared `ChanceResult` + pause-until-clear from [`0102-macerator-secondary-outputs.md`](0102-macerator-secondary-outputs.md), generalized into the 3-slot shared output inventory — build in lockstep. (0102's `ChanceResult` is *also* used for the iron-ore → nickel Macerator byproduct.) The optional `secondary` field is supported by the codec but unused by v1 recipes.
- **Smelter recipes** (both forms; output in kind; no `secondary` in v1):
  ```json
  { "type": "logistics:alloy_smelter",
    "inputs": [ { "ingredient": "#c:ingots/copper", "count": 3 },
                { "ingredient": "logistics:core/tin_ingot", "count": 1 } ],
    "result": { "id": "logistics:core/bronze_ingot", "count": 4 }, "smelttime": 200 }
  { "type": "logistics:alloy_smelter",
    "inputs": [ { "ingredient": "logistics:core/copper_dust", "count": 3 },
                { "ingredient": "logistics:core/tin_dust", "count": 1 } ],
    "result": { "id": "logistics:core/bronze_dust", "count": 4 }, "smelttime": 200 }
  // optional "secondary": { "id": "...", "count": 1, "chance": 0.25 } supported but unused in v1
  ```
- **Hand grid recipe** (`crafting_shapeless`): `copper dust ×3 + tin dust ×1 → bronze dust ×4`; `iron dust ×2 + nickel dust ×1 → invar dust ×3`. No ingot+ingot recipe.
- **Nickel byproduct** (Macerator, uses 0102): iron ore → iron dust + small chance `nickel_dust`. Smelting: `nickel_dust → nickel_ingot`, `invar_dust → invar_ingot` (vanilla/Kiln). This is the first concrete entry in the byproduct map 0102 deferred here.

## Scope & non-goals

- **In:** the Alloy Smelter (2 inputs + **3-slot shared output inventory**, with the `ChanceResult` + pause-until-clear *capability* built and tested but no v1 recipe using it); the **Nickel + Invar** materials; **Invar gear + valve**; the dust-alloy grid recipes; the smelter's dust- and ingot-form recipes; the iron-ore → nickel Macerator byproduct; JEI + GUI.
- **Companion ([`0103-hand-grinder.md`](0103-hand-grinder.md), required for the no-energy-gate goal):** the **Hand Grinder** — a manual, powerless ore→dust block (the hand counterpart to the Macerator; no byproduct chance, since the powered Macerator is what earns the bonus). Built early; this feature assumes it exists.
- **Out:** the **slag item + any slag-producing recipe + slag's use** (all deferred together — the output capability is built but dormant); Electrum/Silver, Constantan, high-tier alloys (Signalum/Lumium/Enderium); a dedicated Nickel **ore + worldgen** (byproduct-sourced for now; add an ore only if supply is too scarce); fluid I/O; machine tiers ([`0106-machine-upgrades.md`](0106-machine-upgrades.md)); **ingot+ingot grid crafting** (intentionally excluded so hand-alloying routes through dusts).

## Decisions

- **Alloy/metal set** — **Bronze (exists) + Invar (new)**, via **Nickel**. Electrum/Silver and the rest are deferred. *Open: confirm **Invar's use** — proposed as Invar gear + Invar valve (next tier in the existing ladders, "engine valve"/precision flavor, future machine frames). If that's not a satisfying use, defer Nickel/Invar until a machine-tier feature gives it a home.*
- **Alloy source** — dust-crafting path (grid: `dust + dust → alloy dust`, then smelt → ingot) **+** the Alloy Smelter (automated, accepts dust- or ingot-pairs → alloy). **No ingot+ingot grid recipe.** Bronze stays reachable without power **via the Hand Grinder**.
- **Output inventory & slag** — build the **3-slot shared output inventory** + the `ChanceResult`/pause-until-clear *capability* now, but ship **no slag item and no slag-producing recipe**. The second/third slots buffer multi-stack alloy runs today and stand ready for a future byproduct; enabling slag later is pure JSON + adding the item. Slag's eventual use = a TE-style recycle/Rich-Slag loop.
- **Nickel sourcing** — **Macerator byproduct of iron ore** (0102 `ChanceResult`), no new worldgen; its own ore only as a fast-follow if supply feels too tight.

> Remaining choices are implementation/balance: exact ratios and nickel byproduct %, smelt time, Invar's recipe consumers, and whether Invar also gets a core (it has copper/bronze precedent).

## Done when

- Block places, rotates, smelts two inputs → alloy on both loaders, persists across save/load.
- The smelter accepts **both** dust-pairs (→ alloy dust) and ingot-pairs (→ alloy ingot).
- Hand path works: `copper dust + tin dust → bronze dust` in the grid → smelt → bronze ingot, reachable **without power** (via the Hand Grinder).
- Iron ore macerates to iron dust + a chance of `nickel_dust`; `iron dust + nickel dust → invar dust` → smelt → invar ingot.
- A full input load runs to completion, buffering **two stacks of alloy** across the output inventory without manual slot-shuffling.
- The **secondary-output capability** (`ChanceResult` roll both branches + pause-until-clear when the output inventory is full) is **unit-tested via a synthetic recipe**, even though no shipped v1 recipe produces a secondary.
- Invar gear + Invar valve are craftable.
- JEI lists alloy recipes; GUI shows progress + energy + the 3-slot output inventory.

## References

- Roadmap: [`../delivery-plan.md`](../delivery-plan.md) → Phase 1 → Alloy Smelter; materials rows in [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md)
- Code pattern: `automation/kiln/*` and `core/macerator/*` (block, BE, recipe, serializer, processing plan, screen, JEI); registration in `LogisticsAutomation.java`; new metal items in `LogisticsCore.java` (next to `TIN_*`/`BRONZE_*`)
- Shared mechanism: [`0102-macerator-secondary-outputs.md`](0102-macerator-secondary-outputs.md) `ChanceResult` (generalized here to the shared output inventory)
- **TE sourcing note:** TE has **no public source** — slag/byproduct behavior and ratios are wiki/knowledge-based, approximate; tune in playtest. Existing metal items confirmed in `LogisticsCore.java`: `TIN_*`, `BRONZE_*`, `*_DUST`, `*_GEAR`, `COPPER/BRONZE_VALVE`, `COPPER/BRONZE_CORE` (Nickel/Invar/Slag do **not** exist yet).
- **Companion brief:** [`0103-hand-grinder.md`](0103-hand-grinder.md) — manual powerless ore→dust; ungates the dust/alloy economy.
