package com.logistics.neoforge.fluids;

import com.logistics.core.lib.fluids.IFluidKey;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * NeoForge 21.1 implementation of {@link IFluidKey}.
 *
 * <p>Wraps a {@link FluidStack} (ignoring amount) as the loader-agnostic fluid identity type.
 * Two keys are equal when they represent the same fluid type and components.
 */
public final class NeoForgeFluidKey implements IFluidKey {
    private final Fluid fluid;
    private final DataComponentPatch components;

    private NeoForgeFluidKey(Fluid fluid, DataComponentPatch components) {
        if (fluid == null) throw new NullPointerException("fluid must not be null");
        if (components == null) throw new NullPointerException("components must not be null");
        this.fluid = fluid;
        this.components = components;
    }

    /**
     * Create a {@link NeoForgeFluidKey} from a {@link FluidStack}.
     *
     * @param stack the stack whose fluid type to capture; amount is ignored
     * @return the key
     */
    public static NeoForgeFluidKey of(FluidStack stack) {
        return new NeoForgeFluidKey(stack.getFluid(), stack.getComponentsPatch());
    }

    /**
     * Create a {@link FluidStack} with the given amount from this key.
     *
     * @param amount the desired amount in millibuckets
     * @return a new FluidStack representing this fluid
     */
    public FluidStack toStack(int amount) {
        return new FluidStack(fluid, amount, components);
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
        if (this == o) return true;
        if (!(o instanceof IFluidKey other)) return false;
        return fluid == other.getFluid() && components.equals(other.getComponents());
    }

    @Override
    public int hashCode() {
        return 31 * fluid.hashCode() + components.hashCode();
    }
}
