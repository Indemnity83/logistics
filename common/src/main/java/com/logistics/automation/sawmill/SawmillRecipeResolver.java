package com.logistics.automation.sawmill;

import com.logistics.LogisticsAutomation;
import com.logistics.core.machine.MachineContext;
import com.logistics.core.machine.component.ChanceOutput;
import com.logistics.core.machine.component.ProcessIO;
import com.logistics.core.machine.component.RecipePlan;
import com.logistics.core.machine.component.RecipeResolver;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves the active {@link SawmillRecipe} for the machine's input via the vanilla
 * {@code RecipeManager}, translating it into an RF-cost {@link RecipePlan} with the chance-based
 * byproduct as a {@link ChanceOutput}.
 */
public final class SawmillRecipeResolver implements RecipeResolver {

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
                .getRecipeFor(LogisticsAutomation.RECIPE.SAWMILL_RECIPE_TYPE, new SingleRecipeInput(input), level)
                .map(holder -> {
                    SawmillRecipe recipe = holder.value();
                    List<ChanceOutput> byproducts = recipe.byproduct()
                            .map(b -> List.of(new ChanceOutput(b.stack(1), b.chance())))
                            .orElse(List.of());
                    return new RecipePlan(
                            recipe.energy(),
                            recipe.ingredientCount(),
                            recipe.getResultItem(),
                            byproducts,
                            0f);
                })
                .orElse(null);
    }
}
