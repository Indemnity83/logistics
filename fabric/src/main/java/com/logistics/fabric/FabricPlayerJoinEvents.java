package com.logistics.fabric;

import com.logistics.core.lib.jei.SyncMachineRecipesPacket;
import com.logistics.power.engine.reaction.ReactionRecipeSyncPacket;
import com.logistics.core.crash.CrashReportNotifier;
import com.logistics.core.fluid.CrudeOilEffects;
import com.logistics.core.lib.platform.ServerNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/**
 * Shows operators the crash-reporting status line on join, resetting the once-per-session dedup when
 * a server starts, ships the machine recipes so JEI can display them on multiplayer clients, and clears
 * per-player timers on disconnect.
 */
public final class FabricPlayerJoinEvents {
    private FabricPlayerJoinEvents() {}

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> CrashReportNotifier.reset());
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            CrashReportNotifier.maybeNotify(handler.player);
            // A connection that never negotiated these channels (synthetic/mock players) rejects them.
            if (ServerNetworking.canSend(handler.player, SyncMachineRecipesPacket.TYPE)) {
                ServerNetworking.send(handler.player, SyncMachineRecipesPacket.from(server));
            }
            if (ServerNetworking.canSend(handler.player, ReactionRecipeSyncPacket.TYPE)) {
                ServerNetworking.send(handler.player, ReactionRecipeSyncPacket.from(server));
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> CrudeOilEffects.clearPlayer(handler.player.getUUID()));
    }
}
