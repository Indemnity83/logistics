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
 *
 * <p>{@link #canSend} reports whether a player's connection has negotiated a payload type.
 * {@link #send} stays strict — sending an un-negotiated payload throws on NeoForge — so guard
 * with {@code canSend} only where a client legitimately may not have the channel yet, such as
 * login handlers and synthetic players.
 */
public final class ServerNetworking {

    @FunctionalInterface
    public interface Sender {
        void send(ServerPlayer player, CustomPacketPayload packet);
    }

    /** Reports whether a player's connection has negotiated a given payload type. */
    @FunctionalInterface
    public interface ChannelCheck {
        boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type);
    }

    private static volatile Sender sender;
    private static volatile ChannelCheck channelCheck;

    private ServerNetworking() {}

    /**
     * Register the platform-specific implementations. Called once during loader initialization.
     *
     * @param s the sender implementation
     * @param c the channel-check implementation
     */
    public static void register(Sender s, ChannelCheck c) {
        if (s == null) throw new NullPointerException("sender must not be null");
        if (c == null) throw new NullPointerException("channelCheck must not be null");
        sender = s;
        channelCheck = c;
    }

    /**
     * Send a packet to the given player.
     *
     * @param player the target player
     * @param packet the packet to send
     * @throws IllegalStateException if no sender has been registered
     */
    public static void send(ServerPlayer player, CustomPacketPayload packet) {
        if (sender == null) throw new IllegalStateException("ServerNetworking sender not registered");
        sender.send(player, packet);
    }

    /**
     * Whether the player's connection has negotiated the given payload type.
     *
     * @param player the target player
     * @param type the payload type to check
     * @return {@code true} if the payload can be sent to this player
     * @throws IllegalStateException if no channel check has been registered
     */
    public static boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        if (channelCheck == null) throw new IllegalStateException("ServerNetworking channelCheck not registered");
        return channelCheck.canSend(player, type);
    }
}
