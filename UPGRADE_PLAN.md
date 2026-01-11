# Minecraft Version Upgrade Plan
**Target**: Minecraft 1.21.0 → 1.21.11

## Current State
- **Minecraft**: 1.21.10 (upgraded from 1.21.0)
- **Yarn Mappings**: 1.21.10+build.3
- **Fabric Loader**: 0.17.2
- **Fabric API**: 0.135.0+1.21.10
- **Loom**: 1.11.8
- **Gradle**: 8.14
- **Java**: 21
- **Progress**: 7/8 phases complete (87.5%)

## Files That Will Need Changes

### High Priority (Definitely Will Change)
- `gradle.properties` - Version numbers (every phase)
- `src/main/resources/fabric.mod.json` - Dependency versions
- `src/main/java/com/logistics/block/LogisticsBlocks.java` - Registry changes (Phase 3)
- `src/main/java/com/logistics/item/LogisticsItems.java` - Registry changes (Phase 3)
- `src/main/java/com/logistics/block/entity/PipeBlockEntity.java` - NBT Optional changes (Phase 5)
- `src/client/java/com/logistics/LogisticsModClient.java` - BlockRenderLayerMap changes (Phase 6)

### Medium Priority (Likely Will Change)
- `src/client/java/com/logistics/client/render/PipeBlockEntityRenderer.java` - Rendering API changes
- `src/main/java/com/logistics/block/PipeBlock.java` - Block settings (Phase 3)
- `src/main/java/com/logistics/pipe/runtime/TravelingItem.java` - NBT Optional changes (Phase 5)
- Any file using `entity.getWorld()` → `entity.getEntityWorld()` (Phase 7)

## Upgrade Phases

### ✅ Phase 0: Research & Planning
**Status**: COMPLETED
- [x] Analyze codebase structure
- [x] Research all Fabric blog posts
- [x] Create upgrade plan
- **Commit**: N/A

---

### ✅ Phase 1: Update to Latest 1.21.0-Compatible Versions
**Status**: COMPLETED
**Goal**: Update Loom, Loader, and Fabric API to latest 1.21.0-compatible versions

**Changes Made**:
```properties
# build.gradle
loom version 1.8-SNAPSHOT → 1.8.10 (stable release)

# Already at latest:
loader_version=0.16.7 (already latest for 1.21.0)
fabric_api_version=0.102.0+1.21 (already latest for 1.21.0)
```

**Code Changes**: NONE
**Test**: `./gradlew build` ✅ SUCCESS
**Commit**: "chore: update to latest 1.21.0-compatible dependencies"

---

### ✅ Phase 2: Update to Minecraft 1.21.1
**Status**: COMPLETED
**Target Versions**:
- Minecraft: 1.21.1
- Yarn: 1.21.1+build.3
- Loom: 1.8.10
- Loader: 0.16.7
- Fabric API: 0.108.0+1.21.1

**Breaking Changes**:
1. `Identifier` constructor now protected
   - `new Identifier(namespace, path)` → `Identifier.of(namespace, path)`
   - `new Identifier(path)` → `Identifier.of(path)` or `Identifier.ofVanilla(path)`

**Files Changed**:
- `gradle.properties` - Updated version numbers
- `fabric.mod.json` - Updated minecraft dependency
- **No code changes needed** - codebase doesn't use `new Identifier()`

**Test**: `./gradlew build` ✅ SUCCESS
**Commit**: "update to Minecraft 1.21.1"

---

### ✅ Phase 3: Update to Minecraft 1.21.2/1.21.3
**Status**: COMPLETED
**Target Versions**:
- Minecraft: 1.21.3
- Yarn: 1.21.3+build.2
- Loom: 1.8.10
- Loader: 0.16.7
- Fabric API: 0.112.1+1.21.3

**Breaking Changes Fixed**:
1. **Registry keys now required** for all blocks/items ✅
2. `FabricBlockSettings` removed (already using `AbstractBlock.Settings`) ✅
3. `ItemStack.encode()` → `ItemStack.toNbt()` ✅
4. `BlockEntityType.Builder` → `FabricBlockEntityTypeBuilder` ✅
5. `neighborUpdate` and `getStateForNeighborUpdate` method signature changes ✅
6. `ModelTransformationMode` moved to `net.minecraft.item` package ✅
7. `DrawContext.drawTexture()` now requires RenderLayer parameter ✅

