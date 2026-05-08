package com.logistics.fabric;

import com.logistics.core.lib.client.model.ClientModelRegistry;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricModelManager;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Wires {@link ClientModelRegistry} to Fabric's model loading API.
 *
 * <p>Call {@link #setup()} once from {@code LogisticsModClient.onInitializeClient()},
 * <em>after</em> {@code CLIENT_BOOTSTRAP.initialize()} so that all domain-client MODEL
 * statics have already called {@link ClientModelRegistry#register} for their models.
 */
public final class FabricModelLoader {

    private static final Map<ClientModelRegistry.ModelKey, ExtraModelKey<BlockStateModel>> FABRIC_KEYS =
            new IdentityHashMap<>();

    public static void setup() {
        // Create a Fabric ExtraModelKey for every model declared in ClientModelRegistry
        for (var entry : ClientModelRegistry.all().entrySet()) {
            ExtraModelKey<BlockStateModel> fabricKey =
                    ExtraModelKey.create(entry.getKey().toIdentifier()::toString);
            FABRIC_KEYS.put(entry.getValue(), fabricKey);
        }

        // Register model loading plugin — runs before Minecraft bakes models
        ModelLoadingPlugin.register(ctx -> {
            for (var entry : ClientModelRegistry.all().entrySet()) {
                ctx.addModel(
                        FABRIC_KEYS.get(entry.getValue()),
                        SimpleUnbakedExtraModel.blockStateModel(entry.getKey().toIdentifier()));
            }
        });

        // Register the render-time provider
        ClientModelRegistry.registerProvider(key -> {
            ExtraModelKey<BlockStateModel> fk = FABRIC_KEYS.get(key);
            if (fk == null) return null;
            return ((FabricModelManager) Minecraft.getInstance().getModelManager()).getModel(fk);
        });
    }

    private FabricModelLoader() {}
}
