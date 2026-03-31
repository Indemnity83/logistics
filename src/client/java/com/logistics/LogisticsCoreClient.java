package com.logistics;

import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.render.ModelKeyRegistry;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.minecraft.client.renderer.block.model.BlockStateModel;

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

        // Register molten glass fluid rendering
        var liquidGlassTexture = LogisticsMod.modId("block/core/liquid_glass").toIdentifier();
        FluidRenderHandlerRegistry.INSTANCE.register(
            LogisticsCore.FLUID.MOLTEN_GLASS_STILL,
            LogisticsCore.FLUID.MOLTEN_GLASS_FLOWING,
            new SimpleFluidRenderHandler(liquidGlassTexture, liquidGlassTexture, 0xFF8800)
        );
    }

    @Override
    public int order() {
        return -100;  // Initialize core first
    }

    public static final class MODEL {
        private static final ModelKeyRegistry REGISTRY = new ModelKeyRegistry(LogisticsCore::model);

        static Iterable<Map.Entry<ExtraModelKey<BlockStateModel>, ResourceId>> getAllModels() {
            return REGISTRY.getAllModels();
        }

        private MODEL() {}
    }
}
