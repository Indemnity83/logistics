package com.logistics.forestry.farm;

import com.logistics.LogisticsForestry;
import com.logistics.core.machine.MachineData;
import com.logistics.core.machine.control.MachineControls;
import com.logistics.core.machine.tier.MachineTier;
import java.util.function.BooleanSupplier;
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
 * The menu every farm opens (176×166), in one of two {@link Layout}s. Machine slots come first, in
 * container order. A tend farm's are its {@value FarmMachine#TEND_SLOT_COUNT} generic slots, a 5×3
 * grid at x 80 + col×18 × y 17 + row×18, row by row. A ring farm's are:
 *
 * <ul>
 *   <li>0–3 germlings, at (42, 17), (60, 17), (78, 17), (96, 17);</li>
 *   <li>4–7 soil, at (42, 35) … (96, 35); always present so slot indices never shift, but
 *       {@linkplain Slot#isActive() inactive} (hidden, unclickable) unless the farm lays soil;</li>
 *   <li>8–16 output, take-only, a 3×3 grid at x 116/134/152 × y 17/35/53, row by row;</li>
 * </ul>
 *
 * <p>The player inventory follows at (8 + col×18, 84 + row×18), then the hotbar at y 142.
 *
 * <p>Synced {@link ContainerData} indices. A "wide" value spans two slots and is read with
 * {@link MachineData#wide}; the getters below decode everything.
 *
 * <ul>
 *   <li>{@link #ENERGY} (wide): stored RF;</li>
 *   <li>{@link #ENERGY_CAPACITY} (wide): RF capacity;</li>
 *   <li>{@link #STATUS}: {@link FarmStatus} ordinal;</li>
 *   <li>{@link #AREA_SIZE}: outer side of the worked ring (a 15 means 15×15);</li>
 *   <li>{@link #FLAGS}: bit {@link #FLAG_LAYS_SOIL};</li>
 *   <li>{@link #TIER}: {@link MachineTier#rank()} of the farm's tier;</li>
 *   <li>{@link #PLOTS_TOTAL}: plots in the worked ring;</li>
 *   <li>{@link #PLOTS_BLOCKED}: ring plots blocked on their most recent visit;</li>
 *   <li>{@link #STATE}: {@link FarmState} ordinal;</li>
 *   <li>{@link #SURVEY_CLEAR}: blocks the survey found the farm would clear;</li>
 *   <li>{@link #SURVEY_FLAGGED}: of those, blocks that look player-built;</li>
 *   <li>{@link #SURVEY_PROTECTED}: blocks it found the farm would skip.</li>
 * </ul>
 *
 * <p>A second data block, {@link #controls()}, carries the redstone control and energy telemetry the
 * side tabs show.
 *
 * <p>Buttons, through {@link #clickMenuButton}: {@link #BUTTON_START} starts a surveying farm,
 * {@link #BUTTON_SHOW_AREA} toggles the area preview; ids from {@link MachineControls#BUTTON_BASE}
 * up belong to the controls.
 */
public class FarmMenu extends AbstractContainerMenu {

    public static final int ENERGY = 0;
    public static final int ENERGY_CAPACITY = 2;
    public static final int STATUS = 4;
    public static final int AREA_SIZE = 5;
    public static final int FLAGS = 6;
    public static final int TIER = 7;
    public static final int PLOTS_TOTAL = 8;
    public static final int PLOTS_BLOCKED = 9;
    public static final int STATE = 10;
    public static final int SURVEY_CLEAR = 11;
    public static final int SURVEY_FLAGGED = 12;
    public static final int SURVEY_PROTECTED = 13;
    public static final int PREVIEW_SHOWN = 14;
    public static final int DATA_COUNT = 15;

    public static final int BUTTON_START = 0;
    public static final int BUTTON_SHOW_AREA = 1;

    public static final int FLAG_LAYS_SOIL = 1;

    private static final int TEND_COLUMNS = 5;
    private static final int TEND_LEFT = 80;

    /** Which farm's slots the menu shows. */
    public enum Layout {
        /** Germling, soil and output slots. */
        RING(FarmMachine.SLOT_COUNT),
        /** One generic inventory. */
        TEND(FarmMachine.TEND_SLOT_COUNT);

        private final int slots;

        Layout(int slots) {
            this.slots = slots;
        }

        /** Machine slots in this layout. */
        public int slots() {
            return slots;
        }

        /** The layout of a farm whose container has {@code size} slots. */
        public static Layout forSize(int size) {
            return size == TEND.slots ? TEND : RING;
        }
    }

    /** What the menu's farm buttons do to the farm; each returns whether it did anything. */
    public interface Buttons {
        /** Buttons that do nothing: the client side, or a menu without a farm. */
        Buttons NONE = new Buttons() {
            @Override
            public boolean start() {
                return false;
            }

            @Override
            public boolean toggleArea() {
                return false;
            }
        };

        boolean start();

        boolean toggleArea();
    }

    private final Layout layout;
    private final int playerStart;
    private final int playerEnd;
    private final Container container;
    private final ContainerData data;
    private final Buttons buttons;
    private final MachineControls controls;

    /** Client-side constructor of a ring farm's menu. */
    public FarmMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(FarmMachine.SLOT_COUNT), new SimpleContainerData(DATA_COUNT));
    }

    /** Client-side constructor of a tend farm's menu. */
    public static FarmMenu tend(int syncId, Inventory playerInventory) {
        return new FarmMenu(
                syncId,
                playerInventory,
                new SimpleContainer(FarmMachine.TEND_SLOT_COUNT),
                new SimpleContainerData(DATA_COUNT));
    }

    /** Server-side constructor without working buttons or a redstone control. */
    public FarmMenu(int syncId, Inventory playerInventory, Container container, ContainerData data) {
        this(syncId, playerInventory, container, data, Buttons.NONE, MachineControls.client());
    }

    /** Server-side constructor; the container's size picks the {@link Layout}. */
    public FarmMenu(
            int syncId,
            Inventory playerInventory,
            Container container,
            ContainerData data,
            Buttons buttons,
            MachineControls controls) {
        this(Layout.forSize(container.getContainerSize()), syncId, playerInventory, container, data, buttons, controls);
    }

    private FarmMenu(
            Layout layout,
            int syncId,
            Inventory playerInventory,
            Container container,
            ContainerData data,
            Buttons buttons,
            MachineControls controls) {
        super(layout == Layout.TEND ? LogisticsForestry.MENU.TEND_FARM : LogisticsForestry.MENU.FARM, syncId);
        checkContainerSize(container, layout.slots());
        checkContainerDataCount(data, DATA_COUNT);
        this.layout = layout;
        this.playerStart = layout.slots();
        this.playerEnd = playerStart + 36;
        this.container = container;
        this.data = data;
        this.buttons = buttons;
        this.controls = controls;

        container.startOpen(playerInventory.player);

        if (layout == Layout.TEND) {
            addTendSlots(container);
        } else {
            addRingSlots(container);
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

    private void addTendSlots(Container container) {
        for (int slot = 0; slot < FarmMachine.TEND_SLOT_COUNT; slot++) {
            int row = slot / TEND_COLUMNS;
            int col = slot % TEND_COLUMNS;
            addSlot(new Slot(container, slot, TEND_LEFT + col * 18, 17 + row * 18));
        }
    }

    private void addRingSlots(Container container) {
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
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new OutputSlot(container, FarmMachine.OUTPUT_START + row * 3 + col, 116 + col * 18, 17 + row * 18));
            }
        }
    }

    /**
     * Server-side data source over a farm's components and whether its area preview shows now; see
     * the class comment for the indices.
     */
    public static ContainerData dataSource(FarmMachine farm, BooleanSupplier previewShown) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case ENERGY, ENERGY + 1 -> MachineData.wideSlot(farm.energy().amount(), index - ENERGY);
                    case ENERGY_CAPACITY, ENERGY_CAPACITY + 1 ->
                        MachineData.wideSlot(farm.energy().capacity(), index - ENERGY_CAPACITY);
                    case STATUS -> farm.farm().status().ordinal();
                    case AREA_SIZE -> farm.farm().areaSize();
                    case FLAGS -> farm.logic() != null && farm.logic().laysSoil() ? FLAG_LAYS_SOIL : 0;
                    case TIER -> farm.tier().tier().rank();
                    case PLOTS_TOTAL -> farm.farm().plotsTotal();
                    case PLOTS_BLOCKED -> farm.farm().plotsBlocked();
                    case STATE -> farm.farm().state().ordinal();
                    case SURVEY_CLEAR -> clampToShort(farm.farm().survey().toClear());
                    case SURVEY_FLAGGED -> clampToShort(farm.farm().survey().flagged());
                    case SURVEY_PROTECTED -> clampToShort(farm.farm().survey().protectedCount());
                    case PREVIEW_SHOWN -> previewShown.getAsBoolean() ? 1 : 0;
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

    /** Synced data travels as shorts. */
    private static int clampToShort(int value) {
        return Math.min(value, Short.MAX_VALUE);
    }

    // ==================== Buttons ====================

    /** Redstone and energy status synced alongside this menu's own data. */
    public MachineControls controls() {
        return controls;
    }

    /** Button ids arrive unchecked from the client; an unknown one does nothing. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (controls.clickMenuButton(player, id)) {
            return true;
        }
        return switch (id) {
            case BUTTON_START -> buttons.start();
            case BUTTON_SHOW_AREA -> buttons.toggleArea();
            default -> false;
        };
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

        if (index < playerStart) {
            boolean fromOutput = layout == Layout.TEND || index >= FarmMachine.OUTPUT_START;
            if (!moveItemStackTo(stack, playerStart, playerEnd, fromOutput)) {
                return ItemStack.EMPTY;
            }
        } else if (layout == Layout.TEND) {
            if (!moveItemStackTo(stack, 0, playerStart, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Germlings first, then soil; each slot's filter decides what it takes.
            boolean moved = moveItemStackTo(stack, FarmMachine.GERMLING_START, FarmMachine.GERMLING_END, false);
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

    public Layout layout() {
        return layout;
    }

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

    public FarmState state() {
        int ordinal = data.get(STATE);
        FarmState[] states = FarmState.values();
        return ordinal >= 0 && ordinal < states.length ? states[ordinal] : FarmState.SURVEY;
    }

    /** Blocks the survey found the farm would clear. */
    public int surveyClear() {
        return data.get(SURVEY_CLEAR);
    }

    /** Of {@link #surveyClear()}, blocks that look player-built. */
    public int surveyFlagged() {
        return data.get(SURVEY_FLAGGED);
    }

    /** Blocks the survey found the farm would skip. */
    public int surveyProtected() {
        return data.get(SURVEY_PROTECTED);
    }

    /** Whether the area preview shows now: what {@link #BUTTON_SHOW_AREA} would hide. */
    public boolean previewShown() {
        return data.get(PREVIEW_SHOWN) != 0;
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
