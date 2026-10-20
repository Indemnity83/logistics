package com.logistics.forestry.farm;

import com.logistics.core.machine.area.Columns;
import com.logistics.forestry.logic.LevelTreeView;
import com.logistics.forestry.logic.TreeView;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * The world a farm works, with its courtyard sealed off: at any Y, a courtyard column is never
 * modifiable, never broken, never written and never swept of items. Reads pass through unchanged.
 * Every write is refused here, whatever the caller checked first, so a missed check fails closed.
 *
 * <p>One exception: natural leaves hanging into the courtyard may be part of a tree's canopy and
 * broken through {@link #breakLeaves} as it is felled. Nothing else in the courtyard is ever part of
 * a tree.
 */
final class CourtyardGuard implements FarmWorldAccess {

    private final FarmWorldAccess world;
    private final Columns courtyard;

    CourtyardGuard(FarmWorldAccess world, Columns courtyard) {
        this.world = world;
        this.courtyard = courtyard;
    }

    @Override
    public boolean isLoaded(BlockPos pos) {
        return world.isLoaded(pos);
    }

    @Override
    public BlockState state(BlockPos pos) {
        return world.state(pos);
    }

    @Override
    public int light(BlockPos pos) {
        return world.light(pos);
    }

    @Override
    public boolean canSurvive(BlockState state, BlockPos pos) {
        return world.canSurvive(state, pos);
    }

    @Override
    public FarmClimate climate(BlockPos pos) {
        return world.climate(pos);
    }

    @Override
    public boolean rainingAt(BlockPos pos) {
        return world.rainingAt(pos);
    }

    @Override
    public TreeView.Kind treeKind(BlockState state) {
        return world.treeKind(state);
    }

    @Override
    public boolean neverClear(BlockState state) {
        return world.neverClear(state);
    }

    @Override
    public boolean naturalClearable(BlockState state) {
        return world.naturalClearable(state);
    }

    @Override
    public TreeView treeView() {
        TreeView inner = world.treeView();
        return new TreeView() {
            @Override
            public Kind kind(BlockPos pos) {
                Kind kind = inner.kind(pos);
                if (!courtyard.contains(pos)) {
                    return kind;
                }
                return kind == Kind.CANOPY && inner.isLeaves(pos) ? kind : Kind.NEVER_TOUCH;
            }

            @Override
            public boolean isLeaves(BlockPos pos) {
                return inner.isLeaves(pos);
            }
        };
    }

    @Override
    public float destroySpeed(BlockPos pos) {
        return world.destroySpeed(pos);
    }

    @Override
    public boolean canModify(BlockPos pos) {
        return !courtyard.contains(pos) && world.canModify(pos);
    }

    @Override
    @Nullable
    public List<ItemStack> breakBlock(BlockPos pos) {
        return courtyard.contains(pos) ? null : world.breakBlock(pos);
    }

    /** In the courtyard, only natural leaves, and only where the world allows it. */
    @Override
    public boolean canBreakLeaves(BlockPos pos) {
        return LevelTreeView.isNaturalLeaves(world.state(pos)) && world.canModify(pos);
    }

    @Override
    @Nullable
    public List<ItemStack> breakLeaves(BlockPos pos) {
        return canBreakLeaves(pos) ? world.breakBlock(pos) : null;
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState state, int flags) {
        return !courtyard.contains(pos) && world.setBlock(pos, state, flags);
    }

    @Override
    public List<ItemStack> collectItems(AABB box, Predicate<BlockPos> where) {
        return world.collectItems(box, pos -> !courtyard.contains(pos) && where.test(pos));
    }
}
