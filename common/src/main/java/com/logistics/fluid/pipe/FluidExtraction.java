package com.logistics.fluid.pipe;

import java.util.List;

/**
 * Pure per-tick extraction policy for an extractor pipe — the decision logic that used to live in the block
 * entity, lifted out so it is unit-testable without a running game.
 *
 * <p>Energy is modelled as a plain millibucket-equivalent count (1 RF = 1 mB): the per-tick allowance is the
 * configured transfer {@code rate} clamped to the available {@code energy}. Fluid is pulled from the ordered
 * {@code sources} until that total allowance is met. To avoid coasting after an engine stops, the whole energy
 * buffer is reported for consumption on any tick that actually moved fluid — never banked.
 */
public final class FluidExtraction {

    private FluidExtraction() {}

    /**
     * The outcome of one extraction tick.
     *
     * @param extractedMb     total fluid moved into the pipe this tick, in millibuckets
     * @param energyToConsume how much stored energy the caller should burn (the whole buffer when fluid moved
     *                        and an engine is required; otherwise zero)
     */
    public record Result(long extractedMb, long energyToConsume) {}

    /**
     * Runs one extraction tick, mutating {@code pipe} as fluid is pulled in.
     *
     * @param pipe           the extractor pipe's storage (mutated)
     * @param sources        adjacent fluid providers, drained in order
     * @param energy         stored energy available this tick (1 RF = 1 mB)
     * @param rate           the per-tick transfer rate, in millibuckets
     * @param requiresEngine whether extraction must be paid for with energy; when {@code false}, the pipe
     *                       moves at the full rate and burns nothing
     */
    public static <F> Result tick(
            FluidPipe<F> pipe, List<FluidProvider<F>> sources, long energy, long rate, boolean requiresEngine) {
        long allowance = requiresEngine ? Math.min(rate, energy) : rate;
        if (allowance <= 0) {
            return new Result(0, 0);
        }

        long extracted = 0;
        for (FluidProvider<F> source : sources) {
            if (extracted >= allowance) {
                break;
            }
            extracted += pipe.extract(source, allowance - extracted);
        }

        long energyToConsume = (extracted > 0 && requiresEngine) ? energy : 0;
        return new Result(extracted, energyToConsume);
    }
}
