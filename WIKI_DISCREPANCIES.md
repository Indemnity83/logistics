# Wiki / Code Discrepancies

Confirmed mismatches between documented behavior (`logistics-docs`, `wiki/*.txt`) and actual code,
found while writing wiki-driven feature tests (see `TESTING.md` → "Wiki-Driven Feature Tests").
Each entry needs a deliberate decision: fix the code to match the documented intent, or fix the
wiki to match actual (intended) behavior.

All entries below are resolved and closed — kept as HTML comments so the same false positives and
fixes aren't rediscovered from scratch later. New findings go in a fresh, uncommented section above
the closed ones.

## Satellite Logistics Pipe

Open: **Names are numbers.** The wiki's Usage section says to "Set a descriptive name in its GUI
(e.g. "Iron Storage")". The satellite GUI is a numeric selector: `SatelliteScreenHandler` steps
through 1–512 (`MAX_ID`) with +/- buttons and skips numbers other satellites hold, and the Process
pipe's own selector addresses satellites by that number (`ProcessModule#getInputSatelliteId` is an
`int`). `SatelliteModule` would store a free-text string, but nothing a player can reach writes
one. Pinned in `SatelliteModuleTest#setSatelliteIdInt_positiveValue_persistsAndReadsBack` and
`CraftingProcessSatelliteGameTest#testProcessPipeSmeltsThroughSatelliteFedFurnace`.

Open: **Shared names do not load-balance.** The wiki says "Several satellites can share a name to
load-balance deliveries." `PipeNetwork#registerSatellite` holds one position per name and ignores a
second pipe claiming a taken name, so every delivery goes to whichever satellite registered first,
and the GUI will not even select a taken number. Confirmed via
`PipeNetworkSatelliteTest#secondSatelliteWithTakenNameIsIgnored`.

Open: **Only the Process pipe addresses satellites.** Usage says satellites let "other pipes" route
to them by name, and See also lists the Requester Logistics Pipe as one that "Routes items to
satellites by name". `RequesterModule` has no satellite setting; `ProcessModule` is the only caller
of `findSatellite`.

## Crafting Logistics Pipe

Open: **It needs an adjacent vanilla Crafter.** The wiki's Usage section describes configuring a
3×3 recipe and the pipe crafting on request, but never mentions that the pipe crafts *through* an
adjacent Crafter block (`CraftingModule#findAutocrafterDirection`). With no Crafter beside it the
pipe advertises nothing (`updateCrafterSupply` registers an empty supply) and rejects every
dispatch, so a player following the wiki gets a pipe that silently never crafts. A documentation
omission rather than a wrong number, like the Sawmill byproducts below. Both crafting tests in
`CraftingProcessSatelliteGameTestBody` place one.

## Process Logistics Pipe

Open: **"Watching machine state so it never overfills" is not backed by code.** The wiki's Usage
section says the pipe watches the machine "so it never overfills". `ProcessModule#onDispatch`
orders the inputs for the whole request up front (`executions × input count`) with no look at the
machine's free space; the only cap is the 64-entry queue (`MAX_QUEUE_SIZE`). The machine's own slot
limits still stop it holding more than a stack, but nothing in the pipe paces deliveries to it. Not
live-verified with an oversized request — a large order against a single-stack machine would show
what actually happens to the surplus.

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

<!--
Resolved (closed): Plant-pulping byproducts were undocumented. The wiki's Plants → Pulped Biomass
table listed no `Byproduct` column for any row, implying none of Oak Leaves, Sugar Cane or Wheat
dropped anything beyond the 1 Pulped Biomass. Only Oak Leaves was actually correct: wheat pulping
grants a 50% `minecraft:wheat_seeds` byproduct and sugar cane pulping a guaranteed 2x
`minecraft:sugar` (chance `2.0`). Unlike the Kiln/Pump findings this was a genuine missing fact, not
a mislabeled number — a player pulping wheat or sugar cane got a real bonus item the wiki never
mentioned. Confirmed via `SawmillRecipeSpotCheckTest`. Resolved as a documentation omission: the
wiki rows now carry `Byproduct=Sugar`/`ByproductChance=200%` and `Byproduct=Wheat Seeds`/`50%`.
Fixed in logistics-docs commit 579e74e91.
-->

## Sequential Fabricator

<!--
Resolved (closed): The queue only sounded like it held one selection at a time. The wiki's Usage
section used singular phrasing throughout ("choose *the* chipset", "produces *the selected*
chipset"), but `FabricatorProcessorComponent` maintains a `List<ResourceId> selected` — the GUI can
toggle any number of chipsets into the queue at once, and the machine cycles round-robin through
all selected-and-currently-craftable ones, building and ejecting each in turn. Confirmed live via
`SequentialFabricatorGameTest#testCyclesThroughMultipleSelectedChipsets`. Not wrong, just
incomplete: a player reading "the selected chipset" would never discover the
queue-multiple-and-let-it-cycle workflow, which is the more useful way to run the machine
unattended. Resolved as a documentation addition — the Usage section now describes queuing multiple
chipsets and the round-robin cycling. Fixed in logistics-docs commit 579e74e91.
-->
