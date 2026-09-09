package com.logistics.automation.laserquarry.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
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

        // Empty a container ourselves rather than letting the break spill it. Vanilla spills from its
        // own removal path, which re-looks-up the block entity and silently drops the contents on the
        // floor of that lookup when it comes back empty -- the block is cleared either way, so the
        // items simply cease to exist. Taking them first makes the outcome independent of it.
        List<ItemStack> contents = dropsCarryContents(drops) ? List.of() : takeContents(blockEntity);

        // Whatever still spills has to be swept off the ground afterwards. Only what this break
        // spawns belongs to the quarry, so record what was lying there first: plenty of block
        // entities spill nothing (signs, beds, spawners), and a container can stand next to items
        // already on the ground.
        Set<Integer> alreadyOnGround = blockEntity == null ? Set.of() : output.itemsNear(world, target);

        // Break the block without natural drops so we can route them ourselves.
        world.destroyBlock(target, false);

        for (ItemStack drop : drops) {
            output.accept(world, drop);
        }

        for (ItemStack stack : contents) {
            output.accept(world, stack);
        }

        if (blockEntity != null) {
            output.sweepNearby(world, target, alreadyOnGround);
        }
    }

    /** Empties a container block entity, returning what it held. */
    private static List<ItemStack> takeContents(BlockEntity blockEntity) {
        if (!(blockEntity instanceof Container container)) {
            return List.of();
        }

        List<ItemStack> taken = new ArrayList<>();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.removeItemNoUpdate(slot);
            if (!stack.isEmpty()) {
                taken.add(stack);
            }
        }
        return taken;
    }

    /**
     * Whether a drop already carries the block's contents, as a shulker box's item does. Those blocks
     * deliberately do not spill on removal, so emptying them here would duplicate every stack.
     */
    private static boolean dropsCarryContents(List<ItemStack> drops) {
        for (ItemStack drop : drops) {
            ItemContainerContents carried = drop.get(DataComponents.CONTAINER);
            if (carried != null && carried.nonEmptyItems().iterator().hasNext()) {
                return true;
            }
        }
        return false;
    }
}
