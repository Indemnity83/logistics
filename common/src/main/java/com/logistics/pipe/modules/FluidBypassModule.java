package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.FluidPipeBehavior;
import com.logistics.core.lib.pipe.Module;

/** Restricts a fluid pipe to pipe-to-pipe connections only — it never connects to external fluid handlers. */
public final class FluidBypassModule implements Module, FluidPipeBehavior {

    @Override
    public boolean canConnectToFluidHandler() {
        return false;
    }
}
