package com.logistics.automation.macerator;

import com.logistics.core.lib.resource.ResourceId;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Recipe for the Iron Macerator.
 * Accepts a single input item and produces a result after consuming energy over time.
 */
public class MaceratorRecipe {
    private final ResourceId id;
    private final Ingredient ingredient;
    private final int processTimeTicks;
    private final int energyPerTick;
    private final ResourceId resultItemId;
    private final int resultCount;

    public MaceratorRecipe(
        ResourceId id,
        Ingredient ingredient,
        int processTimeTicks,
        int energyPerTick,
        ResourceId resultItemId,
        int resultCount
    ) {
        this.id = id;
        this.ingredient = ingredient;
        this.processTimeTicks = processTimeTicks;
        this.energyPerTick = energyPerTick;
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

    public int getProcessTimeTicks() {
        return processTimeTicks;
    }

    public int getEnergyPerTick() {
        return energyPerTick;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }
}
