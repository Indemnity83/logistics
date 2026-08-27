# Testing Guide

## Test Structure

| Module | Location | Type | Framework |
|--------|----------|------|-----------|
| `common` | `common/src/test/java/` | Unit | JUnit 5 + Minecraft bootstrap |
| `fabric` | `fabric/src/test/java/` | Unit | JUnit 5 (plain) |
| `neoforge` | `neoforge/src/test/java/` | Unit | JUnit 5 (plain) |
| `fabric` | `fabric/src/gametest/java/` | Integration | Fabric @GameTest (deprecated/manual) |

### Running tests

```bash
./gradlew :common:test          # Common business logic unit tests
./gradlew :fabric:test          # Fabric ServiceLoader and adapter unit tests
./gradlew :neoforge:test        # NeoForge ServiceLoader and adapter unit tests
./gradlew testCoverage          # Aggregate local JaCoCo coverage report
./gradlew :fabric:runGameTest   # Integration game tests (deprecated/manual; see below)
```

### Formatting

```bash
./gradlew lint                  # Check repository text files and all module Java sources
./gradlew fix                   # Apply repository and module formatting
./gradlew repoLint              # Check repository text files only
./gradlew repoFix               # Format repository text files only
./gradlew :common:lint          # Check one module
./gradlew :common:fix           # Format one module
```

The same `lint`/`fix` aliases are available for `fabric` and `neoforge`.

### CI

Pull request CI runs under the **Check PR** workflow:

- **Check PR**:
  - `lint (pr)` checks the PR title.
- **Check Code**:
  - `lint (common)` checks repository-level formatting, common Java formatting, and import boundaries.
  - `lint (fabric|neoforge)` checks module Java formatting.
  - `test (common|fabric|neoforge)` runs module unit tests and uploads that
    module's JaCoCo coverage to Codecov (tagged with a per-module flag). Codecov
    merges the three uploads into the combined coverage for the commit.

PR CI does not build preview jars automatically. Use the manual **Build PR Artifacts**
workflow when a branch needs downloadable Fabric or NeoForge jars for smoke testing.

### Coverage

Local aggregate coverage is generated with:

```bash
./gradlew testCoverage
```

The HTML report is written to `build/reports/jacoco/testCoverage/html/index.html`.
The XML report is written to `build/reports/jacoco/testCoverage/testCoverage.xml`.

Coverage is reported and gated by [Codecov](https://codecov.io/gh/Indemnity83/logistics).
On every PR each `test` matrix job uploads its module's JaCoCo report (tagged with a
`common` / `fabric` / `neoforge` flag) and Codecov merges them, so the suite runs only
once. Codecov posts a summary comment, flags uncovered changed lines inline, and
publishes `project`/`patch` status checks. The thresholds live in `codecov.yml`
(project: no drop beyond 0.5%; patch target 20%), so there is no hand-maintained
baseline to bump. The local `testCoverage` task above still produces the aggregate
HTML/XML for offline inspection.

Current aggregate snapshot from `./gradlew testCoverage`:

| Counter | Coverage |
|---------|----------|
| Instruction | 18.0% |
| Branch | 15.8% |
| Line | 18.0% |
| Complexity | 17.3% |
| Method | 23.0% |
| Class | 34.9% |

High-coverage pure-logic areas include `core.lib.resource`, `core.lib.filter`,
`power.engine`, `power.cable`, `core.lib.energy`, `core.lib.network`, and `neoforge.energy`.
The aggregate percentage is still low because the report includes block entities,
blocks, menus, live-world runtime paths, bootstrap code, and loader entrypoints
that are documented below as requiring restructuring or integration tests.

---

## Wiki-Driven Feature Tests

Treat the project's public documentation (`logistics-docs`, `wiki/*.txt` — MediaWiki wikitext, one
file per block/item/concept, pushed to the Fandom wiki) as the spec, and write tests that confirm
each documented behavior claim, rather than tests that only assert internal wiring. A pilot pass on
the Kiln (`common/src/test/java/com/logistics/automation/kiln/`,
`fabric/src/gametest/.../automation/KilnGameTest.java`) established the recipe below.

