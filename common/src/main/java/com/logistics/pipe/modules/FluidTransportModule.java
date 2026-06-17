package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.FluidPipeBehavior;
import com.logistics.core.lib.pipe.Module;

/**
 * Scales a fluid pipe's transfer rate by a named tier on top of the configured base rate. Pure rate
 * policy — connection and priority are separate modules.
 */
public final class FluidTransportModule implements Module, FluidPipeBehavior {

    /** Transfer-rate tier: the multiplier applied to the configured base rate. */
    public enum FlowRate {
        SLOW(1),
        NORMAL(2),
        FAST(4);

        private final int multiplier;

        FlowRate(int multiplier) {
            this.multiplier = multiplier;
        }

        public int multiplier() {
            return multiplier;
        }
    }

    private final FlowRate rate;

    public FluidTransportModule(FlowRate rate) {
        this.rate = rate;
    }

    @Override
    public long modifyTransferRate(long currentRate) {
        return currentRate * rate.multiplier();
    }
}
