package com.logistics.fluid.pipe;

/**
 * Pure per-tick extraction policy for an extractor pipe, kept free of Minecraft types so it is unit-testable
 * without a running game.
 *
 * <p>Energy buys fluid at {@link #MB_PER_RF} mB per RF (a balance choice in the spirit of BuildCraft's wooden
 * pipe): the per-tick allowance is the configured transfer {@code rate} clamped to what the {@code energy} can
 * pay for.
 * Fluid is pulled from the single {@code source} up to that allowance. Energy is charged in proportion to the
 * fluid actually moved; since a single tick usually moves less than one RF's worth, the sub-RF remainder is
 * carried ({@code carryMb}) into the next tick so the buffer still drains gradually instead of rounding the
 * per-tick cost down to zero.
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
     * @param source         the adjacent fluid provider to pull from (the wrench-selected pull face)
     * @param energy         stored energy available this tick (buys {@link #MB_PER_RF} mB per RF)
     * @param rate           the per-tick transfer rate, in millibuckets
     * @param requiresEngine whether extraction must be paid for with energy; when {@code false}, the pipe
     *                       moves at the full rate and burns nothing
     * @param carryMb        sub-RF fluid remainder left unpaid from prior ticks (from the previous {@link Result})
     */
    public static <F> Result tick(
            FluidPipe<F> pipe, FluidProvider<F> source, long energy, long rate, boolean requiresEngine, long carryMb) {
        long extracted = pipe.extract(source, allowance(rate, energy, requiresEngine));
        if (requiresEngine && extracted > 0) {
            return chargeEnergy(extracted, carryMb);
        }
        return new Result(extracted, 0, carryMb);
    }

    /** How much fluid (mB) may move this tick: the transfer rate, capped by what stored energy can pay for. */
    private static long allowance(long rate, long energy, boolean requiresEngine) {
        return requiresEngine ? Math.min(rate, energy * MB_PER_RF) : rate;
    }

    /** Charges whole RF for the fluid moved (plus any carried remainder), keeping the sub-RF leftover for next tick. */
    private static Result chargeEnergy(long extracted, long carryMb) {
        long pool = carryMb + extracted;
        long rf = pool / MB_PER_RF;
        return new Result(extracted, rf, pool - rf * MB_PER_RF);
    }
}
