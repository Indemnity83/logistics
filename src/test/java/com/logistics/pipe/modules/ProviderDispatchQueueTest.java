package com.logistics.pipe.modules;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ProviderDispatchQueue}.
 *
 * <p>No Minecraft environment needed — the queue is pure Java.
 */
class ProviderDispatchQueueTest {

    private ProviderDispatchQueue queue;

    private static final BlockPos REQUESTER_A = new BlockPos(1, 0, 0);
    private static final BlockPos REQUESTER_B = new BlockPos(2, 0, 0);
    private static final String DIAMOND = "minecraft:diamond";
    private static final String EMERALD = "minecraft:emerald";
    private static final UUID DELIVERY_1 = UUID.randomUUID();
    private static final UUID DELIVERY_2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        queue = new ProviderDispatchQueue();
    }

    // ===== Basic state =====

    @Test
    void startsEmpty() {
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
        assertNull(queue.peekHead());
    }

    @Test
    void enqueue_addsEntry() {
        queue.enqueue(DIAMOND, 64, REQUESTER_A, DELIVERY_1);

        assertFalse(queue.isEmpty());
        assertEquals(1, queue.size());
    }

    @Test
    void enqueue_ignoresZeroOrNegativeAmount() {
        queue.enqueue(DIAMOND, 0, REQUESTER_A, null);
        queue.enqueue(DIAMOND, -5, REQUESTER_A, null);

        assertTrue(queue.isEmpty());
    }

    @Test
    void peekHead_returnsHeadWithoutRemoving() {
        queue.enqueue(DIAMOND, 64, REQUESTER_A, DELIVERY_1);
        queue.enqueue(EMERALD, 32, REQUESTER_B, DELIVERY_2);

        ProviderDispatchQueue.Entry head = queue.peekHead();
        assertNotNull(head);
        assertEquals(DIAMOND, head.itemId());
        assertEquals(64, head.remaining());
        assertEquals(REQUESTER_A, head.requester());
        assertEquals(DELIVERY_1, head.deliveryId());
        assertEquals(2, queue.size(), "peekHead must not remove the entry");
    }

    @Test
    void peekHead_onEmptyQueue_returnsNull() {
        assertNull(queue.peekHead());
    }

    // ===== removeHead =====

    @Test
    void removeHead_removesFirstEntry() {
        queue.enqueue(DIAMOND, 64, REQUESTER_A, null);
        queue.enqueue(EMERALD, 32, REQUESTER_B, null);

        queue.removeHead();

        assertEquals(1, queue.size());
        assertEquals(EMERALD, queue.peekHead().itemId());
    }

    @Test
    void removeHead_onEmptyQueue_doesNotThrow() {
        assertDoesNotThrow(() -> queue.removeHead());
        assertTrue(queue.isEmpty());
    }

    // ===== consumeFromHead =====

    @Test
    void consumeFromHead_partialConsumption_reducesRemaining() {
        queue.enqueue(DIAMOND, 64, REQUESTER_A, DELIVERY_1);

        queue.consumeFromHead(8);

        assertFalse(queue.isEmpty());
        assertEquals(56, queue.peekHead().remaining());
        assertEquals(DIAMOND, queue.peekHead().itemId());
        assertEquals(DELIVERY_1, queue.peekHead().deliveryId(), "delivery id must be preserved");
    }

    @Test
    void consumeFromHead_fullConsumption_removesEntry() {
        queue.enqueue(DIAMOND, 8, REQUESTER_A, null);

        queue.consumeFromHead(8);

        assertTrue(queue.isEmpty());
    }

    @Test
    void consumeFromHead_overConsumption_removesEntry() {
        queue.enqueue(DIAMOND, 8, REQUESTER_A, null);

        queue.consumeFromHead(100);

        assertTrue(queue.isEmpty());
    }

    @Test
    void consumeFromHead_advancesToNextEntry() {
        queue.enqueue(DIAMOND, 8, REQUESTER_A, null);
        queue.enqueue(EMERALD, 32, REQUESTER_B, null);

        queue.consumeFromHead(8); // exhausts diamond entry

        assertEquals(1, queue.size());
        assertEquals(EMERALD, queue.peekHead().itemId());
    }

    @Test
    void consumeFromHead_onEmptyQueue_doesNotThrow() {
        assertDoesNotThrow(() -> queue.consumeFromHead(8));
        assertTrue(queue.isEmpty());
    }

    @Test
    void consumeFromHead_zeroAmount_doesNothing() {
        queue.enqueue(DIAMOND, 64, REQUESTER_A, null);

        queue.consumeFromHead(0);

        assertEquals(64, queue.peekHead().remaining());
    }

    // ===== getReservations =====

    @Test
    void getReservations_emptyQueue_returnsEmptyMap() {
        assertTrue(queue.getReservations().isEmpty());
    }

    @Test
    void getReservations_singleEntry() {
        queue.enqueue(DIAMOND, 64, REQUESTER_A, null);

        Map<String, Long> res = queue.getReservations();

        assertEquals(1, res.size());
        assertEquals(64L, res.get(DIAMOND));
    }

    @Test
    void getReservations_multipleEntriesSameItem_sumsAmounts() {
        queue.enqueue(DIAMOND, 64, REQUESTER_A, null);
        queue.enqueue(DIAMOND, 32, REQUESTER_B, null);

        Map<String, Long> res = queue.getReservations();

        assertEquals(96L, res.get(DIAMOND));
    }

    @Test
    void getReservations_multipleItems_tracked_separately() {
        queue.enqueue(DIAMOND, 64, REQUESTER_A, null);
        queue.enqueue(EMERALD, 32, REQUESTER_B, null);

        Map<String, Long> res = queue.getReservations();

        assertEquals(64L, res.get(DIAMOND));
        assertEquals(32L, res.get(EMERALD));
    }

    @Test
    void getReservations_decreasesAfterConsume() {
        queue.enqueue(DIAMOND, 64, REQUESTER_A, null);
        queue.consumeFromHead(8);

        Map<String, Long> res = queue.getReservations();

        assertEquals(56L, res.get(DIAMOND));
    }

    @Test
    void getReservations_clearsAfterEntryExhausted() {
        queue.enqueue(DIAMOND, 8, REQUESTER_A, null);
        queue.consumeFromHead(8);

        assertTrue(queue.getReservations().isEmpty());
    }

    // ===== subtractReservation (static helper) =====

    @Test
    void subtractReservation_noReservation_returnsRawAmount() {
        Map<String, Long> reservations = Map.of(EMERALD, 10L);

        long result = ProviderDispatchQueue.subtractReservation(64, reservations, DIAMOND);

        assertEquals(64L, result);
    }

    @Test
    void subtractReservation_partialReservation() {
        Map<String, Long> reservations = Map.of(DIAMOND, 20L);

        long result = ProviderDispatchQueue.subtractReservation(64, reservations, DIAMOND);

        assertEquals(44L, result);
    }

    @Test
    void subtractReservation_reservationEqualsAmount_returnsZero() {
        Map<String, Long> reservations = Map.of(DIAMOND, 64L);

        long result = ProviderDispatchQueue.subtractReservation(64, reservations, DIAMOND);

        assertEquals(0L, result);
    }

    @Test
    void subtractReservation_reservationExceedsAmount_clampsToZero() {
        Map<String, Long> reservations = Map.of(DIAMOND, 100L);

        long result = ProviderDispatchQueue.subtractReservation(64, reservations, DIAMOND);

        assertEquals(0L, result);
    }

    // ===== FIFO ordering =====

    @Test
    void fifoOrdering_headsProcessedInEnqueueOrder() {
        queue.enqueue(DIAMOND, 10, REQUESTER_A, null);
        queue.enqueue(EMERALD, 20, REQUESTER_B, null);

        assertEquals(DIAMOND, queue.peekHead().itemId());
        queue.removeHead();
        assertEquals(EMERALD, queue.peekHead().itemId());
    }

    // ===== entries() snapshot =====

    @Test
    void entries_returnsDefensiveCopy() {
        queue.enqueue(DIAMOND, 64, REQUESTER_A, null);

        var snapshot = queue.entries();
        snapshot.clear(); // modifying the list must not affect the queue

        assertEquals(1, queue.size());
    }

    // ===== itemTag (component-aware dispatch) =====

    @Test
    void enqueue_withItemTag_preservesTagInEntry() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", DIAMOND);
        tag.putInt("count", 1);

        queue.enqueue(DIAMOND, tag, 5, REQUESTER_A, null);

        ProviderDispatchQueue.Entry head = queue.peekHead();
        assertNotNull(head);
        assertEquals(tag, head.itemTag());
    }

    @Test
    void enqueue_withNullItemTag_storesNull() {
        queue.enqueue(DIAMOND, (CompoundTag) null, 5, REQUESTER_A, null);

        assertNull(queue.peekHead().itemTag());
    }

    @Test
    void enqueue_bareOverload_storesNullTag() {
        queue.enqueue(DIAMOND, 5, REQUESTER_A, null);

        assertNull(queue.peekHead().itemTag(), "bare enqueue overload must store null itemTag");
    }

    @Test
    void consumeFromHead_partial_preservesItemTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", DIAMOND);

        queue.enqueue(DIAMOND, tag, 10, REQUESTER_A, DELIVERY_1);
        queue.consumeFromHead(3);

        ProviderDispatchQueue.Entry head = queue.peekHead();
        assertNotNull(head);
        assertEquals(7, head.remaining());
        assertEquals(tag, head.itemTag(), "itemTag must survive partial consumption");
    }

    @Test
    void consumeFromHead_full_removesEntry_tagGone() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", DIAMOND);

        queue.enqueue(DIAMOND, tag, 5, REQUESTER_A, null);
        queue.consumeFromHead(5);

        assertTrue(queue.isEmpty());
    }

    @Test
    void entries_snapshot_preservesItemTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", DIAMOND);

        queue.enqueue(DIAMOND, tag, 8, REQUESTER_A, null);

        var snapshot = queue.entries();
        assertEquals(1, snapshot.size());
        assertEquals(tag, snapshot.get(0).itemTag());
    }

    @Test
    void getReservations_itemTagDoesNotAffectReservationKey() {
        CompoundTag tag = new CompoundTag();
        tag.putString("enchantment", "sharpness");

        queue.enqueue(DIAMOND, tag, 3, REQUESTER_A, null);
        queue.enqueue(DIAMOND, 5, REQUESTER_B, null); // bare, same item type

        // Reservations are keyed by itemId only — components are not factored in
        Map<String, Long> res = queue.getReservations();
        assertEquals(8L, res.get(DIAMOND), "enchanted and bare counts should both contribute to item-type reservation");
    }
}
