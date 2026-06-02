package com.logistics.core.lib.platform;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

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
     * Returns the Logistics mod version string (e.g. {@code "0.5.5"}), used as the Sentry
     * release tag. Defaults to {@code "unknown"} when the loader cannot resolve it.
     */
    default String modVersion() {
        return "unknown";
    }

    /**
     * Returns the loader id used by this runtime (e.g. {@code "fabric"} or {@code "neoforge"}).
     * Used for diagnostics tags such as Sentry grouping/search.
     */
    default String loaderName() {
        return "unknown";
    }

    /**
     * Returns the active Minecraft version string (e.g. {@code "1.21.11"}). Used for diagnostics
     * tags so cross-version reports can be filtered without parsing the mod release string.
     */
    default String minecraftVersion() {
        return "unknown";
    }

    /**
     * Returns {@code true} in a development/dev-runtime environment, {@code false} in a
     * production install. Used to pick the Sentry environment tag and enable SDK debug logging.
     * Defaults to {@code false} (production-safe) for any loader that doesn't override it.
     */
    default boolean isDevelopmentEnvironment() {
        return false;
    }

    /**
     * Registers {@code oldId} as a legacy alias for the same registry entry as
     * {@code currentEntry}, so saves that used the old ID are remapped on load.
     *
     * <p>No-op on loaders that don't support registry aliases (e.g. NeoForge stub).
     * Fabric delegates to {@code Registry.addAlias()} via Fabric API.
     */
    default <T> void registerAlias(Registry<T> registry, Identifier oldId, T currentEntry) {}

    PlatformService INSTANCE = ServiceLoader.load(PlatformService.class, PlatformService.class.getClassLoader())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No PlatformService found"));
}
