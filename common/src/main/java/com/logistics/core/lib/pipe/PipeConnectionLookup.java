package com.logistics.core.lib.pipe;

import com.logistics.core.lib.block.capability.PipeConnection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Platform-neutral entry point for querying pipe connectivity.
 * The loader registers an implementation during initialization.
 */
public final class PipeConnectionLookup {

    @FunctionalInterface
    public interface Finder {
        @Nullable PipeConnection find(Level level, BlockPos pos, Direction direction);
    }

    private static Finder impl = (level, pos, dir) -> null;

    public static void register(Finder finder) {
        impl = finder;
    }

    @Nullable
    public static PipeConnection find(Level level, BlockPos pos, Direction direction) {
        return impl.find(level, pos, direction);
    }

    private PipeConnectionLookup() {}
}
