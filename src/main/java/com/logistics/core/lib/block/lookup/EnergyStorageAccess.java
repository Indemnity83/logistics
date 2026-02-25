package com.logistics.core.lib.block.lookup;

import com.logistics.core.lib.block.capability.HasEnergyStorage;
import team.reborn.energy.api.EnergyStorage;

/**
 * Bridges internal {@link HasEnergyStorage} interface to Team Reborn Energy's lookup API.
 * <p>
 * Any block entity implementing HasEnergyStorage will automatically work with:
 * <ul>
 *   <li>Tech Reborn machines</li>
 *   <li>Other mods using Team Reborn Energy API</li>
 *   <li>This mod's own energy network</li>
 * </ul>
 * <p>
 * Call {@link #register()} once during mod initialization.
 */
public final class EnergyStorageAccess {
    private EnergyStorageAccess() {}

    /**
     * Register the fallback adapter that exposes HasEnergyStorage block entities
     * to Team Reborn Energy's lookup system.
     */
    public static void register() {
        EnergyStorage.SIDED.registerFallback((world, pos, state, blockEntity, direction) -> {
            if (blockEntity instanceof HasEnergyStorage hasStorage) {
                return hasStorage.energyStorage(direction);
            }
            return null;
        });
    }
}
