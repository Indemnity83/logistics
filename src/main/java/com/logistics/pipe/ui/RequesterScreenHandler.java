package com.logistics.pipe.ui;

import com.logistics.LogisticsPipe;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.RequesterModule;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Screen handler for the Requester GUI.
 * Displays 9 request slots where players can configure items and amounts to request from the network.
 */
public class RequesterScreenHandler extends AbstractContainerMenu {
    private static final int REQUEST_SLOT_COUNT = RequesterModule.MAX_REQUEST_SLOTS;
    private static final int SLOT_SIZE = 18;
    private static final int PLAYER_INV_START_Y = 84;
    private static final int HOTBAR_Y = 142;
    private static final int SLOT_START_X = 8;
    private static final int SLOT_START_Y = 18;

    private final RequestInventory requestInventory;
    private final ContainerLevelAccess context;

    public RequesterScreenHandler(int syncId, Container playerInventory) {
        super(LogisticsPipe.SCREEN.REQUESTER, syncId);
        this.context = ContainerLevelAccess.NULL;
        this.requestInventory = new RequestInventory(null);

        addRequestSlots(requestInventory);
        addPlayerInventorySlots(playerInventory);
    }

    public RequesterScreenHandler(int syncId, Container playerInventory, PipeBlockEntity pipeEntity) {
        super(LogisticsPipe.SCREEN.REQUESTER, syncId);
        if (pipeEntity != null) {
            this.context = ContainerLevelAccess.create(pipeEntity.getLevel(), pipeEntity.getBlockPos());
        } else {
            this.context = ContainerLevelAccess.NULL;
        }
        this.requestInventory = new RequestInventory(pipeEntity);

        addRequestSlots(requestInventory);
        addPlayerInventorySlots(playerInventory);
    }

    private void addRequestSlots(Container inventory) {
        // 3x3 grid of request slots
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int x = SLOT_START_X + col * SLOT_SIZE;
                int y = SLOT_START_Y + row * SLOT_SIZE;
                int slotIndex = col + row * 3;
                addSlot(new RequestSlot(inventory, slotIndex, x, y));
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
    public void clicked(int slotIndex, int button, ClickType actionType, Player player) {
        if (slotIndex >= 0 && slotIndex < REQUEST_SLOT_COUNT) {
            // Prevent shift-click and other complex interactions
            if (actionType != ClickType.PICKUP) {
                return;
            }

            // Get the item in the clicked slot
            ItemStack clickedItem = requestInventory.getItem(slotIndex);
            if (clickedItem.isEmpty()) {
                return;
            }

            // Request this item from the network
            context.execute((world, pos) -> {
                if (world.getBlockEntity(pos) instanceof com.logistics.pipe.block.entity.PipeBlockEntity pipeEntity) {
                    com.logistics.pipe.block.PipeBlock block =
                            (com.logistics.pipe.block.PipeBlock) pipeEntity.getBlockState().getBlock();
                    com.logistics.pipe.Pipe pipe = block.getPipe();
                    com.logistics.pipe.modules.RequesterModule module = pipe.getModule(com.logistics.pipe.modules.RequesterModule.class);

                    if (module != null) {
                        com.logistics.pipe.PipeContext ctx = pipeEntity.createContext();
                        // Request 1 of the clicked item (or full stack if shift-clicked)
                        int amount = button == 1 ? 1 : Math.min(clickedItem.getCount(), 64); // Right-click = 1, left-click = shown amount
                        module.requestItem(ctx, clickedItem, amount);
                    }
                }
            });

            return;
        }

        super.clicked(slotIndex, button, actionType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    private static class RequestSlot extends Slot {
        RequestSlot(Container inventory, int index, int x, int y) {
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
