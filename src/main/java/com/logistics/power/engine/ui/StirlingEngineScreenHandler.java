package com.logistics.power.engine.ui;

import com.logistics.power.engine.block.entity.StirlingEngineBlockEntity;
import com.logistics.power.registry.PowerScreenHandlers;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;

/**
 * Screen handler for the Stirling Engine GUI.
 * Manages the fuel slot and syncs burn time/heat data to the client.
 */
public class StirlingEngineScreenHandler extends ScreenHandler {
    private static final int SLOT_SIZE = 18;
    private static final int PLAYER_INV_START_Y = 84;
    private static final int HOTBAR_Y = 142;
    private static final int SLOT_START_X = 8;

    // Fuel slot position (matches vanilla furnace fuel slot position)
    private static final int FUEL_SLOT_X = 56;
    private static final int FUEL_SLOT_Y = 53;

    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    // Client constructor (called from ExtendedScreenHandlerType packet)
    @SuppressWarnings("unused")
    public StirlingEngineScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos unusedPos) {
        this(syncId, playerInventory, new SimpleInventory(1), new ArrayPropertyDelegate(StirlingEngineBlockEntity.PROPERTY_COUNT));
    }

    // Server constructor
    public StirlingEngineScreenHandler(int syncId, PlayerInventory playerInventory, StirlingEngineBlockEntity entity, PropertyDelegate propertyDelegate) {
        this(syncId, playerInventory, (Inventory) entity, propertyDelegate);
    }

    private StirlingEngineScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
        super(PowerScreenHandlers.STIRLING_ENGINE, syncId);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        checkSize(inventory, 1);
        inventory.onOpen(playerInventory.player);

        // Add the fuel slot
        addSlot(new FuelSlot(inventory, 0, FUEL_SLOT_X, FUEL_SLOT_Y));

        // Add player inventory slots
        addPlayerInventorySlots(playerInventory);

        // Register property delegate for syncing
        addProperties(propertyDelegate);
    }

    private void addPlayerInventorySlots(PlayerInventory playerInventory) {
        // Main inventory (3 rows of 9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(
                        playerInventory,
                        col + row * 9 + 9,
                        SLOT_START_X + col * SLOT_SIZE,
                        PLAYER_INV_START_Y + row * SLOT_SIZE));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, SLOT_START_X + col * SLOT_SIZE, HOTBAR_Y));
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasStack()) {
            return ItemStack.EMPTY;
        }

        ItemStack originalStack = slot.getStack();
        ItemStack newStack = originalStack.copy();

        // Slot 0: fuel slot, 1-27: player main inventory, 28-36: hotbar
        if (slotIndex == 0) {
            // Moving from fuel slot to player inventory
            if (!insertItem(originalStack, 1, 37, true)) {
                return ItemStack.EMPTY;
            }
        } else if (isFuel(player, originalStack) && insertItem(originalStack, 0, 1, false)) {
            // Successfully moved fuel to fuel slot
        } else if (!moveWithinPlayerInventory(originalStack, slotIndex)) {
            return ItemStack.EMPTY;
        }

        if (originalStack.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }

        if (originalStack.getCount() == newStack.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTakeItem(player, originalStack);
        return newStack;
    }

    /**
     * Moves an item between main inventory and hotbar.
     * @return true if the item was successfully moved
     */
    private boolean moveWithinPlayerInventory(ItemStack stack, int slotIndex) {
        if (slotIndex < 28) {
            // Move from main inventory to hotbar
            return insertItem(stack, 28, 37, false);
        } else {
            // Move from hotbar to main inventory
            return insertItem(stack, 1, 28, false);
        }
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        inventory.onClose(player);
    }

    // Getters for GUI rendering
    public int getBurnTime() {
        return propertyDelegate.get(StirlingEngineBlockEntity.PROPERTY_BURN_TIME);
    }

    public int getFuelTime() {
        return propertyDelegate.get(StirlingEngineBlockEntity.PROPERTY_FUEL_TIME);
    }

    public int getHeat() {
        return propertyDelegate.get(StirlingEngineBlockEntity.PROPERTY_HEAT);
    }

    public int getEnergy() {
        return propertyDelegate.get(StirlingEngineBlockEntity.PROPERTY_ENERGY);
    }

    /**
     * Gets the current generation rate in RF per tick (scaled by 100).
     * Returns the raw property value which is currentGeneration * 100.
     */
    public int getGenerationScaled() {
        return propertyDelegate.get(StirlingEngineBlockEntity.PROPERTY_GENERATION);
    }

    /**
     * Gets the current generation rate as a displayable value (RF/t).
     * Generation ranges from 3 to 10 RF/t.
     */
    public float getGenerationRate() {
        return getGenerationScaled() / 100.0f;
    }

    /**
     * Gets the burn progress as a fraction (0.0 to 1.0).
     * Used to render the flame icon in the GUI.
     */
    public float getBurnProgress() {
        int fuelTime = getFuelTime();
        if (fuelTime == 0) {
            return 0;
        }
        return 1.0f - ((float) getBurnTime() / fuelTime);
    }

    /**
     * Gets the heat level as a fraction (0.0 to 1.0).
     * Used to render the heat gauge in the GUI.
     * Max heat is 250 (aligned with BuildCraft).
     */
    public float getHeatProgress() {
        return getHeat() / 250.0f;
    }

    /**
     * Checks if the engine is currently burning fuel.
     */
    public boolean isBurning() {
        return getBurnTime() > 0;
    }

    /**
     * Checks if the engine is overheated (100% heat).
     */
    public boolean isOverheated() {
        return getHeatProgress() >= 1.0f;
    }

    /**
     * Custom slot that only accepts fuel items.
     */
    private static class FuelSlot extends Slot {
        FuelSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            // Delegate to the block entity's isValid method if available
            if (inventory instanceof StirlingEngineBlockEntity entity) {
                return entity.isValid(0, stack);
            }
            // Client-side SimpleInventory - allow insertion, server validates
            return true;
        }
    }

    /**
     * Checks if an item is valid fuel using the world's fuel registry.
     */
    private static boolean isFuel(PlayerEntity player, ItemStack stack) {
        return player.getEntityWorld().getFuelRegistry().getFuelTicks(stack) > 0;
    }
}
