package com.logistics.neoforge.storage;

import static com.logistics.neoforge.storage.NeoForgeStorageTestSupport.DIAMOND;
import static com.logistics.neoforge.storage.NeoForgeStorageTestSupport.DIAMOND_KEY;
import static com.logistics.neoforge.storage.NeoForgeStorageTestSupport.GOLD;
import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.lib.storage.IItemStorage;
import com.logistics.core.lib.storage.IItemView;
import com.logistics.core.lib.storage.ISlottedItemStorage;
import com.logistics.neoforge.storage.NeoForgeStorageTestSupport.FakeResourceHandler;
import java.util.ArrayList;
import java.util.List;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The import direction: another mod's {@link net.neoforged.neoforge.transfer.ResourceHandler} seen as
 * one of our storages. What is wrong here mis-describes <em>their</em> inventory to our routing.
 */
@DisplayName("NeoForgeItemStorage.wrap")
class NeoForgeWrappedHandlerTest {

    private static ISlottedItemStorage wrap(FakeResourceHandler handler) {
        IItemStorage storage = NeoForgeItemStorage.wrap(handler);
        assertThat(storage).isInstanceOf(ISlottedItemStorage.class);
        return (ISlottedItemStorage) storage;
    }

    @Test
    @DisplayName("a slot reports what it can hold, not what is in it")
    void slotCapacityIsTheHandlersCapacity() {
        ISlottedItemStorage storage = wrap(new FakeResourceHandler().slot(DIAMOND, 7, 64));

        assertThat(storage.slotCapacity(0)).isEqualTo(64);
    }

    @Test
    @DisplayName("an empty slot still reports the room it has")
    void emptySlotReportsItsCapacity() {
        // slotView produces no view for an empty slot, so the default could only guess at 64 here.
        ISlottedItemStorage storage = wrap(new FakeResourceHandler().emptySlot(16));

        assertThat(storage.slotView(0)).isNull();
        assertThat(storage.slotCapacity(0)).isEqualTo(16);
    }

    @Test
    @DisplayName("a smaller-than-vanilla slot is not rounded up to a stack")
    void restrictedSlotKeepsItsOwnLimit() {
        ISlottedItemStorage storage =
                wrap(new FakeResourceHandler().slot(DIAMOND, 1, 1).emptySlot(4));

        assertThat(storage.slotCapacity(0)).isEqualTo(1);
        assertThat(storage.slotCapacity(1)).isEqualTo(4);
    }

    @Test
    @DisplayName("a full slot is distinguishable from a partly filled one")
    void fullAndPartialSlotsDiffer() {
        ISlottedItemStorage storage =
                wrap(new FakeResourceHandler().slot(DIAMOND, 64, 64).slot(GOLD, 1, 64));

        assertThat(storage.slotCapacity(0)).isEqualTo(storage.slotView(0).amount());
        assertThat(storage.slotCapacity(1)).isGreaterThan(storage.slotView(1).amount());
    }

    @Test
    @DisplayName("slot views carry the capacity too, not just the amount")
    void slotViewReportsCapacity() {
        ISlottedItemStorage storage = wrap(new FakeResourceHandler().slot(DIAMOND, 7, 64));

        IItemView view = storage.slotView(0);
        assertThat(view).isNotNull();
        assertThat(view.resource()).isEqualTo(DIAMOND_KEY);
        assertThat(view.amount()).isEqualTo(7);
        assertThat(view.capacity()).isEqualTo(64);
    }

    @Test
    @DisplayName("contents() views report each slot's own capacity")
    void contentsViewsReportTheirOwnCapacity() {
        // Two occupied slots with different capacities, separated by an empty one contents() skips --
        // so a view built against the wrong index would report the other slot's capacity.
        ISlottedItemStorage storage = wrap(new FakeResourceHandler()
                .slot(DIAMOND, 2, 8)
                .emptySlot(64)
                .slot(GOLD, 3, 32));

        List<IItemView> views = new ArrayList<>();
        storage.contents().forEach(views::add);

        assertThat(views).hasSize(2);
        assertThat(views.get(0).amount()).isEqualTo(2);
        assertThat(views.get(0).capacity()).isEqualTo(8);
        assertThat(views.get(1).amount()).isEqualTo(3);
        assertThat(views.get(1).capacity()).isEqualTo(32);
    }

    @Test
    @DisplayName("a handler under-reporting its capacity cannot produce negative free space")
    void capacityNeverFallsBelowTheAmountPresent() {
        // Third-party handlers are not obliged to honour capacity >= amount, and our view contract is.
        ISlottedItemStorage storage = wrap(new FakeResourceHandler().slot(DIAMOND, 10, 4));

        assertThat(storage.slotCapacity(0)).isEqualTo(10);
        assertThat(storage.slotView(0).capacity()).isEqualTo(10);
    }

    @Test
    @DisplayName("an empty handler wraps without reporting phantom slots")
    void emptyHandlerHasNoSlots() {
        ISlottedItemStorage storage = wrap(new FakeResourceHandler());

        assertThat(storage.slotCount()).isZero();
        assertThat(storage.contents()).isEmpty();
    }

    @Test
    @DisplayName("a null handler wraps to null rather than an empty storage")
    void nullHandlerWrapsToNull() {
        assertThat(NeoForgeItemStorage.wrap(null)).isNull();
    }

    @Test
    @DisplayName("an empty resource in a slot is not advertised as contents")
    void emptySlotsAreNotAdvertised() {
        ISlottedItemStorage storage = wrap(new FakeResourceHandler()
                .emptySlot(64)
                .slot(ItemResource.EMPTY, 5, 64)); // amount without a resource

        assertThat(storage.contents()).isEmpty();
    }
}
