package com.logistics.neoforge.energy;

import com.logistics.core.lib.energy.EnergyCapabilityLookup;
import com.logistics.core.lib.energy.IEnergyStorage;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * NeoForge implementation of {@link EnergyCapabilityLookup}.
 */
public final class NeoForgeEnergyCapabilityLookup implements EnergyCapabilityLookup {

    @Override
    public @Nullable IEnergyStorage find(Level level, BlockPos pos, Direction side) {
        return NeoForgeEnergyStorage.wrap(level.getCapability(Capabilities.Energy.BLOCK, pos, side));
    }
}
