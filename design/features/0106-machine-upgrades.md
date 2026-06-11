# Machine Upgrades / Augments

> **Status:** 🚧 Planned · **Phase:** 1 — Automation core · **Module:** `logistics-automation` (cross-machine; `core.lib` contract)
> **Source:** [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md) (Augments) · **Depends on:** the machine pattern (Macerator/Kiln); pairs with [`0102-macerator-secondary-outputs.md`](0102-macerator-secondary-outputs.md) (secondary augment)
> **Maps to (roadmap):** Phase 1 — machine upgrades

A unified, slot-in modifier system applied across all RF machines (Macerator, Kiln, Sawmill, Alloy Smelter): speed, efficiency, secondary-yield, and auto-output augments. The cross-cutting "make machines feel like a system, not isolated blocks" feature. There is no upgrade system today — this defines one.

## Problem & goal

Each machine is currently a fixed block: fixed speed, fixed energy cost, manual output extraction. TE's augment system is what turned its machines into a progression — you upgraded the machine you had instead of replacing it (the balance principle: *each tier obsoletes the grind, not the gameplay*).

**Goal:** one reusable upgrade mechanism, defined once in `core.lib` and adopted by every machine, with a small, legible augment set.

## Requirements

### Functional
- Every supported machine gains a small set of **upgrade slots** (e.g. 3–4), accessible in its GUI, holding **augment items**.
- Augments modify the machine's processing math each tick. v1 augment types:
  - **Speed** — faster processing (fewer ticks per op), at higher energy/tick.
  - **Efficiency** — lower energy per op (and/or larger buffer).
  - **Secondary yield** — raises secondary/byproduct chance where the recipe defines one (hooks [`0102-macerator-secondary-outputs.md`](0102-macerator-secondary-outputs.md); no-op on recipes without a secondary).
  - **Auto-output** — pushes finished outputs into an adjacent inventory/pipe via `IItemStorage`, no extractor needed.
- Augments stack within sensible caps; effects are computed in the machine's **`ProcessingPlan`** so they stay unit-testable.
- Augment state persists (slots are part of the BE inventory / NBT) and drops with the machine.

### Balance
- Augments are a **trade**, not pure power: Speed costs more energy; the strong effects (max speed + secondary) demand the tiered batteries / better engines to feed them — ties the [`0107-tiered-batteries.md`](0107-tiered-batteries.md) and engine work into a loop.
- Keep the set small and the caps modest; avoid TE's late augment overload.
- Auto-output is convenience, not a logistics replacement — single adjacent target, not network routing.

## Design sketch

Define the contract in `core.lib`, apply it in each machine's processing plan.

- **`core/lib/machine/MachineUpgrades`** (new): holds the augment items for a machine and exposes derived modifiers — e.g. `speedMultiplier()`, `energyPerOpMultiplier()`, `secondaryChanceBonus()`, `autoOutput()`. Backed by a small `ItemInventoryComponent` of upgrade slots.
- **Augment items** in `core` domain (e.g. `SpeedUpgradeItem`/tiered, or a single item type with a tier component). Tier could be a data component to avoid item sprawl.
- **`ProcessingPlan` integration:** each plan's `advance(...)` already takes progress/energy params — extend it to take the derived modifiers (a small `MachineModifiers` record) so speed/efficiency are pure-function inputs and both branches stay testable. This is the key reason to route augments through the plan, not ad-hoc into the tick loop.
- **Auto-output:** in the BE tick, after `complete`, if `upgrades.autoOutput()` push outputs to the adjacent inventory via `ItemStorageLookup.find(...)` (item-side precedent exists).
- **GUI:** add upgrade slots to each machine's `ScreenHandler` + `Screen`. A shared sub-layout (slot strip + tooltips) keeps the four screens consistent.

**Adoption order:** build the contract + Macerator integration first (Macerator already has secondary-output work in flight), then add the slots to Kiln/Sawmill/Alloy Smelter — they share the `ProcessingPlan` shape, so adoption is mechanical.

## Scope & non-goals

- **In:** the `core.lib` upgrade contract, four augment types, GUI slots, adoption across the four Phase-1 machines.
- **Out:** machine **tiers / frames** (Basic→Resonant crafted machine bodies — separate, see TE breakdown "Machine frames / tiers"); per-side augment config; augments that change *what* a machine does (only how fast/cheap/auto); fluid-machine augments (until those machines exist).
- **Out:** engine augments — engines aren't processing machines; revisit separately if wanted.

## Open questions

- **Augment items vs. machine tiers** — do we ship slot-in augments (this doc) *and* crafted machine tiers, or fold tiering into augments? The TE breakdown lists tiers as a separate "—" row. **Lean: augments first (more flexible, less block sprawl); decide on tiers later.**
- One augment item with a tier component, or distinct items per tier? (Data component keeps the item list short.)
- How many slots per machine, and caps per augment type, to avoid stacking into trivialization?
- Should Speed and Efficiency be mutually constraining (a curve) rather than independent multipliers?

## Done when

- A machine with a Speed augment processes faster at higher energy/tick; with Efficiency, cheaper per op — both verified in the `ProcessingPlan` unit tests.
- A Secondary augment raises byproduct chance only where a secondary exists.
- Auto-output pushes finished items to an adjacent inventory/pipe on both loaders.
- The same augment items work in Macerator, Kiln, Sawmill, and Alloy Smelter.

## References

- Roadmap: [`../roadmap.md`](../roadmap.md) → Phase 1 → machine upgrades
- Breakdown: [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md) → "Augments" and "Machine frames / tiers" rows
- Code: the four machines' `*ProcessingPlan` + `*ScreenHandler`; `core/lib/items/ItemInventoryComponent`; `core/lib/storage/ItemStorageLookup` (auto-output); data-component precedent in `pipe/data/PipeDataComponents`
- Related: [`0102-macerator-secondary-outputs.md`](0102-macerator-secondary-outputs.md), [`0107-tiered-batteries.md`](0107-tiered-batteries.md)
