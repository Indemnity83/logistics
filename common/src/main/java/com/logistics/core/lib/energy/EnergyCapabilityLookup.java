package com.logistics.core.lib.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ServiceLoader;

/**
 * Loader-provided lookup: find an {@link IEnergyStorage} exposed by a block from a given side.
 *
 * <p>Common code calls {@code EnergyCapabilityLookup.INSTANCE.find(level, pos, side)} to
 * discover energy storage on adjacent blocks. Each loader provides an implementation via
 * {@code META-INF/services/}:
 * <ul>
 *   <li>Fabric: delegates to {@code EnergyStorage.SIDED.find()} (Team Reborn Energy API)
 *   <li>NeoForge: delegates to NeoForge's {@code Capabilities.EnergyStorage} capability
 * </ul>
 */
public interface EnergyCapabilityLookup {

    /**
     * Find the {@link IEnergyStorage} exposed by the block at {@code pos} from {@code side},
     * or {@code null} if no energy storage is exposed.
     */
    @Nullable
    IEnergyStorage find(Level level, BlockPos pos, Direction side);

    EnergyCapabilityLookup INSTANCE = ServiceLoader.load(EnergyCapabilityLookup.class)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No EnergyCapabilityLookup found"));
}
