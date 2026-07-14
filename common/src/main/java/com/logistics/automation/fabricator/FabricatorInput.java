package com.logistics.automation.fabricator;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * Recipe input over the fabricator's whole material inventory. The machine does its own
 * multi-ingredient availability check ({@link FabricatorRecipe#canCraftFrom}); this exists only to
 * satisfy the {@code Recipe<T>} generic.
 */
public record FabricatorInput(List<ItemStack> items) implements RecipeInput {

    @Override
    public ItemStack getItem(int index) {
        return items.get(index);
    }

    @Override
    public int size() {
        return items.size();
    }
}
