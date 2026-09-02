# Testing Guide

## Vocabulary

The suite is layered, and each layer has a name used consistently in task names, CI job labels, and
this document. Use these terms in PR titles and discussion:

| Term | What it means | Where it lives | How it runs |
|------|---------------|----------------|-------------|
| **Unit test** | Pure logic, no live world — state transitions, codecs, planners, config math, single-file JSON decoding | `common/src/test`, and the loader modules' `src/test` | JUnit 5 |
| **Feature test** | A real Minecraft server verifies player-visible behavior | body in `common/src/gametest`, wired per loader | GameTest |
| **Loader contract test** | A Fabric/NeoForge adapter fulfills a common contract | `fabric/src/test`, `neoforge/src/test` | JUnit 5 |
| **Resource contract test** | Every shipped JSON/asset reference resolves | `common/src/test/java/com/logistics/resource/contract` | JUnit 5 (static, no server) |
| **Client feature test** | A real client verifies screens, models, rendering | not yet built — Fabric-only when it lands | client GameTest |
| **Journey** | A small end-to-end player workflow | not yet built | real driven client |

"Feature test" and "GameTest" refer to the same thing: GameTest is the Minecraft framework, feature
test is what we call the layer. Class files keep the `GameTest` suffix (`<Name>GameTestBody`,
`<Name>GameTest`, `<Name>GameTestRegistration`) — renaming ~95 files across five `mc/*` branches would
cost far more in cherry-pick conflicts than the naming consistency is worth.

## Test Structure

| Module | Location | Type | Framework |
|--------|----------|------|-----------|
| `common` | `common/src/test/java/` | Unit | JUnit 5 + Minecraft bootstrap |
| `fabric` | `fabric/src/test/java/` | Loader contract | JUnit 5 (plain) |
| `neoforge` | `neoforge/src/test/java/` | Loader contract | JUnit 5 (plain) |
| `common` | `common/src/gametest/java/` | Feature (shared body) | plain `Consumer<GameTestHelper>` methods, no loader imports |
| `fabric` | `fabric/src/gametest/java/` | Feature | Fabric `@GameTest` (deprecated/manual; see below) |
| `neoforge` | `neoforge/src/gametest/java/` | Feature | vanilla GameTest registry via `DeferredRegister` (see below) |

### Running tests

```bash
./gradlew unitTest                    # All module unit tests
./gradlew featureTest                 # Feature tests on both loaders
./gradlew featureTestFabric           # Fabric feature tests only
./gradlew featureTestNeoForge         # NeoForge feature tests only
./gradlew clientFeatureTestFabric     # Fabric client feature tests (opens a real client)
./gradlew testCoverage                # Aggregate local JaCoCo coverage report
```

Those aliases delegate to the underlying tasks, which can still be called directly to scope a run to
one module:

```bash
./gradlew :common:test                # Common business logic unit tests
./gradlew :fabric:test                # Fabric ServiceLoader and adapter unit tests
./gradlew :neoforge:test              # NeoForge ServiceLoader and adapter unit tests
./gradlew :fabric:runGameTest         # Fabric feature tests (deprecated/manual; see below)
./gradlew :neoforge:runGameTestServer # NeoForge feature tests (see below)
```

CI deliberately calls the per-module tasks rather than the aggregate aliases: its matrix runs one job
per module so the modules test in parallel and each uploads its own coverage report. The aliases are a
local convenience, not the CI entry point.

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
  - `unit-test (common|fabric|neoforge)` runs module unit tests and uploads that
    module's JaCoCo coverage to Codecov (tagged with a per-module flag). Codecov
    merges the three uploads into the combined coverage for the commit.
  - `feature-test (fabric|neoforge)` runs each loader's GameTests on a real server.

