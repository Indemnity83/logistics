package com.logistics.pipe.ui;

import com.logistics.LogisticsPipe;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.ProviderModule;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Screen handler for the Provider GUI.
 * Displays mode selection button to control which items are available for extraction.
 */
public class ProviderScreenHandler extends AbstractContainerMenu {
    private static final int FILTER_SLOT_COUNT = 9;
    private static final int SLOT_SIZE = 18;
    private static final int FILTER_START_Y = 18;
    private static final int PLAYER_INV_START_Y = 60;
    private static final int HOTBAR_Y = 118;
    private static final int SLOT_START_X = 8;

    private final ProviderFilterInventory filterInventory;
    private final ContainerLevelAccess context;
    private final ContainerData data;

    public ProviderScreenHandler(int syncId, Container playerInventory) {
        this(syncId, playerInventory, null, new SimpleContainerData(2));
    }

    public ProviderScreenHandler(int syncId, Container playerInventory, PipeBlockEntity pipeEntity) {
        this(syncId, playerInventory, pipeEntity, new SimpleContainerData(2));
    }

    private ProviderScreenHandler(int syncId, Container playerInventory, PipeBlockEntity pipeEntity, ContainerData data) {
        super(LogisticsPipe.SCREEN.PROVIDER, syncId);
        this.data = data;
        this.filterInventory = new ProviderFilterInventory(pipeEntity);

        if (pipeEntity != null) {
            this.context = ContainerLevelAccess.create(pipeEntity.getLevel(), pipeEntity.getBlockPos());
            // Load current mode and filter inversion from module
            PipeModuleHelper.withModule(this.context, ProviderModule.class, (ctx, module) -> {
                data.set(0, module.getModeOrdinal(ctx));
                data.set(1, module.isFilterInverted(ctx) ? 1 : 0);
            });
            if (data.get(0) == 0 && data.get(1) == 0) {
                // Set defaults if module not found
                data.set(0, ProviderModule.ProviderMode.SUPPLY.ordinal());
            }
        } else {
            this.context = ContainerLevelAccess.NULL;
        }

        addFilterSlots(filterInventory);
        addPlayerInventorySlots(playerInventory);
        addDataSlots(data);
    }

    private void addFilterSlots(Container inventory) {
        // Single row of 9 filter slots
        for (int col = 0; col < 9; col++) {
            int x = SLOT_START_X + col * SLOT_SIZE;
            int y = FILTER_START_Y;
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
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void clicked(int slotIndex, int button, ClickType actionType, Player player) {
        if (slotIndex >= 0 && slotIndex < FILTER_SLOT_COUNT) {
            // Prevent shift-click
            if (actionType == ClickType.QUICK_MOVE) {
                return;
            }

            ItemStack cursor = getCarried();

            // Right-click or left-click with empty cursor: Clear slot
            if (button == 1 || cursor.isEmpty()) {
                filterInventory.setItem(slotIndex, ItemStack.EMPTY);
                PipeModuleHelper.withModule(context, ProviderModule.class, (ctx, module) -> {
                    module.setFilterItem(ctx, slotIndex, "");
                });
                broadcastChanges();
                return;
            }

            // Left-click with item: Place ghost item
            String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(cursor.getItem()).toString();
            ItemStack ghostItem = cursor.copyWithCount(1);
            filterInventory.setItem(slotIndex, ghostItem);

            // Save to module configuration
            PipeModuleHelper.withModule(context, ProviderModule.class, (ctx, module) -> {
                module.setFilterItem(ctx, slotIndex, itemId);
            });

            broadcastChanges();
            return;
        }

        super.clicked(slotIndex, button, actionType, player);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            // Cycle through modes: Supply(0) -> Reserve(1) -> Guarded(2) -> Seeded(3) -> Sample(4) -> wrap to Supply
            int currentMode = data.get(0);
            int nextMode = (currentMode + 1) % ProviderModule.ProviderMode.values().length;
            data.set(0, nextMode);

            // Save mode to module
            PipeModuleHelper.withModule(context, ProviderModule.class, (ctx, module) -> {
                module.setModeFromOrdinal(ctx, nextMode);
            });
            return true;
        } else if (id == 1) {
            // Toggle filter inversion
            int currentInverted = data.get(1);
            int newInverted = currentInverted == 0 ? 1 : 0;
            data.set(1, newInverted);

            // Save to module
            PipeModuleHelper.withModule(context, ProviderModule.class, (ctx, module) -> {
                module.setFilterInverted(ctx, newInverted == 1);
            });
            return true;
        }
        return false;
    }

    public int getMode() {
        return data.get(0);
    }

    public boolean isFilterInverted() {
        return data.get(1) == 1;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        // Sync mode and filter inversion from module to data slots
        PipeModuleHelper.withModule(context, ProviderModule.class, (ctx, module) -> {
            data.set(0, module.getModeOrdinal(ctx));
            data.set(1, module.isFilterInverted(ctx) ? 1 : 0);
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
