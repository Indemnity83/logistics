package com.logistics.automation.macerator;

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
 * Resolves the active {@link MaceratorRecipeWrapper} for the machine's input via the vanilla
 * {@code RecipeManager}, translating it into an RF-cost {@link RecipePlan}.
 */
public final class MaceratorRecipeResolver implements RecipeResolver {

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
                .getRecipeFor(LogisticsAutomation.RECIPE.MACERATOR_RECIPE_TYPE, new SingleRecipeInput(input), level)
                .map(holder -> new RecipePlan(
                        holder.value().energyRequired(), holder.value().getResultItem(), holder.value().experience()))
                .orElse(null);
    }
}
