package com.logistics.fabric.energy;

import com.logistics.core.lib.block.capability.HasEnergyStorage;
import com.logistics.core.lib.energy.IEnergyStorage;
import com.logistics.core.lib.power.DirectEnergyReceiver;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.reborn.energy.api.EnergyStorage;

/**
 * Bridges {@link HasEnergyStorage} block entities to Team Reborn Energy's lookup API.
 *
 * <p>Any block entity implementing {@link HasEnergyStorage} will automatically work with:
 * <ul>
 *   <li>Tech Reborn machines</li>
 *   <li>Other mods using Team Reborn Energy API</li>
 *   <li>This mod's own energy network</li>
 * </ul>
 *
 * <p>If the returned {@link IEnergyStorage} is already a Team Reborn {@link EnergyStorage}
 * (e.g. {@link FabricEnergyStorage}), it is returned directly. Otherwise a lightweight
 * adapter translates the simulate-boolean API to Team Reborn's transaction system.
 *
 * <p>Call {@link #register()} once during Fabric mod initialization.
 */
public final class EnergyStorageAccess {

    private static final Logger LOGGER = LoggerFactory.getLogger("logistics/energy");

    /**
     * Staging participants, keyed by owning block entity and side.
     *
     * <p>Every lookup of one block and side must hand back the <em>same</em> participant: two
     * participants over one storage each stage their delta against unmodified state, so the caller
     * is told the storage accepted more than it really can.
     *
     * <p>Keys are weak so unloaded block entities stay collectible, and values are weak so the map
     * cannot pin its own keys (a participant reaches its block entity through its delegate). A
     * participant holding a staged delta is strongly referenced by the open transaction, so it can
     * never be collected while it still owes an apply.
     *
     * <p>Thread-confined, because Fabric transactions are: a staged delta must never be shared
     * between threads.
     */
    private static final ThreadLocal<Map<Object, Map<Direction, WeakReference<SimulateBooleanAdapter>>>> ADAPTERS =
            ThreadLocal.withInitial(WeakHashMap::new);

    private EnergyStorageAccess() {}

    /**
     * Register the fallback adapter that exposes {@link HasEnergyStorage} block entities
     * to Team Reborn Energy's lookup system.
     */
    public static void register() {
        EnergyStorage.SIDED.registerFallback((world, pos, state, blockEntity, direction) -> {
            if (!(blockEntity instanceof HasEnergyStorage hasStorage)) return null;
            // Extraction pipes / pump are engine-powered only; keep their buffer off the grid.
            if (blockEntity instanceof DirectEnergyReceiver) return null;
            IEnergyStorage storage = hasStorage.energyStorage(direction);
            if (storage == null) return null;
            // FabricEnergyStorage is already a TR EnergyStorage — hand it directly
            if (storage instanceof EnergyStorage trStorage) return trStorage;
            // Wrap common EnergyComponent (or any other IEnergyStorage) for TR
            return adapterFor(blockEntity, direction, storage);
        });
    }

    /**
     * Returns the staging participant for {@code owner} on {@code side}, creating it on first use.
     *
     * <p>A cached participant is reused even when {@code storage} is a different instance from the
     * one it wraps: block entities that build a fresh {@link IEnergyStorage} per call (cables)
     * return interchangeable views onto the same underlying storage.
     */
    static SimulateBooleanAdapter adapterFor(Object owner, @Nullable Direction side, IEnergyStorage storage) {
        Map<Direction, WeakReference<SimulateBooleanAdapter>> bySide =
                ADAPTERS.get().computeIfAbsent(owner, key -> new HashMap<>());
        WeakReference<SimulateBooleanAdapter> cached = bySide.get(side);
        SimulateBooleanAdapter adapter = cached == null ? null : cached.get();
        if (adapter != null) return adapter;
        SimulateBooleanAdapter created = new SimulateBooleanAdapter(storage);
        bySide.put(side, new WeakReference<>(created));
        return created;
    }

