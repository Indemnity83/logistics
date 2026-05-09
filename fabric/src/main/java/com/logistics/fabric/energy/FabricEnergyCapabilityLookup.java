package com.logistics.fabric.energy;

import com.logistics.core.lib.energy.EnergyCapabilityLookup;
import com.logistics.core.lib.energy.IEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;

/**
 * Fabric implementation of {@link EnergyCapabilityLookup}.
 *
 * <p>Delegates to Team Reborn Energy's {@code EnergyStorage.SIDED} lookup.
 * If the found storage is already an {@link IEnergyStorage} (e.g. cables, engines),
 * it is returned directly. Otherwise it is wrapped in {@link TrToIEnergyStorageAdapter}
 * so that third-party machines are also accessible via the loader-agnostic API.
 */
public final class FabricEnergyCapabilityLookup implements EnergyCapabilityLookup {

    @Override
    public @Nullable IEnergyStorage find(Level level, BlockPos pos, Direction side) {
        EnergyStorage tr = EnergyStorage.SIDED.find(level, pos, side);
        if (tr == null) return null;
        if (tr instanceof IEnergyStorage ies) return ies;
        return new TrToIEnergyStorageAdapter(tr);
    }
}
