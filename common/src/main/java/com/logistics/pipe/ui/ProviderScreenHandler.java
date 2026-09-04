package com.logistics.pipe.ui;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.ProviderModule;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

/**
 * Screen handler for the Provider GUI.
 * Displays mode selection button to control which items are available for extraction.
 */
public class ProviderScreenHandler extends CustomSlotScreenHandler {
    private static final int FILTER_SLOT_COUNT = 9;
    private static final int SLOT_SIZE = 18;
    private static final int FILTER_START_Y = 18;
    private static final int PLAYER_INV_START_Y = 60;
    private static final int HOTBAR_Y = 118;
    private static final int SLOT_START_X = 8;

    private final ProviderFilterInventory filterInventory;
    private final ContainerLevelAccess context;
    private final ContainerData data;
    @Nullable private final String targetModuleStateKey;
    @Nullable private final ServerPlayer itemConfigPlayer;
    @Nullable private final InteractionHand itemConfigHand;
    @Nullable private final ItemStack pinnedItemStack;

    public ProviderScreenHandler(int syncId, Container playerInventory) {
        this(syncId, playerInventory, null, null, new SimpleContainerData(2));
    }

    public ProviderScreenHandler(int syncId, Container playerInventory, PipeBlockEntity pipeEntity) {
        this(syncId, playerInventory, pipeEntity, null, new SimpleContainerData(2));
    }

    public ProviderScreenHandler(
            int syncId, Container playerInventory, PipeBlockEntity pipeEntity, @Nullable String targetModuleStateKey) {
        this(syncId, playerInventory, pipeEntity, targetModuleStateKey, new SimpleContainerData(2));
    }

