package com.logistics.core.lib.fluids;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * A concrete, immutable {@link IFluidKey} over a vanilla {@link Fluid} and component patch.
 *
 * <p>Useful for constructing keys in common code (e.g. water, a potion-bearing water variant) without
 * a loader-specific fluid type. Equality matches any other {@link IFluidKey} with the same fluid and
 * components, consistent with the loader adapters.
 */
public record SimpleFluidKey(Fluid fluid, DataComponentPatch components) implements IFluidKey {

    public static final SimpleFluidKey BLANK = new SimpleFluidKey(Fluids.EMPTY, DataComponentPatch.EMPTY);

    public static SimpleFluidKey of(Fluid fluid) {
        return new SimpleFluidKey(fluid, DataComponentPatch.EMPTY);
    }

    @Override
    public Fluid getFluid() {
        return fluid;
    }

    @Override
    public DataComponentPatch getComponents() {
        return components;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof IFluidKey other
                && fluid == other.getFluid()
                && components.equals(other.getComponents());
    }

    @Override
    public int hashCode() {
        return 31 * fluid.hashCode() + components.hashCode();
    }
}
