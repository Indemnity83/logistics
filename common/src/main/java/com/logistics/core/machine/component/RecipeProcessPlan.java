package com.logistics.core.machine.component;

/**
 * Pure RF-cost processing math, extracted so it can be unit-tested without a live {@code Level}.
 * The machine spends a fixed {@code rfPerTick} (all-or-nothing) each tick toward the recipe's
 * total {@code energyRequired}; progress is the energy spent so far.
 */
public final class RecipeProcessPlan {

    private RecipeProcessPlan() {}

    public static Result advance(
            long energySpent, long energyRequired, long energyStored, long rfPerTick, boolean outputAccepted) {
        if (rfPerTick <= 0 || !outputAccepted || energyStored < rfPerTick) {
            return new Result(energySpent, false, false, false);
        }
        long next = energySpent + rfPerTick;
        return new Result(next, true, true, next >= energyRequired);
    }

    public record Result(long energySpent, boolean consumedEnergy, boolean lit, boolean complete) {}
}
