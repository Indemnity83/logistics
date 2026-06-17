package com.logistics.pipe.modules;

import com.logistics.core.LogisticsConfig;
import com.logistics.core.lib.pipe.Module;

/**
 * The base transport policy every fluid pipe composes: how fast it moves fluid and whether it connects
 * to (and prioritizes) external fluid handlers. The fluid pipe's transfer rate is a single configurable
 * base rate scaled by this per-tier multiplier (stone/extractor/void 1×, copper/bypass 2×, merger 3×,
 * gold 4×); capacity is a shared config value, not per-tier.
 */
public final class FluidTransportModule implements Module {

    private final int rateMultiplier;
    private final boolean connectsToHandlers;
    private final boolean prioritizesHandlers;

    public FluidTransportModule(int rateMultiplier, boolean connectsToHandlers, boolean prioritizesHandlers) {
        this.rateMultiplier = rateMultiplier;
        this.connectsToHandlers = connectsToHandlers;
        this.prioritizesHandlers = prioritizesHandlers;
    }

    /** This pipe's transfer rate in mB/tick — the configurable base rate scaled by the per-tier multiplier. */
    public long transferRate(LogisticsConfig.FluidPipeConfig cfg) {
        return (long) cfg.baseTransferRate * rateMultiplier;
    }

    /** Whether this pipe connects to external (non-pipe) fluid handlers. False for void and bypass. */
    public boolean connectsToHandlers() {
        return connectsToHandlers;
    }

    /** True for the insertion pipe: fill adjacent tanks before spilling to other pipes. */
    public boolean prioritizesHandlers() {
        return prioritizesHandlers;
    }
}
