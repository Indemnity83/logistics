package com.logistics.fabric.platform;

import com.logistics.platform.services.IItemStorageService;
import com.logistics.platform.storage.ItemStorageHandle;
import com.logistics.platform.storage.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Fabric implementation of IItemStorageService using the Transfer API.
 */
public class FabricItemStorageService implements IItemStorageService {

    @Override
    @Nullable public ItemStorageHandle findStorage(World world, BlockPos pos, Direction accessSide) {
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(world, pos, accessSide);
        if (storage == null) {
            return null;
        }
        return new FabricItemStorageHandle(storage);
    }

    @Override
    public <T extends BlockEntity> void registerBlockEntityStorage(
            BlockEntityType<T> type, StorageProvider<T> provider) {
        ItemStorage.SIDED.registerForBlockEntity(
                (blockEntity, direction) -> {
                    ItemStorageHandle handle = provider.getStorage(blockEntity, direction);
                    if (handle instanceof FabricItemStorageHandle fabricHandle) {
                        return fabricHandle.getStorage();
                    }
                    // If it's a custom handle, wrap it - but this shouldn't happen for Fabric
                    return null;
                },
                type);
    }

    @Override
    public TransactionContext openTransaction() {
        return new FabricTransactionContext(Transaction.openOuter());
    }
}
