package com.logistics.automation.macerator;

import com.logistics.core.lib.resource.ResourceId;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Recipe for the Macerator.
 * Accepts a single input item and produces a result.
 * Processing time and energy cost are global constants on {@link MaceratorBlockEntity}.
 */
public class MaceratorRecipe {
    private final ResourceId id;
    private final Ingredient ingredient;
    private final ResourceId resultItemId;
    private final int resultCount;

    public MaceratorRecipe(
        ResourceId id,
        Ingredient ingredient,
        ResourceId resultItemId,
        int resultCount
    ) {
        if (ingredient == null) throw new IllegalArgumentException("ingredient must not be null");
        if (resultItemId == null) throw new IllegalArgumentException("resultItemId must not be null");
        if (resultCount <= 0) throw new IllegalArgumentException("resultCount must be > 0");
        this.id = id;
        this.ingredient = ingredient;
        this.resultItemId = resultItemId;
        this.resultCount = resultCount;
    }

    public ResourceId getId() {
        return id;
    }

    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && ingredient.test(stack);
    }

    public ItemStack getResultItem() {
        var itemHolder = BuiltInRegistries.ITEM.get(resultItemId.toIdentifier())
            .orElseThrow(() -> new IllegalStateException("Result item not found: " + resultItemId));
        return new ItemStack(itemHolder.value(), resultCount);
    }

    public Ingredient getIngredient() {
        return ingredient;
    }
}
