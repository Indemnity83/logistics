package com.logistics.forestry.farm;

import com.logistics.forestry.logic.LevelTreeView;
import com.logistics.forestry.logic.TreeView;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The read-only world a {@link FarmLogic} decides from. No read ever loads a chunk: an unloaded
 * position reads as void air, darkness, and unplantable.
 */
public interface FarmWorld {

    boolean isLoaded(BlockPos pos);

    /** The block state at {@code pos}, or void air when {@code pos} is not loaded. */
    BlockState state(BlockPos pos);

    /** Raw light level at {@code pos} (0–15, sky or block, whichever is brighter); 0 when not loaded. */
    int light(BlockPos pos);

    /** Whether {@code state} could stand at {@code pos}; false when not loaded. */
    boolean canSurvive(BlockState state, BlockPos pos);

    /** Whether bone meal would do anything to the block at {@code pos}; false when not loaded. */
    boolean canFertilize(BlockPos pos);

    /**
     * Whether the block at {@code pos} is ground a farm may dig up to lay its own soil
     * ({@code logistics:forestry/replaceable_soil}); false when not loaded.
     */
    boolean isReplaceableSoil(BlockPos pos);

    /** This world classified for {@link com.logistics.forestry.logic.TreeFeller}. */
    default TreeView treeView() {
        return pos -> isLoaded(pos) ? LevelTreeView.classify(state(pos)) : TreeView.Kind.UNLOADED;
    }
}
