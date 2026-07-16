package com.logistics.fabric;

import com.logistics.automation.jei.SyncMachineRecipesPacket;
import com.logistics.core.crash.CrashReportNotifier;
import com.logistics.core.lib.platform.ServerNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/**
 * Shows operators the crash-reporting status line on join, resetting the once-per-session dedup when
 * a server starts, and ships the machine recipes so JEI can display them on multiplayer clients.
 * Pure wiring — the gate and message live in {@link CrashReportNotifier}.
 */
public final class FabricPlayerJoinEvents {
    private FabricPlayerJoinEvents() {}

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> CrashReportNotifier.reset());
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            CrashReportNotifier.maybeNotify(handler.player);
            ServerNetworking.send(handler.player, SyncMachineRecipesPacket.from(server));
        });
    }
}
