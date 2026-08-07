package com.logistics.automation.transposer;

import com.logistics.core.lib.recipe.FluidResult;
import com.logistics.core.lib.recipe.RecipeByproduct;
import com.logistics.core.machine.MachineContext;
import com.logistics.core.machine.component.ChanceOutput;
import com.logistics.core.machine.component.FluidStoreComponent;
import com.logistics.core.machine.component.ProcessIO;
import com.logistics.core.machine.component.RecipePlan;
import com.logistics.core.machine.component.RecipeResolver;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves the active {@link TransposerRecipe} for the machine's input item and tank. Because matching
 * also depends on the tank's current fluid for Fill-mode recipes (not just the input item), these can't
 * be looked up through the item-keyed {@code RecipeManager}; this scans the full recipe set instead —
 * the same loader-agnostic approach the refinery uses ({@code recipeMap().byType()} is NeoForge-only).
 */
public final class TransposerRecipeResolver implements RecipeResolver {

    private final FluidStoreComponent tank;

    public TransposerRecipeResolver(FluidStoreComponent tank) {
        this.tank = tank;
    }

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
        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            if (!(holder.value() instanceof TransposerRecipe recipe)) {
                continue;
            }
            if (!recipe.input().test(input)) {
                continue;
            }
            SignedFluidAmount fluid = recipe.fluid();
            // Fill-mode recipes need the tank already holding this fluid to be a candidate at all;
            // the exact amount is checked generically by RecipeProcessorComponent.canRun afterward.
            if (fluid.isInput() && (tank.tank().isEmpty() || tank.tank().getFluidKey().getFluid() != fluid.fluid())) {
                continue;
            }
            return toPlan(recipe);
        }
        return null;
    }

    private static RecipePlan toPlan(TransposerRecipe recipe) {
        List<ChanceOutput> byproducts = recipe.byproduct()
                .map(TransposerRecipeResolver::chanceOutput)
                .orElse(List.of());
        FluidResult fluidResult = recipe.fluid().toFluidResult();
        return new RecipePlan(
                recipe.energy(),
                new int[] {recipe.inputCount()},
                recipe.result().toStack(),
                byproducts,
                recipe.experience(),
                recipe.fluid().isInput() ? null : fluidResult,
                recipe.fluid().isInput() ? fluidResult : null);
    }

    private static List<ChanceOutput> chanceOutput(RecipeByproduct byproduct) {
        return List.of(new ChanceOutput(byproduct.stack(1), byproduct.chance()));
    }
}
