package com.logistics.platform.services;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

/**
 * Platform-agnostic service for registering lifecycle event handlers.
 * Abstracts Fabric's ServerLifecycleEvents and NeoForge's event bus.
 */
public interface IEventHelper {

    /**
     * Register a callback for when the server is starting.
     *
     * @param callback the callback to run
     */
    void onServerStarting(Consumer<MinecraftServer> callback);

    /**
     * Register a callback for when the server has started.
     *
     * @param callback the callback to run
     */
    void onServerStarted(Consumer<MinecraftServer> callback);

    /**
     * Register a callback for when a world is loaded.
     *
     * @param callback the callback to run (receives server and world)
     */
    void onWorldLoad(BiConsumer<MinecraftServer, ServerWorld> callback);
}
