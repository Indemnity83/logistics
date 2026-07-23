package com.logistics.power.engine.ui;

import com.logistics.LogisticsPower;
import com.logistics.core.lib.power.HeatStage;
import com.logistics.power.engine.block.entity.MagmaticEngineBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Screen handler for the Magmatic Engine GUI. The engine has no item slots (lava arrives by fluid pipe),
 * so this is a display container: the player inventory plus a {@link ContainerData} carrying the RF buffer,
 * attempted/accepted/wasted generation, burn progress, thermal stage + temperature, the lava tank, and the
 * powered/lit flags the screen renders as a status line.
 */
public class MagmaticEngineScreenHandler extends AbstractContainerMenu {

    private static final int SLOT_SIZE = 18;
    private static final int SLOT_START_X = 8;
    private static final int PLAYER_INV_START_Y = 84;
    private static final int HOTBAR_Y = 142;

    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** Client-side constructor. */
    public MagmaticEngineScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainerData(MagmaticEngineBlockEntity.DATA_COUNT),
                ContainerLevelAccess.NULL);
    }

    /** Server-side constructor. */
    public MagmaticEngineScreenHandler(
            int syncId, Inventory playerInventory, ContainerData data, ContainerLevelAccess access) {
        super(LogisticsPower.SCREEN.MAGMATIC_ENGINE, syncId);
        checkContainerDataCount(data, MagmaticEngineBlockEntity.DATA_COUNT);
        this.data = data;
        this.access = access;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        SLOT_START_X + col * SLOT_SIZE, PLAYER_INV_START_Y + row * SLOT_SIZE));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, SLOT_START_X + col * SLOT_SIZE, HOTBAR_Y));
        }

        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // no machine slots to shuffle into
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(access, player, LogisticsPower.BLOCK.MAGMATIC_ENGINE);
    }

    // ==================== Data getters for GUI rendering ====================

    public int getEnergyBarHeight(int maxPixels) {
        return data.get(MagmaticEngineBlockEntity.DATA_ENERGY) * maxPixels / 10_000;
    }

    public int getAttempted() {
        return data.get(MagmaticEngineBlockEntity.DATA_ATTEMPTED);
    }

    public int getAccepted() {
        return data.get(MagmaticEngineBlockEntity.DATA_ACCEPTED);
    }

    public int getWasted() {
        return data.get(MagmaticEngineBlockEntity.DATA_WASTED);
    }

    /** Burn progress that fills while burning (0 at ignition, 1 as the batch ends). */
    public float getBurnProgress() {
        int total = data.get(MagmaticEngineBlockEntity.DATA_BURN_TOTAL);
        if (total <= 0) {
            return 0f;
        }
        int remaining = data.get(MagmaticEngineBlockEntity.DATA_BURN_REMAINING);
        return Math.clamp(1f - remaining / (float) total, 0f, 1f);
    }

    public int getBurnRemaining() {
        return data.get(MagmaticEngineBlockEntity.DATA_BURN_REMAINING);
    }

    public HeatStage getStage() {
        return HeatStage.fromOrdinal(data.get(MagmaticEngineBlockEntity.DATA_STAGE));
    }

    public int getTemperature() {
        return data.get(MagmaticEngineBlockEntity.DATA_TEMPERATURE);
    }

    public int getLavaId() {
        return data.get(MagmaticEngineBlockEntity.DATA_LAVA_ID);
    }

    public int getLavaAmountMb() {
        return data.get(MagmaticEngineBlockEntity.DATA_LAVA_AMOUNT);
    }

    public float getLavaFillFraction() {
        int amount = getLavaAmountMb();
        int capacity = data.get(MagmaticEngineBlockEntity.DATA_LAVA_CAPACITY);
        if (amount <= 0 || capacity <= 0) {
            return 0f;
        }
        return Math.min(1f, amount / (float) capacity);
    }

    public boolean isPowered() {
        return data.get(MagmaticEngineBlockEntity.DATA_POWERED) != 0;
    }

    public boolean isLit() {
        return data.get(MagmaticEngineBlockEntity.DATA_LIT) != 0;
    }
}
