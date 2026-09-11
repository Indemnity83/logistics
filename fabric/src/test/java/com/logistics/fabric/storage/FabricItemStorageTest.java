package com.logistics.fabric.storage;

import com.logistics.core.lib.storage.IItemStorage;
import com.logistics.core.lib.storage.IItemView;
import com.logistics.core.lib.storage.ISlottedItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedSlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.logistics.fabric.storage.FabricStorageTestSupport.DIAMOND;
import static com.logistics.fabric.storage.FabricStorageTestSupport.GOLD;
import static com.logistics.fabric.storage.FabricStorageTestSupport.slot;
import static org.assertj.core.api.Assertions.assertThat;

/** {@code FabricItemStorage.wrap()}: a Fabric {@code Storage<ItemVariant>} seen as an {@link IItemStorage}. */
@DisplayName("FabricItemStorage.wrap()")
class FabricItemStorageTest {

    @SafeVarargs
    private static SlottedStorage<ItemVariant> slots(SingleVariantStorage<ItemVariant>... parts) {
        return new CombinedSlottedStorage<>(List.of(parts));
    }

    private static List<IItemView> contentsOf(IItemStorage storage) {
        List<IItemView> views = new ArrayList<>();
        storage.contents().forEach(views::add);
        return views;
    }

    @Nested
    @DisplayName("wrapping")
    class Wrapping {

        @Test
        @DisplayName("a null storage wraps to null")
        void wrap_null_returnsNull() {
            assertThat(FabricItemStorage.wrap(null)).isNull();
        }

