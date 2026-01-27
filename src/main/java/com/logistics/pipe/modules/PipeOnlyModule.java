package com.logistics.pipe.modules;

import com.logistics.pipe.Pipe;
import com.logistics.pipe.PipeContext;
import net.minecraft.block.Block;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public class PipeOnlyModule implements Module {
    @Override
    public boolean allowsConnection(
            @Nullable PipeContext ctx, Direction direction, Pipe selfPipe, Block neighborBlock) {
        return neighborBlock instanceof com.logistics.pipe.block.PipeBlock;
    }
}
