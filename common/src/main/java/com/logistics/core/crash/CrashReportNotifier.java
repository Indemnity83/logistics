package com.logistics.core.crash;

import com.logistics.core.LogisticsConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sends operators a one-time (per launch) invite to opt in to sanitized crash reporting.
 *
 * <p>Shared by both loaders; the loader-specific player-join hooks resolve the {@link ServerPlayer}
 * and call {@link #maybeNotify}. The message is suppressed once reporting is enabled or an operator
 * silences it via {@code /logistics crashreports notify off}.
 */
public final class CrashReportNotifier {
    private static final Set<UUID> NOTIFIED_THIS_LAUNCH = ConcurrentHashMap.newKeySet();

    /** Latest crash-reporting & privacy page. Bare URLs are auto-linkified by the client. */
    private static final String DETAILS_URL =
            "https://github.com/Indemnity83/logistics/blob/mc/26.1/CRASH_REPORTING.md";

    private CrashReportNotifier() {}

    /** Notify {@code player} if they are an operator who hasn't been invited yet this launch. */
    public static void maybeNotify(ServerPlayer player) {
        LogisticsConfig.CrashReportingConfig cfg = LogisticsConfig.get().crashReporting;
        if (cfg.enabled || !cfg.notifyOperators) {
            return;
        }
        if (!Commands.LEVEL_GAMEMASTERS.check(player.permissions())) {
            return;
        }
        if (!NOTIFIED_THIS_LAUNCH.add(player.getUUID())) {
            return;
        }
        player.sendSystemMessage(buildInvite());
    }

    private static Component buildInvite() {
        return Component.empty()
                .append(Component.literal("[Logistics] ").withStyle(ChatFormatting.AQUA))
                .append(Component.literal(
                        "Optional crash reporting is OFF. If enabled, Logistics sends sanitized mod "
                        + "error diagnostics (mod/Minecraft/loader/Java/OS versions and stack traces) "
                        + "to help fix bugs. It does not send chat, world data, player names, UUIDs, "
                        + "IPs, or server addresses. ")
                        .withStyle(ChatFormatting.GRAY))
                .append(Component.literal("Opt in: /logistics crashreports enable").withStyle(ChatFormatting.WHITE))
                .append(Component.literal("  •  hide this: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("/logistics crashreports notify off").withStyle(ChatFormatting.WHITE))
                .append(Component.literal("\nWhat's collected: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(DETAILS_URL).withStyle(ChatFormatting.AQUA));
    }
}
