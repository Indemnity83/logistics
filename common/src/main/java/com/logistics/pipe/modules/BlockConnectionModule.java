package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.ModularPipe;
import com.logistics.core.lib.pipe.ModularPipeBlock;
import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.pipe.PipeContext;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * Vetoes a connection to a neighbouring pipe of the blocked type. Used so extractors never chain into
 * one another (each should only reach its source handler and downstream transport pipes). Works for
 * both item and fluid pipe families since both blocks implement {@link ModularPipeBlock}.
 */
public class BlockConnectionModule implements Module {
    private final Supplier<? extends ModularPipe> blockedPipe;

    public BlockConnectionModule(Supplier<? extends ModularPipe> blockedPipe) {
        this.blockedPipe = blockedPipe;
    }

    @Override
    public boolean allowsConnection(
            @Nullable PipeContext ctx, Direction direction, Block neighborBlock) {
        if (neighborBlock instanceof ModularPipeBlock neighborPipeBlock) {
            ModularPipe neighborPipe = neighborPipeBlock.modularPipe();
            ModularPipe blocked = blockedPipe.get();
            return neighborPipe == null || neighborPipe != blocked;
        }
        return true;
    }
}
