package com.logistics.core.lib.power.gen;

import com.logistics.core.machine.MachineContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/**
 * Strategy for how much RF an engine generates on a given burn tick. Encapsulates its own tuning
 * (output range, control curve) so the burn component stays generic: a fixed-rate engine returns a
 * constant, while the Stirling Engine's {@code PidGeneration} ramps output toward a target
 * temperature.
 */
public interface Generation {

    /**
     * @param ctx the hosting engine context
     * @param temperature current engine temperature
     * @param stored current buffer energy in RF
     * @param capacity buffer capacity in RF
     * @return RF to add to the buffer this tick ({@code >= 0})
     */
    long generate(MachineContext ctx, double temperature, long stored, long capacity);

    /** Persist any generation state (e.g. PID integral and fractional carryover). */
    default void save(CompoundTag tag, HolderLookup.Provider registries) {}

    /** Restore state written by {@link #save}. */
    default void load(CompoundTag tag, HolderLookup.Provider registries) {}

    /** Reset transient control state when the engine shuts down. */
    default void reset() {}
}
