package com.logistics.automation.laserquarry.entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
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

        // Read-only, either side of the break: what a container held, and what it still holds
        // afterwards. The difference is what it actually spilled. Nothing is taken out of the block
        // entity, and no assumption is made about which blocks spill -- a shulker box keeps its
        // contents in the item it drops and comes through the break still full, so it registers as
        // having spilled nothing (see QuarryOutput#claimLater).
        List<ItemStack> heldBefore = contentsOf(blockEntity);

        // Break the block without natural drops so we can route them ourselves.
        world.destroyBlock(target, false);

        List<ItemStack> spilled = minus(heldBefore, contentsOf(blockEntity));

        for (ItemStack drop : drops) {
            output.accept(world, drop);
        }

        if (blockEntity != null) {
            List<ItemStack> taken = output.sweepNearby(world, target, alreadyOnGround);
            output.claimLater(target, minus(spilled, taken));
        }
    }

    /** Copies of what a container holds, or empty for anything that is not one. */
    private static List<ItemStack> contentsOf(BlockEntity blockEntity) {
        List<ItemStack> contents = new ArrayList<>();
        if (blockEntity instanceof Container container) {
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (!stack.isEmpty()) {
                    contents.add(stack.copy());
                }
            }
        }
        return contents;
    }

    /** {@code from} less everything in {@code subtract}, matching on item and components. */
    private static List<ItemStack> minus(List<ItemStack> from, List<ItemStack> subtract) {
        for (ItemStack got : subtract) {
            int remaining = got.getCount();
            for (Iterator<ItemStack> owed = from.iterator(); owed.hasNext() && remaining > 0; ) {
                ItemStack want = owed.next();
                if (!ItemStack.isSameItemSameComponents(want, got)) {
                    continue;
                }
                int matched = Math.min(want.getCount(), remaining);
                want.shrink(matched);
                remaining -= matched;
                if (want.isEmpty()) {
                    owed.remove();
                }
            }
        }
        return from;
    }
}
