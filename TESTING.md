# Testing Guide

## Test Structure

| Module | Location | Type | Framework |
|--------|----------|------|-----------|
| `common` | `common/src/test/java/` | Unit | JUnit 5 + Minecraft bootstrap |
| `fabric` | `fabric/src/test/java/` | Unit | JUnit 5 (plain) |
| `neoforge` | `neoforge/src/test/java/` | Unit | JUnit 5 (plain) |
| `common` | `common/src/gametest/java/` | Integration (shared body) | plain `Consumer<GameTestHelper>` methods, no loader imports |
| `fabric` | `fabric/src/gametest/java/` | Integration | Fabric `@GameTest` (deprecated/manual; see below) |
| `neoforge` | `neoforge/src/gametest/java/` | Integration | vanilla GameTest registry via `DeferredRegister` (see below) |

### Running tests

```bash
./gradlew :common:test                # Common business logic unit tests
./gradlew :fabric:test                # Fabric ServiceLoader and adapter unit tests
./gradlew :neoforge:test              # NeoForge ServiceLoader and adapter unit tests
./gradlew testCoverage                # Aggregate local JaCoCo coverage report
./gradlew :fabric:runGameTest         # Fabric integration game tests (deprecated/manual; see below)
./gradlew :neoforge:runGameTestServer # NeoForge integration game tests (see below)
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
8. **Write the test body once, in `common/src/gametest`.** GameTest logic is plain vanilla
   (`GameTestHelper`, block/entity APIs) with no Fabric or NeoForge imports, so it belongs in a
   `common/src/gametest/java/com/logistics/gametest/<domain>/<Name>GameTestBody.java` class as
   `public static void testFoo(GameTestHelper context)` methods — see "Fabric + NeoForge Game
   Tests" below for the full pattern and its one real exception (Fabric-specific capability APIs).
9. **A brand-new Body class needs a wrapper on each loader, or it silently never runs.** Neither
   loader discovers `common/src/gametest` classes on its own:
   - **Fabric** needs a thin `@GameTest`-annotated wrapper class in `fabric/src/gametest` that
     delegates to the Body, *and* a manual entry in
     `fabric/src/gametest/resources/fabric.mod.json`'s `entrypoints."fabric-gametest"` array — or
     Fabric's runner silently never discovers or runs it. No error, no log line, it just isn't in
     the "N GAME TESTS COMPLETE" count.
   - **NeoForge** needs a `<Name>GameTestRegistration` class in `neoforge/src/gametest` (see the
     pattern below), *and* a `.bootstrap()` call added to `LogisticsGameTestMod`'s constructor —
     same silent-miss failure mode as Fabric's `fabric.mod.json`.

   Only new *classes* need either registration step; new methods added to an already-registered
   Body/wrapper pair don't. Always confirm the count went up by the expected amount on **both**
   `:fabric:runGameTest` and `:neoforge:runGameTestServer` after adding a new test class, not just
   that the run stayed green — a registration miss and "test already passed by coincidence" look
   identical otherwise.
10. **Building a recipe/other cross-domain `ResourceId` from a domain's `resource()` helper silently
    prepends that domain's own prefix** (e.g. `LogisticsCore.resource("fabricator/redstone_chipset")`
    produces `logistics:core/fabricator/redstone_chipset`, not the intended
    `logistics:fabricator/redstone_chipset`) — a recipe's real id follows its file path under
    `data/logistics/recipe/<folder>/`, unrelated to any domain's resource-naming convention. Use
    `ResourceId.in(LogisticsMod.MOD_ID, path)` when constructing an id that isn't "this domain's own
    resource," and a test that silently draws 0 RF / never starts is a good sign the id is wrong.

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

#### Automation domain — done (every `LogisticsAutomation.BLOCK` machine now has feature-test coverage)

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
~~6. **Transposer verification**~~ — done, the cheapest pass yet: `TransposerGameTest` already
   covered fill/empty for lava, water, seed oil, and custom mod fluids, plus rejection cases
   (insufficient tank amount, fluid mismatch, blocked output, no energy) — essentially the entire
   wiki's Usage section already had a matching test. All four config numbers (20,000 RF, 128 RF/t,
   20 RF/t drain, 16,000 mB tank) and the crafting recipe matched exactly, no mismatch. Added
   wiki-quote traceability across the board, tightened one test to assert the exact 800 RF bucket
   cost, and added recipe-content checks for the water-bucket conversions and the crafting recipe.
~~7. **Refinery**~~ — done. The first machine in this pass with a genuinely new `RefineryGameTest`
   (previously only exercised as a passive fluid-tank host in `FluidSupplierGameTest`). All five
   config numbers, the crafting recipe, and both distillation recipes (Liquid Biomass → Bio Fuel,
   Crude Oil → Fuel Oil + 50% Tar) matched the wiki exactly — the generic component math was already
   proven in `RecipeProcessorComponentTest`, so this was wiring + wiki-claim extraction, not new
   component work, as predicted. Surfaced a real methodology gap, not a wiki one: a **new** GameTest
   class must be added to `fabric.mod.json`'s `fabric-gametest` entrypoint list or Fabric's runner
   silently skips it — no error, just missing from the test count. Documented above as methodology
   step 9 so it doesn't get missed on Crucible/Alloy Smelter/Fabricator.
~~8. **Crucible**~~ — done. Straightforward once Refinery's pattern existed: new `CrucibleGameTest`
   confirms the tank is genuinely output-only (wiki says so explicitly — pipes can drain it but not
   fill it directly) and a live ice-melting run asserts the exact 1,600 RF cost and 1,000 mB water
   output. All four config numbers, the crafting recipe, and two spot-checked recipes (ice → water,
   bitumen → crude oil) matched the wiki exactly. Registered the new class in `fabric.mod.json`
   from the start this time (185 → 189, confirmed) — no repeat of the Refinery gotcha.
   Note: the Crucible's actual byproduct/chance recipes (oil sand → crude oil family) weren't in
   scope here since the two spot-checked recipes don't have byproducts; a future pass could add one.
~~9. **Alloy Smelter**~~ — done. Confirmed the wiki's "order-independent" dual-input claim live (ore
   in slot A + flux in slot B, and the reverse, both start smelting) — the resolver's own Javadoc
   already documented this, and `AlloySmelterRecipeTest.matchesInEitherInputOrder()` already proved
   it at the pure-logic level, so the GameTest is belt-and-suspenders confirmation, not a new finding.
   Covered both documented recipe families with live runs: ore processing (iron ore + sand → 2 iron
   ingots, 4,000 RF) and alloying (3 copper ingot + 1 tin ingot → 4 bronze ingot, 4,000 RF, no
   byproduct). All three config numbers and three spot-checked recipes (crafting, iron ore, bronze)
   matched the wiki exactly, including a crafting-recipe ingredient that's a genuine *list*
   (Sand-or-Red-Sand) rather than a single item — the first recipe test needing that shape.
~~10. **Sequential Fabricator**~~ — done, and the "multi-step/multi-stage" framing turned out to be
    simpler in practice than it sounded: pick a chipset in the GUI, feed ingredients into a shared
    12-slot pool, it builds and ejects. All three config numbers and three spot-checked recipes
    (crafting, cheapest chipset, most expensive chipset) matched the wiki exactly. The real find:
    the wiki's Usage section reads as one chipset selected at a time ("*the* selected chipset"), but
    the machine actually supports queuing several chipsets and cycles through them round-robin,
    confirmed live with a test that queues two chipsets with only enough shared redstone for one of
    each — proving the machine switches to the other recipe after each completion rather than
    exhausting one first, which would strand the second recipe's own redstone requirement (see
    `WIKI_DISCREPANCIES.md` § Sequential Fabricator). Also hit a new
    mistake worth flagging for next time: constructing a recipe's `ResourceId` via a domain helper
    (`LogisticsCore.resource(...)`) silently prepends that domain's prefix (`core/`) — the recipe's
    real id follows its file path under `data/logistics/recipe/<folder>/`, not any domain
    convention. Use `ResourceId.in(LogisticsMod.MOD_ID, path)` for recipe/other cross-domain ids
    instead of a domain-scoped resource helper.

#### Loose ends (found along the way, not required for automation-domain completion)

11. **Quarry chunk-loading toggle** — surfaced while verifying the Quarry; needs a
    `ChunkLoadingComponent`-level or GameTest check that toggling `quarry_load_chunks` actually
    acquires/releases chunk tickets.
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
16. **Share the 13 `// not-yet-shared:` CableGameTest tests** — NeoForge runs none of them today, so
    the whole cable domain is Fabric-only in practice: topology (connects/doesn't connect, network
    splits and rejoins), tier capping, and engine interaction are all unverified there. Five touch no
    Fabric API and move as-is; the other eight use `Transaction`/`EnergyStorage` only to push and read
    energy, so they need the same rewrite `PipeFlowGameTest` already got — swap the Fabric calls for
    a real adjacent `CREATIVE_ENGINE` and plain block-entity reads. Only the two
    `testAbortedCableTransaction*` tests genuinely stay Fabric-only.

