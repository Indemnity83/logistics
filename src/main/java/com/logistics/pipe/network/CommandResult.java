package com.logistics.pipe.network;

/**
 * Result of executing a {@link NetworkCommand} via {@link NetworkCommandExecutor}.
 *
 * @param itemsHandled number of items the command successfully processed (0 on failure/deferred)
 * @param success      {@code true} if the command was accepted and produced a meaningful result
 * @param deferred     {@code true} if the provider is temporarily unavailable (e.g. crafter buffer
 *                     full) and should be skipped this tick without removing it from the supply table
 */
public record CommandResult(long itemsHandled, boolean success, boolean deferred) {

    /** Successful result with the given item count. */
    public static CommandResult ok(long itemsHandled) {
        return new CommandResult(itemsHandled, true, false);
    }

    /** Failed result — command could not be executed or produced no output. */
    public static CommandResult failed() {
        return new CommandResult(0L, false, false);
    }

    /**
     * Deferred result — provider is temporarily unable to accept the dispatch (e.g. a crafter
     * whose buffer is full). The supply table entry is preserved so the item remains visible
     * in the network; the provider will be retried on the next tick.
     */
    public static CommandResult defer() {
        return new CommandResult(0L, false, true);
    }

    /** Alias for {@link #success()} — reads more naturally in conditional expressions. */
    public boolean isSuccess() {
        return success;
    }

    /** {@code true} when the provider should be skipped this tick without removing its supply. */
    public boolean isDeferred() {
        return deferred;
    }
}
