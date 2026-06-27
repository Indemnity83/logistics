package com.logistics.core.crash;

/**
 * Bridges Logistics' own ERROR log events into a crash-report sink. Implementations attach to the
 * logging backend (e.g. Log4j2) and forward only exceptions logged under the Logistics loggers.
 *
 * <p>Kept as an interface so {@link CrashReporting} stays free of logging-backend imports and so a
 * missing/incompatible backend can degrade to a no-op.
 */
public interface LogisticsErrorLogBridge {
    /**
     * Begin forwarding Logistics ERROR-level exceptions to the sink. Returns false if it could not
     * attach (e.g. an incompatible logging backend), so callers don't claim capture is wired.
     */
    boolean attach();

    /** Stop forwarding and detach from the logging backend. */
    void detach();
}