The workflow's internal job *keys* are still `test` and `gametest`; only the displayed names use the
`unit-test`/`feature-test` vocabulary. The keys are left alone on purpose — renaming them risks
breaking branch protection's required-check contexts and any downstream tooling that references them.

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
`RecipeLoadingGameTestBody` is the mod-wide counterpart, and it is a *feature* test rather than a
unit test: it walks the live datapack stack via `FileToIdConverter` and asserts every `logistics:`
recipe file actually landed in the server's `RecipeManager`. Loading through the real manager is the
point — a recipe that parses as JSON but fails to deserialize (unknown type, malformed ingredient) is
silently absent at runtime, and only a real load catches that. It walks the datapack stack rather than
the raw classpath so it behaves identically regardless of how each loader's dev environment lays out
mod resources on disk.

It stops at "did it load," not "is the content right": it does not resolve ingredient item ids through
Minecraft's registry, because vanilla silently defaults an unknown id to `minecraft:air` instead of
failing, so that check wouldn't reliably catch an item-id typo anyway. Verifying registry-backed ids
belongs in a live server-data contract; verifying a specific recipe's content belongs in a
`*RecipeTest`/`*RecipeSpotCheckTest` unit test.

### Resource contract testing

`common/src/test/java/com/logistics/resource/contract/` proves the shipped asset graph holds
together — model parents resolve, no parent chain loops, every texture a model names ships, every item
definition and blockstate points at a model that exists, loot tables are structurally sound, and tags
contain no tag-of-tag cycles. It also checks the graph against the registries, so a registered block or
item cannot ship with no resource at all. It is plain JUnit that reads JSON off the classpath and boots
only Minecraft's registries, never a server, so it runs on every `./gradlew :common:test` at negligible
cost.

**Namespace policy** (`ResourceFiles`), applied to every reference:

- `logistics:` — must resolve to a shipped file. Failing this is a hard error; it is the suite's job.
- `minecraft:` (and an unqualified reference, which Minecraft itself reads as vanilla) — trusted and
  not resolved. A classpath-only validator cannot prove a vanilla asset exists without a
  version-matched index of Minecraft's own resources, and a hand-maintained substitute would rot
  silently. Tightening this later means adding a real index, not an allowlist.
- Anything else — rejected, so a stray third-party reference can't slip in unnoticed. Add the
  namespace to `TRUSTED_NAMESPACES` with a reason if it's ever intentional.

`ALLOWED_UNRESOLVED` is the escape hatch for a reference that deliberately doesn't ship a file
(dynamic, generated, or loader-supplied). It is empty today, and it should stay small — an explicit
entry shows up in review, whereas loosening the validator after the first false positive does not.

Item definitions nest models in several shapes (`minecraft:model`, `composite`, `select` with
fallback/cases, `special` with its model under `base`). The tests walk the whole JSON tree and treat
any string-valued `model` or `base` as a reference rather than encoding each type's schema, so a new
definition type is covered without a code change.

**Registry coverage.** Following references from a file can only find broken links between files that
already exist. It cannot see a block or item that was registered and never given a resource at all —
which is how a registered thing ends up rendering as the missing-texture checkerboard in a world.
`RegistryCoverageContractTest` closes that by reading the real registries: `DomainRegistrations` drives
each domain's package-private `BLOCK`/`ITEM`/`BUCKET` `register()`, which write straight into
`BuiltInRegistries`. Block entities, menus, and creative tabs are skipped — they resolve loader
services that have no implementation on the common test classpath.

It checks both directions, and the reverse direction is what keeps the test honest: if
`DomainRegistrations` ever stops driving part of the registration, the resources for the missing part
surface as orphans instead of the forward checks quietly covering less. Both exceptions are derived,
not listed by hand:

- A resource may live in common **or** in every loader module. No resource uses the second case today
  — every asset lives in common — but the rule stays, because requiring *every* loader means adding a
  loader-specific resource to one loader and forgetting the other fails here, rather than silently
  shipping a block with no model on that loader. The three cable blockstates were the last example,
  moved to common once NeoForge stopped needing its own.
