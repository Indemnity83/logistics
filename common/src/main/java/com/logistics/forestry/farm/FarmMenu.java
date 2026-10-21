package com.logistics.forestry.farm;

import com.logistics.LogisticsForestry;
import com.logistics.core.machine.MachineData;
import com.logistics.core.machine.control.MachineControls;
import com.logistics.core.machine.tier.MachineTier;
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
 * The menu every farm opens (176×166). Machine slots come first, in container order: the farm's
 * {@value FarmMachine#SLOT_COUNT} generic slots, a 5×3 grid at x 80 + col×18 × y 17 + row×18, row by
 * row. The player inventory follows at (8 + col×18, 84 + row×18), then the hotbar at y 142.
 *
 * <p>Synced {@link ContainerData} indices. A "wide" value spans two slots and is read with
 * {@link MachineData#wide}; the getters below decode everything.
 *
 * <ul>
 *   <li>{@link #ENERGY} (wide): stored RF;</li>
 *   <li>{@link #ENERGY_CAPACITY} (wide): RF capacity;</li>
 *   <li>{@link #STATUS}: {@link FarmStatus} ordinal;</li>
 *   <li>{@link #AREA_SIZE}: side of the Reach (a 7 means 7×7);</li>
 *   <li>{@link #TIER}: {@link MachineTier#rank()} of the farm's tier;</li>
 *   <li>{@link #PLOTS_TOTAL}: the Plots the farm remembers;</li>
 *   <li>{@link #PLOTS_BLOCKED}: of those, the Empty Plots.</li>
 * </ul>
 *
 * <p>A second data block, {@link #controls()}, carries the redstone control and energy telemetry the
 * side tabs show; its buttons are the only ones the menu has.
 */
public class FarmMenu extends AbstractContainerMenu {

    public static final int ENERGY = 0;
    public static final int ENERGY_CAPACITY = 2;
    public static final int STATUS = 4;
    public static final int AREA_SIZE = 5;
    public static final int TIER = 6;
    public static final int PLOTS_TOTAL = 7;
    public static final int PLOTS_BLOCKED = 8;
    public static final int DATA_COUNT = 9;

    private static final int COLUMNS = 5;
    private static final int SLOTS_LEFT = 80;
    private static final int PLAYER_START = FarmMachine.SLOT_COUNT;
    private static final int PLAYER_END = PLAYER_START + 36;

    private final Container container;
    private final ContainerData data;
    private final MachineControls controls;

    /** Client-side constructor. */
    public FarmMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(FarmMachine.SLOT_COUNT), new SimpleContainerData(DATA_COUNT));
    }

    /** Server-side constructor without a redstone control. */
    public FarmMenu(int syncId, Inventory playerInventory, Container container, ContainerData data) {
        this(syncId, playerInventory, container, data, MachineControls.client());
    }

    public FarmMenu(
            int syncId, Inventory playerInventory, Container container, ContainerData data, MachineControls controls) {
        super(LogisticsForestry.MENU.FARM, syncId);
        checkContainerSize(container, FarmMachine.SLOT_COUNT);
        checkContainerDataCount(data, DATA_COUNT);
        this.container = container;
        this.data = data;
        this.controls = controls;

        container.startOpen(playerInventory.player);

        for (int slot = 0; slot < FarmMachine.SLOT_COUNT; slot++) {
            int row = slot / COLUMNS;
            int col = slot % COLUMNS;
            addSlot(new Slot(container, slot, SLOTS_LEFT + col * 18, 17 + row * 18));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        addDataSlots(data);
        checkContainerDataCount(controls.data(), MachineControls.COUNT);
        addDataSlots(controls.data());
    }

    /** Server-side data source over a farm's components; see the class comment for the indices. */
    public static ContainerData dataSource(FarmMachine farm) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case ENERGY, ENERGY + 1 -> MachineData.wideSlot(farm.energy().amount(), index - ENERGY);
                    case ENERGY_CAPACITY, ENERGY_CAPACITY + 1 ->
                        MachineData.wideSlot(farm.energy().capacity(), index - ENERGY_CAPACITY);
                    case STATUS -> farm.farm().status().ordinal();
                    case AREA_SIZE -> farm.farm().areaSize();
                    case TIER -> farm.tier().tier().rank();
                    case PLOTS_TOTAL -> farm.farm().plotsTotal();
                    case PLOTS_BLOCKED -> farm.farm().plotsBlocked();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // Server-side source only; the client uses a SimpleContainerData populated by sync.
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    /** Redstone and energy status synced alongside this menu's own data. */
    public MachineControls controls() {
        return controls;
    }

    /** Button ids arrive unchecked from the client; an unknown one does nothing. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        return controls.clickMenuButton(player, id);
    }

    // ==================== Shift-click ====================

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        boolean moved = index < PLAYER_START
                ? moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)
                : moveItemStackTo(stack, 0, PLAYER_START, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    // ==================== Synced data ====================

    public long energy() {
        return MachineData.wide(data, ENERGY);
    }

    public long energyCapacity() {
        return MachineData.wide(data, ENERGY_CAPACITY);
    }

    public FarmStatus status() {
        return FarmStatus.byOrdinal(data.get(STATUS));
    }

    public int areaSize() {
        return data.get(AREA_SIZE);
    }

    public MachineTier tier() {
        return MachineTier.byRank(data.get(TIER)).orElse(MachineTier.BASIC);
    }

    public int plotsTotal() {
        return data.get(PLOTS_TOTAL);
    }

    public int plotsBlocked() {
        return data.get(PLOTS_BLOCKED);
    }

    public int plotsUsable() {
        return Math.max(0, plotsTotal() - plotsBlocked());
    }
}
