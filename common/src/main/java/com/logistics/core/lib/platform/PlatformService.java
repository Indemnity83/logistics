package com.logistics.core.lib.platform;

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

    PlatformService INSTANCE = ServiceLoader.load(PlatformService.class)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No PlatformService found"));
}
