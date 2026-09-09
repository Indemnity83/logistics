package com.logistics.pipe.ui;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.compat.NbtCompat;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.modules.CraftingModule;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
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
 * Screen handler for the Crafting Logistics Pipe GUI.
 * Displays a 3x3 ghost crafting grid, a ghost result slot, and a blocking mode toggle.
 */
public class CraftingScreenHandler extends CustomSlotScreenHandler {
    // Slot layout
    private static final int INGREDIENT_SLOTS = 9;
    private static final int RESULT_SLOT = 9;
    private static final int TOTAL_GHOST_SLOTS = 10;
    private static final int SLOT_SIZE = 18;

    // ContainerData indices
    private static final int DATA_CRAFT_STATE = 0;
    private static final int DATA_BLOCKING = 1;

    private final CraftingRecipeInventory recipeInventory;
    private final ContainerLevelAccess context;
    private final ContainerData data;
    @Nullable private final String targetModuleStateKey;
    @Nullable private final ServerPlayer itemConfigPlayer;
    @Nullable private final InteractionHand itemConfigHand;
    @Nullable private final ItemStack pinnedStack;

    public CraftingScreenHandler(int syncId, Container playerInventory) {
        this(syncId, playerInventory, null, null, new SimpleContainerData(2));
    }

    public CraftingScreenHandler(int syncId, Container playerInventory, PipeBlockEntity pipeEntity) {
        this(syncId, playerInventory, pipeEntity, null, new SimpleContainerData(2));
    }

    public CraftingScreenHandler(
            int syncId, Container playerInventory, PipeBlockEntity pipeEntity, @Nullable String targetModuleStateKey) {
        this(syncId, playerInventory, pipeEntity, targetModuleStateKey, new SimpleContainerData(2));
    }

    /** Item-mode constructor: backs the screen with a module item in the player's hand. */
    public CraftingScreenHandler(int syncId, Container playerInventory, ServerPlayer player, InteractionHand hand) {
        super(LogisticsPipe.SCREEN.CRAFTING, syncId);
        this.data = new SimpleContainerData(2);
        this.targetModuleStateKey = null;
        this.itemConfigPlayer = player;
        this.itemConfigHand = hand;
        this.context = ContainerLevelAccess.NULL;

        ItemStack stack = player.getItemInHand(hand);
        this.pinnedStack = stack;
        this.recipeInventory = new CraftingRecipeInventory(null);
        this.recipeInventory.loadFromItem(stack);

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            data.set(DATA_BLOCKING, NbtCompat.getInt(tag, CraftingModule.BLOCKING, 0));
        }

