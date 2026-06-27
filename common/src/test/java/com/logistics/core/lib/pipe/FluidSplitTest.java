package com.logistics.core.lib.pipe;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Fluid split allocation")
class FluidSplitTest {

    @Test
    @DisplayName("splits a huge budget without looping per millibucket")
    void largeBudget() {
        long[] alloc = FluidSplit.split(1_000_000, new long[] {600_000, 600_000});
        assertThat(alloc).containsExactly(500_000, 500_000);
        assertThat(alloc[0] + alloc[1]).isEqualTo(1_000_000);
    }

    @Test
    @DisplayName("conserves the total up to the sum of room and never exceeds a cap")
    void conservesAndRespectsCaps() {
        long[] room = {50, 1_000_000, 7, 9_999_999};
        long[] alloc = FluidSplit.split(2_500_000, room);
        long sum = 0;
        for (int i = 0; i < room.length; i++) {
            assertThat(alloc[i]).isBetween(0L, room[i]);
            sum += alloc[i];
        }
        assertThat(sum).isEqualTo(2_500_000); // total <= sum of room, so all of it lands
    }

    @Test
    @DisplayName("matches the original round-robin distribution exactly")
    void matchesRoundRobinReference() {
        long[][] rooms = {
            {250, 250},
            {250, 250, 250},
            {3, 250},
            {0, 0},
            {5, 5, 5, 5},
            {1, 100, 100},
            {10, 0, 10},
            {7, 13, 2, 40},
        };
        for (long[] room : rooms) {
            for (long total : new long[] {0, 1, 2, 5, 13, 20, 47, 200, 5000}) {
                assertThat(FluidSplit.split(total, room))
                        .as("total=%d room=%s", total, java.util.Arrays.toString(room))
                        .containsExactly(roundRobin(total, room));
            }
        }
    }

    /** The original 1-mB-at-a-time round-robin, kept here as the behavioral oracle for the rewrite. */
    private static long[] roundRobin(long total, long[] room) {
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