- Placeable fluids (`FluidDef.placeable()`) get their `LiquidBlock` and bucket registered per loader,
  so common ships the resources and registers neither. Read from `CUSTOM_FLUIDS`, so adding a placeable
  fluid needs no test edit.

Chained with the reference checks above, this covers registry → definition → model → texture end to
end. It found the Seed Oil Bucket shipping with no model at all.

**Required textures, not just declared ones.** Following declared references cannot see a texture a
model *needs* and never supplies — the gap that shipped 17 models with no `particle`, rendering their
break particles as the missing-texture checkerboard. Two checks close it, over the models a blockstate
or item definition actually names:

- every `#variable` the chain's faces use is defined somewhere in that chain, including a `textures`
  entry that is itself a variable (our tank base sets `"particle": "#side"` and lets each child supply
  `side`, so checking only that the `particle` key exists would wave through an alias to nothing);
- every model with geometry supplies `particle` — the one texture reached through no `#variable` at
  all, so nothing else can see it missing.

**Templates are excluded on purpose.** A model that exists only to be inherited from legitimately
leaves variables to its children. `block/tank/tank.json` is the example: one Glass Tank block, but its
blockstate picks between `glass_tank` and `glass_tank_stacked` on `joined_below`, and those two differ
only in the `side` texture — so the shared geometry lives in a base neither renders directly. Requiring
every file on disk to be self-sufficient would report those as failures and invite weakening the check.

Validated against the real failure rather than by construction: run over the tree before the fix, the
particle check flags exactly the 17 models Minecraft warned about and no others.

**What this deliberately does not cover:** the `hasSizeGreaterThanOrEqualTo` checks are a deletion
alarm for walking an empty or wrong directory — they are not a coverage measure, and shouldn't be
described as one. Parent chains stop at the first model we do not ship, so a variable that only a
vanilla ancestor supplies is out of reach, for the same reason the namespace policy trusts
`minecraft:` references without resolving them. Registry-backed id checks (unknown items in loot tables or tags) belong in a live
feature test, not here — vanilla's registry silently resolves an unknown id to `minecraft:air` rather
than failing, so a static check can't catch that class of typo.

The built-in `resourcepacks/classic_crafting` pack is also outside the walk. It is a nested pack root
rather than part of the merged `assets/`+`data/` tree, and it is deprecated and slated for removal, so
nothing under it is validated.

### Server-data loading contract

`ServerDataLoadingGameTestBody` is the live counterpart to the static resource contract tests, and it
exists because those two layers catch genuinely different bugs. A loot table can be valid JSON with a
perfectly well-formed structure and still fail to deserialize — name an entry type that doesn't
exist and the codec rejects it, the table never reaches the registry, and the block silently drops
nothing when broken. Only a running server reveals that.

It covers **server** data: loot tables, worldgen (configured and placed features), and the
registry-to-data direction — every block we register that declares a loot table has that table
actually loaded. That last check is the one file-driven validation structurally cannot do: a block
pointing at a table that was never shipped is invisible if you only inspect the files that do exist.
Recipes have their own body (`RecipeLoadingGameTestBody`).

Models, item definitions, and blockstates are deliberately **out of scope** here — they are client
resources with no server-side registry to walk. Their structure is checked statically in
`com.logistics.resource.contract`, and their actual loading belongs to client feature tests.

Two conventions worth keeping when extending it:

- **Read loaded registries, not JSON.** The point is to verify what the game ended up with, not to
  restate what the files say. Don't scan raw JSON for id-shaped strings; where an id needs checking,
  use `Registry#containsKey` rather than `get`, since a defaulted registry answers an unknown item id
  with `minecraft:air` instead of failing.
- **Guard against vacuous passes.** A live enumeration that matches nothing passes without inspecting
  anything, which is the one way these tests can be silently worthless. Each check asserts it examined
  a plausible number of entries and fails loudly if not — that floor is a tripwire for a broken
  enumeration, not a coverage measure.

