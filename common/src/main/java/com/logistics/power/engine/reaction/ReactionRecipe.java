package com.logistics.power.engine.reaction;

import com.logistics.LogisticsPower;
import com.logistics.core.lib.recipe.AbstractLogisticsRecipe;
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
 * Reaction Engine recipe: a liquid <b>reactant</b> ({@link FluidResult}, fluid + mB) plus a solid
 * <b>reagent</b> ({@link Ingredient} + count) that together release {@code energy} RF over {@code time}
 * ticks. Every axis lives in the datapack — the code hardcodes no material, amount, energy, or duration.
 * The instantaneous rate is <b>derived</b>: {@link #outputPerTick()} = {@code energy / time}.
 *
 * <p>Because the input is a fluid (not an item), the machine never looks these up through the item-keyed
 * {@code RecipeManager}; {@link ReactionEngineReactions} scans them and matches on the tank's fluid and the
 * reagent slot. The item-facing {@link Recipe} methods therefore behave as a non-placeable, non-item recipe
 * (mirrors {@code RefineryRecipe}).
 */
public class ReactionRecipe extends AbstractLogisticsRecipe<SingleRecipeInput> {

    private final FluidResult reactant;
    private final Ingredient reagent;
    private final int reagentCount;
    private final int energy;
    private final int time;

    public ReactionRecipe(FluidResult reactant, Ingredient reagent, int reagentCount, int energy, int time) {
        if (reagentCount <= 0) {
            throw new IllegalArgumentException("reagentCount must be positive, got " + reagentCount);
        }
        if (energy <= 0) {
            throw new IllegalArgumentException("energy must be positive, got " + energy);
        }
        if (time <= 0) {
            throw new IllegalArgumentException("time must be positive, got " + time);
        }
        this.reactant = reactant;
        this.reagent = reagent;
        this.reagentCount = reagentCount;
        this.energy = energy;
        this.time = time;
    }

    /** The liquid reactant (fluid + mB) one reaction consumes. */
    public FluidResult reactant() {
        return reactant;
    }

    /** The solid reagent one reaction consumes. */
    public Ingredient reagent() {
        return reagent;
    }

    /** How many reagent items one reaction consumes. */
    public int reagentCount() {
        return reagentCount;
    }

    /** Total RF the reaction releases across its full duration. */
    public int energy() {
        return energy;
    }

    /** How many ticks the reaction runs once committed. */
    public int time() {
        return time;
    }

    /** RF/t generated while reacting — derived from total energy over the reaction time (never stored). */
    public long outputPerTick() {
        return Math.max(1L, Math.round((double) energy / time));
    }

    @Override
    public boolean matches(@NotNull SingleRecipeInput input, @NotNull Level level) {
        // Fluid+reagent recipe — never matched via the item-keyed RecipeManager; the scan resolves it.
        return false;
    }

    @Override
    protected @NotNull ItemStack assembleResult() {
        // Produces energy, not an item.
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
    public @NotNull RecipeSerializer<ReactionRecipe> getSerializer() {
        return ReactionRecipeSerializer.INSTANCE;
    }

    @Override
    public @NotNull RecipeType<ReactionRecipe> getType() {
        return LogisticsPower.RECIPE.REACTION_RECIPE_TYPE;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }
}
