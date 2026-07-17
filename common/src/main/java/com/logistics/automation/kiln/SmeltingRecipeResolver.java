package com.logistics.automation.kiln;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsConfigHost;
import com.logistics.core.lib.compat.RecipeCompat;
import com.logistics.core.machine.MachineContext;
import com.logistics.core.machine.component.ProcessIO;
import com.logistics.core.machine.component.RecipePlan;
import com.logistics.core.machine.component.RecipeResolver;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves a vanilla smelting recipe for the machine's input. Smelting recipes carry no RF cost,
 * so the energy required is derived from the recipe's cooking time
 * ({@code energyRequired = cookingTime × rf_per_cook_tick}); the machine then spends its RF/tick
 * rate toward that total. At the default rate a standard 200-tick vanilla recipe costs 2,000 RF.
 */
public final class SmeltingRecipeResolver implements RecipeResolver {

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
        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        return recipeManager
                .getRecipeFor(RecipeType.SMELTING, recipeInput, level)
                .map(holder -> new RecipePlan(
                        holder.value().getCookingTime() * LogisticsConfigHost.get(LogisticsAutomation.CONFIG.KILN_RF_PER_COOK_TICK),
                        RecipeCompat.assemble(holder.value(), recipeInput, level),
                        holder.value().getExperience()))
                .orElse(null);
    }
}
