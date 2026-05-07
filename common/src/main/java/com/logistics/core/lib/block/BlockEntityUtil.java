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

        for (IItemView view : storage.contents()) {
            IItemKey key = view.resource();
            long amount = view.amount();
            if (amount <= 0) continue;

            ItemStack template = key.toStack(1);
            int maxStackSize = template.getMaxStackSize();

            // Build drop stacks from view amount, then do a real extract to remove items from storage
            long remaining = amount;
            while (remaining > 0) {
                int stackSize = (int) Math.min(remaining, maxStackSize);
                drops.add(key.toStack(stackSize));
                remaining -= stackSize;
            }

            storage.extract(key, amount, false);
        }

        return drops;
    }
}
