package com.logistics.automation.laserquarry.entity;

import java.util.List;
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

        // Break the block without natural drops so we can route them ourselves.
        world.destroyBlock(target, false);

        for (ItemStack drop : drops) {
            output.accept(world, drop);
        }

        // For container drops the engine spawns items separately — sweep them up.
        if (drops.isEmpty() || blockEntity != null) {
            output.sweepNearby(world, target);
        }
    }
}
