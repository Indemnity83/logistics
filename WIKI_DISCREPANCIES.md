# Wiki / Code Discrepancies

Confirmed mismatches between documented behavior (`logistics-docs`, `wiki/*.txt`) and actual code,
found while writing wiki-driven feature tests (see `TESTING.md` → "Wiki-Driven Feature Tests").
Each entry needs a deliberate decision: fix the code to match the documented intent, or fix the
wiki to match actual (intended) behavior.

All entries below are resolved and closed — kept as HTML comments so the same false positives and
fixes aren't rediscovered from scratch later. New findings go in a fresh, uncommented section above
the closed ones.

## Kiln

<!--
Resolved (closed): RF cost per smelt + Smelt speed. Wiki corrected to match code — the Kiln runs a
200-tick vanilla recipe in 100 ticks (5s) for 2,000 RF total (`KILN_RF_PER_COOK_TICK`(10) × cook
time, spent at `KILN_ENERGY_PER_TICK`(20)/tick), i.e. twice furnace speed, not the same speed at
4,000 RF as previously documented. Fixed in logistics-docs commit 7ba3e7af.
-->

<!--
Resolved (closed): Input sides. Wiki corrected to "top only" (Usage + Setup), matching
`KilnBlockEntity`'s top-only input wiring (`.furnaceAccess(...)`). Documented as top-only until any
future input-filtering work changes the code side. Fixed in logistics-docs commit 7ba3e7af.
-->

<!--
Not a real discrepancy (checked and closed): the Kiln's recipe keys its center ingredient to
`logistics:core/machine_core`, while the wiki's Crafting template calls it "Machine Frame." This
first looked like a naming mismatch, but `logistics-docs/wiki/Machine Frame.txt`'s own History
section says the item was "Added as Machine Core" (v0.5.0) then "Renamed to Machine Frame" (v0.8.0)
— the registry ID (`machine_core`) is a stable identifier that outlived the display-name rename, by
design (changing a registry ID after release breaks existing worlds/recipes). The lang file confirms
`item.logistics.core.machine_core` displays in-game as "Machine Frame," matching the wiki exactly.
Recorded here so the same false positive isn't rediscovered later — see FluidPumpRecipeTest for the
same check on the Pump, which resolves the same way.
-->

<!--
Related fix, not from a wiki-driven feature test: `wiki/Macerator.txt` § Power claimed its 10 RF/t
draw was "more than the Kiln." That was backwards once the Kiln entry above was corrected to 20
RF/t — the Macerator draws less, not more. The comparison clause was dropped rather than restated
in the other direction (it wasn't load-bearing). Fixed in logistics-docs commit 2d62f834.
-->

## Pump

<!--
Resolved (closed): Output rate was mislabeled — the wiki's number is really the intake average.
Wiki reworded to describe 62.5 mB/t as the sustained average throughput (one 1,000 mB source block
drained every `FLUID_PUMP_INTERVAL_TICKS`(16) ticks), not an "output" bandwidth figure. The 400 mB/t
push constant (`FLUID_PUMP_PUSH_RATE_MB`) was left out of the wiki as an implementation detail,
since intake is ~6.4x slower and is always the practical bottleneck. The existing Golden Fluid Pipe
(80 mB/t) recommendation already matched the intake-limited ceiling and needed no change. Fixed in
logistics-docs commit 61b2243a.
-->

## Sawmill

### Plant-pulping byproducts are undocumented
- **Wiki says** (`wiki/Sawmill.txt` § Recipes → Plants → Pulped Biomass): no `Byproduct` column at
  all for any of the three listed rows (Oak Leaves, Sugar Cane, Wheat) — implies none of them drop
  anything beyond the 1 Pulped Biomass.
- **Code does**: only Oak Leaves genuinely has no byproduct (confirmed: `pulped_biomass_from_leaves.json`).
  `pulped_biomass_from_wheat.json` grants a 50% chance of a bonus `minecraft:wheat_seeds`, and
  `pulped_biomass_from_sugar_cane.json` grants a **guaranteed 2x `minecraft:sugar`** (chance `2.0`).
  (`pulped_biomass_from_wheat_seeds.json` is a separate Wheat Seeds → Pulped Biomass recipe, not one
  of the wiki's three listed rows, and also has no byproduct.) Confirmed via `SawmillRecipeSpotCheckTest`.
- **This is different from the Kiln/Pump findings above** — not a mislabeled number, a genuine
  missing fact. A player pulping wheat or sugar cane gets a real bonus item the wiki never mentions.
- **Decision needed**: add a `Byproduct`/`ByproductChance` column to the wiki's Wheat and Sugar Cane
  rows (Wheat: seeds, 50%; Sugar Cane: sugar, guaranteed 2x) — this looks like a documentation
  omission to fill in, not a code change.