Tags are covered by `allLogisticsItemTagEntriesLoad` and `allLogisticsBlockTagEntriesLoad`: every
`logistics:` id a tag file lists must be in the tag the game actually loaded.

The obstacle was that our tag files live under the `c:` and `minecraft:` namespaces rather than our
own, so there is no file-owner mapping to key off. The way around it is to key off our **entries**
instead of our files — a `logistics:` id in any tag is ours by construction, whoever owns the file.

Two details keep it honest:

- It reads the whole resource **stack**, not the winning resource. We add entries to six `minecraft:`
  tag files vanilla also ships; reading only the top resource would read vanilla's copy and silently
  examine nothing for those tags.
- Floors of 18 item tags and 10 block tags, for the same vacuous-pass reason as the other checks here.

What this catches is worse than a single bad entry: one unresolvable id makes Minecraft discard the
**whole tag** (`Couldn't load tag … as it is missing following references`), and it only logs an error
rather than failing startup. Verified by mutation — a single typo in `minecraft:mineable/pickaxe`
removed all 25 of our blocks from it, so every machine in that tag stopped being pickaxe-mineable while
the game ran on regardless.

Biome tags are out of scope: they belong to a dynamic registry rather than `BuiltInRegistries`, and the
one we ship lists no `logistics:` entries.

### Persistence reconstruction

Two tests save a block entity's NBT, replace the block (producing a fresh block entity), and load that
NBT back into it:

- `PipeFlowGameTestBody#testTravelingItemSurvivesPipeReconstruction`
- `CableGameTestBody#testCableNetworkSurvivesCableReconstruction`

**The name is deliberate — this is not a chunk unload.** They cover the block entity's own
`saveCustomOnly`/`loadCustomOnly` round-trip, but they reach it by a different route than an unload
does. `setBlock(AIR)` fires `PipeBlockEntity#preRemoveSideEffects`, which drops in-transit items as
entities and detaches the pipe from its network — and that method is explicitly *not* called on chunk
unload. So the pipe test leaves a stray diamond on the floor that a real unload would never produce
(assert on the chest, never on a world-wide item count), and level unload events and chunk tickets are
not involved at all. A genuine unload/reload test is separate work; calling this one "chunk unload"
would claim coverage that does not exist.

**The two are not equally strong, and the difference is the point.** A pipe carries items in transit
that nothing but NBT can restore — so the pipe test fails if the load is skipped, and the assertion
that the replaced pipe is empty *before* loading is what makes the restored item unambiguous evidence.

A cable persists only its connection mask, and that mask is *derivable from its neighbours*:
`getRenderConnectionMask()` rebuilds it whenever the cache is dirty, so a freshly placed cable reaches
the same value with or without the load. Deleting its `loadCustomOnly` call leaves the cable test
passing — verified by mutation, not assumed. Asserting on the mask would not fix this, for the same
reason.

So the cable test covers `CableNetworkManager` recovery — `registeredInNetwork` is transient, so the
rebuilt block entity must re-register before the network carries energy through that position again —
and not persistence. It is kept for that, and its javadoc says so, because a test named for something
it does not check is worse than no test.

For the record on what the mask is *not*: it drives rendering and the collision shape only. Cable
topology comes from `CableNetwork.buildFrom` testing for a `CableBlockEntity`, and consumers are found
through `EnergyCapabilityLookup` — neither consults it.

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
~~16. **Share the CableGameTest tests**~~ — done. All 13 portable tests now run on both loaders,
    including network split/rejoin and mixed-tier capping. The `Transaction`-based ones were
    rewritten against `IEnergyStorage`'s simulate-boolean API rather than moved verbatim. Only the
    two `testAbortedCableTransaction*` tests stay Fabric-only, plus the grid-visibility half of
    `testCableDoesNotPowerExtractionPipe` — its portable half is shared.

---

## What's Covered

`common/src/test/java/` contains JUnit tests for all testable business logic:

