package com.logistics.automation.laserquarry.entity;

import com.logistics.api.LogisticsApi;
import com.logistics.api.TransportApi;
import com.logistics.automation.ContainerInsert;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

/**
 * The laser quarry's item sink: routes mined drops to a pipe above the quarry, then to a sided or
 * regular inventory above, finally dropping an item entity. Also collects loose items lying in the
 * quarry's own area. The per-slot merge is delegated to {@link ContainerInsert}.
 */
public final class QuarryOutput {

    private final BlockPos quarryPos;

    public QuarryOutput(BlockPos quarryPos) {
        this.quarryPos = quarryPos;
    }

    /** Route a single stack out: pipe above -> inventory above -> dropped above the quarry. */
    public void accept(ServerLevel world, ItemStack stack) {
        ItemStack leftover = route(world, stack);
        if (!leftover.isEmpty()) {
            double x = quarryPos.getX() + 0.5;
            double y = quarryPos.getY() + 1.5;
            double z = quarryPos.getZ() + 0.5;

            ItemEntity itemEntity = new ItemEntity(world, x, y, z, leftover);
            itemEntity.setDeltaMovement(0, 0.2, 0);
            world.addFreshEntity(itemEntity);
        }
    }

    /**
     * Take every loose item lying in {@code area} and route it out.
     *
     * <p>A quarry's pit fills with items it has no other way to reach: a chest's contents, spilled
     * as entities the tick it was broken; an item frame that only notices its wall is gone up to a
     * hundred ticks later, and pops off somewhere the quarry has long since left; dripstone that
     * takes a moment to fall. Rather than predict which break produces what and when, the quarry
     * simply picks up what it finds inside its own frame — so items a player leaves in the pit are
     * collected too.
     *
     * <p>An item is only taken when there is somewhere to put it. A full output would otherwise
     * have the quarry drop it straight back on the floor and pick it up again on the next pass.
     */
    public void collectLooseItems(ServerLevel world, AABB area) {
        for (ItemEntity itemEntity : world.getEntitiesOfClass(ItemEntity.class, area)) {
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack leftover = route(world, stack.copy());
            if (leftover.isEmpty()) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(leftover);
            }
        }
    }

    /** Push a stack at the pipe or inventory above the quarry; returns what would not fit. */
    private ItemStack route(ServerLevel world, ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        BlockPos abovePos = quarryPos.above();

        // Prefer a transport block (pipe) directly above.
        var aboveState = world.getBlockState(abovePos);
        TransportApi transportApi = LogisticsApi.Registry.transport();
        if (transportApi.isTransportBlock(aboveState)) {
            if (transportApi.forceInsert(world, abovePos, stack.copy(), Direction.UP)) {
                return ItemStack.EMPTY;
            }
        }

        // Fall back to a regular or sided inventory above.
        BlockEntity aboveEntity = world.getBlockEntity(abovePos);
        if (aboveEntity instanceof Container inv) {
            if (aboveEntity instanceof WorldlyContainer sidedInv) {
                int[] availableSlots = sidedInv.getSlotsForFace(Direction.DOWN);
                for (int slot : availableSlots) {
                    if (stack.isEmpty()) break;
                    if (!sidedInv.canPlaceItemThroughFace(slot, stack, Direction.DOWN)) continue;
                    stack = ContainerInsert.insertIntoSlot(inv, slot, stack);
                }
            } else {
                for (int slot = 0; slot < inv.getContainerSize(); slot++) {
                    if (stack.isEmpty()) break;
                    if (!inv.canPlaceItem(slot, stack)) continue;
                    stack = ContainerInsert.insertIntoSlot(inv, slot, stack);
                }
            }
        }

        return stack;
    }
}
