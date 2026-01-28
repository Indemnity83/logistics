package com.logistics.pipe.modules;

import com.logistics.pipe.Pipe;
import com.logistics.pipe.PipeContext;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

public class BlockConnectionModule implements Module {
    private final Supplier<Pipe> blockedPipe;

    public BlockConnectionModule(Supplier<Pipe> blockedPipe) {
        this.blockedPipe = blockedPipe;
    }

    @Override
    public boolean allowsConnection(
            @Nullable PipeContext ctx, Direction direction, Pipe selfPipe, Block neighborBlock) {
        if (neighborBlock instanceof com.logistics.pipe.block.PipeBlock neighborPipeBlock) {
            Pipe neighborPipe = neighborPipeBlock.getPipe();
            Pipe blocked = blockedPipe.get();
            return neighborPipe == null || neighborPipe != blocked;
        }
        return true;
    }
}
