package com.logistics;

import com.logistics.core.bootstrap.ClientDomainBootstrap;
import com.logistics.fluid.render.FluidPipeBlockEntityRenderer;
import com.logistics.fluid.render.GlassTankBlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

import static com.logistics.LogisticsMod.LOGGER;

public final class LogisticsFluidClient implements ClientDomainBootstrap {

    @Override
    public void initClient() {
        LOGGER.info("Registering fluid (client)");

        BlockEntityRenderers.register(
                LogisticsFluid.ENTITY.FLUID_PIPE_BLOCK_ENTITY, FluidPipeBlockEntityRenderer::new);
        BlockEntityRenderers.register(
                LogisticsFluid.ENTITY.GLASS_TANK_BLOCK_ENTITY, GlassTankBlockEntityRenderer::new);
    }
}
