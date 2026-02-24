package com.logistics;

import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.fabricator.KilnScreen;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.render.MarkerBlockEntityRenderer;
import com.logistics.core.render.ModelKeyRegistry;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

import java.util.Map;

import static com.logistics.LogisticsMod.LOGGER;

public final class LogisticsCoreClient implements DomainBootstrap {
    public LogisticsCoreClient() {
        ModelLoadingPlugin.register(pluginContext -> {
            for (var entry : MODEL.getAllModels()) {
                pluginContext.addModel(entry.getKey(), SimpleUnbakedExtraModel.blockStateModel(entry.getValue().toIdentifier()));
            }
        });
    }

    @Override
    public void initCommon() {
        // Client-only bootstrap; common init handled in LogisticsCore
    }

    @Override
    public void initClient() {
        LOGGER.info("Registering core (client)");
        BlockRenderLayerMap.putBlock(LogisticsCore.BLOCK.MARKER, ChunkSectionLayer.CUTOUT);
        BlockEntityRendererRegistry.register(LogisticsCore.ENTITY.MARKER_BLOCK_ENTITY, MarkerBlockEntityRenderer::new);
        MenuScreens.register(LogisticsCore.MENU.KILN, KilnScreen::new);

        // Register molten glass fluid rendering
        FluidRenderHandlerRegistry.INSTANCE.register(
            LogisticsCore.FLUID.MOLTEN_GLASS_STILL,
            LogisticsCore.FLUID.MOLTEN_GLASS_FLOWING,
            new SimpleFluidRenderHandler(
                LogisticsMod.modId("block/core/liquid_glass").toIdentifier(),
                LogisticsMod.modId("block/core/liquid_glass").toIdentifier(),
                0xFF8800 // Orange color tint
            )
        );
    }

    @Override
    public int order() {
        return -100;  // Initialize core first
    }

    public static final class MODEL {
        private static final ModelKeyRegistry REGISTRY = new ModelKeyRegistry(LogisticsCore::model);

        public static final ExtraModelKey<BlockStateModel> BEAM = REGISTRY.registerModel("marker_beam");

        static Iterable<Map.Entry<ExtraModelKey<BlockStateModel>, ResourceId>> getAllModels() {
            return REGISTRY.getAllModels();
        }

        private MODEL() {}
    }
}