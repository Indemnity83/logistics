package com.logistics.fabric;

import com.logistics.core.lib.power.FuelHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class FabricFuelHelper implements FuelHelper {

    @Override
    public boolean isFuel(Level level, ItemStack stack) {
        return level.fuelValues().isFuel(stack);
    }

    @Override
    public int getBurnDuration(Level level, ItemStack stack) {
        return level.fuelValues().burnDuration(stack);
    }
}
