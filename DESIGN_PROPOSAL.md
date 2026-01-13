# Design Proposal: Simplified BER-Based Pipe Rendering

## 🎯 Current Status Summary

**✅ COMPLETED:**
- Blockstate properties removed (4 states per pipe, 36 total)
- 99.98% blockstate reduction achieved (was 183,708)
- Load time: 7ms (was 36+ seconds)
- Game logic works - items flow correctly
- Architecture complete - models can be identified
- Connection caching implemented

**❌ BLOCKING ISSUE:**
- **Pipes are invisible** - need to render BakedModels in Block Entity Renderer
- This is the **ONLY** remaining task

**📋 NEXT:** Research how to render BakedModels in Minecraft 1.21.11 Fabric

---

## Original Problem
Issue #6: Blockstate explosion from connection properties and FEATURE_FACE property causing 36+ second load times (183,708 blockstates).

## Original Goal
Reduce blockstates from 183,708 to ~36 (4 per pipe × 9 pipes) by:
- Removing connection properties (NORTH, SOUTH, EAST, WEST, UP, DOWN)
- Removing FEATURE_FACE property
- Using Block Entity Renderer to dynamically assemble visuals

**Status: Goal achieved! Only rendering implementation remains.**

## Core Architecture

### 1. PipeBlock - Minimal Blockstate
```java
// Only these properties in blockstate:
- POWERED (boolean)
- WATERLOGGED (boolean)
// Result: 2 × 2 = 4 states per pipe type
```

### 2. PipeBlockEntity - Dumb Data Storage
```java
public class PipeBlockEntity extends BlockEntity {
    // Connection type cache (for rendering optimization)
    private final ConnectionType[] connectionTypes = new ConnectionType[6];

    // That's it! No business logic.
    // Feature faces, extraction direction, etc. stay in module NBT.
}
```

**Key Principle**: Block entity only stores what the renderer needs. All behavioral state stays in module NBT where it belongs.

### 3. Pipe - Model Provider
```java
public abstract class Pipe {
    private PipeBlock pipeBlock; // back-reference set during registration

    // Core model (always rendered)
    public Identifier getCoreModelId() {
        return Identifier.of("logistics", "block/" + getPipeName() + "_core");
    }

    // Arm model (delegates to modules for overrides)
    public Identifier getArmModelId(PipeContext ctx, Direction direction, boolean extended) {
        // Ask modules if they want to override this arm's model
        for (Module module : modules) {
            Identifier override = module.getArmModelId(ctx, direction, extended);
            if (override != null) {
                return override; // Module provides custom model
            }
        }
        // Default arm model
        String suffix = extended ? "_extension" : "";
        return Identifier.of("logistics", "block/" + getPipeName() + "_" + direction.getName() + suffix);
    }
}
```

### 4. Module - Arm Model Override
```java
public interface Module {
    /**
     * Override the arm model for a specific direction.
     * Modules like ExtractionModule and MergerModule use this to show
     * their feature face model instead of the default arm model.
     *
     * @return custom model ID, or null to use default
     */
    @Nullable
    default Identifier getArmModelId(PipeContext ctx, Direction direction, boolean extended) {
        return null; // Use default
    }
}
```

### 5. Example: ExtractionModule
```java
public class ExtractionModule implements Module {
    @Override
    public Identifier getArmModelId(PipeContext ctx, Direction direction, boolean extended) {
        // Only override for the extraction direction
        if (getExtractionDirection(ctx) == direction) {
            String pipeName = ctx.pipe().getPipeName();
            String suffix = extended ? "_extension" : "";
            return Identifier.of("logistics", "block/" + pipeName + "_feature_face_" + direction.getName() + suffix);
        }
        return null; // Use default for other directions
    }

    private Direction getExtractionDirection(PipeContext ctx) {
        // Read from module NBT (already exists)
        return ctx.moduleState(getStateKey())
            .getString("extract_direction")
            .map(Direction::byId)
            .orElse(null);
    }
}
```

## Rendering Flow

