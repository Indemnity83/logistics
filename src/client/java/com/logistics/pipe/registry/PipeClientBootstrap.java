package com.logistics.pipe.registry;

import com.logistics.client.render.PipeBlockEntityRenderer;
import com.logistics.client.render.PipeModelRegistry;
import com.logistics.client.screen.ItemFilterScreen;
import com.logistics.core.bootstrap.DomainBootstrap;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public final class PipeClientBootstrap implements DomainBootstrap {
    @Override
    public void initCommon() {
        // Client-only bootstrap; common init handled in main bootstrap.
    }

    @Override
    public void initClient() {
        PipeModelRegistry.register();

        BlockRenderLayerMap.putBlock(PipeBlocks.STONE_TRANSPORT_PIPE, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(PipeBlocks.COPPER_TRANSPORT_PIPE, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(PipeBlocks.ITEM_EXTRACTOR_PIPE, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(PipeBlocks.ITEM_MERGER_PIPE, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(PipeBlocks.GOLD_TRANSPORT_PIPE, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(PipeBlocks.ITEM_FILTER_PIPE, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(PipeBlocks.ITEM_INSERTION_PIPE, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(PipeBlocks.ITEM_VOID_PIPE, BlockRenderLayer.CUTOUT);

        BlockEntityRendererFactories.register(PipeBlockEntities.PIPE_BLOCK_ENTITY, PipeBlockEntityRenderer::new);

        HandledScreens.register(PipeScreenHandlers.ITEM_FILTER, ItemFilterScreen::new);
    }
}
