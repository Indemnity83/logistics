package com.logistics.automation.transposer;

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
 * Screen handler for the Transposer GUI: an input slot and an output slot, the player inventory, and
 * progress, energy, and the tank contents synced for rendering.
 */
public class TransposerScreenHandler extends AbstractContainerMenu {

    private static final int MACHINE_SLOT_COUNT = 2;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 36;
    // Must match TransposerScreen.GAUGE_WIDTH (client-only, so duplicated here rather than shared).
    private static final int GAUGE_WIDTH = 23;

    private final Container inventory;
    private final ContainerData data;

    /** Client-side constructor. */
    public TransposerScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(MACHINE_SLOT_COUNT),
                new SimpleContainerData(TransposerBlockEntity.DATA_COUNT));
    }

    /** Server-side constructor. */
    public TransposerScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData data) {
        super(LogisticsAutomation.MENU.TRANSPOSER, syncId);
        checkContainerSize(inventory, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, TransposerBlockEntity.DATA_COUNT);

        this.inventory = inventory;
        this.data = data;

        inventory.startOpen(playerInventory.player);

        // Input slot (0, standard 18x18) and output slot (1, centered in the larger 26x26 frame the
        // texture draws below it: box top-left (52,45) + (26-18)/2 = (56,49)), each nudged 1px right/down
        // from the frame's outer border to center the 16x16 item icon inside it.
        this.addSlot(new Slot(inventory, TransposerBlockEntity.INPUT_SLOT, 57, 24));
        this.addSlot(new Slot(inventory, TransposerBlockEntity.OUTPUT_SLOT, 57, 50));

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
                // Move from player inventory into the input slot only.
                if (!this.moveItemStackTo(stack, TransposerBlockEntity.INPUT_SLOT, TransposerBlockEntity.INPUT_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Move from the machine slots to the player inventory.
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

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.inventory.stopOpen(player);
    }

    // ==================== Data getters for GUI rendering ====================

    /** Progress gauge fill width (0..{@link #GAUGE_WIDTH} px) from the synced progress fraction, or 0 if idle. */
    public int getProgressFillWidth() {
        return MachineData.barPixels(data, MachineData.PROGRESS, GAUGE_WIDTH);
    }

    /** Energy bar height (0..30 px) from the synced energy fill fraction. */
    public int getEnergyBarHeight() {
        return MachineData.barPixels(data, MachineData.ENERGY, 30);
    }

    /** Whether the active recipe drains the tank (Fill mode) — the GUI mirrors its gauge when true. */
    public boolean isFillMode() {
        return data.get(TransposerBlockEntity.DATA_FILL_MODE) != 0;
    }

    /** Registry id of the fluid in the tank, or {@code -1} when empty. */
    public int getTankFluidId() {
        return data.get(TransposerBlockEntity.DATA_FLUID_ID);
    }

    /** Tank fill as a 0..1 fraction of the tank's capacity. */
    public float getTankFillFraction() {
        int amount = data.get(TransposerBlockEntity.DATA_FLUID_AMOUNT);
        int capacity = data.get(TransposerBlockEntity.DATA_FLUID_CAPACITY);
        if (amount <= 0 || capacity <= 0) {
            return 0f;
        }
        return Math.min(1f, amount / (float) capacity);
    }

    /** Current tank amount in mB. */
    public int getTankAmountMb() {
        return data.get(TransposerBlockEntity.DATA_FLUID_AMOUNT);
    }
}
