package com.logistics;

import com.logistics.core.lib.client.render.FluidBoxRenderer;
import com.logistics.core.lib.client.render.FluidSpriteLookup;
import com.logistics.pipe.render.FluidPipeBlockEntityRenderer;
import com.logistics.pipe.render.FluidPumpBlockEntityRenderer;
import com.logistics.pipe.render.GlassTankBlockEntityRenderer;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
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
                LogisticsFluid.ENTITY.FLUID_PIPE_BLOCK_ENTITY, FluidPipeBlockEntityRenderer::new);
        BlockEntityRenderers.register(
                LogisticsFluid.ENTITY.GLASS_TANK_BLOCK_ENTITY, GlassTankBlockEntityRenderer::new);
        BlockEntityRenderers.register(
                LogisticsFluid.ENTITY.FLUID_PUMP_BLOCK_ENTITY, FluidPumpBlockEntityRenderer::new);

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
}
