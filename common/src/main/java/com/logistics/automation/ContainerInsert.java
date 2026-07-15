package com.logistics.automation;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** Shared per-slot inventory-merge primitive used by machine output routers. */
public final class ContainerInsert {

    private ContainerInsert() {}

    /**
     * Attempt to insert {@code stack} into a single slot. Mutates {@code stack} in place, reducing it
     * by the amount that fit, and returns it as the remainder (empty if fully inserted).
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
