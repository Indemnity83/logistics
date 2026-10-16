package com.logistics.forestry.farm;

import com.logistics.forestry.logic.TreeView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * A map-backed world for farm tests: air everywhere unless set, and it records what was read and
 * written. Writes can be made to fail ({@link #refuseSet}, {@link #refuseBreak}), and every read
 * is counted in {@link #readCount}.
 */
final class FakeFarmWorld implements FarmWorldAccess {

    /** Tags are not bound in unit tests: block entities are never touched, everything else is no tree. */
    static final Function<BlockState, TreeView.Kind> NO_TREES =
            state -> state.hasBlockEntity() ? TreeView.Kind.NEVER_TOUCH : TreeView.Kind.OTHER;

    /** Oak logs are wood and natural oak leaves canopy, as the tags would have them. */
    static final Function<BlockState, TreeView.Kind> OAK_TREES = state -> {
        if (state.is(Blocks.OAK_LOG)) {
            return TreeView.Kind.WOOD;
        }
        if (state.is(Blocks.OAK_LEAVES) && !state.getValue(LeavesBlock.PERSISTENT)) {
            return TreeView.Kind.CANOPY;
        }
        return NO_TREES.apply(state);
    };

    final Map<BlockPos, BlockState> blocks = new HashMap<>();
    final Set<BlockPos> unloaded = new HashSet<>();
    final Set<BlockPos> claimed = new HashSet<>();
    final List<BlockPos> reads = new ArrayList<>();
    final List<BlockPos> broken = new ArrayList<>();
    final Map<BlockPos, Integer> writeFlags = new HashMap<>();
    final List<LooseItem> loose = new ArrayList<>();
    Function<BlockState, List<ItemStack>> drops = state -> List.of(new ItemStack(state.getBlock().asItem()));
    Function<BlockState, TreeView.Kind> trees = NO_TREES;
    /** Stand in for the unbound {@code natural_clearable} and {@code never_clear} tags. */
    Predicate<BlockState> natural = state -> false;
    Predicate<BlockState> neverClear = state -> false;
    FarmClimate climate = FarmClimate.NEUTRAL;
    /** A write of a state at a position this refuses reports failure and changes nothing. */
    BiPredicate<BlockPos, BlockState> refuseSet = (pos, state) -> false;
    /** A break at a position this refuses reports failure and changes nothing. */
    Predicate<BlockPos> refuseBreak = pos -> false;
    boolean signal;
    long readCount;

    /** An item entity lying in the world. */
    record LooseItem(BlockPos pos, ItemStack stack) {}

    /** A change the farm made: {@code after} is null for a break, whose drops are given. */
    record Mutation(BlockPos pos, BlockState before, @Nullable BlockState after, List<ItemStack> drops) {}

    /** Every successful break and write, in order. */
    final List<Mutation> mutations = new ArrayList<>();

    void set(BlockPos pos, BlockState state) {
        blocks.put(pos.immutable(), state);
    }

    void drop(BlockPos pos, ItemStack stack) {
        loose.add(new LooseItem(pos.immutable(), stack));
    }

    @Override
    public boolean isLoaded(BlockPos pos) {
        return !unloaded.contains(pos);
    }

    @Override
    public BlockState state(BlockPos pos) {
        reads.add(pos.immutable());
        readCount++;
        return stateAt(pos);
    }

    private BlockState stateAt(BlockPos pos) {
        if (!isLoaded(pos)) {
            return Blocks.VOID_AIR.defaultBlockState();
        }
        return blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
    }

    @Override
    public int light(BlockPos pos) {
        readCount++;
        return isLoaded(pos) ? 15 : 0;
    }

    @Override
    public boolean canSurvive(BlockState state, BlockPos pos) {
        readCount++;
        return isLoaded(pos);
    }

    @Override
    public FarmClimate climate(BlockPos pos) {
        readCount++;
        return climate;
    }

    @Override
    public TreeView.Kind treeKind(BlockState state) {
        return trees.apply(state);
    }

    @Override
    public boolean naturalClearable(BlockState state) {
        return natural.test(state);
    }

    @Override
    public boolean neverClear(BlockState state) {
        return neverClear.test(state);
    }

    @Override
    public float destroySpeed(BlockPos pos) {
        readCount++;
        return hardness(pos);
    }

    private float hardness(BlockPos pos) {
        return isLoaded(pos) ? stateAt(pos).getDestroySpeed(EmptyBlockGetter.INSTANCE, pos) : 0;
    }

    @Override
    public boolean hasNeighborSignal(BlockPos pos) {
        return signal;
    }

    @Override
    public boolean canModify(BlockPos pos) {
        return !claimed.contains(pos);
    }

    @Override
    @Nullable
    public List<ItemStack> breakBlock(BlockPos pos) {
        BlockState state = blocks.get(pos);
        if (!isLoaded(pos) || state == null || state.isAir() || hardness(pos) < 0 || refuseBreak.test(pos)) {
            return null;
        }
        blocks.remove(pos);
        broken.add(pos.immutable());
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack drop : drops.apply(state)) {
            result.add(drop.copy());
        }
        mutations.add(new Mutation(pos.immutable(), state, null, result.stream().map(ItemStack::copy).toList()));
        return result;
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState state, int flags) {
        if (!isLoaded(pos) || refuseSet.test(pos, state)) {
            return false;
        }
        mutations.add(new Mutation(pos.immutable(), stateAt(pos), state, List.of()));
        blocks.put(pos.immutable(), state);
        writeFlags.put(pos.immutable(), flags);
        return true;
    }

    @Override
    public List<ItemStack> collectItems(AABB box, Predicate<BlockPos> where) {
        List<ItemStack> collected = new ArrayList<>();
        loose.removeIf(item -> {
            if (box.contains(item.pos().getX() + 0.5, item.pos().getY() + 0.5, item.pos().getZ() + 0.5) && where.test(item.pos())) {
                collected.add(item.stack());
                return true;
            }
            return false;
        });
        return collected;
    }
}
