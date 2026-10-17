package com.logistics;

import com.logistics.core.bootstrap.ClientDomainBootstrap;
import com.logistics.forestry.farm.FarmPreviewRenderer;
import com.logistics.forestry.farm.FarmScreen;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;

import static com.logistics.LogisticsMod.LOGGER;

public final class LogisticsForestryClient implements ClientDomainBootstrap {

    @Override
    public void initClient() {
        LOGGER.info("Registering forestry (client)");
        MenuScreens.register(LogisticsForestry.MENU.FARM, FarmScreen::new);
        BlockEntityRendererRegistry.register(LogisticsForestry.ENTITY.FARM, FarmPreviewRenderer::new);
    }
}
