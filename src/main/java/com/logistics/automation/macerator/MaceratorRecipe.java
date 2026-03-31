package com.logistics.automation.macerator;

import com.logistics.core.lib.resource.ResourceId;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

/**
 * Recipe for the Macerator.
 * Accepts a single input item and produces a result.
 */
public class MaceratorRecipe {
    static final int DEFAULT_GRINDING_TIME = 200;

    private final ResourceId id;
    @Nullable private final Ingredient ingredient;
    @Nullable private final TagKey<Item> tagIngredient;
    private final ResourceId resultItemId;
    private final int resultCount;
    private final int grindingTime;

    public MaceratorRecipe(
        ResourceId id,
        Ingredient ingredient,
        ResourceId resultItemId,
        int resultCount,
        int grindingTime
    ) {
        if (ingredient == null) throw new IllegalArgumentException("ingredient must not be null");
        if (resultItemId == null) throw new IllegalArgumentException("resultItemId must not be null");
        if (resultCount <= 0) throw new IllegalArgumentException("resultCount must be > 0");
        this.id = id;
        this.ingredient = ingredient;
        this.tagIngredient = null;
        this.resultItemId = resultItemId;
        this.resultCount = resultCount;
        this.grindingTime = grindingTime;
    }

    public MaceratorRecipe(
        ResourceId id,
        TagKey<Item> tagIngredient,
        ResourceId resultItemId,
        int resultCount,
        int grindingTime
    ) {
        if (tagIngredient == null) throw new IllegalArgumentException("tagIngredient must not be null");
        if (resultItemId == null) throw new IllegalArgumentException("resultItemId must not be null");
        if (resultCount <= 0) throw new IllegalArgumentException("resultCount must be > 0");
        this.id = id;
        this.ingredient = null;
        this.tagIngredient = tagIngredient;
        this.resultItemId = resultItemId;
        this.resultCount = resultCount;
        this.grindingTime = grindingTime;
    }

    public ResourceId getId() {
        return id;
    }

    public int getGrindingTime() {
        return grindingTime;
    }

    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (tagIngredient != null) return stack.is(tagIngredient);
        return ingredient.test(stack);
    }

    public ItemStack getResultItem() {
        var itemHolder = BuiltInRegistries.ITEM.get(resultItemId.toIdentifier())
            .orElseThrow(() -> new IllegalStateException("Result item not found: " + resultItemId));
        return new ItemStack(itemHolder.value(), resultCount);
    }

    public boolean isTagBased() {
        return tagIngredient != null;
    }

    @Nullable
    public TagKey<Item> getTagIngredient() {
        return tagIngredient;
    }

    @Nullable
    public Ingredient getIngredient() {
        return ingredient;
    }
}
