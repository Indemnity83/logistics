package com.logistics.core.machine.tier;

/** A machine block entity whose behaviour depends on a {@link MachineTier}. */
public interface TieredMachine {

    TierComponent tierComponent();

    default MachineTier tier() {
        return tierComponent().tier();
    }
}
