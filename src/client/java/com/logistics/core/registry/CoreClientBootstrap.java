package com.logistics.core.registry;

import com.logistics.core.bootstrap.ClientDomainBootstrap;
import com.logistics.core.render.MarkerBlockEntityRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ChunkSectionLayerMap;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

public final class CoreClientBootstrap implements ClientDomainBootstrap {
    @Override
    public void initCommon() {
        // Client-only bootstrap; common init handled in main bootstrap.
    }

    @Override
    public void initClient() {
        ChunkSectionLayerMap.putBlock(CoreBlocks.MARKER, ChunkSectionLayer.CUTOUT);
        BlockEntityRendererRegistry.register(CoreBlockEntities.MARKER_BLOCK_ENTITY, MarkerBlockEntityRenderer::new);
    }

    @Override
    public int order() {
        return -100;
    }
}
