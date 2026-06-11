# RFC 0002: Forestry Bees — Whether and How

> **Status:** 🟡 Open — needs an Ideas/Polls Discussion before any Phase 2 bee work · **Scope:** Phase 2 (Forestry) · **Decides:** maintainer + community demand signal
> **Affects:** [`../mods/forestry.md`](../mods/forestry.md) § Bees (all TBD rows) · **Blocks:** scheduling any apiculture work — **not** farms, trees, processing, or electronics

Bees are the most nostalgia-loaded and most *divergent* part of Forestry. This RFC settles whether they happen at all for v1, and on what model — before any of it is scheduled.

## Context

Forestry apiculture was a deep system: **princess/drone/queen** lifecycle, **Mendelian genetics** (traits, mutations, species discovery), the Apiary/Alveary, and a centrifuge turning combs into honey/wax/jelly.

The problem: **modern vanilla already has bees**, bred by feeding flowers. That model diverges sharply from Forestry's queen-based genetics. Reconciling them is a real design fork, and Forestry's genetics were famously wiki-dependent — in tension with [`../principles.md`](../principles.md) ("modernize to fit vanilla," "learnable without a wiki," and the multiblock stance, which the Alveary violates).

[`../mods/forestry.md`](../mods/forestry.md) already states bees are **not a Phase 2 headline and may be skipped entirely for v1**, and that this needs a deliberate design pass. Farms are the headline; bees must not gate them.

## The decision to make

**For v1, do Logistics ship bees at all — and if so, do we extend vanilla bees or build a parallel genetics system?**

## Options

### Option A — Skip bees for v1 *(leaning)*
Ship `logistics-forestry` without apiculture. Revisit post-1.0 if demand is there.
- **Pros:** removes the hardest, most divergent, most wiki-prone subsystem from the critical path; lets Forestry ship on its actual headline (farms/trees/processing); matches the breakdown's stated lean.
- **Cons:** "Forestry without bees" disappoints the nostalgia crowd for whom bees *were* Forestry.

### Option B — Extend vanilla bees *(the modernized path if pursued)*
Build on the vanilla beehive/bee-nest + flower breeding. Add Forestry-flavored **products** (combs → a centrifuge → wax/jelly/honey) and *light* traits, **without** the princess/drone/queen lifecycle.
- **Pros:** fits "modernize to fit vanilla"; reuses a system players already understand; delivers the recognizable *outputs* (centrifuge products) without the genetics burden.
- **Cons:** not "real" Forestry bees; purists may find it shallow; still non-trivial work.

### Option C — Full parallel genetics
Reimplement princess/drone/queen + Mendelian mutations + species discovery as a standalone system.
- **Pros:** the most faithful recreation; the deepest endgame.
- **Cons:** very high complexity; fights vanilla bees (two parallel bee systems); maximally wiki-dependent; the Alveary pulls in a multiblock. Highest risk against the principles.

## Recommendation / leaning

**A for v1**, with **B as the eventual modernized path** if bees are pursued post-1.0. **C is unlikely** — it conflicts with multiple principles (modernize-to-vanilla, learnable-without-wiki, multiblock stance) and duplicates a system vanilla now owns. But this is taste- and demand-driven, so it should go to a poll rather than be defaulted.

## Sub-questions still open

- **Demand signal:** do players actually want classic queen-based bees, or nostalgia for a system that aged poorly? (This is the crux — poll it.)
- If B: how deep do traits/products go? Apiary as a new block vs. purely extending the vanilla hive?
- The **Alveary** (advanced multiblock) is a Skip regardless of A/B/C — confirm and record.
- Does anything else in Forestry depend on bee products (e.g. Carpenter recipes using honey/wax)? If so, source those another way under A.

## How we'll decide

Ideas/**Polls** Discussion — bees are nostalgia-heavy, so reaction/vote demand is the right signal. Decide **before** any Phase 2 bee scheduling. Outcome flips the `forestry.md` Bees rows from TBD to Modernize/Skip and, if B, seeds a feature brief.

## References

- Breakdown: [`../mods/forestry.md`](../mods/forestry.md) § Bees (deferred) + the bees TODO
- Roadmap: [`../roadmap.md`](../roadmap.md) → Phase 2 → "Bees (deferred / uncertain)"
- Principles in tension: [`../principles.md`](../principles.md) (modernize-to-vanilla, learnable-without-wiki, multiblock stance)
