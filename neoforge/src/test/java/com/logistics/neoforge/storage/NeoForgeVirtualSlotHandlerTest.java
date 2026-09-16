package com.logistics.neoforge.storage;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.logistics.neoforge.storage.NeoForgeStorageTestSupport.DIAMOND;
import static com.logistics.neoforge.storage.NeoForgeStorageTestSupport.DIAMOND_KEY;
import static com.logistics.neoforge.storage.NeoForgeStorageTestSupport.FakeItemStorage;
import static com.logistics.neoforge.storage.NeoForgeStorageTestSupport.GOLD;
import static com.logistics.neoforge.storage.NeoForgeStorageTestSupport.GOLD_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@code NeoForgeItemStorage.asNeoForge()} over a storage with no fixed slots.
 *
 * <p>NeoForge's {@code ResourceHandler} is intrinsically slotted, so a resource-oriented storage is
 * presented as a single virtual slot. The staging layer is the same shape as the slotted adapter's
 * but keyed on the resource alone.
 */
@DisplayName("NeoForgeItemStorage.asNeoForge() — virtual slot")
class NeoForgeVirtualSlotHandlerTest {

    private static ResourceHandler<ItemResource> handlerFor(FakeItemStorage backing) {
        return NeoForgeItemStorage.asNeoForge(backing);
    }

    @Nested
    @DisplayName("staging")
    class Staging {

        @Test
        @DisplayName("an insert reaches the storage only when the root transaction commits")
        void insert_isStagedUntilRootCommit() {
            FakeItemStorage backing = new FakeItemStorage(100);
            ResourceHandler<ItemResource> handler = handlerFor(backing);

            try (Transaction tx = Transaction.openRoot()) {
                assertThat(handler.insert(0, DIAMOND, 40, tx)).isEqualTo(40);
                assertThat(backing.amountOf(DIAMOND_KEY)).isZero();
                tx.commit();
            }

            assertThat(backing.amountOf(DIAMOND_KEY)).isEqualTo(40);
        }

        @Test
        @DisplayName("an extract reaches the storage only when the root transaction commits")
        void extract_isStagedUntilRootCommit() {
            FakeItemStorage backing = new FakeItemStorage(100);
            backing.preload(DIAMOND_KEY, 80);
            ResourceHandler<ItemResource> handler = handlerFor(backing);

            try (Transaction tx = Transaction.openRoot()) {
                assertThat(handler.extract(0, DIAMOND, 30, tx)).isEqualTo(30);
                assertThat(backing.amountOf(DIAMOND_KEY)).isEqualTo(80);
                tx.commit();
            }

            assertThat(backing.amountOf(DIAMOND_KEY)).isEqualTo(50);
        }

        @Test
        @DisplayName("a nested commit inside an aborted transaction must not reach the storage")
        void nestedCommit_thenRootAbort_leavesStorageUntouched() {
            FakeItemStorage backing = new FakeItemStorage(100);
            ResourceHandler<ItemResource> handler = handlerFor(backing);

            try (Transaction root = Transaction.openRoot()) {
                try (Transaction nested = Transaction.open(root)) {
                    assertThat(handler.insert(0, DIAMOND, 25, nested)).isEqualTo(25);
                    nested.commit();
                }
                assertThat(backing.amountOf(DIAMOND_KEY)).isZero();
            }

            assertThat(backing.amountOf(DIAMOND_KEY)).isZero();
        }

        @Test
        @DisplayName("repeated inserts in one transaction must not offer more room than exists")
        void insert_respectsPendingDelta() {
            FakeItemStorage backing = new FakeItemStorage(100);
            ResourceHandler<ItemResource> handler = handlerFor(backing);

            try (Transaction tx = Transaction.openRoot()) {
                assertThat(handler.insert(0, DIAMOND, 60, tx)).isEqualTo(60);
                assertThat(handler.insert(0, DIAMOND, 60, tx)).isEqualTo(40);
                assertThat(handler.insert(0, DIAMOND, 60, tx)).isZero();
                tx.commit();
            }

            assertThat(backing.amountOf(DIAMOND_KEY)).isEqualTo(100);
        }

        @Test
        @DisplayName("repeated extracts in one transaction must not hand out the same items twice")
        void extract_respectsPendingDelta() {
            FakeItemStorage backing = new FakeItemStorage(100);
            backing.preload(DIAMOND_KEY, 100);
            ResourceHandler<ItemResource> handler = handlerFor(backing);

            try (Transaction tx = Transaction.openRoot()) {
                assertThat(handler.extract(0, DIAMOND, 30, tx)).isEqualTo(30);
                assertThat(handler.extract(0, DIAMOND, 90, tx)).isEqualTo(70);
                assertThat(handler.extract(0, DIAMOND, 10, tx)).isZero();
                tx.commit();
            }

            assertThat(backing.amountOf(DIAMOND_KEY)).isZero();
        }

