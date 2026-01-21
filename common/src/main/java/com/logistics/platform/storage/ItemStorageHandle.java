package com.logistics.platform.storage;

import java.util.function.Predicate;
import net.minecraft.item.ItemStack;

/**
 * Platform-agnostic wrapper for item storage.
 * Abstracts Fabric's Storage&lt;ItemVariant&gt; and NeoForge's IItemHandler.
 */
public interface ItemStorageHandle {

    /**
     * Insert items into this storage.
     *
     * @param stack the item stack to insert (not modified)
     * @param maxAmount maximum number of items to insert
     * @param transaction the transaction context
     * @return the number of items actually inserted
     */
    long insert(ItemStack stack, long maxAmount, TransactionContext transaction);

    /**
     * Extract items matching the filter from this storage.
     *
     * @param filter predicate to match items (receives a stack with count 1 for testing)
     * @param maxAmount maximum number of items to extract
     * @param transaction the transaction context
     * @return the extracted ItemStack (may be empty if nothing was extracted)
     */
    ItemStack extract(Predicate<ItemStack> filter, long maxAmount, TransactionContext transaction);

    /**
     * Extract a specific item type from this storage.
     *
     * @param resource the item to extract (count is ignored, used for type matching)
     * @param maxAmount maximum number of items to extract
     * @param transaction the transaction context
     * @return the number of items actually extracted
     */
    long extractExact(ItemStack resource, long maxAmount, TransactionContext transaction);

    /**
     * Check if this storage can accept the given item (simulation).
     *
     * @param stack the item to test
     * @return true if at least one item could be inserted
     */
    boolean canInsert(ItemStack stack);

    /**
     * Iterate over storage contents for inspection.
     * The returned stacks should not be modified.
     *
     * @return iterable of item stacks in the storage
     */
    Iterable<ItemStack> getContents();

    /**
     * Check if this is a pipe storage.
     * Used for speed preservation during pipe-to-pipe transfers.
     *
     * @return true if this storage belongs to a pipe block entity
     */
    boolean isPipeStorage();
}
