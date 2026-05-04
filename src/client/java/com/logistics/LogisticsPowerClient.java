package com.logistics;

import com.logistics.core.bootstrap.ClientDomainBootstrap;
import com.logistics.core.lib.power.AbstractEngineBlockEntity;
import com.logistics.core.lib.resource.ResourceId;
import com.logistics.core.render.ModelKeyRegistry;
import com.logistics.power.render.EngineBlockEntityRenderer;
import com.logistics.power.screen.StirlingEngineScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

import java.util.List;

import java.util.Map;

import static com.logistics.LogisticsMod.LOGGER;

public final class LogisticsPowerClient implements ClientDomainBootstrap {
    public LogisticsPowerClient() {
        ModelLoadingPlugin.register(pluginContext -> {
            for (var entry : MODEL.getAllModels()) {
                pluginContext.addModel(entry.getKey(), SimpleUnbakedExtraModel.blockStateModel(entry.getValue().toIdentifier()));
            }
        });
    }

    @Override
    public void initClient() {
        LOGGER.info("Registering power (client)");

        // Register engine block entity renderers (static rendering for now)
        BlockEntityRenderers.register(LogisticsPower.ENTITY.REDSTONE_ENGINE_BLOCK_ENTITY, EngineBlockEntityRenderer::new);
        BlockEntityRenderers.register(LogisticsPower.ENTITY.STIRLING_ENGINE_BLOCK_ENTITY, EngineBlockEntityRenderer::new);
        BlockEntityRenderers.register(LogisticsPower.ENTITY.CREATIVE_ENGINE_BLOCK_ENTITY, EngineBlockEntityRenderer::new);

        // Register screens
        MenuScreens.register(LogisticsPower.SCREEN.STIRLING_ENGINE, StirlingEngineScreen::new);

        // Register block color providers for engine heat stage tinting
        registerEngineBlockColors();

        // Register cleanup callback for engine animation cache
        AbstractEngineBlockEntity.setOnRemovedCallback(EngineBlockEntityRenderer::clearAnimationCache);

        // Clear all animation caches when disconnecting from server
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> EngineBlockEntityRenderer.clearAllAnimationCache());
    }

    public static final class MODEL {
        private static final ModelKeyRegistry REGISTRY = new ModelKeyRegistry(LogisticsPower::model);

        public static final ExtraModelKey<BlockStateModel> REDSTONE_BELLOW = REGISTRY.registerModel("redstone_engine_bellow");
        public static final ExtraModelKey<BlockStateModel> REDSTONE_PISTON = REGISTRY.registerModel("redstone_engine_piston");
        public static final ExtraModelKey<BlockStateModel> STIRLING_BELLOW = REGISTRY.registerModel("stirling_engine_bellow");
        public static final ExtraModelKey<BlockStateModel> STIRLING_PISTON = REGISTRY.registerModel("stirling_engine_piston");
        public static final ExtraModelKey<BlockStateModel> CREATIVE_BELLOW = REGISTRY.registerModel("creative_engine_bellow");
        public static final ExtraModelKey<BlockStateModel> CREATIVE_PISTON = REGISTRY.registerModel("creative_engine_piston");

        static Iterable<Map.Entry<ExtraModelKey<BlockStateModel>, ResourceId>> getAllModels() {
            return REGISTRY.getAllModels();
        }

        private MODEL() {}
    }

    /**
     * Registers block color providers for engines to tint based on heat stage.
     * Uses the STAGE block state property to determine color.
     *
     * <p>TODO: The non-overheating flash effect (HOT/WARM oscillation) is driven by block state
     * changes on the server tick, which causes chunk rebuilds at each half-stroke transition.
     * The better long-term solution is to render the engine core dynamically in the block entity
     * renderer, where per-frame animation progress is available for smooth, rebuild-free coloring.
     */
    private void registerEngineBlockColors() {
        BlockColorRegistry.register(
            List.of(state -> switch (state.getValue(AbstractEngineBlockEntity.STAGE)) {
                case COLD -> 0xFF3366CC;
                case COOL -> 0xFF33CC33;
                case WARM -> 0xFFCCCC33;
                case HOT -> 0xFFCC3333;
                case OVERHEAT -> 0xFF191919;
            }),
            LogisticsPower.BLOCK.REDSTONE_ENGINE,
            LogisticsPower.BLOCK.STIRLING_ENGINE,
            LogisticsPower.BLOCK.CREATIVE_ENGINE);
    }
}
