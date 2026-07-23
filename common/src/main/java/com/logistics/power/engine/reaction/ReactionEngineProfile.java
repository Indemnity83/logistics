package com.logistics.power.engine.reaction;

/**
 * Centralized, tunable balance profile for the Reaction Engine — the single place the simulation reads its
 * numbers from. Deliberately the smallest engine profile in the mod: no heat/coolant/pressure knobs.
 *
 * <p>{@code outputPerTick} and {@code reactionDurationTicks} are <b>independent</b> axes — output rate and
 * how long a burst lasts are separate concerns; a future reaction may run longer rather than harder. Both
 * are the per-reaction defaults; a {@link ReactionRecipe} may override either.
 */
public record ReactionEngineProfile(
        long outputPerTick,
        int reactionDurationTicks,
        int reactantTankCapacityMb,
        int batchMb) {

    public static ReactionEngineProfile of(
            long outputPerTick, int reactionDurationTicks, int reactantTankCapacityMb, int batchMb) {
        return new ReactionEngineProfile(outputPerTick, reactionDurationTicks, reactantTankCapacityMb, batchMb);
    }

    /** Total RF a default reaction produces (attempted) across its full duration. */
    public long totalEnergyPerReaction() {
        return outputPerTick * (long) reactionDurationTicks;
    }
}
