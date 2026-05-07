package com.logistics.core.lib.storage;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps a vanilla {@link Container} as an {@link IItemStorage}.
 *
 * <p>Iterates slots linearly for both insertion and extraction.
 * For sided access, pass a wrapper that filters slots by face before constructing this.
 *
 * <p>Useful for block entities that implement {@link Container} and want to expose
 * {@link com.logistics.core.lib.block.capability.HasItemStorage} without depending on
 * any loader-specific storage API.
 */
public final class ContainerItemStorage implements IItemStorage {

    private final Container container;

    public ContainerItemStorage(Container container) {
        this.container = container;
    }

    @Override
    public long insert(IItemKey item, long maxAmount, boolean simulate) {
        long remaining = maxAmount;
        ItemStack template = item.toStack(1);

        for (int slot = 0; slot < container.getContainerSize() && remaining > 0; slot++) {
            if (!container.canPlaceItem(slot, template)) continue;
            ItemStack current = container.getItem(slot);
            if (current.isEmpty()) {
                long toInsert = Math.min(remaining, template.getMaxStackSize());
                if (!simulate) container.setItem(slot, item.toStack((int) toInsert));
                remaining -= toInsert;
            } else if (ItemStack.isSameItemSameComponents(current, template)) {
                long canFit = current.getMaxStackSize() - current.getCount();
                long toInsert = Math.min(remaining, canFit);
                if (toInsert > 0 && !simulate) {
                    current.grow((int) toInsert);
                    container.setChanged();
                }
                remaining -= toInsert;
            }
        }
        return maxAmount - remaining;
    }

    @Override
    public long extract(IItemKey item, long maxAmount, boolean simulate) {
        long remaining = maxAmount;
        for (int slot = 0; slot < container.getContainerSize() && remaining > 0; slot++) {
            ItemStack current = container.getItem(slot);
            if (current.isEmpty() || !item.matches(current)) continue;
            long toExtract = Math.min(remaining, current.getCount());
            if (!simulate) container.removeItem(slot, (int) toExtract);
            remaining -= toExtract;
        }
        return maxAmount - remaining;
    }

    @Override
    public Iterable<IItemView> contents() {
        List<IItemView> views = new ArrayList<>();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty()) {
                IItemKey key = ItemStorageLookup.of(stack);
                long amount = stack.getCount();
                views.add(new IItemView() {
                    @Override public IItemKey resource() { return key; }
                    @Override public long amount() { return amount; }
                });
            }
        }
        return views;
    }
}
