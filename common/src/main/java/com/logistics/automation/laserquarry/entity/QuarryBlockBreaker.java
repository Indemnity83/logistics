package com.logistics.automation.laserquarry.entity;

import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stateless helper for the laser quarry's block breaking: computes a target block's drops, removes
 * the block, and hands the drops (and any container-spawned items) to a {@link QuarryOutput} sink.
 * Output routing lives in {@link QuarryOutput}.
 */
public final class QuarryBlockBreaker {

    private QuarryBlockBreaker() {}

    public static void mineBlock(ServerLevel world, BlockPos target, BlockState targetState, QuarryOutput output) {
        BlockEntity blockEntity = world.getBlockEntity(target);
        List<ItemStack> drops = Block.getDrops(targetState, world, target, blockEntity, null, ItemStack.EMPTY);

        // A container spills its contents as loose items instead of returning them from getDrops, so
        // those have to be swept off the ground afterwards. Only what this break spawns belongs to
        // the quarry, so record what was lying there first: plenty of block entities spill nothing
        // (signs, beds, spawners), and a container can stand next to items already on the ground.
        Set<Integer> alreadyOnGround = blockEntity == null ? Set.of() : output.itemsNear(world, target);

        // Break the block without natural drops so we can route them ourselves.
        world.destroyBlock(target, false);

        for (ItemStack drop : drops) {
            output.accept(world, drop);
        }

        if (blockEntity != null) {
            output.sweepNearby(world, target, alreadyOnGround);
        }
    }
}
