package com.logistics.automation.kiln;

import com.logistics.LogisticsAutomation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Screen handler for the Electric Kiln GUI.
 * Manages input/output slots and syncs progress/energy data to the client.
 */
public class KilnScreenHandler extends AbstractContainerMenu {

    private static final int MACHINE_SLOT_COUNT = 2;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 36;

    private final Container inventory;
    private final ContainerData data;

    /** Client-side constructor. */
    public KilnScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(MACHINE_SLOT_COUNT), new SimpleContainerData(KilnBlockEntity.DATA_COUNT));
    }

    /** Server-side constructor. */
    public KilnScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData data) {
        super(LogisticsAutomation.MENU.KILN, syncId);
        checkContainerSize(inventory, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, KilnBlockEntity.DATA_COUNT);

        this.inventory = inventory;
        this.data = data;

        inventory.startOpen(playerInventory.player);

        this.addSlot(new Slot(inventory, KilnBlockEntity.INPUT_SLOT, 56, 35));

        this.addSlot(new Slot(inventory, KilnBlockEntity.OUTPUT_SLOT, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        this.addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index == KilnBlockEntity.OUTPUT_SLOT) {
                if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, result);
            } else if (index >= PLAYER_INVENTORY_START) {
                if (!this.moveItemStackTo(stack, KilnBlockEntity.INPUT_SLOT, KilnBlockEntity.INPUT_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stack);
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.inventory.stillValid(player);
    }

    public int getProcessProgress() { return data.get(0); }
    public int getProcessTotalTicks() { return data.get(1); }
    public int getEnergyStored() { return data.get(2); }
    public int getEnergyCapacity() { return (int) KilnBlockEntity.ENERGY_CAPACITY; }

    public int getProgressArrowWidth() {
        int total = getProcessTotalTicks();
        if (total <= 0) return 0;
        return 25 * getProcessProgress() / total;
    }

    public int getEnergyBarHeight() {
        int capacity = getEnergyCapacity();
        if (capacity <= 0) return 0;
        return 13 * getEnergyStored() / capacity;
    }
}
