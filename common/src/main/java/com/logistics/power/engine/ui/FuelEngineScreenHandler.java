package com.logistics.power.engine.ui;

import com.logistics.LogisticsPower;
import com.logistics.power.engine.block.entity.FuelEngineBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Screen handler for the Fuel Engine GUI. The engine has no item slots (fluids only), so this is a
 * display container: the player inventory plus a {@link ContainerData} carrying temperature, generation,
 * both tanks, and thermal-shutdown state for the screen to render.
 */
public class FuelEngineScreenHandler extends AbstractContainerMenu {

    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** Client-side constructor. */
    public FuelEngineScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainerData(FuelEngineBlockEntity.DATA_COUNT),
                ContainerLevelAccess.NULL);
    }

    /** Server-side constructor. */
    public FuelEngineScreenHandler(
            int syncId, Inventory playerInventory, ContainerData data, ContainerLevelAccess access) {
        super(LogisticsPower.SCREEN.FUEL_ENGINE, syncId);
        checkContainerDataCount(data, FuelEngineBlockEntity.DATA_COUNT);
        this.data = data;
        this.access = access;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // no machine slots to shuffle into
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(access, player, LogisticsPower.BLOCK.FUEL_ENGINE);
    }

    // ==================== Data getters for GUI rendering ====================

    public int getEnergyBarHeight(int maxPixels) {
        return data.get(FuelEngineBlockEntity.DATA_ENERGY) * maxPixels / 10_000;
    }

    public int getTemperature() {
        return data.get(FuelEngineBlockEntity.DATA_TEMPERATURE);
    }

    public int getMaxTemperature() {
        return data.get(FuelEngineBlockEntity.DATA_MAX_TEMP);
    }

    public int getGeneration() {
        return data.get(FuelEngineBlockEntity.DATA_GENERATION);
    }

    public int getFuelId() {
        return data.get(FuelEngineBlockEntity.DATA_FUEL_ID);
    }

    public int getFuelAmountMb() {
        return data.get(FuelEngineBlockEntity.DATA_FUEL_AMOUNT);
    }

    public float getFuelFillFraction() {
        return fill(getFuelAmountMb(), data.get(FuelEngineBlockEntity.DATA_FUEL_CAPACITY));
    }

    public int getCoolantId() {
        return data.get(FuelEngineBlockEntity.DATA_COOLANT_ID);
    }

    public int getCoolantAmountMb() {
        return data.get(FuelEngineBlockEntity.DATA_COOLANT_AMOUNT);
    }

    public float getCoolantFillFraction() {
        return fill(getCoolantAmountMb(), data.get(FuelEngineBlockEntity.DATA_COOLANT_CAPACITY));
    }

    public boolean isThermalShutdown() {
        return data.get(FuelEngineBlockEntity.DATA_SHUTDOWN) != 0;
    }

    public int getCommittedFuel() {
        return data.get(FuelEngineBlockEntity.DATA_COMMITTED_FUEL);
    }

    public int getCommittedCooling() {
        return data.get(FuelEngineBlockEntity.DATA_COMMITTED_COOLING);
    }

    /** Remaining committed fuel as a fraction (1 at commit, 0 when spent) for the burn flame. */
    public float getFuelBurnFraction() {
        return data.get(FuelEngineBlockEntity.DATA_FUEL_BURN) / 1000f;
    }

    public float getTemperatureFraction() {
        int max = getMaxTemperature();
        return max <= 0 ? 0f : Math.min(1f, getTemperature() / (float) max);
    }

    private static float fill(int amountMb, int capacityMb) {
        if (amountMb <= 0 || capacityMb <= 0) {
            return 0f;
        }
        return Math.min(1f, amountMb / (float) capacityMb);
    }
}
