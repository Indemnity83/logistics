package com.logistics;

import com.logistics.block.LogisticsBlocks;
import com.logistics.block.entity.LogisticsBlockEntities;
import com.logistics.client.render.PipeBlockEntityRenderer;
import com.logistics.client.screen.DiamondFilterScreen;
import com.logistics.pipe.ui.PipeScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public class LogisticsModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		LogisticsMod.LOGGER.info("Initializing Logistics client");

		// Register pipe blocks to render with transparency
		BlockRenderLayerMap.putBlock(LogisticsBlocks.STONE_TRANSPORT_PIPE, BlockRenderLayer.CUTOUT);
		BlockRenderLayerMap.putBlock(LogisticsBlocks.COPPER_TRANSPORT_PIPE, BlockRenderLayer.CUTOUT);
		BlockRenderLayerMap.putBlock(LogisticsBlocks.BASIC_EXTRACTOR_PIPE, BlockRenderLayer.CUTOUT);
		BlockRenderLayerMap.putBlock(LogisticsBlocks.BASIC_MERGER_PIPE, BlockRenderLayer.CUTOUT);
		BlockRenderLayerMap.putBlock(LogisticsBlocks.GOLD_TRANSPORT_PIPE, BlockRenderLayer.CUTOUT);
		BlockRenderLayerMap.putBlock(LogisticsBlocks.SMART_SPLITTER_PIPE, BlockRenderLayer.CUTOUT);
		BlockRenderLayerMap.putBlock(LogisticsBlocks.COMPARATOR_PIPE, BlockRenderLayer.CUTOUT);
		BlockRenderLayerMap.putBlock(LogisticsBlocks.VOID_PIPE, BlockRenderLayer.CUTOUT);

		// Register block entity renderer for traveling items
		BlockEntityRendererFactories.register(LogisticsBlockEntities.PIPE_BLOCK_ENTITY, PipeBlockEntityRenderer::new);

		HandledScreens.register(PipeScreenHandlers.SMART_SPLITTER_FILTER, DiamondFilterScreen::new);
	}
}
