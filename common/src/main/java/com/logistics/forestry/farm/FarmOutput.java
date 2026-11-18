package com.logistics.forestry.farm;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** A farm's view of a range of its inventory's slots: counting, taking and inserting stacks. */
final class FarmOutput {

    private final Container container;
    private final int fromSlot;
    private final int toSlotExclusive;

    FarmOutput(Container container, int fromSlot, int toSlotExclusive) {
        this.container = container;
        this.fromSlot = fromSlot;
        this.toSlotExclusive = toSlotExclusive;
    }

    /** Count of items of {@code kind} in the slots. */
    int count(ItemStack kind) {
        int total = 0;
        for (int slot = fromSlot; slot < toSlotExclusive; slot++) {
            ItemStack current = container.getItem(slot);
            if (ItemStack.isSameItemSameComponents(current, kind)) {
                total += current.getCount();
            }
        }
        return total;
    }

    /** Removes up to {@code count} items of {@code kind} from the slots; returns how many. */
    int take(ItemStack kind, int count) {
        int taken = 0;
        for (int slot = fromSlot; slot < toSlotExclusive && taken < count; slot++) {
            ItemStack current = container.getItem(slot);
            if (ItemStack.isSameItemSameComponents(current, kind)) {
                int n = Math.min(count - taken, current.getCount());
                container.removeItem(slot, n);
                taken += n;
            }
        }
        return taken;
    }

    /** Inserts into the slots, filling matching stacks before empty slots; returns the rest. */
    ItemStack insert(ItemStack stack) {
        ItemStack rest = stack.copy();
        for (int slot = fromSlot; slot < toSlotExclusive && !rest.isEmpty(); slot++) {
            ItemStack current = container.getItem(slot);
            if (!current.isEmpty() && ItemStack.isSameItemSameComponents(current, rest)) {
                int moved = Math.min(rest.getCount(), limit(current) - current.getCount());
                if (moved > 0) {
                    current.grow(moved);
                    rest.shrink(moved);
                    container.setChanged();
                }
            }
        }
        for (int slot = fromSlot; slot < toSlotExclusive && !rest.isEmpty(); slot++) {
            if (container.getItem(slot).isEmpty()) {
                int moved = Math.min(rest.getCount(), limit(rest));
                container.setItem(slot, rest.copyWithCount(moved));
                rest.shrink(moved);
            }
        }
        return rest;
    }

    private int limit(ItemStack stack) {
        return Math.min(stack.getMaxStackSize(), container.getMaxStackSize());
    }
}
