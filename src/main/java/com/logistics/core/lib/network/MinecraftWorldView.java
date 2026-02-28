package com.logistics.core.lib.network;

import com.logistics.core.lib.block.capability.PipeConnection;
import com.logistics.pipe.Pipe;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.Module;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
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

        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState state = level.getBlockState(neighborPos);

            if (state.getBlock() instanceof PipeBlock pipeBlock) {
                PipeConnection.Type connectionType =
                    pipeBlock.getConnectionType(level, pos, direction);
                if (connectionType != PipeConnection.Type.NONE && isPipe(neighborPos)) {
                    neighbors.add(neighborPos);
                }
            }
        }

        return neighbors;
    }

    @Override
    @Nullable
    public <T extends Module> T getModule(BlockPos pos, Class<T> moduleClass) {
        if (!(level.getBlockEntity(pos) instanceof PipeBlockEntity pipeEntity)) {
            return null;
        }

        PipeBlock block = (PipeBlock) pipeEntity.getBlockState().getBlock();
        Pipe pipe = block.getPipe();
        return pipe.getModule(moduleClass);
    }

    @Override
    public boolean isClientSide() {
        return level.isClientSide();
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
