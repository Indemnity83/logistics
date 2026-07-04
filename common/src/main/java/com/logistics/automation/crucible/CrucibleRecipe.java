package com.logistics.automation.crucible;

import com.logistics.LogisticsAutomation;
import com.logistics.core.lib.recipe.FluidResult;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Crucible recipe: an input item melted into a fluid, integrated into Minecraft's recipe system
 * via {@link RecipeType} and {@link RecipeSerializer}. Vanilla's {@code RecipeManager} loads these by
 * their {@code logistics:crucible} type. The output is a {@link FluidResult} deposited into the
 * machine's tank — there is no item result.
 */
public class CrucibleRecipe implements Recipe<SingleRecipeInput> {

    public static final int DEFAULT_INGREDIENT_COUNT = 1;
    public static final float DEFAULT_EXPERIENCE = 0.0f;

    private final Ingredient ingredient;
    private final int ingredientCount;
    private final FluidResult result;
    private final int energy;
    private final float experience;

    public CrucibleRecipe(
            Ingredient ingredient, int ingredientCount, FluidResult result, int energy, float experience) {
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
    }

    public Ingredient ingredient() {
        return ingredient;
    }

    /** How many input items one melt consumes. */
    public int ingredientCount() {
        return ingredientCount;
    }

    /** The fluid this recipe outputs into the machine's tank. */
    public FluidResult result() {
        return result;
    }

    /** Total energy (RF) the machine must spend to complete this recipe. */
    public int energy() {
        return energy;
    }

    public float experience() {
        return experience;
    }

    /** Convenience for machine logic: does this stack satisfy the ingredient? */
    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && ingredient.test(stack);
    }

    @Override
    public boolean matches(@NotNull SingleRecipeInput input, @NotNull Level level) {
        return ingredient.test(input.item()) && input.item().getCount() >= ingredientCount;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull SingleRecipeInput input, HolderLookup.@NotNull Provider provider) {
        // Fluid-only output — nothing goes into an item slot.
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider provider) {
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull RecipeSerializer<CrucibleRecipe> getSerializer() {
        return CrucibleRecipeSerializer.INSTANCE;
    }

    @Override
    public @NotNull RecipeType<CrucibleRecipe> getType() {
        return LogisticsAutomation.RECIPE.CRUCIBLE_RECIPE_TYPE;
    }

    @Override
    public boolean isSpecial() {
        return false;
    }
}