    /**
     * Adapts a simulate-boolean {@link IEnergyStorage} to Team Reborn's transaction API,
     * while also implementing {@link IEnergyStorage} directly.
     *
     * <p>The TR interface ({@code insert(long, TransactionContext)}) stages a signed delta rather
     * than touching the delegate: positive is a pending insert, negative a pending extract. As a
     * {@link SnapshotParticipant} that delta is snapshotted per nesting depth, so a nested commit
     * merges into its parent, an abort rolls back, and {@link #onFinalCommit()} writes the net
     * delta to the delegate exactly once, after the outermost transaction closes. Simulating
     * against the pending-adjusted state also keeps repeated calls within one transaction from
     * offering the same room (or the same energy) twice.
     *
     * <p>The {@link IEnergyStorage} interface delegates directly to the underlying storage without
     * opening any new transactions — safe to call from within TR close callbacks or from
     * the cable network's simulate-boolean path.
     */
    static final class SimulateBooleanAdapter extends SnapshotParticipant<Long>
            implements EnergyStorage, IEnergyStorage {
        private final IEnergyStorage delegate;

        /** Staged, not yet applied: positive is a pending insert, negative a pending extract. */
        private long pendingDelta;

        SimulateBooleanAdapter(IEnergyStorage delegate) {
            this.delegate = delegate;
        }

        // ── Team Reborn EnergyStorage (transaction-based) ───────────────────────────

        @Override
        public long insert(long maxAmount, TransactionContext tx) {
            if (maxAmount <= 0) return 0;
            long room = Math.max(0, delegate.insert(Long.MAX_VALUE, true) - pendingDelta);
            long toInsert = Math.min(maxAmount, room);
            if (toInsert > 0) {
                updateSnapshots(tx);
                pendingDelta += toInsert;
            }
            return toInsert;
        }

        @Override
        public long extract(long maxAmount, TransactionContext tx) {
            if (maxAmount <= 0) return 0;
            long available = Math.max(0, delegate.extract(Long.MAX_VALUE, true) + pendingDelta);
            long toExtract = Math.min(maxAmount, available);
            if (toExtract > 0) {
                updateSnapshots(tx);
                pendingDelta -= toExtract;
            }
            return toExtract;
        }

        @Override
        protected Long createSnapshot() {
            return pendingDelta;
        }

        @Override
        protected void readSnapshot(Long snapshot) {
            pendingDelta = snapshot;
        }

        @Override
        protected void onFinalCommit() {
            long delta = pendingDelta;
            pendingDelta = 0;
            if (delta == 0) return;
            long applied = delta > 0 ? delegate.insert(delta, false) : -delegate.extract(-delta, false);
            if (applied != delta) {
                LOGGER.warn(
                        "Energy storage {} applied {} RF of a committed {} RF transfer; {} RF unaccounted for",
                        delegate.getClass().getName(),
                        applied,
                        delta,
                        delta - applied);
            }
        }

        @Override
        public boolean supportsInsertion() {
            return delegate.canInsert();
        }

        @Override
        public boolean supportsExtraction() {
            return delegate.canExtract();
        }

        // ── IEnergyStorage (simulate-boolean, transaction-free) ──────────────────────
        // Used by the cable network; safe to call from within TR close callbacks.

        @Override
        public long insert(long maxAmount, boolean simulate) {
            return delegate.insert(maxAmount, simulate);
        }

        @Override
        public long extract(long maxAmount, boolean simulate) {
            return delegate.extract(maxAmount, simulate);
        }

        @Override
        public long getAmount() {
            return delegate.getAmount();
        }

        @Override
        public long getCapacity() {
            return delegate.getCapacity();
        }

        @Override
        public boolean canInsert() {
            return delegate.canInsert();
        }

        @Override
        public boolean canExtract() {
            return delegate.canExtract();
        }
    }
}
