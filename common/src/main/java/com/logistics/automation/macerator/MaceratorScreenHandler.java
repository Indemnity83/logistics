package com.logistics.automation.macerator;

import com.logistics.LogisticsAutomation;
import com.logistics.core.lib.block.MachineResultSlot;
import com.logistics.core.machine.MachineData;
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

    private static final int MACHINE_SLOT_COUNT = 3;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 36; // 27 + 9 hotbar

    private final Container inventory;
    private final ContainerData data;

    /** Client-side constructor. */
    public MaceratorScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(MACHINE_SLOT_COUNT), new SimpleContainerData(MachineData.COUNT));
    }

    /** Server-side constructor. */
    public MaceratorScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData data) {
        super(LogisticsAutomation.MENU.MACERATOR, syncId);
        checkContainerSize(inventory, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, MachineData.COUNT);

        this.inventory = inventory;
        this.data = data;

        inventory.startOpen(playerInventory.player);

        // Slot positions match the Sawmill GUI. Input (0) on the left.
        this.addSlot(new Slot(inventory, MaceratorBlockEntity.INPUT_SLOT, 56, 35));

        // Primary output (1, upper right) — no insertion; releases banked maceration XP to the player on take
        this.addSlot(new MachineResultSlot(inventory, MaceratorBlockEntity.OUTPUT_SLOT, 116, 25));

        // Secondary chance byproduct (2, lower right) — extraction only, no XP
        this.addSlot(new OutputSlot(inventory, MaceratorBlockEntity.SECONDARY_OUTPUT_SLOT, 116, 51));

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

    /** Progress arrow width (0..24 px) from the synced progress fraction, or 0 if idle. */
    public int getProgressArrowWidth() {
        return MachineData.barPixels(data, MachineData.PROGRESS, 24);
    }

    /** Energy bar height (0..30 px) from the synced energy fill fraction. */
    public int getEnergyBarHeight() {
        return MachineData.barPixels(data, MachineData.ENERGY, 30);
    }

    /** Output slot — players may take but not insert. */
    private static class OutputSlot extends Slot {
        OutputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
