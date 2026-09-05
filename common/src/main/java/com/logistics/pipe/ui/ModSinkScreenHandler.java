package com.logistics.pipe.ui;

import com.logistics.LogisticsPipe;
import com.logistics.pipe.block.entity.PipeBlockEntity;
import com.logistics.pipe.item.ModuleItem;
import com.logistics.pipe.modules.ModSinkModule;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Prediction;
import org.jetbrains.annotations.Nullable;

/**
 * Screen handler for the Mod-Based Item Sink GUI.
 *
 * <p>Slot layout:
 * <ul>
 *   <li>Slot 0: preview slot (real item slot at 8, 19) — place an item here to identify its mod</li>
 *   <li>Slots 1-{@value MAX_FILTER_SLOTS}: mod filter entries (off-screen, synced to client)</li>
 *   <li>Slots {FILTER_SLOT_START + MAX_FILTER_SLOTS} to end: player inventory and hotbar</li>
 * </ul>
 *
 * <p>Button IDs:
 * <ul>
 *   <li>0: Add — copies the preview item's mod to the filter list</li>
 *   <li>1..{@value MAX_FILTER_SLOTS}: Remove filter at index (button - 1)</li>
 * </ul>
 *
 * <p>On close, any item in the preview slot is returned to the player's inventory.
 */
public class ModSinkScreenHandler extends CustomSlotScreenHandler {
    public static final int MAX_FILTER_SLOTS = ModSinkModule.MAX_FILTERS;
    public static final int PREVIEW_SLOT = 0;
    public static final int FILTER_SLOT_START = 1;
    private static final int SLOT_SIZE = 18;
    private static final int PLAYER_INV_START_Y = 102;
    private static final int HOTBAR_Y = 160;
    private static final int SLOT_START_X = 8;

    // Backing container for the single preview slot (real item slot, returned on close)
    private final SimpleContainer previewContainer = new SimpleContainer(1);
    private final ModSinkInventory modSinkInventory;
    private final ContainerLevelAccess context;
    @Nullable private final String targetModuleStateKey;
    @Nullable private final ServerPlayer itemConfigPlayer;
    @Nullable private final InteractionHand itemConfigHand;
    @Nullable private final ItemStack originalModuleStack;

    public ModSinkScreenHandler(int syncId, Container playerInventory) {
        this(syncId, playerInventory, (PipeBlockEntity) null);
    }

    public ModSinkScreenHandler(int syncId, Container playerInventory,
            @Nullable PipeBlockEntity pipeEntity) {
        this(syncId, playerInventory, pipeEntity, null);
    }

    public ModSinkScreenHandler(int syncId, Container playerInventory,
            @Nullable PipeBlockEntity pipeEntity, @Nullable String targetModuleStateKey) {
        super(LogisticsPipe.SCREEN.MOD_SINK, syncId);
        this.itemConfigPlayer = null;
        this.itemConfigHand = null;
        this.originalModuleStack = null;
        this.targetModuleStateKey = targetModuleStateKey;
        this.context = pipeEntity != null
                ? ContainerLevelAccess.create(pipeEntity.getLevel(), pipeEntity.getBlockPos())
                : ContainerLevelAccess.NULL;
        this.modSinkInventory = new ModSinkInventory(pipeEntity, targetModuleStateKey);
        addModSinkSlots();
        addPlayerInventorySlots(playerInventory);
    }

