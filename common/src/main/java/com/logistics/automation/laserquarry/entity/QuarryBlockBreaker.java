package com.logistics.automation.laserquarry.entity;

import com.logistics.api.LogisticsApi;
import com.logistics.api.TransportApi;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Stateless helpers for the laser quarry's block-breaking and item-output side effects.
 *
 * <p>Mines a target block, collects its drops, and routes them through a pipe above
 * the quarry, then a sided/regular inventory above, finally falling back to dropping
 * an item entity. Also provides {@link #insertIntoSlot(Container, int, ItemStack)} as
 * the pure inventory-merge primitive shared by both the inventory-output path and the
 * fallback nearby-item collection used for container blocks.
 */
public final class QuarryBlockBreaker {

    private QuarryBlockBreaker() {}

    public static void mineBlock(ServerLevel world, BlockPos quarryPos, BlockPos target, BlockState targetState) {
        BlockEntity blockEntity = world.getBlockEntity(target);
        List<ItemStack> drops = Block.getDrops(targetState, world, target, blockEntity, null, ItemStack.EMPTY);

        // Break the block without natural drops so we can route them ourselves.
        world.destroyBlock(target, false);

        for (ItemStack drop : drops) {
            outputItem(world, quarryPos, drop);
        }

        // For container drops the engine spawns items separately — sweep them up.
        if (drops.isEmpty() || blockEntity != null) {
            collectNearbyItems(world, quarryPos, target);
        }
    }

    public static void collectNearbyItems(ServerLevel world, BlockPos quarryPos, BlockPos target) {
        List<ItemEntity> itemEntities =
                world.getEntitiesOfClass(ItemEntity.class, new AABB(target).inflate(2.0), item -> true);

        for (ItemEntity itemEntity : itemEntities) {
            ItemStack stack = itemEntity.getItem();
            if (!stack.isEmpty()) {
                outputItem(world, quarryPos, stack.copy());
                itemEntity.discard();
            }
        }
    }

    public static void outputItem(ServerLevel world, BlockPos quarryPos, ItemStack stack) {
        if (stack.isEmpty()) return;

        BlockPos abovePos = quarryPos.above();

        // Prefer a transport block (pipe) directly above.
        BlockState aboveState = world.getBlockState(abovePos);
        TransportApi transportApi = LogisticsApi.Registry.transport();
        if (transportApi.isTransportBlock(aboveState)) {
            transportApi.forceInsert(world, abovePos, stack.copy(), Direction.UP);
            return;
        }

        // Fall back to a regular or sided inventory above.
        BlockEntity aboveEntity = world.getBlockEntity(abovePos);
        if (aboveEntity instanceof Container inv) {
            if (aboveEntity instanceof WorldlyContainer sidedInv) {
                int[] availableSlots = sidedInv.getSlotsForFace(Direction.DOWN);
                for (int slot : availableSlots) {
                    if (stack.isEmpty()) break;
                    if (!sidedInv.canPlaceItemThroughFace(slot, stack, Direction.DOWN)) continue;
                    stack = insertIntoSlot(inv, slot, stack);
                }
            } else {
                for (int slot = 0; slot < inv.getContainerSize(); slot++) {
                    if (stack.isEmpty()) break;
                    if (!inv.canPlaceItem(slot, stack)) continue;
                    stack = insertIntoSlot(inv, slot, stack);
                }
            }
        }

        // Anything left over drops above the quarry.
        if (!stack.isEmpty()) {
            double x = quarryPos.getX() + 0.5;
            double y = quarryPos.getY() + 1.5;
            double z = quarryPos.getZ() + 0.5;

            ItemEntity itemEntity = new ItemEntity(world, x, y, z, stack);
            itemEntity.setDeltaMovement(0, 0.2, 0);
            world.addFreshEntity(itemEntity);
        }
    }

    /**
     * Attempt to insert a stack into a single slot, returning the remainder.
     * Pure stack arithmetic — no side effects beyond the container itself.
     */
    public static ItemStack insertIntoSlot(Container inv, int slot, ItemStack stack) {
        ItemStack existing = inv.getItem(slot);

        if (existing.isEmpty()) {
            int maxInsert = Math.min(stack.getCount(), Math.min(inv.getMaxStackSize(), stack.getMaxStackSize()));
            inv.setItem(slot, stack.split(maxInsert));
        } else if (ItemStack.isSameItemSameComponents(existing, stack)) {
            int space = Math.min(inv.getMaxStackSize(), existing.getMaxStackSize()) - existing.getCount();
            if (space > 0) {
                int toInsert = Math.min(space, stack.getCount());
                existing.grow(toInsert);
                stack.shrink(toInsert);
            }
        }

        return stack;
    }
}
