package com.logistics.core.machine.control;

import org.jetbrains.annotations.Nullable;

/**
 * Server-side status a machine or engine exposes to its menu: redstone control state and energy
 * telemetry. Read live by {@link MachineControls#server} on every sync.
 */
public interface MachineStatus {

    /** The host's redstone control, or {@code null} when it has none. */
    @Nullable
    RedstoneControlComponent redstoneControl();

    /** The physical redstone signal the host reacts to. */
    boolean hasRedstoneSignal();

    /** Whether the host is permitted to work: its redstone mode applied to the signal. */
    boolean isEnabled();

    /** Sets the redstone mode and syncs it; a no-op without a control or when unchanged. */
    void setRedstoneMode(RedstoneMode mode);

    /** Whether the host block entity has been removed from the world. */
    boolean isRemoved();

    /** Whether the host has an RF buffer; when false, {@link #energyStored} and {@link #energyCapacity} are meaningless. */
    boolean hasEnergyBuffer();

    /** RF currently in the buffer. */
    long energyStored();

    /** RF buffer capacity. */
    long energyCapacity();

    /**
     * RF moved during the last completed tick: for a machine, the RF it spent on its own work; for an
     * engine, the RF it generated and retained (accepted into its buffer, or by its consumer).
     */
    long energyRate();

    /** The ceiling for {@link #energyRate}: a machine's maximum input, or an engine's maximum generation. */
    long energyMaxRate();
}
