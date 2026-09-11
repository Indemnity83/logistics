package com.logistics.fabric.storage;

import com.logistics.core.lib.storage.IItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.logistics.fabric.storage.FabricStorageTestSupport.DIAMOND;
import static com.logistics.fabric.storage.FabricStorageTestSupport.FakeItemStorage;
import static com.logistics.fabric.storage.FabricStorageTestSupport.GOLD;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code FabricItemStorage.asFabric()}: an {@link IItemStorage} exposed to Fabric's transfer API.
 *
 * <p>This is the direction with real logic in it. Fabric transactions nest and may abort, while
 * {@link IItemStorage} has only a simulate flag, so the adapter stages every transfer as a pending
 * delta and flushes it once, on the outermost commit. Nothing here asserts against the staging
 * map — every test drives real {@link Transaction}s and checks the backing storage.
 */
@DisplayName("FabricItemStorage.asFabric()")
class FabricStagedStorageTest {

    private static List<StorageView<ItemVariant>> viewsOf(Storage<ItemVariant> storage) {
        List<StorageView<ItemVariant>> views = new ArrayList<>();
        storage.iterator().forEachRemaining(views::add);
        return views;
    }

    @Test
    @DisplayName("a null storage is exposed as null")
    void asFabric_null_returnsNull() {
        assertThat(FabricItemStorage.asFabric(null)).isNull();
    }

    @Nested
    @DisplayName("insertion")
    class Insertion {

        @Test
        @DisplayName("an insert reaches the storage only when the outermost transaction commits")
        void insert_isStagedUntilCommit() {
            FakeItemStorage backing = new FakeItemStorage(100);
            Storage<ItemVariant> staged = FabricItemStorage.asFabric(backing);

            try (Transaction tx = Transaction.openOuter()) {
                assertThat(staged.insert(DIAMOND.variant(), 40, tx)).isEqualTo(40);
                assertThat(backing.amountOf(DIAMOND)).isZero();
                tx.commit();
            }

            assertThat(backing.amountOf(DIAMOND)).isEqualTo(40);
        }

        @Test
        @DisplayName("a nested commit inside an aborted transaction must not reach the storage")
        void nestedCommit_thenOuterAbort_leavesStorageUntouched() {
            // Fabric helpers routinely wrap their work in a nested transaction and commit it. If
            // that nested commit wrote through, a later abort of the enclosing transaction could
            // not undo it and the items would be duplicated.
            FakeItemStorage backing = new FakeItemStorage(100);
            Storage<ItemVariant> staged = FabricItemStorage.asFabric(backing);

            try (Transaction outer = Transaction.openOuter()) {
                try (Transaction nested = outer.openNested()) {
                    assertThat(staged.insert(DIAMOND.variant(), 25, nested)).isEqualTo(25);
                    nested.commit();
                }
                assertThat(backing.amountOf(DIAMOND)).isZero();
                outer.abort();
            }

            assertThat(backing.amountOf(DIAMOND)).isZero();
        }

        @Test
        @DisplayName("a nested commit is applied once, when the outermost transaction commits")
        void nestedCommit_thenOuterCommit_appliesOnce() {
            FakeItemStorage backing = new FakeItemStorage(100);
            Storage<ItemVariant> staged = FabricItemStorage.asFabric(backing);

            try (Transaction outer = Transaction.openOuter()) {
                try (Transaction nested = outer.openNested()) {
                    assertThat(staged.insert(DIAMOND.variant(), 25, nested)).isEqualTo(25);
                    nested.commit();
                }
                outer.commit();
            }

            assertThat(backing.amountOf(DIAMOND)).isEqualTo(25);
        }

        @Test
        @DisplayName("an aborted nested transaction rolls back even when the outer one commits")
        void nestedAbort_thenOuterCommit_appliesNothing() {
            FakeItemStorage backing = new FakeItemStorage(100);
            Storage<ItemVariant> staged = FabricItemStorage.asFabric(backing);

            try (Transaction outer = Transaction.openOuter()) {
                try (Transaction nested = outer.openNested()) {
                    assertThat(staged.insert(DIAMOND.variant(), 25, nested)).isEqualTo(25);
                    nested.abort();
                }
                outer.commit();
            }

            assertThat(backing.amountOf(DIAMOND)).isZero();
        }

