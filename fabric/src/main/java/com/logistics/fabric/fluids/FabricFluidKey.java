package com.logistics.fabric.fluids;

import com.logistics.core.lib.fluids.IFluidKey;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.material.Fluid;

/**
 * Fabric adapter: wraps a {@link FluidVariant} as an {@link IFluidKey}.
 *
 * <p>Delegates {@code equals} and {@code hashCode} to {@link FluidVariant} so instances
 * can be used as {@link java.util.Map} keys with correct identity semantics.
 */
public final class FabricFluidKey implements IFluidKey {

    private final FluidVariant variant;

    private FabricFluidKey(FluidVariant variant) {
        this.variant = variant;
    }

    /**
     * Create a {@link FabricFluidKey} from a {@link FluidVariant}.
     *
     * @param variant the Fabric fluid variant; must not be {@code null}
     * @return a wrapped fluid key
     */
    public static FabricFluidKey of(FluidVariant variant) {
        return new FabricFluidKey(variant);
    }

    /** Returns the underlying Fabric {@link FluidVariant}. */
    public FluidVariant variant() {
        return variant;
    }

    @Override
    public Fluid getFluid() {
        return variant.getFluid();
    }

    @Override
    public DataComponentPatch getComponents() {
        return variant.getComponentsPatch();
    }

    @Override
    public boolean isBlank() {
        return variant.isBlank();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof FabricFluidKey other) return variant.equals(other.variant);
        if (o instanceof IFluidKey other) {
            return getFluid() == other.getFluid() && getComponents().equals(other.getComponents());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return variant.hashCode();
    }
}
