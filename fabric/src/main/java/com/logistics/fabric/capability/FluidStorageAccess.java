package com.logistics.fabric.capability;

import com.logistics.core.lib.block.capability.HasFluidStorage;
import com.logistics.fabric.fluids.FabricFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;

/**
 * Bridges internal {@link HasFluidStorage} interface to Fabric's FluidStorage lookup API.
 * <p>
 * Any block entity implementing HasFluidStorage will automatically work with:
 * <ul>
 *   <li>Fabric mod fluid pipes and tanks</li>
 *   <li>This mod's own fluid pipes (future)</li>
 * </ul>
 * <p>
 * Call {@link #register()} once during mod initialization.
 */
public final class FluidStorageAccess {
    private FluidStorageAccess() {}

    /**
     * Register the fallback adapter that exposes HasFluidStorage block entities
     * to Fabric's FluidStorage lookup system.
     */
    public static void register() {
        FluidStorage.SIDED.registerFallback((world, pos, state, blockEntity, direction) -> {
            if (blockEntity instanceof HasFluidStorage hasStorage) {
                return FabricFluidStorage.asFabric(hasStorage.fluidStorage(direction));
            }
            return null;
        });
    }
}
