package com.logistics.pipe.modules;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPipe;
import com.logistics.core.lib.pipe.Module;
import com.logistics.core.lib.pipe.PipeContext;

public class TransportModule implements Module {

    @Override
    public float getMaxSpeed(PipeContext ctx) {
        return LogisticsConfigHost.get(LogisticsPipe.CONFIG.PIPE_MIN_SPEED);
    }

    @Override
    public float getDrag(PipeContext ctx) {
        return LogisticsConfigHost.get(LogisticsPipe.CONFIG.PIPE_DRAG);
    }
}
