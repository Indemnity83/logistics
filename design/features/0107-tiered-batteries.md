# Tiered Energy-Storage Line (Batteries)

> **Status:** ✅ Shipped (storage ladder) · 🚧 Open (configurable I/O) · **Phase:** 1 — Automation core · **Module:** `logistics-automation` (`power` domain)
> **Source:** [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md) (Energy Cells: Leadstone→Resonant) · **Depends on:** the Transposer (v0.8.6) and liquid redstone
> **Maps to (roadmap):** Phase 1 — Battery → tiered energy-storage line

The single Battery became a five-tier line — **Tin · Copper · Gold · Amethyst · Echo** — whose
upper three tiers are built around the mod's existing **Machine Frame, filled with liquid redstone
in the Transposer**, in the spirit of how Thermal Expansion builds its Energy Cells.

**No frame at all — the battery is what you fill.** Thermal ships *two* fill patterns, and the one
worth copying is the simpler of them:

| Thermal mechanism | Steps |
|---|---|
| **Energy Cells** | craft frame → fill frame → craft cell around frame — **three** |
| **Fluxducts** | craft `… Fluxduct (Empty)` → fill it → working duct — **two** |

We take the Fluxduct pattern. There is no frame or core item: the upper tiers are crafted as
`<Tier> Battery (Empty)` and filled with liquid redstone to become the battery itself. That keeps
the Machine Frame entirely out of energy storage (it is reserved for the structural ladder — see
[`0111-machine-frame-tiers.md`](0111-machine-frame-tiers.md)) and avoids reviving the "cores" line
[`../progression-tiers.md`](../progression-tiers.md) dropped.

**This supersedes the original brief**, which specified three plain crafted tiers (Copper/Gold/Ender)
and explicitly scoped "energy-cell 'frames' cosmetic variants" *out*. Frames turned out not to be
cosmetic: in TE they are the tiering mechanism itself.

## What Thermal actually does (verified against the 1.12.2 jars)

Worth recording, because the mechanic is easy to misremember as "the fluid picks the tier":

| TE item | Behaviour |
|---|---|
| `frameCell` — Energy Cell Frame | crafted, **no fill** |
| `frameCell1` — Hardened Cell Frame | crafted, **no fill** |
| `frameCell2/3/4` — Reinforced / Signalum / Resonant Cell Frame **(Empty)** | + **4,000 mB Destabilized Redstone**, 16,000 RF → **(Full)** |

`TransposerManager` loads `TFFluids.fluidRedstone` **once** and reuses it for all three fills.
Thermal Dynamics' Fluxducts behave identically — Reinforced, Signalum *and* Resonant all fill with
200 mB of the same redstone; only the Cryo-Stabilized tier differs (500 mB Cryotheum).

**So the fluid never selects the tier.** The tier comes from which empty frame was crafted, from
tier-specific metals; the fill is a one-shot activation, and the lowest tiers skip it. That is the
model implemented here.

Separately: TE tiers *machines* with **Conversion / Upgrade Kits** (`ItemUpgrade` in
ThermalFoundation — `incremental1..4` requiring the preceding tier, `full1..4` usable on any lower
tier), applied in-world to a placed block. There is exactly one `frameMachine`. The tiered-frame
ladder is the Energy Cell's alone. If machine tiers are ever revisited, kits — not frames — are the
TE-faithful precedent; see [`0106-machine-upgrades.md`](0106-machine-upgrades.md).

## What shipped

**Five tiers**, each a distinct block + item over one shared `BlockEntityType`, mirroring the
`CableTier`/`CableBlock` arrangement. `BatteryTier` carries the id and display name and reads its
numbers from per-tier config sections (`power/battery/<tier>`).

| Tier | Capacity | Max I/O per side | Push per side | Body | Centre | Then |
|---|---|---|---|---|---|---|
| Tin | 25,000 | 500 | 100 | Tin Ingot | Redstone Block | — done |
| Copper | 100,000 | 1,000 | 200 | Copper Ingot | Redstone Block | — done |
| Gold | 400,000 | 2,000 | 400 | Gold Ingot | Quartz Crystal | + 1,000 mB · 8,000 RF |
| Amethyst | 1,600,000 | 4,000 | 800 | Amethyst Shard | Quartz Crystal | + 2,000 mB · 12,000 RF |
| Echo | 6,400,000 | 8,000 | 1,600 | Echo Shard | Quartz Crystal | + 4,000 mB · 16,000 RF |

**The centre slot carries the progression.** The low tiers pack in *solid* redstone. Past a certain
capacity solid redstone is not dense enough, so the centre becomes a **quartz-crystal vessel** and
the redstone arrives as a liquid from the Transposer. That is why a machine enters the chain at all,
rather than the fluid step being an arbitrary toll. `core/quartz_crystal` is the mod's existing glass
block (`noOcclusion`, `SoundType.GLASS`, smelted from quartz dust) and is already the glass component
in all 15 valve recipes — no new material was needed.

All five share one recipe silhouette, so the ladder reads as a family; only the body material and
the centre change. **The upper three have no crafting recipe of their own** — the Transposer is the
only route to them, so the fluid gate cannot be skipped.

×4 capacity and ×2 I/O per step — a 256× spread, the same *shape* as TE's ~160× without copying its
absolute values, per "copy the shape, not the numbers" below.

