package com.logistics.neoforge.platform;

import com.logistics.platform.storage.TransactionContext;
import java.util.ArrayList;
import java.util.List;

/**
 * NeoForge implementation of TransactionContext.
 * Since NeoForge doesn't have a real transaction system like Fabric's Transfer API,
 * this implementation uses simulation mode for queries and tracks operations to apply on commit.
 *
 * <p>For most operations, NeoForge's IItemHandler uses a simulate flag:
 * - simulate=true: Query what would happen without modifying
 * - simulate=false: Actually perform the operation
 *
 * <p>This context tracks that we're in a "transaction" and operations check isCommitted()
 * to determine whether to use simulate mode or actually execute.
 */
public class NeoForgeTransactionContext implements TransactionContext {
    private boolean committed = false;
    private boolean closed = false;
    private final List<CloseCallback> callbacks = new ArrayList<>();

    @Override
    public void commit() {
        if (closed) {
            throw new IllegalStateException("Transaction already closed");
        }
        committed = true;
    }

    @Override
    public void abort() {
        if (closed) {
            throw new IllegalStateException("Transaction already closed");
        }
        committed = false;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        for (CloseCallback callback : callbacks) {
            callback.onClose(committed);
        }
        callbacks.clear();
    }

    @Override
    public void addCloseCallback(CloseCallback callback) {
        if (closed) {
            throw new IllegalStateException("Cannot add callback to closed transaction");
        }
        callbacks.add(callback);
    }

    @Override
    public boolean isCommitted() {
        return committed;
    }

    /**
     * Check if operations should use simulation mode.
     * In NeoForge, we always simulate first, then apply on commit.
     *
     * @return true if this transaction hasn't been committed yet
     */
    public boolean shouldSimulate() {
        return !committed;
    }
}
