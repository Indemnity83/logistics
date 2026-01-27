package com.logistics;

import com.logistics.block.LogisticsBlocks;
import com.logistics.block.entity.LogisticsBlockEntities;
import com.logistics.client.render.MarkerBlockEntityRenderer;
import com.logistics.client.render.PipeBlockEntityRenderer;
import com.logistics.client.render.PipeModelRegistry;
import com.logistics.client.render.QuarryBlockEntityRenderer;
import com.logistics.client.render.QuarryRenderState;
import com.logistics.client.screen.ItemFilterScreen;
import com.logistics.client.screen.QuarryScreen;
import com.logistics.ui.LogisticsScreenHandlers;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
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
        BlockRenderLayerMap.putBlock(LogisticsBlocks.MARKER, BlockRenderLayer.CUTOUT);

        // Register quarry frame to render with transparency
        BlockRenderLayerMap.putBlock(LogisticsBlocks.QUARRY_FRAME, BlockRenderLayer.CUTOUT);

        // Register block entity renderer for traveling items
        BlockEntityRendererFactories.register(LogisticsBlockEntities.PIPE_BLOCK_ENTITY, PipeBlockEntityRenderer::new);

        // Register block entity renderer for markers
        BlockEntityRendererFactories.register(LogisticsBlockEntities.MARKER_BLOCK_ENTITY, MarkerBlockEntityRenderer::new);

        // Register block entity renderer for quarry arm
        BlockEntityRendererFactories.register(LogisticsBlockEntities.QUARRY_BLOCK_ENTITY, QuarryBlockEntityRenderer::new);

        HandledScreens.register(LogisticsScreenHandlers.ITEM_FILTER, ItemFilterScreen::new);
        HandledScreens.register(LogisticsScreenHandlers.QUARRY, QuarryScreen::new);

        ClientTickEvents.END_WORLD_TICK.register(QuarryRenderState::pruneInterpolationCache);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> QuarryRenderState.clearAllInterpolationCaches());
    }
}
