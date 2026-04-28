package com.logistics;

import com.logistics.core.bootstrap.DomainBootstrap;
import com.logistics.core.macerator.MaceratorScreen;
import com.logistics.core.render.ModelKeyRegistry;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

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

        MenuScreens.register(LogisticsCore.MENU.MACERATOR, MaceratorScreen::new);
        BlockRenderLayerMap.INSTANCE.putBlock(LogisticsCore.BLOCK.QUARTZ_CRYSTAL, RenderType.translucent());
    }

    @Override
    public int order() {
        return -100;  // Initialize core first
    }

    public static final class MODEL {
        private static final ModelKeyRegistry REGISTRY = new ModelKeyRegistry(name -> LogisticsCore.model(name).toIdentifier());

        static ResourceLocation[] getAllModels() {
            return REGISTRY.getAllModels();
        }

        private MODEL() {}
    }
}