---

## What's Covered

`common/src/test/java/` contains JUnit tests for all testable business logic:

- **Pipe modules** — BoostModule, CraftingModule, EnchantmentSinkModule, ExtractionModule, BasicExtractorModule, AdvancedExtractorModule, InsertionModule, ItemFilterModule, MergerModule, ModSinkModule, PassiveSupplierModule, PipeMarkingModule, PolymorphicSinkModule, ProcessModule, QuickSortModule, RequesterModule, SatelliteModule, SinkModule, SupplierModule, TerminusModule, TransportModule, VoidModule, WeatheringModule, BlockConnectionModule (partial), PipeOnlyModule (partial)
- **Pipe network services** — CraftBatchingService, JobCoordinator, NetworkController, ReconciliationService, RequestPlanner, ReservationManager, SinkResolver
- **Failure accounting regressions** — tracked delivery failure, partial delivery followed by failed remainder, retry accounting, and job state after dispatch loss
- **Pipe network graph** — NetworkGraph, NetworkPathfinder
- **Pipe runtime** — TravelingItem, TravelingItemPhysics, RoutePlan
- **Automation** — GridScanner, FrameLayout, QuarryBounds, QuarryPhaseRunner, ActiveQuarryRegistry, QuarryBlockBreaker, LaserQuarryConfig (default mining area), KilnEnergyConfig (config defaults + RecipeProcessPlan smelt math), KilnRecipe (wiki-vs-shipped-JSON content check), FluidPumpConfig (tank/energy/push-rate config defaults), FluidPumpRecipe (wiki-vs-shipped-JSON content check), MaceratorConfig (power config defaults), MaceratorRecipeSpotCheck (wiki-vs-shipped-JSON content check), SawmillConfig (power config defaults), SawmillRecipeSpotCheck (wiki-vs-shipped-JSON content checks, incl. undocumented byproducts), TransposerConfig (power/tank config defaults), TransposerRecipeSpotCheck (crafting + bucket-conversion content checks), RefineryConfig (power/tank config defaults), RefineryRecipeSpotCheck (crafting + both distillation recipes, exhaustive), CrucibleConfig (power/tank config defaults), CrucibleRecipeSpotCheck (crafting + 2 melting recipes), AlloySmelterConfig (power config defaults), AlloySmelterRecipeSpotCheck (crafting + ore-processing + alloying recipes), SequentialFabricatorConfig (power config defaults), SequentialFabricatorRecipeSpotCheck (crafting + cheapest/most-expensive chipset recipes)
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

