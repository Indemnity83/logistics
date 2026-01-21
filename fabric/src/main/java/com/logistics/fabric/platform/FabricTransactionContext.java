package com.logistics.fabric.platform;

import com.logistics.platform.storage.TransactionContext;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

/**
 * Fabric implementation of TransactionContext wrapping a Transfer API Transaction.
 */
public class FabricTransactionContext implements TransactionContext {

    private final Transaction transaction;
    private final List<CloseCallback> closeCallbacks = new ArrayList<>();
    private boolean committed = false;
    private boolean closed = false;

    public FabricTransactionContext(Transaction transaction) {
        this.transaction = transaction;
    }

    /**
     * Get the underlying Fabric transaction for direct access.
     */
    public Transaction getTransaction() {
        return transaction;
    }

    @Override
    public void commit() {
        if (closed) {
            throw new IllegalStateException("Transaction already closed");
        }
        committed = true;
        transaction.commit();
    }

    @Override
    public void abort() {
        if (closed) {
            return; // Already closed, nothing to do
        }
        transaction.abort();
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;

        if (!committed) {
            transaction.abort();
        }
        transaction.close();

        // Notify callbacks
        for (CloseCallback callback : closeCallbacks) {
            callback.onClose(committed);
        }
    }

    @Override
    public void addCloseCallback(CloseCallback callback) {
        closeCallbacks.add(callback);
    }

    @Override
    public boolean isCommitted() {
        return committed;
    }
}
