package com.logistics.core.lib.block.capability;

import com.logistics.core.lib.items.ItemInventoryComponent;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * Marker interface for block entities that expose item storage via the Transfer API.
 * <p>
 * Use {@link ItemInventoryComponent} to implement this easily.
 */
public interface HasItemStorage {
    /**
     * Get item storage for the specified side.
     * Return null if the side doesn't support item access.
     *
     * @param side The side to access, or null for non-sided access
     * @return The item storage, or null if not accessible from this side
     */
    @Nullable Storage<ItemVariant> itemStorage(@Nullable Direction side);
}
