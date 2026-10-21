package com.logistics.forestry.farm;

import com.logistics.core.lib.compat.ProtectionCompat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/** {@link FarmWorldAccess} over a live server level. Never loads a chunk. */
public final class LevelFarmWorld implements FarmWorldAccess {

    private final ServerLevel level;
    @Nullable private final UUID owner;

    public LevelFarmWorld(ServerLevel level, @Nullable UUID owner) {
        this.level = level;
        this.owner = owner;
    }

    @Override
    public boolean isLoaded(BlockPos pos) {
        return level.isLoaded(pos);
    }

    @Override
    public BlockState state(BlockPos pos) {
        return isLoaded(pos) ? level.getBlockState(pos) : Blocks.VOID_AIR.defaultBlockState();
    }

    @Override
    public int light(BlockPos pos) {
        return isLoaded(pos) ? level.getRawBrightness(pos, 0) : 0;
    }

    @Override
    public boolean canSurvive(BlockState state, BlockPos pos) {
        return isLoaded(pos) && state.canSurvive(level, pos);
    }

    @Override
    public float destroySpeed(BlockPos pos) {
        return isLoaded(pos) ? level.getBlockState(pos).getDestroySpeed(level, pos) : 0;
    }

    @Override
    public boolean canModify(BlockPos pos) {
        return ProtectionCompat.canModify(level, pos, owner);
    }

    @Override
    @Nullable
    public List<ItemStack> breakBlock(BlockPos pos) {
        if (!isLoaded(pos)) {
            return null;
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0) {
            return null;
        }
        // Drops are rolled before removal: the loot context needs the block entity.
        List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), null, ItemStack.EMPTY);
        if (!level.destroyBlock(pos, false)) {
            return null;
        }
        return drops;
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState state, int flags) {
        return isLoaded(pos) && level.setBlock(pos, state, flags);
    }

    @Override
    public List<ItemStack> collectItems(AABB box, Predicate<BlockPos> where) {
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, box)) {
            if (entity.isAlive() && !entity.getItem().isEmpty() && where.test(entity.blockPosition())) {
                stacks.add(entity.getItem().copy());
                entity.discard();
            }
        }
        return stacks;
    }

    @Override
    public void dropStacks(BlockPos pos, List<ItemStack> stacks) {
        if (!isLoaded(pos)) {
            return;
        }
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                Block.popResource(level, pos, stack.copy());
            }
        }
    }
}
