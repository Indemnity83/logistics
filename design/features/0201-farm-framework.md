# Farm Framework

> **Status:** 🚧 Planned — design settled, wave 0 (foundations) · **Phase:** 2 — Forestry · **Module:** `logistics-forestry` (`forestry` domain) + shared framework in `core`
> **Source:** [`../mods/forestry.md`](../mods/forestry.md) (Farm (single-block)) · **Depends on:** the machine framework (`core/machine/*`); a Provider-pipe fix (below) · **Maps to (roadmap):** Phase 2 — Farms

The shared base every Phase 2 farm is built on: one block per farm type, a fixed square plot in front of the block, a work loop that harvests, lays soil, irrigates, plants and fertilizes one plot at a time, role-routed item I/O, an input-only water tank, a Range Upgrade, and one shared farm screen. The individual farms ([0203](0203-arboretum.md)–[0209](0209-orchard.md)) only supply a `FarmLogic`.

## Problem & goal

Nothing farm-related exists in code yet: there are no farm, harvest, bonemeal or fake-player classes. The mod does ship the pieces around it: Apatite, Saltpeter, Bog Earth and Peat, Sawdust and Pulped Biomass, the biofuel fluids, and the valve line that was earmarked for farming machines.

**Goal:** a small, well-tested framework so each farm is just "which germlings, which soil, how to harvest", with no per-farm block entity, menu or screen.

## Requirements

### Functional
- **One block per farm type.** No farm-type selector. Every farm block shares one block entity type.
- **Work area:** the farm sits at the edge-midpoint of its plot, facing into it. The plot is **9×9** at the block's Y level, with soil at Y-1. One Range Upgrade makes it **13×13**, two make it **17×17**.
- **Chunk behaviour:** works only while its chunk is loaded. No chunk loading, and a scan never loads chunks (`level.isLoaded(pos)` before every read).
- **Work loop:** every N ticks the farm takes the next plot in a deterministic scan and performs at most one op, in priority order **HARVEST → SOIL/TILL → IRRIGATE → PLANT → FERTILIZE**. Each op costs RF; IRRIGATE also costs water (mB).
- **Output:** a 9-slot internal buffer. When it is full the farm stops harvesting (planting and soil work continue) and shows *Output full*. It never drops items in the world; it sweeps up loose drops in its own area. Random drops beyond the free space go to an NBT overflow list.
- **Redstone:** a redstone signal pauses the farm.
- **Status (Jade + GUI):** one of Working, No power, No water, No germlings, No soil, Output full, Paused (redstone), Idle (nothing to do), plus Too dark / Blocked instead of retrying forever (crops need light ≥ 8; farmland reverts under solid blocks).
- **No fake players.** Plant with `setBlock` after `canSurvive`; harvest with `Block.getDrops(…, ItemStack.EMPTY)` + `destroyBlock` (accept drops only when it returns true); grow through `PlantCompat`. The owner is recorded on placement behind a no-op `ProtectionCompat` seam, since claim mods see no break event without a fake player.
- **Pipes and hoppers:** inserting on any face routes each item by role (germling → germling slots, soil → soil slots, fertilizer → fertilizer slot); anything else is refused. Every face exposes only the output slots for extraction, so a Provider pipe offers only produce and a Supplier pipe keeps inputs stocked.
- **Water:** an input-only tank accepting `c:water`, filled by a Pump, fluid pipe, Glass Tank or Fluid Supplier pipe. Only farms that irrigate have one.
- **Power:** cables or engines like any machine. Not low-tier, so a Redstone engine can't power it.

### Balance
- Steady-state draw of about **10–20 RF/t**, in line with the Kiln and Sawmill. Every number (op cost, water per irrigate, tick interval) is a config key.
- Recipe: the standard machine recipe (Machine Frame + Redstone Reception Coil + copper gears) with themed items and **1–2 valves** per farm. Wave 1 is decided: Arboretum = copper, Crop Farm = tin. Proposed for later waves: Peat Bog = rubber, Mushroom = obsidian, Stalk = iron, Infernal = blazing, Orchard = emerald.

