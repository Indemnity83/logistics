package com.logistics.pipe.client;

import com.logistics.LogisticsCore;
import com.logistics.LogisticsPipe;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.pipe.data.PipeDataComponents.FluidPacket;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Item color provider for the hidden fluid-packet item: tints layer1 (the window) with the color of
 * the carried fluid. Loader-agnostic so Fabric and NeoForge register the SAME logic.
 */
public final class FluidPacketColors {
    private FluidPacketColors() {}

    private static final int UNTINTED = 0xFFFFFFFF;
    private static final int WATER_TINT = 0xFF3F76E4;

    /** Fluid registry id string -> full-alpha ARGB tint, built once from {@link LogisticsCore#CUSTOM_FLUIDS}. */
    private static Map<String, Integer> customTints;

    /** Item color hook: {@code tintIndex 1} tints the fluid window; everything else is untinted. */
    public static int tintFor(ItemStack stack, int tintIndex) {
        if (tintIndex != 1) {
            return UNTINTED;
        }
        FluidPacket packet = stack.get(LogisticsPipe.DATA.FLUID_PACKET);
        if (packet == null) {
            return UNTINTED;
        }
        return tintFor(packet.fluid());
    }

    private static int tintFor(Fluid fluid) {
        if (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER) {
            return WATER_TINT;
        }
        if (fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA) {
            return UNTINTED; // lava sprites are already bright; leave it un-darkened
        }
        var key = ResourceId.wrap(BuiltInRegistries.FLUID.getKey(fluid)).toString();
        Integer tint = tints().get(key);
        return tint != null ? tint : UNTINTED;
    }

    private static Map<String, Integer> tints() {
        Map<String, Integer> map = customTints;
        if (map == null) {
            map = new HashMap<>();
            for (LogisticsCore.FluidDef def : LogisticsCore.CUSTOM_FLUIDS) {
                int tint = def.tint() | 0xFF000000;
                map.put(LogisticsCore.resource(def.name()).toString(), tint);
                map.put(LogisticsCore.resource("flowing_" + def.name()).toString(), tint);
            }
            customTints = map;
        }
        return map;
    }
}
