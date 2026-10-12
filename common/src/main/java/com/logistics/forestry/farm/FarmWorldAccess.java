package com.logistics.forestry.farm;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * The world as {@link FarmComponent} works it: {@link FarmWorld}'s reads plus the writes the farm
 * performs. Writes to an unloaded position do nothing; callers still check
 * {@link #isLoaded(BlockPos)} and {@link #canModify(BlockPos)} first.
 */
public interface FarmWorldAccess extends FarmWorld {

    /** Whether the block at {@code pos} receives a redstone signal. */
    boolean hasNeighborSignal(BlockPos pos);

    /** Whether the farm's owner may change the block at {@code pos} (claim protection). */
    boolean canModify(BlockPos pos);

    /**
     * Breaks the block at {@code pos} as if mined with no tool and returns its drops instead of
     * spawning them.
     *
     * @return the drops, or {@code null} when nothing was broken (unloaded, unbreakable, or the
     *     removal was refused)
     */
    @Nullable
    List<ItemStack> breakBlock(BlockPos pos);

    void setBlock(BlockPos pos, BlockState state, int flags);

    /**
     * Applies one bone-meal step to the block at {@code pos}, with the usual growth particles when
     * it takes.
     *
     * @return whether a growth step happened (the block's own success roll can fail)
     */
    boolean fertilize(BlockPos pos);

    /** Removes the loose item entities inside {@code box} and returns their stacks. */
    List<ItemStack> collectItems(AABB box);
}
