package com.logistics;

import com.logistics.pipe.render.FluidPipeBlockEntityRenderer;
import com.logistics.pipe.render.FluidPumpBlockEntityRenderer;
import com.logistics.pipe.render.GlassTankBlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

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
    }
}
