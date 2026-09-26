# Tiered Machine Frames

> **Status:** 📋 Brief (no code) · **Phase:** 1 — Automation core · **Module:** `logistics-automation`
> **Source:** [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md) ("Machine frames / tiers") · **Depends on:** nothing built yet
> **Maps to (roadmap):** Phase 1 — machine tiers

Tier the single **Machine Frame** (`logistics:core/machine_core`) into the structural ladder —
**Iron → Bronze → Diamond → Netherite** — so machine bodies carry a progression the way batteries
now do, and so the frame has a reason to exist beyond "one ingredient every machine needs".

## Why this brief exists

Written while shipping [`0107-tiered-batteries.md`](0107-tiered-batteries.md). The battery line
briefly used a Transposer-filled Machine Frame as its upper-tier gate; that was removed on the
maintainer's call, because **machine frames are to be tiered for machines and kept separate from
energy storage.** Recording the intent so nothing wires them back together.

The two ladders in [`../progression-tiers.md`](../progression-tiers.md) are deliberately disjoint:

| Ladder | Chosen for | Materials |
|---|---|---|
| Energy / storage | conductivity, then exotic energy properties | Tin → Copper → Gold → Amethyst → Echo |
| **Machine frames** | structural strength and durability | **Iron → Bronze → Diamond → Netherite** |

Bronze is a structural alloy and never a battery tier; Gold is a conductor and never a frame tier.
Keeping them apart is what makes each ladder legible.

## Problem & goal

There is one Machine Frame, consumed flat by every machine recipe. It is a gate, not a progression:
a Kiln and a Sequential Fabricator want the same frame. Meanwhile the structural half of the
canonical ladder (Iron, Bronze, Diamond, Netherite) has no line of its own, and Bronze in particular
has little to do once the alloy exists.

**Goal:** four frame tiers that gate and/or improve the machines built from them, giving the
structural ladder a home and the Bronze alloy a purpose.

## The open question this brief cannot settle alone

**What does a frame tier actually *do*?** Three candidate answers, and the choice drives everything
else:

1. **Gate** — a machine simply requires a given frame tier to be crafted at all. Simple, but it is
   just a recipe cost; the frame has no runtime effect and players will read it as a tax.
2. **Modifier** — the frame a machine was built from sets its speed / energy capacity / buffer.
   Meaningful, but means machine block entities must know their frame tier, which is real work:
   either tiered machine *blocks* (block sprawl — one Kiln per tier) or the tier stored on the block
   entity and shown in the GUI.
3. **Upgrade path** — a machine can be raised in place. This is what **Thermal actually does**, and
   *not* with frames: `ItemUpgrade` in ThermalFoundation ships `incremental1..4`
   ("requires tier immediately preceding") and `full1..4` ("may be used on any lower tier"), applied
   by right-clicking a placed block. TE has exactly **one** `frameMachine`; the tiered-frame ladder
   is the Energy Cell's alone.

**This collides with [`0106-machine-upgrades.md`](0106-machine-upgrades.md)**, whose open question is
already *"do we ship slot-in augments and crafted machine tiers, or fold tiering into augments?"* with
**Lean: augments first … decide on tiers later.** That lean is still unresolved, and (2) and (3) are
substantially the same feature as augments wearing a different hat. **Settle 0106 before building
this** — otherwise we ship two overlapping systems for modifying a machine.

If the answer turns out to be (3), note that the honest name is *conversion kits*, not frames, and
this brief largely dissolves into 0106.

## Requirements (if we proceed with tiered frames)

### Functional
- Four frame items on the structural ladder: Iron, Bronze, Diamond, Netherite.
- The existing `core/machine_core` ("Machine Frame") **maps to the Iron tier** and keeps its registry
  id, so no existing machine recipe changes and no alias is needed. Higher tiers are additive.
- Every current machine recipe keeps working unchanged against the Iron-tier frame.

### Balance
- Bronze should be *cheap relative to its position* — its job here is to give the alloy a use, not to
  wall off mid-game machines.
- Netherite frames must not require more netherite than a player plausibly has at that point; one
  ingot's worth of a shared component, not one per machine.

### Migration
- The rename risk is the display name, not the id: "Machine Frame" would likely become "Iron Machine
  Frame", which the wiki pipeline requires matching `en_us.json` exactly. Registry id stays
  `core/machine_core` (an id outliving a display-name rename is already precedent here — see the
  closed Kiln entry in `WIKI_DISCREPANCIES.md`).

## Scope & non-goals

- **In:** four frame items, their recipes, and whatever runtime meaning the open question resolves to.
- **Out:** tiered machine *blocks* (one Kiln per tier is block sprawl — prefer the tier on the block
  entity if a runtime effect is wanted); anything touching energy storage, which has its own ladder
  and its own vessel; Signalum/Lumium/Enderium, which `../mods/thermal-expansion.md` already flags as
  bloat risk.

## Done when

- [ ] The "what does a tier do" question above is answered, **and reconciled with 0106.**
- [ ] Four frames exist on the structural ladder, with the existing frame as the Iron tier.
- [ ] Every pre-existing machine recipe still crafts with an Iron-tier frame.
- [ ] Bronze has a use that justifies the alloy.

## References

- Ladders: [`../progression-tiers.md`](../progression-tiers.md) → "Purpose-built ladders"
- Breakdown: [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md) → "Machine frames / tiers"
- **Blocking overlap:** [`0106-machine-upgrades.md`](0106-machine-upgrades.md) → "Augment items vs. machine tiers"
- Precedent for craft-then-fill and for keeping the ladders apart: [`0107-tiered-batteries.md`](0107-tiered-batteries.md)
- Thermal reference (verified against `ThermalFoundation-1.12.2-2.6.7.1`): `ItemUpgrade`
  (`upgrade.incremental1..4`, `upgrade.full1..4`, `upgrade.creative`), and `ItemFrame.frameMachine` —
  singular
- Alloy context: [`0105-alloy-smelter.md`](0105-alloy-smelter.md) (Invar is described as pointing at
  "future machine frames" — this is that feature)
