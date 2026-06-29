package com.logistics.automation.macerator;

import com.logistics.LogisticsAutomation;
import com.logistics.core.lib.recipe.MachineResult;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Macerator recipe, integrated into Minecraft's recipe system via {@link RecipeType} and
 * {@link RecipeSerializer}. Vanilla's {@code RecipeManager} loads these by their
 * {@code logistics:macerator} type, so recipes are routed correctly and other mods'
 * {@code recipe/macerator/} files are never absorbed into our machine.
 *
 * <p>RF-cost based: {@code energyRequired} is the total energy the machine spends to complete the
 * recipe. Both the machine ({@code MaceratorBlockEntity}) and JEI read this directly.
 */
public class MaceratorRecipeWrapper implements Recipe<SingleRecipeInput> {

    public static final int DEFAULT_ENERGY_REQUIRED = 2000;
    public static final float DEFAULT_EXPERIENCE = 0.0f;
    public static final int DEFAULT_INGREDIENT_COUNT = 1;

    private final Ingredient ingredient;
    private final int ingredientCount;
    private final MachineResult result;
    private final int energyRequired;
    private final float experience;
    private final Optional<MaceratorByproduct> byproduct;

    public MaceratorRecipeWrapper(
            Ingredient ingredient, int ingredientCount, MachineResult result, int energyRequired, float experience,
            Optional<MaceratorByproduct> byproduct) {
        if (energyRequired <= 0) {
            throw new IllegalArgumentException("energyRequired must be positive, got " + energyRequired);
        }
        if (ingredientCount < 1) {
            throw new IllegalArgumentException("ingredientCount must be positive, got " + ingredientCount);
        }
        this.ingredient = ingredient;
        this.ingredientCount = ingredientCount;
        this.result = result;
        this.energyRequired = energyRequired;
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

    public MachineResult result() {
        return result;
    }

    /** Total energy (RF) the machine must spend to complete this recipe. */
    public int energyRequired() {
        return energyRequired;
    }

    public float experience() {
        return experience;
    }

    /** The chance-based byproduct, if this recipe has one (ore→dust recipes do). */
    public Optional<MaceratorByproduct> byproduct() {
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
    public @NotNull ItemStack assemble(@NotNull SingleRecipeInput input, HolderLookup.@NotNull Provider provider) {
        return result.toStack();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider provider) {
        return result.toStack();
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
        return false;
    }
}
