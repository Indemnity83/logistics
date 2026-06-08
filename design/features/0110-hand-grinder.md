# Hand Grinder

> **Status:** 🚧 Planned — **design settled, ready to build** · **Phase:** 1 — Automation core · **Module:** `logistics-automation` (`core` domain — pairs with the Macerator)
> **Source:** modernization (no direct source) — a manual, powerless counterpart to the Macerator (mortar/quern flavor) · **Depends on:** [`0102-macerator-secondary-outputs.md`](0102-macerator-secondary-outputs.md) (reuses Macerator recipes) · **Required by:** [`0104-alloy-smelter.md`](0104-alloy-smelter.md) (the "Bronze not energy-gated" path)
> **Maps to (roadmap):** Phase 1 — early-game on-ramp · **Build order:** *early — before/with 0104* (despite the `0110` id; see [README](README.md))

A placed crank-station that grinds ore → dust **by hand, with no RF**. The no-power on-ramp that lets a player reach the dust → alloy economy (Bronze) before they have engines and a Macerator. The powered Macerator stays the upgrade: it's faster *and* it earns the chance byproducts (0102) the hand grinder doesn't.

## Problem & goal

The dust-crafting alloy path ([`0104-alloy-smelter.md`](0104-alloy-smelter.md)) routes through dusts (e.g. copper dust + tin dust → bronze dust). But dusts come from the Macerator, which needs power — so without this, **Bronze (and the whole alloy tier) would be hard-gated behind energy**. The Hand Grinder breaks that gate with manual effort.

**Goal:** a cheap, early, powerless block that turns ore into base dust through repeated manual cranking — tedious enough that the Macerator is a real upgrade, but enough to bootstrap the tier.

## Requirements

### Functional
- **Placed block**, no energy. Holds one loaded input + a crank-progress counter; exposes the finished dust as output.
- **Crank interaction** (vanilla-Composter-style, no GUI):
  - *Empty* + player holding a grindable item → load one item, advance to grinding.
  - *Loaded* + right-click → **crank**: advance progress one step, with a grinding sound + crushing particles and a visible block-state stage.
  - *Complete* → the dust is produced to the output; right-click collects it (or it pops out). Then empty again.
- **Reuses Macerator recipes**, restricted to a `#logistics:hand_grindable` tag (ores + raw metals), producing **only the primary result** — *no* secondary/byproduct (that's the Macerator's reward) and no XP.
- **~8 cranks per grind** (tunable) — a few seconds of clicking per ore.
- **Single tier, no upgrades.** It's the bottom-of-ladder manual tool (Crude/Basic), not a tiered line.
- **Hopper/pipe interop, but cranking stays manual:** the block entity may expose its output (and optionally input) as `HasItemStorage` so pipes/hoppers can feed ore and pull dust — but **the cranking cannot be automated**, so it never becomes a free auto-Macerator. That manual step is the gate.

### Balance
- Slower and more tedious than the Macerator by design — manual labor is the cost of skipping power.
- **No byproduct chance** — the powered Macerator earns the bonus dust ([`0102`](0102-macerator-secondary-outputs.md)); the hand grinder gives base output only.
- **Cheap early craft** (stone-tier, no machine needed) — e.g. stone + an iron component. Sits at the bottom of the canonical ladder ([`../progression-tiers.md`](../progression-tiers.md)) as the pre-power on-ramp.

## Design sketch

Not the powered-machine pattern (no energy, no menu). Model on **vanilla `ComposterBlock`** (block-state progress + right-click interaction) plus **`LecternBlock`** (a BE that holds an item), reusing the Macerator recipe type for the actual ore→dust mapping.

