package com.logistics.neoforge.storage;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.logistics.neoforge.storage.NeoForgeStorageTestSupport.DIAMOND;
import static com.logistics.neoforge.storage.NeoForgeStorageTestSupport.DIAMOND_KEY;
import static com.logistics.neoforge.storage.NeoForgeStorageTestSupport.FakeSlottedStorage;
import static com.logistics.neoforge.storage.NeoForgeStorageTestSupport.GOLD;
import static com.logistics.neoforge.storage.NeoForgeStorageTestSupport.GOLD_KEY;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code NeoForgeItemStorage.asNeoForge()} over a slot-aware storage.
 *
 * <p>This is the direction with real logic in it. NeoForge transactions nest and may abort, while
 * {@code ISlottedItemStorage} has only a simulate flag, so the adapter stages every transfer as a
 * pending delta and flushes it once, on the root commit. Nothing here asserts against the staging
 * map — every test drives real {@link Transaction}s and checks the backing storage.
 */
@DisplayName("NeoForgeItemStorage.asNeoForge() — slotted")
class NeoForgeSlottedHandlerTest {

    private static ResourceHandler<ItemResource> handlerFor(FakeSlottedStorage backing) {
        return NeoForgeItemStorage.asNeoForge(backing);
    }

    @Nested
    @DisplayName("staging")
    class Staging {

        @Test
        @DisplayName("an insert reaches the storage only when the root transaction commits")
        void insert_isStagedUntilRootCommit() {
            FakeSlottedStorage backing = new FakeSlottedStorage(2, 64);
            ResourceHandler<ItemResource> handler = handlerFor(backing);

            try (Transaction tx = Transaction.openRoot()) {
                assertThat(handler.insert(0, DIAMOND, 40, tx)).isEqualTo(40);
                assertThat(backing.amountOf(0)).isZero();
                tx.commit();
            }

            assertThat(backing.amountOf(0)).isEqualTo(40);
            assertThat(backing.itemOf(0)).isEqualTo(DIAMOND_KEY);
        }

        @Test
        @DisplayName("an extract reaches the storage only when the root transaction commits")
        void extract_isStagedUntilRootCommit() {
            FakeSlottedStorage backing = new FakeSlottedStorage(2, 64);
            backing.preload(0, DIAMOND_KEY, 50);
            ResourceHandler<ItemResource> handler = handlerFor(backing);

            try (Transaction tx = Transaction.openRoot()) {
                assertThat(handler.extract(0, DIAMOND, 20, tx)).isEqualTo(20);
                assertThat(backing.amountOf(0)).isEqualTo(50);
                tx.commit();
            }

            assertThat(backing.amountOf(0)).isEqualTo(30);
        }

        @Test
        @DisplayName("a nested commit inside an aborted transaction must not reach the storage")
        void nestedCommit_thenRootAbort_leavesStorageUntouched() {
            // Helpers routinely wrap their work in a nested transaction and commit it. If that
            // nested commit wrote through, a later abort of the enclosing transaction could not
            // undo it and the items would be duplicated.
            FakeSlottedStorage backing = new FakeSlottedStorage(2, 64);
            ResourceHandler<ItemResource> handler = handlerFor(backing);

            try (Transaction root = Transaction.openRoot()) {
                try (Transaction nested = Transaction.open(root)) {
                    assertThat(handler.insert(0, DIAMOND, 25, nested)).isEqualTo(25);
                    nested.commit();
                }
                assertThat(backing.amountOf(0)).isZero();
            }

            assertThat(backing.amountOf(0)).isZero();
        }

        @Test
        @DisplayName("a nested commit is applied once, when the root transaction commits")
        void nestedCommit_thenRootCommit_appliesOnce() {
            FakeSlottedStorage backing = new FakeSlottedStorage(2, 64);
            ResourceHandler<ItemResource> handler = handlerFor(backing);

            try (Transaction root = Transaction.openRoot()) {
                try (Transaction nested = Transaction.open(root)) {
                    assertThat(handler.insert(0, DIAMOND, 25, nested)).isEqualTo(25);
                    nested.commit();
                }
                root.commit();
            }

            assertThat(backing.amountOf(0)).isEqualTo(25);
        }

        @Test
        @DisplayName("an aborted nested transaction rolls back even when the root one commits")
        void nestedAbort_thenRootCommit_appliesNothing() {
            FakeSlottedStorage backing = new FakeSlottedStorage(2, 64);
            ResourceHandler<ItemResource> handler = handlerFor(backing);

            try (Transaction root = Transaction.openRoot()) {
                try (Transaction nested = Transaction.open(root)) {
                    assertThat(handler.insert(0, DIAMOND, 25, nested)).isEqualTo(25);
                }
                root.commit();
            }

            assertThat(backing.amountOf(0)).isZero();
        }

