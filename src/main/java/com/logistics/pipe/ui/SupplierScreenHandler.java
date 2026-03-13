package com.logistics.pipe.ui;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.storage.NbtCompat;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.SupplierModule;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

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
    @Nullable private final ServerPlayer itemConfigPlayer;
    @Nullable private final InteractionHand itemConfigHand;
    @Nullable private final ItemStack originalStack;

    public SupplierScreenHandler(int syncId, Container playerInventory) {
        this(syncId, playerInventory, null, new SimpleContainerData(1));
    }

    public SupplierScreenHandler(int syncId, Container playerInventory, PipeBlockEntity pipeEntity) {
        this(syncId, playerInventory, pipeEntity, new SimpleContainerData(1));
    }

    public SupplierScreenHandler(int syncId, Container playerInventory, ServerPlayer player, InteractionHand hand) {
        super(LogisticsPipe.SCREEN.SUPPLIER, syncId);
        this.data = new SimpleContainerData(1);
        this.itemConfigPlayer = player;
        this.itemConfigHand = hand;
        this.context = ContainerLevelAccess.NULL;
        ItemStack stack = player.getItemInHand(hand);
        this.originalStack = stack;
        this.supplyInventory = new SupplyInventory(null);
        this.supplyInventory.loadFromItem(stack);
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            data.set(0, NbtCompat.getInt(customData.copyTag(), SupplierModule.MODE, 0));
        }
        addSupplySlots(supplyInventory);
        addPlayerInventorySlots(playerInventory);
        addDataSlots(data);
    }

    private SupplierScreenHandler(int syncId, Container playerInventory, PipeBlockEntity pipeEntity, ContainerData data) {
        super(LogisticsPipe.SCREEN.SUPPLIER, syncId);
        this.data = data;
        this.itemConfigPlayer = null;
        this.itemConfigHand = null;
        this.originalStack = null;

        if (pipeEntity != null) {
            this.context = ContainerLevelAccess.create(pipeEntity.getLevel(), pipeEntity.getBlockPos());
            data.set(0, SupplierModule.SupplyMode.PARTIAL.ordinal());
            PipeModuleHelper.withModule(this.context, SupplierModule.class, (ctx, module) -> {
                data.set(0, module.getModeOrdinal(ctx));
            });
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

    private boolean isPinnedItemStillHeld() {
        return originalStack != null && itemConfigPlayer != null
                && itemConfigPlayer.getItemInHand(itemConfigHand) == originalStack;
    }

    @Override
    public boolean stillValid(Player player) {
        if (itemConfigPlayer != null) {
            return isPinnedItemStillHeld();
        }
        return true;
    }

    @Override
    public void clicked(int slotIndex, int button, ClickType actionType, Player player) {
        if (slotIndex >= 0 && slotIndex < SUPPLY_SLOT_COUNT) {
            if (actionType == ClickType.QUICK_MOVE) return;
            if (itemConfigPlayer != null && !isPinnedItemStillHeld()) return;

            ItemStack cursor = getCarried();
            ItemStack slotItem = supplyInventory.getItem(slotIndex);

            if (button == 1 || cursor.isEmpty()) {
                clearSupplySlot(slotIndex);
                broadcastChanges();
                return;
            }

            // Left-click with item: Add to count or place new item
            String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(cursor.getItem()).toString();
            int addAmount = cursor.getCount();
            int newAmount;

            if (slotItem.isEmpty()) {
                newAmount = addAmount;
            } else if (ItemStack.isSameItemSameComponents(cursor, slotItem)) {
                newAmount = Math.min(slotItem.getCount() + addAmount, 999); // Cap at 999
            } else {
                newAmount = addAmount;
            }

            supplyInventory.setItem(slotIndex, cursor.copyWithCount(newAmount));
            saveSupplySlot(slotIndex, itemId, newAmount);
            broadcastChanges();
            return;
        }

        super.clicked(slotIndex, button, actionType, player);
    }

    private void clearSupplySlot(int slotIndex) {
        if (itemConfigPlayer != null) {
            supplyInventory.setItem(slotIndex, ItemStack.EMPTY);
            final int s = slotIndex;
            ItemTagUtils.writeToItemTag(itemConfigPlayer, itemConfigHand, tag -> {
                CompoundTag supplies = NbtCompat.getCompoundOrEmpty(tag, SupplierModule.SUPPLIES);
                supplies.remove(String.valueOf(s));
                tag.put(SupplierModule.SUPPLIES, supplies);
            });
        } else {
            supplyInventory.setItem(slotIndex, ItemStack.EMPTY);
            PipeModuleHelper.withModule(context, SupplierModule.class, (ctx, module) ->
                    module.setSupplyConfig(ctx, slotIndex, "", 0));
        }
    }

    private void saveSupplySlot(int slotIndex, String itemId, int amount) {
        if (itemConfigPlayer != null) {
            final int s = slotIndex;
            ItemTagUtils.writeToItemTag(itemConfigPlayer, itemConfigHand, tag -> {
                CompoundTag supplies = NbtCompat.getCompoundOrEmpty(tag, SupplierModule.SUPPLIES);
                CompoundTag slotTag = new CompoundTag();
                slotTag.putString("item", itemId);
                slotTag.putInt("amount", amount);
                supplies.put(String.valueOf(s), slotTag);
                tag.put(SupplierModule.SUPPLIES, supplies);
            });
        } else {
            PipeModuleHelper.withModule(context, SupplierModule.class, (ctx, module) ->
                    module.setSupplyConfig(ctx, slotIndex, itemId, amount));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            int nextMode = (data.get(0) + 1) % SupplierModule.SupplyMode.values().length;
            if (itemConfigPlayer != null) {
                if (!isPinnedItemStillHeld()) return true;
                data.set(0, nextMode);
                ItemTagUtils.writeToItemTag(itemConfigPlayer, itemConfigHand, tag -> tag.putInt(SupplierModule.MODE, nextMode));
            } else {
                data.set(0, nextMode);
                PipeModuleHelper.withModule(context, SupplierModule.class, (ctx, module) -> {
                    module.setModeFromOrdinal(ctx, nextMode);
                });
            }
            return true;
        }
        return false;
    }

    public int getMode() {
        return data.get(0);
    }

    @Override
    public void broadcastChanges() {
        if (itemConfigPlayer != null) {
            if (isPinnedItemStillHeld()) {
                ItemStack stack = itemConfigPlayer.getItemInHand(itemConfigHand);
                CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
                if (customData != null) {
                    data.set(0, NbtCompat.getInt(customData.copyTag(), SupplierModule.MODE, 0));
                } else {
                    data.set(0, 0);
                }
            }
            super.broadcastChanges();
            return;
        }
        PipeModuleHelper.withModule(context, SupplierModule.class, (ctx, module) -> {
            data.set(0, module.getModeOrdinal(ctx));
        });
        super.broadcastChanges();
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
