package com.logistics.neoforge;

import com.logistics.core.lib.power.FuelHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public final class NeoForgeFuelHelper implements FuelHelper {

    /**
     * NeoForge routes fuel through {@code ItemStack.getBurnTime}, which reads the
     * {@code neoforge:furnace_fuels} data map every mod-added fuel is declared in.
     * {@code AbstractFurnaceBlockEntity.getFuel()} is vanilla's static map and knows only vanilla's
     * own fuels, so it reports 0 for ours. Asked as a furnace would ask, so what burns here is
     * exactly what burns in a furnace.
     */
    private static int burnTime(ItemStack stack) {
        return stack.getBurnTime(RecipeType.SMELTING);
    }

    @Override
    public boolean checkIsFuel(Level level, ItemStack stack) {
        return burnTime(stack) > 0;
    }

    @Override
    public int checkBurnDuration(Level level, ItemStack stack) {
        return burnTime(stack);
    }
}
