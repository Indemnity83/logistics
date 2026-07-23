package com.logistics.power.engine.reaction;

import com.logistics.core.machine.MachineContext;

/**
 * The Reaction Engine's direct energy output seam: each reacting tick the engine offers up to
 * {@code amount} RF to the network and is told how much was accepted. Injected (like the Steam Engine's
 * {@code TurbineOutput}) so the simulation is unit-testable without a live level.
 *
 * <p><b>Contract (independent of any implementation):</b> {@code push} must attempt to transmit at most
 * {@code amount} RF <em>immediately</em>. It must not retain energy internally, accumulate partial output,
 * or perform delayed delivery — the Reaction Engine is bufferless, so anything the network can't take this
 * tick is discarded. The block entity supplies the real implementation (a transient conduit bridging to
 * {@code EngineEnergyPusher.push}).
 */
@FunctionalInterface
public interface ReactionOutput {

    /**
     * Attempt to push up to {@code amount} RF to the output-face network this tick.
     *
     * @return the accepted amount; implementations must satisfy {@code 0 <= accepted <= amount}.
     */
    long push(MachineContext ctx, long amount);
}
