package com.logistics.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The narrow view a {@link MachineComponent} has of its hosting block entity.
 *
 * <p>Components talk only to this interface, never to a concrete {@code BlockEntity}, so their
 * behavior can be unit-tested with a fake context and no live {@link Level}. The
 * version-specific {@link #recipeManager()} accessor isolates the one MC-API call that differs
 * across Minecraft versions.
 */
public interface MachineContext {

    /** The hosting level, or {@code null} during deserialization and in unit tests. */
    @Nullable
    Level level();

    BlockPos pos();

    BlockState blockState();

    /** Registry access for ItemStack/codec deserialization; valid even when {@link #level()} is null. */
    HolderLookup.Provider registries();

    /** Marks the host dirty for chunk saving. */
    void setChanged();

    /** Marks dirty and pushes a block update to nearby clients. */
    void sync();

    /** Sets the host's block state (no-op when {@link #level()} is null). */
    void setBlockState(BlockState state, int flags);

    /** The server recipe manager, or {@code null} off-server / in tests without one. */
    @Nullable
    RecipeManager recipeManager();

    default boolean isServer() {
        Level level = level();
        return level != null && !level.isClientSide();
    }
}