1. **Pull the wiki page** (`wiki/<Name>.txt` in `logistics-docs`, `docs` branch). Extract verbatim
   claims from Usage/Power/Setup only — History is explicitly not current-behavior per that repo's
   own convention.
2. **Read the real implementation**, tracing into whatever resolver/component config it delegates
   to. Write down the actual source of every number the wiki quotes.
3. **Compare, don't assume.** Classify each wiki claim: matches / mismatch / needs a live tick to
   verify. Log mismatches in `WIKI_DISCREPANCIES.md` rather than silently picking a side — fixing
   the wiki or the code is a separate, deliberate decision, not something a test should presume.
4. **Classify each existing test method**: pure wiring (keep, no wiki quote needed) vs.
   real-but-unphrased feature test (rewrite with a wiki-quote Javadoc + tightened assertions) vs.
   missing (write new).
5. **Prefer `common/src/test` JUnit over GameTest** wherever the computation is a pure class
   reachable without a live `Level`/`RecipeManager` (config defaults, `RecipeProcessPlan` math).
   Reserve GameTest for what genuinely needs a ticking world: item movement through
   `WorldlyContainer` faces, live recipe *resolution* against an inventory, blockstate changes.
6. **Check the recipe JSON, not just the block entity.** A block's crafting/processing recipe is
   itself a documented claim (the wiki's Crafting section) and a file that can drift independently
   of the code you just read in step 2. Decoding a *specific* recipe JSON is plain JUnit — read it
   directly (see `KilnRecipeTest`) rather than assuming it needs a live `RecipeManager`, which is
   only required for recipe *resolution*, not decoding one known file.
7. **Apply the traceability convention** below to every feature test.
8. **Fabric only.** NeoForge's GameTest registration is blocked upstream (see "NeoForge Game Tests"
   below, once that section exists) — don't add or modify `neoforge/src/gametest` for this work.

### Real connectivity, not direct capability calls

GameTests are the layer that proves a feature works the way a player actually hooks it up; plain
JUnit is the layer that protects the internal pieces (config values, pure recipe/component math)
from regressing. For the assertion that proves a feature works — not a precondition unrelated to
what's under test — prefer:

