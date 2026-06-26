package com.logistics;

import com.logistics.core.bootstrap.ClientDomainBootstrap;
import com.logistics.core.render.MarkerBlockEntityRenderer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.renderer.RenderType;

import static com.logistics.LogisticsMod.LOGGER;

public final class LogisticsCoreClient implements ClientDomainBootstrap {

    @Override
    public void initClient() {
        LOGGER.info("Registering core (client)");
        BlockEntityRendererRegistry.register(
                LogisticsCore.ENTITY.MARKER_BLOCK_ENTITY, MarkerBlockEntityRenderer::new);
        // The marker uses a transparent torch-style texture and needs the cutout layer; without it
        // the inactive marker's transparent pixels render as a black cross.
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsCore.BLOCK.MARKER, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsCore.BLOCK.QUARTZ_CRYSTAL, RenderType.translucent());
    }

    @Override
    public int order() {
        return -100; // Initialize core first
    }
}