        addGhostSlots(recipeInventory);
        addPlayerInventorySlots(playerInventory);
        addDataSlots(data);
    }

    private CraftingScreenHandler(
            int syncId,
            Container playerInventory,
            PipeBlockEntity pipeEntity,
            @Nullable String targetModuleStateKey,
            ContainerData data) {
        super(LogisticsPipe.SCREEN.CRAFTING, syncId);
        this.data = data;
        this.targetModuleStateKey = targetModuleStateKey;
        this.itemConfigPlayer = null;
        this.itemConfigHand = null;
        this.pinnedStack = null;
        this.recipeInventory = new CraftingRecipeInventory(pipeEntity, targetModuleStateKey);

        if (pipeEntity != null) {
            this.context = ContainerLevelAccess.create(pipeEntity.getLevel(), pipeEntity.getBlockPos());
            PipeModuleHelper.withModule(pipeEntity, CraftingModule.class, targetModuleStateKey, (ctx, module) -> {
                data.set(DATA_CRAFT_STATE, module.isActive(ctx) ? 1 : 0);
                data.set(DATA_BLOCKING, module.isBlocking(ctx) ? 1 : 0);
            });
        } else {
            this.context = ContainerLevelAccess.NULL;
        }

        addGhostSlots(recipeInventory);
        addPlayerInventorySlots(playerInventory);
        addDataSlots(data);
    }

    private void addGhostSlots(Container inventory) {
        // 3x3 ingredient grid (slots 0-8)
        int gridStartX = 30;
        int gridStartY = 17;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                addSlot(new GhostSlot(inventory, index, gridStartX + col * SLOT_SIZE, gridStartY + row * SLOT_SIZE));
            }
        }
        // Result slot (slot 9)
        addSlot(new GhostSlot(inventory, RESULT_SLOT, 124, 35));
    }

    private void addPlayerInventorySlots(Container playerInventory) {
        int playerInvStartX = 8;
        int playerInvStartY = 84;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(
                        playerInventory,
                        col + row * 9 + 9,
                        playerInvStartX + col * SLOT_SIZE,
                        playerInvStartY + row * SLOT_SIZE));
            }
        }
        int hotbarY = 142;
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, playerInvStartX + col * SLOT_SIZE, hotbarY));
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
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    protected boolean handleCustomSlotClick(int slotIndex, int button, boolean quickMove, Player player) {
        if (slotIndex >= 0 && slotIndex < TOTAL_GHOST_SLOTS) {
            if (quickMove) return true;

            ItemStack cursor = getCarried();
            boolean isResultSlot = slotIndex == RESULT_SLOT;

            if (button == 1 || cursor.isEmpty()) {
                if (isResultSlot) clearResultSlot();
                else clearIngredientSlot(slotIndex);
                broadcastChanges();
                return true;
            }

            // Left-click with item: place ghost item
            String itemId = BuiltInRegistries.ITEM.getKey(cursor.getItem()).toString();
            int count = cursor.getCount();
            if (isResultSlot) setResultSlot(cursor.copyWithCount(count), itemId, count);
            else setIngredientSlot(slotIndex, cursor.copyWithCount(count), itemId, count);
            broadcastChanges();
            return true;
        }

        return false;
    }

    private void clearResultSlot() {
        if (itemConfigPlayer != null) {
            if (!isPinnedItemStillHeld()) return;
            recipeInventory.setItem(RESULT_SLOT, ItemStack.EMPTY);
            ItemTagUtils.writeToItemTag(itemConfigPlayer, itemConfigHand,tag -> {
                tag.remove(CraftingModule.RESULT_ITEM);
                tag.remove(CraftingModule.RESULT_COUNT);
            });
        } else {
            recipeInventory.setItem(RESULT_SLOT, ItemStack.EMPTY);
            PipeModuleHelper.withModule(
                    context, CraftingModule.class, targetModuleStateKey, (ctx, module) -> module.setResult(ctx, "", 1));
        }
    }

    private void clearIngredientSlot(int slot) {
        if (itemConfigPlayer != null) {
            if (!isPinnedItemStillHeld()) return;
            recipeInventory.setItem(slot, ItemStack.EMPTY);
            ItemTagUtils.writeToItemTag(itemConfigPlayer, itemConfigHand,tag -> {
                CompoundTag items = NbtCompat.getCompoundOrEmpty(tag, CraftingModule.RECIPE_ITEMS);
                CompoundTag counts = NbtCompat.getCompoundOrEmpty(tag, CraftingModule.RECIPE_COUNTS);
                items.remove(String.valueOf(slot));
                counts.remove(String.valueOf(slot));
                tag.put(CraftingModule.RECIPE_ITEMS, items);
                tag.put(CraftingModule.RECIPE_COUNTS, counts);
            });
        } else {
            recipeInventory.setItem(slot, ItemStack.EMPTY);
            PipeModuleHelper.withModule(
                    context,
                    CraftingModule.class,
                    targetModuleStateKey,
                    (ctx, module) -> module.setIngredient(ctx, slot, "", 1));
        }
    }

    private void setResultSlot(ItemStack display, String itemId, int count) {
        if (itemConfigPlayer != null) {
            if (!isPinnedItemStillHeld()) return;
            recipeInventory.setItem(RESULT_SLOT, display);
            ItemTagUtils.writeToItemTag(itemConfigPlayer, itemConfigHand,tag -> {
                tag.putString(CraftingModule.RESULT_ITEM, itemId);
                tag.putInt(CraftingModule.RESULT_COUNT, count);
            });
        } else {
            recipeInventory.setItem(RESULT_SLOT, display);
            PipeModuleHelper.withModule(
                    context,
                    CraftingModule.class,
                    targetModuleStateKey,
                    (ctx, module) -> module.setResult(ctx, itemId, count));
        }
    }

    private void setIngredientSlot(int slot, ItemStack display, String itemId, int count) {
        if (itemConfigPlayer != null) {
            if (!isPinnedItemStillHeld()) return;
            recipeInventory.setItem(slot, display);
            ItemTagUtils.writeToItemTag(itemConfigPlayer, itemConfigHand,tag -> {
                CompoundTag items = NbtCompat.getCompoundOrEmpty(tag, CraftingModule.RECIPE_ITEMS);
                CompoundTag counts = NbtCompat.getCompoundOrEmpty(tag, CraftingModule.RECIPE_COUNTS);
                items.putString(String.valueOf(slot), itemId);
                counts.putInt(String.valueOf(slot), Math.max(1, count));
                tag.put(CraftingModule.RECIPE_ITEMS, items);
                tag.put(CraftingModule.RECIPE_COUNTS, counts);
            });
        } else {
            recipeInventory.setItem(slot, display);
            PipeModuleHelper.withModule(
                    context,
                    CraftingModule.class,
                    targetModuleStateKey,
                    (ctx, module) -> module.setIngredient(ctx, slot, itemId, count));
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            // Toggle blocking mode
            int newBlocking = data.get(DATA_BLOCKING) == 0 ? 1 : 0;
            data.set(DATA_BLOCKING, newBlocking);
            if (itemConfigPlayer != null) {
                if (!isPinnedItemStillHeld()) return true;
                ItemTagUtils.writeToItemTag(itemConfigPlayer, itemConfigHand,tag -> tag.putInt(CraftingModule.BLOCKING, newBlocking));
            } else {
                PipeModuleHelper.withModule(context, CraftingModule.class, targetModuleStateKey, (ctx, module) -> {
                    module.setBlocking(ctx, newBlocking == 1);
                });
            }
            return true;
        }
        if (id == 1 && itemConfigPlayer == null) {
            // Import recipe from adjacent autocrafter
            PipeModuleHelper.withModule(
                    context,
                    CraftingModule.class,
                    targetModuleStateKey,
                    (ctx, module) -> module.importFromAutocrafter(ctx));
            recipeInventory.loadFromModule();
            broadcastChanges();
            return true;
        }
        return false;
    }

    @Override
    public void broadcastChanges() {
        if (itemConfigPlayer != null) {
            if (isPinnedItemStillHeld()) {
                ItemStack stack = itemConfigPlayer.getItemInHand(itemConfigHand);
                CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
                if (customData != null) {
                    CompoundTag tag = customData.copyTag();
                    data.set(DATA_BLOCKING, NbtCompat.getInt(tag, CraftingModule.BLOCKING, 0));
                } else {
                    data.set(DATA_BLOCKING, 0);
                }
            }
            super.broadcastChanges();
            return;
        }
        PipeModuleHelper.withModule(context, CraftingModule.class, targetModuleStateKey, (ctx, module) -> {
            data.set(DATA_CRAFT_STATE, module.isActive(ctx) ? 1 : 0);
            data.set(DATA_BLOCKING, module.isBlocking(ctx) ? 1 : 0);
        });
        super.broadcastChanges();
    }

    public int getCraftState() {
        return data.get(DATA_CRAFT_STATE);
    }

    public boolean isBlocking() {
        return data.get(DATA_BLOCKING) == 1;
    }

    /** Returns true when this screen is backed by a pipe entity with an adjacent autocrafter. */
    public boolean supportsAutocrafterImport() {
        return itemConfigPlayer == null;
    }

    private static class GhostSlot extends Slot {
        GhostSlot(Container inventory, int index, int x, int y) {
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
