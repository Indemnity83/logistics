package com.logistics.core.fabricator;

import com.logistics.LogisticsCore;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Screen handler for the Kiln GUI.
 * Manages inventory slots and syncs data between client and server.
 */
public class KilnScreenHandler extends AbstractContainerMenu {

    private static final int FUEL_SLOT = 0;
    private static final int GLASS_SLOT = 1;
    private static final int GRID_START = 2;
    private static final int GRID_END = 10;
    private static final int OUTPUT_SLOT = 11;
    private static final int TOTAL_SLOTS = 12;

    private static final int PLAYER_INVENTORY_START = 12;
    private static final int PLAYER_INVENTORY_END = 48;

    private final Container inventory;
    private final ContainerData data;

    // Data indices for syncing (updated for energy-based system)
    private static final int TEMPERATURE = 0;
    private static final int GLASS_AMOUNT_LOW = 1;
    private static final int GLASS_AMOUNT_HIGH = 2;
    private static final int MELT_PROGRESS = 3;
    private static final int ANNEAL_PROGRESS = 4;
    private static final int BURNING_FLAG = 5;
    private static final int DATA_COUNT = 6;

    /**
     * Client-side constructor.
     */
    public KilnScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(TOTAL_SLOTS), new SimpleContainerData(DATA_COUNT));
    }

    /**
     * Server-side constructor.
     */
    public KilnScreenHandler(int syncId, Inventory playerInventory, Container inventory, ContainerData data) {
        super(LogisticsCore.MENU.KILN, syncId);
        checkContainerSize(inventory, TOTAL_SLOTS);
        checkContainerDataCount(data, DATA_COUNT);

        this.inventory = inventory;
        this.data = data;

        inventory.startOpen(playerInventory.player);

        // Fuel slot (0)
        this.addSlot(new Slot(inventory, FUEL_SLOT, 19, 53));

        // Glass input slot (1) - x=46-61, y=17-32 means slot at (46, 17)
        this.addSlot(new Slot(inventory, GLASS_SLOT, 46, 17));

        // 3x3 crafting grid (2-10)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new Slot(inventory, GRID_START + row * 3 + col,
                    72 + col * 18, 17 + row * 18));
            }
        }

        // Output slot (11)
        this.addSlot(new Slot(inventory, OUTPUT_SLOT, 141, 49) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // Output only
            }
        });

        // Player inventory (3 rows of 9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                    8 + col * 18, 84 + row * 18));
            }
        }

        // Player hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col,
                8 + col * 18, 142));
        }

        this.addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index == OUTPUT_SLOT) {
                // Extract from output
                if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, result);
            } else if (index >= PLAYER_INVENTORY_START) {
                // From player inventory to machine
                // Try fuel slot first if item is fuel
                if (player.level().fuelValues().burnDuration(stack) > 0) {
                    if (!this.moveItemStackTo(stack, FUEL_SLOT, FUEL_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                // Try glass slot if it's glass/sand/pane
                else if (isGlassItem(stack)) {
                    if (!this.moveItemStackTo(stack, GLASS_SLOT, GLASS_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                // Otherwise try grid
                else if (!this.moveItemStackTo(stack, GRID_START, GRID_END + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From machine to player inventory
                if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stack);
        }

        return result;
    }

    private boolean isGlassItem(ItemStack stack) {
        return stack.is(Items.GLASS)
            || stack.is(Items.SAND)
            || stack.is(Items.GLASS_PANE);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.inventory.stillValid(player);
    }

    // Getters for GUI rendering

    public int getTemperature() {
        return this.data.get(TEMPERATURE);
    }

    public boolean isBurning() {
        return this.data.get(BURNING_FLAG) > 0;
    }

    public int getMeltProgress() {
        return this.data.get(MELT_PROGRESS);
    }

    public int getAnnealProgress() {
        return this.data.get(ANNEAL_PROGRESS);
    }

    public long getGlassAmount() {
        int low = this.data.get(GLASS_AMOUNT_LOW);
        int high = this.data.get(GLASS_AMOUNT_HIGH);
        return ((long) high << 32) | (low & 0xFFFFFFFFL);
    }
}