## Design sketch

### Shared-framework extensions (`core`, each a small PR with JUnit)
- **`SidedLayout` per-slot filters** (`core/machine/component/SidedLayout`): add an `anyFace(inputs, outputs, slotFilter)` factory and `ItemsBuilder.slotFilter(range, predicate)`. Apply the same filter in `canPlaceItem`, so menus and hoppers obey it. `ContainerItemStorage.insert` already walks slots through `canPlaceItemThroughFace`, so role routing then works with no further change.
- **`FluidBuilder.inputOnly(filter)`:** generalises `SteamEngineBlockEntity.SteamEngineFluidView`. Today `FluidStoreComponent` has no input-only or fluid-filter mode.
- **`core/lib/compat/PlantCompat`:** the only place version-specific plant APIs are touched, alongside `NbtCompat` / `RecipeCompat`:
  - `canFertilize(level, pos, state)` / `fertilize(level, random, pos, state)` — 26.x `BonemealableBlock` methods take an extra `BonemealSource`.
  - `harvestBerries(level, pos, state)` — 26.x reads harvest loot tables where 1.21.1 hard-codes counts.
  - Fungus growth — `NetherFungusBlock` with `Feature` keys on 26.x, `FungusBlock` with `ConfiguredFeature` keys on 1.21.1.
