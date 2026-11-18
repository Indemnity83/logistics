package com.logistics.forestry.farm;

import com.logistics.forestry.logic.FellPlan;
import com.logistics.forestry.logic.LevelTreeView;
import com.logistics.forestry.logic.TreeFeller;
import com.logistics.forestry.logic.TreeView;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The read-only world a {@link TendLogic} decides from. No read ever loads a chunk: an unloaded
 * position reads as void air and unplantable.
 */
public interface FarmWorld {

    boolean isLoaded(BlockPos pos);

    /** The block state at {@code pos}, or void air when {@code pos} is not loaded. */
    BlockState state(BlockPos pos);

    /** Whether {@code state} could stand at {@code pos}; false when not loaded. */
    boolean canSurvive(BlockState state, BlockPos pos);

    /** What {@code state} is as far as felling goes; never asked about an unloaded position. */
    default TreeView.Kind treeKind(BlockState state) {
        return LevelTreeView.classify(state);
    }

    /** This world classified for {@link TreeFeller}. */
    default TreeView treeView() {
        return pos -> isLoaded(pos) ? treeKind(state(pos)) : TreeView.Kind.UNLOADED;
    }

    /** The tree standing at {@code plot}, planned by {@link TreeFeller} within {@code limits}. */
    default FellPlan fellPlan(BlockPos plot, TreeFeller.Limits limits) {
        return TreeFeller.plan(treeView(), plot, limits);
    }
}
