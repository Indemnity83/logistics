# Farm Framework

> **Status:** ✅ **Shipped** (#1406; farm screen #1407) — shared farm block, work loop, role-routed I/O, water tank and farm screen; ring farms (#1414–#1418) · 🚧 **Planned:** tiered ring farms — 7×7 courtyard, tiers and kits, budgeted scheduler, survey + Start · **Phase:** 2 — Forestry · **Module:** `logistics-forestry` (`forestry` domain) + shared framework in `core`
> **Source:** [`../mods/forestry.md`](../mods/forestry.md) (Farm (single-block)) · **Depends on:** the machine framework (`core/machine/*`); [0111 Machine Frame Tiers](0111-machine-frame-tiers.md); a Provider-pipe fix (below) · **Maps to (roadmap):** Phase 2 — Farms

The shared base every Phase 2 farm is built on: one block per farm type, sitting at the centre of a protected **7×7 courtyard** and working a ring of plots around it whose width grows with the machine's [tier](0111-machine-frame-tiers.md). A budgeted scheduler clears, harvests, lays soil, tills and plants; role-routed item I/O, an input-only water tank and one shared farm screen complete it. A placed farm only surveys until the player presses **Start**. The individual farms ([0203](0203-arboretum.md)–[0213](0213-desert-farm.md)) only supply a `FarmLogic`.

## Problem & goal

The framework, the Crop Farm and the Arboretum have shipped (unreleased). The tiered rework makes farms grow with the player instead of being fixed-size blocks, bounds their per-tick work, and makes activation safe: nothing in the world changes until the player has seen what will be cleared.

**Goal:** a small, well-tested framework so each farm is just "which germlings, which materials, how to harvest", with no per-farm block entity, menu or screen.

## Lineup

One block per farm type. Replacing one farm block with another is how a player switches.

| # | Farm | Grows | Water |
|---|---|---|---|
| [0203](0203-arboretum.md) | Arboretum | trees | none |
| [0204](0204-crop-farm.md) | Cropland (Crop Farm) | field crops | per till |
| [0205](0205-peat-bog.md) | Peat Bog | Bog Earth → Peat | none from the tank (wetness: see brief) |
| [0206](0206-mushroom-farm.md) | Mushroom Farm | giant mushrooms, huge fungi | none |
| [0207](0207-plantation.md) | Plantation | sugar cane, bamboo | 1000 mB per channel source |
| [0208](0208-nether-wart-farm.md) | Nether Wart Farm | nether wart | none |
| [0209](0209-berry-farm.md) | Berry Farm | sweet berries | none |
| [0210](0210-cocoa-plantation.md) | Cocoa Plantation | cocoa | none |
| [0211](0211-cave-vine-farm.md) | Cave Vine Farm | glow berries | none |
| [0212](0212-gourd-farm.md) | Gourd Farm | pumpkins, melons | per till |
| [0213](0213-desert-farm.md) | Desert Farm | cactus | none |

## Requirements

### Functional
- **One block per farm type.** No farm-type selector. Every farm block shares one block entity type.
- **Work area (centred ring):** the farm block sits at the centre of a **7×7 courtyard** (`FarmGeometry.COURTYARD = 7`, a code constant). The courtyard is excluded through the farm's **whole vertical range**: no write ever lands in a courtyard column, at any Y. It is where the player puts pipes, engines and chests. The farm works the **ring** between the courtyard and an outer square set by its tier:

  | Tier | Outer | Ring width | Plots |
  |---|---|---:|---:|
  | Basic | 11×11 | 2 | 72 |
  | Industrial | 15×15 | 4 | 176 |
  | Precision | 19×19 | 6 | 312 |
  | Infernal | 23×23 | 8 | 480 |

  The soil layer is level with the farm, at its own Y, and plots (the crops and saplings) are one block above it at Y+1, so a farm placed on the ground raises its field one block. Layouts are **lane-stable**: patterns are defined per lane from the inside out, so an upgrade only adds lanes and never moves existing plants.
- **Tiers:** every farm is crafted at Basic and raised one step at a time with Upgrade Kits ([0111](0111-machine-frame-tiers.md)). A tier change resizes the ring, the energy buffer and the tank at once.
- **Clearing:** before the logic works a plot, the farm clears the plot's column top-down, one block per action, from the plot level (Y+1) up to the logic's `clearanceHeight()` (Y+5 for Cropland, Y+10 for the Arboretum). Drops go to the output; fluids, source or flowing, are drained. The farm's own crops and trees are harvested, not cleared. **Block entities, unbreakable blocks (hardness < 0) and `#logistics:forestry/never_clear` (ships empty) are never broken**: the plot reports *Blocked* instead, so chests never spill. The soil layer at Y is replaced with the farm's own soil unless it is already prepared; anything else there is broken into the output first, and air is simply filled.
- **Chunk behaviour:** works only while its chunk is loaded. No chunk loading, and a scan never loads chunks (`level.isLoaded(pos)` before every read).
- **Work loop:** the [scheduler](#scheduler) spends an action budget and a check budget each tick. Inspections cost no RF; every world change is one `PlotAction` with its own cost.
- **Output:** a 9-slot internal buffer. When it is full the farm stops harvesting (planting and soil work continue) and shows *Output full*. It never drops or voids items in the world; it sweeps up loose drops in its own ring. Random drops beyond the free space go to an NBT overflow list.
- **Redstone:** a redstone signal pauses the farm.
- **Status (Jade + GUI):** one of Surveying, Working, No power, No water, No germlings, No materials, Output full, Paused (redstone), Idle (nothing to do), plus Too dark / Blocked instead of retrying forever. Usable and blocked plot counts per pass.
- **No fake players.** Plant with `setBlock` after `canSurvive`; harvest with `Block.getDrops(…, ItemStack.EMPTY)` + `destroyBlock` (accept drops only when it returns true); grow through `PlantCompat`. The owner is recorded on placement behind a no-op `ProtectionCompat` seam, since claim mods see no break event without a fake player.
- **Slots:** 4 germlings (0–3) + 4 **materials** (4–7) + 9 output (8–16). Materials are whatever the farm places besides germlings — soil, support logs, ceiling blocks, fertilizer — and each farm sets the role filter.
- **Pipes and hoppers:** inserting on any face routes each item by role (germling → 0–3, material → 4–7); once its group is full, or for anything else, it is refused. Every face exposes only the output slots for extraction, so a Provider pipe offers only produce and a Supplier pipe keeps inputs stocked.
- **Water:** an input-only tank accepting `c:water`, filled by a Pump, fluid pipe, Glass Tank or Fluid Supplier pipe. Only farms that use water have one: Cropland and the Gourd Farm (per till) and the Plantation (per source placed). All other farms use none.
- **Power:** cables or engines like any machine. Not low-tier, so a Redstone engine can't power it. An idle farm draws **0 RF/t**.

### Balance

Every number is a config key in `forestry/farm.json`. Energy is charged **per successful action**; inspections are free.

| Action | Default RF |
|---|---:|
| Till | 25 |
| Place substrate | 50 |
| Plant (per cell) | 25 |
| Harvest crop (per block) | 50 |
| Harvest log / clear a block | 25 |
| Harvest leaves (per block) | 5 |
| Pick berries / cocoa | 40 |
| Place support | 50 |
| Moisten farmland | 5 |
| Place water source | 50 (+1000 mB) |
| Fertilize (Mushroom Farm) | 25 |

Per-tier profile. Each tier doubles throughput, and every tier's peak fits a cable of the matching rank (30 / 60 / 120 / 240 RF/t):

| Tier | Checks/s | Actions/s | Peak RF/t | Buffer | Max in (RF/t) | Tank |
|---|---:|---:|---:|---:|---:|---:|
| Basic | 16 | 4 | 10 | 20k | 64 | 8 B |
| Industrial | 32 | 8 | 20 | 40k | 128 | 16 B |
| Precision | 64 | 16 | 40 | 80k | 256 | 32 B |
| Infernal | 128 | 32 | 80 | 160k | 512 | 64 B |

- A full inspection pass takes 3.75–5.5 s at every tier.
- **Water:** `water_per_till_mb` (100) × `h`, where `h = clamp(max(temp, 0.8) × (hasPrecipitation ? 1 : 1.5), 0.5, 2) × (raining && canSeeSky ? 0.5 : 1)` — a pure function in `forestry/farm/Hydration`. Placing a water source costs `water_per_source_mb` (1000).
- **Benchmarks** (JUnit simulations, ±10%): idle is 0 RF/t; a fully mature Basic cropland pass takes ≈54 s and 7.2k RF (≈6.7 RF/t); an oak felling (≈65 blocks, ≈425 RF) takes ≈16 s at Basic alone, ≈32 s while cultivation also has work.
- Config keys: `tier.<id>.{outer_size,checks_per_second,actions_per_second,energy_capacity,max_energy_input,water_capacity_mb}`, `rf_*` per action, `water_per_till_mb`, `water_per_source_mb`, `harvest_share`, `reads_per_check`, `cave_vine_max_length`. `work_interval_ticks`, `harvest_blocks_per_tick`, `rf_harvest_per_block` and the flat buffer keys are dropped; farms are unreleased, so there is no migrator.
- Recipe: the standard machine recipe (Machine Frame + Redstone Reception Coil + copper gears) with themed items and **1–2 valves** per farm. Decided: Arboretum = copper, Crop Farm = tin. Proposed: Peat Bog = rubber, Mushroom Farm = obsidian, Nether Wart Farm = blazing, Berry Farm = emerald. The other farms' valves are settled in their PRs.

### Forestry 1.12 reconciliation

Read from Forestry's `mc-1.12` source (`TileFarmGearbox`, `FarmController`, `FarmHydrationManager`, `FarmLogic*`). Forestry prepays 200 RF per operation but only spends it on success, so in practice it costs 200 RF per successful op and nothing when idle, at about 4 ops/s per gearbox. It alternates CULTIVATE and HARVEST, removes one block per harvest call top-down, charges water per op with a climate modifier, and spends one item per substrate block.

- **Kept:** energy per successful action, idle at zero, per-block tree charging, top-down harvest queues, idle back-off, harvest alternating with cultivation, water per op with a climate modifier, one item per substrate. Basic matches one Forestry gearbox: 4 actions/s at a quarter of the energy.
- **Changed:** no prepaid energy; leaves are broken and charged cheaply; fertilizer is optional (a future augment); a simpler, bounded climate modifier; separate inspection and action budgets; tree crawls capped and budgeted.

## Design sketch

### Shared-framework extensions (`core`, each a small PR with JUnit)
- **`SidedLayout` per-slot filters** (`core/machine/component/SidedLayout`): add an `anyFace(inputs, outputs, slotFilter)` factory and `ItemsBuilder.slotFilter(range, predicate)`. Apply the same filter in `canPlaceItem`, so menus and hoppers obey it. `ContainerItemStorage.insert` already walks slots through `canPlaceItemThroughFace`, so role routing then works with no further change.
- **`FluidBuilder.inputOnly(filter)`:** generalises `SteamEngineBlockEntity.SteamEngineFluidView`. Today `FluidStoreComponent` has no input-only or fluid-filter mode.
- **`core/lib/compat/PlantCompat`:** the only place version-specific plant APIs are touched, alongside `NbtCompat` / `RecipeCompat`:
  - `canFertilize(level, pos, state)` / `fertilize(level, random, pos, state)` — 26.x `BonemealableBlock` methods take an extra `BonemealSource`.
  - `harvestBerries(level, pos, state)` — 26.x reads harvest loot tables where 1.21.1 hard-codes counts. Rolls drops only after `setBlock` succeeds (see [Action atomicity](#action-atomicity)).
  - Fungus growth — `NetherFungusBlock` with `Feature` keys on 26.x, `FungusBlock` with `ConfiguredFeature` keys on 1.21.1.
- **`core/machine/area`:** a `PlotArea` interface implemented by `WorkArea` (a square) and `RingArea(origin, inner, outer)` (the band between two odd squares, exposing each plot's `lane` and `step` for planting patterns), plus `PlotScanner` (a resumable round-robin cursor that backs off after a pass with no work).
- **Tiers** come from [0111](0111-machine-frame-tiers.md): `TierComponent`, `MachineBuilder.tier`, `EnergyStorageComponent.resize`, `FluidStoreComponent.resize`.
- Add `'forestry'` to `domains` in the root `build.gradle` (`checkImportBoundaries`).

### Prerequisite fix — Provider pipes advertise only extractable slots
A real `fix(routing)` PR, worth landing without farms: `ProviderModule.scanStorage` advertises a slot only if a simulated 1-item extract succeeds. Without it a Provider pipe on any machine offers input slots it can never deliver, because all three `contents()` implementations list every slot on the face.

### Domain scaffold
- `com.logistics.forestry` / `LogisticsForestry` (`DomainBootstrap`, listed in `META-INF/services`); IDs `logistics:forestry/<name>`. Bog Earth and Peat stay in `core`.
- Client: `LogisticsForestryClient` + its Fabric services entry; NeoForge wiring in `NeoForgeClientSetup`.

### `forestry/farm`
```text
common/src/main/java/com/logistics/forestry/farm/
├── FarmMachineBlock.java     # abstract; extends core/lib/block/MachineBlock; FACING + LIT
├── FarmBlockEntity.java      # one shared type for every farm block; MachineEntity + MachineBuilder + tier
├── FarmComponent.java        # scheduler, survey state, FarmPlan
├── FarmGeometry.java         # COURTYARD = 7; ring from the tier profile
├── FarmWorldAccess.java      # courtyard write guard (fails closed)
├── ColumnClearer.java        # pure: next block to clear from a plot's column
├── FarmLogic.java            # per-farm SPI: clearance, germlings, materials, plot actions, harvestCost
├── PlotAction.java           # Harvest / PlaceSoil / Till / Moisten / Plant / Pick / PlaceSupport / PlaceWater / Fertilize / None / Blocked
├── Hydration.java            # pure climate factor h
└── FarmStatus.java
```
- **Naming:** `FarmMachineBlock`, not `FarmBlock` (vanilla farmland on 1.21.1) and not `BushBlock`. Code never references the farmland class (`FarmBlock` on 1.21.1, `FarmlandBlock` on 26.x); use `Blocks.FARMLAND` + `BlockStateProperties.MOISTURE`, and write moisture with flag 2.
- **One shared block entity type** registered for every farm block (`registerBlockEntity(name, factory, Block...)`), so NeoForge needs one capability line per capability.
- **`FarmLogic` selection** in `configure()` from `getBlockState().getBlock()`. Fields are declared without initializers, as in `LaserQuarryBlockEntity`, because initializers run after `configure()`.
- **Components:** `EnergyStorageComponent`, `ItemStoreComponent` with `SidedLayout.anyFace(...)` role filters, `FluidStoreComponent` with `inputOnly(c:water)` when the logic uses water, `TierComponent`.
- **Costs:** `FarmCost` (an `OperationKey`) per action, plus `FarmLogic.harvestCost(BlockState)`, so a future Efficiency augment ([0106](0106-machine-upgrades.md)) applies through `MachineModifiers`.
- **World-touching component:** modelled on the Fluid Pump's `automation/pump/FluidPumpComponent`. Drop collection re-implements `QuarryBlockBreaker`'s `getDrops` + `destroyBlock` pattern and `QuarryOutput.collectLooseItems` in core, since domains can't import `automation`.
- **Status / Jade:** expose status through `MachineHudModel`; register the abstract `FarmMachineBlock` once in both loaders' Jade plugins and the Fabric client plugin (subclasses match), backed by `compat/jade/MachineServerDataProvider`. `ContainerData` gains `TIER`, `PLOTS_BLOCKED` and `STATE`.
- **Persistent NBT:** `Cursor`, `Owner`, `Harvesting`, `HarvestBase`, `HarvestedPlots`, `Overflow`, `Tier` (string) and `State` (`survey|active`). Not saved: tokens, back-off, planner frontier, harvest queue (re-planned on load), layouts, the preview list.
- **Test hooks:** `setAreaOverride(inner, outer)` (kept as the test seam) and `setClearanceHeightOverride(h)`, per-instance overrides following the `FluidPumpBlockEntity.setArmSpeedOverride` precedent.

### Geometry and courtyard guard
- **`FarmGeometry`:** `COURTYARD = 7`; the outer size comes from `FarmTierProfile` in `FarmSettings`. `FarmLogic.ringInner()` / `ringOuter()` are removed. On a tier change the scanner, energy buffer and tank are resized.
- **Courtyard guard:** one `FarmWorldAccess` wrapper rejects every write whose X/Z falls in the courtyard, at any Y, and fails closed. `TreeView` returns `NEVER_TOUCH` for courtyard columns. Felling bounds are the outer square plus a 3-block canopy margin; wood must sit inside the outer square. The loose-drop sweep stays filtered to the ring.

### Scheduler

Replaces `work_interval_ticks` and `harvest_blocks_per_tick`.

- **Two token buckets.** The action bucket refills at `actions/s ÷ 20` per tick, the check bucket at `checks/s ÷ 20`. Each is capped at 2 ticks' worth (minimum 1), so no tick ever exceeds 2 × rate, even after idling or a reload. Tokens are not saved.
- **Two lanes share the action budget.** The **harvest lane** works the active multi-block harvest; the **cultivate lane** inspects plots and performs the one op each needs. When both have work, actions **alternate 50/50** (`harvest_share`, default 0.5); config can't push the cultivate share below **25%**, so a 500-block tree never stalls planting, tilling or the discovery of other plots. Either lane takes the whole budget when the other is idle.
- **Inspection is bounded by block reads.** Each check token buys `reads_per_check` (64) reads. A plot inspection reads its column (at most `clearanceHeight` + 2 blocks) and costs 1 check; tree discovery is a seed probe of at most 5 reads inside it.
- **Incremental tree planning.** `FellPlanner` runs `TreeFeller` as a resumable BFS (open frontier + visited set in memory); every `step(budgetReads)` spends check tokens. While a plan is in progress the cultivate lane pauses discovery, then gets its reserved share once the plan completes. Capped at 1024 blocks; a Basic farm finishes the largest plan in about 1 s.
- **One plan and one active harvest at a time.** A second tree found during a harvest is deferred: the cursor moves on and finds it again next pass. Nothing queues unboundedly.
- **Hard per-tick ceilings** (asserted in tests): actions ≤ `ceil(2 × actions/s ÷ 20)`; block reads ≤ `ceil(2 × checks/s ÷ 20) × 64`, planner nodes included.
- **Re-validation:** every harvest target is re-checked when executed (loaded, same block, not courtyard, `canModify`). A plan invalidated by a world change, such as a player breaking logs, is dropped and re-planned from the base, never executed stale.
- **Idle:** an empty full pass triggers the `PlotScanner` back-off (20 → 200 ticks).

### Survey and activation

- **State machine `SURVEY → ACTIVE`**, saved as a string in NBT.
  - **SURVEY** (on placement): a read-only scan on the check budget. It **breaks nothing, places nothing and draws no RF**. The preview is shown and the GUI summarises: "N blocks will be cleared, of which M look player-built (amber); P plots are protected".
  - **ACTIVE:** the player presses **Start** (vanilla `clickMenuButton`). **Start is consent**: blocks flagged as player-built are then cleared into the output like any other obstruction. Only block entities, unbreakable blocks and `#never_clear` are skipped.
- **Kit upgrades act immediately.** The new lanes are worked straight away, with no confirmation step. To avoid surprises the kit tooltip says "Expands the work area immediately", and applying it shows the preview for 30 s with an action-bar summary: "Now 15×15 · N blocks will be cleared (M look player-built)".
- **"Looks player-built" classifier:** every block not in `#logistics:forestry/natural_clearable` (grass, dirt, sand, gravel, the stone family, flowers, tall grass, snow, natural leaves, vines, fluids, logs with natural canopy) is flagged. A pure function; it only drives the preview and the warnings.
- **Foreign leaves:** a natural tree outside the farm whose leaves hang over ring columns loses those leaves as obstructions; its trunk, and its leaves outside the ring columns, are never touched.
- **Preview renderer** (client, both loaders): courtyard outline (white), outer boundary (green), flagged blocks (amber), protected blocks (red). Reuses the line approach of `LaserQuarryBlockEntityRenderer`'s frame preview and `MarkerBlockEntityRenderer`'s culling overrides. Flagged and protected positions sync in the update tag (capped at 256 plus a count) only while the preview is visible: in SURVEY, while the GUI is open, and for 30 s after **Show area**.

### Positional ownership

Farm-built structures (a jungle log at a cocoa anchor, a ceiling block at Y+5, a water source in a channel cell) are recognised by **layout position plus expected block**. Nothing is saved, so it is reload-safe and tier-stable. Documented limitations (also on the JEI info page):

1. **Adoption.** A player-placed block that matches the expected block at a structure position is treated as the farm's own: kept, never harvested, never charged for.
2. **Intent is invisible.** A removed support is rebuilt at the cost of material and RF; the farm can't tell deliberate removal from damage.
3. **Wrong block at a structure position** is an obstruction: after Start it is cleared into the output, then the structure is rebuilt. Block entities and `#never_clear` leave that cell Blocked.
4. **Changing the farm type.** The previous farm's structures are not the new farm's: they are cleared into the output (recovering the material) or flagged in the survey.
5. **Outside the vertical range** (above the clearance height or ceiling, below the soil layer) nothing is recognised or touched.
6. **Water channels.** Flowing water from a broken channel is drained by clearing; a channel the player fills in is cleared and re-placed for 1000 mB.

### Action atomicity

Every action **validates** without side effects (loaded, not courtyard, `canModify`, energy ≥ cost, inputs, water, output room where drops are expected, `canSurvive`), then **mutates** the world, then **deducts costs only if the mutation succeeded**. The server tick is single-threaded, so nothing changes in between. Drops are computed before `destroyBlock` and accepted only when it returns true; the overflow list means nothing is voided.

| Action | Mutation | On success | On failure |
|---|---|---|---|
| Clear / Harvest block | `destroyBlock(pos, false)` | add drops, charge `CLEAR` / `HARVEST_*` | no charge, no drops; plot Blocked (refused) or harvest re-planned (changed) |
| Drain | `setBlock(AIR)` | charge `CLEAR` | none |
| PlaceSoil / PlaceSupport over an occupied cell | break (as Clear), then `setBlock(new)` | each step accounted separately: break charges `CLEAR` + drops; place consumes 1 item + `SOIL` / `SUPPORT` | break failed → nothing happens; place failed after the break → cell is air, break accounted, no item or placement charge, retried next pass |
| Till | `setBlock(farmland)` | water + `TILL` | none |
| Moisten | `setBlock` (flag 2) | `MOISTEN` | none |
| Plant (multi-cell) | set each cell | n germlings + n × `PLANT` | a later cell fails → earlier cells rolled back to air, nothing consumed |
| Pick | `PlantCompat.harvestBerries` | add drops, `PICK` | no drops, no charge |
| PlaceWater | `setBlock(water)` | 1000 mB + `WATER_SOURCE` | none |
| Fertilize | `PlantCompat.fertilize` | consumed per valid attempt, like bone meal, even if the growth roll fails | invalid target → nothing consumed |
| Upgrade kit | `setTier` + resize | kit consumed | nothing consumed |

### Multifarm seam (contract only)

Every block holds exactly one logic. To keep a future universal controller possible without building it:
- **`FarmPlan`** in `FarmComponent`: `logicFor(plotIndex)` and `regionFor(plotIndex)`. The only implementation is `SingleLogicPlan`, one logic over the whole ring. The scheduler, harvest lane, costs, clearance height and status counts all key off `logicFor(plot)`, never a field.
- **`FarmLogic` contract** (javadoc + tests): stateless; decides from `(world, region, plot, inputs)`; layouts use region-relative lane/step coordinates through a `PlotLayout` view, never the ring's total plot count; multi-cell placements (2×2 groups, gourd fruit cells, cocoa faces) must satisfy `region.contains(cell)`; felling bounds are the region's outer bounds.
- **`FarmInputs`** stays filtered per logic; water and energy stay shared per block.
- **Not built:** region assignment UI, several logics per block, per-region tiers. A two-region `FarmPlan` test over a scripted logic proves the seam.

### Shared farm screen
One menu + screen for every farm, configured by farm type (approved layout):
- **176×166**, ghost icons instead of text labels.
- **Left:** energy bar, and a water gauge only on farms with a tank.
- **Middle:** a row of 4 germling slots (0–3); a row of 4 material slots (4–7); then the status lamp.
- **Right:** a 3×3 output grid (8–16).
- **Status lamp** tooltip shows the tier and size, e.g. "Working · Industrial · 15×15 · 7×7 courtyard", plus usable and blocked plot counts.
- **Start** button while in SURVEY; **Show area** afterwards.

## Scope & non-goals

- **In:** the framework extensions above, the Provider-pipe fix, the domain scaffold, the farm block/entity/component/logic SPI, the shared screen, Jade, NeoForge capabilities, tiers and Upgrade Kits (via [0111](0111-machine-frame-tiers.md)), the scheduler, survey + Start and the area preview, the `FarmPlan` seam.
- **Out:** genetics ([RFC 0002](../rfcs/0002-forestry-bees.md)), multiblocks and the Multifarm, chunk loading, fake players, circuit-board programmability (redstone enable/disable only — [RFC 0001](../rfcs/0001-programmable-behavior.md) option B), augments such as speed, efficiency and fertilization (later, through [0106](0106-machine-upgrades.md)), several logics per block, farm-specific logistics modules, the Squeezer/Fermenter/Still/Bottler chain and peat/biofuel engines.

## Open questions

- **mc/1.21.1 port prerequisite:** that branch lacks `MachineHudModel` / `HudContributor`. Backport the HUD layer before the farm chunks are ported there.
- Per-farm recipes and valves, textures and exact kit shapes are settled in each PR.
- Polish after Phase 1: live per-plot blocked markers and particles in the preview.

## Done when

- A farm block with a trivial test `FarmLogic` works its plot on both loaders: no power → no work, Output full → stall, redstone → pause.
- A hopper inserts germlings and materials into the right slots, and a hopper / Provider pipe can pull only output.
- Water arrives from a Glass Tank or fluid pipe into the input-only tank.
- **World safety invariants**, each with a test: no write ever lands in a courtyard column; nothing is modified before Start; block entities, unbreakable blocks and `#never_clear` are never broken; no failed action consumes resources, duplicates drops or changes the world unaccounted; felling stays inside its bounds and never steps from canopy into a foreign trunk; per-tick work is bounded; nothing is scheduled outside the block entity and chunks are never loaded; output never voids.
- JUnit covers `WorkArea` / `RingArea` geometry, `ColumnClearer`, `PlotScanner`, the `SidedLayout` filters, `FluidBuilder.inputOnly`, scheduler fairness (cultivate lane ≥ 25% of actions while it has work) and ceilings, planner resume and invalidation, `Hydration`, the `natural_clearable` classifier, survey state across a reload, each partial-failure case in the atomicity table, a conservation property test (items and energy in + out balance against the world delta), and the two-region `FarmPlan` seam.
- GameTests (`common/src/gametest/.../forestry/`, both loaders, `checkFeatureTestParity`) use real connectivity on natural grass terrain: survey changes nothing; Start clears flagged and natural blocks but never block entities; a kit upgrade mid-run expands immediately; break and replace keeps the tier; a chunk-edge farm; removal mid-felling; swapping the farm type; ownership tests per structure farm.

## References

- Breakdown: [`../mods/forestry.md`](../mods/forestry.md) → Farms
- Roadmap: [`../delivery-plan.md`](../delivery-plan.md) → Phase 2 → Farms; [`ROADMAP.md`](../../ROADMAP.md) → Next
- Code: `core/machine/MachineEntity`, `MachineBuilder`, `core/machine/component/{SidedLayout,ItemStoreComponent,FluidStoreComponent,EnergyStorageComponent,SlotRole}`, `core/machine/upgrade/{MachineModifiers,OperationKey}`, `core/machine/MachineHudModel`, `core/machine/area/{PlotArea,RingArea,PlotScanner}`, `core/lib/compat/{PlantCompat,ProtectionCompat}`, `automation/pump/FluidPumpComponent`, `automation/laserquarry/entity/{QuarryBlockBreaker,QuarryOutput}`, `LaserQuarryBlockEntityRenderer`, `MarkerBlockEntityRenderer`, `pipe/modules/ProviderModule`, `core/lib/storage/ContainerItemStorage`
- Related: [0111 Machine Frame Tiers](0111-machine-frame-tiers.md), [0106 Machine Upgrades](0106-machine-upgrades.md), [0202 Fertilizer & Soils](0202-fertilizer-and-soils.md)
