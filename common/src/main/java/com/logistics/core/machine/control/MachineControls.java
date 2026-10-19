package com.logistics.core.machine.control;

import com.logistics.core.machine.MachineData;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import org.jetbrains.annotations.Nullable;

/**
 * A menu's machine-controls block: a second {@link ContainerData} of {@link #COUNT} slots, independent
 * of the menu's own data, carrying redstone state and energy telemetry, plus the menu-button protocol
 * that changes the redstone mode.
 *
 * <p>Layout: slot {@link #STATUS} is a packed word (bits 0-1 the {@link RedstoneMode#syncId()}, then the
 * {@link #SIGNAL_BIT signal}, {@link #ENABLED_BIT enabled}, {@link #SUPPORTED_BIT supported} and
 * {@link #BUFFER_BIT has-buffer} flags); slots 1-8 are four {@link MachineData#wideSlot} pairs. The
 * server instance reads its {@link MachineStatus} live on every {@code get}; the client instance is a
 * plain {@link SimpleContainerData} filled by sync, all zero (unsupported) until the first sync.
 *
 * <p>Buttons {@link #BUTTON_BASE}..{@code BUTTON_BASE + 27} are reserved; redstone uses
 * {@code BUTTON_BASE + syncId}.
 */
public final class MachineControls implements EnergyView, RedstoneView {

    /** Slots in {@link #data()}. */
    public static final int COUNT = 9;
    /** First reserved menu-button id. */
    public static final int BUTTON_BASE = 100;
    /** Reserved menu-button ids, starting at {@link #BUTTON_BASE}. */
    public static final int BUTTON_RANGE = 28;

    static final int STATUS = 0;
    static final int STORED = 1;
    static final int CAPACITY = STORED + MachineData.WIDE_SLOTS;
    static final int RATE = CAPACITY + MachineData.WIDE_SLOTS;
    static final int MAX_RATE = RATE + MachineData.WIDE_SLOTS;

    static final int MODE_MASK = 0b11;
    static final int SIGNAL_BIT = 1 << 2;
    static final int ENABLED_BIT = 1 << 3;
    static final int SUPPORTED_BIT = 1 << 4;
    static final int BUFFER_BIT = 1 << 5;

    @Nullable
    private final MachineStatus status;
    private final ContainerData data;

    private MachineControls(@Nullable MachineStatus status, ContainerData data) {
        this.status = status;
        this.data = data;
    }

    /** The authoritative instance for a server-side menu, reading {@code status} live. */
    public static MachineControls server(MachineStatus status) {
        return new MachineControls(status, new StatusData(status));
    }

    /** The instance for a client-side menu, filled by container sync. */
    public static MachineControls client() {
        return new MachineControls(null, new SimpleContainerData(COUNT));
    }

    /** The button id that selects {@code mode}. */
    public static int redstoneButton(RedstoneMode mode) {
        return BUTTON_BASE + mode.syncId();
    }

    /** The data slots to register with {@code addDataSlots}. */
    public ContainerData data() {
        return data;
    }

    /**
     * Handles a menu-button click. Ids outside the reserved range return false so the menu can handle
     * them itself.
     *
     * <p>On the client this only validates the click (a supported control, a known mode that differs
     * from the current one) and never changes state; the caller sends the click to the server when it
     * returns true. On the server it rejects spectators, unknown ids, hosts without a redstone control
     * and removed hosts without changing anything, and otherwise applies the mode.
     */
    public boolean clickMenuButton(Player player, int id) {
        return click(() -> player.isSpectator(), id);
    }

    /** {@link #clickMenuButton} with the spectator check deferred, so the client path never reads it. */
    boolean click(BooleanSupplier spectator, int id) {
        if (id < BUTTON_BASE || id >= BUTTON_BASE + BUTTON_RANGE) {
            return false;
        }
        Optional<RedstoneMode> requested = RedstoneMode.bySyncId(id - BUTTON_BASE);
        if (requested.isEmpty()) {
            return false;
        }
        if (status == null) {
            return supported() && requested.get() != mode();
        }
        if (spectator.getAsBoolean() || status.isRemoved() || status.redstoneControl() == null) {
            return false;
        }
        status.setRedstoneMode(requested.get());
        return true;
    }

    // ==================== RedstoneView ====================

    @Override
    public boolean supported() {
        return (data.get(STATUS) & SUPPORTED_BIT) != 0;
    }

    /** The synced mode; {@link RedstoneMode#IGNORED} for an unknown sync id. */
    @Override
    public RedstoneMode mode() {
        return RedstoneMode.bySyncId(data.get(STATUS) & MODE_MASK).orElse(RedstoneMode.IGNORED);
    }

    @Override
    public boolean signal() {
        return (data.get(STATUS) & SIGNAL_BIT) != 0;
    }

    @Override
    public boolean enabled() {
        return (data.get(STATUS) & ENABLED_BIT) != 0;
    }

    // ==================== EnergyView ====================

    @Override
    public boolean hasBuffer() {
        return (data.get(STATUS) & BUFFER_BIT) != 0;
    }

    @Override
    public long stored() {
        return MachineData.wide(data, STORED);
    }

    @Override
    public long capacity() {
        return MachineData.wide(data, CAPACITY);
    }

    @Override
    public long rate() {
        return MachineData.wide(data, RATE);
    }

    @Override
    public long maxRate() {
        return MachineData.wide(data, MAX_RATE);
    }

    // ==================== Packing ====================

    /** The {@link #STATUS} word for the given state. */
    static int pack(RedstoneMode mode, boolean signal, boolean enabled, boolean supported, boolean hasBuffer) {
        int word = mode.syncId() & MODE_MASK;
        if (signal) {
            word |= SIGNAL_BIT;
        }
        if (enabled) {
            word |= ENABLED_BIT;
        }
        if (supported) {
            word |= SUPPORTED_BIT;
        }
        if (hasBuffer) {
            word |= BUFFER_BIT;
        }
        return word;
    }

    /** Server-side data reading a {@link MachineStatus} live. */
    private static final class StatusData implements ContainerData {
        private final MachineStatus status;

        StatusData(MachineStatus status) {
            this.status = status;
        }

        @Override
        public int get(int index) {
            return switch (index) {
                case STATUS -> {
                    RedstoneControlComponent redstone = status.redstoneControl();
                    yield pack(
                            redstone == null ? RedstoneMode.IGNORED : redstone.mode(),
                            status.hasRedstoneSignal(),
                            status.isEnabled(),
                            redstone != null,
                            status.hasEnergyBuffer());
                }
                case STORED, STORED + 1 -> MachineData.wideSlot(status.energyStored(), index - STORED);
                case CAPACITY, CAPACITY + 1 -> MachineData.wideSlot(status.energyCapacity(), index - CAPACITY);
                case RATE, RATE + 1 -> MachineData.wideSlot(status.energyRate(), index - RATE);
                case MAX_RATE, MAX_RATE + 1 -> MachineData.wideSlot(status.energyMaxRate(), index - MAX_RATE);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Server-side source only; the client uses a SimpleContainerData populated by sync.
        }

        @Override
        public int getCount() {
            return COUNT;
        }
    }
}
