package com.logistics.automation.refinery;

import com.logistics.LogisticsAutomation;
import com.logistics.core.lib.recipe.FluidResult;
import com.logistics.core.lib.recipe.RecipeByproduct;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Refinery recipe: a fluid input distilled into a fluid output with an optional chance-based item
 * byproduct, using RF energy. Integrated into Minecraft's recipe system via {@link RecipeType} and
 * {@link RecipeSerializer}. Because the input is a fluid (not an item), the machine never looks these up
 * through the item-keyed {@code RecipeManager}; {@link RefineryRecipeResolver} scans them and matches on
 * the input tank's fluid. The item-facing {@link Recipe} methods therefore behave as a non-placeable,
 * non-item recipe.
 */
public class RefineryRecipe implements Recipe<SingleRecipeInput> {

    public static final float DEFAULT_EXPERIENCE = 0.0f;

    private final FluidResult input;
    private final FluidResult result;
    private final Optional<RecipeByproduct> byproduct;
    private final int energy;
    private final float experience;

    public RefineryRecipe(
            FluidResult input,
            FluidResult result,
            Optional<RecipeByproduct> byproduct,
            int energy,
            float experience) {
        if (energy <= 0) {
            throw new IllegalArgumentException("energy must be positive, got " + energy);
        }
        if (!Float.isFinite(experience) || experience < 0) {
            throw new IllegalArgumentException("experience must be finite and non-negative, got " + experience);
        }
        this.input = input;
        this.result = result;
        this.byproduct = byproduct;
        this.energy = energy;
        this.experience = experience;
    }

    /** The fluid this recipe consumes from the machine's input tank. */
    public FluidResult input() {
        return input;
    }

    /** The fluid this recipe outputs into the machine's output tank. */
    public FluidResult result() {
        return result;
    }

    /** The optional chance-based item byproduct (e.g. tar). */
    public Optional<RecipeByproduct> byproduct() {
        return byproduct;
    }

    /** Total energy (RF) the machine must spend to complete this recipe. */
    public int energy() {
        return energy;
    }

    public float experience() {
        return experience;
    }

    @Override
    public boolean matches(@NotNull SingleRecipeInput input, @NotNull Level level) {
        // Fluid-input recipe — never matched via the item-keyed RecipeManager; the resolver scans instead.
        return false;
    }

    @Override
    public @NotNull String group() {
        return "";
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull SingleRecipeInput input) {
        // Fluid-only output — nothing goes into an item slot.
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull RecipeSerializer<RefineryRecipe> getSerializer() {
        return RefineryRecipeSerializer.INSTANCE;
    }

    @Override
    public @NotNull RecipeType<RefineryRecipe> getType() {
        return LogisticsAutomation.RECIPE.REFINERY_RECIPE_TYPE;
    }

    @Override
    public boolean isSpecial() {
        // No item ingredient to place into a crafting grid; keeps it out of the recipe book.
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public @NotNull PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public @NotNull RecipeBookCategory recipeBookCategory() {
        return LogisticsAutomation.RECIPE.REFINERY_CATEGORY;
    }

    @Override
    public @NotNull List<RecipeDisplay> display() {
        return List.of();
    }
}