    public ProviderScreenHandler(int syncId, Container playerInventory, ServerPlayer player, InteractionHand hand) {
        super(LogisticsPipe.SCREEN.PROVIDER, syncId);
        this.data = new SimpleContainerData(2);
        this.targetModuleStateKey = null;
        this.itemConfigPlayer = player;
        this.itemConfigHand = hand;
        this.context = ContainerLevelAccess.NULL;
        ItemStack stack = player.getItemInHand(hand);
        this.pinnedItemStack = stack;
        this.filterInventory = new ProviderFilterInventory(null);
        this.filterInventory.loadFromItem(stack);
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            data.set(0, NbtCompat.getInt(tag, ProviderModule.MODE, 0));
            data.set(1, NbtCompat.getInt(tag, ProviderModule.FILTER_INVERTED, 0));
        }
        addFilterSlots(filterInventory);
        addPlayerInventorySlots(playerInventory);
        addDataSlots(data);
    }

    private ProviderScreenHandler(
            int syncId,
            Container playerInventory,
            PipeBlockEntity pipeEntity,
            @Nullable String targetModuleStateKey,
            ContainerData data) {
        super(LogisticsPipe.SCREEN.PROVIDER, syncId);
        this.data = data;
        this.targetModuleStateKey = targetModuleStateKey;
        this.itemConfigPlayer = null;
        this.itemConfigHand = null;
        this.pinnedItemStack = null;
        this.filterInventory = new ProviderFilterInventory(pipeEntity, targetModuleStateKey);

        if (pipeEntity != null) {
            this.context = ContainerLevelAccess.create(pipeEntity.getLevel(), pipeEntity.getBlockPos());
            PipeModuleHelper.withModule(pipeEntity, ProviderModule.class, targetModuleStateKey, (ctx, module) -> {
                data.set(0, module.getModeOrdinal(ctx));
                data.set(1, module.isFilterInverted(ctx) ? 1 : 0);
            });
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

    private boolean isPinnedItemStillHeld() {
        return pinnedItemStack != null && itemConfigPlayer != null
                && itemConfigPlayer.getItemInHand(itemConfigHand) == pinnedItemStack;
    }

    @Override
    public boolean stillValid(Player player) {
        if (itemConfigPlayer != null) {
            return isPinnedItemStillHeld();
        }
        return PipeMenuValidity.stillValid(context, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    protected boolean handleCustomSlotClick(int slotIndex, int button, boolean quickMove, Player player) {
        if (slotIndex >= 0 && slotIndex < FILTER_SLOT_COUNT) {
            if (quickMove) return true;

            ItemStack cursor = getCarried();

            if (button == 1 || cursor.isEmpty()) {
                if (itemConfigPlayer != null) handleClearSlotForPlayer(slotIndex);
                else handleClearSlotForModule(slotIndex);
                broadcastChanges();
                return true;
            }

            if (itemConfigPlayer != null) handlePlaceItemForPlayer(slotIndex, cursor);
            else handlePlaceItemForModule(slotIndex, cursor);
            broadcastChanges();
            return true;
        }

        return false;
    }

    private void handleClearSlotForPlayer(int slotIndex) {
        if (!isPinnedItemStillHeld()) return;
        filterInventory.setItem(slotIndex, ItemStack.EMPTY);
        final int s = slotIndex;
        ItemTagUtils.writeToItemTag(itemConfigPlayer, itemConfigHand, tag -> {
            CompoundTag fi = NbtCompat.getCompoundOrEmpty(tag, ProviderModule.FILTER_ITEMS);
            fi.remove(String.valueOf(s));
            tag.put(ProviderModule.FILTER_ITEMS, fi);
        });
    }

    private void handleClearSlotForModule(int slotIndex) {
        filterInventory.setItem(slotIndex, ItemStack.EMPTY);
        PipeModuleHelper.withModule(context, ProviderModule.class, targetModuleStateKey, (ctx, module) -> module.setFilterItem(ctx, slotIndex, ""));
    }

    private void handlePlaceItemForPlayer(int slotIndex, ItemStack cursor) {
        if (!isPinnedItemStillHeld()) return;
        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(cursor.getItem()).toString();
        filterInventory.setItem(slotIndex, cursor.copyWithCount(1));
        final int s = slotIndex;
        ItemTagUtils.writeToItemTag(itemConfigPlayer, itemConfigHand, tag -> {
            CompoundTag fi = NbtCompat.getCompoundOrEmpty(tag, ProviderModule.FILTER_ITEMS);
            fi.putString(String.valueOf(s), itemId);
            tag.put(ProviderModule.FILTER_ITEMS, fi);
        });
    }

    private void handlePlaceItemForModule(int slotIndex, ItemStack cursor) {
        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(cursor.getItem()).toString();
        filterInventory.setItem(slotIndex, cursor.copyWithCount(1));
        PipeModuleHelper.withModule(context, ProviderModule.class, targetModuleStateKey, (ctx, module) -> module.setFilterItem(ctx, slotIndex, itemId));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            int nextMode = (data.get(0) + 1) % ProviderModule.ProviderMode.values().length;
            data.set(0, nextMode);
            if (itemConfigPlayer != null) {
                if (!isPinnedItemStillHeld()) return true;
                ItemTagUtils.writeToItemTag(itemConfigPlayer, itemConfigHand,tag -> tag.putInt(ProviderModule.MODE, nextMode));
            } else {
                PipeModuleHelper.withModule(context, ProviderModule.class, targetModuleStateKey, (ctx, module) -> module.setModeFromOrdinal(ctx, nextMode));
            }
            return true;
        } else if (id == 1) {
            int newInverted = data.get(1) == 0 ? 1 : 0;
            data.set(1, newInverted);
            if (itemConfigPlayer != null) {
                if (!isPinnedItemStillHeld()) return true;
                ItemTagUtils.writeToItemTag(itemConfigPlayer, itemConfigHand,tag -> tag.putInt(ProviderModule.FILTER_INVERTED, newInverted));
            } else {
                PipeModuleHelper.withModule(context, ProviderModule.class, targetModuleStateKey, (ctx, module) -> module.setFilterInverted(ctx, newInverted == 1));
            }
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
        if (itemConfigPlayer != null) {
            if (isPinnedItemStillHeld()) {
                ItemStack stack = itemConfigPlayer.getItemInHand(itemConfigHand);
                CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
                if (customData != null) {
                    CompoundTag tag = customData.copyTag();
                    data.set(0, NbtCompat.getInt(tag, ProviderModule.MODE, 0));
                    data.set(1, NbtCompat.getInt(tag, ProviderModule.FILTER_INVERTED, 0));
                } else {
                    data.set(0, 0);
                    data.set(1, 0);
                }
            }
            super.broadcastChanges();
            return;
        }
        PipeModuleHelper.withModule(context, ProviderModule.class, targetModuleStateKey, (ctx, module) -> {
            data.set(0, module.getModeOrdinal(ctx));
            data.set(1, module.isFilterInverted(ctx) ? 1 : 0);
        });
        super.broadcastChanges();
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
