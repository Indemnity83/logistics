package com.logistics.automation;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** Shared per-slot inventory-merge primitive used by machine output routers. */
public final class ContainerInsert {

    private ContainerInsert() {}

    /**
     * Attempt to insert a stack into a single slot, returning the remainder.
     * Pure stack arithmetic — no side effects beyond the container itself.
     */
    public static ItemStack insertIntoSlot(Container inv, int slot, ItemStack stack) {
        ItemStack existing = inv.getItem(slot);

        if (existing.isEmpty()) {
            int maxInsert = Math.min(stack.getCount(), Math.min(inv.getMaxStackSize(), stack.getMaxStackSize()));
            inv.setItem(slot, stack.split(maxInsert));
            inv.setChanged();
        } else if (ItemStack.isSameItemSameComponents(existing, stack)) {
            int space = Math.min(inv.getMaxStackSize(), existing.getMaxStackSize()) - existing.getCount();
            if (space > 0) {
                int toInsert = Math.min(space, stack.getCount());
                existing.grow(toInsert);
                stack.shrink(toInsert);
                inv.setChanged();
            }
        }

        return stack;
    }
}
