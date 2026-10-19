# Machine Frame Tiers

> **Status:** 🚧 Planned — design settled; the farms are the first adopter; the tier contract lands with the tiered farms, the Upgrade Kits in a follow-up · **Phase:** 1 — Automation core (cross-cutting) · **Module:** shared framework in `core` (`core/machine/tier/`)
> **Source:** [`../progression-tiers.md`](../progression-tiers.md) (Machine frames) · **Depends on:** the machine framework (`core/machine/*`) · **Maps to (roadmap):** Phase 2 — Farms (first adopter)

One machine-wide tier contract: a machine block carries a **tier** (Basic → Industrial → Precision → Infernal) that scales what it can do, and the player raises it in place with an **Upgrade Kit**. The [farms](0201-farm-framework.md) adopt it first; other machines can opt in later. Slot-in augments (speed, efficiency, …) are a separate system and stay in [0106](0106-machine-upgrades.md).

## Problem & goal

Every machine is a fixed block today. The farms need a bigger work area and more throughput as the player progresses, and the old answer — "work-area upgrades will come with 0106" — mixed two ideas: a machine's *rank* and its *modifiers*. **Goal:** a small, durable tier contract in `core` that any machine can adopt, saved by stable string id, carried on the item when the block is broken, and raised one step at a time in the world.

## Requirements

### Functional
- **Ladder** — a subset of the canonical [progression ladder](../progression-tiers.md), in order:

  | Tier | Id | Rank material | Upgrade Kit |
  |---|---|---|---|
  | Basic | `basic` | Copper | — (the crafted block) |
  | Industrial | `industrial` | Bronze | Industrial Upgrade Kit |
  | Precision | `precision` | Diamond | Precision Upgrade Kit |
  | Infernal | `infernal` | Netherite | Infernal Upgrade Kit |

- **Stable string ids.** The tier is saved and synced as its id (`"industrial"`), **never as an ordinal**, so the ladder can be reordered or extended without corrupting saves. An unknown id (a later rename or removal) loads as `basic` with a logged warning; it never crashes.
- **Every machine is crafted at Basic.** Higher tiers come only from kits.
- **Upgrade Kits** are applied in the world (use the kit on the machine):
  - one step at a time — a kit applies only to a machine whose tier is the kit's `previous()`;
  - a wrong-step kit is refused and **not consumed**;
  - the kit is consumed only after the tier change succeeds;
  - inventory, energy and fluid are kept.
- **Tiers only go up.** There is no downgrade and no kit refund.
- **The tier survives breaking the block:** the dropped item carries it, and placing it restores it.
- **Resizable buffers:** a tier change can grow the machine's energy buffer, max input and fluid tank. Resizing clamps the stored amount, so nothing is ever created.
- **Visibility:** the tier shows in Jade and in the machine's GUI.

### Balance
- Each adopter defines its own per-tier profile (the farm's is in [0201](0201-farm-framework.md)). The anchor is **×2 throughput per tier**, so peak draw fits a cable of the matching rank (30 / 60 / 120 / 240 RF/t).
- Kit recipe pattern: the rank's **gear ×2 + its valve + its material ×4**. Exact shapes are settled in the kit PR.
- **Kits are an approved exception** to the "no tiered intermediate items" rule. That rule is about crafting intermediates (per-tier frames, cores); a kit is never crafted *into* a machine — it is applied *to* one.

## Design sketch

```text
common/src/main/java/com/logistics/core/machine/tier/
├── MachineTier.java      # enum, StringRepresentable; id, rank, material, lang key; next()/previous(); CODEC
├── TierComponent.java    # MachineComponent; NBT "Tier": "<id>"; tier()/setTier(); onChange listeners
├── TieredMachine.java    # interface on the block entity
└── UpgradeKitItem.java   # UpgradeKitItem(MachineTier target)
```
- **`MachineTier.CODEC = StringRepresentable.fromEnum(...)`**, used for NBT, the network and the data component alike.
- **`TierComponent`** calls `ctx.sync()` on change, notifies `onChange` listeners (adopters resize there), and contributes a Jade line through `MachineHudModel`.
- **`MachineBuilder.tier(id)`** registers the component.
- **Data component `logistics:machine_tier`**, encoded with the same string codec. Drop retention follows the battery pattern: `AbstractBatteryBlockEntity` implicit components plus `copy_components` in the loot table.
- **`EnergyStorageComponent.resize(capacity, maxInsert)`** and **`FluidStoreComponent.resize(capacity)`**, both clamped.
- **`UpgradeKitItem.useOn`:** validate (`TieredMachine`, current tier is `target.previous()`), then `setTier(target)` and resize, then shrink the stack. No partial state on failure.
- The kit's tooltip names its effect for each adopter (the farm's: "Expands the work area immediately").

## Scope & non-goals

- **In:** `MachineTier`, `TierComponent`, `TieredMachine`, `MachineBuilder.tier`, the data component and drop retention, buffer resizing, three Upgrade Kits. The kits arrive in a follow-up PR; until then machines stay Basic in play.
- **Out:** augments — speed, efficiency, fertilization and so on stay in [0106](0106-machine-upgrades.md) and use `MachineModifiers` / `OperationKey`. Downgrades. Per-tier block variants or per-tier crafted frames. Adopting tiers on the processing machines (Macerator, Kiln, …) — possible later, not planned here.

## Open questions

- Exact kit recipe shapes and textures (settled in the kit PR).
- Which machines adopt tiers after the farms.

## Done when

- JUnit: the codec round-trips by string id (golden strings; no ordinal ever appears in NBT); an unknown id loads as `basic` with a warning; `resize` clamps.
- GameTests on both loaders: a kit raises the tier and keeps inventory and energy; a wrong-step kit is refused and kept; breaking and replacing the block keeps the tier.

## References

- Ladder: [`../progression-tiers.md`](../progression-tiers.md) → canonical ladder; Machine frames row
- First adopter: [0201 Farm Framework](0201-farm-framework.md)
- Code: `core/machine/{MachineEntity,MachineBuilder,MachineHudModel}`, `core/machine/component/{EnergyStorageComponent,FluidStoreComponent}`, `core/machine/upgrade/{MachineModifiers,OperationKey}`; drop retention in `AbstractBatteryBlockEntity` and the battery loot tables
- Related: [0106 Machine Upgrades](0106-machine-upgrades.md) (augments), [0107 Tiered Batteries](0107-tiered-batteries.md)
