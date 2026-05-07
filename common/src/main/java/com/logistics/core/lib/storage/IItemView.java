package com.logistics.core.lib.storage;

/**
 * Read-only view of a slot or resource within an {@link IItemStorage}.
 *
 * <p>Equivalent to Fabric's {@code StorageView<ItemVariant>} in the loader-agnostic API.
 * Instances are obtained by iterating {@link IItemStorage#contents()}.
 */
public interface IItemView {

    /** The item type stored in this slot or resource group. */
    IItemKey resource();

    /** The number of items available in this view. */
    long amount();
}
