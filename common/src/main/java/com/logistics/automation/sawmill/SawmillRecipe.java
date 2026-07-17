package com.logistics.automation.sawmill;

import com.logistics.LogisticsAutomation;
import com.logistics.core.lib.recipe.AbstractLogisticsRecipe;
import com.logistics.core.lib.recipe.ItemResult;
import com.logistics.core.lib.recipe.RecipeByproduct;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Sawmill recipe — one ingredient cut into a primary product plus a chance-based byproduct
 * (Wood Pulp). Loaded by vanilla's {@code RecipeManager} under the {@code logistics:sawmill} type.
 */
public class SawmillRecipe extends AbstractLogisticsRecipe<SingleRecipeInput> {

    public static final int DEFAULT_INGREDIENT_COUNT = 1;
    public static final float DEFAULT_EXPERIENCE = 0.0f;

    private final Ingredient ingredient;
    private final int ingredientCount;
    private final ItemResult result;
    private final Optional<RecipeByproduct> byproduct;
    private final int energy;
    private final float experience;
    @Nullable private PlacementInfo placementInfo;

    public SawmillRecipe(
            Ingredient ingredient, int ingredientCount, ItemResult result, Optional<RecipeByproduct> byproduct,
            int energy, float experience) {
        if (energy <= 0) {
            throw new IllegalArgumentException("energy must be positive, got " + energy);
        }
        if (!Float.isFinite(experience) || experience < 0) {
            throw new IllegalArgumentException("experience must be finite and non-negative, got " + experience);
        }
        this.ingredient = ingredient;
        this.ingredientCount = ingredientCount;
        this.result = result;
        this.byproduct = byproduct;
        this.energy = energy;
        this.experience = experience;
    }

    public Ingredient ingredient() {
        return ingredient;
    }

    /** How many of the input item one cut consumes. */
    public int ingredientCount() {
        return ingredientCount;
    }

    public ItemResult result() {
        return result;
    }

    public Optional<RecipeByproduct> byproduct() {
        return byproduct;
    }

    /** Total RF this cut consumes; the machine drains it at a fixed RF/t. */
    public int energy() {
        return energy;
    }

    public float experience() {
        return experience;
    }

    /** The primary product as a fresh stack. */
    public ItemStack getResultItem() {
        return result.toStack();
    }

    @Override
    public boolean matches(@NotNull SingleRecipeInput input, @NotNull Level level) {
        return ingredient.test(input.item()) && input.item().getCount() >= ingredientCount;
    }

    @Override
    public @NotNull String group() {
        return "";
    }

    @Override
    protected @NotNull ItemStack assembleResult() {
        return result.toStack();
    }

    @Override
    public @NotNull RecipeSerializer<SawmillRecipe> getSerializer() {
        return SawmillRecipeSerializer.INSTANCE;
    }

    @Override
    public @NotNull RecipeType<SawmillRecipe> getType() {
        return LogisticsAutomation.RECIPE.SAWMILL_RECIPE_TYPE;
    }

    @Override
    public boolean isSpecial() {
        return false;
    }

    @Override
    public boolean showNotification() {
        return true;
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
        return LogisticsAutomation.RECIPE.SAWMILL_CATEGORY;
    }

    @Override
    public @NotNull List<RecipeDisplay> display() {
        return List.of(new SawmillRecipeDisplay(
            ingredient.display(),
            result.slotDisplay(),
            new SlotDisplay.ItemSlotDisplay(LogisticsAutomation.BLOCK.SAWMILL.asItem()),
            energy,
            experience
        ));
    }
}
