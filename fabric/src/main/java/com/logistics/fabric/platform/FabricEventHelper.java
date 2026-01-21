package com.logistics.fabric.platform;

import com.logistics.platform.services.IEventHelper;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

/**
 * Fabric implementation of IEventHelper using lifecycle events.
 */
public class FabricEventHelper implements IEventHelper {

    @Override
    public void onServerStarting(Consumer<MinecraftServer> callback) {
        ServerLifecycleEvents.SERVER_STARTING.register(callback::accept);
    }

    @Override
    public void onServerStarted(Consumer<MinecraftServer> callback) {
        ServerLifecycleEvents.SERVER_STARTED.register(callback::accept);
    }

    @Override
    public void onWorldLoad(BiConsumer<MinecraftServer, ServerWorld> callback) {
        ServerWorldEvents.LOAD.register(callback::accept);
    }
}
