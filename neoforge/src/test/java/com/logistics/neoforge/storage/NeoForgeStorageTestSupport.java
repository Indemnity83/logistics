package com.logistics.neoforge.storage;

import com.logistics.core.lib.storage.IItemKey;
import com.logistics.core.lib.storage.IItemStorage;
import com.logistics.core.lib.storage.IItemView;
import com.logistics.core.lib.storage.ISlottedItemStorage;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared fixtures for the NeoForge item-storage adapter tests.
 *
 * <p>Minecraft 26.2 binds an item's default data components during a datapack reload, which a plain
 * JUnit run never performs, so {@code ItemResource.of()} fails with "Components not bound yet" when
 * it builds the item's default stack. {@link #bound} binds a minimal map for the handful of items
 * these fixtures use.
 */
final class NeoForgeStorageTestSupport {

    static {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    // Declared after the bootstrap block on purpose: static initializers run in textual order, so
    // touching either resource from a test class bootstraps the registries before Items is resolved.
    static final ItemResource DIAMOND = bound(Items.DIAMOND);
    static final ItemResource GOLD = bound(Items.GOLD_INGOT);

    static final IItemKey DIAMOND_KEY = new NeoForgeItemKey(DIAMOND);
    static final IItemKey GOLD_KEY = new NeoForgeItemKey(GOLD);

    private NeoForgeStorageTestSupport() {}

    private static ItemResource bound(Item item) {
        item.builtInRegistryHolder()
                .bindComponents(DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build());
        return ItemResource.of(item);
    }

    /**
     * Stateful {@link ResourceHandler} fake — the direction other mods' inventories arrive from.
     *
     * <p>Capacity is deliberately independent of contents and of the queried resource, which is what
     * NeoForge's contract promises, so a test can tell a real capacity apart from the amount present.
     */
    static final class FakeResourceHandler implements ResourceHandler<ItemResource> {

        private final List<ItemResource> resources = new ArrayList<>();
        private final List<Long> amounts = new ArrayList<>();
        private final List<Long> capacities = new ArrayList<>();

        /** Adds a slot holding {@code amount} of {@code resource} out of {@code capacity}. */
        FakeResourceHandler slot(ItemResource resource, long amount, long capacity) {
            resources.add(resource);
            amounts.add(amount);
            capacities.add(capacity);
            return this;
        }

        /** Adds an empty slot that could still hold {@code capacity}. */
        FakeResourceHandler emptySlot(long capacity) {
            return slot(ItemResource.EMPTY, 0, capacity);
        }

        @Override
        public int size() {
            return resources.size();
        }

        @Override
        public ItemResource getResource(int index) {
            return resources.get(index);
        }

        @Override
        public long getAmountAsLong(int index) {
            return amounts.get(index);
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            return capacities.get(index);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return !resource.isEmpty();
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return 0;
        }
    }

    /**
     * Stateful {@link ISlottedItemStorage} fake: one item type per slot, bounded by a per-slot
     * capacity, with an optional per-slot filter so validity can be distinguished from room.
     *
     * <p>Stateful on purpose — a fake that answers every simulation identically cannot show whether
     * the adapter accounts for what it has already staged in the current transaction.
     */
    static final class FakeSlottedStorage implements ISlottedItemStorage {

        private final Slot[] slots;

        FakeSlottedStorage(int slotCount, long capacity) {
            this.slots = new Slot[slotCount];
            for (int i = 0; i < slotCount; i++) {
                slots[i] = new Slot(capacity);
            }
        }

        void preload(int slot, IItemKey item, long amount) {
            slots[slot].item = item;
            slots[slot].amount = amount;
        }

        /** Restrict a slot to one item type, the way a machine's fuel slot accepts only fuel. */
        void acceptOnly(int slot, IItemKey item) {
            slots[slot].filter = item;
        }

        long amountOf(int slot) {
            return slots[slot].amount;
        }

        @Nullable
        IItemKey itemOf(int slot) {
            return slots[slot].item;
        }

        @Override
        public int slotCount() {
            return slots.length;
        }

        @Override
        @Nullable
        public IItemView slotView(int slot) {
            Slot s = slots[slot];
            return s.amount > 0 && s.item != null ? view(s.item, s.amount, s.capacity) : null;
        }

        @Override
        public long slotCapacity(int slot) {
            return slots[slot].capacity;
        }

        @Override
        public long insert(int slot, IItemKey item, long maxAmount, boolean simulate) {
            if (maxAmount <= 0) return 0;
            Slot s = slots[slot];
            if (s.filter != null && !s.filter.equals(item)) return 0;
            if (s.item != null && s.amount > 0 && !s.item.equals(item)) return 0;
            long moved = Math.min(maxAmount, s.capacity - s.amount);
            if (moved <= 0) return 0;
            if (!simulate) {
                s.item = item;
                s.amount += moved;
            }
            return moved;
        }

        @Override
        public long extract(int slot, IItemKey item, long maxAmount, boolean simulate) {
            if (maxAmount <= 0) return 0;
            Slot s = slots[slot];
            if (s.item == null || !s.item.equals(item)) return 0;
            long moved = Math.min(maxAmount, s.amount);
            if (moved <= 0) return 0;
            if (!simulate) {
                s.amount -= moved;
                if (s.amount == 0) s.item = null;
            }
            return moved;
        }

        @Override
        public long insert(IItemKey item, long maxAmount, boolean simulate) {
            long total = 0;
            for (int i = 0; i < slots.length && total < maxAmount; i++) {
                total += insert(i, item, maxAmount - total, simulate);
            }
            return total;
        }

        @Override
        public long extract(IItemKey item, long maxAmount, boolean simulate) {
            long total = 0;
            for (int i = 0; i < slots.length && total < maxAmount; i++) {
                total += extract(i, item, maxAmount - total, simulate);
            }
            return total;
        }

        @Override
        public Iterable<IItemView> contents() {
            List<IItemView> views = new ArrayList<>();
            for (int i = 0; i < slots.length; i++) {
                IItemView v = slotView(i);
                if (v != null) views.add(v);
            }
            return views;
        }

        private static final class Slot {
            private final long capacity;
            @Nullable private IItemKey item;
            private long amount;
            @Nullable private IItemKey filter;

            private Slot(long capacity) {
                this.capacity = capacity;
            }
        }
    }

    /** Stateful non-slotted {@link IItemStorage} fake: per-key totals bounded by a shared capacity. */
    static final class FakeItemStorage implements IItemStorage {

        private final Map<IItemKey, Long> stored = new LinkedHashMap<>();
        private final long capacityPerKey;
        @Nullable private IItemKey filter;

        FakeItemStorage(long capacityPerKey) {
            this.capacityPerKey = capacityPerKey;
        }

        /** Restrict the storage to one item type, so validity can be distinguished from room. */
        void acceptOnly(IItemKey item) {
            this.filter = item;
        }

        long amountOf(IItemKey key) {
            return stored.getOrDefault(key, 0L);
        }

        void preload(IItemKey key, long amount) {
            stored.put(key, amount);
        }

        @Override
        public long insert(IItemKey item, long maxAmount, boolean simulate) {
            if (maxAmount <= 0) return 0;
            if (filter != null && !filter.equals(item)) return 0;
            long moved = Math.min(maxAmount, capacityPerKey - amountOf(item));
            if (moved <= 0) return 0;
            if (!simulate) stored.merge(item, moved, Long::sum);
            return moved;
        }

        @Override
        public long extract(IItemKey item, long maxAmount, boolean simulate) {
            if (maxAmount <= 0) return 0;
            long moved = Math.min(maxAmount, amountOf(item));
            if (moved <= 0) return 0;
            if (!simulate) stored.merge(item, -moved, Long::sum);
            return moved;
        }

        @Override
        public Iterable<IItemView> contents() {
            List<IItemView> views = new ArrayList<>();
            stored.forEach((k, amount) -> {
                if (amount > 0) views.add(view(k, amount, capacityPerKey));
            });
            return views;
        }
    }

    static IItemView view(IItemKey key, long amount, long capacity) {
        return new IItemView() {
            @Override
            public IItemKey resource() {
                return key;
            }

            @Override
            public long amount() {
                return amount;
            }

            @Override
            public long capacity() {
                return capacity;
            }
        };
    }
}
