package com.logistics.power.engine.steam;

import com.logistics.core.machine.MachineContext;

/**
 * The Steam Engine's direct energy output seam: each tick the turbine offers up to {@code maxRf} to the
 * configured output face and is told how much the neighbor accepted. Injected (like {@code FuelSource})
 * so the simulation is unit-testable without a live level — the block entity supplies the real
 * implementation (which resolves the output face and pushes via {@code EngineEnergyPusher.pushGenerated}).
 */
@FunctionalInterface
public interface TurbineOutput {

    /**
     * Offer up to {@code maxRf} RF to the output-face neighbor.
     *
     * @return the accepted amount; implementations must satisfy {@code 0 <= accepted <= maxRf}.
     */
    long offer(MachineContext ctx, long maxRf);
}
