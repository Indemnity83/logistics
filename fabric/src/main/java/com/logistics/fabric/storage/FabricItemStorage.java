package com.logistics.fabric.storage;

import com.logistics.core.lib.storage.IItemKey;
import com.logistics.core.lib.storage.IItemStorage;
import com.logistics.core.lib.storage.IItemView;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

/**
 * Fabric adapter: wraps a Fabric {@link Storage}{@code <ItemVariant>} as an {@link IItemStorage}.
 *
 * <p>Translates {@link IItemStorage}'s simulate-boolean API into Fabric's transaction system.
 * When {@code simulate=true}, a transaction is opened and allowed to roll back on close.
 * When {@code simulate=false}, the transaction is committed.
 *
 * <p>Also provides {@link #asFabric(IItemStorage)} to wrap an {@link IItemStorage} back into
 * a Fabric {@code Storage<ItemVariant>}, used by {@code ItemStorageAccess} to expose
 * {@link com.logistics.core.lib.block.capability.HasItemStorage} block entities to Fabric's
 * capability system.
 */
public final class FabricItemStorage implements IItemStorage {

    private final Storage<ItemVariant> storage;

    private FabricItemStorage(Storage<ItemVariant> storage) {
        this.storage = storage;
    }

    /**
     * Wrap a Fabric storage as an {@link IItemStorage}.
     *
     * @param storage the Fabric storage to wrap, may be {@code null}
     * @return a wrapped {@link IItemStorage}, or {@code null} if input is {@code null}
     */
    @Nullable
    public static IItemStorage wrap(@Nullable Storage<ItemVariant> storage) {
        return storage == null ? null : new FabricItemStorage(storage);
    }

    /**
     * Expose an {@link IItemStorage} to Fabric's capability system as a {@code Storage<ItemVariant>}.
     *
     * <p>Used by {@code ItemStorageAccess} to bridge block entities that implement
     * {@link com.logistics.core.lib.block.capability.HasItemStorage} to Fabric's item
     * transfer lookup.
     *
     * @param storage the loader-agnostic storage to expose, may be {@code null}
     * @return a Fabric-compatible storage, or {@code null} if input is {@code null}
     */
    @Nullable
    public static Storage<ItemVariant> asFabric(@Nullable IItemStorage storage) {
        if (storage == null) return null;
        return new Storage<ItemVariant>() {
            @Override
            public long insert(ItemVariant resource, long maxAmount,
                    net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext transaction) {
                IItemKey key = new FabricItemKey(resource);
                long simulated = storage.insert(key, maxAmount, true);
                if (simulated > 0) {
                    transaction.addCloseCallback((tx, result) -> {
                        if (result.wasCommitted()) storage.insert(key, simulated, false);
                    });
                }
                return simulated;
            }

            @Override
            public long extract(ItemVariant resource, long maxAmount,
                    net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext transaction) {
                IItemKey key = new FabricItemKey(resource);
                long simulated = storage.extract(key, maxAmount, true);
                if (simulated > 0) {
                    transaction.addCloseCallback((tx, result) -> {
                        if (result.wasCommitted()) storage.extract(key, simulated, false);
                    });
                }
                return simulated;
            }

            @Override
            public Iterator<StorageView<ItemVariant>> iterator() {
                Iterator<IItemView> inner = storage.contents().iterator();
                return new Iterator<>() {
                    @Override
                    public boolean hasNext() { return inner.hasNext(); }

                    @Override
                    public StorageView<ItemVariant> next() {
                        IItemView view = inner.next();
                        return new StorageView<>() {
                            @Override
                            public long extract(ItemVariant resource, long maxAmount,
                                    net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext tx) {
                                IItemKey key = new FabricItemKey(resource);
                                long simulated = storage.extract(key, maxAmount, true);
                                if (simulated > 0) {
                                    tx.addCloseCallback((t, result) -> {
                                        if (result.wasCommitted()) storage.extract(key, simulated, false);
                                    });
                                }
                                return simulated;
                            }

                            @Override
                            public boolean isResourceBlank() {
                                return view.resource().toStack(1).isEmpty();
                            }

                            @Override
                            public ItemVariant getResource() {
                                IItemKey key = view.resource();
                                if (key instanceof FabricItemKey fk) return fk.variant();
                                return ItemVariant.of(key.toStack(1));
                            }

                            @Override
                            public long getAmount() { return view.amount(); }

                            @Override
                            public long getCapacity() { return view.amount(); }
                        };
                    }
                };
            }
        };
    }

    // ==================== IItemStorage implementation ====================

    @Override
    public long insert(IItemKey item, long maxAmount, boolean simulate) {
        ItemVariant variant = item instanceof FabricItemKey fk ? fk.variant() : ItemVariant.of(item.toStack(1));
        if (simulate) {
            try (Transaction t = Transaction.openOuter()) {
                return storage.insert(variant, maxAmount, t);
                // t closes without commit → rollback
            }
        }
        try (Transaction t = Transaction.openOuter()) {
            long inserted = storage.insert(variant, maxAmount, t);
            t.commit();
            return inserted;
        }
    }

    @Override
    public long extract(IItemKey item, long maxAmount, boolean simulate) {
        ItemVariant variant = item instanceof FabricItemKey fk ? fk.variant() : ItemVariant.of(item.toStack(1));
        if (simulate) {
            try (Transaction t = Transaction.openOuter()) {
                return storage.extract(variant, maxAmount, t);
            }
        }
        try (Transaction t = Transaction.openOuter()) {
            long extracted = storage.extract(variant, maxAmount, t);
            t.commit();
            return extracted;
        }
    }

    @Override
    public Iterable<IItemView> contents() {
        return () -> new Iterator<>() {
            private final Iterator<StorageView<ItemVariant>> inner = storage.iterator();
            private StorageView<ItemVariant> pending = advance();

            private StorageView<ItemVariant> advance() {
                while (inner.hasNext()) {
                    StorageView<ItemVariant> v = inner.next();
                    if (v.getAmount() > 0) return v;
                }
                return null;
            }

            @Override
            public boolean hasNext() { return pending != null; }

            @Override
            public IItemView next() {
                if (pending == null) throw new java.util.NoSuchElementException();
                IItemView view = new FabricItemView(pending);
                pending = advance();
                return view;
            }
        };
    }
}
