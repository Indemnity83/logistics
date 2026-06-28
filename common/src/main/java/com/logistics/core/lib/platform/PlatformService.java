package com.logistics.core.lib.platform;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

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
     * Returns how many platform-native fluid units make up one millibucket (mB).
     *
     * <p>Fluid amounts in {@link com.logistics.core.lib.fluids.IFluidStorage} are expressed in
     * platform-native units, which differ per loader: Fabric uses droplets (81 per mB), NeoForge
     * uses millibuckets directly (1 per mB). Author-facing config values (e.g. "250 mB") must be
     * multiplied by this factor before being handed to the native fluid APIs.
     *
     * @return native fluid units per millibucket; {@code 81} on Fabric, {@code 1} on NeoForge
     */
    long fluidUnitsPerMillibucket();

    /**
     * Whether the given fluid is lighter than air (a gas), used by tank columns to settle gases upward.
     *
     * <p>Loader-specific: Fabric reads {@code FluidVariantAttributes}, NeoForge reads the fluid type.
     */
    boolean isLighterThanAir(net.minecraft.world.level.material.Fluid fluid);

    /**
     * Returns the human-readable display name for the mod with the given namespace/mod-id.
     * Defaults to the namespace itself if the loader does not know the mod.
     */
    default String getModName(String namespace) {
        return namespace;
    }

    /**
     * Returns this mod's version string (e.g. {@code "0.7.0+mc26.2.fabric"}). Used as the Sentry
     * release identifier. Defaults to {@code "unknown"} for any loader that doesn't override it.
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
     * Returns the active Minecraft version string (e.g. {@code "26.2"}). Used for diagnostics
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
    default <T> void registerAlias(Registry<T> registry, ResourceLocation oldId, T currentEntry) {}

    PlatformService INSTANCE = ServiceLoader.load(PlatformService.class, PlatformService.class.getClassLoader())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No PlatformService found"));
}
