package com.logistics.core.lib.power.fuel;

import com.logistics.core.machine.MachineContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/**
 * A source of engine burn time. On {@link #ignite}, it consumes one unit of fuel (a solid item, a
 * quantity of fluid, a reactant pair, …) and reports how many ticks of burn that bought. While the
 * engine's burn timer is non-zero the engine runs (LIT); when it hits zero the burn component asks
 * the fuel source to ignite again.
 *
 * <p>This is the seam that lets every engine variety — solid-fuel, fluid-fuel, hot-fluid, and
 * liquid+solid reactant — plug into the same burn/timer model.
 */
public interface FuelSource {

    /**
     * Attempt to start a new burn by consuming fuel.
     *
     * @return burn duration in ticks ({@code > 0}) if fuel was consumed, otherwise {@code 0}.
     */
    int ignite(MachineContext ctx);

    /** Persist any fuel-source state (most sources are stateless and need nothing). */
    default void save(CompoundTag tag, HolderLookup.Provider registries) {}

    /** Restore state written by {@link #save}. */
    default void load(CompoundTag tag, HolderLookup.Provider registries) {}
}
