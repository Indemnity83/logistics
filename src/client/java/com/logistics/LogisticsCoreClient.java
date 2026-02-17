package com.logistics;

import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.render.MarkerBlockEntityRenderer;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ChunkSectionLayerMap;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.resources.Identifier;

import static com.logistics.LogisticsMod.LOGGER;

public final class LogisticsCoreClient implements DomainBootstrap {
    public LogisticsCoreClient() {
        ModelLoadingPlugin.register(pluginContext -> {
            Identifier beamModel = LogisticsCore.blockModelIdentifier("marker_beam");
            MODEL.BEAM = ExtraModelKey.create(beamModel::toString);
            pluginContext.addModel(MODEL.BEAM, SimpleUnbakedExtraModel.blockStateModel(beamModel));
        });
    }

    @Override
    public void initCommon() {
        // Client-only bootstrap; common init handled in LogisticsCore
    }

    @Override
    public void initClient() {
        LOGGER.info("Registering core (client)");
        ChunkSectionLayerMap.putBlock(LogisticsCore.BLOCK.MARKER, ChunkSectionLayer.CUTOUT);
        BlockEntityRendererRegistry.register(LogisticsCore.ENTITY.MARKER_BLOCK_ENTITY, MarkerBlockEntityRenderer::new);
    }

    @Override
    public int order() {
        return -100;  // Initialize core first
    }

    public static final class MODEL {
        public static ExtraModelKey<BlockStateModel> BEAM;

        private MODEL() {}
    }
}