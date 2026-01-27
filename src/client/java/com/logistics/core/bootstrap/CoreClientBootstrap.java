package com.logistics.core.bootstrap;

import com.logistics.client.render.MarkerBlockEntityRenderer;
import com.logistics.core.registry.CoreBlockEntities;
import com.logistics.core.registry.CoreBlocks;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public final class CoreClientBootstrap implements DomainBootstrap {
    @Override
    public void initCommon() {
        // Client-only bootstrap; common init handled in main bootstrap.
    }

    @Override
    public void initClient() {
        BlockRenderLayerMap.putBlock(CoreBlocks.MARKER, BlockRenderLayer.CUTOUT);
        BlockEntityRendererFactories.register(
                CoreBlockEntities.MARKER_BLOCK_ENTITY, MarkerBlockEntityRenderer::new);
    }

    @Override
    public int order() {
        return -100;
    }
}
