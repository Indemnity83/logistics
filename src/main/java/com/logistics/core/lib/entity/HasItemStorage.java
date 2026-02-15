package com.logistics.core.lib.entity;

import com.logistics.core.lib.items.ItemInventoryComponent;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;

/**
 * Marker interface for block entities that expose item storage via the Transfer API.
 * <p>
 * Use {@link ItemInventoryComponent} to implement this easily.
 */
public interface HasItemStorage {
    Storage<ItemVariant> itemStorage();
}