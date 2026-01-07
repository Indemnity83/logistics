package com.logistics;

import com.logistics.block.LogisticsBlocks;
import com.logistics.block.entity.LogisticsBlockEntities;
import com.logistics.client.render.PipeBlockEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public class LogisticsModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		LogisticsMod.LOGGER.info("Initializing Logistics client");

		// Register pipe blocks to render with transparency
		BlockRenderLayerMap.INSTANCE.putBlock(LogisticsBlocks.COBBLESTONE_PIPE, RenderLayer.getCutout());
		BlockRenderLayerMap.INSTANCE.putBlock(LogisticsBlocks.STONE_PIPE, RenderLayer.getCutout());
		BlockRenderLayerMap.INSTANCE.putBlock(LogisticsBlocks.WOOD_PIPE, RenderLayer.getCutout());
		BlockRenderLayerMap.INSTANCE.putBlock(LogisticsBlocks.IRON_PIPE, RenderLayer.getCutout());
		BlockRenderLayerMap.INSTANCE.putBlock(LogisticsBlocks.GOLD_PIPE, RenderLayer.getCutout());
		BlockRenderLayerMap.INSTANCE.putBlock(LogisticsBlocks.DIAMOND_PIPE, RenderLayer.getCutout());
		BlockRenderLayerMap.INSTANCE.putBlock(LogisticsBlocks.COPPER_PIPE, RenderLayer.getCutout());
		BlockRenderLayerMap.INSTANCE.putBlock(LogisticsBlocks.QUARTZ_PIPE, RenderLayer.getCutout());
		BlockRenderLayerMap.INSTANCE.putBlock(LogisticsBlocks.VOID_PIPE, RenderLayer.getCutout());

		// Register block entity renderer for traveling items
		BlockEntityRendererFactories.register(LogisticsBlockEntities.PIPE_BLOCK_ENTITY, PipeBlockEntityRenderer::new);
	}
}
