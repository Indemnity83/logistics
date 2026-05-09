# Testing Guide

## Test Structure

| Module | Location | Type | Framework |
|--------|----------|------|-----------|
| `common` | `common/src/test/java/` | Unit | JUnit 5 + Minecraft bootstrap |
| `fabric` | `fabric/src/test/java/` | Unit | JUnit 5 (plain) |
| `neoforge` | `neoforge/src/test/java/` | Unit (stubs) | JUnit 5 (all @Disabled) |
| `fabric` | `fabric/src/gametest/java/` | Integration | Fabric @GameTest (deprecated) |

### Running tests

```bash
./gradlew :common:test          # Unit tests for all business logic
./gradlew :fabric:test          # ServiceLoader smoke tests
./gradlew :neoforge:test        # Stubs (0 active tests)
./gradlew :fabric:runGametest   # Integration game tests (deprecated; see below)
```

---

## What's Covered

`common/src/test/java/` contains JUnit tests for all testable business logic:

- **Pipe modules** — BoostModule, CraftingModule, EnchantmentSinkModule, ExtractionModule, BasicExtractorModule, AdvancedExtractorModule, InsertionModule, ItemFilterModule, MergerModule, ModSinkModule, PassiveSupplierModule, PipeMarkingModule, PolymorphicSinkModule, ProcessModule, QuickSortModule, RequesterModule, SatelliteModule, SinkModule, SupplierModule, TerminusModule, TransportModule, VoidModule, WeatheringModule, BlockConnectionModule (partial), PipeOnlyModule (partial)
- **Pipe network services** — CraftBatchingService, JobCoordinator, NetworkController, ReconciliationService, RequestPlanner, ReservationManager, SinkResolver
- **Pipe network graph** — NetworkGraph, NetworkPathfinder
- **Pipe runtime** — TravelingItem, TravelingItemPhysics, RoutePlan
- **Power** — CableTier, PIDController
- **Core** — BaseBlockEntity, ResourceId, MaceratorRecipe, MaceratorBlockEntityLogic
- **Serialization golden tests** — ItemFilterModule (backward compat), ProviderDispatchQueue, TravelingItem

`fabric/src/test/java/` contains ServiceLoader smoke tests verifying all six `META-INF/services/` registrations are present and the implementation classes can be instantiated.

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
| `FabricEnergyStorage` / `TrToIEnergyStorageAdapter` | Wrap Team Reborn Energy interfaces; require running TR Energy environment |

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
| `KilnGameTest` | 8 | Inventory slot access control requires live block entity |
