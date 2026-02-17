package com.logistics;

import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.render.MarkerBlockEntityRenderer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

import static com.logistics.LogisticsMod.LOGGER;

public final class LogisticsCoreClient implements DomainBootstrap {
    public LogisticsCoreClient() {
        ModelLoadingPlugin.register(pluginContext -> {
            pluginContext.addModels(LogisticsCore.blockModelIdentifier("marker_beam"));
        });
    }

    @Override
    public void initCommon() {
        // Client-only bootstrap; common init handled in LogisticsCore
    }

    @Override
    public void initClient() {
        LOGGER.info("Registering core (client)");
        // Set marker to cutout layer for transparent texture support
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsCore.BLOCK.MARKER, RenderType.cutout());
        // Register marker block entity renderer
        BlockEntityRenderers.register(LogisticsCore.ENTITY.MARKER_BLOCK_ENTITY, MarkerBlockEntityRenderer::new);
    }

    @Override
    public int order() {
        return -100;  // Initialize core first
    }
}
