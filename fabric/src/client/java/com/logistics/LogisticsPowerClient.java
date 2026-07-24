package com.logistics;

import com.logistics.core.bootstrap.ClientDomainBootstrap;
import com.logistics.core.lib.power.EngineEntity;
import com.logistics.power.render.CableBlockEntityRenderer;
import com.logistics.power.render.EngineBlockEntityRenderer;
import com.logistics.power.render.EngineHeatTintSource;
import com.logistics.power.screen.FuelEngineScreen;
import com.logistics.power.screen.StirlingEngineScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

import java.util.List;

import static com.logistics.LogisticsMod.LOGGER;

public final class LogisticsPowerClient implements ClientDomainBootstrap {

    @Override
    public void initClient() {
        LOGGER.info("Registering power (client)");

        // Register engine block entity renderers
        BlockEntityRenderers.register(LogisticsCore.ENTITY.REDSTONE_ENGINE_BLOCK_ENTITY, EngineBlockEntityRenderer::new);
        BlockEntityRenderers.register(LogisticsPower.ENTITY.STIRLING_ENGINE_BLOCK_ENTITY, EngineBlockEntityRenderer::new);
        BlockEntityRenderers.register(LogisticsPower.ENTITY.FUEL_ENGINE_BLOCK_ENTITY, EngineBlockEntityRenderer::new);
        BlockEntityRenderers.register(LogisticsPower.ENTITY.CREATIVE_ENGINE_BLOCK_ENTITY, EngineBlockEntityRenderer::new);

        // Cables render fully in code via the shared vanilla CableBlockEntityRenderer
        // (one BE type covers all tiers; tier is read from the block at render time).
        BlockEntityRenderers.register(LogisticsPower.ENTITY.CABLE_BLOCK_ENTITY, CableBlockEntityRenderer::new);

        // Register screens
        MenuScreens.register(LogisticsPower.SCREEN.STIRLING_ENGINE, StirlingEngineScreen::new);
        MenuScreens.register(LogisticsPower.SCREEN.FUEL_ENGINE, FuelEngineScreen::new);

        // Register block color providers for engine heat stage tinting
        registerEngineBlockColors();

        // Register cleanup callback for engine animation cache
        EngineEntity.setOnRemovedCallback(EngineBlockEntityRenderer::clearAnimationCache);

        // Clear all animation caches when disconnecting from server
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> EngineBlockEntityRenderer.clearAllAnimationCache());
    }

    /**
     * Registers the block tint source that colors engines by heat stage. The source declares STAGE
     * as a relevant property so 26.1 re-bakes the tint when the stage changes (see
     * {@link EngineHeatTintSource}).
     *
     * <p>TODO: The non-overheating flash effect (HOT/WARM oscillation) is driven by block state
     * changes on the server tick, which causes chunk rebuilds at each half-stroke transition.
     * The better long-term solution is to render the engine core dynamically in the block entity
     * renderer, where per-frame animation progress is available for smooth, rebuild-free coloring.
     */
    private void registerEngineBlockColors() {
        BlockColorRegistry.register(
            List.of(EngineHeatTintSource.INSTANCE),
            LogisticsCore.BLOCK.REDSTONE_ENGINE,
            LogisticsPower.BLOCK.STIRLING_ENGINE,
            LogisticsPower.BLOCK.FUEL_ENGINE,
            LogisticsPower.BLOCK.CREATIVE_ENGINE);
    }
}
