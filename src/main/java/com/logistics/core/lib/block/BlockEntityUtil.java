package com.logistics.core.lib.block;

import com.logistics.core.lib.block.capability.HasItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
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
     * Uses Transfer API properly with transactions to extract items.
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

        Storage<ItemVariant> storage = hasItems.itemStorage(null);
        if (storage == null) return drops;

        try (Transaction tx = Transaction.openOuter()) {
            for (StorageView<ItemVariant> view : storage) {
                if (view.isResourceBlank()) continue;

                ItemVariant variant = view.getResource();
                long amount = view.getAmount();
                if (amount <= 0) continue;

                // Split large amounts into multiple stacks respecting max stack size
                ItemStack sampleStack = variant.toStack(1);
                int maxStackSize = sampleStack.getMaxStackSize();

                long remaining = amount;
                while (remaining > 0) {
                    int stackSize = (int) Math.min(remaining, maxStackSize);
                    drops.add(variant.toStack(stackSize));
                    remaining -= stackSize;
                }

                view.extract(variant, amount, tx);
            }
            tx.commit();
        }

        return drops;
    }
}