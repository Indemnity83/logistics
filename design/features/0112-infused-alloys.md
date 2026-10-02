# Infused Alloys (Caterium · Lumenite · Echonite)

> **Status:** 🚧 In progress — Caterium and Lumenite shipped · **Phase:** 1 — Automation core · **Module:** `logistics-core` (materials)
> **Source:** [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md) ("High-tier alloys") · **Depends on:** the Transposer (v0.8.6) and the Crucible's charged fluids
> **Maps to (roadmap):** Phase 1 — cable tiers; Phase 1 — Battery tiers

Three metals, each **one dust quenched in one bucket of a charged fluid** in the **Transposer**:

| Alloy | Dust | Fluid | Energy | Tier it bodies | Status |
|---|---|---|---|---|---|
| **Caterium** | Gold Dust | 1,000 mB liquid redstone | 2,000 RF | Conductive (rank 4) | ✅ shipped |
| **Lumenite** | Amethyst Dust | 1,000 mB liquid glowstone | 3,000 RF | Resonant (rank 6) | ✅ shipped |
| **Echonite** | Echo Dust | 1,000 mB liquid ender | 4,000 RF | Deep (rank 9) | 📋 next |

**One rule, three materials: macerate, then infuse.** Holding the dust and the volume constant means
the only thing that varies across the line is *which* fluid — which is exactly the thing that
identifies the tier. Energy is the one escalating cost.

They are the **body material of the upper energy tiers** — what a cable or a battery at that rank is
built from — rather than a tier of their own. The rank is the fluid's, not the metal's.

## Why they exist

The energy ladder's upper tiers needed a body material, and neither obvious option worked:

- **Raw dust in the recipe** (`2 ender dust + 1 gold ingot`, as the old Ender Cable did) reads wrong:
  dust is not a conductor, and "sprinkle powder on gold" says nothing about why the tier is better.
  Dust as *feedstock* is fine — it is dust as the finished conductor that does not work.
- **Craft-an-empty-block-and-fill-it**, which an earlier revision of the battery line used, is two
  steps that produce one single-purpose item. An "Empty Gold Battery" can only ever become a battery.

An alloy fixes both. It is a metal, so a cable made of it makes sense; and it is reusable, so the
same ingot bodies the cable, the battery, and whatever later machine wants that rank. The item count
comes out level — three alloys replace three `<tier>_battery_empty` items — while the *concept* count
drops from two ("fill a block", "dust in a recipe") to one.

Naming them as invented metals also resolves a presentation problem: amethyst and echo are not
metals, so "Amethyst Ingot" would be nonsense — but *Lumenite* is a metal regardless of what went
into it. This is exactly how Thermal's Lumium works (tin + silver + glowstone → a metal).

## What each alloy ships

The full **ingot / nugget / storage block** set that Bronze and Tin already have, with
`#c:ingots/<name>`, `#c:nuggets/<name>` and `#c:storage_blocks/<name>` tags and the usual 9↔1
compression recipes both ways. The battery line needs the block *and* the nugget — its recipe is clay
around a block of the tier material with nuggets as terminals — so neither form is decorative.

**No new machine and no new code.** `TransposerRecipe` already accepted arbitrary `item + fluid →
item`, and a negative `SignedFluidAmount` is the Fill direction. All three fluids were already
Crucible-obtainable, so nothing new had to be made survival-reachable.

**Not the Alloy Smelter**, despite the name. That machine takes two *solid* ingredients
(`ingredients: [...]`) and has no fluid input; the fluid is the entire point here. The Transposer is
the mod's `item + fluid → item` bridge and already does this exact shape.

## Art

Each alloy takes a whole donor set from the unused-texture pack, so its three forms share one
artist's hand:

| Alloy | Donor set | Treatment |
|---|---|---|
| Caterium | `*_fiery` (block, ingot, nugget) | **used verbatim** — already a molten, charged metal |
| Lumenite | `*_aurichalcum` (`block_2`, ingot, nugget) | **hue rotated +218°**, gold → amethyst |
| Echonite | `*_abyssium` (block, nugget) + vanilla `iron_ingot` | recoloured onto a sculk palette |

Where a donor's colour already worked it is left alone. Lumenite needs only a **pure hue rotation** —
saturation and value untouched, which is why the donor's shading and specular survive intact. Only
Echonite needs a built palette, because nothing in the pack carries sculk's near-black blue-green.

## Balance

First-pass numbers, all pure data. Every alloy is 1 dust + 1,000 mB; only the energy escalates,
2,000 → 3,000 → 4,000 RF. Against the Crucible's yields, one bucket costs:

| Alloy | Fluid cost per ingot |
|---|---|
| Caterium | 10 redstone |
| Lumenite | 4 glowstone dust (or 1 glowstone block) |
| Echonite | 4 ender pearls |

A cable tier consumes 3 ingots for 8 cables, so a Deep Cable run is 3 echo dust + 12 ender pearls.

**That is the number most likely to need tuning.** The old Ender Cable cost 2 ender dust for the same
8 cables, so the top tier is substantially dearer — deliberate for a tier that also doubled in
throughput and sits a rank higher, but it has not met real play yet. The flat 1,000 mB is chosen for
legibility over fine-grained balance; if the top tier bites, raise the ingot yield rather than
breaking the one-bucket rule, since the rule is what makes the line readable.

## Scope & non-goals

- **In:** three alloys in ingot/nugget/block form, their Transposer and compression recipes, `c:`
  tags, and their use as the body material of the cable and battery ladders.
- **Out:** any fourth alloy. The set is closed at three because the energy ladder has three tiers
  above Basic. A new alloy needs a new rank to body, not just a new fluid.
- **Out:** Signalum/Lumium/Enderium by name — same idea, but importing another mod's vocabulary
  conflicts with the material-identity principle in [`../progression-tiers.md`](../progression-tiers.md).

## Known loose end — `ender_dust`

`core/ender_dust` exists solely to feed the old Ender Cable. Once that cable is rebuilt on Echonite
nothing will consume it. The options are to leave it (a Macerator recipe producing a dead item),
remove it (player-visible, and it may sit in chests), or find it a home. **No decision yet** — it is
flagged here so it is not discovered by accident. `echo_dust` had the same problem before this
feature, and the alloys fixed it.

An earlier plan gave Echonite a second recipe from `ender_dust`, mirroring Bronze's four dust/ingot
variants. That was dropped: Bronze's variants are all genuinely bronze, whereas "Echonite from ender
dust, with no echo in it" is thematically wrong just to keep an item alive.

## Done when

- [x] Caterium crafts in the Transposer from gold dust and liquid redstone.
- [x] It ships ingot, nugget and storage block with compression recipes both ways.
- [x] Spot-check tests pin the infusion and the 9↔1 round trips.
- [x] Lumenite ships on the same rule.
- [ ] Echonite ships on the same rule.
- [ ] The cable and battery ladders are built from them.
- [ ] `ender_dust` has a purpose or a removal plan.

## References

- Ladders: [`../progression-tiers.md`](../progression-tiers.md) → "Coverage" and "Retrofit plan"
- Breakdown: [`../mods/thermal-expansion.md`](../mods/thermal-expansion.md) → "High-tier alloys"
- Code: registration in `LogisticsCore` (`ITEM`, `BLOCK`), recipes in
  `data/logistics/recipe/transposer/` and `data/logistics/recipe/core/`
- Precedent for the full material set: Bronze (`bronze_ingot` / `bronze_nugget` / `bronze_block`)
- Related: [`0105-alloy-smelter.md`](0105-alloy-smelter.md) (Bronze; why this is not that machine)