```
PipeBlockEntityRenderer.updateRenderState(entity, state) {
    // 1. Calculate connections (like current code does)
    for (direction in Direction.values()) {
        ConnectionType type = pipeBlock.getConnectionType(world, pos, direction);
        entity.setConnectionType(direction, type); // Cache for rendering
    }

    // 2. Build model list
    models.add(pipe.getCoreModelId());

    for (direction in Direction.values()) {
        ConnectionType type = entity.getConnectionType(direction);
        if (type == NONE) continue;

        boolean extended = (type == INVENTORY);
        Identifier armModel = pipe.getArmModelId(ctx, direction, extended);
        models.add(armModel);
    }
}

PipeBlockEntityRenderer.render(state, matrices, queue) {
    // Render all models
    for (modelId in state.models) {
        BakedModel model = modelManager.getModel(modelId);
        // Render model quads...
    }
}
```

## Key Design Decisions

### ✅ Modules Own Behavioral Rendering
- Extraction direction → ExtractionModule controls it
- Feature face model → ExtractionModule provides it
- No duplication: extraction direction already in module NBT
- No special cases in PipeBlock or PipeBlockEntity

### ✅ Block Entity Stays Dumb
- Only caches connection types (performance optimization)
- No business logic about "what is a feature face"
- No need to sync feature face separately from module state

### ✅ Single Source of Truth
- Module NBT contains all module state (extraction direction, etc.)
- Renderer asks module "what model?" and gets the answer
- No parallel state management

### ✅ Easy to Extend
Want a new module with custom rendering?
```java
class MyModule implements Module {
    @Override
    public Identifier getArmModelId(ctx, direction, extended) {
        if (shouldShowSpecialFace(ctx, direction)) {
            return Identifier.of("logistics", "block/my_special_face_" + direction.getName());
        }
        return null;
    }
}
```

## PipeTypes Registration

Stays clean and simple:
```java
public static final Pipe COPPER_TRANSPORT_PIPE = new Pipe() {};
public static final Pipe ITEM_EXTRACTOR_PIPE = new Pipe(
    new ExtractionModule(),
    new BlockConnectionModule(() -> PipeTypes.ITEM_EXTRACTOR_PIPE)
) {};
```

No block references needed!

## LogisticsBlocks Registration

```java
public static final Block COPPER_TRANSPORT_PIPE = register(
    "copper_transport_pipe",
    settings -> new PipeBlock(settings, PipeTypes.COPPER_TRANSPORT_PIPE),
    AbstractBlock.Settings.create()...
);

// After registration loop
public static void initialize() {
    LogisticsMod.LOGGER.info("Registering blocks");
    // Set back-references
    PipeTypes.COPPER_TRANSPORT_PIPE.setPipeBlock((PipeBlock) COPPER_TRANSPORT_PIPE);
    PipeTypes.ITEM_EXTRACTOR_PIPE.setPipeBlock((PipeBlock) ITEM_EXTRACTOR_PIPE);
    // ... etc
}
```

## Benefits Summary

1. **Massive blockstate reduction**: 183,708 → 36 (99.98% reduction)
2. **Clean separation of concerns**:
   - PipeBlock: minimal blockstate
   - PipeBlockEntity: dumb data cache
   - Pipe: model provider
   - Module: behavioral overrides
3. **No boilerplate**: One block per pipe, one JSON per pipe
4. **Easy to maintain**: Adding pipes is straightforward
5. **Module-driven**: Modules control their own rendering without special cases

## Open Questions

1. **Model Rendering**: How exactly do we render `BakedModel`s in the BER?
   - Option A: Use `BlockModelRenderer` directly
   - Option B: Manually render quads from baked model
   - Option C: Find a way to use `queue.submitBlock()` without separate block registrations

2. **Connection Calculation**: Where/when do we calculate and cache connection types?
   - During `PipeBlock.getConnectionType()` calls?
   - During BER update?
   - Both?

