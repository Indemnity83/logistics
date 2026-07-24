package com.logistics.power.engine.reaction;

import com.logistics.core.machine.MachineContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/**
 * The Reaction Engine's ignition resolver seam: given the tank's reactant fluid and the reagent stack,
 * return the matching {@link ReactionRecipe} or null. Injected (like the simulation's {@code ReactionOutput})
 * so the component is unit-testable without a live {@code RecipeManager}; the block entity supplies the real
 * implementation ({@link ReactionEngineReactions#find} against {@code ctx.recipeManager()}).
 */
@FunctionalInterface
public interface ReactionLookup {

    @Nullable
    ReactionRecipe find(MachineContext ctx, Fluid reactant, ItemStack reagent);
}
