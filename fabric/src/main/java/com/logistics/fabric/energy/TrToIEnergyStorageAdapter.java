package com.logistics.fabric.energy;

import com.logistics.core.lib.energy.IEnergyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;

/**
 * Adapts a Team Reborn {@link EnergyStorage} to the loader-agnostic {@link IEnergyStorage}.
 *
 * <p>Used by {@link FabricEnergyCapabilityLookup} when the found TR storage is not already
 * an {@link IEnergyStorage} (e.g. third-party machines or other mods).
 */
final class TrToIEnergyStorageAdapter implements IEnergyStorage {
    private final EnergyStorage delegate;

    TrToIEnergyStorageAdapter(EnergyStorage delegate) {
        this.delegate = delegate;
    }

    @Override
    public long insert(long maxAmount, boolean simulate) {
        if (maxAmount <= 0) return 0;
        Transaction tx = openTransaction();
        if (tx == null) return simulate ? Math.min(maxAmount, freeRoom()) : 0;

        try (tx) {
            long accepted = delegate.insert(maxAmount, tx);
            if (!simulate && accepted > 0) tx.commit();
            return Math.max(0, accepted);
        }
    }

    @Override
    public long extract(long maxAmount, boolean simulate) {
        if (maxAmount <= 0) return 0;
        Transaction tx = openTransaction();
        if (tx == null) return simulate ? Math.min(maxAmount, Math.max(0, delegate.getAmount())) : 0;

        try (tx) {
            long extracted = delegate.extract(maxAmount, tx);
            if (!simulate && extracted > 0) tx.commit();
            return Math.max(0, extracted);
        }
    }

    /**
     * Opens a transaction the caller may abort, nesting inside any transaction already
     * open on this thread.
     *
     * <p>A simulate runs the delegate's real transfer and aborts it, because Team Reborn
     * exposes no per-operation rate getters — capacity arithmetic would overstate what a
     * rate-limited storage actually accepts.
     *
     * @return {@code null} when no transaction can be opened (inside a close callback)
     */
    @Nullable
    private static Transaction openTransaction() {
        try {
            TransactionContext current = Transaction.getCurrentUnsafe();
            return current == null ? Transaction.openOuter() : Transaction.openNested(current);
        } catch (IllegalStateException insideCloseCallback) {
            return null;
        }
    }

    private long freeRoom() {
        return Math.max(0, Math.max(0, delegate.getCapacity()) - Math.max(0, delegate.getAmount()));
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
        return delegate.supportsInsertion();
    }

    @Override
    public boolean canExtract() {
        return delegate.supportsExtraction();
    }
}
