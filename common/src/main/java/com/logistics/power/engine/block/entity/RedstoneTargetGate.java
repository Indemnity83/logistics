package com.logistics.power.engine.block.entity;

import com.logistics.core.lib.power.AcceptsLowTierEnergy;
import net.minecraft.core.Direction;

final class RedstoneTargetGate {
    private RedstoneTargetGate() {}

    static boolean acceptsLowTierEnergy(Object target, Direction outputDirection) {
        if (!(target instanceof AcceptsLowTierEnergy acceptor)) {
            return false;
        }

        return acceptor.acceptsLowTierEnergyFrom(outputDirection.getOpposite());
    }
}
