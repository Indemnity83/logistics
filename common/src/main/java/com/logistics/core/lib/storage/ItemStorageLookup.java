package com.logistics.core.lib.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Loader-agnostic registry for finding {@link IItemStorage} instances from the world
 * and constructing {@link IItemKey} instances from vanilla {@link ItemStack}s.
 *
 * <p>Loader-specific modules register their implementations once during initialization:
 * <ul>
 *   <li>Fabric: registers a {@link Finder} that wraps {@code ItemStorage.SIDED.find()}
 *   <li>NeoForge: registers a {@link Finder} that wraps the {@code IItemHandler} capability
 * </ul>
 *
 * <p>Common code (pipe modules, pipe runtime) calls {@link #find} and {@link #of} without
 * depending on any loader-specific types.
 */
public final class ItemStorageLookup {

    /** Finds an {@link IItemStorage} for a given world position and face. */
    @FunctionalInterface
    public interface Finder {
        @Nullable IItemStorage find(Level world, BlockPos pos, Direction dir);
    }

    /** Constructs an {@link IItemKey} from a vanilla {@link ItemStack}. */
    @FunctionalInterface
    public interface KeyFactory {
        IItemKey of(ItemStack stack);
    }

    private static volatile Finder finder;
    private static volatile KeyFactory keyFactory;

    private ItemStorageLookup() {}

    /**
     * Register the platform-specific storage finder. Called once during loader initialization.
     *
     * @param f the finder implementation
     */
    public static void register(Finder f) {
        finder = f;
    }

    /**
     * Register the platform-specific key factory. Called once during loader initialization.
     *
     * @param f the factory implementation
     */
    public static void registerKeyFactory(KeyFactory f) {
        keyFactory = f;
    }

    /**
     * Find an {@link IItemStorage} for the given position and face.
     *
     * @param world the level
     * @param pos   the block position to query
     * @param dir   the face to access storage from
     * @return the storage, or {@code null} if none is present
     */
    @Nullable
    public static IItemStorage find(Level world, BlockPos pos, Direction dir) {
        return finder != null ? finder.find(world, pos, dir) : null;
    }

    /**
     * Create an {@link IItemKey} representing the item type of the given stack.
     *
     * @param stack the stack to identify (count is ignored)
     * @return an {@link IItemKey} for the stack's item type and components
     * @throws IllegalStateException if no key factory has been registered
     */
    public static IItemKey of(ItemStack stack) {
        if (keyFactory == null) {
            throw new IllegalStateException("ItemStorageLookup key factory not registered");
        }
        return keyFactory.of(stack);
    }
}
