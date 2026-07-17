package com.logistics;

import com.logistics.core.lib.client.render.FluidBoxRenderer;
import com.logistics.core.lib.client.render.FluidSpriteLookup;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.fabric.fluids.FabricFluids;
import com.logistics.pipe.render.FluidPipeBlockEntityRenderer;
import com.logistics.pipe.render.FluidPumpBlockEntityRenderer;
import com.logistics.pipe.render.GlassTankBlockEntityRenderer;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.material.FluidState;

import static com.logistics.LogisticsMod.LOGGER;

/**
 * Fluid client registration. No longer a standalone {@code ClientDomainBootstrap}: fluid is part of the
 * pipe domain, so its renderers are registered from {@link LogisticsPipeClient#initClient()}.
 */
public final class LogisticsFluidClient {

    private LogisticsFluidClient() {}

    public static void registerClient() {
        LOGGER.info("Registering fluid (client)");

        BlockEntityRenderers.register(
                LogisticsPipe.ENTITY.FLUID_PIPE_BLOCK_ENTITY, FluidPipeBlockEntityRenderer::new);
        BlockEntityRenderers.register(
                LogisticsPipe.ENTITY.GLASS_TANK_BLOCK_ENTITY, GlassTankBlockEntityRenderer::new);
        BlockEntityRenderers.register(
                LogisticsAutomation.ENTITY.FLUID_PUMP_BLOCK_ENTITY, FluidPumpBlockEntityRenderer::new);

        // The glass tank uses translucent rendering so the contained fluid shows through. On 1.21.11
        // Fabric the model render_type JSON is not honored, so register it explicitly (NeoForge reads
        // render_type directly). The pipe cores are opaque, so they render fine on the default layer.
        BlockRenderLayerMap.putBlock(LogisticsPipe.BLOCK.GLASS_TANK, ChunkSectionLayer.TRANSLUCENT);

        registerFluidRenderers();

        // Resolve fluid still sprite + tint through Fabric's fluid render handlers (1.21.x has no unified
        // vanilla fluid model). Shared fluid pipe/tank renderers call this via FluidSpriteLookup.
        FluidSpriteLookup.register((fluid, level, pos) -> {
            FluidState fluidState = fluid.defaultFluidState();
            FluidRenderHandler handler = FluidRenderHandlerRegistry.INSTANCE.get(fluid);
            if (handler == null) {
                return null;
            }
            TextureAtlasSprite[] sprites = handler.getFluidSprites(level, pos, fluidState);
            if (sprites == null || sprites.length == 0 || sprites[0] == null) {
                return null;
            }
            return new FluidBoxRenderer.Appearance(sprites[0], handler.getFluidColor(level, pos, fluidState));
        });
    }

    /** Renders each custom fluid from its own still/flow sprites with a flat per-fluid tint. */
    private static void registerFluidRenderers() {
        Map<String, LogisticsCore.FluidDef> defs = new HashMap<>();
        for (LogisticsCore.FluidDef def : LogisticsCore.CUSTOM_FLUIDS) {
            defs.put(def.name(), def);
        }

        FabricFluids.sources().forEach((name, source) -> {
            LogisticsCore.FluidDef def = defs.get(name);
            int tint = def.tint() | 0xFF000000;
            SimpleFluidRenderHandler handler = def.overlay() != null
                    ? new SimpleFluidRenderHandler(
                            ResourceId.parse(def.still()).toIdentifier(),
                            ResourceId.parse(def.flow()).toIdentifier(),
                            ResourceId.parse(def.overlay()).toIdentifier(),
                            tint)
                    : new SimpleFluidRenderHandler(
                            ResourceId.parse(def.still()).toIdentifier(),
                            ResourceId.parse(def.flow()).toIdentifier(),
                            tint);
            FluidRenderHandlerRegistry.INSTANCE.register(source, source.getFlowing(), handler);
        });
    }
}
