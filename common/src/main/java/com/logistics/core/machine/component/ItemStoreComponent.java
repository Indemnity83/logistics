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
    private final int[] inputSlots;
    private final int[] outputSlots;

    public ItemStoreComponent(String id, SlotRole[] roles, SidedLayout layout, Runnable onChanged) {
        this.id = id;
        this.inventory = new ItemInventoryComponent(roles.length, onChanged);
        this.roles = roles.clone();
        this.layout = layout;
        this.inputSlots = slotsWithRole(roles, SlotRole.INPUT);
        this.outputSlots = slotsWithRole(roles, SlotRole.OUTPUT);
    }

    private static int[] slotsWithRole(SlotRole[] roles, SlotRole role) {
        return java.util.stream.IntStream.range(0, roles.length).filter(i -> roles[i] == role).toArray();
    }

    @Override
    public String id() {
        return id;
    }

    // ----- processing helpers -----

    /** Number of input slots (in declaration order). */
    public int inputCount() {
        return inputSlots.length;
    }

    public ItemStack input() {
        return input(0);
    }

    /** The stack in the input slot at {@code inputIndex} (declaration order), or empty when absent. */
    public ItemStack input(int inputIndex) {
        if (inputIndex < 0 || inputIndex >= inputSlots.length) {
            return ItemStack.EMPTY;
        }
        return inventory.getItem(inputSlots[inputIndex]);
    }

    public void consumeInput(int count) {
        consumeInput(0, count);
    }

    /** Consumes {@code count} from the input slot at {@code inputIndex} (declaration order). */
    public void consumeInput(int inputIndex, int count) {
        if (inputIndex >= 0 && inputIndex < inputSlots.length && count > 0) {
            inventory.getItem(inputSlots[inputIndex]).shrink(count);
            inventory.setChanged();
        }
    }

    /** Number of output slots (in declaration order). */
    public int outputCount() {
        return outputSlots.length;
    }

    /** Whether {@code stack} fits into the output slot at {@code outputIndex}. */
    public boolean canAcceptInto(int outputIndex, ItemStack stack) {
        if (outputIndex < 0 || outputIndex >= outputSlots.length) {
            return false;
        }
        ItemStack output = inventory.getItem(outputSlots[outputIndex]);
        if (output.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(output, stack)
                && output.getCount() + stack.getCount() <= output.getMaxStackSize();
    }

    /** Places {@code stack} into the output slot at {@code outputIndex} (merging into a match). */
    public void produceInto(int outputIndex, ItemStack stack) {
        if (outputIndex < 0 || outputIndex >= outputSlots.length || stack.isEmpty()) {
            return;
        }
        int slot = outputSlots[outputIndex];
        ItemStack output = inventory.getItem(slot);
        if (output.isEmpty()) {
            inventory.setItem(slot, stack.copyWithCount(Math.min(stack.getCount(), stack.getMaxStackSize())));
        } else {
            int room = output.getMaxStackSize() - output.getCount();
            int add = Math.min(room, stack.getCount());
            if (add > 0) {
                output.grow(add);
                inventory.setChanged();
            }
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
