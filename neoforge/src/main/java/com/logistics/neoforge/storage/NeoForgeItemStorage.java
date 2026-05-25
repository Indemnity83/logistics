package com.logistics.neoforge.storage;

import com.logistics.core.lib.storage.IItemKey;
import com.logistics.core.lib.storage.IItemStorage;
import com.logistics.core.lib.storage.IItemView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

public final class NeoForgeItemStorage implements IItemStorage {
    private final ResourceHandler<ItemResource> handler;

    private NeoForgeItemStorage(ResourceHandler<ItemResource> handler) {
        this.handler = handler;
    }

    @Nullable
    public static IItemStorage wrap(@Nullable ResourceHandler<ItemResource> handler) {
        return handler == null ? null : new NeoForgeItemStorage(handler);
    }

    @Nullable
    public static ResourceHandler<ItemResource> asNeoForge(@Nullable IItemStorage storage) {
        return storage == null ? null : new CommonItemHandler(storage);
    }

    @Override
    public long insert(IItemKey item, long maxAmount, boolean simulate) {
        ItemResource resource = toResource(item);
        if (resource.isEmpty() || maxAmount <= 0) {
            return 0;
        }
        try (Transaction tx = Transaction.openRoot()) {
            int inserted = handler.insert(resource, clampToInt(maxAmount), tx);
            if (!simulate) {
                tx.commit();
            }
            return inserted;
        }
    }

    @Override
    public long extract(IItemKey item, long maxAmount, boolean simulate) {
        ItemResource resource = toResource(item);
        if (resource.isEmpty() || maxAmount <= 0) {
            return 0;
        }
        try (Transaction tx = Transaction.openRoot()) {
            int extracted = handler.extract(resource, clampToInt(maxAmount), tx);
            if (!simulate) {
                tx.commit();
            }
            return extracted;
        }
    }

    @Override
    public Iterable<IItemView> contents() {
        List<IItemView> views = new ArrayList<>();
        for (int i = 0; i < handler.size(); i++) {
            ItemResource resource = handler.getResource(i);
            long amount = handler.getAmountAsLong(i);
            if (resource.isEmpty() || amount <= 0) {
                continue;
            }
            IItemKey key = new NeoForgeItemKey(resource);
            views.add(new IItemView() {
                @Override public IItemKey resource() { return key; }
                @Override public long amount() { return amount; }
            });
        }
        return views;
    }

    private static ItemResource toResource(IItemKey key) {
        return key instanceof NeoForgeItemKey nfKey ? nfKey.resource() : ItemResource.of(key.toStack(1));
    }

    private static int clampToInt(long amount) {
        return (int) Math.max(0, Math.min(amount, Integer.MAX_VALUE));
    }

    /**
     * Adapts a common {@link IItemStorage} to NeoForge's slot-based {@link ResourceHandler}.
     *
     * <p>Reports {@link #size()}{@code == 1} regardless of how many distinct item types the
     * underlying storage holds. {@link #getResource(int)} returns the first non-empty item
     * (and {@link #getAmountAsLong(int)} its amount). This is an intentional "unified slot"
     * view — the common abstraction has no fixed slot count, so we expose it as a single
     * virtual slot. Insert and extract operations route through {@link #insert} and
     * {@link #extract} which use the {@link IItemKey} content map and behave correctly
     * for storages with multiple item types.
     *
     * <p>External code that enumerates slots will only observe the first item type.
     * For correct multi-item interaction, callers should use the key-based insert/extract
     * methods rather than slot iteration.
     */
    private static final class CommonItemHandler extends SnapshotJournal<Map<IItemKey, Long>>
            implements ResourceHandler<ItemResource> {
        private final IItemStorage storage;
        private Map<IItemKey, Long> pendingDeltas = new HashMap<>();

        private CommonItemHandler(IItemStorage storage) {
            this.storage = storage;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public ItemResource getResource(int index) {
            checkIndex(index);
            for (IItemView view : storage.contents()) {
                if (view.amount() > 0) {
                    return toResource(view.resource());
                }
            }
            return ItemResource.EMPTY;
        }

        @Override
        public long getAmountAsLong(int index) {
            checkIndex(index);
            for (IItemView view : storage.contents()) {
                if (view.amount() > 0) {
                    return view.amount();
                }
            }
            return 0;
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            checkIndex(index);
            if (resource.isEmpty()) {
                return 0;
            }
            return storage.insert(new NeoForgeItemKey(resource), Integer.MAX_VALUE, true);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            checkIndex(index);
            return !resource.isEmpty() && getCapacityAsLong(index, resource) > 0;
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            checkIndex(index);
            if (resource.isEmpty() || amount <= 0) {
                return 0;
            }
            updateSnapshots(transaction);
            IItemKey key = new NeoForgeItemKey(resource);
            long pending = pendingDeltas.getOrDefault(key, 0L);
            long available = Math.max(0, storage.insert(key, Integer.MAX_VALUE, true) - pending);
            long inserted = Math.min(amount, available);
            if (inserted > 0) {
                pendingDeltas.merge(key, inserted, Long::sum);
            }
            return clampToInt(inserted);
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            checkIndex(index);
            if (resource.isEmpty() || amount <= 0) {
                return 0;
            }
            updateSnapshots(transaction);
            IItemKey key = new NeoForgeItemKey(resource);
            long pending = pendingDeltas.getOrDefault(key, 0L);
            long available = Math.max(0, storage.extract(key, Integer.MAX_VALUE, true) + pending);
            long extracted = Math.min(amount, available);
            if (extracted > 0) {
                pendingDeltas.merge(key, -extracted, Long::sum);
            }
            return clampToInt(extracted);
        }

        @Override
        protected Map<IItemKey, Long> createSnapshot() {
            return new HashMap<>(pendingDeltas);
        }

        @Override
        protected void revertToSnapshot(Map<IItemKey, Long> snapshot) {
            pendingDeltas = snapshot;
        }

        @Override
        protected void onRootCommit(Map<IItemKey, Long> originalState) {
            for (var entry : pendingDeltas.entrySet()) {
                long original = originalState.getOrDefault(entry.getKey(), 0L);
                long delta = entry.getValue() - original;
                if (delta > 0) {
                    storage.insert(entry.getKey(), delta, false);
                } else if (delta < 0) {
                    storage.extract(entry.getKey(), -delta, false);
                }
            }
            pendingDeltas = new HashMap<>();
        }

        private static void checkIndex(int index) {
            if (index != 0) {
                throw new IndexOutOfBoundsException(index);
            }
        }
    }
}
