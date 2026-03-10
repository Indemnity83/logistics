package com.logistics.pipe.ui;

import com.logistics.LogisticsPipe;
import com.logistics.pipe.ChassisPipe;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Screen handler for the Chassis Logistics Pipe GUI.
 * Dynamically creates 1, 2, 3, 4, or 8 ghost module slots depending on the chassis mark.
 * Slot positions are centered in a row (or two rows for 8 slots).
 */
public class ChassisScreenHandler extends AbstractContainerMenu {
    private static final int SLOT_SIZE = 18;
    private static final int PLAYER_INV_START_X = 8;
    private static final int PLAYER_INV_START_Y = 102;
    private static final int HOTBAR_Y = 160;

    private final int slotCount;
    private final ChassisInventory chassisInventory;
    private final ContainerLevelAccess context;

    /** Client-side constructor — called by the MenuType factory. */
    public ChassisScreenHandler(int syncId, Container playerInventory, int slotCount) {
        this(syncId, playerInventory, slotCount, null);
    }

    /** Server-side constructor — called via SimpleMenuProvider. */
    public ChassisScreenHandler(
            int syncId, Container playerInventory, int slotCount, PipeBlockEntity pipeEntity) {
        super(menuTypeFor(slotCount), syncId);
        this.slotCount = slotCount;
        this.chassisInventory = new ChassisInventory(pipeEntity);
        this.context = pipeEntity != null
                ? ContainerLevelAccess.create(pipeEntity.getLevel(), pipeEntity.getBlockPos())
                : ContainerLevelAccess.NULL;

        addChassisSlots();
        addPlayerInventorySlots(playerInventory);
    }

    /** Returns the MenuType registered for the given active slot count. */
    public static MenuType<ChassisScreenHandler> menuTypeFor(int slotCount) {
        return LogisticsPipe.SCREEN.chassisMenuTypeFor(slotCount);
    }

    /** Returns the number of active module slots for this chassis. */
    public int getSlotCount() {
        return slotCount;
    }

    // Slot positions: two columns of 4, left column at x=26, right column at x=106, y spacing of 18px
    private static final int[] SLOT_X = {26, 26, 26, 26, 106, 106, 106, 106};
    private static final int[] SLOT_Y = {18, 36, 54, 72,  18,  36,  54,  72};

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
        return true;
    }

    @Override
    public void clicked(int slotIndex, int button, ClickType actionType, Player player) {
        if (slotIndex >= 0 && slotIndex < slotCount) {
            // Ghost slot interaction for chassis module slots
            if (actionType == ClickType.QUICK_MOVE) {
                return;
            }

            if (button == 1) {
                // Right-click: clear slot
                chassisInventory.setItem(slotIndex, ItemStack.EMPTY);
                context.execute((world, pos) -> {
                    if (world.getBlockEntity(pos) instanceof PipeBlockEntity pe) {
                        ChassisPipe.setSlotItem(pe.createContext(), slotIndex, "");
                    }
                });
                broadcastChanges();
                return;
            }

            ItemStack cursor = getCarried();
            if (cursor.isEmpty()) {
                return;
            }

            // Left-click with item: set module slot
            String itemId = BuiltInRegistries.ITEM.getKey(cursor.getItem()).toString();
            chassisInventory.setItem(slotIndex, cursor.copyWithCount(1));
            context.execute((world, pos) -> {
                if (world.getBlockEntity(pos) instanceof PipeBlockEntity pe) {
                    ChassisPipe.setSlotItem(pe.createContext(), slotIndex, itemId);
                }
            });
            broadcastChanges();
            return;
        }

        super.clicked(slotIndex, button, actionType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    private static class ChassisSlot extends Slot {
        ChassisSlot(Container inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }
}
