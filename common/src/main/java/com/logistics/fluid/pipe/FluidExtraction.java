package com.logistics.fluid.pipe;

import java.util.List;

/**
 * Pure per-tick extraction policy for an extractor pipe — the decision logic that used to live in the block
 * entity, lifted out so it is unit-testable without a running game.
 *
 * <p>Energy buys fluid at {@link #MB_PER_RF} mB per RF (matching BuildCraft's wooden fluid pipe): the per-tick
 * allowance is the configured transfer {@code rate} clamped to what the available {@code energy} can pay for.
 * Fluid is pulled from the ordered {@code sources} until that total allowance is met. Energy is charged in
 * proportion to the fluid actually moved; since a single tick usually moves less than one RF's worth, the
 * sub-RF remainder is carried ({@code carryMb}) into the next tick so the buffer still drains gradually instead
 * of rounding the per-tick cost down to zero.
 */
public final class FluidExtraction {

    /** Millibuckets moved per unit of stored energy (RF). Higher = cheaper extraction for weak engines. */
    public static final long MB_PER_RF = 50;

    private FluidExtraction() {}

    /**
     * The outcome of one extraction tick.
     *
     * @param extractedMb     total fluid moved into the pipe this tick, in millibuckets
     * @param energyToConsume whole RF the caller should burn this tick (extraction charged at {@link #MB_PER_RF})
     * @param carryMb         the sub-RF fluid remainder to feed back in as {@code carryMb} next tick
     */
    public record Result(long extractedMb, long energyToConsume, long carryMb) {}

    /**
     * Runs one extraction tick, mutating {@code pipe} as fluid is pulled in.
     *
     * @param pipe           the extractor pipe's storage (mutated)
     * @param sources        adjacent fluid providers, drained in order
     * @param energy         stored energy available this tick (buys {@link #MB_PER_RF} mB per RF)
     * @param rate           the per-tick transfer rate, in millibuckets
     * @param requiresEngine whether extraction must be paid for with energy; when {@code false}, the pipe
     *                       moves at the full rate and burns nothing
     * @param carryMb        sub-RF fluid remainder left unpaid from prior ticks (from the previous {@link Result})
     */
    public static <F> Result tick(
            FluidPipe<F> pipe,
            List<FluidProvider<F>> sources,
            long energy,
            long rate,
            boolean requiresEngine,
            long carryMb) {
        long allowance = requiresEngine ? Math.min(rate, energy * MB_PER_RF) : rate;
        if (allowance <= 0) {
            return new Result(0, 0, carryMb);
        }

        long extracted = 0;
        for (FluidProvider<F> source : sources) {
            if (extracted >= allowance) {
                break;
            }
            extracted += pipe.extract(source, allowance - extracted);
        }

        if (!requiresEngine || extracted <= 0) {
            return new Result(extracted, 0, carryMb);
        }
        // Charge whole RF for the fluid moved (plus any carried remainder); keep the leftover sub-RF for next tick.
        long pool = carryMb + extracted;
        long rf = pool / MB_PER_RF;
        return new Result(extracted, rf, pool - rf * MB_PER_RF);
    }
}
