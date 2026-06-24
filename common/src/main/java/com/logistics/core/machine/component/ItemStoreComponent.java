package com.logistics.core.machine.component;

import com.logistics.core.lib.items.ItemInventoryComponent;
import com.logistics.core.machine.MachineComponent;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Fixed-size item inventory component wrapping the {@link ItemInventoryComponent} holder.
 *
 * <p>Carries a {@link SlotRole} per slot and a {@link SidedLayout} describing furnace-style sided
 * access. The host delegates its {@code WorldlyContainer} methods here via {@link SidedItems};
 * work components read/write the input and output slots through the role-based helpers.
 */
public final class ItemStoreComponent implements MachineComponent, MachineComponent.SidedItems {

    private static final String LEGACY_KEY = "Inventory";

    private final String id;
    private final ItemInventoryComponent inventory;
    private final SlotRole[] roles;
    private final SidedLayout layout;
    private final int inputSlot;
    private final int outputSlot;

    public ItemStoreComponent(String id, SlotRole[] roles, SidedLayout layout, Runnable onChanged) {
        this.id = id;
        this.inventory = new ItemInventoryComponent(roles.length, onChanged);
        this.roles = roles.clone();
        this.layout = layout;
        this.inputSlot = firstSlot(roles, SlotRole.INPUT);
        this.outputSlot = firstSlot(roles, SlotRole.OUTPUT);
    }

    private static int firstSlot(SlotRole[] roles, SlotRole role) {
        for (int i = 0; i < roles.length; i++) {
            if (roles[i] == role) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String id() {
        return id;
    }

    // ----- processing helpers -----

    public ItemStack input() {
        return inputSlot >= 0 ? inventory.getItem(inputSlot) : ItemStack.EMPTY;
    }

    public void shrinkInput() {
        if (inputSlot >= 0) {
            inventory.getItem(inputSlot).shrink(1);
            inventory.setChanged();
        }
    }

    public boolean canAcceptOutput(ItemStack result) {
        if (outputSlot < 0) {
            return false;
        }
        ItemStack output = inventory.getItem(outputSlot);
        if (output.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    public void produceOutput(ItemStack result) {
        if (outputSlot < 0) {
            return;
        }
        ItemStack output = inventory.getItem(outputSlot);
        if (output.isEmpty()) {
            inventory.setItem(outputSlot, result.copy());
        } else {
            output.grow(result.getCount());
            inventory.setChanged();
        }
    }

    // ----- SidedItems -----

    @Override
    public Container container() {
        return inventory;
    }

    @Override
    public int[] slotsForFace(Direction side) {
        return layout.slotsForFace(side);
    }

    @Override
    public boolean canPlace(int slot, ItemStack stack, Direction dir) {
        return layout.canPlace(slot, stack, dir);
    }

    @Override
    public boolean canTake(int slot, ItemStack stack, Direction dir) {
        return layout.canTake(slot, stack, dir);
    }

    // ----- persistence -----

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        inventory.writeNbt(tag, "items", registries);
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        inventory.readNbt(tag, "items", registries);
    }

    @Override
    public void loadLegacy(CompoundTag root, HolderLookup.Provider registries) {
        inventory.readNbt(root, LEGACY_KEY, registries);
    }
}
