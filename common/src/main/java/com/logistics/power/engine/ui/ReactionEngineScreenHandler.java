package com.logistics.power.engine.ui;

import com.logistics.LogisticsPower;
import com.logistics.power.engine.block.entity.ReactionEngineBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Screen handler for the Reaction Engine GUI — one catalyst slot plus synced reaction/reactant data.
 * The engine is bufferless, so there is no energy gauge and no energy data slot.
 */
public class ReactionEngineScreenHandler extends AbstractContainerMenu {
    private static final int SLOT_SIZE = 18;
    private static final int PLAYER_INV_START_Y = 84;
    private static final int HOTBAR_Y = 142;
    private static final int SLOT_START_X = 8;

    // Catalyst slot: texture frame at (58,34); the item sits 1px inside.
    private static final int CATALYST_SLOT_X = 59;
    private static final int CATALYST_SLOT_Y = 35;

    private final Container inventory;
    private final ContainerData propertyDelegate;

    // Client constructor (called from MenuType on screen open)
    public ReactionEngineScreenHandler(int syncId, Inventory playerInventory) {
        this(
                syncId,
                playerInventory,
                new SimpleContainer(1),
                new SimpleContainerData(ReactionEngineBlockEntity.DATA_COUNT));
    }

    // Server constructor
    public ReactionEngineScreenHandler(
            int syncId,
            Inventory playerInventory,
            ReactionEngineBlockEntity entity,
            ContainerData propertyDelegate) {
        this(syncId, playerInventory, (Container) entity, propertyDelegate);
    }

    private ReactionEngineScreenHandler(
            int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate) {
        super(LogisticsPower.SCREEN.REACTION_ENGINE, syncId);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        checkContainerSize(inventory, 1);
        inventory.startOpen(playerInventory.player);

        // Plain slot: recipe-based validity is enforced server-side (the client has no RecipeManager), so
        // the GUI doesn't hard-filter placement — matching the Macerator and the other item-input machines.
        addSlot(new Slot(inventory, 0, CATALYST_SLOT_X, CATALYST_SLOT_Y));
        addInventorySlots(playerInventory);
        addDataSlots(propertyDelegate);
    }

    private void addInventorySlots(Inventory playerInventory) {
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
        return inventory.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack originalStack = slot.getItem();
        ItemStack newStack = originalStack.copy();

        // Slot 0: catalyst slot, 1-27: player main inventory, 28-36: hotbar. Shift-clicking from the
        // inventory targets the catalyst slot first (the server rejects non-reagent items on ignition).
        if (slotIndex == 0) {
            if (!moveItemStackTo(originalStack, 1, 37, true)) {
                return ItemStack.EMPTY;
            }
        } else if (moveItemStackTo(originalStack, 0, 1, false)) {
            // moved into the catalyst slot
        } else if (!moveWithinInventory(originalStack, slotIndex)) {
            return ItemStack.EMPTY;
        }

        if (originalStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (originalStack.getCount() == newStack.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, originalStack);
        return newStack;
    }

    private boolean moveWithinInventory(ItemStack stack, int slotIndex) {
        if (slotIndex < 28) {
            return moveItemStackTo(stack, 28, 37, false);
        } else {
            return moveItemStackTo(stack, 1, 28, false);
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        inventory.stopOpen(player);
    }

    // ==================== GUI getters ====================

    public int getRemainingTicks() {
        return propertyDelegate.get(ReactionEngineBlockEntity.DATA_PROGRESS_REMAINING);
    }

    public int getTotalTicks() {
        return propertyDelegate.get(ReactionEngineBlockEntity.DATA_PROGRESS_TOTAL);
    }

    /** Reaction progress 0..1 — <b>fills</b> as the reaction proceeds (one-shot process, not a burn-down). */
    public float getReactionProgress() {
        int total = getTotalTicks();
        if (total <= 0) {
            return 0f;
        }
        return 1.0f - ((float) getRemainingTicks() / total);
    }

    /** Attempted output (RF/t) — what the reaction generates, matching the "Generating" wording. */
    public int getAttempted() {
        return propertyDelegate.get(ReactionEngineBlockEntity.DATA_ATTEMPTED);
    }

    /** Accepted output (RF/t) — what the network actually took this tick. */
    public int getAccepted() {
        return propertyDelegate.get(ReactionEngineBlockEntity.DATA_ACCEPTED);
    }

    /** Wasted output (RF/t) — generated but discarded because the network couldn't take it. */
    public int getWasted() {
        return Math.max(0, getAttempted() - getAccepted());
    }

    public boolean isReacting() {
        return propertyDelegate.get(ReactionEngineBlockEntity.DATA_REACTING) != 0;
    }

    public boolean isPowered() {
        return propertyDelegate.get(ReactionEngineBlockEntity.DATA_POWERED) != 0;
    }

    public Fluid getReactant() {
        int id = propertyDelegate.get(ReactionEngineBlockEntity.DATA_REACTANT_ID);
        return id < 0 ? Fluids.EMPTY : BuiltInRegistries.FLUID.byId(id);
    }

    /** Raw reactant fluid registry id, or -1 when the tank is empty. */
    public int getReactantFluidId() {
        return propertyDelegate.get(ReactionEngineBlockEntity.DATA_REACTANT_ID);
    }

    /** Reactant tank fill as a fraction 0..1. */
    public float getReactantFillFraction() {
        int capacity = getReactantCapacity();
        return capacity <= 0 ? 0f : Math.max(0f, Math.min(1f, (float) getReactantAmount() / capacity));
    }

    /** Progress-arrow width in pixels (0..24), filling as the reaction proceeds. */
    public int getProgressArrowWidth() {
        return Math.round(getReactionProgress() * 24f);
    }

    public int getReactantAmount() {
        return propertyDelegate.get(ReactionEngineBlockEntity.DATA_REACTANT_AMOUNT);
    }

    public int getReactantCapacity() {
        return propertyDelegate.get(ReactionEngineBlockEntity.DATA_REACTANT_CAPACITY);
    }

    /** Reactant tank fill height in pixels. */
    public int getReactantBarHeight(int maxPixels) {
        int capacity = getReactantCapacity();
        if (capacity <= 0) {
            return 0;
        }
        return Math.min(maxPixels, Math.max(0, getReactantAmount()) * maxPixels / capacity);
    }
}
