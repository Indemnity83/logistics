package com.logistics.neoforge.energy;

import com.logistics.core.lib.energy.EnergyCapabilityLookup;
import com.logistics.core.lib.energy.IEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * NeoForge stub implementation of {@link EnergyCapabilityLookup}.
 *
 * <p>TODO: implement via NeoForge's {@code Capabilities.EnergyStorage} capability lookup,
 * wrapping the found {@code IEnergyStorage} (NeoForge) in an {@link IEnergyStorage} adapter.
 */
public final class NeoForgeEnergyCapabilityLookup implements EnergyCapabilityLookup {

    @Override
    public @Nullable IEnergyStorage find(Level level, BlockPos pos, Direction side) {
        // TODO(neoforge): implement via Capabilities.EnergyStorage.getBlockCapability(level, pos, state, blockEntity, side)
        return null;
    }
}