```text
common/src/main/java/com/logistics/core/handgrinder/
├── HandGrinderBlock.java        # extends BaseEntityBlock; STAGE IntegerProperty (0=empty..N=done);
│                                #   useItemOn() loads a grindable item; use() cranks / collects
├── HandGrinderBlockEntity.java  # extends BaseBlockEntity implements HasItemStorage (no HasEnergyStorage)
│                                #   fields: loaded input ItemStack, crankProgress, output ItemStack
└── (client) crank sound + crushing particles; block-state models per STAGE (Composter-style)
```

- **Recipe lookup:** resolve the loaded item against `RecipeType` `logistics:macerator` (the existing `MaceratorRecipeWrapper`), gated by the `#logistics:hand_grindable` item tag; take `result()` only, ignore `grindingtime`/secondary/experience. (No new recipe type — DRY with the Macerator.)
- **Tag:** `data/logistics/tags/item/hand_grindable.json` listing the ores/raw metals reachable pre-power (copper, raw iron, raw tin, …).
- **Interaction state machine** in the block:
  - `STAGE == 0` (empty) + `useItemOn` with a `#hand_grindable` item that has a macerator recipe → consume one, store in BE, `STAGE = 1`.
  - `STAGE in 1..N-1` + `use` (crank) → `STAGE++`, play sound + particles. At `STAGE == N` → produce the dust into the BE output, reset loaded input.
  - output present + `use` → give dust to player / drop it, `STAGE = 0`.
- **Registration:** in `LogisticsCore` next to the Macerator (block + item + BE type). Client model/particle wiring per loader.

## Scope & non-goals

- **In:** the block, the crank state-machine, ores→dusts via reused Macerator recipes + the `hand_grindable` tag (primary only), pipe/hopper output, the early crafting recipe.
- **Out:** any RF; a GUI; **upgrades/tiers** (single tier); byproduct chance; auto-cranking; non-ore Macerator inputs (the `hand_grindable` tag scopes it to ores/raw metals); recipe-book/JEI catalyst beyond what the Macerator already shows.
- **Out:** replacing the Macerator — it's strictly the slower, bonus-less, powerless predecessor.

## Decisions

- **Form & interaction** — **placed crank-station**: load an ore, **repeatedly right-click to crank** to completion (Composter-style block-state progress, no GUI).
- **Scope** — **ores → dusts only, single un-tiered tier**; the no-power on-ramp, nothing more.
- **Recipes** — **reuse the Macerator recipe type**, filtered by `#logistics:hand_grindable`, **primary result only** (no byproduct, no XP). *(Confirm the tag-reuse approach vs a tiny dedicated recipe set.)*
- **Automation** — pipes/hoppers may move items in/out, but **cranking is manual-only**, preserving the gate.

> Implementation/balance details remain: exact crank count, the crafting recipe, the `hand_grindable` tag contents, and whether dust auto-pops or is collected by click.

## Done when

- A placed Hand Grinder accepts a `#hand_grindable` ore, advances one stage per right-click with sound/particles, and yields the base dust after ~8 cranks — **no power involved** — on both loaders.
- It produces **only** the primary dust (no byproduct), confirming the Macerator keeps the bonus.
- A player can reach Bronze with no engines: hand-grind copper + tin → dusts → craft bronze dust → smelt → bronze ingot.
- A hopper/pipe can feed ore and pull dust, but the grinder still requires manual cranks to process.

## References

- Roadmap: [`../roadmap.md`](../roadmap.md) → Phase 1 (early-game processing on-ramp)
- Pairs with: [`0102-macerator-secondary-outputs.md`](0102-macerator-secondary-outputs.md) (shared recipes; powered upgrade with byproducts), [`0104-alloy-smelter.md`](0104-alloy-smelter.md) (the no-power Bronze path this enables)
- Tier placement: [`../progression-tiers.md`](../progression-tiers.md) (bottom-of-ladder manual tool)
- Code precedent: vanilla `ComposterBlock` (block-state progress + right-click), `LecternBlock` (BE holds an item); `core/macerator/{MaceratorRecipeWrapper,MaceratorRecipeSerializer}` (reused recipes); registration in `LogisticsCore.java`
