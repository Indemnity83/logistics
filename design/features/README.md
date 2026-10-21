# Feature Briefs

This folder expands settled [`delivery-plan.md`](../delivery-plan.md) rows into **feature briefs** — one file per feature, detailed enough to *start the work*. Each brief is grounded in the actual codebase: it names the real classes and patterns a feature plugs into, so an implementer isn't starting from a blank page.

Briefs sit **between** the durable "why" (the [`mods/`](../mods/) breakdowns, [`vision.md`](../vision.md), [`principles.md`](../principles.md)) and the live work tracking (GitHub Project #4). The breakdown says *what we decided and why*; the brief says *what to build and how to start*; the Project issue tracks *the work*. When a brief settles, decompose it onto the board (see the roadmap's [Mapping to the board](../delivery-plan.md#mapping-to-the-board)).

> These are **starting points, not specs set in stone**. Every brief carries an "Open questions" section — the decisions still owed before or during implementation. Resolve the blocking ones (often via a Discussion or a short spike) before scheduling.

## Naming scheme

Files are prefixed **`PPSS-`** — two digits of **phase**, two digits of **step** — so they sort by build order within their phase and stay grouped as later phases' briefs land in this shared folder.

- `01xx` = Phase 1 (Automation core), `02xx` = Phase 2 (Forestry), `03xx` = Phase 3 (Transport).
- The step is the **suggested build order** within the phase, dependency-aware (a feature's prerequisites have lower step numbers). It's a guide, not a contract — parallel tracks exist (see below).
- New briefs in a batch continue the sequence (this batch ends at `0110`; the deferred fluid-blocked items become `0111+`). `0111` is the machine tier contract ([Machine Frame Tiers](0111-machine-frame-tiers.md)). When build order changes, briefs are renumbered so the ids keep tracking it.

## This batch: Phase 1 keystone + ready-now

The first batch covers the **[Fluids foundation](0101-fluids-foundation.md)** (the keystone that unblocks much of Phases 1–3) plus the Phase 1 items with **no blockers** — the work that can start immediately.

| # | Brief | Area | Decision | Depends on |
|---|---|---|---|---|
| 0101 | [Fluids Foundation](0101-fluids-foundation.md) | 🔑 keystone | Port/Modernize | — (platform layer already built) |
| 0102 | [Macerator Secondary Outputs](0102-macerator-secondary-outputs.md) | Machines | Modernize | — |
| 0103 | [Hand Grinder](0103-hand-grinder.md) | Machines (no-power on-ramp) | Modernize | Macerator recipes |
| 0104 | [Sawmill](0104-sawmill.md) | Machines | Port | `ChanceResult` (0102) |
| 0105 | [Alloy Smelter](0105-alloy-smelter.md) ✅ *shipped (Bronze)* | Machines + materials | Port/Modernize | `ChanceResult` (0102); Hand Grinder (0103) *optional, not built* |
| 0106 | [Machine Upgrades / Augments](0106-machine-upgrades.md) 🔍 *exploratory — not committed for 1.0* | Machines (cross-cutting) | Modernize | the machines (0102 / 0104 / 0105) |
| 0107 | [Tiered Batteries](0107-tiered-batteries.md) | Power | Modernize | — (extends Battery) |
| 0108 | [Obsidian Vacuum Pipe](0108-obsidian-vacuum-pipe.md) ❌ *not planned — hoppers cover it* | Pipes | Skip | — |
| 0109 | [Remote Orderer](0109-remote-orderer.md) 🔍 *exploratory — not committed* | Logistics QoL | Modernize | — |
| 0110 | [Firewall Pipe](0110-firewall-pipe.md) ⏸️ *deferred — no use case yet* | Logistics advanced | Port | — |
| 0111 | [Machine Frame Tiers](0111-machine-frame-tiers.md) 🚧 *planned — farms adopt first* | Machines (cross-cutting) | Modernize | the machine framework |
| 0112 | [Infused Alloys](0112-infused-alloys.md) 🚧 *materials shipped* | Materials | Modernize | the Transposer; the Crucible's charged fluids |

### Reading the order

The step numbers encode dependencies, but several briefs are independent and can run in parallel:

- **Shipped — `0101` Fluids:** the keystone, ✅ shipped in v0.7.3 (standalone cellular fluid pipes + Glass Tank + Pump on both loaders). This unblocked the deferred fluid batch below.
- **Machine / dust chain — `0102` ✅ → `0104` ✅ / `0105` ✅ (mostly shipped):** [Macerator Secondary Outputs](0102-macerator-secondary-outputs.md) (v0.8.0), [Sawmill](0104-sawmill.md) (v0.8.0), and [Alloy Smelter](0105-alloy-smelter.md) (v0.8.2, Bronze) are **shipped**. The [Hand Grinder](0103-hand-grinder.md) (no-power ore→dust on-ramp) was **not** built — the machines shipped without needing it; it's still open if we want an early no-power path. [Machine Upgrades](0106-machine-upgrades.md) remains **exploratory** (per [`ROADMAP.md`](../../ROADMAP.md) Exploring/RFC).
- **Still open — `0107`:** [Tiered Batteries](0107-tiered-batteries.md) has no dependencies and is the clearest remaining Phase-1 power gap (single Battery today). ([Obsidian Vacuum Pipe](0108-obsidian-vacuum-pipe.md) is **not planned** — hoppers cover it; [Remote Orderer](0109-remote-orderer.md) is **exploratory** — see [`ROADMAP.md`](../../ROADMAP.md).)
- **Deferred — `0110`:** the [Firewall Pipe](0110-firewall-pipe.md) is **parked** — no solid use case established yet (maintainer, Jul 2026). Not a 1.0 item; revisit if network segmentation earns its place.

### Deferred to a later batch (Phase 1, fluid-blocked or needs a Discussion)

Not written yet — they wait on the keystone or on an open decision, and will take `0111+` steps:

- **Was fluid-blocked — now mostly shipped:** the combustion-tier **Fuel Engine**, the **Magmatic/Steam/Reaction** dynamo tiers, the **Crucible** (magma crucible), and the **oil fuel chain** (Refinery) all shipped in v0.8.2–v0.8.4. Still open from this group: the **Fluid Transposer** and **fluid logistics** (provider/supplier/request) — the transposer is the fluid↔item packaging step the logistics side depends on.
- **Deferred post-1.0 (RFC, not a feature brief):** the programmable-automation / gates+circuits system (BuildCraft gates + TE programmable augments + Forestry circuits) — **not a 1.0 item**; revisit when Forestry needs circuit boards (Phase 2) or later. See [`../rfcs/0001-programmable-behavior.md`](../rfcs/0001-programmable-behavior.md). *(Distinct from the machine-modifier upgrades, [0106](0106-machine-upgrades.md) — themselves exploratory, not committed for 1.0.)*
- **Already done:** pipe operation power gating (✅, #464/#465/#469); the Fluids foundation (✅, v0.7.3 — fluid pipes, Glass Tank, Pump, machine fluid I/O).

## Phase 2 batch: Forestry farms

The Phase 2 batch covers the **farms, soils and fertilizer** (design settled; see [`../mods/forestry.md`](../mods/forestry.md)). It is post-1.0 work: farms ship as later 1.x minors, and the maintainer decides when wave 1 starts. One block per farm type, each sitting at the centre of a protected 7×7 courtyard and working a ring of plots around it, from 11×11 at Basic to 23×23 at Infernal; tiers come from the machine-wide tier contract and its Upgrade Kits ([0111](0111-machine-frame-tiers.md)). The processing chain (Squeezer/Fermenter/Still/Bottler) and peat/biofuel engines are deferred beyond this batch.

The *Plan phase* column is the farm plan's build order: 1 = the tiered framework (Cropland and Arboretum adopt it), 2 = ground farms, 3 = advanced farms.

| # | Brief | Area | Plan phase | Depends on |
|---|---|---|---|---|
| 0201 | [Farm Framework](0201-farm-framework.md) | 🔑 keystone — shared farm base, scheduler, farm screen ✅; superseded by tend-only farms ([ADR 0001](../../docs/adr/0001-tend-only-farms.md)) | 1 | the machine framework; 0111; Provider-pipe extractable-slot fix |
| 0202 | [Fertilizer & Soils](0202-fertilizer-and-soils.md) | Fertilizer ✅ (item only, for a future augmentation), Humus ✅ (fixes the tree shape), bog-earth maturity | — (maturity: 3) | 0201 |
| 0203 | [Arboretum](0203-arboretum.md) | Tree farm + `TreeFeller` ✅; tends trees on Humus | 1 | 0201, 0202 (Humus) |
| 0204 | [Cropland (Crop Farm)](0204-crop-farm.md) | Field crops ✅; water per till × climate | 1 | 0201 |
| 0205 | [Peat Bog](0205-peat-bog.md) | Bog Earth → Peat | 3 | 0201, 0202 (maturity) |
| 0206 | [Mushroom Farm](0206-mushroom-farm.md) | Giant mushrooms + huge fungi, grown with bone meal / Fertilizer | 3 | 0201, 0203 (`TreeFeller`) |
| 0207 | [Plantation](0207-plantation.md) | Sugar cane + bamboo beside water channels | 2 | 0201 |
| 0208 | [Nether Wart Farm](0208-nether-wart-farm.md) | Nether wart | 2 | 0201 |
| 0209 | [Berry Farm](0209-berry-farm.md) | Sweet berries, picked | 2 | 0201 |
| 0210 | [Cocoa Plantation](0210-cocoa-plantation.md) | Cocoa on jungle-log supports | 3 | 0201 |
| 0211 | [Cave Vine Farm](0211-cave-vine-farm.md) | Glow berries under a built ceiling | 3 | 0201 |
| 0212 | [Gourd Farm](0212-gourd-farm.md) | Pumpkins + melons, fruit only | 2 | 0201, 0204 |
| 0213 | [Desert Farm](0213-desert-farm.md) | Cactus on sand | 2 | 0201 |

## Brief template

Every brief uses the same shape. Copy this when adding one (name it `PPSS-<feature>.md`):

```markdown
# <Feature>

> **Status:** … · **Phase:** … · **Module:** …
> **Source:** <mod breakdown row> · **Depends on:** … · **Maps to (roadmap):** …

One-paragraph summary.

## Problem & goal
## Requirements
### Functional
### Balance
## Design sketch        # grounded in real classes/paths the feature plugs into
## Scope & non-goals
## Open questions       # decisions owed before/during implementation
## Done when            # acceptance criteria
## References           # roadmap row, mod breakdown, code precedents
```

**Conventions:**
- The **Design sketch** must reference real code (classes, packages, the pattern to mirror) — that's what makes a brief "enough to start."
- Keep balance numbers as *anchors* (tie to existing constants — Macerator ~10k RF buffer / ~200-tick op, Battery 100k / 1k RF-t, cable tiers 30/60/120 RF-t), not final values.
- **Open questions** are first-class: list the real forks and a leaning, don't paper over them.
- Link related briefs with relative links (use the numbered filename); link back to the roadmap row and the `mods/` rationale.
