package com.logistics.core.machine.tier;

import java.util.Optional;
import net.minecraft.network.chat.Component;

/** A machine block entity whose behaviour depends on a {@link MachineTier}. */
public interface TieredMachine {

    TierComponent tierComponent();

    default MachineTier tier() {
        return tierComponent().tier();
    }

    /**
     * Called once a kit has raised the machine to {@code newTier}; what to tell the player instead of
     * the plain "Upgraded to" message, if anything.
     */
    default Optional<Component> upgradeFeedback(MachineTier newTier) {
        return Optional.empty();
    }
}