**Files to Change**:
- `LogisticsBlocks.java` - Add `.registryKey()` to all block settings
- `LogisticsItems.java` - Add `.registryKey()` to all item settings
- `PipeBlock.java` - Replace `FabricBlockSettings` with `AbstractBlock.Settings`
- Any custom block/item classes

**Example Changes**:
```java
// OLD
public static final Block WOODEN_PIPE = new WoodenPipeBlock(
    FabricBlockSettings.create()
        .mapColor(MapColor.OAK_TAN)
        .strength(2.0F)
);

// NEW
public static final Block WOODEN_PIPE = new WoodenPipeBlock(
    AbstractBlock.Settings.create()
        .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of("logistics", "wooden_pipe")))
        .mapColor(MapColor.OAK_TAN)
        .strength(2.0F)
);
```

**Test**: Look for `NullPointerException: Block id not set` errors
**Commit**: "chore: update to Minecraft 1.21.3"

---

### ✅ Phase 4: Update to Minecraft 1.21.4
**Status**: COMPLETED
**Target Versions**:
- Minecraft: 1.21.4
- Yarn: 1.21.4+build.8
- Loom: 1.9.2
- Loader: 0.16.9
- Fabric API: 0.110.3+1.21.4
- **Gradle**: 8.11 (upgraded from 8.10.2, required by Loom 1.9+)

