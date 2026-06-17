package com.logistics.core.lib.pipe;

/**
 * A facet a fluid pipe {@link Module} may implement to override a single fluid-transport decision. The
 * fluid pipe resolves each decision from a default plus any module overrides, so the block entity asks
 * the pipe a domain question (can it connect? what rate? what priority?) rather than checking which
 * modules are present. Mirrors the item-pipe facets ({@link RoutingModule}, {@link TickingModule}, …).
 *
 * <p>Default behavior lives here; a module overrides only the one decision it owns.
 */
public interface FluidPipeBehavior {
    /** Whether this pipe connects to external (non-pipe) fluid handlers. Default: yes. */
    default boolean canConnectToFluidHandler() {
        return true;
    }

    /** Adjust the transfer rate (mB/tick) flowing in from the previous behavior. Default: unchanged. */
    default long modifyTransferRate(long rate) {
        return rate;
    }

    /** Override the destination priority decided so far. Default: unchanged. */
    default DestinationPriority destinationPriority(DestinationPriority priority) {
        return priority;
    }
}