        @Test
        @DisplayName("repeated inserts in one transaction must not offer more room than exists")
        void insert_respectsPendingDelta() {
            // The storage sees nothing until the commit, so every simulation inside the transaction
            // reports the same empty storage. Only the staged delta stops the adapter promising the
            // same 100 items of room over and over and overfilling on flush.
            FakeItemStorage backing = new FakeItemStorage(100);
            Storage<ItemVariant> staged = FabricItemStorage.asFabric(backing);

            try (Transaction tx = Transaction.openOuter()) {
                assertThat(staged.insert(DIAMOND.variant(), 60, tx)).isEqualTo(60);
                assertThat(staged.insert(DIAMOND.variant(), 60, tx)).isEqualTo(40);
                assertThat(staged.insert(DIAMOND.variant(), 60, tx)).isZero();
                tx.commit();
            }

            assertThat(backing.amountOf(DIAMOND)).isEqualTo(100);
        }

        @Test
        @DisplayName("each resource is staged independently")
        void insert_staysPerResource() {
            FakeItemStorage backing = new FakeItemStorage(100);
            Storage<ItemVariant> staged = FabricItemStorage.asFabric(backing);

            try (Transaction tx = Transaction.openOuter()) {
                assertThat(staged.insert(DIAMOND.variant(), 100, tx)).isEqualTo(100);
                assertThat(staged.insert(GOLD.variant(), 30, tx)).isEqualTo(30);
                tx.commit();
            }

            assertThat(backing.amountOf(DIAMOND)).isEqualTo(100);
            assertThat(backing.amountOf(GOLD)).isEqualTo(30);
        }
    }

    @Nested
    @DisplayName("extraction")
    class Extraction {

        @Test
        @DisplayName("an extract reaches the storage only when the outermost transaction commits")
        void extract_isStagedUntilCommit() {
            FakeItemStorage backing = new FakeItemStorage(100);
            backing.preload(DIAMOND, 80);
            Storage<ItemVariant> staged = FabricItemStorage.asFabric(backing);

            try (Transaction tx = Transaction.openOuter()) {
                assertThat(staged.extract(DIAMOND.variant(), 30, tx)).isEqualTo(30);
                assertThat(backing.amountOf(DIAMOND)).isEqualTo(80);
                tx.commit();
            }

            assertThat(backing.amountOf(DIAMOND)).isEqualTo(50);
        }

        @Test
        @DisplayName("repeated extracts in one transaction must not hand out the same items twice")
        void extract_respectsPendingDelta() {
            FakeItemStorage backing = new FakeItemStorage(100);
            backing.preload(DIAMOND, 100);
            Storage<ItemVariant> staged = FabricItemStorage.asFabric(backing);

            try (Transaction tx = Transaction.openOuter()) {
                assertThat(staged.extract(DIAMOND.variant(), 30, tx)).isEqualTo(30);
                assertThat(staged.extract(DIAMOND.variant(), 90, tx)).isEqualTo(70);
                assertThat(staged.extract(DIAMOND.variant(), 10, tx)).isZero();
                tx.commit();
            }

            assertThat(backing.amountOf(DIAMOND)).isZero();
        }

        @Test
        @DisplayName("items staged into the storage can be taken back out in the same transaction")
        void insertThenExtract_inOneTransaction_netsOut() {
            // The backing storage holds nothing, so this only works if the pending insert is
            // visible to the extract. It nets to zero, so the commit must write nothing at all.
            FakeItemStorage backing = new FakeItemStorage(100);
            Storage<ItemVariant> staged = FabricItemStorage.asFabric(backing);

            try (Transaction tx = Transaction.openOuter()) {
                assertThat(staged.insert(DIAMOND.variant(), 40, tx)).isEqualTo(40);
                assertThat(staged.extract(DIAMOND.variant(), 40, tx)).isEqualTo(40);
                tx.commit();
            }

            assertThat(backing.amountOf(DIAMOND)).isZero();
        }
    }

