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

    /** How long the quarry keeps looking for contents a broken container has not handed over yet. */
    private static final int CLAIM_TICKS = 40;

    private final BlockPos quarryPos;

    /** Contents a broken container owes the quarry but that were not on the ground yet. */
    private final List<Claim> claims = new ArrayList<>();

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
     * Sweep up the items a break spawned near {@code target} — those, and only those, are the
     * quarry's. Anything in {@code alreadyThere} was on the ground before the break and stays.
     *
     * @return copies of what was taken, so a caller can tell what a container still owes it
     */
    public List<ItemStack> sweepNearby(ServerLevel world, BlockPos target, Set<Integer> alreadyThere) {
        List<ItemStack> taken = new ArrayList<>();
        for (ItemEntity itemEntity : world.getEntitiesOfClass(ItemEntity.class, sweepArea(target))) {
            if (alreadyThere.contains(itemEntity.getId())) {
                continue;
            }
            ItemStack stack = itemEntity.getItem();
            if (!stack.isEmpty()) {
                taken.add(stack.copy());
                accept(world, stack.copy());
                itemEntity.discard();
            }
        }
        return taken;
    }

    /**
     * Keep looking, for a short while, for contents a broken container has not handed over.
     *
     * <p>A container spills its contents as item entities, and an entity query cannot see into a
     * chunk section the server has not finished making visible — the section is skipped outright,
     * so a freshly spilled stack reads as absent even though it is lying right there. That window
     * closes within a tick or two, but the quarry breaks a block and moves on, so without a second
     * look the contents stay on the floor until they despawn.
     *
     * <p>{@code outstanding} is what the container demonstrably spilled — the drop in its own
     * contents across the break — less what the sweep already collected. Nothing is ever taken out
     * of the container, and no assumption is made about which blocks spill: one that keeps its
     * contents, like a shulker box, spills nothing by that measure and so claims nothing. Claims
     * are matched by item and components, which is what keeps a waiting retry off a player's
     * belongings (#973).
     */
    public void claimLater(BlockPos target, List<ItemStack> outstanding) {
        // Copied because a claim is drawn down as it is filled, and the caller's list is its own.
        List<ItemStack> owed = new ArrayList<>();
        for (ItemStack stack : outstanding) {
            if (!stack.isEmpty()) {
                owed.add(stack.copy());
            }
        }
        if (!owed.isEmpty()) {
            claims.add(new Claim(target, owed));
        }
    }

    /** Collect anything owed that has since become visible. Bounded by {@link #CLAIM_TICKS}. */
    public void tickClaims(ServerLevel world) {
        Iterator<Claim> iterator = claims.iterator();
        while (iterator.hasNext()) {
            Claim claim = iterator.next();
            collect(world, claim);
            if (claim.outstanding.isEmpty() || ++claim.age >= CLAIM_TICKS) {
                iterator.remove();
            }
        }
    }

    private void collect(ServerLevel world, Claim claim) {
        for (ItemEntity itemEntity : world.getEntitiesOfClass(ItemEntity.class, sweepArea(claim.target))) {
            if (claim.outstanding.isEmpty()) {
                return;
            }
            ItemStack onGround = itemEntity.getItem();
            if (onGround.isEmpty()) {
                continue;
            }
            for (Iterator<ItemStack> owed = claim.outstanding.iterator(); owed.hasNext(); ) {
                ItemStack want = owed.next();
                if (!ItemStack.isSameItemSameComponents(want, onGround)) {
                    continue;
                }
                int moved = Math.min(want.getCount(), onGround.getCount());
                accept(world, onGround.split(moved));
                want.shrink(moved);
                if (want.isEmpty()) {
                    owed.remove();
                }
                if (onGround.isEmpty()) {
                    itemEntity.discard();
                    break;
                }
            }
        }
    }

    private static AABB sweepArea(BlockPos target) {
        return new AABB(target).inflate(2.0);
    }

    /** Contents owed by a container broken at {@code target}, and how long we have been waiting. */
    private static final class Claim {
        private final BlockPos target;
        private final List<ItemStack> outstanding;
        private int age;

        private Claim(BlockPos target, List<ItemStack> outstanding) {
            this.target = target;
            this.outstanding = outstanding;
        }
    }
}
