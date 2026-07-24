package com.logistics.power.engine.ui;

import com.logistics.LogisticsPower;
import com.logistics.core.lib.power.FuelHelper;
import com.logistics.power.engine.block.entity.SteamEngineBlockEntity;
import com.logistics.power.engine.steam.SteamEngineStatus;
import com.logistics.power.engine.steam.SteamFireboxState;
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
 * Screen handler for the Steam Engine GUI: a solid-fuel slot plus a {@link ContainerData} carrying the
 * pressure, generation, water tank, committed burn reserve, and derived status/firebox state that the
 * screen renders. Mirrors the Stirling handler's fuel-slot plumbing.
 */
public class SteamEngineScreenHandler extends AbstractContainerMenu {
    private static final int SLOT_SIZE = 18;
    private static final int PLAYER_INV_START_Y = 84;
    private static final int HOTBAR_Y = 142;
    private static final int SLOT_START_X = 8;

    private static final int FUEL_SLOT_X = 80;
    private static final int FUEL_SLOT_Y = 35;

    private final Container inventory;
    private final ContainerData data;

    /** Client-side constructor. */
    public SteamEngineScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(1), new SimpleContainerData(SteamEngineBlockEntity.DATA_COUNT));
    }

    /** Server-side constructor. */
    public SteamEngineScreenHandler(
            int syncId, Inventory playerInventory, SteamEngineBlockEntity entity, ContainerData data) {
        this(syncId, playerInventory, (Container) entity, data);
    }

    private SteamEngineScreenHandler(
            int syncId, Inventory playerInventory, Container inventory, ContainerData data) {
        super(LogisticsPower.SCREEN.STEAM_ENGINE, syncId);
        checkContainerSize(inventory, 1);
        checkContainerDataCount(data, SteamEngineBlockEntity.DATA_COUNT);
        this.inventory = inventory;
        this.data = data;

        inventory.startOpen(playerInventory.player);

        addSlot(new FuelSlot(inventory, 0, FUEL_SLOT_X, FUEL_SLOT_Y));
        addInventorySlots(playerInventory);
        addDataSlots(data);
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

        // Slot 0: fuel slot, 1-27: player main inventory, 28-36: hotbar.
        if (slotIndex == 0) {
            if (!moveItemStackTo(originalStack, 1, 37, true)) {
                return ItemStack.EMPTY;
            }
        } else if (isFuel(player, originalStack) && moveItemStackTo(originalStack, 0, 1, false)) {
            // Moved fuel into the fuel slot.
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
        }
        return moveItemStackTo(stack, 1, 28, false);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        inventory.stopOpen(player);
    }

    // ==================== Data getters for GUI rendering ====================

    public boolean isSafetyValveActive() {
        return data.get(SteamEngineBlockEntity.DATA_SAFETY_VALVE) != 0;
    }

    public int getPressure() {
        return data.get(SteamEngineBlockEntity.DATA_PRESSURE);
    }

    public int getMaxPressure() {
        return data.get(SteamEngineBlockEntity.DATA_MAX_PRESSURE);
    }

    public int getOperatingPressure() {
        return data.get(SteamEngineBlockEntity.DATA_OPERATING);
    }

    public int getTargetPressure() {
        return data.get(SteamEngineBlockEntity.DATA_TARGET);
    }

    public int getBoilerHeat() {
        return data.get(SteamEngineBlockEntity.DATA_BOILER_HEAT);
    }

    public int getMaxHeat() {
        return data.get(SteamEngineBlockEntity.DATA_MAX_HEAT);
    }

    public int getBoilingHeat() {
        return data.get(SteamEngineBlockEntity.DATA_BOILING_HEAT);
    }

    public float getHeatFraction() {
        return fraction(getBoilerHeat(), getMaxHeat());
    }

    public float getPressureFraction() {
        return fraction(getPressure(), getMaxPressure());
    }

    public int getGeneration() {
        return data.get(SteamEngineBlockEntity.DATA_GENERATION);
    }

    public int getWaterId() {
        return data.get(SteamEngineBlockEntity.DATA_WATER_ID);
    }

    public int getWaterAmountMb() {
        return data.get(SteamEngineBlockEntity.DATA_WATER_AMOUNT);
    }

    public float getWaterFillFraction() {
        return fraction(getWaterAmountMb(), data.get(SteamEngineBlockEntity.DATA_WATER_CAPACITY));
    }

    public int getCommittedBurn() {
        return data.get(SteamEngineBlockEntity.DATA_COMMITTED_BURN);
    }

    /** Remaining committed burn reserve as a fraction (1 at ignition, 0 when spent) for the flame. */
    public float getBurnFraction() {
        return data.get(SteamEngineBlockEntity.DATA_BURN_FRACTION) / 1000f;
    }

    public SteamEngineStatus getStatus() {
        return ordinal(SteamEngineStatus.values(), data.get(SteamEngineBlockEntity.DATA_STATUS));
    }

    public SteamFireboxState getFirebox() {
        return ordinal(SteamFireboxState.values(), data.get(SteamEngineBlockEntity.DATA_FIREBOX));
    }

    private static <E> E ordinal(E[] values, int index) {
        return index >= 0 && index < values.length ? values[index] : values[0];
    }

    private static float fraction(int amount, int capacity) {
        if (amount <= 0 || capacity <= 0) {
            return 0f;
        }
        return Math.min(1f, amount / (float) capacity);
    }

    /** Custom slot for the fuel item (mirrors the Stirling handler's slot). */
    private static class FuelSlot extends Slot {
        FuelSlot(Container inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }
    }

    private static boolean isFuel(Player player, ItemStack stack) {
        return FuelHelper.isFuel(player.level(), stack);
    }
}
