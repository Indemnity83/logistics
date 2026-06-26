package com.logistics;

import com.logistics.core.bootstrap.ClientDomainBootstrap;
import com.logistics.core.render.MarkerBlockEntityRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

import static com.logistics.LogisticsMod.LOGGER;

public final class LogisticsCoreClient implements ClientDomainBootstrap {

    @Override
    public void initClient() {
        LOGGER.info("Registering core (client)");
        BlockEntityRendererRegistry.register(
                LogisticsCore.ENTITY.MARKER_BLOCK_ENTITY, MarkerBlockEntityRenderer::new);
        // The marker uses a transparent torch-style texture and needs the cutout layer; without it
        // the inactive marker's transparent pixels render as a black cross.
        BlockRenderLayerMap.putBlock(LogisticsCore.BLOCK.MARKER, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(LogisticsCore.BLOCK.QUARTZ_CRYSTAL, ChunkSectionLayer.TRANSLUCENT);
    }

    @Override
    public int order() {
        return -100; // Initialize core first
    }
}
