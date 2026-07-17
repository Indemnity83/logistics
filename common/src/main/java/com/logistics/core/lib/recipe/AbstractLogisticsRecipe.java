package com.logistics.core.lib.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import org.jetbrains.annotations.NotNull;

/**
 * Base for the mod's machine recipes. Quarantines the sole cross-version divergence in the vanilla
 * {@link Recipe#assemble} override signature so subclasses stay byte-identical across branches: it
 * is {@code assemble(I)} on mc/26.x and {@code assemble(I, HolderLookup.Provider)} on mc/1.21.x.
 * Subclasses provide their output through the version-stable {@link #assembleResult()} hook and
 * never override {@code assemble} themselves.
 *
 * <p>Per branch, only this file carries the matching signature; the mc/1.21.x form adds the
 * {@code HolderLookup.Provider} parameter (and its import).
 */
public abstract class AbstractLogisticsRecipe<I extends RecipeInput> implements Recipe<I> {

    @Override
    public @NotNull ItemStack assemble(@NotNull I input) {
        return assembleResult();
    }

    /** The recipe's assembled item output (the mod's machine recipes ignore the input here). */
    protected abstract @NotNull ItemStack assembleResult();
}
