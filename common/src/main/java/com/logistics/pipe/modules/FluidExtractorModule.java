package com.logistics.pipe.modules;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPipe;
import com.logistics.core.lib.pipe.FluidPipeBehavior;
import com.logistics.core.lib.pipe.Module;

/**
 * Marks a fluid pipe as a powered extractor: it pulls fluid from the handler on its wrench-set pull face
 * when it has energy. Pairs with {@code FluidPipe.withEnergy()} so the block entity creates an energy
 * store. The block entity reads this marker to drive its extraction tick and pull-face logic.
 *
 * <p>Widens the buffer to the extraction capacity: an all-or-nothing source such as a cauldron parts with a
 * whole level at a time (a bucket of lava, a third of a bucket of water), so a pipe that cannot hold one
 * whole level can never take anything from it.
 */
public final class FluidExtractorModule implements Module, FluidPipeBehavior {

    @Override
    public long modifyCapacity(long capacity) {
        return Math.max(capacity, LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PIPE_EXTRACTOR_CAPACITY));
    }
}
