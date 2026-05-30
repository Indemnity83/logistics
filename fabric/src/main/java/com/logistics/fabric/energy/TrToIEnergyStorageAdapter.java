package com.logistics.fabric.energy;

import com.logistics.core.lib.energy.IEnergyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
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
        if (simulate) {
            // Approximate simulate using capacity math — avoids opening a transaction,
            // so this is safe to call from within any transaction context.
            long available = Math.max(0, Math.max(0, delegate.getCapacity()) - Math.max(0, delegate.getAmount()));
            return Math.min(maxAmount, available);
        } else {
            try (Transaction tx = Transaction.openOuter()) {
                long accepted = delegate.insert(maxAmount, tx);
                if (accepted > 0) tx.commit();
                return accepted;
            }
        }
    }

    @Override
    public long extract(long maxAmount, boolean simulate) {
        if (maxAmount <= 0) return 0;
        if (simulate) {
            // Approximate simulate using stored amount — avoids opening a transaction.
            return Math.min(maxAmount, Math.max(0, delegate.getAmount()));
        } else {
            try (Transaction tx = Transaction.openOuter()) {
                long extracted = delegate.extract(maxAmount, tx);
                if (extracted > 0) tx.commit();
                return extracted;
            }
        }
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
