package com.logistics.fabric.energy;

import com.logistics.core.lib.block.capability.HasEnergyStorage;
import com.logistics.core.lib.energy.IEnergyStorage;
import team.reborn.energy.api.EnergyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

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
    private EnergyStorageAccess() {}

    /**
     * Register the fallback adapter that exposes {@link HasEnergyStorage} block entities
     * to Team Reborn Energy's lookup system.
     */
    public static void register() {
        EnergyStorage.SIDED.registerFallback((world, pos, state, blockEntity, direction) -> {
            if (!(blockEntity instanceof HasEnergyStorage hasStorage)) return null;
            IEnergyStorage storage = hasStorage.energyStorage(direction);
            if (storage == null) return null;
            // FabricEnergyStorage is already a TR EnergyStorage — hand it directly
            if (storage instanceof EnergyStorage trStorage) return trStorage;
            // Wrap common EnergyComponent (or any other IEnergyStorage) for TR
            return new SimulateBooleanAdapter(storage);
        });
    }

    /**
     * Adapts a simulate-boolean {@link IEnergyStorage} to Team Reborn's transaction API,
     * while also implementing {@link IEnergyStorage} directly.
     *
     * <p>The TR interface ({@code insert(long, TransactionContext)}) uses close callbacks so
     * that the actual commit is deferred until the caller's transaction commits. The
     * {@link IEnergyStorage} interface delegates directly to the underlying storage without
     * opening any new transactions — safe to call from within TR close callbacks or from
     * the cable network's simulate-boolean path.
     */
    static final class SimulateBooleanAdapter implements EnergyStorage, IEnergyStorage {
        private final IEnergyStorage delegate;

        SimulateBooleanAdapter(IEnergyStorage delegate) {
            this.delegate = delegate;
        }

        // ── Team Reborn EnergyStorage (transaction-based) ───────────────────────────

        @Override
        public long insert(long maxAmount, TransactionContext tx) {
            long accepted = delegate.insert(maxAmount, true); // simulate
            if (accepted > 0) {
                tx.addCloseCallback((context, result) -> {
                    if (result == TransactionContext.Result.COMMITTED) {
                        delegate.insert(accepted, false);
                    }
                });
            }
            return accepted;
        }

        @Override
        public long extract(long maxAmount, TransactionContext tx) {
            long extracted = delegate.extract(maxAmount, true); // simulate
            if (extracted > 0) {
                tx.addCloseCallback((context, result) -> {
                    if (result == TransactionContext.Result.COMMITTED) {
                        delegate.extract(extracted, false);
                    }
                });
            }
            return extracted;
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
