package com.logistics.core.lib.fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Loader-agnostic registry for finding {@link IFluidStorage} instances from the world.
 *
 * <p>Loader-specific modules register their implementations once during initialization:
 * <ul>
 *   <li>Fabric: registers a {@link Finder} that wraps {@code FluidStorage.SIDED.find()}
 *   <li>NeoForge: registers a {@link Finder} that wraps the fluid capability
 * </ul>
 *
 * <p>Common code calls {@link #find} without depending on any loader-specific types.
 */
public final class FluidStorageLookup {

    /** Finds an {@link IFluidStorage} for a given world position and face. */
    @FunctionalInterface
    public interface Finder {
        @Nullable IFluidStorage find(Level world, BlockPos pos, Direction dir);
    }

    private static volatile Finder finder;

    private FluidStorageLookup() {}

    /**
     * Register the platform-specific storage finder. Called once during loader initialization.
     *
     * @param f the finder implementation
     */
    public static void register(Finder f) {
        finder = f;
    }

    /**
     * Find an {@link IFluidStorage} for the given position and face.
     *
     * @param world the level
     * @param pos   the block position to query
     * @param dir   the face to access storage from
     * @return the storage, or {@code null} if none is present
     */
    @Nullable
    public static IFluidStorage find(Level world, BlockPos pos, Direction dir) {
        return finder != null ? finder.find(world, pos, dir) : null;
    }
}
