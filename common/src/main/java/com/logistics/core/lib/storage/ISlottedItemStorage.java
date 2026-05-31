package com.logistics.core.lib.storage;

import org.jetbrains.annotations.Nullable;

/**
 * Optional extension for {@link IItemStorage} implementations backed by addressable slots.
 *
 * <p>The base storage contract is resource-oriented. Implement this contract when slot identity
 * matters to loader APIs or external automation.
 */
public interface ISlottedItemStorage extends IItemStorage {

    /** Number of addressable slots exposed by this storage view. */
    int slotCount();

    /**
     * Read a single slot.
     *
     * @param slot the exposed slot index
     * @return the non-empty slot view, or {@code null} when the slot is empty
     * @throws IndexOutOfBoundsException when {@code slot < 0 || slot >= slotCount()}
     */
    @Nullable IItemView slotView(int slot);

    /**
     * Insert into a single exposed slot.
     *
     * @param slot      the exposed slot index
     * @param item      the item type to insert
     * @param maxAmount the maximum number of items to insert
     * @param simulate  whether to leave backing state unchanged
     * @return the amount inserted, or that would be inserted when simulating
     * @throws IndexOutOfBoundsException when {@code slot < 0 || slot >= slotCount()}
     *
     * <p>If {@code maxAmount <= 0}, implementations must return {@code 0} without modifying
     * backing state, even when {@code simulate} is {@code true}.
     */
    long insert(int slot, IItemKey item, long maxAmount, boolean simulate);

    /**
     * Extract from a single exposed slot.
     *
     * @param slot      the exposed slot index
     * @param item      the item type to extract
     * @param maxAmount the maximum number of items to extract
     * @param simulate  whether to leave backing state unchanged
     * @return the amount extracted, or that would be extracted when simulating
     * @throws IndexOutOfBoundsException when {@code slot < 0 || slot >= slotCount()}
     *
     * <p>If {@code maxAmount <= 0}, implementations must return {@code 0} without modifying
     * backing state, even when {@code simulate} is {@code true}.
     */
    long extract(int slot, IItemKey item, long maxAmount, boolean simulate);
}
