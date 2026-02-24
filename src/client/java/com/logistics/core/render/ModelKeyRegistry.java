package com.logistics.core.render;

import com.logistics.core.lib.resource.ResourceId;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.minecraft.client.renderer.block.model.BlockStateModel;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utility for registering block state models with the Fabric Extra Model API.
 * Maintains a registry of model keys and their corresponding identifiers.
 */
public final class ModelKeyRegistry {
    private final Map<ExtraModelKey<BlockStateModel>, ResourceId> models = new HashMap<>();
    private final Function<String, ResourceId> identifierFactory;

    /**
     * Creates a new model registry.
     * @param identifierFactory Function to create resource helpers from model names (e.g., LogisticsDomain::blockModelIdentifier)
     */
    public ModelKeyRegistry(Function<String, ResourceId> identifierFactory) {
        this.identifierFactory = identifierFactory;
    }

    /**
     * Registers a model and returns its key.
     * @param name Model name (will be passed to the identifier factory)
     * @return ExtraModelKey for the registered model
     * @throws IllegalArgumentException if the model is already registered (indicates a programming error)
     */
    public ExtraModelKey<BlockStateModel> registerModel(String name) {
        ResourceId id = identifierFactory.apply(name);

        // Detect duplicate registration (programming error)
        if (models.containsValue(id)) {
            throw new IllegalArgumentException("Model already registered: " + id);
        }

        ExtraModelKey<BlockStateModel> key = ExtraModelKey.create(id::toString);
        models.put(key, id);
        return key;
    }

    /**
     * Returns all registered models as key-ResourceHelper pairs.
     * Used by ModelLoadingPlugin to register models with Fabric.
     * Callers should unwrap ResourceHelper when passing to Fabric APIs.
     * @return Immutable view of registered models
     */
    public Iterable<Map.Entry<ExtraModelKey<BlockStateModel>, ResourceId>> getAllModels() {
        return models.entrySet().stream()
            .map(entry -> Map.entry(entry.getKey(), entry.getValue()))
            .toList();
    }
}