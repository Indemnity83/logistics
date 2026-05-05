package com.logistics.core.macerator;

import com.logistics.LogisticsCore;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Recipe wrapper integrating macerator recipes into Minecraft's recipe system.
 * Carries real ingredient/result data for recipe book display and placement.
 * Actual machine operation still uses MaceratorRecipeManager.
 */
public class MaceratorRecipeWrapper implements Recipe<SingleRecipeInput> {

    private final Ingredient ingredient;
    private final ItemStack result;
    private final int grindingTime;
    private final float experience;
    @Nullable private PlacementInfo placementInfo;

    public MaceratorRecipeWrapper(Ingredient ingredient, ItemStack result, int grindingTime, float experience) {
        this.ingredient = ingredient;
        this.result = result;
        this.grindingTime = grindingTime;
        this.experience = experience;
    }

    public Ingredient ingredient() {
        return ingredient;
    }

    public ItemStack result() {
        return result;
    }

    public int grindingTime() {
        return grindingTime;
    }

    public float experience() {
        return experience;
    }

    @Override
    public boolean matches(@NotNull SingleRecipeInput input, @NotNull Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull SingleRecipeInput input, HolderLookup.@NotNull Provider provider) {
        return result.copy();
    }

    @Override
    public @NotNull RecipeSerializer<MaceratorRecipeWrapper> getSerializer() {
        return LogisticsCore.RECIPE.MACERATOR_RECIPE_SERIALIZER;
    }

    @Override
    public @NotNull RecipeType<MaceratorRecipeWrapper> getType() {
        return LogisticsCore.RECIPE.MACERATOR_RECIPE_TYPE;
    }

    @Override
    public boolean isSpecial() {
        return false;
    }

    @Override
    public @NotNull PlacementInfo placementInfo() {
        if (placementInfo == null) {
            placementInfo = PlacementInfo.create(ingredient);
        }
        return placementInfo;
    }

    @Override
    public @NotNull RecipeBookCategory recipeBookCategory() {
        return LogisticsCore.RECIPE.MACERATOR_CATEGORY;
    }

    @Override
    public @NotNull List<RecipeDisplay> display() {
        return List.of(new MaceratorRecipeDisplay(
            ingredient.display(),
            new SlotDisplay.ItemStackSlotDisplay(result),
            new SlotDisplay.ItemSlotDisplay(LogisticsCore.BLOCK.MACERATOR.asItem()),
            grindingTime,
            experience
        ));
    }
}
