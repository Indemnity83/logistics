package com.logistics.power.engine.ui;

import com.logistics.LogisticsPower;
import com.logistics.power.engine.block.entity.StirlingEngineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Screen handler for the Stirling Engine GUI.
 * Manages the fuel slot and syncs burn time/heat data to the client.
 */
public class StirlingEngineScreenHandler extends AbstractContainerMenu {
    private static final int SLOT_SIZE = 18;
    private static final int PLAYER_INV_START_Y = 84;
    private static final int HOTBAR_Y = 142;
    private static final int SLOT_START_X = 8;

    // Fuel slot position (centered horizontally, near top)
    private static final int FUEL_SLOT_X = 80;
    private static final int FUEL_SLOT_Y = 39;

    private final Container inventory;
    private final ContainerData propertyDelegate;

    // Client constructor (called from ExtendedAbstractContainerMenuType packet)
    @SuppressWarnings("unused")
    public StirlingEngineScreenHandler(int syncId, Inventory playerInventory, BlockPos unusedPos) {
        this(
                syncId,
                playerInventory,
                new SimpleContainer(1),
                new SimpleContainerData(StirlingEngineBlockEntity.PROPERTY_COUNT));
    }

    // Server constructor
    public StirlingEngineScreenHandler(
            int syncId,
            Inventory playerInventory,
            StirlingEngineBlockEntity entity,
            ContainerData propertyDelegate) {
        this(syncId, playerInventory, (Container) entity, propertyDelegate);
    }

    private StirlingEngineScreenHandler(
            int syncId, Inventory playerInventory, Container inventory, ContainerData propertyDelegate) {
        super(LogisticsPower.SCREEN.STIRLING_ENGINE, syncId);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;

        checkContainerSize(inventory, 1);
        inventory.startOpen(playerInventory.player);

        // Add the fuel slot
        addSlot(new FuelSlot(inventory, 0, FUEL_SLOT_X, FUEL_SLOT_Y));

        // Add player inventory slots
        addInventorySlots(playerInventory);

        // Register property delegate for syncing
        addDataSlots(propertyDelegate);
    }

    private void addInventorySlots(Inventory playerInventory) {
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

        // Slot 0: fuel slot, 1-27: player main inventory, 28-36: hotbar
        if (slotIndex == 0) {
            // Moving from fuel slot to player inventory
            if (!moveItemStackTo(originalStack, 1, 37, true)) {
                return ItemStack.EMPTY;
            }
        } else if (isFuel(player, originalStack) && moveItemStackTo(originalStack, 0, 1, false)) {
            // Successfully moved fuel to fuel slot
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

    /**
     * Moves an item between main inventory and hotbar.
     * @return true if the item was successfully moved
     */
    private boolean moveWithinInventory(ItemStack stack, int slotIndex) {
        if (slotIndex < 28) {
            // Move from main inventory to hotbar
            return moveItemStackTo(stack, 28, 37, false);
        } else {
            // Move from hotbar to main inventory
            return moveItemStackTo(stack, 1, 28, false);
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        inventory.stopOpen(player);
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
        FuelSlot(Container inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }
    }

    /**
     * Checks if an item is valid fuel using the world's fuel registry.
     */
    private static boolean isFuel(Player player, ItemStack stack) {
        return player.level().fuelValues().isFuel(stack);
    }
}
