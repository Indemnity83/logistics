package com.logistics.fabric;

import com.logistics.core.lib.client.model.ClientModelRegistry;
import com.logistics.core.lib.resource.ResourceId;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockStateModel;

import java.util.ArrayList;
import java.util.IdentityHashMap;
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

    private static final Map<ClientModelRegistry.ModelKey, ExtraModelKey<BlockStateModel>> FABRIC_KEYS =
            new IdentityHashMap<>();

    public static void setup() {
        if (initialized) return;

        // Take a single consistent snapshot so FABRIC_KEYS and the plugin lambda
        // iterate the exact same entries — no risk of null lookups from interleaved writes.
        Map<ResourceId, ClientModelRegistry.ModelKey> snapshot = ClientModelRegistry.all();

        List<Map.Entry<ExtraModelKey<BlockStateModel>, ResourceId>> pluginEntries = new ArrayList<>();
        for (var entry : snapshot.entrySet()) {
            ExtraModelKey<BlockStateModel> fabricKey =
                    ExtraModelKey.create(entry.getKey().toIdentifier()::toString);
            FABRIC_KEYS.put(entry.getValue(), fabricKey);
            pluginEntries.add(Map.entry(fabricKey, entry.getKey()));
        }

        // Register model loading plugin — runs before Minecraft bakes models
        ModelLoadingPlugin.register(ctx -> {
            for (var entry : pluginEntries) {
                ctx.addModel(
                        entry.getKey(),
                        SimpleUnbakedExtraModel.blockStateModel(entry.getValue().toIdentifier()));
            }
        });

        // Register the render-time provider
        ClientModelRegistry.registerProvider(key -> {
            ExtraModelKey<BlockStateModel> fk = FABRIC_KEYS.get(key);
            if (fk == null) return null;
            return ((FabricBakedModelManager) Minecraft.getInstance().getModelManager()).getModel(fk);
        });

        initialized = true;
    }

    private FabricModelLoader() {}
}
