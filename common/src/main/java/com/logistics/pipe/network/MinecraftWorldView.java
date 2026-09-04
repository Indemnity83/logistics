package com.logistics.pipe.network;

import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.core.lib.block.capability.HasEnergyStorage;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.network.IWorldView;
import com.logistics.pipe.ItemPipe;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.core.lib.pipe.Module;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.logistics.core.lib.storage.IItemKey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Minecraft implementation of IWorldView.
 * Provides access to the actual Level for pipe and module queries.
 */
public class MinecraftWorldView implements IWorldView {
    private final Level level;

    public MinecraftWorldView(Level level) {
        this.level = level;
    }

    @Override
    public boolean isPipe(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof PipeBlock;
    }

    @Override
    public List<BlockPos> getConnectedNeighbors(BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>();

        // Ask *this* pipe whether it connects each way — see NetworkRegistry.getNeighbors.
        if (!(level.getBlockState(pos).getBlock() instanceof PipeBlock pipeBlock)) {
            return neighbors;
        }

        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            if (pipeBlock.getConnectionType(level, pos, direction) != PipeConnection.Type.NONE
                    && isPipe(neighborPos)) {
                neighbors.add(neighborPos);
            }
        }

        return neighbors;
    }

    @Nullable
    public <T extends Module> T getModule(BlockPos pos, Class<T> moduleClass) {
        if (!(level.getBlockEntity(pos) instanceof PipeBlockEntity pipeEntity)) {
            return null;
        }

        PipeBlock block = (PipeBlock) pipeEntity.getBlockState().getBlock();
        ItemPipe pipe = block.getPipe();
        return pipe.getModule(moduleClass, pipeEntity);
    }

    @Override
    public boolean matchesSinkFilter(BlockPos pos, ItemStack stack) {
        if (!(level.getBlockEntity(pos) instanceof PipeBlockEntity pipeEntity)) {
            return false;
        }
        PipeBlock block = (PipeBlock) pipeEntity.getBlockState().getBlock();
        ItemPipe pipe = block.getPipe();
        PipeContext ctx = pipeEntity.createContext();

        return pipe.matchesSinkFilter(ctx, stack);
    }

    @Override
    public long dispatch(BlockPos provider, BlockPos requester, IItemKey item, long amount, UUID deliveryId) {
        if (!(level.getBlockEntity(provider) instanceof PipeBlockEntity pipeEntity)) return 0;
        BlockState state = level.getBlockState(provider);
        if (!(state.getBlock() instanceof PipeBlock pipeBlock)) return 0;

        ItemPipe pipe = pipeBlock.getPipe();
        PipeContext ctx = pipeEntity.createContext();
        return pipe.dispatch(ctx, requester, item, amount, deliveryId);
    }

    @Override
    public long dispatchFluid(BlockPos provider, BlockPos requester, Fluid fluid, long amountMb, UUID deliveryId) {
        if (!(level.getBlockEntity(provider) instanceof PipeBlockEntity pipeEntity)) return 0;
        BlockState state = level.getBlockState(provider);
        if (!(state.getBlock() instanceof PipeBlock pipeBlock)) return 0;

        ItemPipe pipe = pipeBlock.getPipe();
        PipeContext ctx = pipeEntity.createContext();
        return pipe.dispatchFluid(ctx, requester, fluid, amountMb, deliveryId);
    }

    @Override
    public boolean isClientSide() {
        return level.isClientSide();
    }

    @Override
    public void broadcastAlert(BlockPos pos, Component message) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        serverLevel.players().forEach(p -> {
            if (p.blockPosition().distSqr(pos) < 64 * 64) {
                p.sendSystemMessage(message);
            }
        });
    }

    @Override
    @Nullable
    public IEnergyStorage energyStorageAt(BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof HasEnergyStorage hasStorage) {
            return hasStorage.energyStorage(null);
        }
        return null;
    }

    @Override
    public long gameTime() {
        return level.getGameTime();
    }

    /**
     * Get the underlying Level.
     * This is a temporary bridge method for code that still needs Level access.
     * @deprecated Prefer using IWorldView methods instead
     */
    @Deprecated
    public Level getLevel() {
        return level;
    }
}
