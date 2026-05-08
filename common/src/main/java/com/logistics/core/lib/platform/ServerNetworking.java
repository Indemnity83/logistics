package com.logistics.core.lib.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Loader-agnostic server-to-client packet sending (Pattern A — SPI).
 *
 * <p>Common code calls {@link #send} to dispatch a packet to a player without depending
 * on any loader-specific networking API. The loader registers its implementation once
 * during initialization:
 * <ul>
 *   <li>Fabric: delegates to {@code ServerPlayNetworking.send(player, packet)}
 *   <li>NeoForge: delegates to the equivalent NeoForge API
 * </ul>
 */
public final class ServerNetworking {

    @FunctionalInterface
    public interface Sender {
        void send(ServerPlayer player, CustomPacketPayload packet);
    }

    private static volatile Sender sender;

    private ServerNetworking() {}

    /**
     * Register the platform-specific sender. Called once during loader initialization.
     *
     * @param s the sender implementation
     */
    public static void register(Sender s) {
        sender = s;
    }

    /**
     * Send a packet to the given player.
     *
     * @param player the target player
     * @param packet the packet to send
     */
    public static void send(ServerPlayer player, CustomPacketPayload packet) {
        if (sender != null) sender.send(player, packet);
    }
}
