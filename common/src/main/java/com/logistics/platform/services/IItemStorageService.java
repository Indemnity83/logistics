package com.logistics.platform.services;

import com.logistics.platform.storage.ItemStorageHandle;
import com.logistics.platform.storage.TransactionContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Platform-agnostic service for item storage operations.
 * Abstracts Fabric's Transfer API and NeoForge's IItemHandler capability.
 */
public interface IItemStorageService {

    /**
     * Find an item storage at the given position, accessible from the given direction.
     *
     * @param world the world
     * @param pos the position to look at
     * @param accessSide the side from which to access the storage (the side of the target block)
     * @return the storage handle, or null if no storage exists
     */
    @Nullable ItemStorageHandle findStorage(World world, BlockPos pos, Direction accessSide);

    /**
     * Check if there is an item storage at the given position accessible from the given direction.
     *
     * @param world the world
     * @param pos the position to look at
     * @param accessSide the side from which to access the storage
     * @return true if a storage exists
     */
    default boolean hasStorage(World world, BlockPos pos, Direction accessSide) {
        return findStorage(world, pos, accessSide) != null;
    }

    /**
     * Register a block entity type as providing item storage.
     * Called during mod initialization.
     *
     * @param type the block entity type
     * @param provider function to get the storage handle from a block entity and direction
     */
    <T extends BlockEntity> void registerBlockEntityStorage(BlockEntityType<T> type, StorageProvider<T> provider);

    /**
     * Open a new transaction for batched storage operations.
     * On Fabric, this wraps a real Transaction.
     * On NeoForge, this tracks operations and applies them on commit.
     *
     * @return a new transaction context
     */
    TransactionContext openTransaction();

    /**
     * Functional interface for providing storage from a block entity.
     */
    @FunctionalInterface
    interface StorageProvider<T extends BlockEntity> {
        @Nullable ItemStorageHandle getStorage(T blockEntity, Direction direction);
    }
}
