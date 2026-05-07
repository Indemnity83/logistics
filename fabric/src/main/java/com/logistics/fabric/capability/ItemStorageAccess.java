package com.logistics.fabric.capability;

import com.logistics.core.lib.block.capability.HasItemStorage;
import com.logistics.fabric.storage.FabricItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;

/**
 * Bridges the loader-agnostic {@link HasItemStorage} interface to Fabric's ItemStorage lookup API.
 * <p>
 * Any block entity implementing HasItemStorage will automatically work with:
 * <ul>
 *   <li>Vanilla automation (hoppers)</li>
 *   <li>Fabric mod pipes and storage</li>
 *   <li>This mod's own pipes</li>
 * </ul>
 * <p>
 * Call {@link #register()} once during mod initialization.
 */
public final class ItemStorageAccess {
    private ItemStorageAccess() {}

    /**
     * Register the fallback adapter that exposes HasItemStorage block entities
     * to Fabric's ItemStorage lookup system.
     */
    public static void register() {
        ItemStorage.SIDED.registerFallback((world, pos, state, blockEntity, direction) -> {
            if (blockEntity instanceof HasItemStorage hasStorage) {
                return FabricItemStorage.asFabric(hasStorage.itemStorage(direction));
            }
            return null;
        });
    }
}
