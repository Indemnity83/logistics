# Minecraft Version Upgrade Plan
**Target**: Minecraft 1.21.0 → 1.21.11

## Current State (Starting Point)
- **Minecraft**: 1.21.0
- **Yarn Mappings**: 1.21+build.9
- **Fabric Loader**: 0.16.7
- **Fabric API**: 0.102.0+1.21
- **Loom**: 1.8-SNAPSHOT
- **Java**: 21

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
- Yarn: 1.21.5+build.1 (or latest)
- Loom: 1.10+
- Loader: 0.16.10
- Fabric API: [find latest for 1.21.5]

**Breaking Changes** (CRITICAL):
1. **NBT methods return `Optional`** instead of direct values
   - `nbt.getInt("key")` → `nbt.getInt("key").orElse(0)`
   - `nbt.getString("key")` → `nbt.getString("key").orElse("")`
   - Or use fallback: `nbt.getInt("key", 0)`

**Files to Change**:
- `PipeBlockEntity.java` - NBT read/write methods
- `TravelingItem.java` - NBT serialization
- Any other NBT usage

**Test**: Test saving/loading pipes with items inside
**Commit**: "chore: update to Minecraft 1.21.5"

---

### 🔲 Phase 6: Update to Minecraft 1.21.6-1.21.8
**Status**: NOT STARTED
**Target Versions**:
- Minecraft: 1.21.8
- Yarn: 1.21.8+build.1 (or latest)
- Loom: 1.10+
- Loader: 0.16.14
- Fabric API: [find latest for 1.21.8]

**Breaking Changes**:
1. HUD API rewritten (we don't use this)
2. `BlockRenderLayerMap` import/usage changed
3. Several deprecated modules removed

**Files to Change**:
- `LogisticsModClient.java` - Update `BlockRenderLayerMap` usage

**Example Change**:
```java
// OLD
BlockRenderLayerMap.INSTANCE.putBlock(block, RenderLayer.getCutout());

// NEW
BlockRenderLayerMap.put(RenderLayer.getCutout(), block);
```

**Test**: Ensure pipes render with transparency correctly
**Commit**: "chore: update to Minecraft 1.21.8"

---

### 🔲 Phase 7: Update to Minecraft 1.21.9/1.21.10
**Status**: NOT STARTED
**Target Versions**:
- Minecraft: 1.21.10
- Yarn: 1.21.10+build.1 (or latest)
- Loom: 1.11+
- Loader: 0.17.2
- Fabric API: [find latest for 1.21.10]

**Breaking Changes**:
1. `Entity#getWorld()` → `Entity#getEntityWorld()`
2. World render events removed (we don't use this)
3. Keybinding system restructured (we don't use this)

**Files to Change**:
- Search for `.getWorld()` calls on Entity objects
- Likely minimal changes for this mod

**Test**: `./gradlew build && ./gradlew runClient`
**Commit**: "chore: update to Minecraft 1.21.10"

---

### 🔲 Phase 8: Update to Minecraft 1.21.11 (FINAL)
**Status**: NOT STARTED
**Target Versions**:
- Minecraft: 1.21.11
- Yarn: 1.21.11+build.4 (FINAL Yarn version)
- Loom: 1.14+
- Loader: 0.18.4
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
