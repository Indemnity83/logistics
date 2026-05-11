package com.logistics.neoforge.client.render;

import com.logistics.LogisticsPower;
import com.logistics.core.lib.client.model.ClientModelRegistry;
import com.logistics.core.lib.resource.ResourceId;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public final class NeoForgeModelLoader {
    private static final Map<ClientModelRegistry.ModelKey, StandaloneModelKey<BlockStateModel>> STANDALONE_KEYS =
            new IdentityHashMap<>();

    private NeoForgeModelLoader() {}

    public static void registerPowerModels() {
        register(LogisticsPower.model("redstone_engine_bellow"));
        register(LogisticsPower.model("redstone_engine_piston"));
        register(LogisticsPower.model("stirling_engine_bellow"));
        register(LogisticsPower.model("stirling_engine_piston"));
        register(LogisticsPower.model("creative_engine_bellow"));
        register(LogisticsPower.model("creative_engine_piston"));

        ClientModelRegistry.registerProvider(key -> {
            StandaloneModelKey<BlockStateModel> standaloneKey = STANDALONE_KEYS.get(key);
            if (standaloneKey == null) {
                return null;
            }
            return Minecraft.getInstance().getModelManager().getStandaloneModel(standaloneKey);
        });
    }

    public static void registerStandaloneModels(ModelEvent.RegisterStandalone event) {
        for (var entry : ClientModelRegistry.all().entrySet()) {
            StandaloneModelKey<BlockStateModel> standaloneKey = STANDALONE_KEYS.get(entry.getValue());
            if (standaloneKey == null) {
                continue;
            }
            event.register(
                    standaloneKey,
                    SimpleUnbakedStandaloneModel.blockStateModel(entry.getKey().toIdentifier()));
        }
    }

    private static void register(ResourceId modelPath) {
        ClientModelRegistry.ModelKey key = ClientModelRegistry.register(modelPath);
        STANDALONE_KEYS.computeIfAbsent(key,
                ignored -> new StandaloneModelKey<>(() -> modelPath.toIdentifier().toString()));
    }
}
