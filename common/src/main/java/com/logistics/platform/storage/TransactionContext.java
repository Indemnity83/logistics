package com.logistics.platform.storage;

/**
 * Platform-agnostic transaction context for batched storage operations.
 *
 * <p>On Fabric, this wraps a real Transaction with rollback support.
 * On NeoForge, this tracks operations and applies them on commit (using simulation).
 *
 * <p>Usage:
 * <pre>{@code
 * try (TransactionContext tx = Services.itemStorage().openTransaction()) {
 *     long inserted = storage.insert(stack, count, tx);
 *     if (inserted > 0) {
 *         tx.commit();
 *     }
 * } // automatically aborts if not committed
 * }</pre>
 */
public interface TransactionContext extends AutoCloseable {

    /**
     * Commit all operations in this transaction.
     * After calling this, close() will not abort.
     */
    void commit();

    /**
     * Abort all operations in this transaction.
     * On Fabric, this triggers rollback. On NeoForge, tracked operations are discarded.
     */
    void abort();

    /**
     * Close this transaction. If not committed, aborts.
     */
    @Override
    void close();

    /**
     * Add a callback to run when the transaction closes.
     *
     * @param callback the callback to run
     */
    void addCloseCallback(CloseCallback callback);

    /**
     * Check if this transaction has been committed.
     *
     * @return true if committed
     */
    boolean isCommitted();

    /**
     * Callback for transaction close events.
     */
    @FunctionalInterface
    interface CloseCallback {
        /**
         * Called when the transaction closes.
         *
         * @param committed true if the transaction was committed, false if aborted
         */
        void onClose(boolean committed);
    }
}
