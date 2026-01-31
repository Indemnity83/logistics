package com.logistics.api.fabric;

import com.logistics.api.EnergyStorage;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

/**
 * Adapter that wraps our internal EnergyStorage to implement
 * Team Reborn Energy's interface for cross-mod compatibility.
 *
 * <p>This adapter bridges the gap between our simulate-based API and TR Energy's
 * transaction-based API. When a transaction is committed, the actual operation
 * is performed on the underlying storage.
 */
public class TREnergyStorageAdapter implements team.reborn.energy.api.EnergyStorage {
    private final EnergyStorage delegate;

    public TREnergyStorageAdapter(EnergyStorage delegate) {
        this.delegate = delegate;
    }

    @Override
    public long insert(long maxAmount, TransactionContext transaction) {
        long accepted = delegate.insert(maxAmount, true);
        if (accepted > 0) {
            if (transaction != null) {
                transaction.addCloseCallback((ctx, result) -> {
                    if (result.wasCommitted()) {
                        delegate.insert(accepted, false);
                    }
                });
            } else {
                // Null transaction = immediate execution
                delegate.insert(accepted, false);
            }
        }
        return accepted;
    }

    @Override
    public long extract(long maxAmount, TransactionContext transaction) {
        long extracted = delegate.extract(maxAmount, true);
        if (extracted > 0) {
            if (transaction != null) {
                transaction.addCloseCallback((ctx, result) -> {
                    if (result.wasCommitted()) {
                        delegate.extract(extracted, false);
                    }
                });
            } else {
                // Null transaction = immediate execution
                delegate.extract(extracted, false);
            }
        }
        return extracted;
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
    public boolean supportsInsertion() {
        return delegate.canInsert();
    }

    @Override
    public boolean supportsExtraction() {
        return delegate.canExtract();
    }
}