- **Power in** via a real, directly-adjacent `CREATIVE_ENGINE` (no cable needed unless cable
  routing itself is what's under test), not `energyStorage().insert()`. Cycle its output level a
  few notches above the machine's own max-input cap so the engine is never the bottleneck being
  measured; the machine's own cap still throttles what it actually receives per tick.
- **Items in** via a real `Blocks.HOPPER` above (or whichever face the wiki calls out) with the
  ingredient placed directly in the hopper's own slot — filling the *hopper's* inventory this way is
  a precondition unrelated to what's under test (hopper→machine transfer), same as pre-filling energy
  to isolate recipe math.
- **Output verified** by what a real downstream `Blocks.HOPPER`/pipe/chest received
  (`context.succeedWhen(() -> context.assertContainerContains(pos, item))`), not by reading the
  block entity's own slot/tank state directly.
- Direct capability manipulation (`setItem()`, `energyStorage().insert()`, `fluidStorage().insert()`)
  remains fine for multi-slot/multi-item setups where real hopper distribution across several
  distinct slots is genuinely uncertain (e.g. dual-input recipes) — add the real-connectivity test
  alongside the existing precise-math test rather than replacing it.

**Gotcha: `CREATIVE_ENGINE`'s `POWERED` blockstate is not a stable manual flag.**
`AbstractEngineBlock.neighborChanged()` recomputes `POWERED` from the actual redstone signal on
every neighbor update and overwrites a hand-set `true` — within 2-3 ticks in practice, once the
adjacent hopper/machine placement fires an update. A test that just does
`.setValue(AbstractEngineBlock.POWERED, true)` with no real signal will silently lose power after a
couple of ticks (existing short-window tests never noticed because they only check `amount > 0`).
Place a real `Blocks.REDSTONE_BLOCK` adjacent to the engine (on a face other than its output) so the
signal is genuine and `POWERED` stays true for the test's full duration.

### Traceability convention

`@GameTest` methods are discovered by Fabric's shim, not JUnit, so `@DisplayName` isn't available —
use a Javadoc block instead:

```java
/**
 * Wiki claim (Power): "It holds 10,000 RF and accepts up to 128 RF/tick."
 *
 * @see <a href="https://logistics.fandom.com/wiki/Kiln#Power">wiki/Kiln.txt § Power</a>
 */
@GameTest
public void testKilnEnergyCapacityAndInputCap(GameTestHelper context) { ... }
```

Where code and wiki disagree, add a `NOTE:` line and assert the **code's real behavior** — never
adjust an assertion to match a wiki number without independently confirming it in source:

```java
// NOTE: wiki/Kiln.txt claims ~4,000 RF / 10s; code computes cookingTime(200) *
// KILN_RF_PER_COOK_TICK(10) = 2,000 RF at 100 ticks. See WIKI_DISCREPANCIES.md.
```

Pure wiring tests keep their existing plain one-line Javadoc, no wiki quote — that absence *is* the
signal "this is an implementation invariant, not a documented behavior."

### Recipe testing

Recipes are ~628 hand-authored JSON files (`common/src/main/resources/data/logistics/recipe/**`, 13
domains, no datagen). Decoding a *specific* recipe file to check its content is plain JUnit — no
live `RecipeManager`/datapack reload needed, that machinery is only required for *resolving* a
recipe against a live inventory. `KilnRecipeTest` reads `kiln.json` directly and compares it against
the wiki's Crafting section — this is also how a stable registry id surviving a display-name rename
(`machine_core` / "Machine Frame") was confirmed *not* to be a mismatch, a useful reminder to check
the lang file before flagging an id-vs-wiki-name difference as a discrepancy.
`RecipeJsonSmokeTest` is mod-wide infrastructure, not block-specific: it walks every recipe JSON
under `common/src/main/resources/data/logistics/recipe/` and checks each one is valid JSON with a
non-blank `type`, and — where a `result` is present — a non-blank `id` or `fluid` field (it does not
check `count`/`amount`) — catching a JSON syntax error, a missing `type`, or a `result` with no
identifiable item/fluid (the cheap, high-frequency ways a hand-edited recipe silently breaks) on
every `./gradlew :common:test` run, mod-wide, at effectively zero marginal cost per recipe added.
It's deliberately structural, not semantic: it doesn't resolve ingredient item ids through
Minecraft's registry (vanilla's registry silently defaults an unknown id to `minecraft:air` instead
of failing, so that check wouldn't reliably catch an item-id typo anyway).

### Feature-test backlog

Recorded here so a pass doesn't have to re-derive priority order or re-discover what's already done.

#### Automation domain (in progress — goal is full coverage of every `LogisticsAutomation.BLOCK`)

~~1. **Kiln**~~ — done. Heavy rewrite; 3 confirmed wiki mismatches (RF cost, smelt speed, input
   sides), since fixed on the wiki side (`logistics-docs` commits `7ba3e7af`/`2d62f834`).
~~2. **Pump**~~ — done. Unlike the Kiln, most of `FluidPumpGameTest`'s existing suite was already
   feature-shaped (furthest-first draining, infinite-body detection, output routing); the gap was a
   few unasserted wiki claims, not wholesale rewrites. Added an explicit "no power, no pumping" test
   and a push-rate test that caught a real mislabeling (wiki's 62.5 mB/t is the intake average, not
   the 400 mB/t push constant — since fixed on the wiki side, `logistics-docs` commit `61b2243a`).
   Also surfaced a lesson for the methodology itself: a recipe's raw item id (e.g. `machine_core`)
   can outlive a display-name rename ("Machine Frame") — check the lang file before flagging an
   id-vs-wiki-name difference as a mismatch (see `WIKI_DISCREPANCIES.md`'s closed Kiln entry).
~~3. **Laser Quarry verification**~~ — done, and genuinely cheap as predicted: `QuarryMiningGameTest`
   already had deep, well-targeted coverage (phase transitions, lava-as-unminable, blocked-column
   tracking across zigzag/reload, re-mining reappeared blocks) — mostly *undocumented* implementation
   robustness, not wiki claims, so it stayed untouched. Added wiki-quote traceability to the tests
   that do map to documented claims (reaching MINING phase — not a full frame-then-mine assertion,
   since the frame blocks themselves aren't checked; stops without power; output with no extractor
   needed), plus a new test confirming a quarry placed with no adjacent markers leaves custom bounds
   unset, so it falls back to the default `QUARRY_AREA = 16` config value (previously asserted
   nowhere) — this doesn't measure the resulting mined area itself. Surfaced two real gaps, tracked
   separately below: marker consumption and the chunk-loading toggle are both completely untested.
~~4. **Macerator verification**~~ — done, cheap like the Quarry: `MaceratorGameTest` already asserted
   sided access and a live maceration run correctly, and — unlike the Kiln — the wiki's numbers
   (10,000 RF capacity, 128 RF/t input, 10 RF/t drain, top-and-sides input) already matched the code
   exactly, no mismatch to flag. Added a config-default test, tightened the live test to assert the
   exact 2,000 RF cost and exact 2-dust output, and added one recipe-content spot check (iron ore →
   2 iron dust + 10% tin dust byproduct) confirming a representative one of the Macerator's 150+
   recipes matches its wiki grinding-table row exactly.
~~5. **Sawmill verification**~~ — done, and the cheapest-to-verify but highest-signal find yet:
   config numbers (10,000 RF, 128 RF/t) matched the wiki exactly, and the "2,000-3,000 RF per
   recipe" range checked out for both spot-checked recipes (oak log = 3,000, within range). But
   spot-checking the wiki's Plants → Pulped Biomass table against the actual JSON surfaced a real
   omission, not a mislabeling: the wiki lists no byproduct for any of its three rows, while wheat
   pulping genuinely grants a 50% wheat-seeds byproduct and sugar cane pulping grants a **guaranteed
   2x sugar** byproduct — both undocumented (see `WIKI_DISCREPANCIES.md` § Sawmill). Also confirmed
   the wiki's ">100% chance" wording (Oak Boat, 125%) is real, not a typo — the code supports chance
   values above 1.0 as "guaranteed + partial," now pinned by a test.
6. **Transposer verification** — has `TransposerGameTest`; same treatment (check wiki's fluid⇄item
   conversion claims and RF cost against config, spot-check one recipe).
7. **Refinery** — currently only exercised as a passive fluid-tank host in `FluidSupplierGameTest`;
   its own distillation/byproduct recipe logic (fluid-in → fluid-out + chance byproduct) has no
   dedicated test. Generic component math already proven in `RecipeProcessorComponentTest`
   (`drainsFluidInputAndDepositsFluidOutputWithByproduct`), so this is mostly wiki-claim extraction +
   a Refinery-specific GameTest/recipe check, not new component work.
8. **Crucible** — zero coverage. Item → fluid transform with chance byproducts (see the wiki's
   Oil-blocks-→-Bitumen-and-Tar recipes); moderate complexity, similar shape to Refinery but
   item-input instead of fluid-input.
9. **Alloy Smelter** — zero coverage. Dual-input recipe matching + byproduct chance; the most complex
   remaining automation machine besides the Fabricator.
10. **Sequential Fabricator** — zero coverage. Multi-step/multi-stage fabrication; likely the biggest
    lift in the domain — research its actual stage model before estimating scope.
11. **Quarry chunk-loading toggle** — surfaced by item 3; needs a `ChunkLoadingComponent`-level or
    GameTest check that toggling `quarry_load_chunks` actually acquires/releases chunk tickets.
12. **Marker block** — stateful, zero coverage, likely a short wiki page; do this before revisiting
    quarry marker-consumption, since that depends on the Marker block having basic tests first. Not
    itself an automation-domain block (`LogisticsCore.BLOCK.MARKER`), but tightly coupled to the
    Laser Quarry's custom-bounds feature.

#### Other domains (after automation)

13. **CraftingModule / ProcessModule / SatelliteModule pipes** — well unit-tested, zero in-world
    GameTest; low-risk pass adding wiki-traceability to already-correct assertions.
14. **GoldCable** — untested sibling in an otherwise-tested power-tier family; small,
    pure-math-friendly like Kiln's RF numbers.
15. **Fluid-routing modules** (FluidInsertionModule, FluidMergerModule, FluidBypassModule,
    FluidVoidModule) — zero coverage at any level, real check-valve/routing-policy logic backing 4
    registered pipe blocks.

---

## What's Covered

`common/src/test/java/` contains JUnit tests for all testable business logic:

- **Pipe modules** — BoostModule, CraftingModule, EnchantmentSinkModule, ExtractionModule, BasicExtractorModule, AdvancedExtractorModule, InsertionModule, ItemFilterModule, MergerModule, ModSinkModule, PassiveSupplierModule, PipeMarkingModule, PolymorphicSinkModule, ProcessModule, QuickSortModule, RequesterModule, SatelliteModule, SinkModule, SupplierModule, TerminusModule, TransportModule, VoidModule, WeatheringModule, BlockConnectionModule (partial), PipeOnlyModule (partial)
- **Pipe network services** — CraftBatchingService, JobCoordinator, NetworkController, ReconciliationService, RequestPlanner, ReservationManager, SinkResolver
- **Failure accounting regressions** — tracked delivery failure, partial delivery followed by failed remainder, retry accounting, and job state after dispatch loss
- **Pipe network graph** — NetworkGraph, NetworkPathfinder
- **Pipe runtime** — TravelingItem, TravelingItemPhysics, RoutePlan
- **Automation** — GridScanner, FrameLayout, QuarryBounds, QuarryPhaseRunner, ActiveQuarryRegistry, QuarryBlockBreaker, LaserQuarryConfig (default mining area), KilnEnergyConfig (config defaults + RecipeProcessPlan smelt math), KilnRecipe (wiki-vs-shipped-JSON content check), FluidPumpConfig (tank/energy/push-rate config defaults), FluidPumpRecipe (wiki-vs-shipped-JSON content check), MaceratorConfig (power config defaults), MaceratorRecipeSpotCheck (wiki-vs-shipped-JSON content check), SawmillConfig (power config defaults), SawmillRecipeSpotCheck (wiki-vs-shipped-JSON content checks, incl. undocumented byproducts)
- **Power** — CableTier, PIDController, EngineHeatModel, EngineCyclePlanner, StirlingGenerationPlanner, StirlingFuelState, CreativeOutputLevels, RedstoneTargetGate, CreativeSinkDrainState
- **Core** — BaseBlockEntity, ResourceId, MaceratorRecipe, MaceratorBlockEntityLogic, FluidTankComponent, ItemInventoryComponent, RecipeProcessPlan (shared RF-cost math backing Kiln/Macerator/etc.)
- **Serialization golden tests** — ItemFilterModule (backward compat), ProviderDispatchQueue, TravelingItem
- **Recipe JSON smoke test** — every recipe file under `data/logistics/recipe/**` (~628 files, 13 domains) parses as JSON with a non-blank `type` and well-formed `result`; see "Recipe testing" above

`fabric/src/test/java/` and `neoforge/src/test/java/` contain ServiceLoader smoke tests
verifying `META-INF/services/` registrations are present and the implementation classes
can be instantiated. Loader adapter unit tests cover thin storage and energy wrappers.

---

## Cannot Test Without Code Restructuring

The items below cannot be unit tested in their current form. Each entry notes what restructuring would unlock testing.

### Requires a real `Level` / world context

These classes take a `Level` in their constructor or rely on world state during normal operation. Extracting pure logic into a separate class (not a `BlockEntity` subclass) would make it testable.

| Class | Why |
|-------|-----|
| All `Block` and `BlockEntity` subclasses (~30+) | `BlockEntity` constructors require `Level`; block interaction is inherently stateful |
| `MinecraftWorldView` | Wraps a live `ServerLevel`; every method delegates to it |
| `NetworkCommandExecutor` | Executes commands that mutate world blocks |
| `NetworkSnapshotBuilder` | Reads live pipe state by iterating world positions |
| `NetworkRegistry` | Maps `BlockPos → PipeBlockEntity` within a world |
| `PipeNetwork` / `PipeRuntime` | Full network graph is built from world-resident blocks |
| `InsertionModule` — inventory routing paths | `getInsertSpace()` calls `ItemStorageLookup.find(level, ...)` which NPEs with null world |

### Requires Minecraft container / menu infrastructure

| Class | Why |
|-------|-----|
| All screen handlers (`ChassisScreenHandler`, `RequesterScreenHandler`, `ProviderScreenHandler`, `SupplierScreenHandler`, `SinkScreenHandler`, `ModSinkScreenHandler`, `SatelliteScreenHandler`, `ProcessScreenHandler`, `ItemFilterScreenHandler`, `AdvancedExtractorScreenHandler`, `CraftingScreenHandler`, `StirlingEngineScreenHandler`, `MaceratorScreenHandler`, `KilnScreenHandler`) | Require `AbstractContainerMenu` infrastructure and a live player |
| All inventory classes (`ChassisInventory`, `RequestInventory`, `FilterInventory`, `SinkInventory`, `SupplyInventory`, `ModSinkInventory`, `ProcessInventory`, `ProviderFilterInventory`, `AdvancedExtractorFilterInventory`, `CraftingRecipeInventory`) | Tightly coupled to screen handlers |
| `ItemTagUtils` | Queries tag registry at runtime from a live `HolderLookup` |

### Requires Minecraft networking infrastructure

| Class | Why |
|-------|-----|
| `RequestItemPacket` | Requires Minecraft's `FriendlyByteBuf` / codec pipeline |
| `OpenChassisSlotPacket` | Same |
| `SetSatelliteIdPacket` | Same |
| `SyncRequesterInventoryPacket` | Same |
| `PacketValidation` | Tests server-side packet rejection against live world state |

### Requires registered mod blocks (not in vanilla bootstrap)

| Code path | Why |
|-----------|-----|
| `BlockConnectionModule.allowsConnection()` — PipeBlock neighbor case | Returns `false` for the blocked pipe type; requires a registered `PipeBlock` instance |
| `PipeOnlyModule.allowsConnection()` — returning `true` | Only `true` for `PipeBlock` neighbors; no `PipeBlock` available in vanilla bootstrap |

### Requires Fabric / NeoForge loader initialization

| Class | Why |
|-------|-----|
| `FabricPlatformService.configDir()` / `getModName()` / `registerAlias()` | Calls `FabricLoader.getInstance()` at method invocation time |
| `FabricItemStorage` / `FabricFluidStorage` | Fabric Transfer API resource types require Fabric Loader JUnit or GameTest mixins; enabling that in the current plain unit-test task conflicts with ServiceLoader smoke-test classloading |
| `NeoForgeItemStorage` — populated item-resource slots | Creating real `ItemResource` instances touches vanilla item bootstrap, which requires a full FML loader context in the NeoForge unit-test task |
| `NeoForgeFluidStorage` | `FluidResource` / vanilla `Fluids` bootstrap touches FML-only feature flag loading without a full NeoForge loader context |

---

## Deprecated: Fabric Game Tests

`fabric/src/gametest/` contains **90+ `@GameTest` integration tests** that require a full Minecraft server process (real block placement, game ticks, Fabric transfer API). These are **deprecated** — the long-term goal is to replace them with plain JUnit equivalents as code is restructured to separate pure logic from world dependencies.

They are kept for now to preserve coverage. The primary blocker for conversion is that all tests depend on `GameTestHelper` to place blocks in an actual level and tick the server. Even "simple" placement tests require registered mod blocks and a running level — neither of which is available in a vanilla bootstrap.

### Coverage by file

| File | Tests | Conversion blocker |
|------|-------|-------------------|
| `CableGameTest` | 13 | Energy storage + TR transaction API + ticking |
| `EngineGameTest` | 16 | Fuel burning and energy production require block entity ticking |
| `PipeFlowGameTest` | 8 | Item travel requires ticking; enchantment serialization needs live data pack |
| `ModuleGameTest` | 14 | Filter/sink routing requires live `PipeContext` with real block entities |
| `PipeInfrastructureGameTest` | 5 | Pipe connectivity and cache invalidation require live blocks |
| `OreGenerationGameTest` | 11 | Ore feature registration and block placement require data-driven registries |
| `NetworkIntegrationGameTest` | 5 | Provider/requester delivery requires 100-tick game simulation |
| `QuarryGameTest` | 7 | Energy acceptance and phase tracking require live block entity |
| `QuarryMiningGameTest` | 3 | Phase machine progression and block mining require full simulation |
| `KilnGameTest` | 9 | Inventory slot access control requires live block entity |
