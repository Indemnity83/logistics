package com.logistics.automation.sawmill;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** Pure processing logic for the Sawmill — energy/progress stepping and multi-slot output insertion. */
final class SawmillProcessingPlan {
    private SawmillProcessingPlan() {}

    static Result advance(int progress, int totalTicks, long energyStored, int energyPerTick, boolean outputAccepted) {
        if (energyStored < energyPerTick || !outputAccepted) {
            return new Result(progress, false, false);
        }
        int next = progress + 1;
        return new Result(next, true, next >= totalTicks);
    }

    /** True if {@code stack} fits across the given output slots (merging into matches, then empties). */
    static boolean canInsert(Container container, int[] slots, ItemStack stack) {
        if (stack.isEmpty()) return true;
        int remaining = stack.getCount();
        for (int slot : slots) {
            ItemStack existing = container.getItem(slot);
            if (existing.isEmpty()) {
                remaining -= stack.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(existing, stack)) {
                remaining -= existing.getMaxStackSize() - existing.getCount();
            }
            if (remaining <= 0) return true;
        }
        return remaining <= 0;
    }

    /** Places {@code stack} into the output slots (matches first, then empties). Returns the leftover count. */
    static int insert(Container container, int[] slots, ItemStack stack) {
        int remaining = stack.getCount();
        for (int slot : slots) {
            if (remaining <= 0) break;
            ItemStack existing = container.getItem(slot);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack)) {
                int moved = Math.min(existing.getMaxStackSize() - existing.getCount(), remaining);
                if (moved > 0) {
                    existing.grow(moved);
                    container.setItem(slot, existing);
                    remaining -= moved;
                }
            }
        }
        for (int slot : slots) {
            if (remaining <= 0) break;
            if (container.getItem(slot).isEmpty()) {
                int moved = Math.min(stack.getMaxStackSize(), remaining);
                container.setItem(slot, stack.copyWithCount(moved));
                remaining -= moved;
            }
        }
        return remaining;
    }

    record Result(int progress, boolean consumedEnergy, boolean complete) {}
}
