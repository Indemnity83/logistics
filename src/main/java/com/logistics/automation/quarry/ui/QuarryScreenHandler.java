package com.logistics.automation.quarry.ui;

import com.logistics.automation.quarry.entity.QuarryBlockEntity;
import com.logistics.automation.registry.AutomationScreenHandlers;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class QuarryScreenHandler extends AbstractContainerMenu {
    private static final int SLOT_SIZE = 18;
    private static final int PLAYER_INV_START_Y = 84;
    private static final int HOTBAR_Y = 142;
    private static final int SLOT_START_X = 8;
    private static final int INVENTORY_SIZE = 9;

    // Tool slots position (3x3 grid centered)
    private static final int TOOL_SLOTS_START_X = 62;
    private static final int TOOL_SLOTS_START_Y = 17;

    private final Container inventory;
    private final ContainerLevelAccess context;

    // Client constructor (pos comes from ExtendedScreenHandlerType packet codec)
    @SuppressWarnings("unused")
    public QuarryScreenHandler(int syncId, Inventory playerInventory, BlockPos unusedPos) {
        this(syncId, playerInventory, new SimpleContainer(INVENTORY_SIZE), ContainerLevelAccess.NULL);
    }

    // Server constructor
    public QuarryScreenHandler(int syncId, Inventory playerInventory, QuarryBlockEntity quarryEntity) {
        this(
                syncId,
                playerInventory,
                quarryEntity,
                ContainerLevelAccess.create(quarryEntity.getLevel(), quarryEntity.getBlockPos()));
    }

    private QuarryScreenHandler(
            int syncId, Inventory playerInventory, Container inventory, ContainerLevelAccess context) {
        super(AutomationScreenHandlers.QUARRY, syncId);
        this.inventory = inventory;
        this.context = context;

        checkContainerSize(inventory, INVENTORY_SIZE);
        inventory.startOpen(playerInventory.player);

        // Tool slots (3x3 grid)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotIndex = col + row * 3;
                addSlot(new ToolSlot(
                        inventory,
                        slotIndex,
                        TOOL_SLOTS_START_X + col * SLOT_SIZE,
                        TOOL_SLOTS_START_Y + row * SLOT_SIZE));
            }
        }

        // Player inventory
        addPlayerInventorySlots(playerInventory);
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
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
    public boolean stillValid(Player player) {
        return inventory.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);

        if (slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();

            // Slots 0-8: tool slots, 9-35: player main inventory, 36-44: hotbar
            if (slotIndex < INVENTORY_SIZE) {
                // Moving from tool slot to player inventory
                if (!moveItemStackTo(originalStack, INVENTORY_SIZE, INVENTORY_SIZE + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory to tool slots
                if (isValidTool(originalStack)) {
                    if (!moveItemStackTo(originalStack, 0, INVENTORY_SIZE, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (slotIndex < INVENTORY_SIZE + 27) {
                    // Move from main inventory to hotbar
                    if (!moveItemStackTo(originalStack, INVENTORY_SIZE + 27, INVENTORY_SIZE + 36, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    // Move from hotbar to main inventory
                    if (!moveItemStackTo(originalStack, INVENTORY_SIZE, INVENTORY_SIZE + 27, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (originalStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (originalStack.getCount() == newStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, originalStack);
        }

        return newStack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        inventory.stopOpen(player);
    }

    private static boolean isValidTool(ItemStack stack) {
        return stack.is(ItemTags.PICKAXES)
                || stack.is(ItemTags.SHOVELS)
                || stack.is(ItemTags.AXES)
                || stack.is(ItemTags.HOES);
    }

    private static class ToolSlot extends Slot {
        ToolSlot(Container inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isValidTool(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