        @Test
        @DisplayName("a slotted storage keeps its slot identity")
        void wrap_slottedStorage_exposesSlots() {
            IItemStorage wrapped = FabricItemStorage.wrap(slots(slot(64), slot(64), slot(64)));

            assertThat(wrapped).isInstanceOf(ISlottedItemStorage.class);
            assertThat(((ISlottedItemStorage) wrapped).slotCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("a storage without slots is not presented as slotted")
        void wrap_plainStorage_isNotSlotted() {
            // Slot identity is not part of the Storage contract; callers that reserve particular
            // slots must not be told they can when the backing storage cannot honour it.
            // A plain forwarding Storage: same transfers, no getSlot()/getSlotCount().
            IItemStorage wrapped =
                    FabricItemStorage.wrap(new FilteringStorage<>(slots(slot(64))) {});

            assertThat(wrapped).isNotNull();
            assertThat(wrapped).isNotInstanceOf(ISlottedItemStorage.class);
        }
    }

    @Nested
    @DisplayName("resource-scoped transfer")
    class ResourceTransfer {

        @Test
        @DisplayName("a simulated insert reports what would move but changes nothing")
        void insert_simulate_doesNotMutate() {
            SingleVariantStorage<ItemVariant> backing = slot(64);
            IItemStorage wrapped = FabricItemStorage.wrap(backing);

            assertThat(wrapped.insert(DIAMOND, 10, true)).isEqualTo(10);
            assertThat(backing.getAmount()).isZero();
        }

        @Test
        @DisplayName("a committed insert reaches the backing storage")
        void insert_commit_mutates() {
            SingleVariantStorage<ItemVariant> backing = slot(64);
            IItemStorage wrapped = FabricItemStorage.wrap(backing);

            assertThat(wrapped.insert(DIAMOND, 10, false)).isEqualTo(10);
            assertThat(backing.getAmount()).isEqualTo(10);
            assertThat(backing.getResource()).isEqualTo(DIAMOND.variant());
        }

        @Test
        @DisplayName("an insert is bounded by the room actually available")
        void insert_isBoundedByCapacity() {
            SingleVariantStorage<ItemVariant> backing = slot(16);
            IItemStorage wrapped = FabricItemStorage.wrap(backing);

            assertThat(wrapped.insert(DIAMOND, 100, false)).isEqualTo(16);
            assertThat(backing.getAmount()).isEqualTo(16);
        }

        @Test
        @DisplayName("a simulated extract reports what would move but changes nothing")
        void extract_simulate_doesNotMutate() {
            SingleVariantStorage<ItemVariant> backing = slot(64);
            IItemStorage wrapped = FabricItemStorage.wrap(backing);
            wrapped.insert(DIAMOND, 10, false);

            assertThat(wrapped.extract(DIAMOND, 4, true)).isEqualTo(4);
            assertThat(backing.getAmount()).isEqualTo(10);
        }

        @Test
        @DisplayName("a committed extract reaches the backing storage")
        void extract_commit_mutates() {
            SingleVariantStorage<ItemVariant> backing = slot(64);
            IItemStorage wrapped = FabricItemStorage.wrap(backing);
            wrapped.insert(DIAMOND, 10, false);

            assertThat(wrapped.extract(DIAMOND, 4, false)).isEqualTo(4);
            assertThat(backing.getAmount()).isEqualTo(6);
        }

        @Test
        @DisplayName("a resource the storage does not hold extracts nothing")
        void extract_wrongResource_movesNothing() {
            SingleVariantStorage<ItemVariant> backing = slot(64);
            IItemStorage wrapped = FabricItemStorage.wrap(backing);
            wrapped.insert(DIAMOND, 10, false);

            assertThat(wrapped.extract(GOLD, 4, false)).isZero();
            assertThat(backing.getAmount()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("contents()")
    class Contents {

        @Test
        @DisplayName("an entirely empty storage yields no views")
        void contents_ofEmptyStorage_isEmpty() {
            IItemStorage wrapped = FabricItemStorage.wrap(slots(slot(64), slot(64)));

            assertThat(contentsOf(wrapped)).isEmpty();
        }

        @Test
        @DisplayName("every non-empty slot is reported")
        void contents_reportsEachNonEmptySlot() {
            ISlottedItemStorage wrapped =
                    (ISlottedItemStorage) FabricItemStorage.wrap(slots(slot(64), slot(64), slot(64)));
            wrapped.insert(0, DIAMOND, 3, false);
            wrapped.insert(2, GOLD, 5, false);

            assertThat(contentsOf(wrapped))
                    .extracting(IItemView::resource, IItemView::amount)
                    .containsExactlyInAnyOrder(
                            org.assertj.core.groups.Tuple.tuple(DIAMOND, 3L),
                            org.assertj.core.groups.Tuple.tuple(GOLD, 5L));
        }
    }

    @Nested
    @DisplayName("slot-scoped access")
    class Slots {

        @Test
        @DisplayName("a filled slot reports its resource and amount")
        void slotView_filledSlot_reportsContents() {
            ISlottedItemStorage wrapped = (ISlottedItemStorage) FabricItemStorage.wrap(slots(slot(64), slot(64)));
            wrapped.insert(1, DIAMOND, 9, false);

            assertThat(wrapped.slotView(0)).isNull();
            IItemView view = wrapped.slotView(1);
            assertThat(view).isNotNull();
            assertThat(view.resource()).isEqualTo(DIAMOND);
            assertThat(view.amount()).isEqualTo(9);
        }

        @Test
        @DisplayName("an empty slot still reports the capacity it could hold")
        void slotCapacity_emptySlot_isNotZero() {
            // Deriving capacity from contents reports an empty slot as holding nothing ever.
            ISlottedItemStorage wrapped = (ISlottedItemStorage) FabricItemStorage.wrap(slots(slot(16), slot(48)));

            assertThat(wrapped.slotCapacity(0)).isEqualTo(16);
            assertThat(wrapped.slotCapacity(1)).isEqualTo(48);
        }

        @Test
        @DisplayName("a simulated slot transfer reports what would move but changes nothing")
        void slotTransfer_simulate_doesNotMutate() {
            // Slot insert and extract share one transfer() method, so one test covers both.
            SingleVariantStorage<ItemVariant> only = slot(64);
            ISlottedItemStorage wrapped = (ISlottedItemStorage) FabricItemStorage.wrap(slots(only));
            wrapped.insert(0, DIAMOND, 8, false);

            assertThat(wrapped.insert(0, DIAMOND, 5, true)).isEqualTo(5);
            assertThat(wrapped.extract(0, DIAMOND, 3, true)).isEqualTo(3);
            assertThat(only.getAmount()).isEqualTo(8);
        }

        @Test
        @DisplayName("a slot insert lands in the slot it names")
        void slotInsert_targetsThatSlot() {
            SingleVariantStorage<ItemVariant> first = slot(64);
            SingleVariantStorage<ItemVariant> second = slot(64);
            ISlottedItemStorage wrapped = (ISlottedItemStorage) FabricItemStorage.wrap(slots(first, second));

            assertThat(wrapped.insert(1, DIAMOND, 5, false)).isEqualTo(5);
            assertThat(first.getAmount()).isZero();
            assertThat(second.getAmount()).isEqualTo(5);
        }

        @Test
        @DisplayName("a committed slot extract takes from that slot")
        void slotExtract_commit_mutates() {
            SingleVariantStorage<ItemVariant> only = slot(64);
            ISlottedItemStorage wrapped = (ISlottedItemStorage) FabricItemStorage.wrap(slots(only));
            wrapped.insert(0, DIAMOND, 8, false);

            assertThat(wrapped.extract(0, DIAMOND, 3, false)).isEqualTo(3);
            assertThat(only.getAmount()).isEqualTo(5);
        }

        @Test
        @DisplayName("a non-positive slot transfer moves nothing and touches nothing")
        void slotTransfer_nonPositiveAmount_movesNothing() {
            // Fabric's StoragePreconditions throws on a negative amount, so the guard has to come
            // first — reaching the backing storage at all would be a crash, not a zero.
            SingleVariantStorage<ItemVariant> only = slot(64);
            ISlottedItemStorage wrapped = (ISlottedItemStorage) FabricItemStorage.wrap(slots(only));
            wrapped.insert(0, DIAMOND, 8, false);

            assertThat(wrapped.insert(0, DIAMOND, 0, false)).isZero();
            assertThat(wrapped.insert(0, DIAMOND, -5, false)).isZero();
            assertThat(wrapped.extract(0, DIAMOND, 0, false)).isZero();
            assertThat(wrapped.extract(0, DIAMOND, -5, false)).isZero();
            assertThat(only.getAmount()).isEqualTo(8);
        }
    }
}
