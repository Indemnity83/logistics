package com.logistics.pipe.modules;

import com.logistics.pipe.Pipe;
import com.logistics.pipe.PipeContext;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

public class PipeOnlyModule implements Module {
    @Override
    public boolean allowsConnection(
            @Nullable PipeContext ctx, Direction direction, Pipe selfPipe, Block neighborBlock) {
        return neighborBlock instanceof com.logistics.pipe.block.PipeBlock;
    }
}