        @Test
        @DisplayName("staged deltas are scoped per resource")
        void staging_isScopedPerResource() {
            FakeItemStorage backing = new FakeItemStorage(100);
            ResourceHandler<ItemResource> handler = handlerFor(backing);

            try (Transaction tx = Transaction.openRoot()) {
                assertThat(handler.insert(0, DIAMOND, 100, tx)).isEqualTo(100);
                assertThat(handler.insert(0, GOLD, 30, tx)).isEqualTo(30);
                tx.commit();
            }

            assertThat(backing.amountOf(DIAMOND_KEY)).isEqualTo(100);
            assertThat(backing.amountOf(GOLD_KEY)).isEqualTo(30);
        }
    }

    @Nested
    @DisplayName("reads inside a transaction")
    class Reads {

        @Test
        @DisplayName("the virtual slot reports a resource staged into an empty storage")
        void getResource_reportsResourceStagedIntoAnEmptyStorage() {
            // The backing storage shows nothing for the whole transaction, so a caller reading back
            // its own write sees an empty handler and a read-until-empty loop spins.
            FakeItemStorage backing = new FakeItemStorage(100);
            ResourceHandler<ItemResource> handler = handlerFor(backing);

            try (Transaction tx = Transaction.openRoot()) {
                handler.insert(0, DIAMOND, 15, tx);
                assertThat(handler.getResource(0)).isEqualTo(DIAMOND);
                assertThat(handler.getAmountAsLong(0)).isEqualTo(15);
                tx.commit();
            }
        }

        @Test
        @DisplayName("the virtual slot folds a staged insert into what the storage already holds")
        void getAmountAsLong_includesPendingInsert() {
            FakeItemStorage backing = new FakeItemStorage(100);
            backing.preload(DIAMOND_KEY, 10);
            ResourceHandler<ItemResource> handler = handlerFor(backing);

            try (Transaction tx = Transaction.openRoot()) {
                handler.insert(0, DIAMOND, 15, tx);
                assertThat(handler.getAmountAsLong(0)).isEqualTo(25);
                tx.commit();
            }
        }
    }

    @Nested
    @DisplayName("capacity")
    class Capacity {

        @Test
        @DisplayName("an empty storage reports the default slot capacity, not zero")
        void getCapacityAsLong_ofEmptyStorage_isTheDefaultSlotCapacity() {
            // ResourceHandlerUtil.isFull compares amount >= capacity, so a 0 here made an empty pipe
            // or engine fuel slot read as full and a hopper never inserted into one.
            ResourceHandler<ItemResource> handler = handlerFor(new FakeItemStorage(100));

            assertThat(handler.getCapacityAsLong(0, ItemResource.EMPTY)).isEqualTo(64);
        }

        @Test
        @DisplayName("a storage holding items reports what it could hold, not what is in it")
        void getCapacityAsLong_reportsTheStoredViewCapacity() {
            FakeItemStorage backing = new FakeItemStorage(100);
            backing.preload(DIAMOND_KEY, 5);
            ResourceHandler<ItemResource> handler = handlerFor(backing);

            assertThat(handler.getCapacityAsLong(0, DIAMOND)).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("validity")
    class Validity {

        @Test
        @DisplayName("the virtual slot does not accept an empty resource")
        void isValid_rejectsAnEmptyResource() {
            ResourceHandler<ItemResource> handler = handlerFor(new FakeItemStorage(100));

            assertThat(handler.isValid(0, ItemResource.EMPTY)).isFalse();
        }

        @Test
        @DisplayName("a full storage still accepts a resource it already holds")
        void isValid_ofFullStorage_acceptsWhatItHolds() {
            FakeItemStorage backing = new FakeItemStorage(100);
            backing.preload(DIAMOND_KEY, 100);
            ResourceHandler<ItemResource> handler = handlerFor(backing);

            assertThat(handler.isValid(0, DIAMOND)).isTrue();
        }

        @Test
        @DisplayName("a storage rejects a resource it would never take")
        void isValid_rejectsAResourceTheStorageFiltersOut() {
            // Empty and roomy, so only the storage's own filter can refuse it.
            FakeItemStorage backing = new FakeItemStorage(100);
            backing.acceptOnly(GOLD_KEY);
            ResourceHandler<ItemResource> handler = handlerFor(backing);

            assertThat(handler.isValid(0, DIAMOND)).isFalse();
            assertThat(handler.isValid(0, GOLD)).isTrue();
        }
    }

    @Test
    @DisplayName("the read methods reject any index but the virtual slot")
    void readMethods_rejectIndexesOtherThanZero() {
        ResourceHandler<ItemResource> handler = handlerFor(new FakeItemStorage(100));

        for (int index : new int[] {-1, 1}) {
            assertThatThrownBy(() -> handler.getResource(index)).isInstanceOf(IndexOutOfBoundsException.class);
            assertThatThrownBy(() -> handler.getAmountAsLong(index)).isInstanceOf(IndexOutOfBoundsException.class);
            assertThatThrownBy(() -> handler.getCapacityAsLong(index, DIAMOND))
                    .isInstanceOf(IndexOutOfBoundsException.class);
            assertThatThrownBy(() -> handler.isValid(index, DIAMOND)).isInstanceOf(IndexOutOfBoundsException.class);
        }
    }
}