    @Nested
    @DisplayName("iteration")
    class Iteration {

        @Test
        @DisplayName("a view reports what the slot could hold, not what is in it")
        void view_reportsCapacityNotAmount() {
            // Fabric's StorageView contract asks for the total the view could hold. Reporting the
            // amount instead makes every view look exactly full, so a comparator reading one of
            // our machines sees 15 on the first item.
            FakeItemStorage backing = new FakeItemStorage(64);
            backing.preload(DIAMOND, 5);
            Storage<ItemVariant> staged = FabricItemStorage.asFabric(backing);

            List<StorageView<ItemVariant>> views = viewsOf(staged);

            assertThat(views).hasSize(1);
            assertThat(views.getFirst().getAmount()).isEqualTo(5);
            assertThat(views.getFirst().getCapacity()).isEqualTo(64);
            assertThat(views.getFirst().getResource()).isEqualTo(DIAMOND.variant());
        }

        @Test
        @DisplayName("an extract through a view is staged like any other")
        void view_extract_isStagedUntilCommit() {
            FakeItemStorage backing = new FakeItemStorage(64);
            backing.preload(DIAMOND, 50);
            Storage<ItemVariant> staged = FabricItemStorage.asFabric(backing);

            try (Transaction tx = Transaction.openOuter()) {
                StorageView<ItemVariant> view = viewsOf(staged).getFirst();
                assertThat(view.extract(DIAMOND.variant(), 20, tx)).isEqualTo(20);
                assertThat(backing.amountOf(DIAMOND)).isEqualTo(50);
                tx.commit();
            }

            assertThat(backing.amountOf(DIAMOND)).isEqualTo(30);
        }

        @Test
        @DisplayName("an extract through a view is bounded by what that view holds")
        void view_extract_isBoundedByViewAmount() {
            // 100 items spread over two views of 50, the way a chest splits a stack across slots.
            // Extracting through one view must take that view's 50, not the storage's whole 100 —
            // the storage has plenty left, so only the view's own amount can bound this.
            FakeItemStorage backing = new FakeItemStorage(200, 50);
            backing.preload(DIAMOND, 100);
            Storage<ItemVariant> staged = FabricItemStorage.asFabric(backing);

            assertThat(viewsOf(staged)).hasSize(2);

            try (Transaction tx = Transaction.openOuter()) {
                assertThat(viewsOf(staged).getFirst().extract(DIAMOND.variant(), 500, tx))
                        .isEqualTo(50);
                tx.commit();
            }

            assertThat(backing.amountOf(DIAMOND)).isEqualTo(50);
        }

        @Test
        @DisplayName("a view refuses to extract a resource it does not hold")
        void view_extract_wrongResource_movesNothing() {
            // The storage does hold gold, just not in this view. Only the view's own resource
            // check can refuse here — the storage-wide availability check would happily allow it.
            FakeItemStorage backing = new FakeItemStorage(64);
            backing.preload(DIAMOND, 50);
            backing.preload(GOLD, 40);
            Storage<ItemVariant> staged = FabricItemStorage.asFabric(backing);

            try (Transaction tx = Transaction.openOuter()) {
                StorageView<ItemVariant> diamondView = viewsOf(staged).getFirst();
                assertThat(diamondView.getResource()).isEqualTo(DIAMOND.variant());
                assertThat(diamondView.extract(GOLD.variant(), 10, tx)).isZero();
                tx.commit();
            }

            assertThat(backing.amountOf(DIAMOND)).isEqualTo(50);
            assertThat(backing.amountOf(GOLD)).isEqualTo(40);
        }

        @Test
        @DisplayName("an empty storage exposes no views")
        void iterator_ofEmptyStorage_isEmpty() {
            Storage<ItemVariant> staged = FabricItemStorage.asFabric(new FakeItemStorage(64));

            assertThat(viewsOf(staged)).isEmpty();
        }
    }
}
