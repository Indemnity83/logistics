# RFCs — Contested Design Decisions

This folder holds **RFCs**: write-ups of the genuinely *unsettled, high-impact* design calls — the ones [`../principles.md`](../principles.md) says to mark **TBD** and route to a **Discussion** rather than silently default. An RFC frames a decision; it does not assume one.

**RFC ≠ feature brief.** A [feature brief](../features/) describes accepted work, *start-ready* and code-grounded. An RFC describes a **fork in the road** — the question, the options with trade-offs, a leaning, and how we'll decide. Briefs are for building; RFCs are for *choosing*, usually before a brief can even be written.

## Operating model

Consistent with the [`../README.md`](../README.md) operating model (**markdown-first, then the board, feedback in Discussions**):

1. **Author the RFC here** — capture the question, options, and leaning in markdown (reviewable in a PR).
2. **Open a GitHub Discussion** (Ideas, or **Polls** when demand signal matters) linking the RFC. This is where the call actually gets made.
3. **Record the outcome** — flip the relevant **TBD** rows in the [`../mods/`](../mods/) breakdowns to `Port` / `Modernize` / `Skip`, and (if accepted) seed a [feature brief](../features/). Update the RFC's status to ✅ Decided with a one-line outcome + link.

RFCs are numbered sequentially (`NNNN-`), independent of the phase/step build order used by feature briefs — a decision isn't a build step.

## Open RFCs

| # | RFC | Scope | The question | Leaning |
|---|---|---|---|---|
| 0001 | [Programmable Behavior](0001-programmable-behavior.md) | Phase 2+ (post-1.0) | Unified gates+circuits logic system, vanilla-leaning hooks, or skip for v1? | **Deferred post-1.0** (maintainer): not a 1.0 item; revisit when Forestry needs circuit boards |
| 0002 | [Forestry Genetics](0002-forestry-bees.md) | Phase 2 (Forestry) | Do bees/trees/butterflies (genetics) happen at all? | ✅ **Decided: out of scope** — genetics belongs in a separate, dedicated mod |
| 0003 | [Railcraft Multiblocks & Steam](0003-railcraft-multiblocks.md) | Phase 3 (Transport) | Single-block / tileable / multiblock per signature block; is steam a power tier? | Single-block machines; tileable bulk tanks; **steam via a Create compat layer** (else simplified) |
| 0004 | [Material Sourcing & Worldgen Stability](0004-worldgen-stability.md) | Cross-cutting (post-1.0 materials) | How do we keep post-1.0 materials obtainable on existing 1.0 worlds without dead pre-seeded ore? | Source without new worldgen ore by default; retrogen as fallback; pre-seed only when certain |

> The **leaning** column is a starting position to argue against, not a decision. Each RFC carries the full options + trade-offs.

## RFC template

Copy this when adding one (name it `NNNN-<topic>.md`):

```markdown
# RFC NNNN: <Title>

> **Status:** 🟡 Open — needs Discussion · **Scope:** … · **Decides:** …
> **Affects:** <breakdown rows> · **Blocks:** <what scheduling this gates>

## Context
## The decision to make        # the crisp question
## Options                     # each: what / pros / cons / effort
## Recommendation / leaning
## Sub-questions still open
## How we'll decide            # criteria: balance, complexity, community poll
## References
```

(Decision-table RFCs — like 0003 — may swap the Options list for a "Recommended disposition" table when the call is per-item rather than one global fork.)