    public ModSinkScreenHandler(int syncId, Container playerInventory,
            ServerPlayer player, InteractionHand hand) {
        super(LogisticsPipe.SCREEN.MOD_SINK, syncId);
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || !(stack.getItem() instanceof ModuleItem)) {
            throw new IllegalArgumentException("Expected a ModuleItem in " + hand + " hand");
        }
        this.itemConfigPlayer = player;
        this.itemConfigHand = hand;
        this.originalModuleStack = stack;
        this.targetModuleStateKey = null;
        this.context = ContainerLevelAccess.NULL;
        this.modSinkInventory = new ModSinkInventory(null);
        this.modSinkInventory.loadFromItem(stack);
        addModSinkSlots();
        addPlayerInventorySlots(playerInventory);
    }

    private void addModSinkSlots() {
        // Preview slot: real item slot at (8, 19); item is returned to player on close
        addSlot(new Slot(previewContainer, 0, SLOT_START_X, 19));

        // Filter slots: off-screen ghost slots, registered only for client sync
        for (int i = 0; i < MAX_FILTER_SLOTS; i++) {
            addSlot(new GhostSlot(modSinkInventory, i, -200, -200));
        }
    }

    private void addPlayerInventorySlots(Container playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        SLOT_START_X + col * SLOT_SIZE, PLAYER_INV_START_Y + row * SLOT_SIZE));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, SLOT_START_X + col * SLOT_SIZE, HOTBAR_Y));
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // Return the preview item to the player, like a crafting table does
        if (!player.level().isClientSide()) {
            ItemStack preview = previewContainer.getItem(0);
            if (!preview.isEmpty()) {
                if (!player.getInventory().add(preview)) {
                    player.drop(preview, false, Prediction.SERVER_ONLY);
                }
                previewContainer.setItem(0, ItemStack.EMPTY);
            }
        }
    }

    @Override
    protected boolean handleCustomSlotClick(int slotIndex, int button, boolean quickMove, Player player) {
        // Filter display slots are not directly interactable
        return slotIndex >= FILTER_SLOT_START && slotIndex < FILTER_SLOT_START + MAX_FILTER_SLOTS;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            // Add: record the preview item's mod namespace in the filter list
            ItemStack preview = previewContainer.getItem(0);
            if (preview.isEmpty()) return false;

            String itemId = BuiltInRegistries.ITEM.getKey(preview.getItem()).toString();
            String namespace = BuiltInRegistries.ITEM.getKey(preview.getItem()).getNamespace();

            // Check for duplicate namespace
            for (int i = 0; i < MAX_FILTER_SLOTS; i++) {
                ItemStack existing = modSinkInventory.getItem(i);
                if (!existing.isEmpty()) {
                    String existNs = BuiltInRegistries.ITEM.getKey(existing.getItem()).getNamespace();
                    if (existNs.equals(namespace)) return true;
                }
            }

            // Add to first empty filter slot
            for (int i = 0; i < MAX_FILTER_SLOTS; i++) {
                if (modSinkInventory.getItem(i).isEmpty()) {
                    modSinkInventory.setItem(i, preview.copyWithCount(1));
                    final int slotIndex = i;
                    if (itemConfigPlayer != null) {
                        if (!isPinnedItemStillHeld()) return true;
                        ItemTagUtils.writeToItemTag(itemConfigPlayer, itemConfigHand,
                                tag -> tag.putString(ModSinkModule.MOD_IDS + "_" + slotIndex, itemId));
                    } else {
                        PipeModuleHelper.withModule(context, ModSinkModule.class, targetModuleStateKey,
                                (ctx, module) -> module.addModId(ctx, itemId));
                    }
                    break;
                }
            }
            broadcastChanges();
            return true;
        }

        if (id >= 1 && id <= MAX_FILTER_SLOTS) {
            // Remove: delete filter at index (id - 1), compact remaining
            int removeIndex = id - 1;
            if (modSinkInventory.getItem(removeIndex).isEmpty()) return false;

            // Compact: shift entries down
            for (int i = removeIndex; i < MAX_FILTER_SLOTS - 1; i++) {
                modSinkInventory.setItem(i, modSinkInventory.getItem(i + 1));
            }
            modSinkInventory.setItem(MAX_FILTER_SLOTS - 1, ItemStack.EMPTY);

            if (itemConfigPlayer != null) {
                if (!isPinnedItemStillHeld()) return true;
                ItemTagUtils.writeToItemTag(itemConfigPlayer, itemConfigHand, tag -> {
                    for (int i = 0; i < MAX_FILTER_SLOTS; i++) {
                        ItemStack s = modSinkInventory.getItem(i);
                        if (!s.isEmpty()) {
                            tag.putString(ModSinkModule.MOD_IDS + "_" + i,
                                    BuiltInRegistries.ITEM.getKey(s.getItem()).toString());
                        } else {
                            tag.remove(ModSinkModule.MOD_IDS + "_" + i);
                        }
                    }
                });
            } else {
                final int ri = removeIndex;
                PipeModuleHelper.withModule(context, ModSinkModule.class, targetModuleStateKey,
                        (ctx, module) -> module.removeModId(ctx, ri));
            }
            broadcastChanges();
            return true;
        }

        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        if (itemConfigPlayer != null) return isPinnedItemStillHeld();
        return PipeMenuValidity.stillValid(context, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    /** Returns the number of active (non-empty) mod filter entries. */
    public int getFilterCount() {
        int count = 0;
        for (int i = 0; i < MAX_FILTER_SLOTS; i++) {
            if (!modSinkInventory.getItem(i).isEmpty()) count++;
        }
        return count;
    }

    private boolean isPinnedItemStillHeld() {
        return originalModuleStack != null && itemConfigPlayer != null
                && itemConfigPlayer.getItemInHand(itemConfigHand) == originalModuleStack;
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
