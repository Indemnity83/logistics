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
     * <p>If {@code maxAmount <= 0}, implementations must return {@code 0} immediately.
     * The return value is always {@code >= 0} and never exceeds {@code maxAmount}.
     *
     * @param item      the item type to insert
     * @param maxAmount the maximum number of items to insert
     * @param simulate  if {@code true}, perform a dry run without modifying state; the returned
     *                  value is the amount that would be inserted without any state change
     * @return the amount actually inserted (or that would be inserted if simulating); never negative
     */
    long insert(IItemKey item, long maxAmount, boolean simulate);

    /**
     * Extract items from this storage.
     *
     * <p>If {@code maxAmount <= 0}, implementations must return {@code 0} immediately.
     * The return value is always {@code >= 0} and never exceeds {@code maxAmount}.
     *
     * @param item      the item type to extract
     * @param maxAmount the maximum number of items to extract
     * @param simulate  if {@code true}, perform a dry run without modifying state; the returned
     *                  value is the amount that would be extracted without any state change
     * @return the amount actually extracted (or that would be extracted if simulating); never negative
     */
    long extract(IItemKey item, long maxAmount, boolean simulate);

    /**
     * Iterate over the contents of this storage.
     *
     * <p>Each {@link IItemView} represents a non-empty resource group or slot.
     * Implementations must omit empty slots — callers may rely on this.
     *
     * @return an iterable over the non-empty views in this storage
     */
    Iterable<IItemView> contents();
}
