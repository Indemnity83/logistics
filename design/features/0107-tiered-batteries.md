# Tiered Energy-Storage Line (Batteries)

> **Status:** ✅ Shipped (storage ladder) · 🚧 Open (configurable I/O) · **Phase:** 1 — Automation core · **Module:** `logistics-automation` (`power` domain)
> **Source:** [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md) (Energy Cells: Leadstone→Resonant) · **Depends on:** the infused alloys ([`0112`](0112-infused-alloys.md))
> **Maps to (roadmap):** Phase 1 — Battery → tiered energy-storage line

The single Battery became a four-tier line on the energy ladder — **Basic · Conductive · Resonant ·
Deep** — each crafted whole from a block of its own storage material.

| Tier | Capacity | Max I/O per side | Push per side | Core | Terminals |
|---|---|---|---|---|---|
| **Basic** | 100,000 | 1,000 | 200 | Redstone Block | Copper Nugget |
| **Conductive** | 400,000 | 2,000 | 400 | Block of Caterium | Caterium Nugget |
| **Resonant** | 1,600,000 | 4,000 | 800 | Block of Lumenite | Lumenite Nugget |
| **Deep** | 6,400,000 | 8,000 | 1,600 | Block of Echonite | Echonite Nugget |

×4 capacity and ×2 I/O per step — a 64× spread, the same *shape* as TE's ~160× without copying its
absolute values, per "copy the shape, not the numbers" below. Every number is config-driven, one
section per tier (`power.battery.<tier>`).

## The recipe

One silhouette across the whole line: **clay packed around a block of the tier's storage material,
with two nuggets of its conductor as terminals.**

```
N C N      N = nugget of the tier's conductor (terminals)
C B C      C = clay ball (the housing)
C C C      B = block of the tier's storage material (the core)
```

This is why the alloys ship in block *and* nugget form — neither is decorative, both are consumed
here. It also matches the art: the battery's top face carries two metal posts, and the housing reads
as fired clay.

