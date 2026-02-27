package com.logistics.core.fabricator;

import com.logistics.LogisticsCore;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Minimal Recipe wrapper for KilnRecipe to satisfy Minecraft's recipe system requirements.
 * This is only used for registration - actual recipe matching is done by KilnRecipeManager.
 */
public class KilnRecipeWrapper implements Recipe<RecipeInput> {

    @Override
    public boolean matches(@NotNull RecipeInput input, @NotNull Level level) {
        // Not used - KilnRecipeManager handles matching
        return false;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull RecipeInput input, HolderLookup.@NotNull Provider provider) {
        // Not used - KilnBlockEntity handles assembly
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width == 3 && height == 3;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider provider) {
        // Not used - each KilnRecipe has its own result
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return LogisticsCore.KILN_RECIPE_SERIALIZER;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return LogisticsCore.KILN_RECIPE_TYPE;
    }
}
