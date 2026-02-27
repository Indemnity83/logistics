package com.logistics.core.fabricator;

import com.logistics.LogisticsCore;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
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
    public @NotNull ItemStack assemble(@NotNull RecipeInput input) {
        // Not used - KilnBlockEntity handles assembly
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull String group() {
        return "";
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public @NotNull RecipeSerializer<KilnRecipeWrapper> getSerializer() {
        return LogisticsCore.KILN_RECIPE_SERIALIZER;
    }

    @Override
    public @NotNull RecipeType<KilnRecipeWrapper> getType() {
        return LogisticsCore.KILN_RECIPE_TYPE;
    }

    @Override
    public @NotNull PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public @NotNull RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.FURNACE_MISC;
    }
}
