package com.logistics.power.engine.magmatic;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/**
 * The Magmatic Engine's fuel recognition — lava, by tag. Unlike the Fuel Engine's per-fluid energy table,
 * no RF value lives here: a batch's energy is burn-time-based (duration × temperature-driven output), and
 * the authoritative numbers live in {@link MagmaticEngineProfile}. This single predicate drives tank-insert
 * filtering, pipe-capability acceptance, container draining, and batch ignition.
 */
public final class MagmaticEngineFuels {

    private MagmaticEngineFuels() {}

    /** Any lava-tagged fluid (so modded lava variants qualify too). */
    public static boolean isLava(@Nullable Fluid fluid) {
        return fluid != null && fluid.defaultFluidState().is(FluidTags.LAVA);
    }
}
