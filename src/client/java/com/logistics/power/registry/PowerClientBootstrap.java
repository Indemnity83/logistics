package com.logistics.power.registry;

import com.logistics.core.bootstrap.ClientDomainBootstrap;
import com.logistics.core.lib.power.AbstractEngineBlockEntity;
import com.logistics.power.render.EngineBlockEntityRenderer;
import com.logistics.power.screen.StirlingEngineScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public final class PowerClientBootstrap implements ClientDomainBootstrap {
    @Override
    public void initCommon() {
        // Client-only bootstrap; common init handled in main bootstrap.
    }

    @Override
    public void initClient() {
        // Register engine block entity renderers
        BlockEntityRenderers.register(
                PowerBlockEntities.REDSTONE_ENGINE_BLOCK_ENTITY, EngineBlockEntityRenderer::new);
        BlockEntityRenderers.register(
                PowerBlockEntities.STIRLING_ENGINE_BLOCK_ENTITY, EngineBlockEntityRenderer::new);
        BlockEntityRenderers.register(
                PowerBlockEntities.CREATIVE_ENGINE_BLOCK_ENTITY, EngineBlockEntityRenderer::new);

        // Register screens
        MenuScreens.register(PowerScreenHandlers.STIRLING_ENGINE, StirlingEngineScreen::new);

        // Register cleanup callback for engine animation cache
        AbstractEngineBlockEntity.setOnRemovedCallback(EngineBlockEntityRenderer::clearAnimationCache);

        // Clear all animation caches when disconnecting from server
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> EngineBlockEntityRenderer.clearAllAnimationCache());

        // Note: Item tints for engines are defined in assets/logistics/items/power/*.json
        // using the minecraft:constant tint source (1.21.4+ data-driven approach)
    }
}