3. **Model Files**: Do we need to restructure existing model JSONs?
   - Core model: `copper_transport_pipe_core.json`
   - Arm models: `copper_transport_pipe_north.json`, `copper_transport_pipe_north_extension.json`, etc.
   - Feature faces: `item_extractor_pipe_feature_face_north.json`, etc.

## Current Progress (✅ Completed)

### ✅ Step 1: Removed Blockstate Properties from PipeBlock ✅
- Removed all 7 connection properties (NORTH, SOUTH, EAST, WEST, UP, DOWN, FEATURE_FACE)
- Only POWERED and WATERLOGGED remain
- **Result: 4 blockstates per pipe (was 20,412)**
- **Total: 36 blockstates for 9 pipes (was 183,708)**
- **99.98% reduction achieved!**
- Changed `getRenderType()` to `INVISIBLE` - all rendering happens in BER
- Renamed `canConnectTo()` to `getConnectionType(world, pos, direction)`
- Added client-side caching support in `getConnectionType()`

### ✅ Step 2: Updated PipeContext Connection Queries ✅
- `getConnectedDirections()` calls `PipeBlock.getConnectionType()` instead of reading blockstate
- `hasConnection()` calls `PipeBlock.getConnectionType()`
- Added `getConnectionType(direction)` method to PipeContext

### ✅ Step 3: Connection Caching in PipeRuntime ✅
- Cache connection types in block entity every tick (lines 50-55 in PipeRuntime.java)
- Updated all helper methods to use `getConnectionType()` instead of blockstate properties:
  - `getAllConnectedDirections(world, pos, state)`
  - `getValidDirections(world, pos, state, currentDirection)`
- Client-side renderer will use cached values for performance

### ✅ Step 5: Set Pipe Back-References ✅
- Added back-reference setup in `LogisticsBlocks.initialize()`
- Each Pipe now knows its PipeBlock
- Pipes can now generate model identifiers:
  - `pipe.getPipeName()` → "copper_transport_pipe"
  - `pipe.getCoreModelId()` → `Identifier.of("logistics", "block/copper_transport_pipe_core")`
  - `pipe.getArmModelId(ctx, direction, extended)` → delegates to modules or returns default

### ✅ Architecture Implementation Complete ✅
1. **Pipe.java** - Back-reference system and model ID methods working
2. **Module.java** - Arm model override method: `getArmModelId(ctx, direction, extended)`
3. **ExtractionModule & MergerModule** - Override `getArmModelId()` to show feature faces
4. **PipeBlockEntity.java** - Dumb data storage with connection type cache only
5. **PipeContext.java** - `pipe()` accessor works, `setFeatureFace()` deprecated

### ✅ Performance Validation ✅
- **Block loading time: 7ms** (was 36+ seconds)
- **Items flow correctly through pipes** - game logic unaffected
- **Pipes are invisible** - expected, waiting on renderer
- **Code compiles with no errors**

---

# 🔴 CRITICAL RESEARCH NEEDED: Step 4 - BakedModel Rendering

## The Problem
We need to render `BakedModel` instances in a Block Entity Renderer (BER) for Minecraft 1.21.11 Fabric.

**What Works:**
- Architecture is complete - we can generate model `Identifier`s
- Game logic works - items flow through pipes correctly
- Performance is perfect - 7ms load time (was 36+ seconds)
- Connection caching works - block entity stores what to render

**What Doesn't Work:**
- Pipes are invisible because we don't know how to render the `BakedModel`s

## The Goal
Implement `PipeBlockEntityRenderer.render()` to:
1. Load `BakedModel` instances from `Identifier`s (e.g., `logistics:block/copper_transport_pipe_core`)
2. Render those models at the correct position with proper lighting/overlay
3. Handle multiple models per pipe (1 core + multiple arms)

## Current Renderer Code (As-Is)

**File:** `src/client/java/com/logistics/client/render/PipeBlockEntityRenderer.java`

**Current State:** This renderer currently only renders traveling items. The pipe models themselves are not rendered yet.

