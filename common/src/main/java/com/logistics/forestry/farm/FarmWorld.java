package com.logistics.forestry.farm;

import com.logistics.forestry.logic.FellPlan;
import com.logistics.forestry.logic.LevelTreeView;
import com.logistics.forestry.logic.TreeFeller;
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

    /** The climate at {@code pos}, which scales the water a till costs; neutral by default. */
    default FarmClimate climate(BlockPos pos) {
        return FarmClimate.NEUTRAL;
    }

    /** What {@code state} is as far as felling goes; never asked about an unloaded position. */
    default TreeView.Kind treeKind(BlockState state) {
        return LevelTreeView.classify(state);
    }

    /** This world classified for {@link com.logistics.forestry.logic.TreeFeller}. */
    default TreeView treeView() {
        return pos -> isLoaded(pos) ? treeKind(state(pos)) : TreeView.Kind.UNLOADED;
    }

    /**
     * The tree standing at {@code plot}, planned within {@code limits}. Logics ask for trees here
     * rather than calling {@link TreeFeller} themselves, so the farm can spread the planning of a
     * large tree over several ticks; a decision made while the plan is still incomplete is
     * discarded and asked again once it is done.
     */
    default FellPlan fellPlan(BlockPos plot, TreeFeller.Limits limits) {
        return TreeFeller.plan(treeView(), plot, limits);
    }
}
