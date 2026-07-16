package com.logistics.neoforge;

import com.logistics.automation.jei.SyncMachineRecipesPacket;
import com.logistics.core.crash.CrashReportNotifier;
import com.logistics.core.lib.platform.ServerNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

/**
 * Shows operators the crash-reporting status line on join, resetting the once-per-session dedup when
 * a server starts, and ships the machine recipes so JEI can display them on multiplayer clients.
 * Pure wiring — the gate and message live in {@link CrashReportNotifier}.
 */
public final class NeoForgePlayerJoinEvents {
    private NeoForgePlayerJoinEvents() {}

    public static void register(IEventBus gameBus) {
        gameBus.addListener(NeoForgePlayerJoinEvents::onServerStarting);
        gameBus.addListener(NeoForgePlayerJoinEvents::onLogin);
    }

    private static void onServerStarting(ServerStartingEvent event) {
        CrashReportNotifier.reset();
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CrashReportNotifier.maybeNotify(player);
            ServerNetworking.send(player, SyncMachineRecipesPacket.from(player.level().getServer()));
        }
    }
}
