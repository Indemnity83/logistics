package com.logistics.fabric;

import com.logistics.core.crash.CrashReportNotifier;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/**
 * Shows operators the crash-reporting status line on join, resetting the once-per-session dedup when
 * a server starts. Pure wiring — the gate and message live in {@link CrashReportNotifier}.
 */
public final class FabricPlayerJoinEvents {
    private FabricPlayerJoinEvents() {}

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> CrashReportNotifier.reset());
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                CrashReportNotifier.maybeNotify(handler.player));
    }
}
