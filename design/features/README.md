# Feature Briefs

This folder expands settled [`delivery-plan.md`](../delivery-plan.md) rows into **feature briefs** — one file per feature, detailed enough to *start the work*. Each brief is grounded in the actual codebase: it names the real classes and patterns a feature plugs into, so an implementer isn't starting from a blank page.

Briefs sit **between** the durable "why" (the [`mods/`](../mods/) breakdowns, [`vision.md`](../vision.md), [`principles.md`](../principles.md)) and the live work tracking (GitHub Project #4). The breakdown says *what we decided and why*; the brief says *what to build and how to start*; the Project issue tracks *the work*. When a brief settles, decompose it onto the board (see the roadmap's [Mapping to the board](../delivery-plan.md#mapping-to-the-board)).

> These are **starting points, not specs set in stone**. Every brief carries an "Open questions" section — the decisions still owed before or during implementation. Resolve the blocking ones (often via a Discussion or a short spike) before scheduling.

## Naming scheme

Files are prefixed **`PPSS-`** — two digits of **phase**, two digits of **step** — so they sort by build order within their phase and stay grouped as later phases' briefs land in this shared folder.

- `01xx` = Phase 1 (Automation core), `02xx` = Phase 2 (Forestry), `03xx` = Phase 3 (Transport).
- The step is the **suggested build order** within the phase, dependency-aware (a feature's prerequisites have lower step numbers). It's a guide, not a contract — parallel tracks exist (see below).
- New briefs in a batch continue the sequence (this batch ends at `0110`; the deferred fluid-blocked items become `0111+`). When build order changes, briefs are renumbered so the ids keep tracking it.

## This batch: Phase 1 keystone + ready-now

The first batch covers the **[Fluids foundation](0101-fluids-foundation.md)** (the keystone that unblocks much of Phases 1–3) plus the Phase 1 items with **no blockers** — the work that can start immediately.

| # | Brief | Area | Decision | Depends on |
|---|---|---|---|---|
| 0101 | [Fluids Foundation](0101-fluids-foundation.md) | 🔑 keystone | Port/Modernize | — (platform layer already built) |
| 0102 | [Macerator Secondary Outputs](0102-macerator-secondary-outputs.md) | Machines | Modernize | — |
| 0103 | [Hand Grinder](0103-hand-grinder.md) | Machines (no-power on-ramp) | Modernize | Macerator recipes |
| 0104 | [Sawmill](0104-sawmill.md) | Machines | Port | `ChanceResult` (0102) |
| 0105 | [Alloy Smelter](0105-alloy-smelter.md) | Machines + materials | Port/Modernize | `ChanceResult` (0102), Hand Grinder (0103) |
| 0106 | [Machine Upgrades / Augments](0106-machine-upgrades.md) 🔍 *exploratory — not committed for 1.0* | Machines (cross-cutting) | Modernize | the machines (0102 / 0104 / 0105) |
| 0107 | [Tiered Batteries](0107-tiered-batteries.md) | Power | Modernize | — (extends Battery) |
| 0108 | [Obsidian Vacuum Pipe](0108-obsidian-vacuum-pipe.md) ❌ *not planned — hoppers cover it* | Pipes | Skip | — |
| 0109 | [Remote Orderer](0109-remote-orderer.md) 🔍 *exploratory — not committed* | Logistics QoL | Modernize | — |
| 0110 | [Firewall Pipe](0110-firewall-pipe.md) | Logistics advanced | Port | — |

### Reading the order

The step numbers encode dependencies, but several briefs are independent and can run in parallel:

- **Shipped — `0101` Fluids:** the keystone, ✅ shipped in v0.7.3 (standalone cellular fluid pipes + Glass Tank + Pump on both loaders). This unblocked the deferred fluid batch below.
- **Machine / dust chain — `0102` → `0103` → `0104` / `0105` → `0106`:** build [Macerator Secondary Outputs](0102-macerator-secondary-outputs.md) first (the shared `ChanceResult` mechanism); then the [Hand Grinder](0103-hand-grinder.md) (the no-power ore→dust on-ramp, prerequisite for the Alloy Smelter's no-power Bronze path). [Sawmill](0104-sawmill.md) and [Alloy Smelter](0105-alloy-smelter.md) reuse `ChanceResult` (Alloy also needs the Hand Grinder). [Machine Upgrades](0106-machine-upgrades.md) could later layer across them, but it's **exploratory** (per [`ROADMAP.md`](../../ROADMAP.md) Exploring/RFC), not part of the committed batch.
- **Independent (any time) — `0107`:** [Tiered Batteries](0107-tiered-batteries.md) has no in-batch dependencies. ([Obsidian Vacuum Pipe](0108-obsidian-vacuum-pipe.md) is now **not planned** — hoppers cover it; [Remote Orderer](0109-remote-orderer.md) is **exploratory**, not committed — see [`ROADMAP.md`](../../ROADMAP.md).)
- **Spike before scheduling — `0110`:** the [Firewall Pipe](0110-firewall-pipe.md) (routing-gate vs. graph-segmentation, which touches stable network code) needs a short spike to settle the approach. *(The [Fluids](0101-fluids-foundation.md) approach is settled and shipped.)*

### Deferred to a later batch (Phase 1, fluid-blocked or needs a Discussion)

Not written yet — they wait on the keystone or on an open decision, and will take `0111+` steps:

- **Needs fluids first** *(now unblocked — fluids shipped):* Combustion-tier engine, Magmatic/dynamo tier, Magma Crucible, Fluid Transposer, fluid logistics (provider/supplier/request), the oil/biofuel fuel chain. *(The Pump shipped with the Fluids foundation as its validation slice.)*
- **Deferred post-1.0 (RFC, not a feature brief):** the programmable-automation / gates+circuits system (BuildCraft gates + TE programmable augments + Forestry circuits) — **not a 1.0 item**; revisit when Forestry needs circuit boards (Phase 2) or later. See [`../rfcs/0001-programmable-behavior.md`](../rfcs/0001-programmable-behavior.md). *(Distinct from the machine-modifier upgrades, [0106](0106-machine-upgrades.md) — themselves exploratory, not committed for 1.0.)*
- **Already done:** pipe operation power gating (✅, #464/#465/#469); the Fluids foundation (✅, v0.7.3 — fluid pipes, Glass Tank, Pump, machine fluid I/O).

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