- **`core/machine/area`:** `WorkArea` (a square in front of a facing, enlarged by `range`) and `PlotScanner` (a resumable round-robin cursor that backs off after a pass with no work). Pure code; the quarry's `QuarryFrameRect` / `GridScanner` can adopt it later.
- **`UpgradeComponent` made real** (`core/machine/upgrade/UpgradeComponent`) + the **Range Upgrade** item — the first concrete slice of [0106](0106-machine-upgrades.md):
  - The upgrade slot is a new `UPGRADE` `SlotRole` in the machine's single `ItemStoreComponent`, not a separate Container. A separate one would be invisible to menus and lost on block break, because `MachineEntity` uses the first `SidedItems` it finds and drops read `itemStorage(null)`.
  - `modifiers()` returns one stable object that re-reads the slot on each call, because the quarry captures it once at construction (`LaserQuarryBlockEntity`). It is identity when no slot is bound, so the quarry is unchanged.
  - The menu slot accepts only upgrade items, at most 2 (one Range Upgrade item stacking to 2). When the upgrade changes: sync, and reset the plot cursor.
  - `capacity` / `receiveRate` modifiers stay build-time only.
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
├── FarmComponent.java        # the work-loop state machine; ops are FarmOperation OperationKeys
├── FarmLogic.java            # per-farm SPI: germlings, soil, plot classification, harvest
├── FarmOperation.java        # HARVEST / SOIL / IRRIGATE / PLANT / FERTILIZE
└── FarmStatus.java
```
- **Naming:** `FarmMachineBlock`, not `FarmBlock` (vanilla farmland on 1.21.1) and not `BushBlock`. Code never references the farmland class (`FarmBlock` on 1.21.1, `FarmlandBlock` on 26.x); use `Blocks.FARMLAND` + `BlockStateProperties.MOISTURE`, and write moisture with flag 2.
- **One shared block entity type** registered for every farm block (`registerBlockEntity(name, factory, Block...)`), so NeoForge needs one capability line per capability.
- **`FarmLogic` selection** in `configure()` from `getBlockState().getBlock()`. Fields are declared without initializers, as in `LaserQuarryBlockEntity`, because initializers run after `configure()`.
- **Components:** `EnergyStorageComponent`, `ItemStoreComponent` with `SidedLayout.anyFace(...)` role filters, `FluidStoreComponent` with `inputOnly(c:water)` when the logic irrigates, the real `UpgradeComponent`.
- **World-touching component:** model `FarmComponent` on the Fluid Pump's `automation/pump/FluidPumpComponent`. Drop collection re-implements `QuarryBlockBreaker`'s `getDrops` + `destroyBlock` pattern and `QuarryOutput.collectLooseItems` in core, since domains can't import `automation`.
- **Status / Jade:** expose status through `MachineHudModel`; register the abstract `FarmMachineBlock` once in both loaders' Jade plugins and the Fabric client plugin (subclasses match), backed by `compat/jade/MachineServerDataProvider`.
- **Test hook:** `FarmBlockEntity.setAreaSizeOverride(int)`, a per-instance override following the `FluidPumpBlockEntity.setArmSpeedOverride` precedent, so GameTests fit a 3×3 plot into the standard empty structure.

### Shared farm screen
One menu + screen for every farm, configured by farm type (approved layout):
- **176×166**, ghost icons instead of text labels.
- **Left:** energy bar, and a water gauge only for farms that irrigate.
- **Middle:** a row of 4 germling slots; a row of 4 soil slots (hidden if the farm lays no soil); then the fertilizer slot + charge bar + status lamp + upgrade slot.
- **Right:** a 3×3 output grid.
- **Status lamp** tooltip reads like "Working · 9×9 plot".

## Scope & non-goals

- **In:** the framework extensions above, the Provider-pipe fix, the domain scaffold, the farm block/entity/component/logic SPI, the Range Upgrade, the shared screen, Jade, NeoForge capabilities.
- **Out:** genetics ([RFC 0002](../rfcs/0002-forestry-bees.md)), multiblocks and the Multifarm, chunk loading, fake players, circuit-board programmability (redstone enable/disable only — [RFC 0001](../rfcs/0001-programmable-behavior.md) option B), speed and efficiency upgrades ([0106](0106-machine-upgrades.md)), an area-preview renderer, farm-specific logistics modules, the Squeezer/Fermenter/Still/Bottler chain and peat/biofuel engines.

## Open questions

- **When wave 1 starts.** [`delivery-plan.md`](../delivery-plan.md) has farms wait for 1.0, and fluid request is still open. The maintainer decides.
- **mc/1.21.1 port prerequisite:** that branch lacks `MachineHudModel` / `HudContributor`. Backport the HUD layer before the farm chunks are ported there.
- Exact RF per op, water per irrigate, and the scan interval — tune in playtest within the 10–20 RF/t anchor.

## Done when

- A farm block with a trivial test `FarmLogic` works its plot on both loaders: no power → no work, Output full → stall, redstone → pause, Range Upgrade → wider area.
- A hopper inserts germlings, soil and fertilizer into the right slots, and a hopper / Provider pipe can pull only output.
- Water arrives from a Glass Tank or fluid pipe into the input-only tank.
- JUnit covers `WorkArea` geometry for 9 / 13 / 17 and every facing, `PlotScanner` resume and back-off, the `SidedLayout` slot filters, `FluidBuilder.inputOnly`, and the `UpgradeComponent` identity case (quarry unchanged).
- GameTests (`common/src/gametest/.../forestry/`, both loaders, `checkFeatureTestParity`) use real connectivity — Creative Engine power, hopper input, hopper-to-chest output — and `setAreaSizeOverride(3)` instead of new `.nbt` structures.

## References

- Breakdown: [`../mods/forestry.md`](../mods/forestry.md) → Farms
- Roadmap: [`../delivery-plan.md`](../delivery-plan.md) → Phase 2 → Farms; [`ROADMAP.md`](../../ROADMAP.md) → Next
- Code: `core/machine/MachineEntity`, `MachineBuilder`, `core/machine/component/{SidedLayout,ItemStoreComponent,FluidStoreComponent,EnergyStorageComponent,SlotRole}`, `core/machine/upgrade/{UpgradeComponent,MachineModifiers,OperationKey}`, `core/machine/MachineHudModel`, `automation/pump/FluidPumpComponent`, `automation/laserquarry/entity/{QuarryBlockBreaker,QuarryOutput,QuarryFrameRect,GridScanner}`, `pipe/modules/ProviderModule`, `core/lib/storage/ContainerItemStorage`
- Related: [0106 Machine Upgrades](0106-machine-upgrades.md), [0202 Fertilizer & Soils](0202-fertilizer-and-soils.md)
