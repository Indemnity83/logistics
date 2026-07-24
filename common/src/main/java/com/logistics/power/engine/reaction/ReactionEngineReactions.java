package com.logistics.power.engine.reaction;

import com.logistics.core.machine.MachineContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/**
 * Datapack-driven reaction recognition: scans the loaded {@link ReactionRecipe}s (fluid-keyed, so they
 * can't go through the item-keyed {@code RecipeManager#getRecipeFor}). Serves both ignition (find the
 * matching recipe) and the machine's server-side input filters (which fluids/items participate in any
 * recipe). All lookups need a {@link RecipeManager}, so they return "no match" when one isn't available
 * (e.g. on the client, where the reaction engine simply doesn't filter its GUI slot — see
 * {@code ReactionEngineScreenHandler}).
 */
public final class ReactionEngineReactions {

    private ReactionEngineReactions() {}

    /** {@link ReactionLookup} implementation: resolve the reaction for a (reactant, reagent) pair. */
    @Nullable
    public static ReactionRecipe find(MachineContext ctx, Fluid reactant, ItemStack reagent) {
        return find(ctx.recipeManager(), reactant, reagent);
    }

    /** Resolve the reaction for a (reactant, reagent) pair, or null if none matches / no recipe manager. */
    @Nullable
    public static ReactionRecipe find(@Nullable RecipeManager recipeManager, Fluid reactant, ItemStack reagent) {
        if (recipeManager == null || reactant == Fluids.EMPTY || reagent.isEmpty()) {
            return null;
        }
        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            if (holder.value() instanceof ReactionRecipe recipe
                    && recipe.reactant().fluid() == reactant
                    && recipe.reagent().test(reagent)
                    && reagent.getCount() >= recipe.reagentCount()) {
                return recipe;
            }
        }
        return null;
    }

    /** Whether a fluid participates in any reaction — the tank insert filter (server-side). */
    public static boolean isReactant(@Nullable RecipeManager recipeManager, @Nullable Fluid fluid) {
        if (recipeManager == null || fluid == null || fluid == Fluids.EMPTY) {
            return false;
        }
        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            if (holder.value() instanceof ReactionRecipe recipe && recipe.reactant().fluid() == fluid) {
                return true;
            }
        }
        return false;
    }

    /** Whether an item participates in any reaction — the reagent-slot filter (server-side). */
    public static boolean isCatalyst(@Nullable RecipeManager recipeManager, ItemStack stack) {
        if (recipeManager == null || stack.isEmpty()) {
            return false;
        }
        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            if (holder.value() instanceof ReactionRecipe recipe && recipe.reagent().test(stack)) {
                return true;
            }
        }
        return false;
    }
}
