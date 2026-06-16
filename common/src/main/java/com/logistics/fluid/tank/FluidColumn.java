package com.logistics.fluid.tank;

/**
 * Pure fluid-distribution math for a vertical column of tanks that all hold the same fluid.
 *
 * <p>A column is described by per-tank capacities ordered <em>bottom-to-top</em> plus a single
 * aggregate amount — every tank in a connected column holds one fluid, so its contents are fully
 * described by a total. Liquids settle toward the bottom; gases rise toward the top. Amounts are in
 * platform-native fluid units. Holds no Minecraft types, so it is unit-testable without a game.
 */
public final class FluidColumn {

    private FluidColumn() {}

    /** Total capacity of the column (sum of per-tank capacities). */
    public static long totalCapacity(long[] capacities) {
        long sum = 0;
        for (long c : capacities) {
            sum += c;
        }
        return sum;
    }

    /**
     * Distributes {@code total} units across {@code capacities}, filling each tank to capacity in
     * settle order — bottom-up for liquids, top-down for gases — and returns the per-tank amounts
     * (always indexed bottom-to-top, matching {@code capacities}).
     *
     * @throws IllegalArgumentException if {@code total} is negative or exceeds the column capacity
     */
    public static long[] settle(long[] capacities, long total, boolean gas) {
        if (total < 0) {
            throw new IllegalArgumentException("total must be >= 0: " + total);
        }
        long capacity = totalCapacity(capacities);
        if (total > capacity) {
            throw new IllegalArgumentException("total " + total + " exceeds column capacity " + capacity);
        }
        int n = capacities.length;
        long[] out = new long[n];
        long remaining = total;
        for (int k = 0; k < n; k++) {
            int i = gas ? (n - 1 - k) : k;
            long put = Math.min(capacities[i], remaining);
            out[i] = put;
            remaining -= put;
        }
        return out;
    }
}