```java
public class PipeBlockEntityRenderer implements BlockEntityRenderer<PipeBlockEntity, PipeRenderState> {
    private final ItemModelManager itemModelManager;

    public PipeBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        this.itemModelManager = ctx.itemModelManager();
        // TODO: Need to get BakedModelManager or similar here
    }

    @Override
    public PipeRenderState createRenderState() {
        return new PipeRenderState();
    }

    @Override
    public void updateRenderState(PipeBlockEntity entity, PipeRenderState state, float tickDelta, ...) {
        // Currently: Updates BlockEntityRenderState and traveling items
        // NEEDS: Build list of model Identifiers to render

        BlockState blockState = entity.getCachedState();
        if (blockState.getBlock() instanceof PipeBlock pipeBlock && pipeBlock.getPipe() != null) {
            Pipe pipe = pipeBlock.getPipe();
            PipeContext ctx = new PipeContext(entity.getWorld(), entity.getPos(), blockState, entity);

            // TODO: Add core model
            // Identifier coreModelId = pipe.getCoreModelId();
            // state.modelIds.add(coreModelId);

            // TODO: Add arm models for each connection
            // for (Direction direction : Direction.values()) {
            //     PipeBlock.ConnectionType type = entity.getConnectionType(direction);
            //     if (type != PipeBlock.ConnectionType.NONE) {
            //         boolean extended = (type == PipeBlock.ConnectionType.INVENTORY);
            //         Identifier armModelId = pipe.getArmModelId(ctx, direction, extended);
            //         state.modelIds.add(armModelId);
            //     }
            // }
        }

        // Existing traveling item code (works fine)
        state.travelingItems.clear();
        for (TravelingItem travelingItem : entity.getTravelingItems()) {
            // ... existing item rendering code ...
        }
    }

    @Override
    public void render(PipeRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, ...) {
        // TODO: Render pipe models BEFORE rendering items
        // for (Identifier modelId : state.modelIds) {
        //     BakedModel model = GET_MODEL_SOMEHOW(modelId);
        //     RENDER_MODEL_SOMEHOW(model, matrices, queue, state.lightmapCoordinates);
        // }

        // Existing traveling item rendering (works fine)
        for (TravelingItemRenderState itemState : state.travelingItems) {
            matrices.push();
            // ... existing item rendering code ...
            itemState.itemRenderState.render(matrices, queue, state.lightmapCoordinates, ...);
            matrices.pop();
        }
    }

    public static class PipeRenderState extends BlockEntityRenderState {
        public final List<TravelingItemRenderState> travelingItems = new ArrayList<>();
        // TODO: Add this field
        // public final List<Identifier> modelIds = new ArrayList<>();
        public float tickDelta;
    }
}
```

## What Needs to be Added

### In Constructor:
```java
public PipeBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
    this.itemModelManager = ctx.itemModelManager();
    // TODO: Get BakedModelManager - HOW?
    // this.modelManager = ???
}
```

### In PipeRenderState:
```java
public static class PipeRenderState extends BlockEntityRenderState {
    public final List<TravelingItemRenderState> travelingItems = new ArrayList<>();
    public final List<Identifier> modelIds = new ArrayList<>();  // ADD THIS
    public float tickDelta;
}
```

### In updateRenderState():
```java
// Add the model building logic (see TODOs in code above)
Identifier coreModelId = pipe.getCoreModelId();
state.modelIds.add(coreModelId);

for (Direction direction : Direction.values()) {
    PipeBlock.ConnectionType type = entity.getConnectionType(direction);
    if (type != PipeBlock.ConnectionType.NONE) {
        boolean extended = (type == PipeBlock.ConnectionType.INVENTORY);
        Identifier armModelId = pipe.getArmModelId(ctx, direction, extended);
        state.modelIds.add(armModelId);
    }
}
```

### In render():
```java
// BEFORE rendering items, render the pipe models
for (Identifier modelId : state.modelIds) {
    BakedModel model = modelManager.getModel(modelId);  // How to get model?
    // How to render this BakedModel with proper lighting/overlay?
}

// Then render items (existing code stays)
```