- **Pipe modules** — BoostModule, CraftingModule, EnchantmentSinkModule, ExtractionModule, BasicExtractorModule, AdvancedExtractorModule, InsertionModule, ItemFilterModule, MergerModule, ModSinkModule, PassiveSupplierModule, PipeMarkingModule, PolymorphicSinkModule, ProcessModule, QuickSortModule, RequesterModule, SatelliteModule, SinkModule, SupplierModule, TerminusModule, TransportModule, VoidModule, WeatheringModule, BlockConnectionModule (partial), PipeOnlyModule (partial)
- **Pipe network services** — CraftBatchingService, JobCoordinator, NetworkController, ReconciliationService, RequestPlanner, ReservationManager, SinkResolver
- **Failure accounting regressions** — tracked delivery failure, partial delivery followed by failed remainder, retry accounting, and job state after dispatch loss
- **Pipe network graph** — NetworkGraph, NetworkPathfinder
- **Pipe runtime** — TravelingItem, TravelingItemPhysics, RoutePlan
- **Automation** — GridScanner, FrameLayout, QuarryBounds, QuarryPhaseRunner, LaserQuarryFrameBlock (frame lifecycle contract), QuarryBlockBreaker, LaserQuarryConfig (default mining area), KilnEnergyConfig (config defaults + RecipeProcessPlan smelt math), KilnRecipe (wiki-vs-shipped-JSON content check), FluidPumpConfig (tank/energy/push-rate config defaults), FluidPumpRecipe (wiki-vs-shipped-JSON content check), MaceratorConfig (power config defaults), MaceratorRecipeSpotCheck (wiki-vs-shipped-JSON content check), SawmillConfig (power config defaults), SawmillRecipeSpotCheck (wiki-vs-shipped-JSON content checks, incl. undocumented byproducts), TransposerConfig (power/tank config defaults), TransposerRecipeSpotCheck (crafting + bucket-conversion content checks), RefineryConfig (power/tank config defaults), RefineryRecipeSpotCheck (crafting + both distillation recipes, exhaustive), CrucibleConfig (power/tank config defaults), CrucibleRecipeSpotCheck (crafting + 2 melting recipes), AlloySmelterConfig (power config defaults), AlloySmelterRecipeSpotCheck (crafting + ore-processing + alloying recipes), SequentialFabricatorConfig (power config defaults), SequentialFabricatorRecipeSpotCheck (crafting + cheapest/most-expensive chipset recipes)
- **Power** — CableTier, PIDController, EngineHeatModel, EngineCyclePlanner, StirlingGenerationPlanner, StirlingFuelState, CreativeOutputLevels, RedstoneTargetGate, CreativeSinkDrainState
- **Core** — BaseBlockEntity, ResourceId, MaceratorRecipe, MaceratorBlockEntityLogic, FluidTankComponent, ItemInventoryComponent, RecipeProcessPlan (shared RF-cost math backing Kiln/Macerator/etc.)
- **Serialization golden tests** — ItemFilterModule (backward compat), ProviderDispatchQueue, TravelingItem
- **Resource contracts** — model parent chains (resolution + loop detection), model texture references, item definition model references; see "Resource contract testing" above

Recipe loading is covered by a feature test rather than a unit test — `RecipeLoadingGameTestBody`
asserts every recipe file under `data/logistics/recipe/**` (~628 files, 13 domains) actually loads
into a live `RecipeManager`; see "Recipe testing" above.

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

`fabric/src/gametest/` and `neoforge/src/gametest/` run **210 shared feature tests** on each loader
(plus 3 unshared Fabric ones — see "Parity is enforced, not assumed" below), all requiring a full
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

  Everything else in `CableGameTest` now lives in `CableGameTestBody` and runs on both loaders. The
  tests that used `Transaction` only as plumbing were rewritten against `IEnergyStorage`'s
  simulate-boolean API, which is loader-agnostic by design — the same treatment `PipeFlowGameTest`
  got (next bullet). The `not-yet-shared` backlog is empty and the ratchet now holds it at zero.
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

