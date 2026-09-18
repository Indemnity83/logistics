package com.logistics.automation.laserquarry.entity;

import com.logistics.api.LogisticsApi;
import com.logistics.api.TransportApi;
import com.logistics.automation.ContainerInsert;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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

    /** How long the quarry keeps sweeping the spot where it broke a block entity. */
    private static final int SWEEP_TICKS = 20;

    private final BlockPos quarryPos;

    /** Spots the quarry is still sweeping, because a break there may not have shown up yet. */
    private final List<PendingSweep> pending = new ArrayList<>();

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

    /** Ids of the items already lying in the sweep area, taken before a block is broken. */
    public Set<Integer> itemsNear(ServerLevel world, BlockPos target) {
        return world.getEntitiesOfClass(ItemEntity.class, sweepArea(target)).stream()
                .map(ItemEntity::getId)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Sweep up the items a break spawned near {@code target} -- those, and only those, are the
     * quarry's. Anything in {@code alreadyThere} was on the ground before the break and stays.
     */
    public void sweepNearby(ServerLevel world, BlockPos target, Set<Integer> alreadyThere) {        for (ItemEntity itemEntity : world.getEntitiesOfClass(ItemEntity.class, sweepArea(target))) {
            if (alreadyThere.contains(itemEntity.getId())) {
                continue;
            }
            ItemStack stack = itemEntity.getItem();
            if (!stack.isEmpty()) {
                accept(world, stack.copy());
                itemEntity.discard();
            }
        }
    }

    /**
     * Keep sweeping {@code target} for a short while after breaking a block entity there.
     *
     * <p>A container spills its contents as item entities, and an entity query cannot see into a
     * chunk section the server has not finished making visible -- the section is skipped outright,
     * so a freshly spilled stack reads as absent even though it is lying right there. The window
     * closes within a tick or two, but the quarry breaks a block and moves on, so a single look
     * leaves the contents on the floor until they despawn.
     *
     * <p>{@code alreadyThere} carries over from the break, so items lying around beforehand stay
     * put. Anything that turns up during the window is taken, which includes items the quarry did
     * not spill -- a deliberate trade for a sweep that stays fast and predictable, and bounded to
     * {@link #SWEEP_TICKS} around a block the quarry actually broke.
     */
    public void sweepAgainFor(BlockPos target, Set<Integer> alreadyThere) {
        pending.add(new PendingSweep(target, alreadyThere));
    }

    /** Re-sweep the spots still in their window, and retire the ones that have run out. */
    public void tickPendingSweeps(ServerLevel world) {
        Iterator<PendingSweep> iterator = pending.iterator();
        while (iterator.hasNext()) {
            PendingSweep sweep = iterator.next();
            sweepNearby(world, sweep.target, sweep.alreadyThere);
            if (++sweep.age >= SWEEP_TICKS) {
                iterator.remove();
            }
        }
    }

    private static AABB sweepArea(BlockPos target) {
        return new AABB(target).inflate(2.0);
    }

    /** A spot the quarry broke a block entity at, and how long it has been watching it. */
    private static final class PendingSweep {
        private final BlockPos target;
        private final Set<Integer> alreadyThere;
        private int age;

        private PendingSweep(BlockPos target, Set<Integer> alreadyThere) {
            this.target = target;
            this.alreadyThere = alreadyThere;
        }
    }
}
