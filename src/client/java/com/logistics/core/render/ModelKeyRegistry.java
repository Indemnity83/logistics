package com.logistics.core.render;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utility for registering block state models with the Fabric Model Loading API.
 * Maintains a registry of model identifiers for use with ModelLoadingPlugin.
 *
 * <p>Note: In MC 1.21.1, the simpler ResourceLocation-based API is used.
 * Newer versions (1.21.11+) use ExtraModelKey for type-safe model references.
 */
public final class ModelKeyRegistry {
    private final Map<ResourceLocation, ResourceLocation> models = new HashMap<>();
    private final Function<String, ResourceLocation> identifierFactory;

    /**
     * Creates a new model registry.
     * @param identifierFactory Function to create identifiers from model names (e.g., LogisticsDomain::blockModelIdentifier)
     */
    public ModelKeyRegistry(Function<String, ResourceLocation> identifierFactory) {
        this.identifierFactory = identifierFactory;
    }

    /**
     * Registers a model and returns its identifier.
     * @param name Model name (will be passed to the identifier factory)
     * @return ResourceLocation for the registered model
     * @throws IllegalArgumentException if the model is already registered (indicates a programming error)
     */
    public ResourceLocation registerModel(String name) {
        ResourceLocation id = identifierFactory.apply(name);

        // Detect duplicate registration (programming error)
        if (models.containsValue(id)) {
            throw new IllegalArgumentException("Model already registered: " + id);
        }

        models.put(id, id);
        return id;
    }

    /**
     * Returns all registered models as identifiers.
     * Used by ModelLoadingPlugin to register models with Fabric.
     * @return Array of registered model identifiers
     */
    public ResourceLocation[] getAllModels() {
        return models.values().toArray(new ResourceLocation[0]);
    }
}