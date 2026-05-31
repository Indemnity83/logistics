package com.logistics.neoforge.storage;

import com.logistics.core.lib.storage.IItemKey;
import com.logistics.core.lib.storage.IItemStorage;
import com.logistics.core.lib.storage.IItemView;
import com.logistics.core.lib.storage.ISlottedItemStorage;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("NeoForgeItemStorage adapter")
class NeoForgeItemStorageTest {

    @Test
    @DisplayName("slotted common storage exposes its slot count")
    void slottedCommonStorage_exposesSlotCount() {
        IItemHandler handler = NeoForgeItemStorage.asNeoForge(new EmptySlottedStorage(3));

        assertThat(handler.getSlots()).isEqualTo(3);
        assertThat(handler.getStackInSlot(0)).isEqualTo(ItemStack.EMPTY);
        assertThat(handler.getStackInSlot(2)).isEqualTo(ItemStack.EMPTY);
    }

    @ParameterizedTest
    @MethodSource("emptyHandlerFactories")
    @DisplayName("handlers reject out-of-range transfer indexes")
    void outOfRangeTransferIndexes(Supplier<IItemHandler> handlerFactory) {
        IItemHandler handler = handlerFactory.get();

        assertThatThrownBy(() -> handler.insertItem(-1, ItemStack.EMPTY, false))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> handler.insertItem(handler.getSlots(), ItemStack.EMPTY, false))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> handler.extractItem(-1, 1, false))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> handler.extractItem(handler.getSlots(), 1, false))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    @DisplayName("non-slotted common storage remains one virtual slot")
    void nonSlottedStorage_remainsOneVirtualSlot() {
        IItemHandler handler = NeoForgeItemStorage.asNeoForge(new EmptyStorage());

        assertThat(handler.getSlots()).isEqualTo(1);
        assertThat(handler.getStackInSlot(0)).isEqualTo(ItemStack.EMPTY);
    }

    private static Stream<Supplier<IItemHandler>> emptyHandlerFactories() {
        return Stream.of(
                () -> NeoForgeItemStorage.asNeoForge(new EmptySlottedStorage(1)),
                () -> NeoForgeItemStorage.asNeoForge(new EmptyStorage()));
    }

    private static class EmptyStorage implements IItemStorage {
        @Override
        public long insert(IItemKey item, long maxAmount, boolean simulate) {
            return 0;
        }

        @Override
        public long extract(IItemKey item, long maxAmount, boolean simulate) {
            return 0;
        }

        @Override
        public Iterable<IItemView> contents() {
            return java.util.List.of();
        }
    }

    private static final class EmptySlottedStorage extends EmptyStorage implements ISlottedItemStorage {
        private final int slots;

        private EmptySlottedStorage(int slots) {
            this.slots = slots;
        }

        @Override
        public int slotCount() {
            return slots;
        }

        @Override
        public IItemView slotView(int slot) {
            return null;
        }

        @Override
        public long insert(int slot, IItemKey item, long maxAmount, boolean simulate) {
            return 0;
        }

        @Override
        public long extract(int slot, IItemKey item, long maxAmount, boolean simulate) {
            return 0;
        }
    }
}
