package com.logistics.pipe.ui;

import com.logistics.LogisticsPipe;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.ItemFilterModule;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

public class ItemFilterScreenHandler extends CustomSlotScreenHandler {
    private static final int FILTER_SLOT_COUNT =
            ItemFilterModule.FILTER_ORDER.length * ItemFilterModule.FILTER_SLOTS_PER_SIDE;
    private static final int SLOT_SIZE = 18;
    private static final int PLAYER_INV_START_Y = 140;
    private static final int HOTBAR_Y = 198;
    private static final int SLOT_START_X = 8;
    private static final int SLOT_START_Y = 18;
    private static final int FILTER_SLOT_START_X = SLOT_START_X + SLOT_SIZE;

    private final FilterInventory filterInventory;
    private final ContainerLevelAccess context;
    @Nullable private final String targetModuleStateKey;
    @Nullable private final ServerPlayer itemConfigPlayer;
    @Nullable private final InteractionHand itemConfigHand;
    @Nullable private final ItemStack originalStack;

    public ItemFilterScreenHandler(int syncId, Container playerInventory) {
        super(LogisticsPipe.SCREEN.ITEM_FILTER, syncId);
        this.context = ContainerLevelAccess.NULL;
        this.targetModuleStateKey = null;
        this.itemConfigPlayer = null;
        this.itemConfigHand = null;
        this.originalStack = null;
        this.filterInventory = new FilterInventory(null);

        addFilterSlots(filterInventory);
        addPlayerInventorySlots(playerInventory);
    }

    public ItemFilterScreenHandler(int syncId, Container playerInventory, PipeBlockEntity pipeEntity) {
        this(syncId, playerInventory, pipeEntity, null);
    }

    public ItemFilterScreenHandler(
            int syncId, Container playerInventory, PipeBlockEntity pipeEntity, @Nullable String targetModuleStateKey) {
        super(LogisticsPipe.SCREEN.ITEM_FILTER, syncId);
        this.targetModuleStateKey = targetModuleStateKey;
        this.itemConfigPlayer = null;
        this.itemConfigHand = null;
        this.originalStack = null;
        if (pipeEntity != null) {
            this.context = ContainerLevelAccess.create(pipeEntity.getLevel(), pipeEntity.getBlockPos());
        } else {
            this.context = ContainerLevelAccess.NULL;
        }
        this.filterInventory = new FilterInventory(pipeEntity, targetModuleStateKey);

        addFilterSlots(filterInventory);
        addPlayerInventorySlots(playerInventory);
    }

    public ItemFilterScreenHandler(int syncId, Container playerInventory, ServerPlayer player, InteractionHand hand) {
        super(LogisticsPipe.SCREEN.ITEM_FILTER, syncId);
        this.targetModuleStateKey = null;
        this.itemConfigPlayer = player;
        this.itemConfigHand = hand;
        this.originalStack = player.getItemInHand(hand);
        this.context = ContainerLevelAccess.NULL;
        this.filterInventory = new FilterInventory(null);
        this.filterInventory.loadFromItem(this.originalStack, player.level().registryAccess());

        addFilterSlots(filterInventory);
        addPlayerInventorySlots(playerInventory);
    }

    private void addFilterSlots(Container inventory) {
        int slotIndex = 0;
        for (int row = 0; row < ItemFilterModule.FILTER_ORDER.length; row++) {
            for (int col = 0; col < ItemFilterModule.FILTER_SLOTS_PER_SIDE; col++) {
                int x = FILTER_SLOT_START_X + col * SLOT_SIZE;
                int y = SLOT_START_Y + row * SLOT_SIZE;
                addSlot(new FilterSlot(inventory, slotIndex++, x, y));
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
        if (itemConfigPlayer != null) {
            return player.getItemInHand(itemConfigHand) == originalStack;
        }
        return PipeMenuValidity.stillValid(context, player);
    }

    @Override
    protected boolean handleCustomSlotClick(int slotIndex, int button, boolean quickMove, Player player) {
        if (slotIndex >= 0 && slotIndex < FILTER_SLOT_COUNT) {
            if (quickMove) {
                return true;
            }

            ItemStack cursor = getCarried();
            if (cursor.isEmpty()) {
                filterInventory.setItem(slotIndex, ItemStack.EMPTY);
            } else {
                filterInventory.setItem(slotIndex, cursor.copyWithCount(1));
            }

            if (itemConfigPlayer != null) {
                syncFiltersToItem();
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

    /** Writes the full filter state to the held item's CUSTOM_DATA, preserving component data. */
    private void syncFiltersToItem() {
        if (itemConfigPlayer == null || itemConfigPlayer.getItemInHand(itemConfigHand) != originalStack) return;
        ItemStack stack = originalStack;
        CustomData existing = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = existing != null ? existing.copyTag() : new CompoundTag();

        RegistryOps<Tag> ops = itemConfigPlayer.level().registryAccess()
                .createSerializationContext(NbtOps.INSTANCE);
        CompoundTag filtersTag = new CompoundTag();
        int slotIndex = 0;
        for (Direction direction : ItemFilterModule.FILTER_ORDER) {
            ListTag list = new ListTag();
            boolean hasAny = false;
            for (int i = 0; i < ItemFilterModule.FILTER_SLOTS_PER_SIDE; i++) {
                ItemStack slotItem = filterInventory.getItem(slotIndex++);
                if (!slotItem.isEmpty()) {
                    hasAny = true;
                    CompoundTag encoded = ItemStack.CODEC.encodeStart(ops, slotItem.copyWithCount(1))
                            .result()
                            .filter(t -> t instanceof CompoundTag)
                            .map(t -> (CompoundTag) t)
                            .orElse(new CompoundTag());
                    list.add(encoded);
                } else {
                    list.add(new CompoundTag());
                }
            }
            if (hasAny) {
                filtersTag.put(direction.getName(), list);
            }
        }
        tag.put(ItemFilterModule.FILTERS, filtersTag);

        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        itemConfigPlayer.getInventory().setChanged();
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
