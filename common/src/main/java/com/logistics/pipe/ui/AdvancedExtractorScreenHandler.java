package com.logistics.pipe.ui;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.AdvancedExtractorModule;
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
 * Screen handler for the Advanced Extractor filter GUI.
 * Shows 9 filter ghost slots and an Include/Exclude toggle.
 * No mode selection (unlike the Provider GUI).
 */
public class AdvancedExtractorScreenHandler extends CustomSlotScreenHandler {
    private static final int FILTER_SLOT_COUNT = 9;
    private static final int SLOT_SIZE = 18;
    private static final int FILTER_START_Y = 18;
    private static final int PLAYER_INV_START_Y = 60;
    private static final int HOTBAR_Y = 118;
    private static final int SLOT_START_X = 8;

    // data[0] = filter inverted (0 = include, 1 = exclude)
    private final ContainerData data;
    private final AdvancedExtractorFilterInventory filterInventory;
    private final ContainerLevelAccess context;
    @Nullable private final String targetModuleStateKey;
    @Nullable private final ServerPlayer itemConfigPlayer;
    @Nullable private final InteractionHand itemConfigHand;
    @Nullable private final ItemStack pinnedStack;

    public AdvancedExtractorScreenHandler(int syncId, Container playerInventory) {
        this(syncId, playerInventory, null, null, new SimpleContainerData(1));
    }

    public AdvancedExtractorScreenHandler(int syncId, Container playerInventory, PipeBlockEntity pipeEntity) {
        this(syncId, playerInventory, pipeEntity, null, new SimpleContainerData(1));
    }

    public AdvancedExtractorScreenHandler(
            int syncId, Container playerInventory, PipeBlockEntity pipeEntity, @Nullable String targetModuleStateKey) {
        this(syncId, playerInventory, pipeEntity, targetModuleStateKey, new SimpleContainerData(1));
    }