## Fabric + NeoForge Game Tests

`fabric/src/gametest/` and `neoforge/src/gametest/` run **191 shared feature tests** on each loader
(plus 16 unshared Fabric ones — see "Parity is enforced, not assumed" below), all requiring a full
Minecraft server process (real block placement, game ticks). Fabric's are considered
**deprecated** as a long-term matter — the goal is to replace them with plain JUnit equivalents as
code is restructured to separate pure logic from world dependencies — but they're kept to preserve
coverage. The primary blocker for conversion is that all tests depend on `GameTestHelper` to place
blocks in an actual level and tick the server; even "simple" placement tests require registered mod
blocks and a running level, neither of which is available in a vanilla bootstrap. This applies
equally to the NeoForge side; nothing below changes that long-term goal, only which loader(s) run
the tests in the meantime.

### Shared test bodies, per-loader registration glue

The actual test logic — assertions, block placement, tick-delayed checks — is written **once**, as
plain static methods in `common/src/gametest/java/com/logistics/gametest/<domain>/<Name>GameTestBody.java`.
These classes import only vanilla Minecraft and `com.logistics.*` types, no `net.fabricmc.*` or
`net.neoforged.*` — the same loader-agnostic rule as `common/src/main`, just for a source set that
isn't shipped in the jar (see the `gametest` source set in `common/build.gradle`, wired into both
loaders' own `gametest` compilation via `commonGametestJava`, the same mechanism `commonClientJava`
uses for `common/src/client`).

Each loader supplies only the wiring needed to run those bodies through its own GameTest mechanism:

