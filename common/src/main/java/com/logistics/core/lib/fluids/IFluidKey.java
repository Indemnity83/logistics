package com.logistics.core.lib.fluids;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Loader-agnostic identity token for a fluid type (ignoring amount).
 *
 * <p>Implementations must provide correct {@code equals} and {@code hashCode} semantics
 * so that instances can be used as {@link java.util.Map} keys. Two keys are equal if and
 * only if they represent the same fluid with the same components.
 *
 * <p>This is one of the primary extension points for the fluid-storage abstraction layer.
 * Loader-specific adapters ({@code FabricFluidKey}, {@code NeoForgeFluidKey}) wrap the
 * native fluid-identity type and implement this interface.
 */
public interface IFluidKey {

    /**
     * The vanilla fluid type represented by this key.
     *
     * @return the fluid; never {@code null}
     */
    Fluid getFluid();

    /**
     * The data components associated with this fluid variant.
     *
     * @return the component patch; never {@code null}
     */
    DataComponentPatch getComponents();

    /**
     * Returns {@code true} if this key represents the empty/blank fluid
     * (i.e. {@link Fluids#EMPTY}).
     *
     * @return {@code true} if blank
     */
    default boolean isBlank() {
        return getFluid() == Fluids.EMPTY;
    }
}
