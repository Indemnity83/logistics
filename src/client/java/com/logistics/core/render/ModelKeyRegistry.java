package com.logistics.core.render;

import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utility for registering block state models with the Fabric Extra Model API.
 * Maintains a registry of model keys and their corresponding identifiers.
 */
public final class ModelKeyRegistry {
    private final Map<ExtraModelKey<BlockStateModel>, Identifier> models = new HashMap<>();
    private final Function<String, Identifier> identifierFactory;

    /**
     * Creates a new model registry.
     * @param identifierFactory Function to create identifiers from model names (e.g., LogisticsDomain::blockModelIdentifier)
     */
    public ModelKeyRegistry(Function<String, Identifier> identifierFactory) {
        this.identifierFactory = identifierFactory;
    }

    /**
     * Registers a model and returns its key.
     * @param name Model name (will be passed to the identifier factory)
     * @return ExtraModelKey for the registered model
     */
    public ExtraModelKey<BlockStateModel> registerModel(String name) {
        Identifier id = identifierFactory.apply(name);
        ExtraModelKey<BlockStateModel> key = ExtraModelKey.create(id::toString);
        models.put(key, id);
        return key;
    }

    /**
     * Returns all registered models as key-identifier pairs.
     * Used by ModelLoadingPlugin to register models with Fabric.
     */
    public Iterable<Map.Entry<ExtraModelKey<BlockStateModel>, Identifier>> getAllModels() {
        return models.entrySet();
    }
}