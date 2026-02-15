package com.logistics.core.lib.entity;

import com.logistics.core.lib.fluids.FluidTankComponent;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;

/**
 * Marker interface for block entities that expose fluid storage via the Transfer API.
 * <p>
 * Use {@link FluidTankComponent} to implement this easily.
 */
public interface HasFluidStorage {
    Storage<FluidVariant> fluidStorage();
}