package com.logistics.pipe.ui;

import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.ItemFilterModule;
import com.logistics.pipe.registry.PipeScreenHandlers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public class ItemFilterScreenHandler extends AbstractContainerMenu {
    private static final int FILTER_SLOT_COUNT =
            ItemFilterModule.FILTER_ORDER.length * ItemFilterModule.FILTER_SLOTS_PER_SIDE;
    private static final int SLOT_SIZE = 18;
    private static final int PLAYER_INV_START_Y = 140;
    private static final int HOTBAR_Y = 198;
    private static final int SLOT_START_X = 8;
    private static final int SLOT_START_Y = 18;
    private static final int FILTER_SLOT_START_X = SLOT_START_X + SLOT_SIZE;

    private final FilterInventory filterInventory;
    private final ContainerLevelAccess context;

    public ItemFilterScreenHandler(int syncId, Container playerInventory) {
        super(PipeScreenHandlers.ITEM_FILTER, syncId);
        this.context = ContainerLevelAccess.NULL;
        this.filterInventory = new FilterInventory(null);

        addFilterSlots(filterInventory);
        addPlayerInventorySlots(playerInventory);
    }

    public ItemFilterScreenHandler(int syncId, Container playerInventory, PipeBlockEntity pipeEntity) {
        super(PipeScreenHandlers.ITEM_FILTER, syncId);
        if (pipeEntity != null) {
            this.context = ContainerLevelAccess.create(pipeEntity.getLevel(), pipeEntity.getBlockPos());
        } else {
            this.context = ContainerLevelAccess.NULL;
        }
        this.filterInventory = new FilterInventory(pipeEntity);

        addFilterSlots(filterInventory);
        addPlayerInventorySlots(playerInventory);
    }

    private void addFilterSlots(Container inventory) {
        int slotIndex = 0;
        for (int row = 0; row < ItemFilterModule.FILTER_ORDER.length; row++) {
            for (int col = 0; col < ItemFilterModule.FILTER_SLOTS_PER_SIDE; col++) {
                int x = FILTER_SLOT_START_X + col * SLOT_SIZE;
                int y = SLOT_START_Y + row * SLOT_SIZE;
                addSlot(new FilterSlot(inventory, slotIndex++, x, y));
            }
        }
    }

    private void addPlayerInventorySlots(Container playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(
                        playerInventory,
                        col + row * 9 + 9,
                        SLOT_START_X + col * SLOT_SIZE,
                        PLAYER_INV_START_Y + row * SLOT_SIZE));
            }
        }

        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, SLOT_START_X + col * SLOT_SIZE, HOTBAR_Y));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput actionType, Player player) {
        if (slotIndex >= 0 && slotIndex < FILTER_SLOT_COUNT) {
            if (actionType == ContainerInput.QUICK_MOVE) {
                return;
            }

            ItemStack cursor = getCarried();
            if (cursor.isEmpty()) {
                filterInventory.setItem(slotIndex, ItemStack.EMPTY);
            } else {
                filterInventory.setItem(slotIndex, cursor.copyWithCount(1));
            }
            broadcastChanges();
            return;
        }

        super.clicked(slotIndex, button, actionType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    private static class FilterSlot extends Slot {
        FilterSlot(Container inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player playerEntity) {
            return false;
        }
    }
}