## Research Questions

### Question 1: How to get BakedModel from Identifier in 1.21.11?
- Is there a `BakedModelManager` available in the BER context?
- Can we get it from `BlockEntityRendererFactory.Context`?
- Do we use `MinecraftClient.getInstance().getBakedModelManager()`?
- Method signature to look for: `getModel(Identifier)` or similar

### Question 2: How to render BakedModel in the new render system?
- What's the equivalent of `queue.submitBlock(BlockState)` for rendering a `BakedModel`?
- Do we use `BlockModelRenderer`?
- Do we manually get quads from `model.getQuads()` and submit them?
- How do we pass lighting (`lightmapCoordinates`) and overlay (`OverlayTexture.DEFAULT_UV`)?

### Question 3: How do other BERs render custom models?
- Look at `ChestBlockEntityRenderer` in 1.21.11 - does it render models or use a different approach?
- Look at `ShulkerBoxBlockEntityRenderer`
- Are there any Fabric mods that render BakedModels in BERs?
- What about conduits, beacons, or other complex block renderers?

### Question 4: Model rotations and transforms
- Our model files (e.g., `copper_transport_pipe_north.json`) define directional models
- Are these already rotated in the JSON (via Minecraft's blockstate system)?
- Or do we need to apply rotation transforms when rendering?
- How to handle the `extended` variants (for inventory connections)?

## Technical Context

**Minecraft Version:** 1.21.11
**Mod Loader:** Fabric
**Yarn Mappings:** 1.21.11+build.3

**Key Classes to Research:**
- `BlockEntityRenderer` (interface we implement)
- `BlockEntityRendererFactory.Context` (what's available in constructor?)
- `BakedModelManager` or `ModelManager` (how to get models?)
- `BakedModel` (the model interface)
- `BlockModelRenderer` (vanilla class for rendering block models)
- `OrderedRenderCommandQueue` (the queue we submit to)
- `MatrixStack` (for transforms)

**Current File:** `src/client/java/com/logistics/client/render/PipeBlockEntityRenderer.java`

## Success Criteria
A working implementation that:
1. ✅ Loads models by `Identifier`
2. ✅ Renders core model (always visible)
3. ✅ Renders arm models based on connections
4. ✅ Modules' feature faces render correctly via `getArmModelId()` override
5. ✅ Proper lighting (uses `state.lightmapCoordinates`)
6. ✅ Proper overlay (uses `OverlayTexture.DEFAULT_UV`)
7. ✅ Traveling items still render (already working)

## Additional Notes
- We already have all the model JSON files in `src/main/resources/assets/logistics/models/block/`
- Models follow naming convention: `{pipe_name}_core.json`, `{pipe_name}_north.json`, etc.
- The blockstate JSON files will need updating later (Step 6), but that's separate from BER rendering
- Performance is critical - this renders every frame for every pipe on screen

---

## Remaining Work

### ❌ Step 4: Implement BakedModel Rendering (CRITICAL - SEE ABOVE) ❌

**Status:** Needs research - see "CRITICAL RESEARCH NEEDED" section above

This is the blocking issue. Everything else is done.

---

### ⏸️ Step 6: Update Blockstate JSON Files (Can Wait)

**Status:** Not blocking - can be done after rendering works

Once rendering works, update all blockstate JSONs to simple format:
```json
{
  "variants": {
    "": { "model": "logistics:block/pipe_empty" }
  }
}
```

---

## Summary for Research

**The only remaining task:** Figure out how to render `BakedModel` instances in `PipeBlockEntityRenderer.render()` for Minecraft 1.21.11 Fabric.

**Key unknowns:**
1. How to get `BakedModelManager` or equivalent in the BER
2. How to load a `BakedModel` from an `Identifier`  
3. How to render that model with proper lighting/overlay in the new render system

**All other work is complete** - architecture, blockstate reduction, caching, model ID generation - everything works except the actual model rendering.

Good luck with the research!
