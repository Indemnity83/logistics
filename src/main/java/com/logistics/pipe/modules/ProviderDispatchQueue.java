package com.logistics.pipe.modules;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pure-Java dispatch queue for {@link ProviderModule}.
 *
 * <p>Decouples the network's dispatch command from physical extraction: when the network
 * requests a delivery, the request is enqueued here; extraction happens later at a
 * controlled rate ({@code ITEMS_PER_EXTRACT} items per scan cycle).
 *
 * <p>Tracks per-item reservation totals so the supply scan can subtract committed-but-
 * not-yet-extracted amounts, preventing double-counting.
 *
 * <p>Zero Minecraft API coupling — 100% testable with pure Java.
 */
public class ProviderDispatchQueue {

    /**
     * A single pending dispatch request.
     *
     * @param itemId     registry key of the item (e.g. "minecraft:iron_ingot")
     * @param itemTag    full serialized ItemStack tag preserving data components (null = bare item)
     * @param remaining  items still to be extracted and injected into the pipe
     * @param requester  destination pipe position
     * @param deliveryId delivery correlation UUID (may be null)
     */
    public record Entry(
            String itemId,
            @Nullable CompoundTag itemTag,
            long remaining,
            BlockPos requester,
            @Nullable UUID deliveryId) {}

    private final Deque<Entry> entries = new ArrayDeque<>();

    // ===== Mutation =====

    /**
     * Add a new dispatch request to the tail of the queue.
     *
     * @param itemTag serialized ItemStack tag preserving data components; null for bare items
     */
    public void enqueue(String itemId, @Nullable CompoundTag itemTag, long amount, BlockPos requester, @Nullable UUID deliveryId) {
        if (amount <= 0) return;
        entries.addLast(new Entry(itemId, itemTag, amount, requester, deliveryId));
    }

    /** Add a bare-item dispatch request (no data components). */
    public void enqueue(String itemId, long amount, BlockPos requester, @Nullable UUID deliveryId) {
        enqueue(itemId, null, amount, requester, deliveryId);
    }

    /**
     * Record that {@code consumed} items were extracted from the head entry.
     * Reduces {@code remaining} by {@code consumed}; removes the head entry when exhausted.
     */
    public void consumeFromHead(long consumed) {
        Entry head = entries.peekFirst();
        if (head == null || consumed <= 0) return;
        entries.pollFirst();
        long newRemaining = head.remaining() - consumed;
        if (newRemaining > 0) {
            entries.addFirst(new Entry(head.itemId(), head.itemTag(), newRemaining, head.requester(), head.deliveryId()));
        }
    }

    /**
     * Remove the head entry unconditionally (e.g. when extraction fails permanently).
     */
    public void removeHead() {
        entries.pollFirst();
    }

    // ===== Query =====

    /**
     * Return the head entry without removing it, or {@code null} if the queue is empty.
     */
    @Nullable
    public Entry peekHead() {
        return entries.peekFirst();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int size() {
        return entries.size();
    }

    /**
     * Return total reserved amounts per item ID across all queued entries.
     * Used by the supply scan to subtract committed-but-not-yet-extracted items
     * so the network doesn't double-count them as available stock.
     */
    public Map<String, Long> getReservations() {
        Map<String, Long> result = new HashMap<>();
        for (Entry e : entries) {
            result.merge(e.itemId(), e.remaining(), Long::sum);
        }
        return result;
    }

    /**
     * Return a snapshot of all entries (for NBT serialisation).
     */
    public List<Entry> entries() {
        return new ArrayList<>(entries);
    }

    /**
     * Compute the effective available amount for an item after subtracting queue reservations.
     *
     * @param rawAmount  raw amount from inventory scan
     * @param reservations reservation map from {@link #getReservations()}
     * @param itemId     item to look up in the reservation map
     * @return effective available amount (≥ 0)
     */
    public static long subtractReservation(long rawAmount, Map<String, Long> reservations, String itemId) {
        Long reserved = reservations.get(itemId);
        if (reserved == null) return rawAmount;
        return Math.max(0L, rawAmount - reserved);
    }
}
