package com.logistics.fabric.storage;

import com.logistics.core.lib.storage.IItemKey;
import com.logistics.core.lib.storage.IItemStorage;
import com.logistics.core.lib.storage.IItemView;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared fixtures for the Fabric item-storage adapter tests.
 *
 * <p>{@code ItemVariant.of()} resolves through a mixin-injected cache on {@code Item}, so these
 * tests only work under the {@code fabric-loader-junit} launcher, which applies mixins. Data
 * components are never bound here, so {@code ItemVariant.toStack()} is unavailable — the fixtures
 * stay on the variant/{@link FabricItemKey} side of the adapter.
 */
final class FabricStorageTestSupport {

    static {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    // Declared after the bootstrap block on purpose: static initializers run in textual order, so
    // touching either key from a test class bootstraps the registries before Items is resolved.
    // A test class holding its own `key(Items.X)` constant would evaluate Items.X first and crash.
    static final FabricItemKey DIAMOND = new FabricItemKey(ItemVariant.of(Items.DIAMOND));
    static final FabricItemKey GOLD = new FabricItemKey(ItemVariant.of(Items.GOLD_INGOT));

    private FabricStorageTestSupport() {}

    /** A real Fabric single-slot storage, so transaction behaviour comes from the API and not a fake. */
    static SingleVariantStorage<ItemVariant> slot(long capacity) {
        return new SingleVariantStorage<>() {
            @Override
            protected ItemVariant getBlankVariant() {
                return ItemVariant.blank();
            }

            @Override
            protected long getCapacity(ItemVariant variant) {
                return capacity;
            }
        };
    }

    /**
     * Stateful {@link IItemStorage} fake: per-key totals bounded by a shared per-key capacity.
     *
     * <p>Stateful on purpose — a fake that answers every simulation identically cannot show whether
     * the adapter accounts for what it has already staged in the current transaction.
     */
    static final class FakeItemStorage implements IItemStorage {

        private final Map<IItemKey, Long> stored = new LinkedHashMap<>();
        private final long capacityPerKey;
        private final long viewChunk;

        FakeItemStorage(long capacityPerKey) {
            this(capacityPerKey, capacityPerKey);
        }

        /**
         * @param viewChunk largest amount a single view may report, so one key can span several
         *                  views the way an item spread over two chest slots does
         */
        FakeItemStorage(long capacityPerKey, long viewChunk) {
            this.capacityPerKey = capacityPerKey;
            this.viewChunk = viewChunk;
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
                for (long left = amount; left > 0; left -= viewChunk) {
                    views.add(view(k, Math.min(left, viewChunk), viewChunk));
                }
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
