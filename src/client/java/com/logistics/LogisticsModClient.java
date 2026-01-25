package com.logistics;

import com.logistics.block.LogisticsBlocks;
import com.logistics.block.entity.LogisticsBlockEntities;
import com.logistics.client.render.MarkerBlockEntityRenderer;
import com.logistics.client.render.PipeBlockEntityRenderer;
import com.logistics.client.render.PipeModelRegistry;
import com.logistics.client.screen.ItemFilterScreen;
import com.logistics.client.screen.QuarryScreen;
import com.logistics.marker.MarkerBlockEntities;
import com.logistics.marker.MarkerBlocks;
import com.logistics.pipe.ui.PipeScreenHandlers;
import com.logistics.quarry.ui.QuarryScreenHandlers;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public class LogisticsModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LogisticsMod.LOGGER.info("Initializing Logistics client");

        PipeModelRegistry.register();

        // Register pipe blocks to render with transparency
        BlockRenderLayerMap.putBlock(LogisticsBlocks.STONE_TRANSPORT_PIPE, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(LogisticsBlocks.COPPER_TRANSPORT_PIPE, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(LogisticsBlocks.ITEM_EXTRACTOR_PIPE, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(LogisticsBlocks.ITEM_MERGER_PIPE, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(LogisticsBlocks.GOLD_TRANSPORT_PIPE, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(LogisticsBlocks.ITEM_FILTER_PIPE, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(LogisticsBlocks.ITEM_INSERTION_PIPE, BlockRenderLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(LogisticsBlocks.ITEM_VOID_PIPE, BlockRenderLayer.CUTOUT);

        // Register marker block to render with transparency
        BlockRenderLayerMap.putBlock(MarkerBlocks.MARKER, BlockRenderLayer.CUTOUT);

        // Register block entity renderer for traveling items
        BlockEntityRendererFactories.register(LogisticsBlockEntities.PIPE_BLOCK_ENTITY, PipeBlockEntityRenderer::new);

        // Register block entity renderer for markers
        BlockEntityRendererFactories.register(MarkerBlockEntities.MARKER_BLOCK_ENTITY, MarkerBlockEntityRenderer::new);

        HandledScreens.register(PipeScreenHandlers.ITEM_FILTER, ItemFilterScreen::new);
        HandledScreens.register(QuarryScreenHandlers.QUARRY, QuarryScreen::new);
    }
}
