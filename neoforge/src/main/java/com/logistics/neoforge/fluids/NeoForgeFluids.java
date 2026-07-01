package com.logistics.neoforge.fluids;

import com.logistics.LogisticsCore;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.FlowingFluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Registers the custom fluids on NeoForge: a {@link FluidType} per fluid plus a {@link BaseFlowingFluid}
 * source/flowing pair, all under {@code logistics:core/<name>}. No world block/bucket (tank/pipe contents
 * only). Client textures + tint are supplied separately via {@code IClientFluidTypeExtensions}.
 */
public final class NeoForgeFluids {

    private static final Map<String, FluidType> TYPES = new LinkedHashMap<>();
    private static final Map<String, FlowingFluid> SOURCES = new LinkedHashMap<>();
    private static final Map<String, FlowingFluid> FLOWINGS = new LinkedHashMap<>();

    private NeoForgeFluids() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(NeoForgeFluids::onRegister);
    }

    private static void onRegister(RegisterEvent event) {
        event.register(NeoForgeRegistries.Keys.FLUID_TYPES, helper -> {
            for (LogisticsCore.FluidDef def : LogisticsCore.CUSTOM_FLUIDS) {
                FluidType type = new FluidType(
                        FluidType.Properties.create().descriptionId("fluid.logistics.core." + def.name()));
                helper.register(LogisticsCore.resource(def.name()).toIdentifier(), type);
                TYPES.put(def.name(), type);
            }
        });
        event.register(Registries.FLUID, helper -> {
            for (LogisticsCore.FluidDef def : LogisticsCore.CUSTOM_FLUIDS) {
                String name = def.name();
                BaseFlowingFluid.Properties props = new BaseFlowingFluid.Properties(
                        () -> TYPES.get(name), () -> SOURCES.get(name), () -> FLOWINGS.get(name))
                        .bucket(() -> LogisticsCore.BUCKET.forFluid(name));
                BaseFlowingFluid.Source source = new BaseFlowingFluid.Source(props);
                BaseFlowingFluid.Flowing flowing = new BaseFlowingFluid.Flowing(props);
                SOURCES.put(name, source);
                FLOWINGS.put(name, flowing);
                helper.register(LogisticsCore.resource(name).toIdentifier(), source);
                helper.register(LogisticsCore.resource("flowing_" + name).toIdentifier(), flowing);
            }
        });
    }

    /** Registered fluid types by name — for the client texture/tint extensions. */
    public static Map<String, FluidType> types() {
        return TYPES;
    }

    /** Registered source fluids by name — for client fluid-model registration. */
    public static Map<String, FlowingFluid> sources() {
        return SOURCES;
    }

    /** Registered flowing fluids by name — for client fluid-model registration. */
    public static Map<String, FlowingFluid> flowings() {
        return FLOWINGS;
    }
}
