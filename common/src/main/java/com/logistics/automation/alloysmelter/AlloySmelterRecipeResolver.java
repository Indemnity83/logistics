package com.logistics.automation.alloysmelter;

import com.logistics.LogisticsAutomation;
import com.logistics.core.machine.MachineContext;
import com.logistics.core.machine.component.ChanceOutput;
import com.logistics.core.machine.component.ProcessIO;
import com.logistics.core.machine.component.RecipePlan;
import com.logistics.core.machine.component.RecipeResolver;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves the active {@link AlloySmelterRecipe} for the machine's two inputs via the vanilla
 * {@code RecipeManager}, translating it into an RF-cost {@link RecipePlan}. Per-slot consume counts
 * follow the physical placement so the inputs are order-independent.
 */
public final class AlloySmelterRecipeResolver implements RecipeResolver {

    @Override
    @Nullable
    public RecipePlan resolve(ProcessIO io, MachineContext ctx) {
        ItemStack a = io.input(0);
        ItemStack b = io.input(1);
        if (a.isEmpty() || b.isEmpty()) {
            return null;
        }
        RecipeManager recipeManager = ctx.recipeManager();
        Level level = ctx.level();
        if (recipeManager == null || level == null) {
            return null;
        }
        return recipeManager
                .getRecipeFor(LogisticsAutomation.RECIPE.ALLOY_SMELTER_RECIPE_TYPE, new DualRecipeInput(a, b), level)
                .map(holder -> {
                    AlloySmelterRecipe recipe = holder.value();
                    List<ChanceOutput> byproducts = recipe.byproduct()
                            .map(x -> List.of(new ChanceOutput(x.stack(1), x.chance())))
                            .orElse(List.of());
                    return new RecipePlan(
                            recipe.energy(),
                            recipe.consumptionForSlots(a, b),
                            recipe.getResultItem(),
                            byproducts,
                            recipe.experience());
                })
                .orElse(null);
    }
}