It separately checks that each test class is actually **discovered at runtime**, which is not the
same as being wired. A class can be correctly written and correctly registered in source and still
never run, because the step that makes the runtime aware of it is missing — and that failure is
silent on both loaders: no error, no log line, the test simply isn't in the count. The three
discovery paths are:

| Class | Must appear in |
|---|---|
| NeoForge `*GameTestRegistration` | a `bootstrap()` call in `LogisticsGameTestMod` |
| Fabric `@GameTest` wrapper | `fabric.mod.json`'s `fabric-gametest` entrypoints |
| Fabric `FabricClientGameTest` | `fabric.mod.json`'s `fabric-client-gametest` entrypoints |

This gap is not hypothetical: a merge once dropped `ServerDataLoadingGameTestRegistration.bootstrap()`
and nothing caught it, because the `GameTestCase` entries it registers were still present in source —
only the initialization call was gone.

Matching *counts* are not parity and the task never checks them: two loaders can each run 191 tests
while running different sets. Comparing the catalog against each loader's wiring is what actually
proves it.

For reference, the current split is 210 shared tests, plus 3 `// loader-only:` Fabric tests and no
`// not-yet-shared:` backlog. The runs report 214 on Fabric and 211 on NeoForge; the totals
include a built-in instance from the test framework itself, so read the *difference* rather than
either number — 3, exactly the unshared Fabric set, and expected rather than a defect.

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

`ReloadLifecycleGameTestBody` holds the resulting contract: a smelt already in flight when the
datapack reloads finishes exactly once for exactly its normal cost, and the kiln still resolves
recipes afterwards. The energy assertion is what makes the first one meaningful — a run that
restarted would still finish, just later and after spending more, so asserting only on the output
item would let a silent progress reset through.

Both tests confirm the reload actually replaced the server's recipe manager before asserting
anything. Without that they would pass just as happily against a reload that did nothing, which is
the one way they could be worthless.

These are regression pins rather than bug-finders: `SmeltingRecipeResolver` fetches the recipe
manager fresh on each resolve and `RecipeProcessorComponent` compares the resolved plan by value
rather than identity, so today's code is reload-safe by construction. The tests exist so that a
change to either of those — an innocent-looking switch to identity comparison, say, or caching the
manager — fails here instead of quietly charging players twice for one smelt.

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

---

## Client Feature Tests

Run with `./gradlew clientFeatureTestFabric` (or `:fabric:runClientGameTest` directly). This starts a
**real Minecraft client** with the mod loaded, builds a world, renders it, and can capture
screenshots — the layer that covers screens, models, and rendering, none of which a headless server
can see.

**Fabric only.** NeoForge has no equivalent harness here, and unlike server feature tests there is
nothing to share: these are not portable test bodies, so `common/src/gametest` is not involved.

### A different framework from the server tests

Despite the similar name, this is not vanilla GameTest. It is Fabric API's client test framework
(`fabric-client-gametest-api-v1`), and it works differently:

| | Server feature tests | Client feature tests |
|---|---|---|
| Framework | vanilla GameTest | Fabric API client test |
| Unit of a test | one `@GameTest` method | one class, run as a script |
| Discovery | `fabric-gametest` entrypoint | `fabric-client-gametest` entrypoint |
| Shared across loaders | yes, via `common/src/gametest` | no |

Because a client test is a whole class rather than annotated methods, `checkFeatureTestParity` — which
scans `@GameTest` methods — does not see them. They neither satisfy nor violate it.

No Gradle wiring was needed: loom's `enableClientGameTests` defaults to true, so `runClientGameTest`
already existed, and `fabric-client-gametest-api-v1` is already on the gametest classpath via the
existing `fabric-api` dependency. The one change required was `fabric.mod.json`'s `environment`,
which was `"server"` and kept the test mod off the client entirely; it is now `"*"`.

### Determinism contract

A screenshot that varies run to run is worse than no screenshot. Fix all of these before adding a
capture — most are one call, and the framework provides them:

