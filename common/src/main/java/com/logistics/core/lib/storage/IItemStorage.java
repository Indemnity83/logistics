package com.logistics.core.lib.storage;

/**
 * Loader-agnostic item storage contract.
 *
 * <p>Uses simulate-boolean semantics (not transactions) to remain compatible with both
 * Fabric (Transfer API) and NeoForge (IItemHandler) item APIs. Loader-specific adapters
 * bridge this interface to each platform's native API.
 *
 * <p>This is the primary extension point for the item-storage abstraction layer. Any block
 * entity that participates in the item network should implement
 * {@link com.logistics.core.lib.block.capability.HasItemStorage} and return this type.
 *
 * @see com.logistics.core.lib.block.capability.HasItemStorage
 * @see ItemStorageLookup
 */
public interface IItemStorage {

    /**
     * Insert items into this storage.
     *
     * @param item      the item type to insert
     * @param maxAmount the maximum number of items to insert
     * @param simulate  if {@code true}, perform a dry run without modifying state
     * @return the amount actually inserted (or that would be inserted if simulating)
     */
    long insert(IItemKey item, long maxAmount, boolean simulate);

    /**
     * Extract items from this storage.
     *
     * @param item      the item type to extract
     * @param maxAmount the maximum number of items to extract
     * @param simulate  if {@code true}, perform a dry run without modifying state
     * @return the amount actually extracted (or that would be extracted if simulating)
     */
    long extract(IItemKey item, long maxAmount, boolean simulate);

    /**
     * Iterate over the contents of this storage.
     *
     * <p>Each {@link IItemView} represents a resource group or slot. Empty slots may
     * or may not be included, depending on the implementation.
     *
     * @return an iterable over the non-empty views in this storage
     */
    Iterable<IItemView> contents();
}
