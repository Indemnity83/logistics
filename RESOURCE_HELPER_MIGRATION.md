# ResourceHelper Migration Guide

## Overview

`ResourceHelper` is a stable wrapper around Minecraft's version-specific identifier types:
- **mc/1.21.11 & mc/26.1**: wraps `net.minecraft.resources.Identifier`
- **mc/1.21.1**: wraps `net.minecraft.resources.ResourceLocation`

This enables cherry-pick-friendly code across versions.

## Quick Start

```java
import com.logistics.core.lib.resource.ResourceId;

// Create identifiers
ResourceHelper id1 = ResourceHelper.of("logistics", "pipe/copper");
ResourceHelper id2 = ResourceHelper.parse("minecraft:stone");
ResourceHelper id3 = LogisticsMod.modId("block/quarry");  // logistics:block/quarry

// Use with Minecraft APIs (unwrap when needed)
Registry.register(BuiltInRegistries.BLOCK, id1.toIdentifier(), block);

// Access components
String namespace = id1.getNamespace();  // "logistics"
String path = id1.getPath();            // "pipe/copper"
String full = id1.toString();           // "logistics:pipe/copper"
```

## Migration Strategy

### Phase 1: Use ResourceHelper for new code ✅ COMPLETED

The `ResourceHelper` class is now available in `core.lib.resource`.

**New code should use:**
```java
// ✅ Preferred - stable type across versions
ResourceHelper id = LogisticsMod.modId("my/resource");

// ⚠️ Legacy - still works but deprecated
Identifier id = LogisticsMod.getIdentifier("my/resource");
```

### Phase 2: Migrate LogisticsMod helpers ✅ COMPLETED

The following methods now delegate to ResourceHelper:
- `LogisticsMod.modId(String)` - **NEW**: returns `ResourceHelper` (preferred)
- `LogisticsMod.getIdentifier(String)` - **DEPRECATED**: returns `Identifier`
- `LogisticsMod.parseIdentifier(String)` - **DEPRECATED**: use `ResourceHelper.parse()`
- `LogisticsMod.createIdentifier(String, String)` - **DEPRECATED**: use `ResourceHelper.of()`

### Phase 3: Migrate storage locations (OPTIONAL, INCREMENTAL)

Files that store `Identifier` can be migrated to `ResourceHelper` over time:

#### High-Priority Files (frequently cherry-picked):
1. **KilnBlockEntity.java:105** - `private Identifier activeRecipeId`
2. **KilnRecipeManager.java:38** - `Map<Identifier, KilnRecipe> RECIPES`
3. **Pipe.java:72-130** - Return types for model methods
4. **Module.java:95,139,118** - Return types for module methods

#### Migration Pattern:

**Before:**
```java
private Identifier activeRecipeId;

public void setRecipe(Identifier id) {
    this.activeRecipeId = id;
}

public Identifier getRecipe() {
    return activeRecipeId;
}

// NBT save
protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    if (activeRecipeId != null) {
        tag.putString("Recipe", activeRecipeId.toString());
    }
}

// NBT load
protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    if (tag.contains("Recipe")) {
        activeRecipeId = Identifier.parse(tag.getString("Recipe"));
    }
}
```

**After:**
```java
private ResourceHelper activeRecipeId;

public void setRecipe(ResourceHelper id) {
    this.activeRecipeId = id;
}

public ResourceHelper getRecipe() {
    return activeRecipeId;
}

// NBT save
protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    if (activeRecipeId != null) {
        tag.putString("Recipe", activeRecipeId.toString());
    }
}

// NBT load
protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    if (tag.contains("Recipe")) {
        activeRecipeId = ResourceHelper.parse(tag.getString("Recipe"));
    }
}
```

**When calling Minecraft APIs:**
```java
// Need to unwrap for Minecraft Registry API
RecipeHolder<KilnRecipe> recipe = level.getRecipeManager()
    .byKey(activeRecipeId.toIdentifier())  // ← unwrap here
    .orElse(null);
```

## Cherry-Pick Workflow

When cherry-picking commits between branches:

1. **Most code remains unchanged** - uses `ResourceHelper` type
2. **Only `ResourceHelper.java` needs version-specific changes:**

   ```diff
   # mc/1.21.11 → mc/1.21.1
   - import net.minecraft.resources.Identifier;
   + import net.minecraft.resources.ResourceLocation;

   - private final Identifier value;
   + private final ResourceLocation value;

   - public Identifier toIdentifier() {
   + public ResourceLocation toResourceLocation() {
       return value;
   }
   ```

3. **All usage sites compile without changes** because they use `ResourceHelper`

## Benefits

- ✅ **Cherry-pick friendly**: Only one file changes between versions
- ✅ **Type safe**: Compiler catches missing migrations
- ✅ **Clean API**: Consistent identifier creation across codebase
- ✅ **Incremental migration**: Can migrate files over time
- ✅ **Zero runtime overhead**: Single object wrapper with full delegation

## Common Patterns

### Creating Identifiers

```java
// Logistics namespace
ResourceHelper id = LogisticsMod.modId("pipe/copper");

// Arbitrary namespace
ResourceHelper id = ResourceHelper.of("minecraft", "stone");
ResourceHelper id = ResourceHelper.of("other_mod", "item");

// Parse from string
ResourceHelper id = ResourceHelper.parse("logistics:pipe/copper");
ResourceHelper id = ResourceHelper.tryParse("might:be:invalid");  // returns null if invalid
```

### Using with Minecraft APIs

```java
// Registration
Registry.register(BuiltInRegistries.BLOCK, id.toIdentifier(), block);

// Recipe manager
RecipeHolder<Recipe> recipe = recipeManager.byKey(id.toIdentifier()).orElse(null);

// Resource keys
ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id.toIdentifier());
```

### Storing in Collections

```java
// Maps
Map<ResourceHelper, Recipe> recipes = new HashMap<>();
recipes.put(LogisticsMod.modId("copper_pipe"), recipe);

// Sets
Set<ResourceHelper> ids = new HashSet<>();
ids.add(ResourceHelper.parse("logistics:item/wrench"));

// Lists
List<ResourceHelper> models = List.of(
    LogisticsMod.modId("block/quarry"),
    LogisticsMod.modId("block/engine")
);
```

## Next Steps

1. **New code**: Use `LogisticsMod.modId()` or `ResourceHelper` directly
2. **Existing code**: Continue using deprecated methods (they still work)
3. **Incremental migration**: Convert high-churn files to ResourceHelper when touching them
4. **Cherry-picks**: Update `ResourceHelper.java` when porting between versions

The deprecated methods will be removed in a future cleanup PR after most code has migrated.
