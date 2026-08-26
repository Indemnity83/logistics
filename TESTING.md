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

Pull request CI runs under two workflows:

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

When adding new tests, prefer extracting deterministic logic into plain common
classes and keeping Minecraft `Level`, block entity, menu, networking, and
loader APIs in thin adapters. This keeps coverage cherry-pickable across
supported branches.

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
the wiki's Crafting section (which is how the Machine Core/Machine Frame mismatch below was found).
`RecipeJsonSmokeTest` is mod-wide infrastructure, not block-specific: it walks every recipe JSON
under `common/src/main/resources/data/logistics/recipe/` and checks each one is valid JSON with a
non-blank `type` and a well-formed `result` — catching a JSON syntax error, a missing field, or a
malformed result shape (the cheap, high-frequency ways a hand-edited recipe silently breaks) on
every `./gradlew :common:test` run, mod-wide, at effectively zero marginal cost per recipe added.
It's deliberately structural, not semantic: it doesn't resolve ingredient item ids through
Minecraft's registry (vanilla's registry silently defaults an unknown id to `minecraft:air` instead
of failing, so that check wouldn't reliably catch an item-id typo anyway).

### Feature-test backlog

Not executed yet — recorded here so the next pass doesn't have to re-derive priority order.

1. **Pump** — similarly rich wiki page, single-machine story, good second data point before
   anything with dual inputs.
2. **Laser Quarry verification** — already has real coverage (`QuarryPhaseRunnerTest`,
   `QuarryGameTest`, `QuarryMiningGameTest`); cheap pass to check existing tests actually assert the
   wiki's specific claims (marker-consumption, chunk-loading toggle, power-scaling).
3. **CraftingModule / ProcessModule / SatelliteModule pipes** — well unit-tested, zero in-world
   GameTest; low-risk pass adding wiki-traceability to already-correct assertions.
4. **GoldCable** — untested sibling in an otherwise-tested tier family; small, pure-math-friendly
   like Kiln's RF numbers.
5. **Refinery's own distillation/byproduct logic** — currently only exercised as a passive tank
   host; generic component math already proven in `RecipeProcessorComponentTest`, so this is mostly
   wiki-claim extraction + a Refinery-specific test.
6. **Marker block** — stateful, zero coverage, likely a short wiki page; good "wiring tests from
   scratch" exercise.
7. **Alloy Smelter / Crucible / Sequential Fabricator / fluid-routing modules
   (FluidInsertionModule, FluidMergerModule, FluidBypassModule, FluidVoidModule) last** — genuinely
   complex (dual-input, multi-stage, chance-byproduct, routing-policy); tackle once the methodology
   is proven on 2-3 simpler blocks.

---

## What's Covered

`common/src/test/java/` contains JUnit tests for all testable business logic:

- **Pipe modules** — BoostModule, CraftingModule, EnchantmentSinkModule, ExtractionModule, BasicExtractorModule, AdvancedExtractorModule, InsertionModule, ItemFilterModule, MergerModule, ModSinkModule, PassiveSupplierModule, PipeMarkingModule, PolymorphicSinkModule, ProcessModule, QuickSortModule, RequesterModule, SatelliteModule, SinkModule, SupplierModule, TerminusModule, TransportModule, VoidModule, WeatheringModule, BlockConnectionModule (partial), PipeOnlyModule (partial)
- **Pipe network services** — CraftBatchingService, JobCoordinator, NetworkController, ReconciliationService, RequestPlanner, ReservationManager, SinkResolver
- **Failure accounting regressions** — tracked delivery failure, partial delivery followed by failed remainder, retry accounting, and job state after dispatch loss
- **Pipe network graph** — NetworkGraph, NetworkPathfinder
- **Pipe runtime** — TravelingItem, TravelingItemPhysics, RoutePlan
- **Automation** — GridScanner, FrameLayout, QuarryBounds, QuarryPhaseRunner, ActiveQuarryRegistry, QuarryBlockBreaker, KilnEnergyConfig (config defaults + RecipeProcessPlan smelt math), KilnRecipe (wiki-vs-shipped-JSON content check)
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