        @Test
        @DisplayName("repeated inserts in one transaction must not offer more room than the slot has")
        void insert_respectsPendingDelta() {
            // The storage sees nothing until the commit, so every simulation inside the transaction
            // reports the same empty slot. Only the staged delta stops the adapter promising the
            // same 64 items of room over and over and overfilling on flush.
            FakeSlottedStorage backing = new FakeSlottedStorage(2, 64);
            ResourceHandler<ItemResource> handler = handlerFor(backing);

            try (Transaction tx = Transaction.openRoot()) {
                assertThat(handler.insert(0, DIAMOND, 40, tx)).isEqualTo(40);
                assertThat(handler.insert(0, DIAMOND, 40, tx)).isEqualTo(24);
                assertThat(handler.insert(0, DIAMOND, 40, tx)).isZero();
                tx.commit();
            }

            assertThat(backing.amountOf(0)).isEqualTo(64);
        }

        @Test
        @DisplayName("repeated extracts in one transaction must not hand out the same items twice")
        void extract_respectsPendingDelta() {
            FakeSlottedStorage backing = new FakeSlottedStorage(2, 64);
            backing.preload(0, DIAMOND_KEY, 60);
            ResourceHandler<ItemResource> handler = handlerFor(backing);

            try (Transaction tx = Transaction.openRoot()) {
                assertThat(handler.extract(0, DIAMOND, 40, tx)).isEqualTo(40);
                assertThat(handler.extract(0, DIAMOND, 40, tx)).isEqualTo(20);
                assertThat(handler.extract(0, DIAMOND, 40, tx)).isZero();
                tx.commit();
            }

            assertThat(backing.amountOf(0)).isZero();
        }

        @Test
        @DisplayName("items staged into a slot can be taken back out in the same transaction")
        void insertThenExtract_inOneTransaction_netsOut() {
            // The slot holds nothing, so this only works if the pending insert is visible to the
            // extract. It nets to zero, so the commit must write nothing at all.
            FakeSlottedStorage backing = new FakeSlottedStorage(2, 64);
            ResourceHandler<ItemResource> handler = handlerFor(backing);

            try (Transaction tx = Transaction.openRoot()) {
                assertThat(handler.insert(0, DIAMOND, 40, tx)).isEqualTo(40);
                assertThat(handler.extract(0, DIAMOND, 40, tx)).isEqualTo(40);
                tx.commit();
            }

            assertThat(backing.amountOf(0)).isZero();
            assertThat(backing.itemOf(0)).isNull();
        }

