package com.logistics;

import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.fabricator.KilnScreen;
import com.logistics.core.render.MarkerBlockEntityRenderer;
import com.logistics.core.render.ModelKeyRegistry;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.minecraft.client.gui.screens.MenuScreens;

import static com.logistics.LogisticsMod.LOGGER;

public final class LogisticsCoreClient implements DomainBootstrap {
    public LogisticsCoreClient() {
        ModelLoadingPlugin.register(pluginContext -> {
            pluginContext.addModels(MODEL.getAllModels());
        });
    }

    @Override
    public void initCommon() {
        // Client-only bootstrap; common init handled in LogisticsCore
    }

    @Override
    public void initClient() {
        LOGGER.info("Registering core (client)");
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsCore.BLOCK.MARKER, RenderType.cutout());
        BlockEntityRenderers.register(LogisticsCore.ENTITY.MARKER_BLOCK_ENTITY, MarkerBlockEntityRenderer::new);
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
        private static final ModelKeyRegistry REGISTRY = new ModelKeyRegistry(name -> LogisticsCore.model(name).toIdentifier());

        public static final ResourceLocation BEAM = REGISTRY.registerModel("marker_beam");

        static ResourceLocation[] getAllModels() {
            return REGISTRY.getAllModels();
        }

        private MODEL() {}
    }
}
