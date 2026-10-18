# Farm Framework

> **Status:** ✅ **Shipped** (#1406; farm screen #1407) — shared farm block, work loop, role-routed I/O, water tank and farm screen; ring farms (#1414–#1418) · **Phase:** 2 — Forestry · **Module:** `logistics-forestry` (`forestry` domain) + shared framework in `core`
> **Source:** [`../mods/forestry.md`](../mods/forestry.md) (Farm (single-block)) · **Depends on:** the machine framework (`core/machine/*`); a Provider-pipe fix (below) · **Maps to (roadmap):** Phase 2 — Farms

The shared base every Phase 2 farm is built on: one block per farm type, sitting at the centre of a free 9×9 and working a ring of plots around it, a work loop that clears, harvests, lays soil, tills and plants one plot at a time, role-routed item I/O, an input-only water tank, and one shared farm screen. The individual farms ([0203](0203-arboretum.md)–[0209](0209-orchard.md)) only supply a `FarmLogic`.

## Problem & goal

Nothing farm-related exists in code yet: there are no farm, harvest, bonemeal or fake-player classes. The mod does ship the pieces around it: Apatite, Saltpeter, Bog Earth and Peat, Sawdust and Pulped Biomass, the biofuel fluids, and the valve line that was earmarked for farming machines.

**Goal:** a small, well-tested framework so each farm is just "which germlings, which soil, how to harvest", with no per-farm block entity, menu or screen.

## Requirements

### Functional
- **One block per farm type.** No farm-type selector. Every farm block shares one block entity type.
- **Work area (centred ring):** the farm block sits at the centre. The inner **9×9**, farm included, is never touched: it is where the player puts pipes, engines and chests. The one exception: the Arboretum breaks the natural leaves of a tree it fells that hang into it, and nothing else. The farm works the **ring** between that square and an outer square: **15×15** for the Crop Farm (a 3-wide band, 144 plots), **13×13** for the Arboretum (a 2-wide band, 88 plots). The soil layer is level with the farm, at its own Y, and plots (the crops and saplings) are one block above it at Y+1, so a farm placed on the ground raises its field one block. Each `FarmLogic` supplies `ringInner()`/`ringOuter()`. Work-area upgrades will come with the machine-wide upgrade design ([0106](0106-machine-upgrades.md)).
- **Clearing:** before the logic works a plot, the farm clears the plot's column, top-down, one block per op at `rf_clear_per_block` RF, from the plot level (Y+1): up to Y+5 for the Crop Farm, Y+10 for the Arboretum (`FarmLogic.clearanceHeight()`). Drops go to the output; fluids, source or flowing, are drained. The farm's own crops and trees are harvested, not cleared. **Block entities and unbreakable blocks (hardness < 0) are never broken**: the plot reports *Blocked* instead, so chests never spill. The soil layer at Y is replaced with the farm's own soil unless it is already prepared (dirt, farmland or tillable ground such as grass for the Crop Farm, which hoes it in place; Humus for the Arboretum): anything else there is broken into the output first, and air is simply filled.
- **Chunk behaviour:** works only while its chunk is loaded. No chunk loading, and a scan never loads chunks (`level.isLoaded(pos)` before every read).
- **Work loop:** every N ticks the farm takes the next plot in a deterministic scan and performs at most one op: while the plot's column needs clearing, one CLEAR; otherwise the one op the logic picks (HARVEST, SOIL, TILL or PLANT). Each op costs RF only; no op needs water.
- **Irrigation:** a working farm with a tank keeps its ring's farmland wet through vanilla's own moisture check. Vanilla farmland asks, on each of its random ticks, whether water is near: wet, it goes to or stays at moisture 7; dry, it loses a level, and empty farmland at 0 turns back to dirt. Farmland on a ring plot's soil cell that finds no real water asks the farm instead, which answers wet for `water_per_irrigation_mb` (100) × climate factor from its tank. Rain and real water nearby wet it for free, a tank short of the full charge answers dry, and a surveying or paused farm never irrigates. The check is routed through `core.lib.farmland.FarmlandHydration`: a mixin on `FarmlandBlock.isNearWater` on Fabric, a `FarmlandWaterManager` ticket on NeoForge.
- **Output:** a 9-slot internal buffer. When it is full the farm stops harvesting (planting and soil work continue) and shows *Output full*. It never drops or voids items in the world; it sweeps up loose drops in its own ring. Random drops beyond the free space go to an NBT overflow list.
- **Redstone:** a redstone signal pauses the farm.
- **Status (Jade + GUI):** one of Working, No power, No germlings, No soil, Output full, Paused (redstone), Idle (nothing to do), plus Too dark / Blocked instead of retrying forever (crops need light ≥ 8; a protected block in a plot's column).
- **No fake players.** Plant with `setBlock` after `canSurvive`; harvest with `Block.getDrops(…, ItemStack.EMPTY)` + `destroyBlock` (accept drops only when it returns true); grow through `PlantCompat`. The owner is recorded on placement behind a no-op `ProtectionCompat` seam, since claim mods see no break event without a fake player.
- **Pipes and hoppers:** inserting on any face routes each item by role (germling → germling slots 0–3, soil → soil slots 4–7); once its group is full, or for anything else, it is refused. Output is slots 8–16. Every face exposes only the output slots for extraction, so a Provider pipe offers only produce and a Supplier pipe keeps inputs stocked.
- **Water:** an input-only tank accepting `c:water`, filled by a Pump, fluid pipe, Glass Tank or Fluid Supplier pipe. Only farms that irrigate have one (the Crop Farm). The farm works fully without water: its crops only grow at the dry rate.
- **Power:** cables or engines like any machine. Not low-tier, so a Redstone engine can't power it.

### Balance
- Steady-state draw of about **10–20 RF/t**, in line with the Kiln and Sawmill. Every number (op cost, irrigation water, tick interval) is a config key in `forestry/farm.json`: `rf_clear_per_block` (40), `water_per_irrigation_mb` (100), and the other op costs.
- Recipe: the standard machine recipe (Machine Frame + Redstone Reception Coil + copper gears) with themed items and **1–2 valves** per farm. Wave 1 is decided: Arboretum = copper, Crop Farm = tin. Proposed for later waves: Peat Bog = rubber, Mushroom = obsidian, Stalk = iron, Infernal = blazing, Orchard = emerald.

## Design sketch

### Shared-framework extensions (`core`, each a small PR with JUnit)
- **`SidedLayout` per-slot filters** (`core/machine/component/SidedLayout`): add an `anyFace(inputs, outputs, slotFilter)` factory and `ItemsBuilder.slotFilter(range, predicate)`. Apply the same filter in `canPlaceItem`, so menus and hoppers obey it. `ContainerItemStorage.insert` already walks slots through `canPlaceItemThroughFace`, so role routing then works with no further change.
- **`FluidBuilder.inputOnly(filter)`:** generalises `SteamEngineBlockEntity.SteamEngineFluidView`. Today `FluidStoreComponent` has no input-only or fluid-filter mode.
- **`core/lib/compat/PlantCompat`:** the only place version-specific plant APIs are touched, alongside `NbtCompat` / `RecipeCompat`:
  - `canFertilize(level, pos, state)` / `fertilize(level, random, pos, state)` — 26.x `BonemealableBlock` methods take an extra `BonemealSource`.
  - `harvestBerries(level, pos, state)` — 26.x reads harvest loot tables where 1.21.1 hard-codes counts.
  - Fungus growth — `NetherFungusBlock` with `Feature` keys on 26.x, `FungusBlock` with `ConfiguredFeature` keys on 1.21.1.
- **`core/machine/area`:** a `PlotArea` interface implemented by `WorkArea` (a square) and `RingArea(origin, inner, outer)` (the band between two odd squares, exposing each plot's `lane` and `step` for planting patterns), plus `PlotScanner` (a resumable round-robin cursor that backs off after a pass with no work). Pure code; the quarry's `QuarryFrameRect` / `GridScanner` can adopt it later.
- Add `'forestry'` to `domains` in the root `build.gradle` (`checkImportBoundaries`).

### Prerequisite fix — Provider pipes advertise only extractable slots
A real `fix(routing)` PR, worth landing without farms: `ProviderModule.scanStorage` advertises a slot only if a simulated 1-item extract succeeds. Today a Provider pipe on any machine offers input slots it can never deliver, because all three `contents()` implementations list every slot on the face.

### Domain scaffold
- `com.logistics.forestry` / `LogisticsForestry` (`DomainBootstrap`, listed in `META-INF/services`); IDs `logistics:forestry/<name>`. Bog Earth and Peat stay in `core`.
- Client: `LogisticsForestryClient` + its Fabric services entry; NeoForge wiring in `NeoForgeClientSetup`.

### `forestry/farm`
```text
common/src/main/java/com/logistics/forestry/farm/
├── FarmMachineBlock.java     # abstract; extends core/lib/block/MachineBlock; FACING + LIT
├── FarmBlockEntity.java      # one shared type for every farm block; MachineEntity + MachineBuilder
├── FarmComponent.java        # the work loop: clears, then performs the logic's PlotAction
├── ColumnClearer.java        # pure: next block to clear from a plot's column
├── FarmLogic.java            # per-farm SPI: ring sizes, clearance, germlings, soil, plot actions
├── PlotAction.java           # Harvest / PlaceSoil / Till / Plant / None / Blocked
└── FarmStatus.java
```
- **Naming:** `FarmMachineBlock`, not `FarmBlock` (vanilla farmland on 1.21.1) and not `BushBlock`. Code never references the farmland class (`FarmBlock` on 1.21.1, `FarmlandBlock` on 26.x); use `Blocks.FARMLAND` + `BlockStateProperties.MOISTURE`, and write moisture with flag 2.
- **One shared block entity type** registered for every farm block (`registerBlockEntity(name, factory, Block...)`), so NeoForge needs one capability line per capability.
- **`FarmLogic` selection** in `configure()` from `getBlockState().getBlock()`. Fields are declared without initializers, as in `LaserQuarryBlockEntity`, because initializers run after `configure()`.
- **Components:** `EnergyStorageComponent`, `ItemStoreComponent` with `SidedLayout.anyFace(...)` role filters, `FluidStoreComponent` with `inputOnly(c:water)` when the logic tills with water.
- **World-touching component:** model `FarmComponent` on the Fluid Pump's `automation/pump/FluidPumpComponent`. Drop collection re-implements `QuarryBlockBreaker`'s `getDrops` + `destroyBlock` pattern and `QuarryOutput.collectLooseItems` in core, since domains can't import `automation`.
- **Status / Jade:** expose status through `MachineHudModel`; register the abstract `FarmMachineBlock` once in both loaders' Jade plugins and the Fabric client plugin (subclasses match), backed by `compat/jade/MachineServerDataProvider`.
- **Test hooks:** `setAreaOverride(inner, outer)` and `setClearanceHeightOverride(h)`, per-instance overrides following the `FluidPumpBlockEntity.setArmSpeedOverride` precedent. GameTests use a (1, 5) ring so the farm fits the standard 8×8×8 empty structure.

### Shared farm screen
One menu + screen for every farm, configured by farm type (approved layout):
- **176×166**, ghost icons instead of text labels.
- **Left:** energy bar, and a water gauge only on the Crop Farm.
- **Middle:** a row of 4 germling slots (0–3); a row of 4 soil slots (4–7); then the status lamp.
- **Right:** a 3×3 output grid (8–16).
- **Status lamp** tooltip shows the ring size, e.g. "Working · 15×15".

## Scope & non-goals

- **In:** the framework extensions above, the Provider-pipe fix, the domain scaffold, the farm block/entity/component/logic SPI, the shared screen, Jade, NeoForge capabilities.
- **Out:** genetics ([RFC 0002](../rfcs/0002-forestry-bees.md)), multiblocks and the Multifarm, chunk loading, fake players, circuit-board programmability (redstone enable/disable only — [RFC 0001](../rfcs/0001-programmable-behavior.md) option B), machine upgrades of any kind, including range, speed and efficiency ([0106](0106-machine-upgrades.md)), an area-preview renderer, farm-specific logistics modules, the Squeezer/Fermenter/Still/Bottler chain and peat/biofuel engines.

## Open questions

- **When wave 1 starts.** [`delivery-plan.md`](../delivery-plan.md) has farms wait for 1.0, and fluid request is still open. The maintainer decides.
- **mc/1.21.1 port prerequisite:** that branch lacks `MachineHudModel` / `HudContributor`. Backport the HUD layer before the farm chunks are ported there.
- Exact RF per op, water per till, and the scan interval — tune in playtest within the 10–20 RF/t anchor.

## Done when

- A farm block with a trivial test `FarmLogic` works its plot on both loaders: no power → no work, Output full → stall, redstone → pause.
- A hopper inserts germlings and soil into the right slots, and a hopper / Provider pipe can pull only output.
- Water arrives from a Glass Tank or fluid pipe into the input-only tank.
- JUnit covers `WorkArea` and `RingArea` geometry (counts, `indexOf` round-trip, corners, the excluded inner square), `ColumnClearer`, `PlotScanner` resume and back-off, the `SidedLayout` slot filters, and `FluidBuilder.inputOnly`.
- GameTests (`common/src/gametest/.../forestry/`, both loaders, `checkFeatureTestParity`) use real connectivity — Creative Engine power, hopper input, hopper-to-chest output — and `setAreaOverride(1, 5)` instead of new `.nbt` structures.

## References

- Breakdown: [`../mods/forestry.md`](../mods/forestry.md) → Farms
- Roadmap: [`../delivery-plan.md`](../delivery-plan.md) → Phase 2 → Farms; [`ROADMAP.md`](../../ROADMAP.md) → Next
- Code: `core/machine/MachineEntity`, `MachineBuilder`, `core/machine/component/{SidedLayout,ItemStoreComponent,FluidStoreComponent,EnergyStorageComponent,SlotRole}`, `core/machine/upgrade/OperationKey`, `core/machine/MachineHudModel`, `automation/pump/FluidPumpComponent`, `automation/laserquarry/entity/{QuarryBlockBreaker,QuarryOutput,QuarryFrameRect,GridScanner}`, `pipe/modules/ProviderModule`, `core/lib/storage/ContainerItemStorage`
- Related: [0106 Machine Upgrades](0106-machine-upgrades.md), [0202 Fertilizer & Soils](0202-fertilizer-and-soils.md)
