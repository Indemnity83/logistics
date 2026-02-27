package com.logistics.pipe.ui;

import com.logistics.LogisticsPipe;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.SupplierModule;
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
 * Screen handler for the Supplier GUI.
 * Displays 9 supply slots where players can configure items and target amounts to maintain in the connected inventory.
 */
public class SupplierScreenHandler extends AbstractContainerMenu {
    private static final int SUPPLY_SLOT_COUNT = SupplierModule.MAX_SUPPLY_SLOTS;
    private static final int SLOT_SIZE = 18;
    private static final int PLAYER_INV_START_Y = 60;
    private static final int HOTBAR_Y = 118;
    private static final int SLOT_START_X = 8;
    private static final int SLOT_START_Y = 18;

    private final SupplyInventory supplyInventory;
    private final ContainerLevelAccess context;
    private final ContainerData data;

    public SupplierScreenHandler(int syncId, Container playerInventory) {
        this(syncId, playerInventory, null, new SimpleContainerData(1));
    }

    public SupplierScreenHandler(int syncId, Container playerInventory, PipeBlockEntity pipeEntity) {
        this(syncId, playerInventory, pipeEntity, new SimpleContainerData(1));
    }

    private SupplierScreenHandler(int syncId, Container playerInventory, PipeBlockEntity pipeEntity, ContainerData data) {
        super(LogisticsPipe.SCREEN.SUPPLIER, syncId);
        this.data = data;

        if (pipeEntity != null) {
            this.context = ContainerLevelAccess.create(pipeEntity.getLevel(), pipeEntity.getBlockPos());
            // Load current mode (placeholder - will be implemented later)
            data.set(0, 3); // Default to Partial mode
        } else {
            this.context = ContainerLevelAccess.NULL;
        }
        this.supplyInventory = new SupplyInventory(pipeEntity);

        addSupplySlots(supplyInventory);
        addPlayerInventorySlots(playerInventory);
        addDataSlots(data);
    }

    private void addSupplySlots(Container inventory) {
        // Single row of 9 supply slots
        for (int col = 0; col < 9; col++) {
            int x = SLOT_START_X + col * SLOT_SIZE;
            int y = SLOT_START_Y;
            addSlot(new SupplySlot(inventory, col, x, y));
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
        if (slotIndex >= 0 && slotIndex < SUPPLY_SLOT_COUNT) {
            // Prevent shift-click
            if (actionType == ClickType.QUICK_MOVE) {
                return;
            }

            ItemStack cursor = getCarried();
            ItemStack slotItem = supplyInventory.getItem(slotIndex);

            // Right-click: Clear slot
            if (button == 1) {
                supplyInventory.setItem(slotIndex, ItemStack.EMPTY);
                context.execute((world, pos) -> {
                    if (world.getBlockEntity(pos) instanceof com.logistics.pipe.block.entity.PipeBlockEntity pipeEntity) {
                        com.logistics.pipe.block.PipeBlock block =
                                (com.logistics.pipe.block.PipeBlock) pipeEntity.getBlockState().getBlock();
                        com.logistics.pipe.Pipe pipe = block.getPipe();
                        com.logistics.pipe.modules.SupplierModule module = pipe.getModule(com.logistics.pipe.modules.SupplierModule.class);

                        if (module != null) {
                            com.logistics.pipe.PipeContext ctx = pipeEntity.createContext();
                            module.setSupplyConfig(ctx, slotIndex, "", 0);
                        }
                    }
                });
                broadcastChanges();
                return;
            }

            // Left-click: Add to count or place new item
            if (cursor.isEmpty()) {
                return; // Nothing to do
            }

            String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(cursor.getItem()).toString();
            int addAmount = cursor.getCount();
            int newAmount;

            if (slotItem.isEmpty()) {
                // Place new item
                newAmount = addAmount;
            } else if (ItemStack.isSameItemSameComponents(cursor, slotItem)) {
                // Same item - add to existing count
                newAmount = Math.min(slotItem.getCount() + addAmount, 999); // Cap at 999
            } else {
                // Different item - replace
                newAmount = addAmount;
            }

            ItemStack ghostItem = cursor.copyWithCount(newAmount);
            supplyInventory.setItem(slotIndex, ghostItem);

            // Save to module configuration
            int finalAmount = newAmount;
            context.execute((world, pos) -> {
                if (world.getBlockEntity(pos) instanceof com.logistics.pipe.block.entity.PipeBlockEntity pipeEntity) {
                    com.logistics.pipe.block.PipeBlock block =
                            (com.logistics.pipe.block.PipeBlock) pipeEntity.getBlockState().getBlock();
                    com.logistics.pipe.Pipe pipe = block.getPipe();
                    com.logistics.pipe.modules.SupplierModule module = pipe.getModule(com.logistics.pipe.modules.SupplierModule.class);

                    if (module != null) {
                        com.logistics.pipe.PipeContext ctx = pipeEntity.createContext();
                        module.setSupplyConfig(ctx, slotIndex, itemId, finalAmount);
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

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            // Cycle through modes: Bulk50(0) -> Bulk100(1) -> Infinite(2) -> Partial(3) -> Full(4) -> wrap to Bulk50
            int currentMode = data.get(0);
            int nextMode = (currentMode + 1) % 5;
            data.set(0, nextMode);

            // TODO: Save mode to module when implemented
            return true;
        }
        return false;
    }

    public int getMode() {
        return data.get(0);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        // TODO: Sync mode from module when implemented
    }

    private static class SupplySlot extends Slot {
        SupplySlot(Container inventory, int index, int x, int y) {
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
