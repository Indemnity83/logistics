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
 * Screen handler for the Crucible GUI. One input slot (the output is a fluid tank, not an item
 * slot) plus the player inventory, and progress/energy data synced for rendering.
 */
public class CrucibleScreenHandler extends AbstractContainerMenu {

    private static final int MACHINE_SLOT_COUNT = 1;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 36;

    private final Container inventory;
    private final ContainerData data;

    /** Client-side constructor. */
    public CrucibleScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(MACHINE_SLOT_COUNT),
                new SimpleContainerData(CrucibleBlockEntity.DATA_COUNT));
    }

    /** Server-side constructor. */
    public CrucibleScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData data) {
        super(LogisticsAutomation.MENU.CRUCIBLE, syncId);
        checkContainerSize(inventory, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, CrucibleBlockEntity.DATA_COUNT);

        this.inventory = inventory;
        this.data = data;

        inventory.startOpen(playerInventory.player);

        // Input slot (0)
        this.addSlot(new Slot(inventory, CrucibleBlockEntity.INPUT_SLOT, 56, 35));

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
                if (!this.moveItemStackTo(stack, CrucibleBlockEntity.INPUT_SLOT, CrucibleBlockEntity.INPUT_SLOT + 1, false)) {
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

    /** Progress arrow width (0..24 px) from the synced 0..{@code DATA_SCALE} progress fraction. */
    public int getProgressArrowWidth() {
        return Math.min(24, 24 * data.get(CrucibleBlockEntity.DATA_PROGRESS) / CrucibleBlockEntity.DATA_SCALE);
    }

    /** Energy bar height (0..30 px) from the synced 0..{@code DATA_SCALE} energy fill fraction. */
    public int getEnergyBarHeight() {
        return Math.min(30, 30 * data.get(CrucibleBlockEntity.DATA_ENERGY) / CrucibleBlockEntity.DATA_SCALE);
    }

    /** Registry id of the fluid in the tank, or {@code -1} when empty. */
    public int getTankFluidId() {
        return data.get(CrucibleBlockEntity.DATA_FLUID_ID);
    }

    /** Tank fill as a 0..1 fraction of the tank's capacity. */
    public float getTankFillFraction() {
        int amount = data.get(CrucibleBlockEntity.DATA_FLUID_AMOUNT);
        long capacity = CrucibleBlockEntity.TANK_CAPACITY_MB;
        if (amount <= 0 || capacity <= 0) {
            return 0f;
        }
        return Math.min(1f, amount / (float) capacity);
    }

    /** Current tank amount in mB. */
    public int getTankAmountMb() {
        return data.get(CrucibleBlockEntity.DATA_FLUID_AMOUNT);
    }

    /** Tank capacity in mB. */
    public int getTankCapacityMb() {
        return (int) CrucibleBlockEntity.TANK_CAPACITY_MB;
    }
}
