package com.logistics.core.lib.block;

import com.logistics.core.lib.block.capability.HasItemStorage;
import com.logistics.core.lib.storage.IItemKey;
import com.logistics.core.lib.storage.IItemStorage;
import com.logistics.core.lib.storage.IItemView;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Cross-cutting utility methods for block entity operations.
 * These are mechanical operations, not behavioral systems.
 */
public final class BlockEntityUtil {

    private BlockEntityUtil() {}

    /**
     * Get all items from a block entity's storage for dropping when the block is broken.
     * Works with any BlockEntity implementing HasItemStorage.
     *
     * <p>This is intended for use in Block.getDrops() to include inventory contents
     * in the dropped items when a block is broken.
     *
     * @param blockEntity The block entity (must implement HasItemStorage)
     * @return List of ItemStacks extracted from storage (empty if no storage or not HasItemStorage)
     */
    public static List<ItemStack> getInventoryDrops(BlockEntity blockEntity) {
        List<ItemStack> drops = new ArrayList<>();

        if (!(blockEntity instanceof HasItemStorage hasItems)) {
            return drops;
        }

        IItemStorage storage = hasItems.itemStorage(null);
        if (storage == null) return drops;

        // Snapshot contents before extracting to avoid modifying a live iterable during iteration
        List<IItemView> views = new ArrayList<>();
        for (IItemView v : storage.contents()) views.add(v);

        for (IItemView view : views) {
            IItemKey key = view.resource();
            long amount = view.amount();
            if (amount <= 0) continue;

            ItemStack template = key.toStack(1);
            int maxStackSize = template.getMaxStackSize();

            for (int stackSize : splitIntoStacks(amount, maxStackSize)) {
                drops.add(key.toStack(stackSize));
            }

            storage.extract(key, amount, false);
        }

        return drops;
    }

    /**
     * Splits a total item count into max-stack-sized chunks (a final smaller chunk holds the
     * remainder). Returns an empty list for a non-positive amount.
     *
     * @throws IllegalArgumentException if {@code maxStackSize} is not positive (a zero or negative
     *     chunk size would never make progress)
     */
    static List<Integer> splitIntoStacks(long amount, int maxStackSize) {
        if (maxStackSize <= 0) {
            throw new IllegalArgumentException("maxStackSize must be positive, got " + maxStackSize);
        }
        List<Integer> sizes = new ArrayList<>();
        long remaining = amount;
        while (remaining > 0) {
            int stackSize = (int) Math.min(remaining, maxStackSize);
            sizes.add(stackSize);
            remaining -= stackSize;
        }
        return sizes;
    }
}
