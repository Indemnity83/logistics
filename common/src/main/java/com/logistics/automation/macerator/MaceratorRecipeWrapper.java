package com.logistics.automation.macerator;

import com.logistics.LogisticsAutomation;
import com.logistics.core.lib.recipe.AbstractLogisticsRecipe;
import com.logistics.core.lib.recipe.ItemResult;
import com.logistics.core.lib.recipe.RecipeByproduct;
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

import java.util.List;
import java.util.Optional;

/**
 * Macerator recipe, integrated into Minecraft's recipe system via {@link RecipeType} and
 * {@link RecipeSerializer}. Vanilla's {@code RecipeManager} loads these by their
 * {@code logistics:macerator} type, so recipes are routed correctly and other mods'
 * {@code recipe/macerator/} files are never absorbed into our machine.
 *
 * <p>Both the machine ({@code MaceratorBlockEntity}) and recipe display read this directly.
 */
public class MaceratorRecipeWrapper extends AbstractLogisticsRecipe<SingleRecipeInput> {

    public static final int DEFAULT_ENERGY = 2000;
    public static final float DEFAULT_EXPERIENCE = 0.0f;
    public static final int DEFAULT_INGREDIENT_COUNT = 1;

    private final Ingredient ingredient;
    private final int ingredientCount;
    private final ItemResult result;
    private final int energy;
    private final float experience;
    private final Optional<RecipeByproduct> byproduct;
    @Nullable private PlacementInfo placementInfo;

    public MaceratorRecipeWrapper(
            Ingredient ingredient, int ingredientCount, ItemResult result, int energy, float experience,
            Optional<RecipeByproduct> byproduct) {
        if (energy <= 0) {
            throw new IllegalArgumentException("energy must be positive, got " + energy);
        }
        if (ingredientCount < 1) {
            throw new IllegalArgumentException("ingredientCount must be positive, got " + ingredientCount);
        }
        if (!Float.isFinite(experience) || experience < 0) {
            throw new IllegalArgumentException("experience must be finite and non-negative, got " + experience);
        }
        this.ingredient = ingredient;
        this.ingredientCount = ingredientCount;
        this.result = result;
        this.energy = energy;
        this.experience = experience;
        this.byproduct = byproduct;
    }

    public Ingredient ingredient() {
        return ingredient;
    }

    /** How many input items one craft consumes (e.g. 2 cinnabar slabs → 1 quicksilver). */
    public int ingredientCount() {
        return ingredientCount;
    }

    public ItemResult result() {
        return result;
    }

    /** Total energy (RF) the machine must spend to complete this recipe. */
    public int energy() {
        return energy;
    }

    public float experience() {
        return experience;
    }

    /** The chance-based byproduct, if this recipe has one (ore→dust recipes do). */
    public Optional<RecipeByproduct> byproduct() {
        return byproduct;
    }

    /** Convenience for machine logic: does this stack satisfy the ingredient? */
    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && ingredient.test(stack);
    }

    /** Convenience for machine/JEI: the result as a fresh stack (avoids the version-specific assemble signature). */
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
    public @NotNull RecipeSerializer<MaceratorRecipeWrapper> getSerializer() {
        return MaceratorRecipeSerializer.INSTANCE;
    }

    @Override
    public @NotNull RecipeType<MaceratorRecipeWrapper> getType() {
        return LogisticsAutomation.RECIPE.MACERATOR_RECIPE_TYPE;
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
        return LogisticsAutomation.RECIPE.MACERATOR_CATEGORY;
    }

    @Override
    public @NotNull List<RecipeDisplay> display() {
        return List.of(new MaceratorRecipeDisplay(
            ingredient.display(),
            result.slotDisplay(),
            new SlotDisplay.ItemSlotDisplay(LogisticsAutomation.BLOCK.MACERATOR.asItem()),
            energy,
            experience
        ));
    }
}
