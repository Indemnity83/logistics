package com.logistics.core.config;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.Minecraft;

/**
 * Client side of the server config sync: adopts the server's values on join and drops them on disconnect.
 *
 * <p>Both loaders route their join packet and their disconnect event here so the single-player gate lives in
 * one place rather than being duplicated per loader.
 *
 * <p>Anything that caches a derived config value registers through {@link #onValuesChanged} instead of being
 * called from here, so this class does not have to know which domains those are.
 */
public final class ClientConfigSync {

    private static final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private ClientConfigSync() {}

    /**
     * Register a callback fired whenever the values {@link com.logistics.LogisticsConfigHost} resolves to
     * change -- a server's arriving, or its being dropped on disconnect. Call once during client setup.
     */
    public static void onValuesChanged(Runnable listener) {
        listeners.add(listener);
    }

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
        notifyListeners();
    }

    /** Fall back to the local config, and let cached values rebuild from it. */
    public static void clear() {
        boolean wasActive = RemoteConfig.isActive();
        RemoteConfig.clear();
        if (wasActive) {
            notifyListeners();
        }
    }

    private static void notifyListeners() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }
}
