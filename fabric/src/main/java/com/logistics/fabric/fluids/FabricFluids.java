package com.logistics.fabric.fluids;

import com.logistics.LogisticsCore;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * Registers the custom fluids (source + flowing pair per {@link LogisticsFluid#CUSTOM_FLUIDS}) into
 * {@code BuiltInRegistries.FLUID} on Fabric. Keeps the registered source fluids by name so the client
 * render handlers and bucket items can look them up.
 */
public final class FabricFluids {

    private static final Map<String, FlowingFluid> SOURCES = new LinkedHashMap<>();

    private FabricFluids() {}

    public static void register() {
        for (LogisticsCore.FluidDef def : LogisticsCore.CUSTOM_FLUIDS) {
            String name = def.name();
            FlowingFluid[] pair = new FlowingFluid[2]; // [0] source, [1] flowing
            pair[0] = new FabricLogisticsFluid(true, () -> pair[0], () -> pair[1], () -> null);
            pair[1] = new FabricLogisticsFluid(false, () -> pair[0], () -> pair[1], () -> null);
            Registry.register(BuiltInRegistries.FLUID, LogisticsCore.resource(name).toIdentifier(), pair[0]);
            Registry.register(BuiltInRegistries.FLUID, LogisticsCore.resource("flowing_" + name).toIdentifier(), pair[1]);
            SOURCES.put(name, pair[0]);
        }
    }

    /** Registered source fluids by name (in {@link LogisticsFluid#CUSTOM_FLUIDS} order). */
    public static Map<String, FlowingFluid> sources() {
        return SOURCES;
    }
}
