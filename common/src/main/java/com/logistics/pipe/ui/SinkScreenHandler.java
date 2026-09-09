package com.logistics.pipe.ui;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.item.ModuleItem;
import com.logistics.pipe.modules.SinkModule;
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
 * Screen handler for the Sink GUI (Basic Logistics Pipe).
 * Displays 9 filter slots and a Default Route toggle.
 */
public class SinkScreenHandler extends CustomSlotScreenHandler {
    private static final int FILTER_SLOT_COUNT = SinkModule.MAX_FILTER_SLOTS;
    private static final int SLOT_SIZE = 18;
    private static final int PLAYER_INV_START_Y = 60;
    private static final int HOTBAR_Y = 118;
    private static final int SLOT_START_X = 8;
    private static final int SLOT_START_Y = 18;

    private final SinkInventory sinkInventory;
    private final ContainerLevelAccess context;
    private final ContainerData data;
    @Nullable private final String targetModuleStateKey;
    @Nullable private final ServerPlayer itemConfigPlayer;
    @Nullable private final InteractionHand itemConfigHand;
    @Nullable private final ItemStack originalModuleStack;

    public SinkScreenHandler(int syncId, Container playerInventory) {
        this(syncId, playerInventory, null, null, new SimpleContainerData(1));
    }

    public SinkScreenHandler(int syncId, Container playerInventory, PipeBlockEntity pipeEntity) {
        this(syncId, playerInventory, pipeEntity, null, new SimpleContainerData(1));
    }

    public SinkScreenHandler(
            int syncId, Container playerInventory, PipeBlockEntity pipeEntity, @Nullable String targetModuleStateKey) {
        this(syncId, playerInventory, pipeEntity, targetModuleStateKey, new SimpleContainerData(1));
    }

