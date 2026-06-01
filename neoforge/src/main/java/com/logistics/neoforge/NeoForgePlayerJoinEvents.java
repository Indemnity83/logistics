package com.logistics.neoforge;

import com.logistics.core.crash.CrashReportNotifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Invites operators to opt in to crash reporting when they join. Pure wiring — the gate and message
 * live in {@link CrashReportNotifier}.
 */
public final class NeoForgePlayerJoinEvents {
    private NeoForgePlayerJoinEvents() {}

    public static void register(IEventBus gameBus) {
        gameBus.addListener(NeoForgePlayerJoinEvents::onLogin);
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CrashReportNotifier.maybeNotify(player);
        }
    }
}