**Basic is deliberately the odd one out.** Its core is a Redstone Block while its terminals are
copper — *store* versus *transmit*. Redstone is the storage material at that rank and has no nugget
form, so copper (the Basic conductor, and the Basic cable's body) supplies the terminals. Every tier
above it uses one alloy for both jobs, because an infused alloy is both the store and the conductor.

Keeping the Redstone Block at the centre also means **the Basic recipe barely moves** from the
pre-tier Battery, which already had a redstone block in the middle.

## Why not craft-then-fill

An earlier revision built the upper tiers as `<Tier> Battery (Empty)` items filled with a fluid in
the Transposer, copying Thermal's Fluxduct pattern. That shipped three single-purpose items — an
"Empty Gold Battery" can only ever become a battery — and asked the player to learn a mechanic used
in exactly one place.

The **infused alloys** ([`0112`](0112-infused-alloys.md)) moved the fill one step upstream, onto the
material. The fluid gate is still there and still unskippable, but now it buys a reusable metal that
bodies the cable, the battery, and whatever later machine wants that rank. Net item count is level,
and the concept count drops from two to one.

## Naming

Tier adjectives, not material names, matching [`0112`](0112-infused-alloys.md) and the cable line.
Three of the four bodies are *invented* alloys whose names carry no ladder position — "Caterium
Battery" says nothing about where it sits, while "Conductive Battery" names the rank. Material
identity still does the visual work through the accent colour on each housing.

## Compatibility

**Basic carries the pre-tier Battery's exact numbers** (100,000 RF / 1,000 / 200) and
`logistics:power/battery` aliases to it, so a world saved before the line existed loads unchanged.

The alias target is chosen by **storage parity, not recipe parity**: a saved battery's stored energy
rides along in `block_entity_data`, so aliasing to a smaller tier would silently clamp and destroy
it. A test pins Basic's three numbers for exactly this reason.

The block entity type keeps its own `power/battery` id and needs no alias — only the block and item
ids moved. Aliasing a name that is still the live one throws on registration.

## Still open — configurable I/O

The original brief's headline UX was **player-configurable input/output rates** ("so a cell could act
as a buffer, a limiter, or a burst source"). That did **not** ship. Rates are per-tier and adjustable
only through the config file or `/logistics config power.battery.<tier> <key> <value>`, not per block
in-game.

This remains the most valuable follow-up, and the original design work still stands:

- **Storage:** data component / BE NBT holding `maxInput`/`maxOutput`, edited in a small GUI,
  following the `PipeDataComponents` precedent — preferred over block-state `IntegerProperties`
  cycled by wrench, which is coarse and bloats blockstates.
- `EnergyComponent` already takes max-insert/max-extract, so the configured values feed a wrapper
  that clamps to the lower of (tier max, configured). **Config throttles down, never up.**
- **Per-side** I/O is the classic Energy Cell behaviour and the natural fast-follow.

## Rate alignment vs TE

| | Logistics | TE Energy Cells |
|---|---|---|
| Tiers | 4 — Basic / Conductive / Resonant / Deep | 5 — Leadstone → Resonant |
| Spread | 64× | ~160× |
| Configurable I/O | ❌ not yet | ✅ |

**Aligned on *philosophy*, deliberately *not* on absolute rates.** Our RF economy is ~2 orders of
magnitude smaller (a Macerator draws ~10 RF/t; a Stirling outputs 3–10 RF/t), so TE's numbers would
be wildly oversized. **Copy the shape, not the numbers.**

Battery I/O is the *direct per-face* limit and already exceeds cable throughput at every tier
(1,000 RF/t against a Basic cable's 30), so through a network the cable is always the smaller pipe.
Battery rates were sized against machine demand, not to saturate a cable.

## Scope & non-goals

- **In (shipped):** four tiers, per-tier capacity/throughput, one recipe silhouette, in-world and
  item charge display scaled per tier, registry alias from the untiered Battery.
- **Open:** configurable I/O (per-block, then per-side).
- **Out:** wireless/cross-dim transfer (Tesseract — out of scope per the TE breakdown),
  redstone-controlled output modes (a later augment), in-world upgrade kits (see
  [`0106-machine-upgrades.md`](0106-machine-upgrades.md)).
- **Out:** any Machine Frame in energy storage. The frame is reserved for the structural ladder —
  see [`0111-machine-frame-tiers.md`](0111-machine-frame-tiers.md). An earlier revision used a
  Transposer-filled Machine Frame and it was removed for exactly this reason; do not reintroduce it.

## Done when

- [x] Four battery tiers place, store, and transfer energy with distinct capacity/throughput on both
      loaders.
- [x] Charge shows in-world and on the item for all tiers, scaled to each tier's own capacity.
- [x] A world saved before the line loads with its batteries intact at Basic's stats.
- [x] Every tier is crafted whole — no empty item, no fill step.
- [x] Energy storage consumes no Machine Frame.
- [ ] I/O rates are configurable in-game and clamp correctly (never exceed tier max), persisted
      across save/load and break/place.
- [ ] **Verified on a real pre-0.9 world, on both loaders.** `PlatformService.registerAlias` is
      implemented per-loader, so a Fabric-only pass proves nothing.

## References

- Roadmap: [`../delivery-plan.md`](../delivery-plan.md) → Phase 1 → Battery (tiers); [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md) → "Energy Cells" row
- Bodies: [`0112-infused-alloys.md`](0112-infused-alloys.md)
- Code: `power/block/{BatteryTier,BatteryBlock,BatteryBlockItem}`, `power/block/entity/BatteryBlockEntity`,
  `core/lib/power/AbstractBatteryBlockEntity`, registration + `ALIAS` in `LogisticsPower`
- Tier precedent: `power/cable/{CableTier,CableBlock}`; data-component precedent `pipe/data/PipeDataComponents`
