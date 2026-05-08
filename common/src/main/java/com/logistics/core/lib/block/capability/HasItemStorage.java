package com.logistics.core.lib.block.capability;

import com.logistics.core.lib.storage.IItemStorage;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * Marker interface for block entities that expose item storage.
 *
 * <p>Returns the loader-agnostic {@link IItemStorage} contract. Loader-specific adapters
 * (Fabric, NeoForge) bridge this to their native item-storage lookup APIs.
 *
 * <p>Use {@link com.logistics.core.lib.items.ItemInventoryComponent} for simple fixed-size
 * inventories, or implement directly for custom storage behaviour (e.g. slot filtering).
 */
public interface HasItemStorage {

    /**
     * Get item storage for the specified side.
     * Return {@code null} if the side doesn't support item access.
     *
     * @param side the side to access, or {@code null} for non-sided access
     * @return the item storage, or {@code null} if not accessible from this side
     */
    @Nullable IItemStorage itemStorage(@Nullable Direction side);
}
