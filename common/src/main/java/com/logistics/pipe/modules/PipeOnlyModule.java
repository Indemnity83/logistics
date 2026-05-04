package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.pipe.PipeContext;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

public class PipeOnlyModule implements Module {
    @Override
    public boolean allowsConnection(
            @Nullable PipeContext ctx, Direction direction, Block neighborBlock) {
        return neighborBlock instanceof com.logistics.pipe.block.PipeBlock;
    }
}
