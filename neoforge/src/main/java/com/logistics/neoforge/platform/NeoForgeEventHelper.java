package com.logistics.neoforge.platform;

import com.logistics.platform.services.IEventHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

/**
 * NeoForge implementation of IEventHelper using the NeoForge event bus.
 */
public class NeoForgeEventHelper implements IEventHelper {
    private static final List<Consumer<net.minecraft.server.MinecraftServer>> serverStartingCallbacks =
            new ArrayList<>();
    private static final List<Consumer<net.minecraft.server.MinecraftServer>> serverStartedCallbacks =
            new ArrayList<>();
    private static final List<BiConsumer<net.minecraft.server.MinecraftServer, net.minecraft.server.world.ServerWorld>>
            worldLoadCallbacks = new ArrayList<>();

    private static boolean registered = false;

    public NeoForgeEventHelper() {
        if (!registered) {
            NeoForge.EVENT_BUS.register(EventHandler.class);
            registered = true;
        }
    }

    @Override
    public void onServerStarting(Consumer<net.minecraft.server.MinecraftServer> callback) {
        serverStartingCallbacks.add(callback);
    }

    @Override
    public void onServerStarted(Consumer<net.minecraft.server.MinecraftServer> callback) {
        serverStartedCallbacks.add(callback);
    }

    @Override
    public void onWorldLoad(
            BiConsumer<net.minecraft.server.MinecraftServer, net.minecraft.server.world.ServerWorld> callback) {
        worldLoadCallbacks.add(callback);
    }

    public static class EventHandler {
        @SubscribeEvent
        public static void onServerStarting(ServerStartingEvent event) {
            MinecraftServer server = event.getServer();
            for (Consumer<net.minecraft.server.MinecraftServer> callback : serverStartingCallbacks) {
                callback.accept((net.minecraft.server.MinecraftServer) (Object) server);
            }
        }

        @SubscribeEvent
        public static void onServerStarted(ServerStartedEvent event) {
            MinecraftServer server = event.getServer();
            for (Consumer<net.minecraft.server.MinecraftServer> callback : serverStartedCallbacks) {
                callback.accept((net.minecraft.server.MinecraftServer) (Object) server);
            }
        }

        @SubscribeEvent
        public static void onWorldLoad(LevelEvent.Load event) {
            if (event.getLevel() instanceof ServerLevel serverLevel) {
                MinecraftServer server = serverLevel.getServer();
                for (BiConsumer<net.minecraft.server.MinecraftServer, net.minecraft.server.world.ServerWorld> callback :
                        worldLoadCallbacks) {
                    callback.accept(
                            (net.minecraft.server.MinecraftServer) (Object) server,
                            (net.minecraft.server.world.ServerWorld) (Object) serverLevel);
                }
            }
        }
    }
}
