package com.logistics.fabric.fluids;

import com.logistics.core.lib.fluids.IFluidKey;
import com.logistics.core.lib.fluids.IFluidStorage;
import com.logistics.core.lib.fluids.IFluidView;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Fabric adapter: wraps a Fabric {@link Storage}{@code <FluidVariant>} as an {@link IFluidStorage}.
 *
 * <p>Translates {@link IFluidStorage}'s simulate-boolean API into Fabric's transaction system.
 * When {@code simulate=true}, a transaction is opened and allowed to roll back on close.
 * When {@code simulate=false}, the transaction is committed.
 *
 * <p>Also provides {@link #asFabric(IFluidStorage)} to wrap an {@link IFluidStorage} back into
 * a Fabric {@code Storage<FluidVariant>}, used by {@code FluidStorageAccess} to expose
 * {@link com.logistics.core.lib.block.capability.HasFluidStorage} block entities to Fabric's
 * capability system.
 */
public final class FabricFluidStorage implements IFluidStorage {

    private final Storage<FluidVariant> storage;

    private FabricFluidStorage(Storage<FluidVariant> storage) {
        this.storage = storage;
    }

    /**
     * Wrap a Fabric storage as an {@link IFluidStorage}.
     *
     * @param storage the Fabric storage to wrap, may be {@code null}
     * @return a wrapped {@link IFluidStorage}, or {@code null} if input is {@code null}
     */
    @Nullable
    public static IFluidStorage wrap(@Nullable Storage<FluidVariant> storage) {
        return storage == null ? null : new FabricFluidStorage(storage);
    }

    /**
     * Expose an {@link IFluidStorage} to Fabric's capability system as a {@code Storage<FluidVariant>}.
     *
     * <p>Used by {@code FluidStorageAccess} to bridge block entities that implement
     * {@link com.logistics.core.lib.block.capability.HasFluidStorage} to Fabric's fluid
     * transfer lookup.
     *
     * @param storage the loader-agnostic storage to expose, may be {@code null}
     * @return a Fabric-compatible storage, or {@code null} if input is {@code null}
     */
    @Nullable
    public static Storage<FluidVariant> asFabric(@Nullable IFluidStorage storage) {
        return storage == null ? null : new StagedStorage(storage);
    }

    // ==================== IFluidStorage implementation ====================

    @Override
    public long insert(IFluidKey fluid, long maxAmount, boolean simulate) {
        FluidVariant variant = toVariant(fluid);
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
    public long extract(IFluidKey fluid, long maxAmount, boolean simulate) {
        FluidVariant variant = toVariant(fluid);
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
    public Iterable<IFluidView> contents() {
        return () -> new Iterator<>() {
            private final Iterator<StorageView<FluidVariant>> inner = storage.iterator();
            private StorageView<FluidVariant> pending = advance();

            private StorageView<FluidVariant> advance() {
                while (inner.hasNext()) {
                    StorageView<FluidVariant> v = inner.next();
                    if (!v.isResourceBlank() && v.getAmount() > 0) return v;
                }
                return null;
            }

            @Override
            public boolean hasNext() { return pending != null; }

            @Override
            public IFluidView next() {
                if (pending == null) throw new java.util.NoSuchElementException();
                StorageView<FluidVariant> view = pending;
                pending = advance();
                IFluidKey key = FabricFluidKey.of(view.getResource());
                long amt = view.getAmount();
                return new IFluidView() {
                    @Override public IFluidKey resource() { return key; }
                    @Override public long amount() { return amt; }
                };
            }
        };
    }

    // ==================== asFabric() inner classes ====================

    /**
     * Fabric {@code Storage<FluidVariant>} backed by an {@link IFluidStorage}.
     *
     * <p>Extends {@link SnapshotParticipant} to correctly handle nested transactions.
     * Positive delta = pending insert; negative delta = pending extract.
     */
    private static final class StagedStorage extends SnapshotParticipant<Map<IFluidKey, Long>>
            implements Storage<FluidVariant> {

        private final IFluidStorage storage;
        private Map<IFluidKey, Long> pendingDeltas = new HashMap<>();

        StagedStorage(IFluidStorage storage) {
            this.storage = storage;
        }

        @Override
        protected Map<IFluidKey, Long> createSnapshot() {
            return new HashMap<>(pendingDeltas);
        }

        @Override
        protected void readSnapshot(Map<IFluidKey, Long> snapshot) {
            pendingDeltas = snapshot;
        }

        @Override
        protected void onFinalCommit() {
            for (var e : pendingDeltas.entrySet()) {
                long delta = e.getValue();
                if (delta > 0) storage.insert(e.getKey(), delta, false);
                else if (delta < 0) storage.extract(e.getKey(), -delta, false);
            }
            pendingDeltas = new HashMap<>();
        }

        Map<IFluidKey, Long> deltas(TransactionContext tx) {
            updateSnapshots(tx);
            return pendingDeltas;
        }

        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            IFluidKey key = FabricFluidKey.of(resource);
            Map<IFluidKey, Long> d = deltas(transaction);
            long netDelta = d.getOrDefault(key, 0L);
            long effectiveCapacity = Math.max(0, storage.insert(key, Long.MAX_VALUE, true) - netDelta);
            long toInsert = Math.min(maxAmount, effectiveCapacity);
            if (toInsert > 0) d.merge(key, toInsert, Long::sum);
            return toInsert;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            IFluidKey key = FabricFluidKey.of(resource);
            Map<IFluidKey, Long> d = deltas(transaction);
            long netDelta = d.getOrDefault(key, 0L);
            long effectiveAvailable = Math.max(0, storage.extract(key, Long.MAX_VALUE, true) + netDelta);
            long toExtract = Math.min(maxAmount, effectiveAvailable);
            if (toExtract > 0) d.merge(key, -toExtract, Long::sum);
            return toExtract;
        }

        @Override
        public Iterator<StorageView<FluidVariant>> iterator() {
            return new Iterator<>() {
                private final Iterator<IFluidView> inner = storage.contents().iterator();

                @Override
                public boolean hasNext() { return inner.hasNext(); }

                @Override
                public StorageView<FluidVariant> next() {
                    IFluidView view = inner.next();
                    FluidVariant variant = toVariant(view.resource());
                    long amount = view.amount();
                    StagedStorage staged = StagedStorage.this;
                    return new StorageView<>() {
                        @Override
                        public long extract(FluidVariant resource, long maxAmount, TransactionContext tx) {
                            if (!resource.equals(variant)) return 0;
                            IFluidKey key = FabricFluidKey.of(resource);
                            Map<IFluidKey, Long> d = staged.deltas(tx);
                            long netDelta = d.getOrDefault(key, 0L);
                            long effectiveAvailable = Math.max(0, staged.storage.extract(key, Long.MAX_VALUE, true) + netDelta);
                            long toExtract = Math.min(Math.min(maxAmount, amount), effectiveAvailable);
                            if (toExtract > 0) d.merge(key, -toExtract, Long::sum);
                            return toExtract;
                        }

                        @Override public boolean isResourceBlank() { return variant.isBlank(); }
                        @Override public FluidVariant getResource() { return variant; }
                        @Override public long getAmount() { return amount; }
                        @Override public long getCapacity() { return amount; }
                    };
                }
            };
        }
    }

    // ==================== Helpers ====================

    private static FluidVariant toVariant(IFluidKey key) {
        if (key instanceof FabricFluidKey fk) return fk.variant();
        return FluidVariant.of(key.getFluid(), key.getComponents());
    }
}
