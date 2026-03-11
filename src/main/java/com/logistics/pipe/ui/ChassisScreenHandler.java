package com.logistics.pipe.ui;

import com.logistics.LogisticsPipe;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.item.ModuleItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Screen handler for the Chassis Logistics Pipe GUI.
 * Dynamically creates 1, 2, 3, 4, or 8 module slots depending on the chassis mark.
 * Each slot holds exactly one module item (real inventory, not ghost).
 */
public class ChassisScreenHandler extends AbstractContainerMenu {
    private static final int SLOT_SIZE = 18;
    private static final int PLAYER_INV_START_X = 8;
    private static final int PLAYER_INV_START_Y = 102;
    private static final int HOTBAR_Y = 160;

    // Two columns of 4 slots: left at x=26, right at x=106, 18px apart vertically
    private static final int[] SLOT_X = {26, 26, 26, 26, 106, 106, 106, 106};
    private static final int[] SLOT_Y = {18, 36, 54, 72,  18,  36,  54,  72};

    private final int slotCount;
    private final ChassisInventory chassisInventory;
    @Nullable private final PipeBlockEntity pipeEntity;

    /** Client-side constructor — called by the MenuType factory. */
    public ChassisScreenHandler(int syncId, Container playerInventory, int slotCount) {
        this(syncId, playerInventory, slotCount, null);
    }

    /** Server-side constructor — called via SimpleMenuProvider. */
    public ChassisScreenHandler(
            int syncId, Container playerInventory, int slotCount, PipeBlockEntity pipeEntity) {
        super(menuTypeFor(slotCount), syncId);
        if (slotCount != 1 && slotCount != 2 && slotCount != 3 && slotCount != 4 && slotCount != 8)
            throw new IllegalArgumentException("Invalid chassis slot count: " + slotCount);
        this.slotCount = slotCount;
        this.pipeEntity = pipeEntity;
        this.chassisInventory = new ChassisInventory(pipeEntity);

        addChassisSlots();
        addPlayerInventorySlots(playerInventory);
    }

    /** Returns the backing pipe entity; non-null only on the server side. */
    @Nullable
    public PipeBlockEntity getPipeEntity() {
        return pipeEntity;
    }

    /** Returns the MenuType registered for the given active slot count. */
    public static MenuType<ChassisScreenHandler> menuTypeFor(int slotCount) {
        return LogisticsPipe.SCREEN.chassisMenuTypeFor(slotCount);
    }

    /** Returns the number of active module slots for this chassis. */
    public int getSlotCount() {
        return slotCount;
    }

    private void addChassisSlots() {
        for (int i = 0; i < slotCount; i++) {
            addSlot(new ChassisSlot(chassisInventory, i, SLOT_X[i], SLOT_Y[i]));
        }
    }

    private void addPlayerInventorySlots(Container playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(
                        playerInventory,
                        col + row * 9 + 9,
                        PLAYER_INV_START_X + col * SLOT_SIZE,
                        PLAYER_INV_START_Y + row * SLOT_SIZE));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, PLAYER_INV_START_X + col * SLOT_SIZE, HOTBAR_Y));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return chassisInventory.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < slotCount) {
            // Chassis slot → player inventory
            if (!moveItemStackTo(stack, slotCount, slotCount + 36, true)) return ItemStack.EMPTY;
        } else {
            // Player inventory → chassis slot (only ModuleItems)
            if (!(stack.getItem() instanceof ModuleItem)) return ItemStack.EMPTY;
            if (!moveItemStackTo(stack, 0, slotCount, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    /** Module slots: only accept ModuleItem, limit to one per slot. */
    private static class ChassisSlot extends Slot {
        ChassisSlot(Container inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof ModuleItem;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
