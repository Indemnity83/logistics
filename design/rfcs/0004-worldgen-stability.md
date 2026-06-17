# RFC 0004: Post-1.0 Material Sourcing & Worldgen Stability

> **Status:** 🟡 Open — needs a Discussion to ratify the policy · **Scope:** cross-cutting (set in Phase 1; governs every material added in Phases 2–3 and beyond) · **Decides:** maintainer
> **Affects:** [`../progression-tiers.md`](../progression-tiers.md) (feedstock/alloy sourcing), [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md) (Base metals · Alloys · High-tier alloys rows), any new-material rows in [`../mods/forestry.md`](../mods/forestry.md) / [`../mods/railcraft.md`](../mods/railcraft.md), and the [`../delivery-plan.md`](../delivery-plan.md) 1.0 Definition of Done · **Blocks:** nothing immediately — it's a standing policy for *how* post-1.0 materials may be introduced

The 1.0 promise is save stability. New materials arrive in **post-1.0** phases (Forestry, Railcraft). If any of those materials depends on **new worldgen ore**, a player's existing 1.0 world won't contain it — a content-availability gap that quietly breaks progression on long-running saves. This RFC sets the policy that keeps that promise.

## Context

Ore placement runs at **chunk generation**. A mod that adds an ore later only affects **newly generated chunks**; already-generated chunks never receive it unless a **retrogen** pass re-applies placement (gated by a per-chunk "features version" flag). So a new ore introduced in (say) Phase 2 is invisible to everything a 1.0 player has already explored — not a crash, but a real gating failure.

This matters less than it sounds, because the design already minimizes bespoke ores:

- The canonical ladder in [`../progression-tiers.md`](../progression-tiers.md) is almost entirely **vanilla** (Copper · Iron · Gold · Diamond · Amethyst · Netherite · Ender · Echo Shard).
- The non-vanilla materials are **alloys** (Bronze, Invar) or **byproducts** — e.g. macerator-byproduct nickel feeds Invar. Neither needs its own ore.
- Tin is the main legacy worldgen ore; high-tier alloys (signalum/lumium/enderium) are explicitly TBD and may never land.

So the worldgen-ore surface is already small by design. The question is what to do when a future material *does* want a fresh ore.

## The decision to make

**When a post-1.0 material needs a raw resource, how do we guarantee it's obtainable in worlds created at 1.0 — without stranding existing saves or seeding speculative dead content?**

## Options

### Option A — Pre-seed worldgen before 1.0
Register every anticipated ore feature now (unused), so all worlds from 1.0 onward contain it everywhere.
- **Pros:** simplest technically (just register features); no retrogen; every world has the ore from day one.
- **Cons:** commits to specific ores / biomes / distributions **before Phases 2–3 are designed** — and scope has been churning (genetics out, electric rails out, steam → Create compat). Dead ores clutter worlds and confuse players. If a distribution is later changed, existing worlds keep the old one — the gap reappears. Brittle against design churn.

### Option B — Commit to retrogen later
Build a version-tracked retro-injector that places new ores into already-generated chunks when a feature ships.
- **Pros:** add only what we actually use, when we use it; no speculative content; existing worlds get the ore.
- **Cons:** retrogen is historically fragile — double-placement, load-time performance, mod-compat edge cases — and must be built and maintained on **both** Fabric and NeoForge.

### Option C — Source new materials *without* new worldgen ore *(leaning — the default)*
Design each new material to be obtainable from already-available inputs: **vanilla ore + alloying + machine byproduct + processing + structure loot / trade**. A material with no chunk-gen dependency behaves identically in old and new worlds.
- **Pros:** dissolves the problem entirely for most materials; fits the mod's alloying/byproduct identity; zero save risk; no fragile infra; already where [`../progression-tiers.md`](../progression-tiers.md) points.
- **Cons:** not every conceivable material can be sourced this way; constrains material design (must tie to an existing input); a few genuinely-mined resources may still need a real ore.

## Recommendation / leaning

**C is the default; B is the fallback.**

1. **Source without worldgen (C) by default.** Every new material should derive from vanilla ores, alloying, byproducts, processing, or structure/loot. This is already the documented sourcing stance — make it an explicit rule.
2. **If a material genuinely must be a fresh worldgen ore, commit to version-tracked retrogen (B)** for it, on both loaders — don't pre-seed.

And: **fold content availability into the 1.0 stability promise.** "No save-breaking changes" should also mean "a 1.0 world stays fully playable through later phases — no material becomes unobtainable on an existing save." (Added to the delivery-plan Definition of Done.)

## Sub-questions still open

- Do we want a **single reusable retrogen utility** in `core.lib` (loader-adapted) ready *before* the first post-1.0 ore, so option B is cheap when needed — or build it lazily on first need?
- Is **structure/loot or trade** an acceptable primary source for a "mined-feeling" material, or does that break the classic-tech feel for some resources?
- For tin (the existing legacy ore): is its current distribution considered settled, or in scope for a future change (which would itself be a soft availability change on old worlds)?
- Where exactly does the policy live long-term — here as the RFC, mirrored as a one-line **principle** in [`../principles.md`](../principles.md)?

## How we'll decide

A short **Ideas Discussion** — this is an infrastructure/policy call, not a taste-heavy one, so it mainly needs maintainer ratification against the save-stability promise and the "minimize bespoke content" principle. Once ratified, flip this RFC to 🟢 Decided, and apply the rule when scheduling any post-1.0 material row in the `mods/` breakdowns.

## References

- Progression ladder & sourcing: [`../progression-tiers.md`](../progression-tiers.md) (vanilla-anchored ladder; Tin/Nickel as feedstock; nickel as macerator byproduct)
- Material rows that this governs: [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md) (Base metals · Alloys · High-tier alloys)
- The stability promise: [`../delivery-plan.md`](../delivery-plan.md) → Versioning & 1.0 → Definition of done
- Principle in tension: [`../principles.md`](../principles.md) (modernize-to-vanilla; minimize bespoke content)
