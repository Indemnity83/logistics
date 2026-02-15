package com.logistics.core.lib.items;

import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

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
        this.storage = InventoryStorage.of(this, null);
    }

    public Storage<ItemVariant> storage() {
        return storage;
    }

    public void readNbt(CompoundTag nbt, String key) {
        if (nbt.contains(key)) {
            ContainerHelper.loadAllItems(nbt.getCompound(key), stacks, null);
        }
    }

    public void writeNbt(CompoundTag nbt, String key) {
        nbt.put(key, ContainerHelper.saveAllItems(new CompoundTag(), stacks, null));
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
        ItemStack result = ContainerHelper.removeItem(stacks, slot, amount);
        if (!result.isEmpty()) {
            onChanged.run();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = ContainerHelper.takeItem(stacks, slot);
        if (!result.isEmpty()) {
            onChanged.run();
        }
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
        stacks.clear();
        onChanged.run();
    }
}