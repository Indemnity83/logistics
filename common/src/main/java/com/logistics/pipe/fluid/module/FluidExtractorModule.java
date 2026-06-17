package com.logistics.pipe.fluid.module;

import com.logistics.core.lib.pipe.Module;

/**
 * Marks a fluid pipe as a powered extractor: it pulls fluid from the handler on its wrench-set pull face
 * when it has energy. Pairs with {@code FluidPipe.withEnergy()} so the block entity creates an energy
 * store. The block entity reads this marker to drive its extraction tick and pull-face logic.
 */
public final class FluidExtractorModule implements Module {}
