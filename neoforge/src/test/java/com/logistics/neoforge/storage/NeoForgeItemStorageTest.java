package com.logistics.neoforge.storage;

import com.logistics.core.lib.storage.IItemKey;
import com.logistics.core.lib.storage.IItemStorage;
import com.logistics.core.lib.storage.IItemView;
import com.logistics.core.lib.storage.ISlottedItemStorage;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NeoForgeItemStorage adapter")
class NeoForgeItemStorageTest {

    @Test
    @DisplayName("slotted common storage exposes its slot count")
    void slottedCommonStorage_exposesSlotCount() {
        ResourceHandler<ItemResource> handler = NeoForgeItemStorage.asNeoForge(new EmptySlottedStorage(3));

        assertThat(handler.size()).isEqualTo(3);
        assertThat(handler.getResource(0)).isEqualTo(ItemResource.EMPTY);
        assertThat(handler.getAmountAsLong(0)).isZero();
        assertThat(handler.getResource(2)).isEqualTo(ItemResource.EMPTY);
        assertThat(handler.getAmountAsLong(2)).isZero();
    }

    @Test
    @DisplayName("non-slotted common storage remains one virtual slot")
    void nonSlottedStorage_remainsOneVirtualSlot() {
        ResourceHandler<ItemResource> handler = NeoForgeItemStorage.asNeoForge(new EmptyStorage());

        assertThat(handler.size()).isEqualTo(1);
        assertThat(handler.getResource(0)).isEqualTo(ItemResource.EMPTY);
        assertThat(handler.getAmountAsLong(0)).isZero();
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
