package com.logistics.pipe.ui;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.storage.NbtCompat;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.item.ModuleItem;
import com.logistics.pipe.modules.CraftingModule;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
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
 * Screen handler for the Crafting Logistics Pipe GUI.
 * Displays a 3x3 ghost crafting grid, a ghost result slot, and a blocking mode toggle.
 */
public class CraftingScreenHandler extends AbstractContainerMenu {
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
    @Nullable private final ServerPlayer itemConfigPlayer;
    @Nullable private final InteractionHand itemConfigHand;

    public CraftingScreenHandler(int syncId, Container playerInventory) {
        this(syncId, playerInventory, null, new SimpleContainerData(2));
    }

    public CraftingScreenHandler(int syncId, Container playerInventory, PipeBlockEntity pipeEntity) {
        this(syncId, playerInventory, pipeEntity, new SimpleContainerData(2));
    }

    /** Item-mode constructor: backs the screen with a module item in the player's hand. */
    public CraftingScreenHandler(int syncId, Container playerInventory, ServerPlayer player, InteractionHand hand) {
        super(LogisticsPipe.SCREEN.CRAFTING, syncId);
        this.data = new SimpleContainerData(2);
        this.itemConfigPlayer = player;
        this.itemConfigHand = hand;
        this.context = ContainerLevelAccess.NULL;

        ItemStack stack = player.getItemInHand(hand);
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
            int syncId, Container playerInventory, PipeBlockEntity pipeEntity, ContainerData data) {
        super(LogisticsPipe.SCREEN.CRAFTING, syncId);
        this.data = data;
        this.itemConfigPlayer = null;
        this.itemConfigHand = null;
        this.recipeInventory = new CraftingRecipeInventory(pipeEntity);

        if (pipeEntity != null) {
            this.context = ContainerLevelAccess.create(pipeEntity.getLevel(), pipeEntity.getBlockPos());
            PipeModuleHelper.withModule(pipeEntity, CraftingModule.class, (ctx, module) -> {
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

    @Override
    public boolean stillValid(Player player) {
        if (itemConfigPlayer != null) {
            ItemStack held = player.getItemInHand(itemConfigHand);
            return !held.isEmpty() && held.getItem() instanceof ModuleItem;
        }
        return context.evaluate(
                (level, pos) -> player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0,
                true);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void clicked(int slotIndex, int button, ClickType actionType, Player player) {
        if (slotIndex >= 0 && slotIndex < TOTAL_GHOST_SLOTS) {
            if (actionType == ClickType.QUICK_MOVE) return;

            ItemStack cursor = getCarried();
            boolean isResultSlot = slotIndex == RESULT_SLOT;

            // Right-click or empty cursor clears slot
            if (button == 1 || cursor.isEmpty()) {
                recipeInventory.setItem(slotIndex, ItemStack.EMPTY);
                if (itemConfigPlayer != null) {
                    if (isResultSlot) {
                        ItemTagUtils.writeToItemTag(itemConfigPlayer, itemConfigHand,tag -> {
                            tag.remove(CraftingModule.RESULT_ITEM);
                            tag.remove(CraftingModule.RESULT_COUNT);
                        });
                    } else {
                        final int s = slotIndex;
                        ItemTagUtils.writeToItemTag(itemConfigPlayer, itemConfigHand,tag -> {
                            CompoundTag items = NbtCompat.getCompoundOrEmpty(tag, CraftingModule.RECIPE_ITEMS);
                            CompoundTag counts = NbtCompat.getCompoundOrEmpty(tag, CraftingModule.RECIPE_COUNTS);
                            items.remove(String.valueOf(s));
                            counts.remove(String.valueOf(s));
                            tag.put(CraftingModule.RECIPE_ITEMS, items);
                            tag.put(CraftingModule.RECIPE_COUNTS, counts);
                        });
                    }
                } else if (isResultSlot) {
                    PipeModuleHelper.withModule(context, CraftingModule.class, (ctx, module) -> module.setResult(ctx, "", 1));
                } else {
                    final int s = slotIndex;
                    PipeModuleHelper.withModule(context, CraftingModule.class, (ctx, module) -> module.setIngredient(ctx, s, "", 1));
                }
                broadcastChanges();
                return;
            }

            // Left-click with item: place ghost item
            String itemId = BuiltInRegistries.ITEM.getKey(cursor.getItem()).toString();
            int count = cursor.getCount();
            recipeInventory.setItem(slotIndex, cursor.copyWithCount(count));

            if (itemConfigPlayer != null) {
                if (isResultSlot) {
                    ItemTagUtils.writeToItemTag(itemConfigPlayer, itemConfigHand,tag -> {
                        tag.putString(CraftingModule.RESULT_ITEM, itemId);
                        tag.putInt(CraftingModule.RESULT_COUNT, count);
                    });
                } else {
                    final int s = slotIndex;
                    ItemTagUtils.writeToItemTag(itemConfigPlayer, itemConfigHand,tag -> {
                        CompoundTag items = NbtCompat.getCompoundOrEmpty(tag, CraftingModule.RECIPE_ITEMS);
                        CompoundTag counts = NbtCompat.getCompoundOrEmpty(tag, CraftingModule.RECIPE_COUNTS);
                        items.putString(String.valueOf(s), itemId);
                        counts.putInt(String.valueOf(s), Math.max(1, count));
                        tag.put(CraftingModule.RECIPE_ITEMS, items);
                        tag.put(CraftingModule.RECIPE_COUNTS, counts);
                    });
                }
            } else if (isResultSlot) {
                PipeModuleHelper.withModule(context, CraftingModule.class, (ctx, module) -> module.setResult(ctx, itemId, count));
            } else {
                final int s = slotIndex;
                PipeModuleHelper.withModule(context, CraftingModule.class, (ctx, module) -> module.setIngredient(ctx, s, itemId, count));
            }
            broadcastChanges();
            return;
        }

        super.clicked(slotIndex, button, actionType, player);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            // Toggle blocking mode
            int newBlocking = data.get(DATA_BLOCKING) == 0 ? 1 : 0;
            data.set(DATA_BLOCKING, newBlocking);
            if (itemConfigPlayer != null) {
                ItemTagUtils.writeToItemTag(itemConfigPlayer, itemConfigHand,tag -> tag.putInt(CraftingModule.BLOCKING, newBlocking));
            } else {
                PipeModuleHelper.withModule(context, CraftingModule.class, (ctx, module) -> {
                    module.setBlocking(ctx, newBlocking == 1);
                });
            }
            return true;
        }
        return false;
    }

    @Override
    public void broadcastChanges() {
        if (itemConfigPlayer != null) {
            ItemStack stack = itemConfigPlayer.getItemInHand(itemConfigHand);
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                CompoundTag tag = customData.copyTag();
                data.set(DATA_BLOCKING, NbtCompat.getInt(tag, CraftingModule.BLOCKING, 0));
            }
            super.broadcastChanges();
            return;
        }
        PipeModuleHelper.withModule(context, CraftingModule.class, (ctx, module) -> {
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
