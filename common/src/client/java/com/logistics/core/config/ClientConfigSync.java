package com.logistics.core.config;

import com.logistics.power.engine.magmatic.jei.MagmaticJeiSyncAdapter;
import net.minecraft.client.Minecraft;

/**
 * Client side of the server config sync: adopts the server's values on join and drops them on disconnect.
 *
 * <p>Both loaders route their join packet and their disconnect event here so the single-player gate and the
 * JEI refresh live in one place rather than being duplicated per loader.
 */
public final class ClientConfigSync {

    private ClientConfigSync() {}

    /**
     * Adopt the server's config, unless this client <em>is</em> the server.
     *
     * <p>In single-player the integrated server reads the very same files, so the local values are already
     * authoritative. Skipping the install there also keeps the 140-odd server-side {@code get} call sites in
     * that shared JVM reading the live config rather than a join-time snapshot, which would go stale the
     * moment an operator changed a value with the config command.
     */
    public static void accept(ConfigSyncPacket packet) {
        if (Minecraft.getInstance().hasSingleplayerServer()) {
            return;
        }
        RemoteConfig.install(packet);
        MagmaticJeiSyncAdapter.INSTANCE.rebuild();
    }

    /** Fall back to the local config, and rebuild the JEI figures that were showing the server's numbers. */
    public static void clear() {
        boolean wasActive = RemoteConfig.isActive();
        RemoteConfig.clear();
        if (wasActive) {
            MagmaticJeiSyncAdapter.INSTANCE.rebuild();
        }
    }
}
