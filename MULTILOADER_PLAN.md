# Multi-Loader Support: Fabric + NeoForge

## Overview

Refactor the Logistics mod to support both **Fabric** and **NeoForge** using a manual service abstraction pattern (Java ServiceLoader). This keeps JARs clean with no runtime dependencies while giving full control over platform abstractions.

**Target**: Minecraft 1.21.11 (same as current Fabric version)

## Project Structure

```
logistics/
├── build.gradle                    # Root build orchestration
├── settings.gradle                 # Multi-project config
├── gradle.properties               # Shared versions
├── common/
│   ├── build.gradle
│   └── src/main/java/com/logistics/
│       ├── LogisticsCommon.java           # Shared init logic (no entry point)
│       ├── LogisticsDataComponents.java   # Unchanged (vanilla only)
│       ├── block/
│       ├── item/
│       ├── pipe/                          # All pipe logic (modules, runtime, etc.)
│       ├── platform/                      # NEW: Service interfaces
│       │   ├── Services.java              # Service loader entry point
│       │   └── services/
│       │       ├── IItemStorageService.java
│       │       ├── IRegistrationHelper.java
│       │       ├── IBlockEntityHelper.java
│       │       ├── IItemGroupHelper.java
│       │       └── IEventHelper.java
│       └── util/
├── fabric/
│   ├── build.gradle                       # fabric-loom setup
│   └── src/
│       ├── main/java/com/logistics/fabric/
│       │   ├── LogisticsMod.java          # ModInitializer
│       │   └── platform/                  # Fabric service implementations
│       ├── client/java/com/logistics/fabric/
│       │   ├── LogisticsModClient.java    # ClientModInitializer
│       │   └── client/
│       └── resources/
│           ├── fabric.mod.json
│           └── META-INF/services/         # ServiceLoader registration
└── neoforge/
    ├── build.gradle                       # NeoGradle setup
    └── src/main/java/com/logistics/neoforge/
        ├── LogisticsMod.java              # @Mod entry point
        ├── platform/                      # NeoForge service implementations
        └── resources/
            ├── META-INF/neoforge.mods.toml
            └── META-INF/services/
```

---

## Key Abstractions Needed

### 1. Item Storage Service (Critical - Transfer API → IItemHandler)

The biggest challenge. Fabric's Transfer API and NeoForge's capability system are fundamentally different.

**Interface** (`common/platform/services/IItemStorageService.java`):
```java
public interface IItemStorageService {
    @Nullable ItemStorageHandle findStorage(World world, BlockPos pos, Direction side);
    void registerPipeStorage(BlockEntityType<?> type, StorageProvider provider);
    TransactionContext openTransaction();
}
```

**ItemStorageHandle** (platform-agnostic wrapper):
```java
public interface ItemStorageHandle {
    long insert(ItemStack stack, long maxAmount, TransactionContext tx);
    ItemStack extract(Predicate<ItemStack> filter, long maxAmount, TransactionContext tx);
    boolean isPipeStorage();  // For speed preservation on pipe-to-pipe transfers
}
```

**Transaction handling difference**:
- **Fabric**: Real transactions with rollback (`Transaction.openOuter()`)
- **NeoForge**: Simulation flag (`simulate=true`), no atomic rollback

The NeoForge implementation will track operations and apply on commit.

### 2. Block Entity Helper

```java
public interface IBlockEntityHelper {
    <T extends BlockEntity> BlockEntityType<T> createType(
        BlockEntityFactory<T> factory, Block... blocks);
}
```
- Fabric: `FabricBlockEntityTypeBuilder`
- NeoForge: `BlockEntityType.Builder`

### 3. Item Group Helper

```java
public interface IItemGroupHelper {
    ItemGroup createGroup(Identifier id, Text name,
        Supplier<ItemStack> icon, Consumer<ItemGroup.Entries> entries);
}
```
- Fabric: `FabricItemGroup.builder()`
- NeoForge: `CreativeModeTab.Builder`

### 4. Event Helper

```java
public interface IEventHelper {
    void onServerStarting(Consumer<MinecraftServer> callback);
    void onServerStarted(Consumer<MinecraftServer> callback);
}
```
- Fabric: `ServerLifecycleEvents.SERVER_STARTING.register()`
- NeoForge: `@SubscribeEvent` on `ServerStartingEvent`

### 5. Client Services (separate interface)

```java
public interface IClientHelper {
    void setBlockRenderLayer(Block block, RenderLayer layer);
    void registerBlockEntityRenderer(BlockEntityType<?> type, Provider<?> provider);
}
```
- Fabric: `BlockRenderLayerMap`, `BlockEntityRendererFactories`
- NeoForge: `EntityRenderersEvent`, `RenderType` registration

---

## Files to Modify

### Move to `common/` (platform-agnostic)

| File | Changes Needed |
|------|----------------|
| `pipe/*.java` | None - already agnostic |
| `pipe/modules/*.java` | Replace `ItemStorage.SIDED.find()` → `Services.itemStorage().findStorage()` |
| `pipe/runtime/PipeRuntime.java` | Replace Transfer API calls with service calls |
| `pipe/runtime/TravelingItem.java` | None |
| `pipe/runtime/RoutePlan.java` | None |
| `block/PipeBlock.java` | Replace `ItemStorage.SIDED.find()` with service call |
| `block/entity/PipeBlockEntity.java` | Return `ItemStorageHandle` instead of `Storage<ItemVariant>` |
| `item/*.java` | Minor registration changes |
| `util/TimingLog.java` | None |

