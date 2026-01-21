package com.logistics.neoforge.platform;

import com.logistics.block.entity.LogisticsBlockEntities;
import com.logistics.platform.services.IItemStorageService;
import com.logistics.platform.storage.ItemStorageHandle;
import com.logistics.platform.storage.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * NeoForge implementation of IItemStorageService using IItemHandler capabilities.
 */
public class NeoForgeItemStorageService implements IItemStorageService {

    @Override
    @Nullable
    public ItemStorageHandle findStorage(
            net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos, net.minecraft.util.math.Direction accessSide) {
        // Convert Yarn-mapped types to Mojmap for NeoForge
        if (!(world instanceof Level level)) {
            return null;
        }

        BlockPos blockPos = new BlockPos(pos.getX(), pos.getY(), pos.getZ());
        Direction direction = Direction.from3DDataValue(accessSide.getId());

        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, blockPos, direction);
        if (handler == null) {
            return null;
        }

        // Check if this is a pipe storage
        BlockEntity be = level.getBlockEntity(blockPos);
        boolean isPipe = be != null && be.getType() == LogisticsBlockEntities.PIPE_BLOCK_ENTITY;

        return new NeoForgeItemStorageHandle(handler, isPipe);
    }

    @Override
    public <T extends net.minecraft.block.entity.BlockEntity> void registerBlockEntityStorage(
            net.minecraft.block.entity.BlockEntityType<T> type, StorageProvider<T> provider) {
        // Registration happens via RegisterCapabilitiesEvent, not through this method on NeoForge
        // This method is a no-op; capability registration is handled in registerPipeCapability()
    }

    @Override
    public TransactionContext openTransaction() {
        return new NeoForgeTransactionContext();
    }

    /**
     * Register the pipe block entity's item handler capability.
     * Called from the mod's RegisterCapabilitiesEvent handler.
     */
    @SuppressWarnings("unchecked")
    public static void registerPipeCapability(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                (BlockEntityType<? extends BlockEntity>) (Object) LogisticsBlockEntities.PIPE_BLOCK_ENTITY,
                (blockEntity, direction) -> {
                    // Get the item handler from the pipe block entity
                    // This requires the pipe to implement a method that returns IItemHandler
                    if (blockEntity instanceof NeoForgePipeItemHandler.Provider provider) {
                        return provider.getItemHandler(direction);
                    }
                    return null;
                });
    }
}
