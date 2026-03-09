package com.logistics.pipe.network;

/**
 * Result of executing a {@link NetworkCommand} via {@link NetworkCommandExecutor}.
 *
 * @param itemsHandled number of items the command successfully processed (0 on failure)
 * @param success      {@code true} if the command was accepted and produced a meaningful result
 */
public record CommandResult(long itemsHandled, boolean success) {

    /** Successful result with the given item count. */
    public static CommandResult ok(long itemsHandled) {
        return new CommandResult(itemsHandled, true);
    }

    /** Failed result — command could not be executed or produced no output. */
    public static CommandResult failed() {
        return new CommandResult(0L, false);
    }

    /** Alias for {@link #success()} — reads more naturally in conditional expressions. */
    public boolean isSuccess() {
        return success;
    }
}
