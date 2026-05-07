package com.logistics.core.lib.platform;

import com.logistics.core.lib.resource.ResourceId;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.util.ServiceLoader;

/**
 * Loader-agnostic registration of server-data reload listeners.
 *
 * <p>Common code calls {@code ResourceReloadRegistrar.INSTANCE.register(...)} to attach
 * a reload listener. Each loader provides an implementation via {@code META-INF/services/}.
 *
 * <pre>{@code
 * // common — register a listener
 * ResourceReloadRegistrar.INSTANCE.register(
 *     LogisticsMod.modId("macerator_recipes"),
 *     new MaceratorReloadListener()
 * );
 *
 * // fabric implementation
 * public void register(ResourceId id, PreparableReloadListener listener) {
 *     ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(id.toIdentifier(), listener);
 * }
 * }</pre>
 */
public interface ResourceReloadRegistrar {
    /** Registers a server-data reload listener with the given identifier. */
    void register(ResourceId id, PreparableReloadListener listener);

    ResourceReloadRegistrar INSTANCE = ServiceLoader.load(ResourceReloadRegistrar.class)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No ResourceReloadRegistrar found"));
}
