# RFC 0001: Programmable Behavior (Gates + Circuits)

> **Status:** 🟡 Open — **deferred post-1.0** (maintainer call, Jun 2026): explicitly **not a 1.0 item**; revisit when Forestry needs circuit boards (Phase 2) or later · **Scope:** Phase 2+ · **Decides:** maintainer + community signal
> **Affects:** [`../mods/buildcraft.md`](../mods/buildcraft.md) (Gates, Pipe wiring, Autarchic gate), [`../mods/forestry.md`](../mods/forestry.md) (Circuit boards + Soldering), [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md) (programmable augments) · **Blocks:** scheduling the BC gate rows and Forestry circuit-board rows — **not** the machines/farms, and **not** the (separate, exploratory) machine-modifier upgrades ([`../features/0106-machine-upgrades.md`](../features/0106-machine-upgrades.md))

The biggest open design call spanning the source mods. Three of them each shipped a "make this block do something conditional" system; the roadmap floats unifying them. This RFC frames the decision.

## Context

Three lineages of "program a block's behavior":

- **BuildCraft gates** — trigger → action logic clipped onto pipes/machines (basic/iron/gold/diamond tiers), with colored pipe wiring to carry signals. *Logic.*
- **Forestry circuit boards + soldering** — program machine/farm behavior by arranging **electron tubes** on a board. *Logic / configuration.*
- **Thermal Expansion augments** — slot-in machine **modifiers** (speed/efficiency/secondary/auto-output). *Modifiers, not logic.*

**Important distinction this RFC draws:** the *modifier* axis (TE augments) is tracked separately as the (exploratory) [machine upgrades](../features/0106-machine-upgrades.md) brief (`0106`) — a different question from this one, and itself not a committed 1.0 item. That is **not** what's contested here. This RFC is specifically about **programmable logic** — gates and circuits — i.e. "when condition X holds, do action Y." The augment work may *share an item vocabulary* (Logistics already has logic chips, cores, and valves that mirror Forestry's electron tubes), but the logic system is a separate, much larger design question.

The tension with [`../principles.md`](../principles.md): programmable logic is high-value for power users but risks violating **"learnable without a wiki"** and is a large, cross-cutting design + implementation effort. Modern Minecraft also already has redstone + comparators, which cover a lot of the simple cases gates once did.

## The decision to make

**Do we build a unified programmable-logic subsystem, lean on vanilla redstone with only targeted per-feature hooks, or defer programmable logic entirely for v1?** And if unified — how deep (trigger→action lists vs. a visual circuit)?

## Options

### Option A — Unified programmable-behavior system
One logic layer pluggable into pipes, machines, and farms: read state (inventory level, energy, network demand, crafting status) → take actions (toggle, emit redstone, gate routing). Crafted from the existing electron-tube-like components (cores/valves/logic chips). Possibly a visual/board editor (Forestry-style) or compact trigger→action rules (BC-gate-style).
- **Pros:** the deepest, most faithful classic-tech automation; one coherent system instead of three; reuses already-seeded components.
- **Cons:** very large design + UX + implementation effort; highest "needs-a-wiki" risk; spans three mods' worth of expectations; easy to over-scope.

### Option B — Vanilla-leaning, targeted hooks *(leaning for v1)*
No general programming system. Lean on vanilla redstone/comparators, and add only **narrow, per-feature** config that covers the cases people actually reach for. Concretely: an **inverted wooden (extraction) pipe**, **redstone-controlled extraction/routing**, a pipe/machine that emits redstone on a condition, farm enable/disable. Reuse the existing module config UIs.
- **Pros:** low complexity; ships incrementally; stays "learnable"; doesn't gate 1.0.
- **Cons:** less power-user depth; "gates" as a distinct feature effectively becomes "skip / use redstone"; no single programmable layer.

### Option C — Skip programmable logic for v1
Ship machines/farms with fixed behavior + basic redstone hooks; defer all gates/circuits post-1.0.
- **Pros:** simplest; clearest 1.0 scope.
- **Cons:** leaves a recognizable classic mechanic absent; punts rather than decides the unification.

## Recommendation / leaning

**B for now; A is explicitly post-1.0 — deferred until Forestry needs it (decided, Jun 2026).** Rationale: 1.0 = the Phase 1 automation core complete ([`../delivery-plan.md`](../delivery-plan.md)); a general programming system is *not* required for that and would be a major scope risk. The maintainer has confirmed the unified system is **not a 1.0 item** — the natural trigger to revisit it is Forestry **circuit boards** (programming Forestry blocks, Phase 2), or later. Until then, targeted redstone hooks (B) cover the common cases; C is the fallback if even those aren't wanted. When revisited, let community demand (poll) weigh A's complexity.

*Review concurrence (Jun 2026):* a contributor independently landed on the same call — Option B for v1 (purpose-built solutions like an inverted wooden pipe and redstone-controlled extraction/routing), with C as the acceptable fallback, agreeing that a unified gates/circuits layer is too much scope for something not needed at 1.0.

## Sub-questions still open

- What machine/network **state** should be readable (levels, energy, demand, crafting in-progress)?
- What **actions** are in scope (redstone out, toggle, route/divert)?
- If A: visual board vs. trigger→action rule lists? How does it avoid being wiki-dependent?
- How much do the existing **cores/valves/logic chips** become the crafting vocabulary regardless of which option wins?
- Does the **Autarchic gate** (self-pulsing engine) just fold into engine/extractor config (already noted as "Modernize" in `buildcraft.md`) independent of this RFC? *(Likely yes — handle separately.)*

## How we'll decide

Ideas/Polls **Discussion** (it spans three mods and is taste-heavy). Weigh against the complexity budget and the "learnable without a wiki" principle. The outcome flips the affected TBD rows in the breakdowns to Port/Modernize/Skip and, if A/B, seeds a feature brief.

## References

- Roadmap RFC note: [`../delivery-plan.md`](../delivery-plan.md) → Phase 1 → "RFC (cross-cutting)"
- Breakdowns: [`../mods/buildcraft.md`](../mods/buildcraft.md) (Gates/wiring rows + TODO), [`../mods/forestry.md`](../mods/forestry.md) (Circuit boards row + TODO), [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md) (Augments)
- Related feature brief (the modifier axis, *not* this RFC): [`../features/0106-machine-upgrades.md`](../features/0106-machine-upgrades.md)
- Already-seeded components: `core` cores / valves / logic chips
