package com.logistics.automation.fabricator;

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

/**
 * Pushes a produced stack out of a machine: tries every side except {@link Direction#UP} (the
 * fabricator's input face) for a pipe or inventory, and drops whatever will not fit. Reuses
 * {@link ContainerInsert#insertIntoSlot} for the per-slot merge.
 */
public final class NeighborItemOutput {

    private NeighborItemOutput() {}

    /** Eject {@code stack} out any non-top face into a pipe/inventory, dropping the remainder. */
    public static void eject(ServerLevel level, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        TransportApi transportApi = LogisticsApi.Registry.transport();

        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP || stack.isEmpty()) {
                continue;
            }
            BlockPos neighborPos = pos.relative(dir);
            Direction insertFace = dir.getOpposite();

            // Prefer a transport block (pipe) on this side.
            if (transportApi.isTransportBlock(level.getBlockState(neighborPos))
                    && transportApi.forceInsert(level, neighborPos, stack.copy(), dir)) {
                return;
            }

            // Fall back to a regular or sided inventory on this side.
            BlockEntity neighborEntity = level.getBlockEntity(neighborPos);
            if (neighborEntity instanceof Container inv) {
                if (inv instanceof WorldlyContainer sidedInv) {
                    for (int slot : sidedInv.getSlotsForFace(insertFace)) {
                        if (stack.isEmpty()) break;
                        if (!sidedInv.canPlaceItemThroughFace(slot, stack, insertFace)) continue;
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
        }

        if (!stack.isEmpty()) {
            ItemEntity itemEntity = new ItemEntity(
                    level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            itemEntity.setDeltaMovement(0, 0.1, 0);
            level.addFreshEntity(itemEntity);
        }
    }
}
