package com.logistics.pipe.modules;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FluidPacketChunker")
class FluidPacketChunkerTest {

    // ==================== chunk() ====================

    @Test
    @DisplayName("even split produces only full-size chunks, no tail")
    void evenSplitHasNoTail() {
        List<Long> chunks = FluidPacketChunker.chunk(3000, 1000);
        assertThat(chunks).containsExactly(1000L, 1000L, 1000L);
    }

    @Test
    @DisplayName("uneven split produces full chunks plus a smaller tail last")
    void unevenSplitHasTailLast() {
        List<Long> chunks = FluidPacketChunker.chunk(2350, 1000);
        assertThat(chunks).containsExactly(1000L, 1000L, 350L);
    }

    @Test
    @DisplayName("an amount smaller than maxMb produces a single tail-only chunk")
    void subMaxAmountIsSingleTail() {
        List<Long> chunks = FluidPacketChunker.chunk(137, 1000);
        assertThat(chunks).containsExactly(137L);
    }

    @Test
    @DisplayName("a non-positive extracted amount produces no chunks")
    void nonPositiveAmountIsEmpty() {
        assertThat(FluidPacketChunker.chunk(0, 1000)).isEmpty();
        assertThat(FluidPacketChunker.chunk(-5, 1000)).isEmpty();
    }

    // ==================== probeAffordablePrefixCount() ====================

    @Test
    @DisplayName("full affordability keeps every chunk")
    void fullyAffordableKeepsEverything() {
        int kept = FluidPacketChunker.probeAffordablePrefixCount(3, count -> true);
        assertThat(kept).isEqualTo(3);
    }

    @Test
    @DisplayName("energy shortfall drops the tail chunk first, before any full chunk")
    void shortfallDropsTailFirst() {
        // Simulate: chunk() built [1000, 1000, 350] (2 full + 1 tail); only 2 packets are affordable.
        List<Long> chunks = FluidPacketChunker.chunk(2350, 1000);
        int kept = FluidPacketChunker.probeAffordablePrefixCount(chunks.size(), count -> count <= 2);

        assertThat(kept).isEqualTo(2);
        // The kept prefix is the two FULL chunks — the tail (last in the list) was dropped first.
        assertThat(chunks.subList(0, kept)).containsExactly(1000L, 1000L);
    }

    @Test
    @DisplayName("full chunks are dropped only once dropping the tail alone isn't enough")
    void dropsFullChunksOnlyAfterTailInsufficient() {
        List<Long> chunks = FluidPacketChunker.chunk(2350, 1000); // [1000, 1000, 350]
        int kept = FluidPacketChunker.probeAffordablePrefixCount(chunks.size(), count -> count <= 1);

        assertThat(kept).isEqualTo(1);
        assertThat(chunks.subList(0, kept)).containsExactly(1000L);
    }

    @Test
    @DisplayName("zero affordability drops every chunk")
    void zeroAffordableDropsAll() {
        int kept = FluidPacketChunker.probeAffordablePrefixCount(4, count -> false);
        assertThat(kept).isEqualTo(0);
    }

    @Test
    @DisplayName("probe never calls the predicate again once it has succeeded")
    void probeStopsAtFirstSuccess() {
        AtomicInteger calls = new AtomicInteger();
        int kept = FluidPacketChunker.probeAffordablePrefixCount(5, count -> {
            calls.incrementAndGet();
            return count == 3; // succeeds on the third probe (5 -> 4 -> 3)
        });

        assertThat(kept).isEqualTo(3);
        assertThat(calls.get()).isEqualTo(3);
    }
}
