package com.logistics.core.lib.storage;

import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps a vanilla {@link Container} as an {@link IItemStorage}.
 *
 * <p>Iterates slots linearly for both insertion and extraction.
 * When a {@link Direction} is provided and the container implements {@link WorldlyContainer},
 * slot access is restricted to {@link WorldlyContainer#getSlotsForFace} with the appropriate
 * directional placement/extraction checks.
 *
 * <p>Useful for block entities that implement {@link Container} and want to expose
 * {@link com.logistics.core.lib.block.capability.HasItemStorage} without depending on
 * any loader-specific storage API.
 */
public final class ContainerItemStorage implements IItemStorage {

    private final Container container;
    @Nullable private final Direction side;

    public ContainerItemStorage(Container container) {
        this(container, null);
    }

    public ContainerItemStorage(Container container, @Nullable Direction side) {
        this.container = container;
        this.side = side;
    }

    @Override
    public long insert(IItemKey item, long maxAmount, boolean simulate) {
        long remaining = maxAmount;
        ItemStack template = item.toStack(1);

        if (side != null && container instanceof WorldlyContainer wc) {
            for (int slot : wc.getSlotsForFace(side)) {
                if (remaining <= 0) break;
                if (!wc.canPlaceItemThroughFace(slot, template, side)) continue;
                remaining -= insertIntoSlot(item, template, slot, remaining, simulate);
            }
        } else {
            for (int slot = 0; slot < container.getContainerSize() && remaining > 0; slot++) {
                if (!container.canPlaceItem(slot, template)) continue;
                remaining -= insertIntoSlot(item, template, slot, remaining, simulate);
            }
        }
        return maxAmount - remaining;
    }

    private long insertIntoSlot(IItemKey item, ItemStack template, int slot, long remaining, boolean simulate) {
        ItemStack current = container.getItem(slot);
        if (current.isEmpty()) {
            long toInsert = Math.min(remaining, template.getMaxStackSize());
            if (!simulate) {
                container.setItem(slot, item.toStack((int) toInsert));
                container.setChanged();
            }
            return toInsert;
        } else if (ItemStack.isSameItemSameComponents(current, template)) {
            long canFit = current.getMaxStackSize() - current.getCount();
            long toInsert = Math.min(remaining, canFit);
            if (toInsert > 0 && !simulate) {
                current.grow((int) toInsert);
                container.setChanged();
            }
            return toInsert;
        }
        return 0;
    }

    @Override
    public long extract(IItemKey item, long maxAmount, boolean simulate) {
        long remaining = maxAmount;

        if (side != null && container instanceof WorldlyContainer wc) {
            for (int slot : wc.getSlotsForFace(side)) {
                if (remaining <= 0) break;
                ItemStack current = container.getItem(slot);
                if (current.isEmpty() || !item.matches(current)) continue;
                if (!wc.canTakeItemThroughFace(slot, current, side)) continue;
                long toExtract = Math.min(remaining, current.getCount());
                if (!simulate) container.removeItem(slot, (int) toExtract);
                remaining -= toExtract;
            }
        } else {
            for (int slot = 0; slot < container.getContainerSize() && remaining > 0; slot++) {
                ItemStack current = container.getItem(slot);
                if (current.isEmpty() || !item.matches(current)) continue;
                long toExtract = Math.min(remaining, current.getCount());
                if (!simulate) container.removeItem(slot, (int) toExtract);
                remaining -= toExtract;
            }
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