    public AdvancedExtractorScreenHandler(int syncId, Container playerInventory, ServerPlayer player, InteractionHand hand) {
        super(LogisticsPipe.SCREEN.ADVANCED_EXTRACTOR, syncId);
        this.data = new SimpleContainerData(1);
        this.itemConfigPlayer = player;
        this.itemConfigHand = hand;
        this.targetModuleStateKey = null;
        this.context = ContainerLevelAccess.NULL;
        ItemStack stack = player.getItemInHand(hand);
        this.pinnedStack = stack;
        this.filterInventory = new AdvancedExtractorFilterInventory(null);
        this.filterInventory.loadFromItem(stack);
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            data.set(0, NbtCompat.getInt(customData.copyTag(), AdvancedExtractorModule.FILTER_INVERTED, 0));
        }
        addFilterSlots(filterInventory);
        addPlayerInventorySlots(playerInventory);
        addDataSlots(data);
    }

    private AdvancedExtractorScreenHandler(
            int syncId,
            Container playerInventory,
            PipeBlockEntity pipeEntity,
            @Nullable String targetModuleStateKey,
            ContainerData data) {
        super(LogisticsPipe.SCREEN.ADVANCED_EXTRACTOR, syncId);
        this.data = data;
        this.targetModuleStateKey = targetModuleStateKey;
        this.itemConfigPlayer = null;
        this.itemConfigHand = null;
        this.pinnedStack = null;
        this.filterInventory = new AdvancedExtractorFilterInventory(pipeEntity, targetModuleStateKey);

        if (pipeEntity != null) {
            this.context = ContainerLevelAccess.create(pipeEntity.getLevel(), pipeEntity.getBlockPos());
            PipeModuleHelper.withModule(pipeEntity, AdvancedExtractorModule.class, targetModuleStateKey, (ctx, module) ->
                    data.set(0, module.isFilterInverted(ctx) ? 1 : 0));
        } else {
            this.context = ContainerLevelAccess.NULL;
        }

        addFilterSlots(filterInventory);
        addPlayerInventorySlots(playerInventory);
        addDataSlots(data);
    }

    private void addFilterSlots(Container inventory) {
        for (int col = 0; col < 9; col++) {
            addSlot(new FilterSlot(inventory, col, SLOT_START_X + col * SLOT_SIZE, FILTER_START_Y));
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
        return pinnedStack != null && itemConfigPlayer != null
                && itemConfigPlayer.getItemInHand(itemConfigHand) == pinnedStack;
    }

    @Override
    public boolean stillValid(Player player) {
        if (itemConfigPlayer != null) {
            return isPinnedItemStillHeld();
        }
        return PipeMenuValidity.stillValid(context, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }

    @Override
    protected boolean handleCustomSlotClick(int slotIndex, int button, boolean quickMove, Player player) {
        if (slotIndex >= 0 && slotIndex < FILTER_SLOT_COUNT) {
            if (quickMove) return true;

            ItemStack cursor = getCarried();

            if (button == 1 || cursor.isEmpty()) {
                if (itemConfigPlayer != null) {
                    if (!isPinnedItemStillHeld()) return true;
                    filterInventory.setItem(slotIndex, ItemStack.EMPTY);
                    final int s = slotIndex;
                    ItemTagUtils.writeToItemTag(itemConfigPlayer, itemConfigHand, tag -> {
                        CompoundTag fi = NbtCompat.getCompoundOrEmpty(tag, AdvancedExtractorModule.FILTER_ITEMS);
                        fi.remove(String.valueOf(s));
                        tag.put(AdvancedExtractorModule.FILTER_ITEMS, fi);
                    });
                } else {
                    filterInventory.setItem(slotIndex, ItemStack.EMPTY);
                    PipeModuleHelper.withModule(context, AdvancedExtractorModule.class, targetModuleStateKey, (ctx, module) ->
                            module.setFilterItem(ctx, slotIndex, ""));
                }
                broadcastChanges();
                return true;
            }

            String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .getKey(cursor.getItem()).toString();
            if (itemConfigPlayer != null) {
                if (!isPinnedItemStillHeld()) return true;
                filterInventory.setItem(slotIndex, cursor.copyWithCount(1));
                final int s = slotIndex;
                ItemTagUtils.writeToItemTag(itemConfigPlayer, itemConfigHand, tag -> {
                    CompoundTag fi = NbtCompat.getCompoundOrEmpty(tag, AdvancedExtractorModule.FILTER_ITEMS);
                    fi.putString(String.valueOf(s), itemId);
                    tag.put(AdvancedExtractorModule.FILTER_ITEMS, fi);
                });
            } else {
                filterInventory.setItem(slotIndex, cursor.copyWithCount(1));
                PipeModuleHelper.withModule(context, AdvancedExtractorModule.class, targetModuleStateKey, (ctx, module) ->
                        module.setFilterItem(ctx, slotIndex, itemId));
            }
            broadcastChanges();
            return true;
        }

        return false;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            int newInverted = data.get(0) == 0 ? 1 : 0;
            data.set(0, newInverted);
            if (itemConfigPlayer != null) {
                if (!isPinnedItemStillHeld()) return true;
                ItemTagUtils.writeToItemTag(itemConfigPlayer, itemConfigHand, tag -> tag.putInt(AdvancedExtractorModule.FILTER_INVERTED, newInverted));
            } else {
                PipeModuleHelper.withModule(context, AdvancedExtractorModule.class, targetModuleStateKey, (ctx, module) ->
                        module.setFilterInverted(ctx, newInverted == 1));
            }
            return true;
        }
        return false;
    }

    public boolean isFilterInverted() {
        return data.get(0) == 1;
    }

    @Override
    public void broadcastChanges() {
        if (itemConfigPlayer != null) {
            if (isPinnedItemStillHeld()) {
                ItemStack stack = itemConfigPlayer.getItemInHand(itemConfigHand);
                CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
                if (customData != null) {
                    data.set(0, NbtCompat.getInt(customData.copyTag(), AdvancedExtractorModule.FILTER_INVERTED, 0));
                } else {
                    data.set(0, 0);
                }
            }
            super.broadcastChanges();
            return;
        }
        PipeModuleHelper.withModule(context, AdvancedExtractorModule.class, targetModuleStateKey, (ctx, module) ->
                data.set(0, module.isFilterInverted(ctx) ? 1 : 0));
        super.broadcastChanges();
    }

    private static class FilterSlot extends Slot {
        FilterSlot(Container inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) { return false; }

        @Override
        public boolean mayPickup(Player player) { return false; }
    }
}
