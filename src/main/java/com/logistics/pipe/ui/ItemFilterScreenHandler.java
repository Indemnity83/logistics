package com.logistics.pipe.ui;

import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.ItemFilterModule;
import com.logistics.pipe.registry.PipeScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemFilterScreenHandler extends ScreenHandler {
    private static final int FILTER_SLOT_COUNT =
            ItemFilterModule.FILTER_ORDER.length * ItemFilterModule.FILTER_SLOTS_PER_SIDE;
    private static final int SLOT_SIZE = 18;
    private static final int PLAYER_INV_START_Y = 140;
    private static final int HOTBAR_Y = 198;
    private static final int SLOT_START_X = 8;
    private static final int SLOT_START_Y = 18;
    private static final int FILTER_SLOT_START_X = SLOT_START_X + SLOT_SIZE;

    private final FilterInventory filterInventory;
    private final ScreenHandlerContext context;

    public ItemFilterScreenHandler(int syncId, PlayerInventory playerInventory) {
        super(PipeScreenHandlers.ITEM_FILTER, syncId);
        this.context = ScreenHandlerContext.EMPTY;
        this.filterInventory = new FilterInventory(null);

        addFilterSlots(filterInventory);
        addPlayerInventorySlots(playerInventory);
    }

    public ItemFilterScreenHandler(int syncId, PlayerInventory playerInventory, PipeBlockEntity pipeEntity) {
        super(PipeScreenHandlers.ITEM_FILTER, syncId);
        World world = pipeEntity != null ? pipeEntity.getWorld() : playerInventory.player.getEntityWorld();
        BlockPos pos = pipeEntity != null ? pipeEntity.getPos() : playerInventory.player.getBlockPos();
        this.context = ScreenHandlerContext.create(world, pos);
        this.filterInventory = new FilterInventory(pipeEntity);

        addFilterSlots(filterInventory);
        addPlayerInventorySlots(playerInventory);
    }

    private void addFilterSlots(Inventory inventory) {
        int slotIndex = 0;
        for (int row = 0; row < ItemFilterModule.FILTER_ORDER.length; row++) {
            for (int col = 0; col < ItemFilterModule.FILTER_SLOTS_PER_SIDE; col++) {
                int x = FILTER_SLOT_START_X + col * SLOT_SIZE;
                int y = SLOT_START_Y + row * SLOT_SIZE;
                addSlot(new FilterSlot(inventory, slotIndex++, x, y));
            }
        }
    }

    private void addPlayerInventorySlots(PlayerInventory playerInventory) {
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
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex >= 0 && slotIndex < FILTER_SLOT_COUNT) {
            if (actionType == SlotActionType.QUICK_MOVE) {
                return;
            }

            ItemStack cursor = getCursorStack();
            if (cursor.isEmpty()) {
                filterInventory.setStack(slotIndex, ItemStack.EMPTY);
            } else {
                filterInventory.setStack(slotIndex, cursor.copyWithCount(1));
            }
            sendContentUpdates();
            return;
        }

        super.onSlotClick(slotIndex, button, actionType, player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    private static class FilterSlot extends Slot {
        FilterSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false;
        }

        @Override
        public boolean canTakeItems(PlayerEntity playerEntity) {
            return false;
        }
    }
}
