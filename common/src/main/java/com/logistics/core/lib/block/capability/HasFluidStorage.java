package com.logistics.core.lib.block.capability;

import com.logistics.core.lib.fluids.FluidTankComponent;
import com.logistics.core.lib.fluids.IFluidStorage;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * Marker interface for block entities that expose fluid storage.
 * <p>
 * Use {@link FluidTankComponent} to implement this easily.
 */
public interface HasFluidStorage {
    /**
     * Get fluid storage for the specified side.
     * Return null if the side doesn't support fluid access.
     *
     * @param side The side to access, or null for non-sided access
     * @return The fluid storage, or null if not accessible from this side
     */
    @Nullable IFluidStorage fluidStorage(@Nullable Direction side);
}
