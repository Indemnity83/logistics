package com.logistics.forestry.farm;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsForestry;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.machine.MachineData;
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
 * The menu every farm opens (176×166). Machine slots come first, in container order:
 *
 * <ul>
 *   <li>0–3 germlings, at (42, 17), (60, 17), (78, 17), (96, 17);</li>
 *   <li>4–7 soil, at (42, 35) … (96, 35); always present so slot indices never shift, but
 *       {@linkplain Slot#isActive() inactive} (hidden, unclickable) unless the farm lays soil;</li>
 *   <li>8 fertilizer, at (42, 53);</li>
 *   <li>9–17 output, take-only, a 3×3 grid at x 116/134/152 × y 17/35/53, row by row;</li>
 *   <li>18–44 player inventory at (8 + col×18, 84 + row×18), then 45–53 hotbar at y 142.</li>
 * </ul>
 *
 * <p>Synced {@link ContainerData} indices. A "wide" value spans two slots and is read with
 * {@link MachineData#wide}; the getters below decode everything.
 *
 * <ul>
 *   <li>{@link #ENERGY} (wide): stored RF;</li>
 *   <li>{@link #ENERGY_CAPACITY} (wide): RF capacity;</li>
 *   <li>{@link #WATER} (wide): water in mB;</li>
 *   <li>{@link #WATER_CAPACITY} (wide): tank capacity in mB, 0 for a farm without water;</li>
 *   <li>{@link #FERTILIZER_CHARGES}: banked fertilizer charges;</li>
 *   <li>{@link #STATUS}: {@link FarmStatus} ordinal;</li>
 *   <li>{@link #AREA_SIZE}: plot width (a 9 means 9×9);</li>
 *   <li>{@link #FLAGS}: bit {@link #FLAG_USES_WATER}, bit {@link #FLAG_LAYS_SOIL};</li>
 *   <li>{@link #FERTILIZER_CHARGE_CAPACITY}: most charges one fertilizer item can bank, for a charge bar.</li>
 * </ul>
 */
public class FarmMenu extends AbstractContainerMenu {

    public static final int ENERGY = 0;
    public static final int ENERGY_CAPACITY = 2;
    public static final int WATER = 4;
    public static final int WATER_CAPACITY = 6;
    public static final int FERTILIZER_CHARGES = 8;
    public static final int STATUS = 9;
    public static final int AREA_SIZE = 10;
    public static final int FLAGS = 11;
    public static final int FERTILIZER_CHARGE_CAPACITY = 12;
    public static final int DATA_COUNT = 13;

    public static final int FLAG_USES_WATER = 1;
    public static final int FLAG_LAYS_SOIL = 1 << 1;

    private static final int PLAYER_START = FarmMachine.SLOT_COUNT;
    private static final int PLAYER_END = PLAYER_START + 36;

    private final Container container;
    private final ContainerData data;

    /** Client-side constructor. */
    public FarmMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(FarmMachine.SLOT_COUNT), new SimpleContainerData(DATA_COUNT));
    }

    /** Server-side constructor. */
    public FarmMenu(int syncId, Inventory playerInventory, Container container, ContainerData data) {
        super(LogisticsForestry.MENU.FARM, syncId);
        checkContainerSize(container, FarmMachine.SLOT_COUNT);
        checkContainerDataCount(data, DATA_COUNT);
        this.container = container;
        this.data = data;

        container.startOpen(playerInventory.player);

        for (int i = 0; i < 4; i++) {
            addSlot(new InputSlot(container, FarmMachine.GERMLING_START + i, 42 + i * 18, 17));
        }
        for (int i = 0; i < 4; i++) {
            addSlot(new InputSlot(container, FarmMachine.SOIL_START + i, 42 + i * 18, 35) {
                @Override
                public boolean isActive() {
                    return laysSoil();
                }
            });
        }
        addSlot(new InputSlot(container, FarmMachine.FERTILIZER_SLOT, 42, 53));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new OutputSlot(container, FarmMachine.OUTPUT_START + row * 3 + col, 116 + col * 18, 17 + row * 18));
            }
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
                    case WATER, WATER + 1 -> MachineData.wideSlot(waterMb(farm), index - WATER);
                    case WATER_CAPACITY, WATER_CAPACITY + 1 ->
                        MachineData.wideSlot(waterCapacityMb(farm), index - WATER_CAPACITY);
                    case FERTILIZER_CHARGES -> Math.min(farm.farm().charges(), Short.MAX_VALUE);
                    case STATUS -> farm.farm().status().ordinal();
                    case AREA_SIZE -> farm.farm().areaSize();
                    case FLAGS -> (farm.logic().usesWater() ? FLAG_USES_WATER : 0)
                            | (farm.logic().laysSoil() ? FLAG_LAYS_SOIL : 0);
                    case FERTILIZER_CHARGE_CAPACITY -> Math.min(fertilizerChargeCapacity(farm), Short.MAX_VALUE);
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

    private static long waterMb(FarmMachine farm) {
        return farm.water() == null ? 0 : FluidUnits.toMillibuckets(farm.water().tank().getAmount());
    }

    private static long waterCapacityMb(FarmMachine farm) {
        return farm.water() == null ? 0 : FluidUnits.toMillibuckets(farm.water().tank().getCapacity());
    }

    private static int fertilizerChargeCapacity(FarmMachine farm) {
        int perItem = Math.max(
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FERTILIZER_CHARGES),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.BONE_MEAL_CHARGES));
        return Math.max(perItem, farm.farm().charges());
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

        if (index < PLAYER_START) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, index >= FarmMachine.OUTPUT_START)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Fertilizer first, then germlings, then soil; each slot's filter decides what it takes.
            boolean moved = moveItemStackTo(stack, FarmMachine.FERTILIZER_SLOT, FarmMachine.FERTILIZER_SLOT + 1, false);
            if (!stack.isEmpty()) {
                moved |= moveItemStackTo(stack, FarmMachine.GERMLING_START, FarmMachine.GERMLING_END, false);
            }
            if (!stack.isEmpty() && laysSoil()) {
                moved |= moveItemStackTo(stack, FarmMachine.SOIL_START, FarmMachine.SOIL_END, false);
            }
            if (!moved) {
                return ItemStack.EMPTY;
            }
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

    public int waterMb() {
        return MachineData.wide(data, WATER);
    }

    public int waterCapacityMb() {
        return MachineData.wide(data, WATER_CAPACITY);
    }

    public int fertilizerCharges() {
        return data.get(FERTILIZER_CHARGES);
    }

    public int fertilizerChargeCapacity() {
        return data.get(FERTILIZER_CHARGE_CAPACITY);
    }

    public FarmStatus status() {
        return FarmStatus.byOrdinal(data.get(STATUS));
    }

    public int areaSize() {
        return data.get(AREA_SIZE);
    }

    public boolean usesWater() {
        return (data.get(FLAGS) & FLAG_USES_WATER) != 0;
    }

    public boolean laysSoil() {
        return (data.get(FLAGS) & FLAG_LAYS_SOIL) != 0;
    }

    /** An input slot that takes only what the farm's per-slot filter allows. */
    private static class InputSlot extends Slot {
        InputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isActive() && container.canPlaceItem(getContainerSlot(), stack);
        }
    }

    /** Produce: take-only. */
    private static final class OutputSlot extends Slot {
        OutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
