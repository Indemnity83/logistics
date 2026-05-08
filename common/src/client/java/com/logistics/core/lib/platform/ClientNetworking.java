package com.logistics.core.lib.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Loader-agnostic client-to-server packet sending (Pattern A — SPI).
 *
 * <p>Common client code calls {@link #send} to dispatch a packet to the server without
 * depending on any loader-specific networking API. The loader registers its implementation
 * once during client initialization:
 * <ul>
 *   <li>Fabric: delegates to {@code ClientPlayNetworking.send(packet)}
 *   <li>NeoForge: delegates to the equivalent NeoForge API
 * </ul>
 */
public final class ClientNetworking {

    @FunctionalInterface
    public interface Sender {
        void send(CustomPacketPayload packet);
    }

    private static volatile Sender sender;

    private ClientNetworking() {}

    /**
     * Register the platform-specific sender. Called once during client loader initialization.
     *
     * @param s the sender implementation
     */
    public static void register(Sender s) {
        if (s == null) throw new NullPointerException("sender must not be null");
        sender = s;
    }

    /**
     * Send a packet to the server.
     *
     * @param packet the packet to send
     * @throws IllegalStateException if no sender has been registered
     */
    public static void send(CustomPacketPayload packet) {
        if (sender == null) throw new IllegalStateException("ClientNetworking sender not registered");
        sender.send(packet);
    }
}
