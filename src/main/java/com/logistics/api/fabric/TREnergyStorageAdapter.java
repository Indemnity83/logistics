package com.logistics.api.fabric;

import com.logistics.api.EnergyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;

/**
 * Adapter that wraps our internal EnergyStorage to implement
 * Team Reborn Energy's interface for cross-mod compatibility.
 *
 * <p>This adapter bridges the gap between our simulate-based API and TR Energy's
 * transaction-based API. It uses Fabric's SnapshotParticipant to properly handle
 * nested transactions, ensuring that multiple operations within the same transaction
 * observe each other's effects during simulation.
 */
public class TREnergyStorageAdapter extends SnapshotParticipant<Long> implements team.reborn.energy.api.EnergyStorage {
    private final EnergyStorage delegate;

    /** Pending energy delta: positive = inserted, negative = extracted. */
    private long pendingDelta = 0;

    public TREnergyStorageAdapter(EnergyStorage delegate) {
        this.delegate = delegate;
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
        // Apply the accumulated delta to the actual storage
        if (pendingDelta > 0) {
            delegate.insert(pendingDelta, false);
        } else if (pendingDelta < 0) {
            delegate.extract(-pendingDelta, false);
        }
        pendingDelta = 0;
    }

    @Override
    public long insert(long maxAmount, TransactionContext transaction) {
        if (transaction == null) {
            // Null transaction = immediate execution
            return delegate.insert(maxAmount, false);
        }

        // Calculate effective current amount including pending changes
        long effectiveAmount = delegate.getAmount() + pendingDelta;
        long effectiveCapacity = delegate.getCapacity();
        long effectiveSpace = Math.max(0, effectiveCapacity - effectiveAmount);

        // Only insert if the delegate supports it
        if (!delegate.canInsert()) {
            return 0;
        }

        long accepted = Math.min(maxAmount, effectiveSpace);
        if (accepted > 0) {
            updateSnapshots(transaction);
            pendingDelta += accepted;
        }
        return accepted;
    }

    @Override
    public long extract(long maxAmount, TransactionContext transaction) {
        if (transaction == null) {
            // Null transaction = immediate execution
            return delegate.extract(maxAmount, false);
        }

        // Calculate effective current amount including pending changes
        long effectiveAmount = delegate.getAmount() + pendingDelta;

        // Only extract if the delegate supports it
        if (!delegate.canExtract()) {
            return 0;
        }

        long extracted = Math.min(maxAmount, Math.max(0, effectiveAmount));
        if (extracted > 0) {
            updateSnapshots(transaction);
            pendingDelta -= extracted;
        }
        return extracted;
    }

    @Override
    public long getAmount() {
        // Return effective amount including pending transaction changes
        return Math.max(0, delegate.getAmount() + pendingDelta);
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
