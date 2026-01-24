package com.logistics.quarry.ui;

import com.logistics.quarry.entity.QuarryBlockEntity;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;

public class QuarryScreenHandler extends ScreenHandler {
    private static final int SLOT_SIZE = 18;
    private static final int PLAYER_INV_START_Y = 84;
    private static final int HOTBAR_Y = 142;
    private static final int SLOT_START_X = 8;

    // Tool slot position (centered)
    private static final int TOOL_SLOT_X = 80;
    private static final int TOOL_SLOT_Y = 35;

    private final Inventory inventory;
    private final ScreenHandlerContext context;

    // Client constructor
    public QuarryScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos pos) {
        this(syncId, playerInventory, new SimpleInventory(1), ScreenHandlerContext.EMPTY);
    }

    // Server constructor
    public QuarryScreenHandler(int syncId, PlayerInventory playerInventory, QuarryBlockEntity quarryEntity) {
        this(syncId, playerInventory, quarryEntity,
                ScreenHandlerContext.create(quarryEntity.getWorld(), quarryEntity.getPos()));
    }

    private QuarryScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, ScreenHandlerContext context) {
        super(QuarryScreenHandlers.QUARRY, syncId);
        this.inventory = inventory;
        this.context = context;

        checkSize(inventory, 1);
        inventory.onOpen(playerInventory.player);

        // Tool slot
        addSlot(new ToolSlot(inventory, 0, TOOL_SLOT_X, TOOL_SLOT_Y));

        // Player inventory
        addPlayerInventorySlots(playerInventory);
    }

    private void addPlayerInventorySlots(PlayerInventory playerInventory) {
        // Main inventory (3 rows of 9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(
                        playerInventory,
                        col + row * 9 + 9,
                        SLOT_START_X + col * SLOT_SIZE,
                        PLAYER_INV_START_Y + row * SLOT_SIZE));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, SLOT_START_X + col * SLOT_SIZE, HOTBAR_Y));
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);

        if (slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            if (slotIndex == 0) {
                // Moving from tool slot to player inventory
                if (!insertItem(originalStack, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory to tool slot
                if (isValidTool(originalStack)) {
                    if (!insertItem(originalStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (slotIndex < 28) {
                    // Move from main inventory to hotbar
                    if (!insertItem(originalStack, 28, 37, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    // Move from hotbar to main inventory
                    if (!insertItem(originalStack, 1, 28, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }

            if (originalStack.getCount() == newStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTakeItem(player, originalStack);
        }

        return newStack;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        inventory.onClose(player);
    }

    private static boolean isValidTool(ItemStack stack) {
        return stack.getMaxDamage() > 0;
    }

    private static class ToolSlot extends Slot {
        ToolSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return isValidTool(stack);
        }

        @Override
        public int getMaxItemCount() {
            return 1;
        }
    }
}
