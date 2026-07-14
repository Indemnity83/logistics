package com.logistics.automation.refinery;

import com.logistics.core.lib.recipe.RecipeByproduct;
import com.logistics.core.machine.MachineContext;
import com.logistics.core.machine.component.ChanceOutput;
import com.logistics.core.machine.component.FluidStoreComponent;
import com.logistics.core.machine.component.ProcessIO;
import com.logistics.core.machine.component.RecipePlan;
import com.logistics.core.machine.component.RecipeResolver;
import java.util.List;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves the active {@link RefineryRecipe} from the fluid in the machine's input tank. Because refinery
 * recipes are keyed on a fluid (not an item), they can't be looked up through the item-keyed
 * {@code RecipeManager}; this scans the full recipe set and matches on the input fluid — the same
 * loader-agnostic approach the alloy smelter uses ({@code recipeMap().byType()} is NeoForge-only).
 */
public final class RefineryRecipeResolver implements RecipeResolver {

    private final FluidStoreComponent inputTank;

    public RefineryRecipeResolver(FluidStoreComponent inputTank) {
        this.inputTank = inputTank;
    }

    @Override
    @Nullable
    public RecipePlan resolve(ProcessIO io, MachineContext ctx) {
        if (inputTank.tank().isEmpty()) {
            return null;
        }
        RecipeManager recipeManager = ctx.recipeManager();
        Level level = ctx.level();
        if (recipeManager == null || level == null) {
            return null;
        }
        Fluid stored = inputTank.tank().getFluidKey().getFluid();
        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            if (holder.value() instanceof RefineryRecipe recipe && recipe.input().fluid() == stored) {
                List<ChanceOutput> byproducts = recipe.byproduct()
                        .map(RefineryRecipeResolver::chanceOutput)
                        .orElse(List.of());
                return new RecipePlan(
                        recipe.energy(), recipe.input(), recipe.result(), byproducts, recipe.experience());
            }
        }
        return null;
    }

    private static List<ChanceOutput> chanceOutput(RecipeByproduct byproduct) {
        return List.of(new ChanceOutput(byproduct.stack(1), byproduct.chance()));
    }
}
