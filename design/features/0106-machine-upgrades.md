# Machine Upgrades / Augments

> **Status:** 🔍 Exploring — **not committed for 1.0** ([`ROADMAP.md`](../../ROADMAP.md) lists machine upgrades under *Exploring / RFC*). This brief captures *how* we'd build it if/when it's accepted; it is not yet scheduled. · **Module:** `logistics-automation` (cross-machine; `core.lib` contract)
> **Source:** [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md) (Augments) · **Depends on:** the machine pattern (Macerator/Kiln); pairs with [`0102-macerator-secondary-outputs.md`](0102-macerator-secondary-outputs.md) (secondary augment)
> **Maps to (roadmap):** [`ROADMAP.md`](../../ROADMAP.md) → Exploring / RFC → machine upgrades

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
- **GUI:** add upgrade slots to each machine's `ScreenHandler` + `Screen`. A shared sub-layout (slot strip + tooltips) keeps the four screens consistent. The natural home is an **Upgrades side tab** next to the existing Info/Energy/Redstone tabs — see below.

### Upgrade slots in a side tab

The side-tab framework (`core/lib/client/screen/tab/`, guide in its `package-info.java`) does not host slots yet. This is the contract it was designed to accept them under; the additions are small and none of it is built.

1. **Tab slots are ordinary menu slots.** The server treats them as always active; all authority stays in `Slot.mayPlace` and the container's `canPlaceItem`, so a modified client can do nothing an open tab couldn't.
2. **Positions are fixed.** With one open tab per side, an open tab's position depends only on its index on its side and the GUI width, so the slots get fixed menu coordinates outside the GUI image — which suits `Slot.x/y` being final.
3. **The client hides them while the tab isn't fully open.** A `TabSlot` whose `isActive()` reads a client-only `BooleanSupplier` injected by the screen (`host.isFullyOpen(id)`), defaulting to `true` so the server is unaffected. Inactive slots aren't hovered, drawn, or used as quick-craft targets.
4. **Rendering comes free:** slots draw above widgets, so slot items render on the open panel.
5. **Host additions needed:** a `passThroughRects()` hook so the tab widget's `isMouseOver` returns false over its slots (otherwise vanilla never sees the slot click), and a `minOpenWidth` wide enough for the slot strip.

**Accepted limitations:**
- Shift-click (`quickMoveStack`) can route items into the upgrade slots while the tab is collapsed — the server can't know the client's tab state. Keep augments out of the shift-click route, or accept it.
- At a GUI scale that leaves too little margin, the tab can't open and the slots are reachable only by shift-click.

**Adoption order:** build the contract + Macerator integration first (Macerator already has secondary-output work in flight), then add the slots to Kiln/Sawmill/Alloy Smelter — they share the `ProcessingPlan` shape, so adoption is mechanical.

## Scope & non-goals

- **In (if accepted):** the `core.lib` upgrade contract, four augment types, GUI slots, adoption across the four core machines (Macerator/Kiln/Sawmill/Alloy Smelter).
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

- Roadmap: [`ROADMAP.md`](../../ROADMAP.md) → Exploring / RFC → machine upgrades (and [`../delivery-plan.md`](../delivery-plan.md) → Phase 1 → exploratory)
- Breakdown: [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md) → "Augments" and "Machine frames / tiers" rows
- Code: the four machines' `*ProcessingPlan` + `*ScreenHandler`; `core/lib/items/ItemInventoryComponent`; `core/lib/storage/ItemStorageLookup` (auto-output); data-component precedent in `pipe/data/PipeDataComponents`
- Related: [`0102-macerator-secondary-outputs.md`](0102-macerator-secondary-outputs.md), [`0107-tiered-batteries.md`](0107-tiered-batteries.md)
