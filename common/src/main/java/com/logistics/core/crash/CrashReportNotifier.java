package com.logistics.core.crash;

import com.logistics.core.LogisticsConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;

import java.net.URI;
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

    private static final String ENABLE_COMMAND = "/logistics crashreports enable";
    private static final String DISABLE_COMMAND = "/logistics crashreports disable";
    private static final String HIDE_COMMAND = "/logistics crashreports notify off";

    /** Latest crash-reporting & privacy page, opened by the [More Info] link. */
    private static final String DETAILS_URL =
            "https://github.com/Indemnity83/logistics/blob/mc/26.1/CRASH_REPORTING.md";

    private CrashReportNotifier() {}

    /** Show operators the once-per-launch crash-reporting status line (unless they've silenced it). */
    public static void maybeNotify(ServerPlayer player) {
        LogisticsConfig.CrashReportingConfig cfg = LogisticsConfig.get().crashReporting;
        boolean isOperator = Commands.LEVEL_GAMEMASTERS.check(player.permissions());
        if (!shouldNotify(cfg.notifyOperators, isOperator)) {
            return;
        }
        if (NOTIFIED_THIS_LAUNCH.add(player.getUUID())) {
            player.sendSystemMessage(buildInvite(cfg.enabled));
        }
    }

    /** Pure gate: show the status line only to operators who haven't silenced the notice. */
    static boolean shouldNotify(boolean notifyOperators, boolean isOperator) {
        return notifyOperators && isOperator;
    }

    /** Status line whose toggle reflects the current state: [ON] turns it off, [OFF] turns it on. */
    static Component buildInvite(boolean enabled) {
        return Component.empty()
                .append(Component.literal("[Logistics] ").withStyle(ChatFormatting.AQUA))
                .append(Component.literal("Crash reporting is ").withStyle(ChatFormatting.GRAY))
                .append(statusToggle(enabled))
                .append(Component.literal(" "))
                .append(hideNotificationLink())
                .append(Component.literal(" "))
                .append(moreInfoLink());
    }

    private static Component statusToggle(boolean enabled) {
        if (enabled) {
            return Component.literal("[ON]").withStyle(style -> style
                    .withColor(ChatFormatting.GREEN)
                    .withBold(true)
                    .withClickEvent(new ClickEvent.RunCommand(DISABLE_COMMAND))
                    .withHoverEvent(new HoverEvent.ShowText(
                            Component.literal("Click to turn crash reporting off"))));
        }
        return Component.literal("[OFF]").withStyle(style -> style
                .withColor(ChatFormatting.RED)
                .withBold(true)
                .withClickEvent(new ClickEvent.RunCommand(ENABLE_COMMAND))
                .withHoverEvent(new HoverEvent.ShowText(
                        Component.literal("Click to turn on sanitized crash reporting"))));
    }

    private static Component hideNotificationLink() {
        return Component.literal("[Hide Notification]").withStyle(style -> style
                .withColor(ChatFormatting.GRAY)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent.RunCommand(HIDE_COMMAND))
                .withHoverEvent(new HoverEvent.ShowText(
                        Component.literal("Stop showing this on join (you can still use /logistics crashreports)"))));
    }

    private static Component moreInfoLink() {
        return Component.literal("[More Info]").withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent.OpenUrl(URI.create(DETAILS_URL)))
                .withHoverEvent(new HoverEvent.ShowText(
                        Component.literal("Open the crash reporting & privacy page"))));
    }
}
