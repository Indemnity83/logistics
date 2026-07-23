package com.logistics.core.lib.power.gen;

import com.logistics.core.machine.MachineContext;

/**
 * Generates a fixed amount of RF per burn tick, capped by the buffer's remaining room. The simplest
 * {@link Generation} strategy, suitable for engines with a flat output rate.
 */
public final class FixedGeneration implements Generation {

    private final long rfPerTick;

    public FixedGeneration(long rfPerTick) {
        this.rfPerTick = Math.max(0, rfPerTick);
    }

    @Override
    public long generate(MachineContext ctx, double temperature, long stored, long capacity) {
        long room = Math.max(0, capacity - stored);
        return Math.min(rfPerTick, room);
    }
}
