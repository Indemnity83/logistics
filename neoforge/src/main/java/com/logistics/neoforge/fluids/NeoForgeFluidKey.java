package com.logistics.neoforge.fluids;

import com.logistics.core.lib.fluids.IFluidKey;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

public final class NeoForgeFluidKey implements IFluidKey {
    private final FluidResource resource;

    public NeoForgeFluidKey(FluidResource resource) {
        if (resource == null) {
            throw new NullPointerException("resource must not be null");
        }
        this.resource = resource;
    }

    public static NeoForgeFluidKey of(FluidStack stack) {
        return new NeoForgeFluidKey(FluidResource.of(stack));
    }

    public static NeoForgeFluidKey of(FluidResource resource) {
        return new NeoForgeFluidKey(resource);
    }

    public FluidResource resource() {
        return resource;
    }

    @Override
    public Fluid getFluid() {
        return resource.getFluid();
    }

    @Override
    public DataComponentPatch getComponents() {
        return resource.getComponentsPatch();
    }

    @Override
    public boolean isBlank() {
        return resource.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof NeoForgeFluidKey other) return resource.equals(other.resource);
        if (o instanceof IFluidKey other) {
            return getFluid() == other.getFluid() && getComponents().equals(other.getComponents());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 31 * getFluid().hashCode() + getComponents().hashCode();
    }
}
