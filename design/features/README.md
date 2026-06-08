# Feature Briefs

This folder expands settled [`roadmap.md`](../roadmap.md) rows into **feature briefs** — one file per feature, detailed enough to *start the work*. Each brief is grounded in the actual codebase: it names the real classes and patterns a feature plugs into, so an implementer isn't starting from a blank page.

Briefs sit **between** the durable "why" (the [`mods/`](../mods/) breakdowns, [`vision.md`](../vision.md), [`principles.md`](../principles.md)) and the live work tracking (GitHub Project #4). The breakdown says *what we decided and why*; the brief says *what to build and how to start*; the Project issue tracks *the work*. When a brief settles, decompose it onto the board (see the roadmap's [Mapping to the board](../roadmap.md#mapping-to-the-board)).

> These are **starting points, not specs set in stone**. Every brief carries an "Open questions" section — the decisions still owed before or during implementation. Resolve the blocking ones (often via a Discussion or a short spike) before scheduling.

## Naming scheme

Files are prefixed **`PPSS-`** — two digits of **phase**, two digits of **step** — so they sort by build order within their phase and stay grouped as later phases' briefs land in this shared folder.

- `01xx` = Phase 1 (Automation core), `02xx` = Phase 2 (Forestry), `03xx` = Phase 3 (Transport).
- The step is the **suggested build order** within the phase, dependency-aware (a feature's prerequisites have lower step numbers). It's a guide, not a contract — parallel tracks exist (see below).
- New briefs in a batch continue the sequence (this batch ends at `0109`; the deferred fluid-blocked items become `0110+`).

## This batch: Phase 1 keystone + ready-now

The first batch covers the **[Fluids foundation](0101-fluids-foundation.md)** (the keystone that unblocks much of Phases 1–3) plus the Phase 1 items with **no blockers** — the work that can start immediately.

| # | Brief | Area | Decision | Depends on |
|---|---|---|---|---|
| 0101 | [Fluids Foundation](0101-fluids-foundation.md) | 🔑 keystone | Port/Modernize | — (platform layer already built) |
| 0102 | [Macerator Secondary Outputs](0102-macerator-secondary-outputs.md) | Machines | Modernize | — |
| 0103 | [Sawmill](0103-sawmill.md) | Machines | Port | `ChanceResult` (0102) |
| 0104 | [Alloy Smelter](0104-alloy-smelter.md) | Machines | Port | `ChanceResult` (0102) |
| 0105 | [Machine Upgrades / Augments](0105-machine-upgrades.md) | Machines (cross-cutting) | Modernize | the machines (0102–0104) |
| 0106 | [Tiered Batteries](0106-tiered-batteries.md) | Power | Modernize | — (extends Battery) |
| 0107 | [Obsidian Vacuum Pipe](0107-obsidian-vacuum-pipe.md) | Pipes | Port | — |
| 0108 | [Remote Orderer](0108-remote-orderer.md) | Logistics QoL | Modernize | — |
| 0109 | [Firewall Pipe](0109-firewall-pipe.md) | Logistics advanced | Port | — |

### Reading the order

The step numbers encode dependencies, but several briefs are independent and can run in parallel:

- **Start first — `0101` Fluids:** the keystone. Begin with its design spike (parallel fluid pipe vs. network-integrated) since it unblocks the entire deferred batch.
- **Machine chain — `0102` → `0103`/`0104` → `0105`:** build [Macerator Secondary Outputs](0102-macerator-secondary-outputs.md) first; it produces the shared `ChanceResult` mechanism that [Sawmill](0103-sawmill.md) (sawdust) and [Alloy Smelter](0104-alloy-smelter.md) (slag) reuse. Then layer [Machine Upgrades](0105-machine-upgrades.md) across all of them.
- **Independent (any time) — `0106`/`0107`/`0108`:** [Tiered Batteries](0106-tiered-batteries.md), [Obsidian Vacuum Pipe](0107-obsidian-vacuum-pipe.md), [Remote Orderer](0108-remote-orderer.md) have no in-batch dependencies.
- **Spike before scheduling — `0101` and `0109`:** the [Fluids](0101-fluids-foundation.md) pipe approach and the [Firewall Pipe](0109-firewall-pipe.md) (routing-gate vs. graph-segmentation, which touches stable network code) each need a short spike to settle the approach.

### Deferred to a later batch (Phase 1, fluid-blocked or needs a Discussion)

Not written yet — they wait on the keystone or on an open decision, and will take `0110+` steps:

- **Needs fluids first:** Combustion-tier engine, Magmatic/dynamo tier, Magma Crucible, Fluid Transposer, fluid logistics (provider/supplier/request), Pump (built as the Fluids-foundation validation slice), the oil/biofuel fuel chain.
- **Needs a Discussion (RFC, not a feature brief yet):** the programmable-automation / gates system (BuildCraft gates + TE augments + Forestry circuits) — see [`../mods/buildcraft.md`](../mods/buildcraft.md).
- **Already done:** pipe operation power gating (✅, #464/#465/#469).

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
