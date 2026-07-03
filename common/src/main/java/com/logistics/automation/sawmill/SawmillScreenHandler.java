package com.logistics.automation.sawmill;

import com.logistics.LogisticsAutomation;
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
 * Screen handler for the Sawmill GUI.
 * Input slot plus primary (planks/sticks) and secondary (Wood Pulp) output slots, with
 * progress + energy synced to the client.
 */
public class SawmillScreenHandler extends AbstractContainerMenu {

    private static final int MACHINE_SLOT_COUNT = SawmillBlockEntity.TOTAL_SLOTS;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 36;

    private final Container inventory;
    private final ContainerData data;

    /** Client-side constructor. */
    public SawmillScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(MACHINE_SLOT_COUNT),
                new SimpleContainerData(MachineData.COUNT));
    }

    /** Server-side constructor. */
    public SawmillScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData data) {
        super(LogisticsAutomation.MENU.SAWMILL, syncId);
        checkContainerSize(inventory, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, MachineData.COUNT);

        this.inventory = inventory;
        this.data = data;

        inventory.startOpen(playerInventory.player);

        // Positions matched to textures/gui/automation/sawmill.png.
        // Input (0)
        this.addSlot(new Slot(inventory, SawmillBlockEntity.INPUT_SLOT, 56, 35));
        // Primary output (1, upper right) and secondary Wood Pulp (2, lower right) — extraction only.
        this.addSlot(new OutputSlot(inventory, SawmillBlockEntity.PRIMARY_OUTPUT_SLOT, 116, 25));
        this.addSlot(new OutputSlot(inventory, SawmillBlockEntity.SECONDARY_OUTPUT_SLOT, 116, 51));

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

            if (index < PLAYER_INVENTORY_START) {
                // Machine slot -> player inventory
                if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, SawmillBlockEntity.INPUT_SLOT, SawmillBlockEntity.INPUT_SLOT + 1, false)) {
                // Player inventory -> input slot
                return ItemStack.EMPTY;
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

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.inventory.stopOpen(player);
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
