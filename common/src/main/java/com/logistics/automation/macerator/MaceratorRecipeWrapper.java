package com.logistics.automation.macerator;

import com.logistics.LogisticsAutomation;
import com.logistics.core.lib.recipe.MachineResult;
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
import java.util.Optional;

/**
 * Macerator recipe, integrated into Minecraft's recipe system via {@link RecipeType} and
 * {@link RecipeSerializer}. Vanilla's {@code RecipeManager} loads these by their
 * {@code logistics:macerator} type, so recipes are routed correctly and other mods'
 * {@code recipe/macerator/} files are never absorbed into our machine.
 *
 * <p>Both the machine ({@code MaceratorBlockEntity}) and recipe display read this directly.
 */
public class MaceratorRecipeWrapper implements Recipe<SingleRecipeInput> {

    public static final int DEFAULT_ENERGY_REQUIRED = 2000;
    public static final float DEFAULT_EXPERIENCE = 0.0f;

    private final Ingredient ingredient;
    private final MachineResult result;
    private final int energyRequired;
    private final float experience;
    private final Optional<MaceratorByproduct> byproduct;
    @Nullable private PlacementInfo placementInfo;

    public MaceratorRecipeWrapper(
            Ingredient ingredient, MachineResult result, int energyRequired, float experience,
            Optional<MaceratorByproduct> byproduct) {
        if (energyRequired <= 0) {
            throw new IllegalArgumentException("energyRequired must be positive, got " + energyRequired);
        }
        this.ingredient = ingredient;
        this.result = result;
        this.energyRequired = energyRequired;
        this.experience = experience;
        this.byproduct = byproduct;
    }

    public Ingredient ingredient() {
        return ingredient;
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
        return ingredient.test(input.item());
    }

    @Override
    public @NotNull String group() {
        return "";
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull SingleRecipeInput input) {
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
            energyRequired,
            experience
        ));
    }
}
