package com.logistics.core.lib.pipe;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure helper for splitting a quantity of fluid across a pipe junction's outputs. Kept free of Minecraft
 * types so it is unit-testable without a bootstrapped game.
 */
public final class FluidSplit {

    private FluidSplit() {}

    /**
     * Distributes {@code total} mB across the outputs as evenly as the caps allow: each output is filled equally
     * until it hits its available {@code room}, with any indivisible remainder handed to the earliest outputs.
     * Conserves the total up to the sum of room.
     *
     * <p>This is a max-min fair (water-filling) allocation computed arithmetically in passes over the outputs —
     * one pass per output that caps out — rather than a per-millibucket loop, so its cost is bounded by the
     * number of outputs and never by {@code total}.
     */
    public static long[] split(long total, long[] room) {
        long[] alloc = new long[room.length];
        if (total <= 0) {
            return alloc;
        }
        // Indices still able to take more fluid, kept in order so the remainder favors the earliest outputs.
        List<Integer> active = new ArrayList<>(room.length);
        for (int i = 0; i < room.length; i++) {
            if (room[i] > 0) {
                active.add(i);
            }
        }

        long remaining = total;
        while (remaining > 0 && !active.isEmpty()) {
            long share = remaining / active.size();
            if (share == 0) {
                // Fewer mB left than outputs: hand one each to the earliest, which all still have room.
                for (int idx : active) {
                    if (remaining == 0) {
                        break;
                    }
                    alloc[idx]++;
                    remaining--;
                }
                break;
            }

            long minRoomLeft = Long.MAX_VALUE;
            for (int idx : active) {
                minRoomLeft = Math.min(minRoomLeft, room[idx] - alloc[idx]);
            }
            if (minRoomLeft <= share) {
                // An even fill of minRoomLeft caps the tightest outputs; apply it and drop the now-full ones.
                for (int idx : active) {
                    alloc[idx] += minRoomLeft;
                }
                remaining -= minRoomLeft * active.size();
                active.removeIf(idx -> alloc[idx] >= room[idx]);
            } else {
                // No output caps this pass: even share to all, the indivisible remainder to the earliest.
                long rem = remaining - share * active.size();
                for (int idx : active) {
                    alloc[idx] += share + (rem-- > 0 ? 1 : 0);
                }
                remaining = 0;
            }
        }
        return alloc;
    }
}
