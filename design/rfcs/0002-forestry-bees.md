# RFC 0002: Forestry Genetics (Bees, Trees, Butterflies) — Whether and How

> **Status:** 🟢 Decided — **genetics is out of scope for Logistics** (maintainer + contributor call, Jun 2026). No bees, no tree-breeding, no butterflies for v1; revisit only as a *separate, dedicated mod* if demand ever warrants. · **Scope:** Phase 2 (Forestry) · **Decided by:** maintainer, with contributor concurrence
> **Affects:** [`../mods/forestry.md`](../mods/forestry.md) § Bees + § Trees (arboriculture/breeding rows) · **Blocks:** nothing in the Logistics roadmap — Forestry ships on farms, processing, power, and electronics **without** any genetics system

Bees are the most nostalgia-loaded and most *divergent* part of Forestry — and the same is true of tree breeding and butterflies. This RFC originally asked whether *bees* happen for v1; the call has since broadened: **the whole genetics/biology axis (bees, trees, butterflies) is out of scope for this mod.** What follows is preserved as the reasoning behind that decision.

## Context

Forestry apiculture was a deep system: **princess/drone/queen** lifecycle, **Mendelian genetics** (traits, mutations, species discovery), the Apiary/Alveary, and a centrifuge turning combs into honey/wax/jelly.

The problem: **modern vanilla already has bees**, bred by feeding flowers. That model diverges sharply from Forestry's queen-based genetics. Reconciling them is a real design fork, and Forestry's genetics were famously wiki-dependent — in tension with [`../principles.md`](../principles.md) ("modernize to fit vanilla," "learnable without a wiki," and the multiblock stance, which the Alveary violates).

[`../mods/forestry.md`](../mods/forestry.md) already states bees are **not a Phase 2 headline and may be skipped entirely for v1**, and that this needs a deliberate design pass. Farms are the headline; bees must not gate them.

## The decision to make

**For v1, do Logistics ship bees at all — and if so, do we extend vanilla bees or build a parallel genetics system?**

## Options

### Option A — Skip bees for v1 *(leaning)*
Ship `logistics-forestry` without apiculture. Revisit post-1.0 if demand is there.
- **Pros:** removes the hardest, most divergent, most wiki-prone subsystem from the critical path; lets Forestry ship on its actual headline (farms/processing/electronics); matches the breakdown's stated lean.
- **Cons:** "Forestry without bees" disappoints the nostalgia crowd for whom bees *were* Forestry.

### Option B — Extend vanilla bees *(the modernized path if pursued)*
Build on the vanilla beehive/bee-nest + flower breeding. Add Forestry-flavored **products** (combs → a centrifuge → wax/jelly/honey) and *light* traits, **without** the princess/drone/queen lifecycle.
- **Pros:** fits "modernize to fit vanilla"; reuses a system players already understand; delivers the recognizable *outputs* (centrifuge products) without the genetics burden.
- **Cons:** not "real" Forestry bees; purists may find it shallow; still non-trivial work.

### Option C — Full parallel genetics
Reimplement princess/drone/queen + Mendelian mutations + species discovery as a standalone system.
- **Pros:** the most faithful recreation; the deepest endgame.
- **Cons:** very high complexity; fights vanilla bees (two parallel bee systems); maximally wiki-dependent; the Alveary pulls in a multiblock. Highest risk against the principles.

## Decision (Jun 2026)

**Option A — skip, and broaden it: genetics is not Logistics' job.** The maintainer call (with contributor concurrence) is that **none of the genetics/biology systems — bees, tree-breeding/arboriculture, butterflies — belong in this mod.** Forestry's strongest fit within Logistics is the *industrial* side: farms, automation, processing chains, and infrastructure. The genetics side is extremely complex, highly design-dependent, and historically wiki-dependent; it would dominate the Forestry module and fight several principles (modernize-to-vanilla, learnable-without-wiki, multiblock stance).

If breeding-style genetics is ever pursued, it should be its **own dedicated mod** (ours or someone else's), not bolted onto Logistics. Option B (extend vanilla bees for the recognizable *products*) and Option C (full parallel genetics) are kept below only as the shape such a separate effort might take — they are **not** on the Logistics roadmap.

## Sub-questions still open

- **Demand signal:** do players actually want classic queen-based bees, or nostalgia for a system that aged poorly? (This is the crux — poll it.)
- If B: how deep do traits/products go? Apiary as a new block vs. purely extending the vanilla hive?
- The **Alveary** (advanced multiblock) is a Skip regardless of A/B/C — confirm and record.
- Does anything else in Forestry depend on bee products (e.g. Carpenter recipes using honey/wax)? If so, source those another way under A.

## Historical note

This was originally expected to need an Ideas/**Polls** Discussion because bees are nostalgia-heavy. The June 2026 maintainer + contributor call closed it earlier: genetics is out of scope for Logistics, and any future genetics work should live in a separate dedicated mod.

## References

- Breakdown: [`../mods/forestry.md`](../mods/forestry.md) § Bees + § Trees (both out of scope)
- Roadmap: [`../delivery-plan.md`](../delivery-plan.md) → Phase 2 → "Bees / genetics (out of scope)"
- Principles in tension: [`../principles.md`](../principles.md) (modernize-to-vanilla, learnable-without-wiki, multiblock stance)
