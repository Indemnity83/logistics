package com.logistics.automation.macerator;

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
 * Screen handler for the Iron Macerator GUI.
 * Manages input/output slots and syncs progress/energy data to the client.
 */
public class MaceratorScreenHandler extends AbstractContainerMenu {

    private static final int MACHINE_SLOT_COUNT = 2;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 36; // 27 + 9 hotbar

    private final Container inventory;
    private final ContainerData data;

    /** Client-side constructor. */
    public MaceratorScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(MACHINE_SLOT_COUNT), new SimpleContainerData(MaceratorBlockEntity.DATA_COUNT));
    }

    /** Server-side constructor. */
    public MaceratorScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData data) {
        super(LogisticsAutomation.MENU.MACERATOR, syncId);
        checkContainerSize(inventory, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, MaceratorBlockEntity.DATA_COUNT);

        this.inventory = inventory;
        this.data = data;

        inventory.startOpen(playerInventory.player);

        // Input slot (0) — left side of GUI
        this.addSlot(new Slot(inventory, MaceratorBlockEntity.INPUT_SLOT, 56, 35));

        // Output slot (1) — right side of GUI, no insertion allowed
        this.addSlot(new Slot(inventory, MaceratorBlockEntity.OUTPUT_SLOT, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        // Player inventory (3 rows of 9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player hotbar
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

            if (index == MaceratorBlockEntity.OUTPUT_SLOT) {
                // Move output to player inventory
                if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, result);
            } else if (index >= PLAYER_INVENTORY_START) {
                // Move from player inventory to input slot
                if (!this.moveItemStackTo(stack, MaceratorBlockEntity.INPUT_SLOT, MaceratorBlockEntity.INPUT_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Move from machine to player inventory
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

    // ==================== Data Getters for GUI Rendering ====================

    /** Progress in ticks (0 to processTotalTicks). */
    public int getProcessProgress() {
        return data.get(0);
    }

    /** Total ticks for the current recipe (0 if idle). */
    public int getProcessTotalTicks() {
        return data.get(1);
    }

    /** Current energy stored (RF). */
    public int getEnergyStored() {
        return data.get(2);
    }

    /** Max energy capacity (RF). */
    public int getEnergyCapacity() {
        return (int) MaceratorBlockEntity.ENERGY_CAPACITY;
    }

    /** Progress as a 0–24 pixel width for the arrow, or 0 if idle. */
    public int getProgressArrowWidth() {
        int total = getProcessTotalTicks();
        if (total <= 0) return 0;
        return 24 * getProcessProgress() / total;
    }

    /** Energy bar height in pixels (0–52). */
    public int getEnergyBarHeight() {
        int capacity = getEnergyCapacity();
        if (capacity <= 0) return 0;
        return 52 * getEnergyStored() / capacity;
    }
}
