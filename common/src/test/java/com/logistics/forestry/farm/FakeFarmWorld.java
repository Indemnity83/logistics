package com.logistics.forestry.farm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/** A map-backed world for farm tests: air everywhere unless set, and it records what was read and written. */
final class FakeFarmWorld implements FarmWorldAccess {

    final Map<BlockPos, BlockState> blocks = new HashMap<>();
    final Set<BlockPos> unloaded = new HashSet<>();
    final Set<BlockPos> claimed = new HashSet<>();
    final Set<BlockPos> fertilizable = new HashSet<>();
    final List<BlockPos> reads = new ArrayList<>();
    final List<BlockPos> broken = new ArrayList<>();
    final Map<BlockPos, Integer> writeFlags = new HashMap<>();
    final List<BlockPos> fertilized = new ArrayList<>();
    final List<ItemStack> loose = new ArrayList<>();
    Function<BlockState, List<ItemStack>> drops = state -> List.of(new ItemStack(state.getBlock().asItem()));
    boolean signal;

    void set(BlockPos pos, BlockState state) {
        blocks.put(pos.immutable(), state);
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
    public boolean canFertilize(BlockPos pos) {
        return isLoaded(pos) && fertilizable.contains(pos);
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
        if (!isLoaded(pos) || state == null || state.isAir()) {
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
    public boolean fertilize(BlockPos pos) {
        fertilized.add(pos.immutable());
        return true;
    }

    @Override
    public List<ItemStack> collectItems(AABB box) {
        List<ItemStack> collected = new ArrayList<>(loose);
        loose.clear();
        return collected;
    }
}
