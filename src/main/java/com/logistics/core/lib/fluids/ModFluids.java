package com.logistics.core.lib.fluids;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;

/**
 * Registry for custom fluids used by Logistics machines.
 * Currently provides molten glass for the Kiln.
 */
public final class ModFluids {

    // Molten Glass - used by Kiln
    public static final FlowingFluid MOLTEN_GLASS_FLOWING;
    public static final Fluid MOLTEN_GLASS_STILL;

    static {
        // Register flowing variant first (required by still variant)
        MOLTEN_GLASS_FLOWING = Registry.register(
                BuiltInRegistries.FLUID,
                Identifier.parse("logistics:molten_glass_flowing"),
                new MoltenGlassFluid.Flowing()
        );

        // Register still variant
        MOLTEN_GLASS_STILL = Registry.register(
                BuiltInRegistries.FLUID,
                Identifier.parse("logistics:molten_glass"),
                new MoltenGlassFluid.Still()
        );
    }

    /**
     * Initializes fluid registration.
     * Call this during common initialization.
     */
    public static void register() {
        // Static initialization handles registration
    }

    private ModFluids() {
        // Utility class
    }
}
