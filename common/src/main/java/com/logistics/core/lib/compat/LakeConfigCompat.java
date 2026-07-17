package com.logistics.core.lib.compat;

import net.minecraft.world.level.levelgen.feature.LakeFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Rebuilds a {@link LakeFeature.Configuration} with a replaced barrier state provider, hiding the
 * cross-version arity of its constructor: mc/26.2 takes
 * {@code (fluid, barrier, canPlaceFeature, canReplaceWithAirOrFluid, canReplaceWithBarrier)} while
 * mc/26.1 and mc/1.21.x take {@code (fluid, barrier)}.
 *
 * <p>Per branch, only this file carries the matching constructor; the mc/26.2 form forwards the
 * three extra placement flags from {@code base}.
 */
public final class LakeConfigCompat {

    private LakeConfigCompat() {}

    public static LakeFeature.Configuration withBarrier(LakeFeature.Configuration base, BlockStateProvider barrier) {
        return new LakeFeature.Configuration(base.fluid(), barrier);
    }
}
