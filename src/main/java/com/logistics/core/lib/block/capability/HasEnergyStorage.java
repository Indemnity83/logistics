package com.logistics.core.lib.block.capability;

import com.logistics.core.lib.energy.EnergyComponent;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;

/**
 * Marker interface for block entities that expose energy storage via Team Reborn Energy API.
 * <p>
 * Use {@link EnergyComponent} to implement this easily.
 */
public interface HasEnergyStorage {
    /**
     * Get energy storage for the specified side.
     * Return null if the side doesn't support energy access.
     *
     * @param side The side to access, or null for non-sided access
     * @return The energy storage, or null if not accessible from this side
     */
    @Nullable EnergyStorage energyStorage(@Nullable Direction side);
}