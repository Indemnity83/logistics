package com.logistics.core.registry;

import com.logistics.core.bootstrap.ClientDomainBootstrap;
import com.logistics.core.render.MarkerBlockEntityRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public final class CoreClientBootstrap implements ClientDomainBootstrap {
    @Override
    public void initCommon() {
        // Client-only bootstrap; common init handled in main bootstrap.
    }

    @Override
    public void initClient() {
        BlockRenderLayerMap.putBlock(CoreBlocks.MARKER, BlockRenderLayer.CUTOUT);
        BlockEntityRendererFactories.register(CoreBlockEntities.MARKER_BLOCK_ENTITY, MarkerBlockEntityRenderer::new);
    }

    @Override
    public int order() {
        return -100;
    }
}
