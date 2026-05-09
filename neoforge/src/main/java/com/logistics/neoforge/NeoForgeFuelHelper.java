package com.logistics.neoforge;

import com.logistics.core.lib.power.FuelHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class NeoForgeFuelHelper implements FuelHelper {

    @Override
    public boolean checkIsFuel(Level level, ItemStack stack) {
        return level.fuelValues().isFuel(stack);
    }

    @Override
    public int checkBurnDuration(Level level, ItemStack stack) {
        return level.fuelValues().burnDuration(stack);
    }
}
