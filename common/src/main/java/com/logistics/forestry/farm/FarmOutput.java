package com.logistics.forestry.farm;

import com.logistics.core.lib.compat.NbtCompat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * A farm's produce buffer: a range of output slots plus an overflow list for whatever did not fit.
 * Nothing a farm collects is ever dropped in the world; it waits in the overflow, which drains back
 * into the slots first and blocks further harvesting while it holds anything.
 */
final class FarmOutput {

    private final Container container;
    private final int fromSlot;
    private final int toSlotExclusive;
    private final List<ItemStack> overflow = new ArrayList<>();

    FarmOutput(Container container, int fromSlot, int toSlotExclusive) {
        this.container = container;
        this.fromSlot = fromSlot;
        this.toSlotExclusive = toSlotExclusive;
    }

    /** Stores {@code stack}: into the output slots as far as it fits, the rest into the overflow. */
    void accept(ItemStack stack) {
        ItemStack rest = insert(stack);
        if (!rest.isEmpty()) {
            addOverflow(rest);
        }
    }

    /** Moves as much of the overflow back into the output slots as fits; returns whether any moved. */
    boolean drainOverflow() {
        boolean moved = false;
        Iterator<ItemStack> it = overflow.iterator();
        while (it.hasNext()) {
            ItemStack stack = it.next();
            ItemStack rest = insert(stack);
            moved |= rest.getCount() != stack.getCount();
            if (rest.isEmpty()) {
                it.remove();
            } else {
                stack.setCount(rest.getCount());
            }
        }
        return moved;
    }

    /** True while the overflow holds anything or no output slot has room for even one more item. */
    boolean isFull() {
        if (!overflow.isEmpty()) {
            return true;
        }
        for (int slot = fromSlot; slot < toSlotExclusive; slot++) {
            ItemStack current = container.getItem(slot);
            if (current.isEmpty() || current.getCount() < limit(current)) {
                return false;
            }
        }
        return true;
    }

    List<ItemStack> overflow() {
        return Collections.unmodifiableList(overflow);
    }

    /** Count of items of {@code kind} in the output slots. */
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

    /** Removes up to {@code count} items of {@code kind} from the output slots; returns how many. */
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

    /** Inserts into the output slots, filling matching stacks before empty slots; returns the rest. */
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

    private void addOverflow(ItemStack stack) {
        ItemStack rest = stack.copy();
        for (ItemStack held : overflow) {
            if (ItemStack.isSameItemSameComponents(held, rest)) {
                int moved = Math.min(rest.getCount(), held.getMaxStackSize() - held.getCount());
                held.grow(moved);
                rest.shrink(moved);
                if (rest.isEmpty()) {
                    return;
                }
            }
        }
        while (!rest.isEmpty()) {
            int moved = Math.min(rest.getCount(), rest.getMaxStackSize());
            overflow.add(rest.copyWithCount(moved));
            rest.shrink(moved);
        }
    }

    private int limit(ItemStack stack) {
        return Math.min(stack.getMaxStackSize(), container.getMaxStackSize());
    }

    void save(CompoundTag tag, String key, HolderLookup.Provider registries) {
        if (overflow.isEmpty()) {
            return;
        }
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        ListTag list = new ListTag();
        for (ItemStack stack : overflow) {
            ItemStack.CODEC.encodeStart(ops, stack).result().ifPresent(list::add);
        }
        tag.put(key, list);
    }

    void load(CompoundTag tag, String key, HolderLookup.Provider registries) {
        overflow.clear();
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        ListTag list = NbtCompat.getListOrEmpty(tag, key);
        for (int i = 0; i < list.size(); i++) {
            NbtCompat.ifHasCompoundAt(list, i, itemTag -> ItemStack.CODEC
                    .parse(ops, itemTag)
                    .result()
                    .filter(stack -> !stack.isEmpty())
                    .ifPresent(overflow::add));
        }
    }
}
