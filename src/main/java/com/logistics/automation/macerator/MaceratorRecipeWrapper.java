package com.logistics.automation.macerator;

import com.logistics.LogisticsAutomation;
import net.minecraft.core.HolderLookup;
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
 * Minimal Recipe wrapper to satisfy Minecraft's recipe system.
 * Actual recipe matching is handled by MaceratorRecipeManager.
 */
public class MaceratorRecipeWrapper implements Recipe<RecipeInput> {

    @Override
    public boolean matches(@NotNull RecipeInput input, @NotNull Level level) {
        return false;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull RecipeInput input, HolderLookup.@NotNull Provider provider) {
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull RecipeSerializer<MaceratorRecipeWrapper> getSerializer() {
        return LogisticsAutomation.RECIPE.MACERATOR_RECIPE_SERIALIZER;
    }

    @Override
    public @NotNull RecipeType<MaceratorRecipeWrapper> getType() {
        return LogisticsAutomation.RECIPE.MACERATOR_RECIPE_TYPE;
    }

    @Override
    public boolean isSpecial() {
        return true;
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