**Breaking Changes**:
1. Block entities auto-render their block models (didn't affect us)
2. `ItemColors` API removed (we don't use this)
3. `BuiltinItemRenderer` changes (we don't use this)

**Files Changed**:
- `gradle.properties` - Updated versions
- `fabric.mod.json` - Updated minecraft dependency
- `build.gradle` - Updated Loom to 1.9.2
- `gradle/wrapper/gradle-wrapper.properties` - Upgraded Gradle to 8.11
- **No code changes needed** ✅

**Test**: `./gradlew build` ✅ SUCCESS
**Commit**: "update to Minecraft 1.21.4"

---

### ✅ Phase 5: Update to Minecraft 1.21.5
**Status**: COMPLETED
**Target Versions**:
- Minecraft: 1.21.5
- Yarn: 1.21.5+build.1
- Loom: 1.10.1
- Loader: 0.16.10
- Fabric API: 0.119.5+1.21.5
- **Gradle**: 8.12 (upgraded from 8.11, required by Loom 1.10+)

**Breaking Changes Fixed**:
1. **NBT methods return `Optional`** instead of direct values ✅
   - Changed `PipeBlockEntity.getOrCreateModuleState()` to return `NbtCompound` directly
   - Updated all NBT access to use `orElse()` or `orElseGet()`
2. **Direction API changes** ✅
   - `Direction.getId()` → numeric index (0-5)
   - `Direction.getName()` → string ID ("north", "south", etc.)
3. **Particle API changes** ✅
   - `World.addParticle()` → `World.addParticleClient()`
4. **ModelTransformationMode renamed** ✅
   - → `ItemDisplayContext`
5. **BlockEntityRenderer signature change** ✅
   - Added `Vec3d cameraPos` parameter
6. **Block entity lifecycle** ✅
   - Moved cleanup from `onStateReplaced()` to `PipeBlockEntity.onBlockReplaced()`

**Code Improvements**:
- Added `Module.getStateKey()` default method
- Added PipeContext helper methods: `getString()`, `saveString()`, `getInt()`, `saveInt()`, `remove()`
- Improved code readability with better constant naming
- Made SmartSplitterModule filter methods instance methods
- Added `Pipe.getModule(Class)` to retrieve module instances
- Added `PipeBlockEntity.createContext()` helper

**Files Changed**:
- `gradle.properties` - Updated versions
- `build.gradle` - Updated Loom to 1.10.1
- `gradle/wrapper/gradle-wrapper.properties` - Gradle 8.12
- `fabric.mod.json` - Updated minecraft dependency to ~1.21.5
- `PipeBlockEntity.java` - NBT Optional handling
- `PipeContext.java` - Added convenience methods, NBT helpers
- `Module.java` - Added getStateKey() default method
- `VoidModule.java` - addParticle → addParticleClient
- `PipeBlock.java` - Moved onStateReplaced logic
- `PipeBlockEntityRenderer.java` - ModelTransformationMode → ItemDisplayContext, render() signature
- `ExtractionModule.java`, `MergerModule.java`, `SplitterModule.java`, `SmartSplitterModule.java` - Updated to use new API
- `FilterInventory.java` - Removed NbtCompound import, uses module from pipe
- `Pipe.java` - Added getModule(Class) method

**Test**: `./gradlew build` ✅ SUCCESS
**Commit**: "update to Minecraft 1.21.5"

---

### ✅ Phase 6: Update to Minecraft 1.21.6-1.21.8
**Status**: COMPLETED
**Target Versions**:
- Minecraft: 1.21.8
- Yarn: 1.21.8+build.1
- Loom: 1.10.5
- Loader: 0.16.14
- Fabric API: 0.136.1+1.21.8

**Breaking Changes Fixed**:
1. **BlockRenderLayerMap API migration** ✅
   - Package: `net.fabricmc.fabric.api.blockrenderlayer.v1` → `net.fabricmc.fabric.api.client.rendering.v1`
   - API: `BlockRenderLayerMap.INSTANCE.putBlock()` → static `BlockRenderLayerMap.putBlock()`
   - RenderLayer: `RenderLayer.getCutout()` → `BlockRenderLayer.CUTOUT`

2. **Codec-based serialization replaces manual NBT** ✅
   - NBT read/write now uses `WriteView`/`ReadView` instead of `writeNbt()`/`readNbt()`
   - Data serialization uses `Codec` instances
   - TravelingItem uses `CODEC` for serialization

**Critical Client Sync Fix**:
- Added `toInitialChunkDataNbt()` method to PipeBlockEntity
- **Issue**: Items were moving on server but not rendering on client when chunks loaded
- **Cause**: Missing initial chunk data sync
- **Fix**: Override `toInitialChunkDataNbt()` to return `createNbt(registries)`

**Files Changed**:
- `gradle.properties` - Updated versions
- `build.gradle` - Updated Loom to 1.10.5
- `fabric.mod.json` - Updated minecraft dependency to ~1.21.8
- `LogisticsModClient.java` - BlockRenderLayerMap API migration
- `PipeBlockEntity.java` - Migrated to WriteView/ReadView, added toInitialChunkDataNbt()
- `TravelingItem.java` - Replaced writeNbt/fromNbt with CODEC

**Example Changes**:
```java
// BlockRenderLayerMap migration
// OLD
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
BlockRenderLayerMap.INSTANCE.putBlock(block, RenderLayer.getCutout());

// NEW
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
BlockRenderLayerMap.putBlock(block, BlockRenderLayer.CUTOUT);

// NBT serialization migration
// OLD
@Override
protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
    NbtList list = new NbtList();
    for (TravelingItem item : items) {
        list.add(item.writeNbt(new NbtCompound(), registryLookup));
    }
    nbt.put("Items", list);
}

// NEW
@Override
protected void writeData(WriteView view) {
    WriteView.ListAppender<TravelingItem> appender = view.getListAppender("Items", TravelingItem.CODEC);
    for (TravelingItem item : items) {
        appender.add(item);
    }
}
```

**Test**: `./gradlew build` ✅ SUCCESS, items render properly on client
**Commit**: "update to Minecraft 1.21.8"

---

### ✅ Phase 7: Update to Minecraft 1.21.9/1.21.10
**Status**: COMPLETED
**Target Versions**:
- Minecraft: 1.21.10
- Yarn: 1.21.10+build.3
- Loom: 1.11.8
- Loader: 0.17.2
- Fabric API: 0.135.0+1.21.10
- **Gradle**: 8.14 (upgraded from 8.12, required by Loom 1.11.8)

**Breaking Changes Fixed**:
1. **Entity API Changes** ✅
   - `Entity#getWorld()` → `Entity#getEntityWorld()`
   - Updated in `DiamondFilterScreenHandler.java`

2. **World API Changes** ✅
   - `world.isClient` field → `world.isClient()` method
   - Updated in 7 files: PipeRuntime, ExtractionModule, PipeBlock, PipeContext, FilterInventory, PipeBlockEntity, SmartSplitterModule

3. **Comparator Output Signature** ✅
   - Changed from `public int getComparatorOutput(BlockState, World, BlockPos)`
   - To `protected int getComparatorOutput(BlockState, World, BlockPos, Direction direction)`

4. **BlockEntityRenderer Complete Rewrite** ✅
   - **New Type Parameters**: `BlockEntityRenderer<T, S extends BlockEntityRenderState>` (two instead of one)
   - **New Method Pattern**: Three methods replace single `render()`:
     - `createRenderState()` - creates render state instance
     - `updateRenderState()` - extracts data from block entity
     - `render()` - performs actual rendering
   - **New Classes**:
     - `ItemModelManager` replaces `ItemRenderer`
     - `ItemRenderState` replaces direct ItemStack rendering
     - `BlockEntityRenderState` from `net.minecraft.client.render.block.entity.state`
     - `CameraRenderState` from `net.minecraft.client.render.state`
     - `OrderedRenderCommandQueue` replaces `VertexConsumerProvider`
   - **Rendering Flow**:
     - `updateRenderState()` uses `ItemModelManager.update()` to populate `ItemRenderState`
     - `render()` calls `ItemRenderState.render()` to submit to command queue
     - All physics calculations (acceleration, interpolation) preserved

**Files Changed**:
- `gradle.properties` - Updated versions
- `build.gradle` - Updated Loom to 1.11.8
- `gradle/wrapper/gradle-wrapper.properties` - Gradle 8.14
- `fabric.mod.json` - Updated minecraft dependency to ~1.21.10
- `DiamondFilterScreenHandler.java` - getWorld() → getEntityWorld()
- `PipeRuntime.java`, `ExtractionModule.java`, `PipeBlock.java`, `PipeContext.java`, `FilterInventory.java`, `PipeBlockEntity.java`, `SmartSplitterModule.java` - isClient field → isClient() method
- `PipeBlock.java` - getComparatorOutput() signature change
- `PipeBlockEntityRenderer.java` - Complete rewrite for new rendering system

**Test**: `./gradlew build` ✅ SUCCESS
**Commit**: "update to Minecraft 1.21.10"

---

### 🔲 Phase 8: Update to Minecraft 1.21.11 (FINAL)
**Status**: NOT STARTED
**Target Versions**:
- Minecraft: 1.21.11
- Yarn: 1.21.11+build.3 (or latest)
- Loom: 1.11.8 (or latest)
- Loader: 0.17.2 (or latest)
- Fabric API: [find latest for 1.21.11]

**Breaking Changes**:
- This is the final obfuscated version
- Yarn will stop being updated after this
- Future: Will need to migrate to Mojang Mappings for 26.1+

**Files to Change**:
- Likely none, just version bumps

**Test**: Full game test - create world, place pipes, test all pipe types
**Commit**: "chore: update to Minecraft 1.21.11"

---

## Known Issues to Address Later

### Performance: Blockstate Explosion
- **Issue**: 36+ second initialization time due to 183,708 total blockstates (20,412 per pipe × 9 pipes)
- **Root cause**: `FEATURE_FACE` property multiplies states by 7× (729 × 7 × 2 × 2 = 20,412 per pipe)
- **Solution**: Move `FEATURE_FACE` rendering to Block Entity Renderer, removing it from blockstate
- **Impact**: Would reduce to 2,916 states per pipe (26,244 total) - 85% reduction
- **Status**: Deferred until after version migration complete

## Notes

### After Each Phase
1. Update version numbers in `gradle.properties`
2. Update version numbers in `fabric.mod.json`
3. Run `./gradlew build` to test compilation
4. Run `./gradlew runClient` to test in-game (when possible)
5. Stage changes with `git add .`
6. Run `hack commit` for commit message
7. Update this file to mark phase as complete

### Recovery Strategy
If any phase fails:
1. Document the error in this file
2. Research the specific API change
3. Fix code as needed
4. Re-test

### Future Work (Beyond 1.21.11)
- Migrate from Yarn to Mojang Mappings for 26.1+ compatibility
- This will require recompilation but minimal code changes