        @Test
        @DisplayName("staged deltas are scoped to the slot they were staged against")
        void staging_isScopedPerSlot() {
            FakeSlottedStorage backing = new FakeSlottedStorage(2, 64);
            ResourceHandler<ItemResource> handler = handlerFor(backing);

            try (Transaction tx = Transaction.openRoot()) {
                assertThat(handler.insert(0, DIAMOND, 64, tx)).isEqualTo(64);
                assertThat(handler.insert(1, DIAMOND, 10, tx)).isEqualTo(10);
                tx.commit();
            }

            assertThat(backing.amountOf(0)).isEqualTo(64);
            assertThat(backing.amountOf(1)).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("reads inside a transaction")
    class Reads {

        @Test
        @DisplayName("a slot reports what this transaction has staged into it")
        void getAmountAsLong_includesPendingInsert() {
            // A caller that cannot read back its own write inside its own transaction sees an
            // unchanged slot and writes again.
            FakeSlottedStorage backing = new FakeSlottedStorage(2, 64);
            backing.preload(0, DIAMOND_KEY, 10);
            ResourceHandler<ItemResource> handler = handlerFor(backing);

            try (Transaction tx = Transaction.openRoot()) {
                handler.insert(0, DIAMOND, 15, tx);
                assertThat(handler.getAmountAsLong(0)).isEqualTo(25);
                tx.commit();
            }
        }

        @Test
        @DisplayName("an empty slot reports the resource this transaction staged into it")
        void getResource_reportsResourceStagedIntoAnEmptySlot() {
            FakeSlottedStorage backing = new FakeSlottedStorage(2, 64);
            ResourceHandler<ItemResource> handler = handlerFor(backing);

            try (Transaction tx = Transaction.openRoot()) {
                handler.insert(0, DIAMOND, 15, tx);
                assertThat(handler.getResource(0)).isEqualTo(DIAMOND);
                assertThat(handler.getAmountAsLong(0)).isEqualTo(15);
                tx.commit();
            }
        }

        @Test
        @DisplayName("a slot emptied by this transaction reads as empty")
        void getResource_ofFullyStagedOutSlot_isEmpty() {
            // Without the pending delta a read-until-empty loop never terminates: the backing slot
            // still shows its original contents for the whole transaction.
            FakeSlottedStorage backing = new FakeSlottedStorage(2, 64);
            backing.preload(0, DIAMOND_KEY, 30);
            ResourceHandler<ItemResource> handler = handlerFor(backing);

            try (Transaction tx = Transaction.openRoot()) {
                assertThat(handler.extract(0, DIAMOND, 30, tx)).isEqualTo(30);
                assertThat(handler.getAmountAsLong(0)).isZero();
                assertThat(handler.getResource(0)).isEqualTo(ItemResource.EMPTY);
                tx.commit();
            }
        }
    }

    @Nested
    @DisplayName("capacity")
    class Capacity {

        @Test
        @DisplayName("a full slot still reports the capacity it has")
        void getCapacityAsLong_ofFullSlot_isTheSlotCapacity() {
            // Deriving this from insert room reports 0 for a full slot, which reads as "nothing
            // ever fits here" rather than "this is full right now".
            FakeSlottedStorage backing = new FakeSlottedStorage(1, 64);
            backing.preload(0, DIAMOND_KEY, 64);
            ResourceHandler<ItemResource> handler = handlerFor(backing);

            assertThat(handler.getCapacityAsLong(0, DIAMOND)).isEqualTo(64);
        }

        @Test
        @DisplayName("an empty slot reports the capacity it could hold")
        void getCapacityAsLong_ofEmptySlot_isTheSlotCapacity() {
            // ResourceHandlerUtil.isFull compares amount >= capacity, so a 0 here makes an empty
            // slot read as full and a hopper never inserts into it.
            FakeSlottedStorage backing = new FakeSlottedStorage(1, 16);
            ResourceHandler<ItemResource> handler = handlerFor(backing);

            assertThat(handler.getCapacityAsLong(0, ItemResource.EMPTY)).isEqualTo(16);
        }

        @Test
        @DisplayName("capacity does not depend on the resource asked about")
        void getCapacityAsLong_ignoresTheResourceAsked() {
            // NeoForge asks for the capacity "irrespective of the current amount or resource at
            // that index", so a slot holding diamonds answers the same for gold.
            FakeSlottedStorage backing = new FakeSlottedStorage(1, 16);
            backing.preload(0, DIAMOND_KEY, 4);
            ResourceHandler<ItemResource> handler = handlerFor(backing);

            assertThat(handler.getCapacityAsLong(0, GOLD)).isEqualTo(16);
            assertThat(handler.getCapacityAsLong(0, ItemResource.EMPTY)).isEqualTo(16);
        }
    }

    @Nested
    @DisplayName("validity")
    class Validity {

        @Test
        @DisplayName("no slot accepts an empty resource")
        void isValid_rejectsAnEmptyResource() {
            FakeSlottedStorage backing = new FakeSlottedStorage(1, 64);
            ResourceHandler<ItemResource> handler = handlerFor(backing);

            assertThat(handler.isValid(0, ItemResource.EMPTY)).isFalse();
        }

        @Test
        @DisplayName("a full slot still accepts the resource it already holds")
        void isValid_ofFullSlot_acceptsWhatItHolds() {
            // Compatibility, not current room: answering "no room" here tells other mods the item
            // may never go in this slot at all.
            FakeSlottedStorage backing = new FakeSlottedStorage(1, 64);
            backing.preload(0, DIAMOND_KEY, 64);
            ResourceHandler<ItemResource> handler = handlerFor(backing);

            assertThat(handler.isValid(0, DIAMOND)).isTrue();
        }

        @Test
        @DisplayName("an empty slot accepts a resource it would take")
        void isValid_ofEmptySlot_acceptsAnAcceptableResource() {
            FakeSlottedStorage backing = new FakeSlottedStorage(1, 64);
            ResourceHandler<ItemResource> handler = handlerFor(backing);

            assertThat(handler.isValid(0, DIAMOND)).isTrue();
        }

        @Test
        @DisplayName("a slot rejects a resource it would never take")
        void isValid_rejectsAResourceTheSlotFiltersOut() {
            // Empty and roomy, so only the slot's own filter can refuse it.
            FakeSlottedStorage backing = new FakeSlottedStorage(1, 64);
            backing.acceptOnly(0, GOLD_KEY);
            ResourceHandler<ItemResource> handler = handlerFor(backing);

            assertThat(handler.isValid(0, DIAMOND)).isFalse();
            assertThat(handler.isValid(0, GOLD)).isTrue();
        }
    }
}
