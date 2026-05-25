package com.logistics.neoforge.energy;

import com.logistics.core.lib.energy.EnergyCapabilityLookup;
import com.logistics.core.lib.energy.IEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;

/**
 * NeoForge 21.1 implementation of {@link EnergyCapabilityLookup}.
 *
 * <p>Queries the NeoForge {@code Capabilities.EnergyStorage.BLOCK} capability and wraps
 * the result in a {@link NeoForgeEnergyStorage} adapter for use by common code.
 */
public final class NeoForgeEnergyCapabilityLookup implements EnergyCapabilityLookup {

    @Override
    public @Nullable IEnergyStorage find(Level level, BlockPos pos, Direction side) {
        return NeoForgeEnergyStorage.wrap(level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side));
    }
}
