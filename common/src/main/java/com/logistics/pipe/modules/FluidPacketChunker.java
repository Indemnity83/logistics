package com.logistics.pipe.modules;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

/**
 * Pure chunking/refund math for splitting a dispatched fluid amount into individual physical packets —
 * extracted out of {@link FluidProviderModule} so it's unit-testable without game-test overhead.
 *
 * <p>Fluid packets never stack (see {@code FluidPacketItem}), so every entry in a {@link #chunk} result
 * becomes exactly one physical packet/{@code TravelingItem}.
 */
final class FluidPacketChunker {
    private FluidPacketChunker() {}

    /**
     * Split {@code extractedMb} into individual packet amounts: as many whole {@code maxMb} chunks as
     * fit, then one smaller tail chunk for the remainder (if any). Order matters — the tail is always
     * last — since {@link #probeAffordablePrefixCount} drops from the end of this list first.
     *
     * @return per-packet amounts in dispatch order; empty if {@code extractedMb <= 0}
     */
    static List<Long> chunk(long extractedMb, long maxMb) {
        if (extractedMb <= 0 || maxMb <= 0) return List.of();
        long fullCount = extractedMb / maxMb;
        long tailMb = extractedMb % maxMb;
        List<Long> chunks = new ArrayList<>((int) fullCount + (tailMb > 0 ? 1 : 0));
        for (long i = 0; i < fullCount; i++) chunks.add(maxMb);
        if (tailMb > 0) chunks.add(tailMb);
        return chunks;
    }

    /**
     * Downward-probe for the largest affordable prefix count, starting from {@code totalPackets} and
     * decrementing until {@code tryAfford} succeeds (or reaches 0). {@code tryAfford} must be
     * non-destructive on failure (mirrors {@code PipeContext.consumeEnergy}'s contract) so a failed
     * probe never partially charges.
     *
     * <p>Combined with {@link #chunk}'s tail-last ordering, dropping from the end here always drops
     * the tail chunk before any full chunk — a full chunk carries strictly more payload than the tail
     * for the same flat per-packet fee, so this maximizes mB delivered per unit of energy spent.
     *
     * @return the number of chunks (counted from the start of the list) that remain affordable
     */
    static int probeAffordablePrefixCount(int totalPackets, IntPredicate tryAfford) {
        int count = totalPackets;
        while (count > 0 && !tryAfford.test(count)) {
            count--;
        }
        return count;
    }
}
