package com.logistics.automation.crucible;

import com.logistics.LogisticsAutomation;
import com.logistics.core.machine.MachineContext;
import com.logistics.core.machine.component.ProcessIO;
import com.logistics.core.machine.component.RecipePlan;
import com.logistics.core.machine.component.RecipeResolver;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves the active {@link MagmaCrucibleRecipe} for the machine's input via the vanilla
 * {@code RecipeManager}, translating it into a fluid-output {@link RecipePlan}.
 */
public final class MagmaCrucibleRecipeResolver implements RecipeResolver {

    @Override
    @Nullable
    public RecipePlan resolve(ProcessIO io, MachineContext ctx) {
        ItemStack input = io.input();
        if (input.isEmpty()) {
            return null;
        }
        RecipeManager recipeManager = ctx.recipeManager();
        Level level = ctx.level();
        if (recipeManager == null || level == null) {
            return null;
        }
        return recipeManager
                .getRecipeFor(LogisticsAutomation.RECIPE.MAGMA_CRUCIBLE_RECIPE_TYPE, new SingleRecipeInput(input), level)
                .map(holder -> {
                    MagmaCrucibleRecipe recipe = holder.value();
                    return new RecipePlan(
                            recipe.energyRequired(), recipe.ingredientCount(), recipe.result(), recipe.experience());
                })
                .orElse(null);
    }
}
