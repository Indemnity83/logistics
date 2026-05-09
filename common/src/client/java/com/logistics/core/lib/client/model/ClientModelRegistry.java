package com.logistics.core.lib.client.model;

import com.logistics.core.lib.resource.ResourceId;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loader-agnostic registry for extra block-state models (Pattern A — SPI).
 *
 * <p>Domain clients declare models at class-load time by calling {@link #register}.
 * The loader (Fabric, NeoForge, …) iterates {@link #all()} during its model-loading
 * phase, bakes each model, and registers a {@link Provider} so renderers can call
 * {@link #get} at render time without knowing which loader is active.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Domain client MODEL inner class — class-load time:
 * public static final ModelKey BELLOW =
 *         ClientModelRegistry.register(LogisticsPower.model("engine_bellow"));
 *
 * // Renderer — render time:
 * BlockStateModel model = ClientModelRegistry.get(LogisticsPowerClient.MODEL.BELLOW);
 *
 * // Fabric loader wiring (once, in LogisticsModClient):
 * FabricModelLoader.setup();   // calls registerProvider()
 * }</pre>
 *
 * <h2>NeoForge sketch</h2>
 * <pre>{@code
 * // NeoForgeModelLoader.setup():
 * //   @SubscribeEvent
 * //   void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
 * //       for (ResourceId path : ClientModelRegistry.all().keySet())
 * //           event.register(path.toResourceLocation());
 * //   }
 * ClientModelRegistry.registerProvider(key ->
 *     (BlockStateModel) Minecraft.getInstance()
 *         .getModelManager().getModel(key.path().toResourceLocation()));
 * }</pre>
 */
public final class ClientModelRegistry {

    /**
     * Opaque handle returned by {@link #register} and used at render time with {@link #get}.
     * The {@link #path()} accessor gives loaders the resource path for retrieval.
     */
    public static final class ModelKey {
        private final ResourceId path;

        ModelKey(ResourceId path) {
            this.path = path;
        }

        /** The resource path this key was registered with. */
        public ResourceId path() {
            return path;
        }
    }

    /** Loader callback that resolves a {@link ModelKey} to a baked {@link BlockStateModel}. */
    @FunctionalInterface
    public interface Provider {
        @Nullable
        BlockStateModel get(ModelKey key);
    }

    private static final Map<ResourceId, ModelKey> REGISTRY = new LinkedHashMap<>();
    private static volatile Provider provider;

    private ClientModelRegistry() {}

    /**
     * Declare an extra model. Called at class-load time from domain-client MODEL statics.
     * Registering the same path twice returns the same key (idempotent).
     *
     * @param modelPath resource path of the block-state model file
     * @return stable key for use at render time
     */
    public static synchronized ModelKey register(ResourceId modelPath) {
        return REGISTRY.computeIfAbsent(modelPath, ModelKey::new);
    }

    /**
     * Look up a key by its registered path.
     * Useful for renderers that compute model paths at render time rather than holding a named field.
     *
     * @param modelPath the path passed to {@link #register}
     * @return the key, or {@code null} if not registered
     */
    @Nullable
    public static synchronized ModelKey find(ResourceId modelPath) {
        return REGISTRY.get(modelPath);
    }

    /**
     * All registered models. Iterated by the loader during its model-loading setup phase
     * to register each model with its native API.
     *
     * @return unmodifiable snapshot; order matches registration order at time of call
     */
    public static synchronized Map<ResourceId, ModelKey> all() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(REGISTRY));
    }

    /**
     * Retrieve the baked model for {@code key} at render time.
     * Delegates to the loader's registered {@link Provider}.
     *
     * @param key the key returned by {@link #register}; {@code null} returns {@code null}
     * @return the baked model, or {@code null} if not yet baked
     * @throws IllegalStateException if no provider has been registered
     */
    @Nullable
    public static BlockStateModel get(ModelKey key) {
        if (key == null) return null;
        Provider p = provider;
        if (p == null) throw new IllegalStateException("ClientModelRegistry provider not registered");
        return p.get(key);
    }

    /**
     * Register the loader-specific provider. Called once during client initialization,
     * after all domain-client MODEL statics have run.
     *
     * @param p the provider
     * @throws NullPointerException  if {@code p} is null
     * @throws IllegalStateException if a provider is already registered
     */
    public static void registerProvider(Provider p) {
        if (p == null) throw new NullPointerException("provider must not be null");
        if (provider != null) throw new IllegalStateException("ClientModelRegistry provider already registered");
        provider = p;
    }
}
