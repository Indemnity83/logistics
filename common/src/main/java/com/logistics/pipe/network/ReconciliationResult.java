package com.logistics.pipe.network;

/**
 * Outcome of a {@link ReconciliationService#reconcile} check.
 *
 * @param amountLost units that are no longer sourceable from current supply
 */
public record ReconciliationResult(long amountLost) {

    /** No stock loss detected. */
    public static ReconciliationResult none() {
        return new ReconciliationResult(0L);
    }

    /** {@code true} when supply has dropped below the job's outstanding requirement. */
    public boolean hasLoss() {
        return amountLost > 0;
    }
}
