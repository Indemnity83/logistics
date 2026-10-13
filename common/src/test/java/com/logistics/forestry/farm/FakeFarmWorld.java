package com.logistics.forestry.farm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/** A map-backed world for farm tests: air everywhere unless set, and it records what was read and written. */
final class FakeFarmWorld implements FarmWorldAccess {

    final Map<BlockPos, BlockState> blocks = new HashMap<>();
    final Set<BlockPos> unloaded = new HashSet<>();
    final Set<BlockPos> claimed = new HashSet<>();
    final List<BlockPos> reads = new ArrayList<>();
    final List<BlockPos> broken = new ArrayList<>();
    final Map<BlockPos, Integer> writeFlags = new HashMap<>();
    final List<LooseItem> loose = new ArrayList<>();
    Function<BlockState, List<ItemStack>> drops = state -> List.of(new ItemStack(state.getBlock().asItem()));
    boolean signal;

    /** An item entity lying in the world. */
    record LooseItem(BlockPos pos, ItemStack stack) {}

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
        if (!isLoaded(pos)) {
            return Blocks.VOID_AIR.defaultBlockState();
        }
        return blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
    }

    @Override
    public int light(BlockPos pos) {
        return isLoaded(pos) ? 15 : 0;
    }

    @Override
    public boolean canSurvive(BlockState state, BlockPos pos) {
        return isLoaded(pos);
    }

    @Override
    public float destroySpeed(BlockPos pos) {
        return isLoaded(pos)
                ? blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState()).getDestroySpeed(EmptyBlockGetter.INSTANCE, pos)
                : 0;
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
        if (!isLoaded(pos) || state == null || state.isAir() || destroySpeed(pos) < 0) {
            return null;
        }
        blocks.remove(pos);
        broken.add(pos.immutable());
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack drop : drops.apply(state)) {
            result.add(drop.copy());
        }
        return result;
    }

    @Override
    public void setBlock(BlockPos pos, BlockState state, int flags) {
        if (isLoaded(pos)) {
            blocks.put(pos.immutable(), state);
            writeFlags.put(pos.immutable(), flags);
        }
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
