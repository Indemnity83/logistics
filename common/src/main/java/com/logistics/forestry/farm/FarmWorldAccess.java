package com.logistics.forestry.farm;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * The world as {@link FarmComponent} works it: {@link FarmWorld}'s reads plus the writes the farm
 * performs. Writes to an unloaded position do nothing and report failure; callers still check
 * {@link #isLoaded(BlockPos)} and {@link #canModify(BlockPos)} first, and charge for a write only
 * once it reports success.
 */
public interface FarmWorldAccess extends FarmWorld {

    /** Hardness of the block at {@code pos}, below 0 when unbreakable; 0 when not loaded. */
    float destroySpeed(BlockPos pos);

    /** Whether the farm's owner may change the block at {@code pos} (claim protection). */
    boolean canModify(BlockPos pos);

    /**
     * Breaks the block at {@code pos} as if mined with no tool and returns its drops instead of
     * spawning them. The drops are rolled before the block is removed and returned only if it was.
     *
     * @return the drops, or {@code null} when nothing was broken (unloaded, unbreakable, or the
     *     removal was refused)
     */
    @Nullable
    List<ItemStack> breakBlock(BlockPos pos);

    /**
     * Places {@code state} at {@code pos}.
     *
     * @return whether the world now holds the change; false when unloaded or refused
     */
    boolean setBlock(BlockPos pos, BlockState state, int flags);

    /**
     * Removes the loose item entities inside {@code box} whose block position passes {@code where},
     * and returns their stacks.
     */
    List<ItemStack> collectItems(AABB box, Predicate<BlockPos> where);
}
