package com.logistics.pipe.ui;

import com.logistics.LogisticsPipe;
import com.logistics.pipe.block.PipeBlock;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.SinkModule;
import com.logistics.pipe.Pipe;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
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
    private boolean defaultRoute = false;

    public SinkScreenHandler(int syncId, Container playerInventory) {
        super(LogisticsPipe.SCREEN.SINK, syncId);
        this.context = ContainerLevelAccess.NULL;
        this.sinkInventory = new SinkInventory(null);

        addFilterSlots(sinkInventory);
        addPlayerInventorySlots(playerInventory);
    }

    public SinkScreenHandler(int syncId, Container playerInventory, PipeBlockEntity pipeEntity) {
        super(LogisticsPipe.SCREEN.SINK, syncId);
        if (pipeEntity != null) {
            this.context = ContainerLevelAccess.create(pipeEntity.getLevel(), pipeEntity.getBlockPos());

            // Load default route setting
            PipeBlock block = (PipeBlock) pipeEntity.getBlockState().getBlock();
            Pipe pipe = block.getPipe();
            SinkModule module = pipe.getModule(SinkModule.class);
            if (module != null) {
                this.defaultRoute = module.isDefaultRoute(pipeEntity.createContext());
            }
        } else {
            this.context = ContainerLevelAccess.NULL;
        }
        this.sinkInventory = new SinkInventory(pipeEntity);

        addFilterSlots(sinkInventory);
        addPlayerInventorySlots(playerInventory);
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
                context.execute((world, pos) -> {
                    if (world.getBlockEntity(pos) instanceof PipeBlockEntity pipeEntity) {
                        PipeBlock block = (PipeBlock) pipeEntity.getBlockState().getBlock();
                        Pipe pipe = block.getPipe();
                        SinkModule module = pipe.getModule(SinkModule.class);

                        if (module != null) {
                            module.setFilter(pipeEntity.createContext(), slotIndex, "");
                        }
                    }
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
            context.execute((world, pos) -> {
                if (world.getBlockEntity(pos) instanceof PipeBlockEntity pipeEntity) {
                    PipeBlock block = (PipeBlock) pipeEntity.getBlockState().getBlock();
                    Pipe pipe = block.getPipe();
                    SinkModule module = pipe.getModule(SinkModule.class);

                    if (module != null) {
                        module.setFilter(pipeEntity.createContext(), slotIndex, itemId);
                    }
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

    public boolean isDefaultRoute() {
        return defaultRoute;
    }

    public void setDefaultRoute(boolean enabled) {
        this.defaultRoute = enabled;

        context.execute((world, pos) -> {
            if (world.getBlockEntity(pos) instanceof PipeBlockEntity pipeEntity) {
                PipeBlock block = (PipeBlock) pipeEntity.getBlockState().getBlock();
                Pipe pipe = block.getPipe();
                SinkModule module = pipe.getModule(SinkModule.class);

                if (module != null) {
                    module.setDefaultRoute(pipeEntity.createContext(), enabled);
                }
            }
        });
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