- `context.restoreDefaultGameOptions()` — GUI scale, render distance, graphics options. Without it a
  developer's local settings change what gets captured.
- `worldBuilder().setUseConsistentSettings(true)` — fixed world settings and seed. Use
  `adjustSettings(...)` for per-test time of day, weather, or world type.
- `getClientLevel().waitForChunksRender()` — capturing earlier yields a partly-empty frame.
- `TestScreenshotOptions.of(name).disableCounterPrefix()` — stable filename. The default prefixes an
  incrementing counter, which makes captures impossible to compare between builds.
- Fixed camera position and angle, once a test actually looks at something specific — and put the
  player in **spectator** before teleporting. A survival player teleported above ground falls and
  settles a fraction of a block differently each run, shifting the whole frame by a sub-pixel. That
  one detail moved the showcase capture from 0.00996 (twice the default tolerance, i.e. unusable as
  a gate) to 0.00012, comfortably inside it. The symptom is distinctive: the sky compares
  pixel-identical while nearly every ground pixel differs slightly.

Loom also clears the run directory before each run (`deleteGameTestRunDir`), so stale state doesn't
leak between runs.

### Never compare screenshots byte-for-byte

Two runs of the same test on the same machine do **not** produce identical PNGs. Measured on this
repo: same 854x480 dimensions, but different file sizes, and **33% of channel samples differed**.
The differences are sub-unit GPU noise spread across the whole frame — a normalised mean squared
difference of **0.000006**.

So `assertScreenshotEquals` is safe but `TestScreenshotComparisonAlgorithm.exact()` is not: it would
fail on every run. The default algorithm is `meanSquaredDifference(0.005)`, roughly 800x the observed
noise floor, which is why comparison works without tuning. Use `withRegion(...)` to narrow a
comparison to the part of the frame under test, and `withGrayscale()` when colour is not the point.

Until a baseline corpus exists, prefer treating captures as **reviewed CI artifacts** rather than a
pass/fail gate — upload them and look at them. A visual-diff gate is a deliberate later step, not the
default.

### What runs today

- `ClientBootstrapGameTest` — starts a client, builds a world, renders it, captures a frame. Asserts
  nothing about our own content; it exists to fail loudly if the harness itself breaks, which is
  otherwise only discovered when someone tries to add a real client test.
- `ShowcaseClientGameTest` — renders the block types whose drawing differs from a plain cube, and
  opens a real machine screen.

The showcase covers what the static resource contract structurally cannot. That suite proves a model
file *resolves*; only a client proves the model actually draws. Specifically:

| Subject | Why it's in the showcase |
|---|---|
| Copper transport pipes | Multipart blockstate — the model is chosen from neighbours, so they are placed in a connected run rather than as a lone stub |
| Copper cable | Blockstate ships per loader rather than from `common`, and the geometry comes from a dynamic renderer (its base model has empty `elements`) |
| Kiln | Machine with a block entity renderer, and the screen subject below |
| Glass tank | Fluid container rendering |
| Kiln screen | Opened through the server's real menu path, so the menu, its synced data, and the screen layout are all exercised rather than hand-constructed |

The screen test uses `waitForScreen`, so it fails if the screen never opens rather than quietly
capturing the world behind it.

The HUD is toggled off for the block capture via the real F1 binding: health, hunger, and hotbar
state have nothing to do with block rendering but would still show up as differences between
captures. (There is no longer a `hideGui` field to set directly.)

### Cost and CI

A world-creating capture test runs in about 15 seconds locally on a warm cache. CI needs a display —
the run config inherits from `client` — so the `client-test (fabric)` job installs `xvfb` and
runs through `xvfb-run`.

That job is gated on a `client_changed` path filter: client sources, shipped assets, or the client
test sources themselves. A change that a client test could not possibly observe doesn't pay for a
client boot. Screenshots upload on success as well as failure, because they are review artifacts
rather than failure diagnostics.
