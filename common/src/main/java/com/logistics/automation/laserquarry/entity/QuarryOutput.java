package com.logistics.automation.laserquarry.entity;

import com.logistics.api.LogisticsApi;
import com.logistics.api.TransportApi;
import com.logistics.automation.ContainerInsert;
import java.util.List;
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
 * regular inventory above, finally dropping an item entity. Also sweeps up items that broken
 * container blocks spawn separately. The per-slot merge is delegated to {@link ContainerInsert}.
 */
public final class QuarryOutput {

    private final BlockPos quarryPos;

    public QuarryOutput(BlockPos quarryPos) {
        this.quarryPos = quarryPos;
    }

    /** Route a single stack out: pipe above -> inventory above -> dropped above the quarry. */
    public void accept(ServerLevel world, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        BlockPos abovePos = quarryPos.above();

        // Prefer a transport block (pipe) directly above.
        var aboveState = world.getBlockState(abovePos);
        TransportApi transportApi = LogisticsApi.Registry.transport();
        if (transportApi.isTransportBlock(aboveState)) {
            if (transportApi.forceInsert(world, abovePos, stack.copy(), Direction.UP)) {
                return;
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

    /** Sweep up items that a broken container block spawned near {@code target}. */
    public void sweepNearby(ServerLevel world, BlockPos target) {
        List<ItemEntity> itemEntities =
                world.getEntitiesOfClass(ItemEntity.class, new AABB(target).inflate(2.0), item -> true);

        for (ItemEntity itemEntity : itemEntities) {
            ItemStack stack = itemEntity.getItem();
            if (!stack.isEmpty()) {
                accept(world, stack.copy());
                itemEntity.discard();
            }
        }
    }
}
