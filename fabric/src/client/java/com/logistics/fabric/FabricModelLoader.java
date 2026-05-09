package com.logistics.fabric;

import com.logistics.core.lib.client.model.ClientModelRegistry;
import com.logistics.core.lib.resource.ResourceId;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Wires {@link ClientModelRegistry} to Fabric's model loading API.
 *
 * <p>Call {@link #setup()} once from {@code LogisticsModClient.onInitializeClient()},
 * <em>after</em> {@code CLIENT_BOOTSTRAP.initialize()} so that all domain-client MODEL
 * statics have already called {@link ClientModelRegistry#register} for their models.
 */
public final class FabricModelLoader {

    private static boolean initialized = false;

    public static void setup() {
        if (initialized) return;

        // Take a single consistent snapshot so the plugin lambda iterates
        // the exact same entries — no risk of null lookups from interleaved writes.
        Map<ResourceId, ClientModelRegistry.ModelKey> snapshot = ClientModelRegistry.all();

        List<ResourceId> resourceIds = new ArrayList<>(snapshot.keySet());

        // Register model loading plugin — runs before Minecraft bakes models
        ModelLoadingPlugin.register(ctx -> {
            for (ResourceId id : resourceIds) {
                ctx.addModels(id.toIdentifier());
            }
        });

        // Register the render-time provider using FabricBakedModelManager
        ClientModelRegistry.registerProvider(key -> {
            ResourceId id = null;
            for (var entry : snapshot.entrySet()) {
                if (entry.getValue() == key) {
                    id = entry.getKey();
                    break;
                }
            }
            if (id == null) return null;
            ResourceLocation loc = id.toIdentifier();
            return ((FabricBakedModelManager) Minecraft.getInstance().getModelManager()).getModel(loc);
        });

        initialized = true;
    }

    private FabricModelLoader() {}
}
