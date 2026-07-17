package com.logistics.core.lib.compat;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;

/**
 * Hides the cross-version arity of {@link Recipe#assemble} at call sites that invoke a
 * <em>vanilla</em> recipe (which can't route through {@link
 * com.logistics.core.lib.recipe.AbstractLogisticsRecipe}). The call is {@code assemble(input)} on
 * mc/26.x and {@code assemble(input, level.registryAccess())} on mc/1.21.x.
 *
 * <p>Per branch, only this file carries the matching call; the mc/1.21.x form passes
 * {@code level.registryAccess()} as the second argument.
 */
public final class RecipeCompat {

    private RecipeCompat() {}

    public static <I extends RecipeInput> ItemStack assemble(Recipe<I> recipe, I input, Level level) {
        return recipe.assemble(input, level.registryAccess());
    }
}
