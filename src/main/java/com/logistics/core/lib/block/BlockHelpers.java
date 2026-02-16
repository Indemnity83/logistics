package com.logistics.core.lib.block;

import com.logistics.core.lib.entity.HasItemStorage;
import com.logistics.core.lib.entity.HasMenu;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility helpers for block operations.
 */
public final class BlockHelpers {

    private BlockHelpers() {}

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

        Storage<ItemVariant> storage = hasItems.itemStorage();

        try (Transaction tx = Transaction.openOuter()) {
            for (StorageView<ItemVariant> view : storage) {
                if (view.isResourceBlank()) continue;

                ItemVariant variant = view.getResource();
                long amount = view.getAmount();

                if (amount > 0) {
                    ItemStack stack = variant.toStack((int) amount);
                    drops.add(stack);
                    view.extract(variant, amount, tx);
                }
            }
            tx.commit();
        }

        return drops;
    }

    /**
     * Try to open a menu/GUI for the block entity at the given position.
     * Only works if the block entity implements HasMenu.
     *
     * <p>This is intended for use in Block.useWithoutItem() or Block.useItemOn()
     * to conditionally open GUIs only for blocks that support them.
     *
     * @param level The world
     * @param pos The block position
     * @param player The player trying to open the menu
     * @return SUCCESS if menu was opened, PASS if no menu available
     */
    public static InteractionResult tryOpenMenu(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof HasMenu hasMenu) {
            player.openMenu(hasMenu.createMenuProvider());
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }
}