- **Fabric** — a thin `<Name>GameTest` class in `fabric/src/gametest` with one `@GameTest`-annotated
  method per test, each a single-line delegating call to the Body class. Fabric API's
  `TestAnnotationLocator` discovers these by reflection through the `logistics-gametest` mod's
  `fabric-gametest` entrypoints (declared in `fabric/src/gametest/resources/fabric.mod.json`).
- **NeoForge** — a `<Name>GameTestRegistration` class in `neoforge/src/gametest` that registers each
  test as a named function in the vanilla `Registries.TEST_FUNCTION` registry (via
  `GameTestFunctions.TEST_FUNCTION`, a `DeferredRegister<Consumer<GameTestHelper>>`) and as a
  `FunctionGameTestInstance` referencing that function (via `RegisterGameTestsEvent`), using the
  shared boilerplate in `GameTestRegistrationSupport`. These run as their own `logistics_gametest`
  mod, present only on the `:neoforge:runGameTestServer` classpath, never in the shipped jar — see
  `LogisticsGameTestMod` for the wiring and "Adding a new test" below for the registration steps.

**Why NeoForge needs `DeferredRegister`, not vanilla's own test-loading hook:** MC replaced the old
`@GameTest`-annotated/reflection-scanned framework with a data-driven registry of
`GameTestInstance`s, each backed by a named function in `Registries.TEST_FUNCTION`. Vanilla's own
`TestFunctionLoader.registerLoader()` bootstraps that registry at `BuiltInRegistries` class-load
time — before FML loads a single mod class — so it's genuinely unreachable from mod code (an earlier
attempt at this integration used that hook and concluded NeoForge GameTests were blocked upstream).
The actual answer is that `Registries.TEST_FUNCTION` is a plain `BuiltInRegistries` entry like any
other (Blocks, Items, ...), so it goes through the same `DeferredRegister` + `RegisterEvent`
unfreeze mechanism NeoForge already uses for those — not vanilla's bootstrap-time loader.

### The one real exception: loader-native capability tests

A test that specifically exercises a loader's own capability/interop API — not this mod's code —
can't be shared, because the two loaders' APIs are genuinely different, not just differently named:

- `power/CableGameTest#testAbortedCableTransactionDoesNotReachCreativeSink` and
  `#testAbortedCableTransactionAfterTickDoesNotReachCreativeSink` are genuinely Fabric-only. Both open
  a Team Reborn `Transaction` (`Transaction.openOuter()`, an unclosed transaction implicitly aborting)
  to verify the cable's energy capability participates correctly in Fabric's transactional
  insert/rollback semantics. NeoForge's own energy capability
  (`IEnergyStorage.insert(amount, simulate)`) has no transaction/rollback concept to test — a
  `simulate=true` dry run is a different mechanism, not an equivalent.

  The **rest** of `CableGameTest` is not a real exception, only an unfinished port: 5 of its tests
  touch no Fabric API at all, and the other 8 use `Transaction`/`EnergyStorage` purely as test
  plumbing to move energy around — exactly the situation `PipeFlowGameTest` was already rewritten out
  of (next bullet). Those 13 carry a `// not-yet-shared:` marker and are counted by
  `checkFeatureTestParity`; the count may shrink but never grow. NeoForge currently runs none of
  them, which is a real coverage gap, not a documented boundary.
- `pipe/PipeFlowGameTest#testChestItemStorageReachable` stays inline in the Fabric wrapper (not
  delegated to a Body method, not registered on NeoForge): it specifically verifies Fabric API's own
  vanilla-chest-to-`ItemStorage` adapter, which has no NeoForge equivalent to test — every other
  method in that file only used the same Fabric API as incidental test-setup plumbing (filling or
  reading a chest) and was rewritten to use plain vanilla `ChestBlockEntity` access instead, which
  behaves identically on both loaders and is now fully shared.
- `pipe/GlassTankBucketGameTestBody#emptyBucketDrainsGlassTankInCreative` is a partial case: the
  shared body covers everything genuinely loader-agnostic (tank drains, held item unaffected) and
  returns the mock player instead of calling `succeed()`; Fabric's wrapper adds one more assertion
  on top (a water bucket lands elsewhere in the creative inventory) because that's Fabric API's own
  `FluidStorageUtil` nicety, not something NeoForge's `FluidUtil.interactWithFluidHandler`
  replicates or something this mod's `GlassTankBlock` promises itself.

When you hit a test like this, don't force a shared body that only compiles on one loader (or
silently duplicates the loader-native call under two different names) — leave the loader-specific
assertion where it belongs and document why, the way the cases above do.

### Parity is enforced, not assumed