**Copper carries the pre-tier Battery's exact numbers** (100,000 RF / 1,000 / 200) and
`logistics:power/battery` aliases to it, so a world saved before the line existed loads unchanged.
Tin was added *underneath* Copper rather than displacing it, precisely so that stays true.
The block entity type keeps its own `power/battery` id and needs no alias — only the block and item
ids moved.

**The fill step is pure data.** `TransposerRecipe` already accepted arbitrary `item + fluid → item`
(`fill_black_concrete.json` is the same shape), and liquid redstone was already obtainable from the
Crucible, so the fill chain is three recipe files and no JEI work.

The only new registry entries are the three **`<tier>_battery_empty` items**. They are items rather
than blocks deliberately: the fill happens in a machine slot, so the empty has to be an item to go
in, and making it placeable would only let a player place a battery that stores nothing.

**Ladder choice.** Copper/Gold/Amethyst come from the decided cable subset in
[`../progression-tiers.md`](../progression-tiers.md); Bronze fills the early-alloy gap at rank 3;
**Echo Shard** tops the line at rank 9 ("Deep … ultimate") rather than Ender at rank 8. Batteries and
cables therefore **diverge at the top** — Ender is "Dimensional", which suits transport, while Echo
is the ultimate storage tier.

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
- **Per-side** I/O is the classic Energy Cell behaviour and the natural fast-follow after
  whole-block.

## Rate alignment vs TE energy ducts

| | Logistics | TE Fluxducts |
|---|---|---|
| Tiers | 3 cables — Copper / Gold / Ender | 5 + unlimited — Leadstone / Hardened / Redstone / Signalum / Resonant / Cryo |
| Rate (RF/t) | 30 / 60 / 120 | 200 / 800 / 8,000 / … / 32,000 / ∞ |
| Per-tier ratio | ×2 | ~×4–×10 |
| Total spread | 4× | ~160× (+∞) |
| Loss / buffer | lossless, no buffer | lossless, no buffer |

**Aligned on *philosophy*, deliberately *not* on absolute rates.** Both are lossless, bufferless,
per-segment-rate-capped tiered conduits, but our RF economy is ~2 orders of magnitude smaller
(Macerator draws ~10 RF/t; Stirling outputs 3–10 RF/t), so TE's 200–32,000 RF/t ducts would be wildly
oversized. **Copy the shape, not the numbers.**

**Cable ladder (still pending).** Cables remain Copper · Gold · Ender at 30/60/120 RF/t. The decided
retrofit to **Copper · Gold · Amethyst · Ender** at 30/60/120/240 is unshipped and is now tangled
with the question of whether cables also adopt frame-and-fill — see the PR 2 outline. Battery I/O is
the *direct per-face* limit and already exceeds cable throughput; through a cable network the cable
is always the smaller pipe, so battery rates were sized against machine demand, not "to saturate a
cable".

## Scope & non-goals

- **In (shipped):** five tiers, per-tier capacity/throughput, craft-then-fill on the upper three,
  in-world + item charge display, registry alias from the untiered Battery.
- **Open:** configurable I/O (per-block, then per-side).
- **Out:** wireless/cross-dim transfer (Tesseract — out of scope per the TE breakdown),
  redstone-controlled output modes (a later augment), in-world upgrade kits (see
  [`0106-machine-upgrades.md`](0106-machine-upgrades.md)).

## Done when

- [x] Five battery tiers place, store, and transfer energy with distinct capacity/throughput on both
      loaders.
- [x] Charge shows in-world and on the item for all tiers, scaled to each tier's own capacity.
- [x] A world saved before the line loads with its batteries intact at Copper's stats.
- [x] The upper three tiers exist only via a Transposer fill; the lower two are crafted whole.
- [x] Energy storage consumes no Machine Frame.
- [x] Each tier is built from its own namesake material — no tiered frame or core items.
- [ ] I/O rates are configurable in-game and clamp correctly (never exceed tier max), persisted
      across save/load and break/place.

## References

- Roadmap: [`../delivery-plan.md`](../delivery-plan.md) → Phase 1 → Battery (tiers); [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md) → "Energy Cells" row
- Code: `power/block/{BatteryTier,BatteryBlock,BatteryBlockItem}`, `power/block/entity/BatteryBlockEntity`, `core/lib/power/AbstractBatteryBlockEntity` (tier-ready constructor), registration + `ALIAS` in `LogisticsPower.java`; tier precedent `power/cable/{CableTier,CableBlock}`; data-component precedent `pipe/data/PipeDataComponents`
- Fill chain: `data/logistics/recipe/transposer/fill_<tier>_battery.json`, `automation/transposer/{TransposerRecipe,SignedFluidAmount}`
- TE reference (verified against `ThermalExpansion-1.12.2-5.5.7.1` / `ThermalDynamics-1.12.2-2.5.6.1` / `ThermalFoundation-1.12.2-2.6.7.1`): `ItemFrame.frameCell*`, `TransposerManager`, `TDCrafting.addTransposerFill`, `ItemUpgrade`
- Related: [`0105-alloy-smelter.md`](0105-alloy-smelter.md) (materials gate), [`0106-machine-upgrades.md`](0106-machine-upgrades.md) (upgrade kits; creates the demand for bigger storage)
