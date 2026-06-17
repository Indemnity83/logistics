package com.logistics.core.lib.pipe;

/**
 * Pure helper for splitting a quantity of fluid across a pipe junction's outputs. Kept free of Minecraft
 * types so it is unit-testable without a bootstrapped game.
 */
public final class FluidSplit {

    private FluidSplit() {}

    /**
     * Distributes {@code total} mB across the outputs, 1 mB at a time round-robin, each output capped by its
     * available {@code room}. Spreads as evenly as the caps allow, hands any remainder to the earliest outputs,
     * and conserves the total (up to the sum of room).
     */
    public static long[] split(long total, long[] room) {
        long[] alloc = new long[room.length];
        long left = total;
        boolean progress = true;
        while (left > 0 && progress) {
            progress = false;
            for (int i = 0; i < room.length && left > 0; i++) {
                if (alloc[i] < room[i]) {
                    alloc[i]++;
                    left--;
                    progress = true;
                }
            }
        }
        return alloc;
    }
}