`./gradlew checkFeatureTestParity` (wired into `:common:lint`, so it runs in the `lint (common)` CI
job) builds the catalog of shared tests — every `public static` method taking a `GameTestHelper` in
`common/src/gametest` — and fails if either loader doesn't wire one up. It also fails on a reference
to a body method that doesn't exist, and on an unshared Fabric test with no justification marker.

Matching *counts* are not parity and the task never checks them: two loaders can each run 191 tests
while running different sets. Comparing the catalog against each loader's wiring is what actually
proves it.

For reference, the current split is 191 shared tests, plus 3 `// loader-only:` and 13
`// not-yet-shared:` Fabric tests. The runs report 208 on Fabric and 192 on NeoForge; the totals
include a built-in instance from the test framework itself, so read the *difference* rather than
either number — 16, exactly the unshared Fabric set, and expected rather than a defect.

Every unshared Fabric test needs one of two markers directly above its `@GameTest` annotation:

- `// loader-only: <reason>` — genuinely tests a loader-native API with no counterpart. Permanent.
- `// not-yet-shared: <reason>` — should live in a shared body eventually. Tracked, and the task
  ratchets the total down: adding one fails the build.

### Per-test reports

Both loaders write a JUnit-style XML report of every test, uploaded as a CI artifact on the
`feature-test` jobs:

- Fabric — `fabric/build/test-results/gameTest/report.xml` (via `fabric-api.gametest.report-file`)
- NeoForge — `neoforge/build/test-results/gameTestServer/report.xml` (via vanilla's `--report`)

Useful when a run fails, since the "N GAME TESTS COMPLETE" line alone doesn't say which test broke.
The reports also record execution order, which is how the reload question below was settled.

Both frameworks can also run a subset, which is worth knowing when iterating on one test:

- Fabric — `-Dfabric-api.gametest.filter=<name>`
- NeoForge — `--tests <namespaced selector>` (supports wildcards)

### A datapack reload does not need an isolated lane

Triggering `MinecraftServer#reloadResources` inside a normal run looks like it should disturb the
rest of the suite — it swaps recipes, loot tables, and tags globally, mid-run. Measured on both
loaders, it does not.

A probe that reloaded all selected packs mid-suite ran at position 2 of 209 on Fabric (207 tests
after it) and position 73 of 193 on NeoForge (120 tests after it). Every subsequent test passed on
both, including the recipe- and machine-dependent ones — kiln, macerator, pipe, engine, and quarry
tests all ran after the reload and were unaffected.

So a reload test can be an ordinary shared feature test. It does not need a separate run
configuration, a filtered invocation, or a manually triggered lane. If that ever changes, the
per-test reports above are how you would notice: a reload-order problem shows up as failures
clustered after the reload test rather than spread through the run.

### Adding a new test

1. Write (or extend) the `<Name>GameTestBody` class in `common/src/gametest`.
2. Add/update the Fabric `<Name>GameTest` wrapper. New wrapper *class* → add it to
   `fabric/src/gametest/resources/fabric.mod.json`.
3. Add/update the NeoForge `<Name>GameTestRegistration` class, following an existing one as a
   template. A new test *method* on an already-registered class needs a new `GameTestCase` entry
   added to that class's `TESTS` list (passed to `GameTestRegistrationSupport.registerFunctions`) —
   give it a namespace-unique path (`"<domain>/<slug>"`); a collision throws at runtime because
   every domain's `@SubscribeEvent` handler shares the same `RegisterGameTestsEvent`/registry. A new
   registration *class* additionally needs its own namespace-unique environment id
   (`registerInstances(event, "<domain>/<slug>", ...)`) and a `.bootstrap()` call added to
   `LogisticsGameTestMod`'s constructor.
4. Run `./gradlew checkFeatureTestParity` — it catches a missing wrapper or registration instantly,
   without booting a server, and is the check that would otherwise only surface as a silently absent
   test.
5. Run both `./gradlew :fabric:runGameTest` and `./gradlew :neoforge:runGameTestServer` and confirm
   the "N GAME TESTS COMPLETE" count went up by the expected amount on each.

A NeoForge timed test (`runAfterDelay`, a tight `maxTicks`) generally needs a slightly larger
`maxTicks` budget than its Fabric counterpart — NeoForge's `GameTestInstance` ticks the
environment/structure differently before handing control to the test body. There's no fixed
conversion factor; if a new timed test times out on NeoForge but not Fabric, that's why — give it
more headroom (the existing registrations add roughly 20 ticks as a starting point) and re-run.
