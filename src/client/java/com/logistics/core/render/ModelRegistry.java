package com.logistics.core.render;

import com.logistics.LogisticsMod;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public final class ModelRegistry {
    private static final Map<Identifier, ExtraModelKey<BlockStateModel>> MODEL_KEYS = new HashMap<>();
    private static boolean registered;

    private ModelRegistry() {}

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        Set<Identifier> modelIds = collectModelIds();
        if (modelIds.isEmpty()) {
            LogisticsMod.LOGGER.warn("No models found to register for block entity rendering");
        }

        for (Identifier id : modelIds) {
            MODEL_KEYS.put(id, ExtraModelKey.create(id::toString));
        }

        ModelLoadingPlugin.register(context -> {
            for (Identifier id : modelIds) {
                ExtraModelKey<BlockStateModel> key = MODEL_KEYS.get(id);
                if (key != null) {
                    context.addModel(key, SimpleUnbakedExtraModel.blockStateModel(id));
                }
            }
        });
    }

    @Nullable public static BlockStateModel getModel(Identifier id) {
        ExtraModelKey<BlockStateModel> key = MODEL_KEYS.get(id);
        if (key == null) {
            return null;
        }

        BakedModelManager modelManager = MinecraftClient.getInstance().getBakedModelManager();
        if (!(modelManager instanceof FabricBakedModelManager fabricManager)) {
            return null;
        }

        return fabricManager.getModel(key);
    }

    private static Set<Identifier> collectModelIds() {
        Optional<ModContainer> container = FabricLoader.getInstance().getModContainer(LogisticsMod.MOD_ID);
        Optional<Path> root = container.flatMap(mod -> mod.findPath("assets/" + LogisticsMod.MOD_ID + "/models/block"));
        if (root.isEmpty()) {
            return Set.of();
        }

        Set<Identifier> modelIds = new HashSet<>();
        try (Stream<Path> files = Files.walk(root.get())) {
            files.filter(Files::isRegularFile).forEach(path -> {
                String relative = root.get().relativize(path).toString().replace('\\', '/');
                if (!relative.endsWith(".json")) {
                    return;
                }
                String name = relative.substring(0, relative.length() - ".json".length());
                modelIds.add(Identifier.of(LogisticsMod.MOD_ID, "block/" + name));
            });
        } catch (IOException e) {
            LogisticsMod.LOGGER.warn("Failed to scan model resources", e);
        }

        return modelIds;
    }
}
