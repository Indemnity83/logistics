package com.logistics.automation.laserquarry.entity;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stateless helper for the laser quarry's block breaking: computes a target block's drops, removes
 * the block, and hands the drops to a {@link QuarryOutput} sink. Anything a break spills as loose
 * items instead -- a container's contents, an item frame that falls off later -- is picked up by
 * {@link QuarryOutput#collectLooseItems}, not from here. Output routing lives in {@link QuarryOutput}.
 */
public final class QuarryBlockBreaker {

    private QuarryBlockBreaker() {}

    public static void mineBlock(ServerLevel world, BlockPos target, BlockState targetState, QuarryOutput output) {
        List<ItemStack> drops = Block.getDrops(
                targetState, world, target, world.getBlockEntity(target), null, ItemStack.EMPTY);

        // Break the block without natural drops so we can route them ourselves.
        world.destroyBlock(target, false);

        for (ItemStack drop : drops) {
            output.accept(world, drop);
        }
    }
}
