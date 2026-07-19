package com.logistics.power.engine.fuel;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/**
 * The Fuel Engine's supported coolants and their properties. Water (by the {@link FluidTags#WATER}
 * tag, so modded water variants also qualify) is the only coolant for now; extensible later without
 * touching the engine tick logic.
 */
public final class FuelEngineCoolants {

    private static final FuelEngineCoolant WATER = new FuelEngineCoolant(4.0, 6.0);

    private FuelEngineCoolants() {}

    @Nullable
    public static FuelEngineCoolant lookup(@Nullable Fluid fluid) {
        if (fluid == null) {
            return null;
        }
        return fluid.defaultFluidState().is(FluidTags.WATER) ? WATER : null;
    }

    /** Single source of truth shared with the coolant tank's insert filter. */
    public static boolean isCoolant(@Nullable Fluid fluid) {
        return lookup(fluid) != null;
    }
}
