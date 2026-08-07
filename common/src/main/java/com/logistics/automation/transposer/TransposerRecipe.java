package com.logistics.automation.transposer;

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
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Transposer recipe: an item plus one signed fluid amount ({@link SignedFluidAmount}) converted into an
 * optional result item, using RF energy. Fill mode (negative amount) drains the fluid from the tank;
 * Empty mode (positive amount) deposits it — e.g. an empty bucket + −1000 mB lava → a lava bucket, or a
 * lava bucket + 1000 mB lava → an empty bucket. The item result is absent for recipes that only consume
 * an item (e.g. cactus + 2400 RF → 500 mB water, with the cactus simply gone). Integrated into
 * Minecraft's recipe system via {@link RecipeType} and {@link RecipeSerializer}. Because matching also
 * depends on the tank's current fluid (not just the input item), the machine never looks these up
 * through the item-keyed {@code RecipeManager}; {@link TransposerRecipeResolver} scans them instead, the
 * same approach the refinery uses.
 */
public class TransposerRecipe extends AbstractLogisticsRecipe<SingleRecipeInput> {

    public static final float DEFAULT_EXPERIENCE = 0.0f;
    public static final int DEFAULT_INGREDIENT_COUNT = 1;

    private final Ingredient input;
    private final int inputCount;
    private final Optional<ItemResult> result;
    private final SignedFluidAmount fluid;
    private final int energy;
    private final float experience;
    private final Optional<RecipeByproduct> byproduct;

    public TransposerRecipe(
            Ingredient input,
            int inputCount,
            Optional<ItemResult> result,
            SignedFluidAmount fluid,
            int energy,
            float experience,
            Optional<RecipeByproduct> byproduct) {
        if (energy <= 0) {
            throw new IllegalArgumentException("energy must be positive, got " + energy);
        }
        if (inputCount < 1) {
            throw new IllegalArgumentException("inputCount must be positive, got " + inputCount);
        }
        if (!Float.isFinite(experience) || experience < 0) {
            throw new IllegalArgumentException("experience must be finite and non-negative, got " + experience);
        }
        this.input = input;
        this.inputCount = inputCount;
        this.result = result;
        this.fluid = fluid;
        this.energy = energy;
        this.experience = experience;
        this.byproduct = byproduct;
    }

    public Ingredient input() {
        return input;
    }

    /** How many input items one run consumes (buckets always take 1). */
    public int inputCount() {
        return inputCount;
    }

    /** The item result, absent for a recipe that only consumes its input (e.g. cactus → water). */
    public Optional<ItemResult> result() {
        return result;
    }

    /** The result as a fresh stack, or {@link ItemStack#EMPTY} when this recipe has no item result. */
    public ItemStack resultStack() {
        return result.map(ItemResult::toStack).orElse(ItemStack.EMPTY);
    }

    /** The fluid side of this recipe, signed relative to the machine's tank. */
    public SignedFluidAmount fluid() {
        return fluid;
    }

    /** Total energy (RF) the machine must spend to complete this recipe. */
    public int energy() {
        return energy;
    }

    public float experience() {
        return experience;
    }

    public Optional<RecipeByproduct> byproduct() {
        return byproduct;
    }

    @Override
    public boolean matches(@NotNull SingleRecipeInput input, @NotNull Level level) {
        // Matching also depends on the tank's current fluid — never matched via the item-keyed
        // RecipeManager; the resolver scans instead.
        return false;
    }

    @Override
    public @NotNull String group() {
        return "";
    }

    @Override
    protected @NotNull ItemStack assembleResult() {
        return resultStack();
    }

    @Override
    public @NotNull RecipeSerializer<TransposerRecipe> getSerializer() {
        return TransposerRecipeSerializer.INSTANCE;
    }

    @Override
    public @NotNull RecipeType<TransposerRecipe> getType() {
        return LogisticsAutomation.RECIPE.TRANSPOSER_RECIPE_TYPE;
    }

    @Override
    public boolean isSpecial() {
        // The tank-fluid requirement can't be placed in a crafting grid; keeps it out of the recipe book.
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
        return LogisticsAutomation.RECIPE.TRANSPOSER_CATEGORY;
    }

    @Override
    public @NotNull List<RecipeDisplay> display() {
        return List.of();
    }
}
