package com.logistics.core.crash;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsCore;
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
 * Shows operators a once-per-server-session crash-reporting status line with clickable toggles.
 *
 * <p>Shared by both loaders; the loader-specific hooks call {@link #maybeNotify} on player join and
 * {@link #reset()} when a server starts so each new world/server session shows the line once. It is
 * suppressed when an operator silences it via {@code /logistics config reporting show_notification false}.
 */
public final class CrashReportNotifier {
    private static final Set<UUID> NOTIFIED_THIS_SESSION = ConcurrentHashMap.newKeySet();

    private static final String ENABLE_COMMAND = "/logistics config reporting enabled true";
    private static final String DISABLE_COMMAND = "/logistics config reporting enabled false";
    private static final String HIDE_COMMAND = "/logistics config reporting show_notification false";

    /** Latest crash-reporting & privacy page, opened by the [More Info] link. */
    private static final String DETAILS_URL =
            "https://github.com/Indemnity83/logistics/blob/mc/26.2/CRASH_REPORTING.md";

    private CrashReportNotifier() {}

    /** Show operators the once-per-session crash-reporting status line (unless they've silenced it). */
    public static void maybeNotify(ServerPlayer player) {
        boolean isOperator = Commands.LEVEL_GAMEMASTERS.check(player.permissions());
        if (!shouldNotify(LogisticsConfigHost.get(LogisticsCore.CONFIG.CRASH_REPORTING_SHOW_NOTIFICATION), isOperator)) {
            return;
        }
        if (NOTIFIED_THIS_SESSION.add(player.getUUID())) {
            player.sendSystemMessage(buildInvite(LogisticsConfigHost.get(LogisticsCore.CONFIG.CRASH_REPORTING_ENABLED)));
        }
    }

    /** Clear the per-session dedup so the next server session shows the status line again. */
    public static void reset() {
        NOTIFIED_THIS_SESSION.clear();
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
                    .withClickEvent(new ClickEvent.RunCommand(DISABLE_COMMAND))
                    .withHoverEvent(new HoverEvent.ShowText(
                            Component.literal("Click to turn crash reporting off"))));
        }
        return Component.literal("[OFF]").withStyle(style -> style
                .withColor(ChatFormatting.RED)
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
                        Component.literal("Stop showing this on join (you can still use /logistics config)"))));
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
