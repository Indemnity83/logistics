package com.logistics.automation.crucible;

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
 * Screen handler for the Magma Crucible GUI. One input slot (the output is a fluid tank, not an item
 * slot) plus the player inventory, and progress/energy data synced for rendering.
 */
public class MagmaCrucibleScreenHandler extends AbstractContainerMenu {

    private static final int MACHINE_SLOT_COUNT = 1;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 36;

    private final Container inventory;
    private final ContainerData data;

    /** Client-side constructor. */
    public MagmaCrucibleScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(MACHINE_SLOT_COUNT),
                new SimpleContainerData(MagmaCrucibleBlockEntity.DATA_COUNT));
    }

    /** Server-side constructor. */
    public MagmaCrucibleScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData data) {
        super(LogisticsAutomation.MENU.MAGMA_CRUCIBLE, syncId);
        checkContainerSize(inventory, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, MagmaCrucibleBlockEntity.DATA_COUNT);

        this.inventory = inventory;
        this.data = data;

        inventory.startOpen(playerInventory.player);

        // Input slot (0)
        this.addSlot(new Slot(inventory, MagmaCrucibleBlockEntity.INPUT_SLOT, 56, 35));

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

            if (index >= PLAYER_INVENTORY_START) {
                // Move from player inventory to the input slot
                if (!this.moveItemStackTo(stack, MagmaCrucibleBlockEntity.INPUT_SLOT, MagmaCrucibleBlockEntity.INPUT_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Move from the machine input to the player inventory
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

    public int getEnergySpent() {
        return data.get(MagmaCrucibleBlockEntity.DATA_PROGRESS);
    }

    public int getEnergyRequired() {
        return data.get(MagmaCrucibleBlockEntity.DATA_TOTAL);
    }

    public int getEnergyStored() {
        return data.get(MagmaCrucibleBlockEntity.DATA_ENERGY);
    }

    public int getEnergyCapacity() {
        return (int) MagmaCrucibleBlockEntity.ENERGY_CAPACITY;
    }

    public int getProgressArrowWidth() {
        int required = getEnergyRequired();
        if (required <= 0) return 0;
        return 24 * getEnergySpent() / required;
    }

    public int getEnergyBarHeight() {
        int capacity = getEnergyCapacity();
        if (capacity <= 0) return 0;
        return 30 * getEnergyStored() / capacity;
    }

    /** Registry id of the fluid in the tank, or {@code -1} when empty. */
    public int getTankFluidId() {
        return data.get(MagmaCrucibleBlockEntity.DATA_FLUID_ID);
    }

    /** Tank fill as a 0..1 fraction of the tank's capacity. */
    public float getTankFillFraction() {
        int amount = data.get(MagmaCrucibleBlockEntity.DATA_FLUID_AMOUNT);
        long capacity = MagmaCrucibleBlockEntity.TANK_CAPACITY_MB;
        if (amount <= 0 || capacity <= 0) return 0f;
        return Math.min(1f, amount / (float) capacity);
    }
}
