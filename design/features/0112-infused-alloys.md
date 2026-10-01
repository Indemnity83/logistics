# Infused Alloys (Caterium · Lumenite · Echonite)

> **Status:** ✅ Shipped · **Phase:** 1 — Automation core · **Module:** `logistics-core` (materials)
> **Source:** [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md) ("High-tier alloys") · **Depends on:** the Transposer (v0.8.6) and the Crucible's charged fluids
> **Maps to (roadmap):** Phase 1 — cable tiers; Phase 1 — Battery tiers

Three metals made by quenching a conductor in a charged fluid in the **Transposer**:

| Alloy | Conductor | Fluid | Energy | Tier it bodies |
|---|---|---|---|---|
| **Caterium** | Gold Ingot | 250 mB liquid redstone | 2,000 RF | Conductive |
| **Lumenite** | Amethyst Dust | 500 mB liquid glowstone | 3,000 RF | Resonant |
| **Echonite** | Echo Dust | 750 mB liquid ender | 4,000 RF | Deep |

They are the **body material of the upper energy tiers** — the thing a cable or a battery at that
rank is built from — rather than a tier of their own. The rank is the fluid's, not the metal's.

## Why they exist

The energy ladder's upper tiers needed a body material, and neither obvious option worked:

- **Raw dust in the recipe** (`2 ender dust + 1 gold ingot`, as the old Ender Cable did) reads wrong:
  dust is not a conductor, and "sprinkle powder on gold" says nothing about why the tier is better.
- **Craft-an-empty-block-and-fill-it**, which the battery line used, is two steps that produce one
  single-purpose item. An "Empty Gold Battery" can only ever become a battery.

An alloy fixes both. It is a conductor, so a cable made of it makes sense; and it is reusable, so the
same ingot bodies the cable, the battery, and whatever later machine wants that rank. The item count
comes out level — three alloys replace three `<tier>_battery_empty` items — while the *concept* count
drops from two ("fill a block", "dust in a recipe") to one.

Naming them as invented metals also resolves a presentation problem: amethyst and echo are not
metals, so "Amethyst Ingot" would be nonsense — but *Lumenite* is a metal regardless of what went
into it. This is exactly how Thermal's Lumium works (tin + silver + glowstone → a metal).

## What shipped

- Three materials in `logistics-core`, each in the full **ingot / nugget / storage block** set that
  Bronze and Tin already ship, with `#c:ingots/<name>`, `#c:nuggets/<name>` and
  `#c:storage_blocks/<name>` tags and the usual 9↔1 compression recipes both ways.
- Three Transposer recipes. **No new machine and no new code** — `TransposerRecipe` already accepted
  arbitrary `item + fluid → item`, and a negative `SignedFluidAmount` is the Fill direction.
- All three fluids were already Crucible-obtainable, so nothing new had to be made survival-reachable.

**Not the Alloy Smelter**, despite the name. That machine takes two *solid* ingredients
(`ingredients: [...]`) and has no fluid input; the fluid is the entire point here. The Transposer is
the mod's `item + fluid → item` bridge and already does this exact shape.

## Art

One palette per alloy, applied to three shared silhouettes so every form reads as the same material:

| Alloy | Donor set | Treatment |
|---|---|---|
| Caterium | `*_fiery` (block, ingot, nugget) | **used verbatim** — already a molten, charged metal |
| Lumenite | `*_aurichalcum` (`block_2`, ingot, nugget) | **hue rotated +218°**, gold → amethyst |
| Echonite | `*_abyssium` (block, nugget) + vanilla `iron_ingot` | recoloured onto a sculk palette |

Each alloy takes a whole donor set, so its three forms share one artist's hand. Where a donor's own
colour already worked it is left alone; Lumenite needed only a **pure hue rotation** — saturation
and value untouched, which is why the donor's shading and specular survive intact. Only Echonite
needed a built palette, because nothing in the pack carried sculk's near-black blue-green.

Echonite's ingot is the one piece still on the vanilla ingot silhouette rather than its donor's,
since the abyssium ingot's shape did not read as cleanly at inventory scale.

Each palette is eight steps from a near-black shadow to a single bright **glint**. The ingot mapping
is deliberate rather than mechanical: the vanilla base is top-heavy — levels 5–7 are 53% of its
pixels — so a linear map washes it out, and the bright end is held back so only the lit edge fires.
The block donors all carry a real dark-to-light spread of their own, so they take a plain rank map.

## Balance

First-pass numbers, all pure data. The fluid cost escalates 250 → 500 → 750 mB and the energy 2,000
→ 4,000 RF. Against the Crucible's yields that is 2½ redstone, 2 glowstone dust, and 3 ender pearls
per ingot respectively. A cable tier consumes 3 ingots for 8 cables.

Worth watching: the old Ender Cable cost 2 ender dust for 8 cables, and the Echo Cable now costs
9 pearls' worth. That is a deliberate increase for a tier that also doubled in throughput, but it is
the number most likely to need tuning once the top of the ladder sees real play.

## Scope & non-goals

- **In:** three ingots, three Transposer recipes, `c:` tags, and their use as cable body material.
- **Out:** any fourth alloy. The set is closed at three because the energy ladder has three tiers
  above Copper. A new alloy needs a new rank to body, not just a new fluid.

## Known loose end — `ender_dust`

`core/ender_dust` existed solely to feed the old Ender Cable, and nothing consumes it now. The
options are to leave it (a Macerator recipe producing a dead item), remove it (player-visible, and
it may sit in chests), or find it a home. **Left in place for now, deliberately, as the smallest
irreversible choice.** `echo_dust` had the same problem before this feature and the alloys fixed it —
`ender_dust` is waiting for the same kind of answer.

An earlier plan gave Echonite a second recipe from `ender_dust`, mirroring Bronze's four
dust/ingot variants. That was dropped: Bronze's variants are all genuinely bronze, whereas
"Echonite from ender dust, with no echo in it" is thematically wrong just to keep an item alive.

## Done when

- [x] Three alloys craft in the Transposer from their own conductor and fluid.
- [x] Each is the body material of its cable tier, with one shared recipe silhouette.
- [x] A spot-check test pins each alloy to its conductor, fluid, volume and energy.
- [ ] The battery line is rebuilt on them, retiring the `<tier>_battery_empty` items.
- [x] Each alloy ships ingot, nugget and storage block with compression recipes both ways.
- [ ] `ender_dust` has a purpose or a removal plan.

## References

- Ladders: [`../progression-tiers.md`](../progression-tiers.md) → "Coverage" and "Retrofit plan"
- Breakdown: [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md) → "High-tier alloys"
- Consumers: `data/logistics/recipe/power/{amethyst,echo}_cable.json`
- Code: registration in `LogisticsCore` (`ITEM`), recipes in `data/logistics/recipe/transposer/`
- Related: [`0105-alloy-smelter.md`](0105-alloy-smelter.md) (Bronze; why this is not that machine),
  [`0107-tiered-batteries.md`](0107-tiered-batteries.md) (the fill step these replace)
