package com.logistics.neoforge.energy;

import com.logistics.core.lib.energy.IEnergyStorage;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import org.jetbrains.annotations.Nullable;

public final class NeoForgeEnergyStorage implements IEnergyStorage {
    private final EnergyHandler handler;

    private NeoForgeEnergyStorage(EnergyHandler handler) {
        this.handler = handler;
    }

    @Nullable
    public static IEnergyStorage wrap(@Nullable EnergyHandler handler) {
        return handler == null ? null : new NeoForgeEnergyStorage(handler);
    }

    @Nullable
    public static EnergyHandler asNeoForge(@Nullable IEnergyStorage storage) {
        return storage == null ? null : new CommonEnergyHandler(storage);
    }

    @Override
    public long insert(long maxAmount, boolean simulate) {
        try (Transaction tx = openTransaction()) {
            int inserted = handler.insert(clampToInt(maxAmount), tx);
            if (!simulate) {
                tx.commit();
            }
            return inserted;
        }
    }

    @Override
    public long extract(long maxAmount, boolean simulate) {
        try (Transaction tx = openTransaction()) {
            int extracted = handler.extract(clampToInt(maxAmount), tx);
            if (!simulate) {
                tx.commit();
            }
            return extracted;
        }
    }

    @Override
    public long getAmount() {
        return handler.getAmountAsLong();
    }

    @Override
    public long getCapacity() {
        return handler.getCapacityAsLong();
    }

    @Override
    public boolean canInsert() {
        if (handler instanceof CommonEnergyHandler common) {
            return common.canInsert();
        }
        try (Transaction tx = openTransaction()) {
            return handler.insert(1, tx) > 0;
        }
    }

    @Override
    public boolean canExtract() {
        if (handler instanceof CommonEnergyHandler common) {
            return common.canExtract();
        }
        try (Transaction tx = openTransaction()) {
            return handler.extract(1, tx) > 0;
        }
    }

    @Override
    public boolean acceptsEnergy() {
        // Our own handlers carry a real direction flag. NeoForge exposes none for anyone else, so
        // the probe below cannot tell "full right now" from "never accepts" — report presence and
        // let the transfer decide, rather than mis-answering a one-shot placement question.
        return !(handler instanceof CommonEnergyHandler common) || common.canInsert();
    }

    private static Transaction openTransaction() {
        TransactionContext current = Transaction.getCurrentOpenedTransaction();
        return current == null ? Transaction.openRoot() : Transaction.open(current);
    }

    private static int clampToInt(long amount) {
        return (int) Math.max(0, Math.min(amount, Integer.MAX_VALUE));
    }

    private static final class CommonEnergyHandler extends SnapshotJournal<Long> implements EnergyHandler {
        private final IEnergyStorage storage;
        private long pendingDelta;

        private CommonEnergyHandler(IEnergyStorage storage) {
            this.storage = storage;
        }

        @Override
        public long getAmountAsLong() {
            return Math.max(0, storage.getAmount() + pendingDelta);
        }

        @Override
        public long getCapacityAsLong() {
            return Math.max(0, storage.getCapacity());
        }

        private boolean canInsert() {
            return storage.canInsert();
        }

        private boolean canExtract() {
            return storage.canExtract();
        }

        @Override
        public int insert(int amount, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
            if (amount <= 0) return 0;
            updateSnapshots(transaction);
            // Ask the wrapped storage how much it can actually accept, accounting for
            // what we've already staged this transaction. Delegating (instead of using
            // capacity - amount) lets zero-capacity conduits such as cables, which
            // forward energy into a network, accept inserts. Staged inserts (positive
            // pendingDelta) occupy room already claimed this transaction; staged
            // extractions (negative pendingDelta) free room that can be reused.
            long stagedInserts = Math.max(0, pendingDelta);
            long stagedExtractions = Math.max(0, -pendingDelta);
            long committedInsertable = storage.insert(stagedInserts + amount, true);
            long inserted = Math.max(0,
                    Math.min(amount, committedInsertable - stagedInserts + stagedExtractions));
            if (inserted > 0) {
                pendingDelta += inserted;
            }
            return clampToInt(inserted);
        }

        @Override
        public int extract(int amount, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
            if (amount <= 0) return 0;
            updateSnapshots(transaction);
            long extracted = Math.min(getAmountAsLong(), storage.extract(amount, true));
            if (extracted > 0) {
                pendingDelta -= extracted;
            }
            return clampToInt(extracted);
        }

        @Override
        protected Long createSnapshot() {
            return pendingDelta;
        }

        @Override
        protected void revertToSnapshot(Long snapshot) {
            pendingDelta = snapshot;
        }

        @Override
        protected void onRootCommit(Long originalState) {
            long delta = pendingDelta - originalState;
            if (delta > 0) {
                storage.insert(delta, false);
            } else if (delta < 0) {
                storage.extract(-delta, false);
            }
            pendingDelta = 0;
        }
    }
}