### Keep in `fabric/` (loader-specific)

| File | Reason |
|------|--------|
| `LogisticsMod.java` | `ModInitializer` entry point |
| `LogisticsModClient.java` | `ClientModInitializer`, render layer setup |
| `PipeItemStorage.java` | Implements `Storage<ItemVariant>` |
| `PipeModelRegistry.java` | `ModelLoadingPlugin`, `FabricBakedModelManager` |
| Service implementations | `FabricItemStorageService`, etc. |

### Create for `neoforge/`

| File | Purpose |
|------|---------|
| `LogisticsMod.java` | `@Mod` entry point |
| `LogisticsModClient.java` | Client setup event handler |
| `NeoForgeItemStorageService.java` | IItemHandler-based implementation |
| `NeoForgePipeItemStorage.java` | IItemHandler for pipe block entity |
| `NeoForgeTransactionContext.java` | Simulation-based "transaction" |
| Other service implementations | Registration, events, etc. |

---

## Implementation Phases

### Phase 1: Project Restructure
- [x] Create multi-module Gradle structure
- [x] Move existing code to `fabric/` module (working state)
- [x] Configure build to produce Fabric JAR
- [x] **Verify**: Fabric mod still builds and runs

### Phase 2: Create Common Module
- [x] Create `common/` with service interfaces
- [x] Create `Services.java` service loader entry point
- [x] Move platform-agnostic code to `common/` (partial - standalone types only due to circular deps)
- [x] Mod still builds and runs

### Phase 3: Fabric Service Implementations
- [ ] Implement all service interfaces for Fabric
- [ ] Adapt existing Fabric-specific code to implement interfaces
- [ ] Register services via `META-INF/services/`
- [ ] **Verify**: Fabric mod builds and runs with new structure

### Phase 4: NeoForge Implementation
- [ ] Create `neoforge/` module with NeoGradle
- [ ] Implement `NeoForgeItemStorageService` using capabilities
- [ ] Implement transaction simulation layer
- [ ] Create entry point and mod descriptor
- [ ] **Verify**: NeoForge mod builds and runs

### Phase 5: Client-Side Abstraction
- [ ] Create client service interface
- [ ] Implement Fabric client services
- [ ] Implement NeoForge client services (model loading is the tricky part)
- [ ] **Verify**: Both loaders render correctly

### Phase 6: CI/CD Updates
- [ ] Update GitHub Actions to build both JARs
- [ ] Configure publishing for both CurseForge/Modrinth loaders
- [ ] Update version naming (e.g., `logistics-fabric-0.3.0.jar`, `logistics-neoforge-0.3.0.jar`)

---

## Build Configuration

### settings.gradle
```groovy
pluginManagement {
    repositories {
        maven { url = 'https://maven.fabricmc.net/' }
        maven { url = 'https://maven.neoforged.net/releases' }
        gradlePluginPortal()
    }
}

rootProject.name = 'logistics'
include 'common', 'fabric', 'neoforge'
```

### gradle.properties additions
```properties
# NeoForge
neoforge_version=21.1.x  # Match to 1.21.1
```

---

## Fabric-Specific Files (Current State)

These 11 files currently have Fabric API dependencies:

**Server-side:**
1. `src/main/java/com/logistics/LogisticsMod.java` - Entry point, lifecycle events, ItemStorage registration
2. `src/main/java/com/logistics/block/entity/LogisticsBlockEntities.java` - FabricBlockEntityTypeBuilder
3. `src/main/java/com/logistics/item/LogisticsItemGroups.java` - FabricItemGroup
4. `src/main/java/com/logistics/block/entity/PipeItemStorage.java` - Storage<ItemVariant> impl
5. `src/main/java/com/logistics/block/entity/PipeBlockEntity.java` - ItemVariant, Storage
6. `src/main/java/com/logistics/block/PipeBlock.java` - ItemStorage.SIDED.find()
7. `src/main/java/com/logistics/pipe/modules/ExtractionModule.java` - ItemStorage, Transaction, StorageView
8. `src/main/java/com/logistics/pipe/modules/InsertionModule.java` - ItemStorage, Transaction
9. `src/main/java/com/logistics/pipe/runtime/PipeRuntime.java` - ItemStorage, ItemVariant, Transaction

**Client-side:**
10. `src/client/java/com/logistics/LogisticsModClient.java` - ClientModInitializer, BlockRenderLayerMap
11. `src/client/java/com/logistics/client/render/PipeModelRegistry.java` - ModelLoadingPlugin, FabricBakedModelManager, FabricLoader

---

## Verification Plan

After each phase:
1. **Build test**: `./gradlew build` succeeds
2. **Runtime test**: Launch Minecraft with mod, place pipes, verify item transport
3. **Integration test**: Connect pipes to vanilla chests, verify extraction/insertion
4. **Cross-mod test**: Test with a popular inventory mod (e.g., Iron Chests if available)

---

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Transaction semantics mismatch | NeoForge impl tracks ops, applies on commit; test edge cases thoroughly |
| Model loading differences | May need separate model registry per loader; Fabric's system is more flexible |
| Capability registration timing | NeoForge registers caps in specific events; follow their lifecycle |
| Mappings differences | Use Mojmap for NeoForge (default); Fabric uses Yarn - intermediary handles this |

---

## Progress Tracking

**Current Phase**: Phase 2 Complete - Ready for Phase 3 (Fabric service implementations)

**Last Updated**: 2026-01-21

**Notes**:
-
