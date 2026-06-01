package com.logistics.fabric;

import com.logistics.core.crash.CrashReportNotifier;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/**
 * Invites operators to opt in to crash reporting when they join. Pure wiring — the gate and message
 * live in {@link CrashReportNotifier}.
 */
public final class FabricPlayerJoinEvents {
    private FabricPlayerJoinEvents() {}

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                CrashReportNotifier.maybeNotify(handler.player));
    }
}
