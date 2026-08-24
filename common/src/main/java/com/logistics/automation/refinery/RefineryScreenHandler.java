package com.logistics.automation.refinery;

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
 * Screen handler for the Refinery GUI. One extract-only byproduct slot (both fluids live in tanks, not
 * item slots) plus the player inventory, with progress, energy, and both tanks synced for rendering.
 */
public class RefineryScreenHandler extends AbstractContainerMenu {

    private static final int MACHINE_SLOT_COUNT = 1;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 36;
    private static final int PLAYER_MAIN_END = PLAYER_INVENTORY_START + 27;

    private final Container inventory;
    private final ContainerData data;

    /** Client-side constructor. */
    public RefineryScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(MACHINE_SLOT_COUNT),
                new SimpleContainerData(RefineryBlockEntity.DATA_COUNT));
    }

    /** Server-side constructor. */
    public RefineryScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData data) {
        super(LogisticsAutomation.MENU.REFINERY, syncId);
        checkContainerSize(inventory, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, RefineryBlockEntity.DATA_COUNT);

        this.inventory = inventory;
        this.data = data;

        inventory.startOpen(playerInventory.player);

        // Byproduct output slot (0) — extract-only.
        this.addSlot(new Slot(inventory, RefineryBlockEntity.OUTPUT_SLOT, 123, 60) {
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

            if (index < PLAYER_INVENTORY_START) {
                // Move the byproduct out to the player inventory.
                if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < PLAYER_MAIN_END) {
                // Nothing goes into the machine from the player side; shuffle main inventory <-> hotbar
                // (never the slot's own range, or moveItemStackTo merges the live stack with itself and dupes it).
                if (!this.moveItemStackTo(stack, PLAYER_MAIN_END, PLAYER_INVENTORY_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_MAIN_END, false)) {
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

    // ==================== Data getters for GUI rendering ====================

    /** Progress arrow width (0..22 px) from the synced progress fraction. */
    public int getProgressArrowWidth() {
        return MachineData.barPixels(data, MachineData.PROGRESS, 22);
    }

    /** Energy bar height (0..30 px) from the synced energy fill fraction. */
    public int getEnergyBarHeight() {
        return MachineData.barPixels(data, MachineData.ENERGY, 30);
    }

    public int getInputFluidId() {
        return data.get(RefineryBlockEntity.DATA_IN_FLUID_ID);
    }

    public int getInputAmountMb() {
        return data.get(RefineryBlockEntity.DATA_IN_FLUID_AMOUNT);
    }

    public int getInputCapacityMb() {
        return data.get(RefineryBlockEntity.DATA_IN_CAPACITY);
    }

    public float getInputFillFraction() {
        return fill(getInputAmountMb(), getInputCapacityMb());
    }

    public int getOutputFluidId() {
        return data.get(RefineryBlockEntity.DATA_OUT_FLUID_ID);
    }

    public int getOutputAmountMb() {
        return data.get(RefineryBlockEntity.DATA_OUT_FLUID_AMOUNT);
    }

    public int getOutputCapacityMb() {
        return data.get(RefineryBlockEntity.DATA_OUT_CAPACITY);
    }

    public float getOutputFillFraction() {
        return fill(getOutputAmountMb(), getOutputCapacityMb());
    }

    private static float fill(int amountMb, int capacityMb) {
        if (amountMb <= 0 || capacityMb <= 0) {
            return 0f;
        }
        return Math.min(1f, amountMb / (float) capacityMb);
    }
}
