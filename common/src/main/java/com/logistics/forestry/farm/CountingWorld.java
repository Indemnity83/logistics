package com.logistics.forestry.farm;

import com.logistics.forestry.logic.TreeView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * The farm's world with every block read counted, so the farm can charge its reads to its check
 * budget, and the states read recorded, so a harvest can later tell whether a block it planned on
 * has changed. Writes pass straight through and are not counted.
 */
final class CountingWorld implements FarmWorldAccess {

    private final Map<BlockPos, BlockState> seen = new HashMap<>();
    @Nullable private FarmWorldAccess world;
    private long reads;

    /** Works {@code world} from now on; the count and the recorded states carry over. */
    void attach(FarmWorldAccess world) {
        this.world = world;
    }

    /** Every read so far. */
    long reads() {
        return reads;
    }

    /** The state last read at {@code pos} since {@link #forget}, or {@code null}. */
    @Nullable
    BlockState seen(BlockPos pos) {
        return seen.get(pos);
    }

    /** Forgets the recorded states. */
    void forget() {
        seen.clear();
    }

    private FarmWorldAccess world() {
        if (world == null) {
            throw new IllegalStateException("no world attached");
        }
        return world;
    }

    @Override
    public boolean isLoaded(BlockPos pos) {
        return world().isLoaded(pos);
    }

    @Override
    public BlockState state(BlockPos pos) {
        reads++;
        BlockState state = world().state(pos);
        seen.put(pos.immutable(), state);
        return state;
    }

    @Override
    public int light(BlockPos pos) {
        reads++;
        return world().light(pos);
    }

    @Override
    public boolean canSurvive(BlockState state, BlockPos pos) {
        reads++;
        return world().canSurvive(state, pos);
    }

    @Override
    public FarmClimate climate(BlockPos pos) {
        reads++;
        return world().climate(pos);
    }

    @Override
    public boolean rainingAt(BlockPos pos) {
        reads++;
        return world().rainingAt(pos);
    }

    @Override
    public TreeView.Kind treeKind(BlockState state) {
        return world().treeKind(state);
    }

    @Override
    public boolean neverClear(BlockState state) {
        return world().neverClear(state);
    }

    @Override
    public boolean naturalClearable(BlockState state) {
        return world().naturalClearable(state);
    }

    @Override
    public boolean hasNeighborSignal(BlockPos pos) {
        return world().hasNeighborSignal(pos);
    }

    @Override
    public float destroySpeed(BlockPos pos) {
        reads++;
        return world().destroySpeed(pos);
    }

    @Override
    public boolean canModify(BlockPos pos) {
        return world().canModify(pos);
    }

    @Override
    @Nullable
    public List<ItemStack> breakBlock(BlockPos pos) {
        return world().breakBlock(pos);
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState state, int flags) {
        return world().setBlock(pos, state, flags);
    }

    @Override
    public List<ItemStack> collectItems(AABB box, Predicate<BlockPos> where) {
        return world().collectItems(box, where);
    }
}
