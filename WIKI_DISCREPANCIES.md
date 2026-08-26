# Wiki / Code Discrepancies

Confirmed mismatches between documented behavior (`logistics-docs`, `wiki/*.txt`) and actual code,
found while writing wiki-driven feature tests (see `TESTING.md` → "Wiki-Driven Feature Tests").
Each entry needs a deliberate decision: fix the code to match the documented intent, or fix the
wiki to match actual (intended) behavior. Not resolved here — logged for a follow-up triage pass.

## Kiln

### RF cost per smelt
- **Wiki says** (`wiki/Kiln.txt` § Power): "about 4,000 RF for a standard 10-second smelt"
- **Code does** (`SmeltingRecipeResolver.java`): 200-tick recipe × `KILN_RF_PER_COOK_TICK`(10) = 2,000 RF
- **Root cause**: `KILN_RF_PER_COOK_TICK` (10) and `KILN_ENERGY_PER_TICK` (20) are independently
  configured; if equal, the wiki's numbers would be exactly correct.
- **Decision needed**: retune `KILN_RF_PER_COOK_TICK` to 20 (matches wiki, doubles RF cost), or
  correct the wiki to "2,000 RF / 5 seconds — twice furnace speed."

### Smelt speed
- **Wiki says**: "runs recipes at the same speed as a furnace" (200 ticks / 10s)
- **Code does**: 100 ticks / 5s (half furnace time), a direct consequence of the RF-cost mismatch above
- **Decision needed**: same knob as above — resolved together.

### Input sides
- **Wiki says** (stated twice — Usage and Setup): "input is accepted from the top and sides"
- **Code does** (`KilnBlockEntity` class Javadoc + `.furnaceAccess(...)`): top only
- **Decision needed**: correct the wiki (two sentences) to "top only," or reconsider whether side
  input should be added to match documented behavior — likely a wiki fix given furnace-parity is
  the whole design intent.

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
