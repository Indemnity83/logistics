package com.logistics.pipe.ui;

import com.logistics.LogisticsPipe;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.SinkModule;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Screen handler for the Sink GUI (Basic Logistics Pipe).
 * Displays 9 filter slots and a Default Route toggle.
 */
public class SinkScreenHandler extends AbstractContainerMenu {
    private static final int FILTER_SLOT_COUNT = SinkModule.MAX_FILTER_SLOTS;
    private static final int SLOT_SIZE = 18;
    private static final int PLAYER_INV_START_Y = 60;
    private static final int HOTBAR_Y = 118;
    private static final int SLOT_START_X = 8;
    private static final int SLOT_START_Y = 18;

    private final SinkInventory sinkInventory;
    private final ContainerLevelAccess context;
    private final ContainerData data;

    public SinkScreenHandler(int syncId, Container playerInventory) {
        this(syncId, playerInventory, null, new SimpleContainerData(1));
    }

    public SinkScreenHandler(int syncId, Container playerInventory, PipeBlockEntity pipeEntity) {
        this(syncId, playerInventory, pipeEntity, new SimpleContainerData(1));
    }

    private SinkScreenHandler(int syncId, Container playerInventory, PipeBlockEntity pipeEntity, ContainerData data) {
        super(LogisticsPipe.SCREEN.SINK, syncId);
        this.data = data;

        if (pipeEntity != null) {
            this.context = ContainerLevelAccess.create(pipeEntity.getLevel(), pipeEntity.getBlockPos());

            // Load default route setting (server-side)
            PipeModuleHelper.withModule(this.context, SinkModule.class, (module, ctx) -> {
                data.set(0, module.isDefaultRoute(ctx) ? 1 : 0);
            });
        } else {
            this.context = ContainerLevelAccess.NULL;
        }
        this.sinkInventory = new SinkInventory(pipeEntity);

        addFilterSlots(sinkInventory);
        addPlayerInventorySlots(playerInventory);
        addDataSlots(data);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        // Sync default route value from module to data slot
        PipeModuleHelper.withModule(context, SinkModule.class, (module, ctx) -> {
            data.set(0, module.isDefaultRoute(ctx) ? 1 : 0);
        });
    }

    private void addFilterSlots(Container inventory) {
        // Single row of 9 filter slots
        for (int col = 0; col < 9; col++) {
            int x = SLOT_START_X + col * SLOT_SIZE;
            int y = SLOT_START_Y;
            addSlot(new FilterSlot(inventory, col, x, y));
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
        if (slotIndex >= 0 && slotIndex < FILTER_SLOT_COUNT) {
            // Prevent shift-click
            if (actionType == ClickType.QUICK_MOVE) {
                return;
            }

            ItemStack cursor = getCarried();

            // Right-click: Clear slot
            if (button == 1) {
                sinkInventory.setItem(slotIndex, ItemStack.EMPTY);
                PipeModuleHelper.withModule(context, SinkModule.class, (module, ctx) -> {
                    module.setFilter(ctx, slotIndex, "");
                });
                broadcastChanges();
                return;
            }

            // Left-click: Set filter
            if (cursor.isEmpty()) {
                return; // Nothing to do
            }

            String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(cursor.getItem()).toString();
            ItemStack ghostItem = cursor.copyWithCount(1); // Filters don't need count
            sinkInventory.setItem(slotIndex, ghostItem);

            // Save to module configuration
            PipeModuleHelper.withModule(context, SinkModule.class, (module, ctx) -> {
                module.setFilter(ctx, slotIndex, itemId);
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

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            // Toggle default route (server-side only)
            boolean newValue = data.get(0) == 0;
            data.set(0, newValue ? 1 : 0);

            PipeModuleHelper.withModule(context, SinkModule.class, (module, ctx) -> {
                module.setDefaultRoute(ctx, newValue);
            });
            return true;
        }
        return false;
    }

    public boolean isDefaultRoute() {
        return data.get(0) == 1;
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
