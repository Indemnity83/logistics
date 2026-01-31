package com.logistics.power;

import com.logistics.core.bootstrap.ClientDomainBootstrap;
import com.logistics.power.registry.PowerBlockEntities;
import com.logistics.power.registry.PowerScreenHandlers;
import com.logistics.power.render.EngineBlockEntityRenderer;
import com.logistics.power.screen.StirlingEngineScreen;

import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public final class PowerClientBootstrap implements ClientDomainBootstrap {
    @Override
    public void initCommon() {
        // Client-only bootstrap; common init handled in main bootstrap.
    }

    @Override
    public void initClient() {
        // Register engine block entity renderers
        BlockEntityRendererFactories.register(
                PowerBlockEntities.REDSTONE_ENGINE_BLOCK_ENTITY, EngineBlockEntityRenderer::new);
        BlockEntityRendererFactories.register(
                PowerBlockEntities.STIRLING_ENGINE_BLOCK_ENTITY, EngineBlockEntityRenderer::new);
        BlockEntityRendererFactories.register(
                PowerBlockEntities.CREATIVE_ENGINE_BLOCK_ENTITY, EngineBlockEntityRenderer::new);

        // Register screens
        HandledScreens.register(PowerScreenHandlers.STIRLING_ENGINE, StirlingEngineScreen::new);

        // Note: Item tints for engines are defined in assets/logistics/items/power/*.json
        // using the minecraft:constant tint source (1.21.4+ data-driven approach)
    }
}
