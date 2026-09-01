package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.DestinationPriority;
import com.logistics.core.lib.pipe.FluidPipeBehavior;
import com.logistics.core.lib.pipe.Module;

/** Fills adjacent fluid handlers (tanks) before spilling to other pipes. */
public final class FluidInsertionModule implements Module, FluidPipeBehavior {

    @Override
    public DestinationPriority destinationPriority(DestinationPriority priority) {
        return DestinationPriority.HANDLERS_FIRST;
    }
}
