package com.logistics.core.lib.platform;

import com.logistics.core.lib.resource.ResourceId;
import net.minecraft.core.Registry;

import java.nio.file.Path;
import java.util.ServiceLoader;

/**
 * Loader-agnostic access to platform utilities.
 *
 * <p>Common code calls {@code PlatformService.INSTANCE} to access platform-specific
 * paths and services. Each loader provides an implementation via {@code META-INF/services/}.
 *
 * <pre>{@code
 * // common — use the service
 * Path configFile = PlatformService.INSTANCE.configDir().resolve("logistics.json");
 *
 * // fabric implementation
 * public Path configDir() { return FabricLoader.getInstance().getConfigDir(); }
 * }</pre>
 */
public interface PlatformService {
    /** Returns the loader's standard config directory (e.g. {@code .minecraft/config/}). */
    Path configDir();

    /**
     * Returns the human-readable display name for the mod with the given namespace/mod-id.
     * Defaults to the namespace itself if the loader does not know the mod.
     */
    default String getModName(String namespace) {
        return namespace;
    }

    /**
     * Registers {@code oldId} as a legacy alias for the same registry entry as
     * {@code currentEntry}, so saves that used the old ID are remapped on load.
     *
     * <p>No-op on loaders that don't support registry aliases (e.g. NeoForge stub).
     * Fabric delegates to {@code Registry.addAlias()} via Fabric API.
     */
    default <T> void registerAlias(Registry<T> registry, ResourceId oldId, T currentEntry) {}

    PlatformService INSTANCE = ServiceLoader.load(PlatformService.class)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No PlatformService found"));
}
