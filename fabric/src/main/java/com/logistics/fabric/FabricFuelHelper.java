package com.logistics.fabric;

import com.logistics.core.lib.power.FuelHelper;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class FabricFuelHelper implements FuelHelper {

    @Override
    public boolean checkIsFuel(Level level, ItemStack stack) {
        return FuelRegistry.INSTANCE.get(stack.getItem()) != null;
    }

    @Override
    public int checkBurnDuration(Level level, ItemStack stack) {
        Integer ticks = FuelRegistry.INSTANCE.get(stack.getItem());
        return ticks != null ? ticks : 0;
    }
}