    public SinkScreenHandler(int syncId, Container playerInventory, ServerPlayer player, InteractionHand hand) {
        super(LogisticsPipe.SCREEN.SINK, syncId);
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || !(stack.getItem() instanceof ModuleItem)) {
            throw new IllegalArgumentException("Expected a ModuleItem in " + hand + " hand");
        }
        this.data = new SimpleContainerData(1);
        this.targetModuleStateKey = null;
        this.itemConfigPlayer = player;
        this.itemConfigHand = hand;
        this.context = ContainerLevelAccess.NULL;
        this.originalModuleStack = stack;
        this.sinkInventory = new SinkInventory(null);
        this.sinkInventory.loadFromItem(stack);
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            data.set(0, NbtCompat.getInt(customData.copyTag(), SinkModule.DEFAULT_ROUTE, 0));
        }
        addFilterSlots(sinkInventory);
        addPlayerInventorySlots(playerInventory);
        addDataSlots(data);
    }

    private SinkScreenHandler(
            int syncId,
            Container playerInventory,
            PipeBlockEntity pipeEntity,
            @Nullable String targetModuleStateKey,
            ContainerData data) {
        super(LogisticsPipe.SCREEN.SINK, syncId);
        this.data = data;
        this.targetModuleStateKey = targetModuleStateKey;
        this.itemConfigPlayer = null;
        this.itemConfigHand = null;
        this.originalModuleStack = null;

        if (pipeEntity != null) {
            this.context = ContainerLevelAccess.create(pipeEntity.getLevel(), pipeEntity.getBlockPos());

            // Load default route setting (server-side)
            PipeModuleHelper.withModule(this.context, SinkModule.class, targetModuleStateKey, (ctx, module) -> {
                data.set(0, module.isDefaultRoute(ctx) ? 1 : 0);
            });
        } else {
            this.context = ContainerLevelAccess.NULL;
        }
        this.sinkInventory = new SinkInventory(pipeEntity, targetModuleStateKey);

        addFilterSlots(sinkInventory);
        addPlayerInventorySlots(playerInventory);
        addDataSlots(data);
    }

    @Override
    public void broadcastChanges() {
        if (itemConfigPlayer != null) {
            if (isPinnedItemStillHeld()) {
                ItemStack stack = itemConfigPlayer.getItemInHand(itemConfigHand);
                CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
                if (customData != null) {
                    data.set(0, NbtCompat.getInt(customData.copyTag(), SinkModule.DEFAULT_ROUTE, 0));
                } else {
                    data.set(0, 0);
                }
            }
            super.broadcastChanges();
            return;
        }
        // Sync default route value from module to data slot
        PipeModuleHelper.withModule(context, SinkModule.class, targetModuleStateKey, (ctx, module) -> {
            data.set(0, module.isDefaultRoute(ctx) ? 1 : 0);
        });
        super.broadcastChanges();
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

    private boolean isPinnedItemStillHeld() {
        return originalModuleStack != null && itemConfigPlayer != null
                && itemConfigPlayer.getItemInHand(itemConfigHand) == originalModuleStack;
    }

    @Override
    public boolean stillValid(Player player) {
        if (itemConfigPlayer != null) {
            return isPinnedItemStillHeld();
        }
        return PipeMenuValidity.stillValid(context, player);
    }

    @Override
    protected boolean handleCustomSlotClick(int slotIndex, int button, boolean quickMove, Player player) {
        if (slotIndex >= 0 && slotIndex < FILTER_SLOT_COUNT) {
            // Prevent shift-click
            if (quickMove) {
                return true;
            }

            ItemStack cursor = getCarried();

            // Right-click: Clear slot
            if (button == 1) {
                if (itemConfigPlayer != null) {
                    if (!isPinnedItemStillHeld()) return true;
                    sinkInventory.setItem(slotIndex, ItemStack.EMPTY);
                    final int s = slotIndex;
                    ItemTagUtils.writeToItemTag(itemConfigPlayer, itemConfigHand, tag -> {
                        CompoundTag filters = NbtCompat.getCompoundOrEmpty(tag, SinkModule.FILTERS);
                        filters.remove(String.valueOf(s));
                        if (filters.isEmpty()) {
                            tag.remove(SinkModule.FILTERS);
                        } else {
                            tag.put(SinkModule.FILTERS, filters);
                        }
                    });
                } else {
                    sinkInventory.setItem(slotIndex, ItemStack.EMPTY);
                    PipeModuleHelper.withModule(context, SinkModule.class, targetModuleStateKey, (ctx, module) -> {
                        module.setFilter(ctx, slotIndex, "");
                    });
                }
                broadcastChanges();
                return true;
            }

            // Left-click: Set filter
            if (cursor.isEmpty()) {
                return true; // Nothing to do
            }

            String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(cursor.getItem()).toString();
            ItemStack ghostItem = cursor.copyWithCount(1); // Filters don't need count

            // Save to module configuration
            if (itemConfigPlayer != null) {
                if (!isPinnedItemStillHeld()) return true;
                sinkInventory.setItem(slotIndex, ghostItem);
                final int s = slotIndex;
                ItemTagUtils.writeToItemTag(itemConfigPlayer, itemConfigHand, tag -> {
                    CompoundTag filters = NbtCompat.getCompoundOrEmpty(tag, SinkModule.FILTERS);
                    filters.putString(String.valueOf(s), itemId);
                    tag.put(SinkModule.FILTERS, filters);
                });
            } else {
                sinkInventory.setItem(slotIndex, ghostItem);
                PipeModuleHelper.withModule(context, SinkModule.class, targetModuleStateKey, (ctx, module) -> {
                    module.setFilter(ctx, slotIndex, itemId);
                });
            }

            broadcastChanges();
            return true;
        }

        return false;
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

            if (itemConfigPlayer != null) {
                if (!isPinnedItemStillHeld()) return true;
                ItemTagUtils.writeToItemTag(itemConfigPlayer, itemConfigHand, tag -> tag.putInt(SinkModule.DEFAULT_ROUTE, newValue ? 1 : 0));
            } else {
                PipeModuleHelper.withModule(context, SinkModule.class, targetModuleStateKey, (ctx, module) -> {
                    module.setDefaultRoute(ctx, newValue);
                });
            }
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
