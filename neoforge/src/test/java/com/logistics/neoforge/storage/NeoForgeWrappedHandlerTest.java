package com.logistics.neoforge.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.lib.storage.IItemStorage;
import com.logistics.core.lib.storage.IItemView;
import com.logistics.core.lib.storage.ISlottedItemStorage;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.IItemHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The import direction: another mod's {@link IItemHandler} seen as one of our storages. What is wrong
 * here mis-describes <em>their</em> inventory to our routing.
 *
 * <p>This branch wraps the old {@code IItemHandler} capability rather than the transfer-API
 * {@code ResourceHandler}, so capacity comes from {@code getSlotLimit} rather than
 * {@code getCapacityAsLong}, and the fixtures are local rather than shared.
 */
@DisplayName("NeoForgeItemStorage.wrap")
class NeoForgeWrappedHandlerTest {

    static {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    /**
     * {@link IItemHandler} fake whose per-slot limit is independent of its contents, so a test can tell
     * a real capacity apart from the amount present.
     */
    private static final class FakeItemHandler implements IItemHandler {

        private final List<ItemStack> stacks = new ArrayList<>();
        private final List<Integer> limits = new ArrayList<>();

        FakeItemHandler slot(ItemStack stack, int limit) {
            stacks.add(stack);
            limits.add(limit);
            return this;
        }

        FakeItemHandler emptySlot(int limit) {
            return slot(ItemStack.EMPTY, limit);
        }

        @Override
        public int getSlots() {
            return stacks.size();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return stacks.get(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return limits.get(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
        }
    }

    private static ISlottedItemStorage wrap(FakeItemHandler handler) {
        IItemStorage storage = NeoForgeItemStorage.wrap(handler);
        assertThat(storage).isInstanceOf(ISlottedItemStorage.class);
        return (ISlottedItemStorage) storage;
    }

    private static ItemStack diamonds(int count) {
        return new ItemStack(Items.DIAMOND, count);
    }

    private static ItemStack gold(int count) {
        return new ItemStack(Items.GOLD_INGOT, count);
    }

    @Test
    @DisplayName("a slot reports what it can hold, not what is in it")
    void slotCapacityIsTheHandlersLimit() {
        ISlottedItemStorage storage = wrap(new FakeItemHandler().slot(diamonds(7), 64));

        assertThat(storage.slotCapacity(0)).isEqualTo(64);
    }

    @Test
    @DisplayName("an empty slot still reports the room it has")
    void emptySlotReportsItsCapacity() {
        // slotView produces no view for an empty slot, so the default could only guess at 64 here.
        ISlottedItemStorage storage = wrap(new FakeItemHandler().emptySlot(16));

        assertThat(storage.slotView(0)).isNull();
        assertThat(storage.slotCapacity(0)).isEqualTo(16);
    }

    @Test
    @DisplayName("a smaller-than-vanilla slot is not rounded up to a stack")
    void restrictedSlotKeepsItsOwnLimit() {
        ISlottedItemStorage storage = wrap(new FakeItemHandler().slot(diamonds(1), 1).emptySlot(4));

        assertThat(storage.slotCapacity(0)).isEqualTo(1);
        assertThat(storage.slotCapacity(1)).isEqualTo(4);
    }

    @Test
    @DisplayName("a full slot is distinguishable from a partly filled one")
    void fullAndPartialSlotsDiffer() {
        ISlottedItemStorage storage = wrap(new FakeItemHandler().slot(diamonds(64), 64).slot(gold(1), 64));

        assertThat(storage.slotCapacity(0)).isEqualTo(storage.slotView(0).amount());
        assertThat(storage.slotCapacity(1)).isGreaterThan(storage.slotView(1).amount());
    }

    @Test
    @DisplayName("slot views carry the capacity too, not just the amount")
    void slotViewReportsCapacity() {
        ISlottedItemStorage storage = wrap(new FakeItemHandler().slot(diamonds(7), 64));

        IItemView view = storage.slotView(0);
        assertThat(view).isNotNull();
        assertThat(view.amount()).isEqualTo(7);
        assertThat(view.capacity()).isEqualTo(64);
    }

    @Test
    @DisplayName("contents() views report each slot's own capacity")
    void contentsViewsReportTheirOwnCapacity() {
        // Two occupied slots with different limits, separated by an empty one contents() skips --
        // so a view built against the wrong index would report the other slot's capacity.
        ISlottedItemStorage storage =
                wrap(new FakeItemHandler().slot(diamonds(2), 8).emptySlot(64).slot(gold(3), 32));

        List<IItemView> views = new ArrayList<>();
        storage.contents().forEach(views::add);

        assertThat(views).hasSize(2);
        assertThat(views.get(0).amount()).isEqualTo(2);
        assertThat(views.get(0).capacity()).isEqualTo(8);
        assertThat(views.get(1).amount()).isEqualTo(3);
        assertThat(views.get(1).capacity()).isEqualTo(32);
    }

    @Test
    @DisplayName("a handler under-reporting its limit cannot produce negative free space")
    void capacityNeverFallsBelowTheAmountPresent() {
        // Third-party handlers are not obliged to honour capacity >= amount, and our view contract is.
        ISlottedItemStorage storage = wrap(new FakeItemHandler().slot(diamonds(10), 4));

        assertThat(storage.slotCapacity(0)).isEqualTo(10);
        assertThat(storage.slotView(0).capacity()).isEqualTo(10);
    }

    @Test
    @DisplayName("an empty handler wraps without reporting phantom slots")
    void emptyHandlerHasNoSlots() {
        ISlottedItemStorage storage = wrap(new FakeItemHandler());

        assertThat(storage.slotCount()).isZero();
        assertThat(storage.contents()).isEmpty();
    }

    @Test
    @DisplayName("a null handler wraps to null rather than an empty storage")
    void nullHandlerWrapsToNull() {
        assertThat(NeoForgeItemStorage.wrap(null)).isNull();
    }

    @Test
    @DisplayName("empty slots are not advertised as contents")
    void emptySlotsAreNotAdvertised() {
        ISlottedItemStorage storage = wrap(new FakeItemHandler().emptySlot(64).emptySlot(16));

        assertThat(storage.contents()).isEmpty();
    }
}
