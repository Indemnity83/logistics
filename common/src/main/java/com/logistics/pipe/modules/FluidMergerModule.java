package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.Module;

/**
 * Marks a fluid pipe as a directional check valve: a wrench-set output face is the only face fluid may
 * leave, and fluid never flows back in through it. The block entity reads this marker to gate its
 * candidate-output and check-valve logic and to enable the wrench feature-face cycling.
 */
public final class FluidMergerModule implements Module {}
