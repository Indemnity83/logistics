package com.logistics.core.lib.fluids;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;

/**
 * Shared fluid display-name resolution: a fluid's world-block name (water, lava), or an id-derived lang
 * key for block-less fluids (our custom fluids). Vanilla's {@code toLanguageKey} leaves the path's
 * {@code /} intact ({@code fluid.logistics.core/liquid_redstone}), but our lang keys use dots, so the key
 * is built here with {@code /} → {@code .}.
 */
public final class FluidDisplay {

    private FluidDisplay() {}

    public static Component name(Fluid fluid) {
        var block = fluid.defaultFluidState().createLegacyBlock().getBlock();
        if (block != Blocks.AIR) {
            return block.getName();
        }
        var id = BuiltInRegistries.FLUID.getKey(fluid);
        return Component.translatable("fluid." + id.getNamespace() + "." + id.getPath().replace('/', '.'));
    }
}
