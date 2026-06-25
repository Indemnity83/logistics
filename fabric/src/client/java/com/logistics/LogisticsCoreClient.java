package com.logistics;

import com.logistics.core.bootstrap.ClientDomainBootstrap;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

import static com.logistics.LogisticsMod.LOGGER;

public final class LogisticsCoreClient implements ClientDomainBootstrap {

    @Override
    public void initClient() {
        LOGGER.info("Registering core (client)");
        BlockRenderLayerMap.putBlock(LogisticsCore.BLOCK.QUARTZ_CRYSTAL, ChunkSectionLayer.TRANSLUCENT);
    }

    @Override
    public int order() {
        return -100; // Initialize core first
    }
}
