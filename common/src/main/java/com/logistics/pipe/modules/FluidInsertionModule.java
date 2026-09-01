package com.logistics.pipe.modules;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPipe;
import com.logistics.core.lib.pipe.DestinationPriority;
import com.logistics.core.lib.pipe.FluidPipeBehavior;
import com.logistics.core.lib.pipe.Module;

/**
 * Fills adjacent fluid handlers (tanks) before spilling to other pipes.
 *
 * <p>Widens the buffer to the handler-boundary capacity for the same reason as the extractor: an
 * all-or-nothing handler such as a cauldron accepts a whole level at a time, so a pipe that cannot hold one
 * whole level can never fill it.
 */
public final class FluidInsertionModule implements Module, FluidPipeBehavior {

    @Override
    public DestinationPriority destinationPriority(DestinationPriority priority) {
        return DestinationPriority.HANDLERS_FIRST;
    }

    @Override
    public long modifyCapacity(long capacity) {
        return Math.max(capacity, LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PIPE_HANDLER_CAPACITY));
    }
}
