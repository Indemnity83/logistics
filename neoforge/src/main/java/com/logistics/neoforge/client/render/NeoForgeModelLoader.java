package com.logistics.neoforge.client.render;

import com.logistics.LogisticsAutomationClientModels;
import com.logistics.LogisticsPipeClientModels;
import com.logistics.LogisticsPower;
import com.logistics.core.lib.client.model.ClientModelRegistry;
import com.logistics.core.lib.resource.ResourceId;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * NeoForge 21.1 model loader for power engines and other custom models.
 *
 * <p>Registers extra models via {@link ModelEvent.RegisterAdditional} so that
 * NeoForge loads them as part of the baking phase.  After baking, a
 * {@link ClientModelRegistry.Provider} is wired up that retrieves models from
 * the baked model map.
 */
public final class NeoForgeModelLoader {
    private static final Map<ClientModelRegistry.ModelKey, ModelResourceLocation> MODEL_LOCATIONS =
            new IdentityHashMap<>();

    private NeoForgeModelLoader() {}

    /**
     * Called once during client setup (before model loading) to declare which
     * models to load and to register the {@link ClientModelRegistry} provider.
     */
    public static void registerPowerModels() {
        ClientModelRegistry.register(LogisticsPower.model("redstone_engine_bellow"));
        ClientModelRegistry.register(LogisticsPower.model("redstone_engine_piston"));
        ClientModelRegistry.register(LogisticsPower.model("stirling_engine_bellow"));
        ClientModelRegistry.register(LogisticsPower.model("stirling_engine_piston"));
        ClientModelRegistry.register(LogisticsPower.model("creative_engine_bellow"));
        ClientModelRegistry.register(LogisticsPower.model("creative_engine_piston"));

        LogisticsPipeClientModels.init();
        LogisticsAutomationClientModels.init();

        for (var entry : ClientModelRegistry.all().entrySet()) {
            register(entry.getValue(), entry.getKey());
        }

        ClientModelRegistry.registerProvider(key -> {
            ModelResourceLocation location = MODEL_LOCATIONS.get(key);
            if (location == null) {
                return null;
            }
            return Minecraft.getInstance().getModelManager().getModel(location);
        });
    }

    /**
     * Subscribes to {@link ModelEvent.RegisterAdditional} to add the declared
     * models to the loading set.
     */
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        for (ModelResourceLocation location : MODEL_LOCATIONS.values()) {
            event.register(location);
        }
    }

    /**
     * Subscribes to {@link ModelEvent.RegisterGeometryLoaders} to register
     * the cable model geometry loader.
     */
    public static void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(
                ResourceLocation.fromNamespaceAndPath("logistics", "cable_model"),
                NeoForgeCableBlockModelDefinition.INSTANCE);
    }

    private static void register(ClientModelRegistry.ModelKey key, ResourceId modelPath) {
        MODEL_LOCATIONS.computeIfAbsent(key,
                ignored -> ModelResourceLocation.standalone(modelPath.toIdentifier()));
    }
}
