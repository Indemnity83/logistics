package com.logistics.core.lib.items;

import com.logistics.core.lib.compat.NbtCompat;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;

/**
 * Minimal item inventory component that implements vanilla {@link Container}
 * and exposes a Transfer API {@link Storage} wrapper.
 * <p>
 * Use this for simple fixed-size inventories. For more complex inventory behavior,
 * implement {@link Container} directly.
 */
public final class ItemInventoryComponent implements Container {
    private final NonNullList<ItemStack> stacks;
    private final Storage<ItemVariant> storage;
    private final Runnable onChanged;

    public ItemInventoryComponent(int size, Runnable onChanged) {
        this.stacks = NonNullList.withSize(size, ItemStack.EMPTY);
        this.onChanged = onChanged;

        // Transfer API wrapper over vanilla Inventory
        this.storage = ContainerStorage.of(this, null);
    }

    public Storage<ItemVariant> storage() {
        return storage;
    }

    public void readNbt(CompoundTag nbt, String key, HolderLookup.Provider registries) {
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        ListTag list = NbtCompat.getListOrEmpty(nbt, key);

        // Clear existing stacks
        Collections.fill(stacks, ItemStack.EMPTY);

        // Load items from list
        for (int i = 0; i < list.size(); i++) {
            loadItemAt(ops, list, i);
        }
    }

    public void writeNbt(CompoundTag nbt, String key, HolderLookup.Provider registries) {
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        ListTag listTag = new ListTag();
        for (int i = 0; i < stacks.size(); i++) {
            saveItemAt(ops, listTag, i);
        }
        if (!listTag.isEmpty()) {
            nbt.put(key, listTag);
        }
    }

    private void loadItemAt(RegistryOps<Tag> ops, ListTag list, int index) {
        NbtCompat.ifHasCompoundAt(list, index, itemTag -> {
            int slot = NbtCompat.getInt(itemTag, "Slot", 0) & 255;
            if (slot < stacks.size()) {
                ItemStack.CODEC.parse(ops, itemTag)
                        .result()
                        .ifPresent(stack -> stacks.set(slot, stack));
            }
        });
    }

    private void saveItemAt(RegistryOps<Tag> ops, ListTag list, int slot) {
        ItemStack stack = stacks.get(slot);
        if (stack.isEmpty()) {
            return;
        }

        ItemStack.CODEC.encodeStart(ops, stack)
                .result()
                .ifPresent(tag -> {
                    if (tag instanceof CompoundTag itemTag) {
                        itemTag.putByte("Slot", (byte) slot);
                        list.add(itemTag);
                    }
                });
    }

    // ----- Container impl -----

    @Override
    public int getContainerSize() {
        return stacks.size();
    }

    @Override
    public boolean isEmpty() {
        return stacks.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return stacks.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = getItem(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack result;
        if (stack.getCount() <= amount) {
            result = stack;
            setItem(slot, ItemStack.EMPTY);
        } else {
            result = stack.split(amount);
            if (stack.isEmpty()) {
                setItem(slot, ItemStack.EMPTY);
            } else {
                onChanged.run();
            }
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = stacks.get(slot);
        stacks.set(slot, ItemStack.EMPTY);
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        stacks.set(slot, stack);
        onChanged.run();
    }

    @Override
    public void setChanged() {
        onChanged.run();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        Collections.fill(stacks, ItemStack.EMPTY);
        onChanged.run();
    }
}
