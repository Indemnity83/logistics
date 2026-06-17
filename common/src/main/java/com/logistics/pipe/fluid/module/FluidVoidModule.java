package com.logistics.pipe.fluid.module;

import com.logistics.core.lib.pipe.Module;

/**
 * Marks a fluid pipe as a void sink: ready fluid is destroyed rather than moved on. The block entity
 * reads this marker to switch its tick from the normal move path to the destroy path.
 */
public final class FluidVoidModule implements Module {}
