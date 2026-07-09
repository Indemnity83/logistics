package com.logistics.core;

import com.indemnity83.configory.Config;
import com.indemnity83.configory.command.CommandFeedback;
import com.indemnity83.configory.command.ConfigCommands;
import com.logistics.LogisticsConfigHost;
import com.logistics.core.crash.CrashReporting;
import com.logistics.core.lib.platform.PlatformService;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Builds the /logistics command tree using the Brigadier API.
 *
 * <p>This class is loader-agnostic — it constructs the command literal that each
 * platform (Fabric, NeoForge) registers via its own event system.
 *
 * <p>/logistics debug                  — show registered domains and enabled state
 * /logistics debug &lt;domain&gt; true    — enable debug logging for a named domain
 * /logistics debug &lt;domain&gt; false   — disable debug logging for a named domain
 *
 * <p>/logistics config &lt;domain&gt;               — list a domain's keys (engines/machines/pipes/fluids/reporting)
 * /logistics config &lt;domain&gt; &lt;key&gt;         — show one value
 * /logistics config &lt;domain&gt; &lt;key&gt; &lt;value&gt; — set it (parsed + validated)
 * /logistics reload-configs                — reload configory-backed config from disk
 */
public final class LogisticsCommandTree {
    private LogisticsCommandTree() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("logistics/command");

    private static final SuggestionProvider<CommandSourceStack> DOMAIN_SUGGESTIONS =
        (ctx, builder) -> SharedSuggestionProvider.suggest(sortedDomains(), builder);

    /** Delivers configory's generated-command output (list/get/set results) to the command source. */
    private static final CommandFeedback<CommandSourceStack> CONFIG_FEEDBACK =
        (source, message) -> source.sendSuccess(() -> Component.literal(message), false);

    private static List<String> sortedDomains() {
        List<String> domains = new ArrayList<>(DebugLog.getRegisteredDomains());
        Collections.sort(domains);
        return domains;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("logistics")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.literal("debug")
                .executes(ctx -> {
                    List<String> registered = sortedDomains();
                    Set<String> enabled = DebugLog.getEnabledDomains();
                    if (registered.isEmpty()) {
                        ctx.getSource().sendSuccess(
                            () -> Component.literal("No debug domains registered."), false);
                    } else {
                        StringBuilder sb = new StringBuilder("Debug domains:");
                        for (String domain : registered) {
                            sb.append("\n  ").append(domain).append(": ")
                              .append(enabled.contains(domain) ? "ON" : "off");
                        }
                        String msg = sb.toString();
                        ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
                    }
                    return 1;
                })
                .then(Commands.argument("domain", StringArgumentType.word())
                    .suggests(DOMAIN_SUGGESTIONS)
                    .then(Commands.argument("state", BoolArgumentType.bool())
                        .executes(ctx -> {
                            String domain = StringArgumentType.getString(ctx, "domain");
                            boolean state = BoolArgumentType.getBool(ctx, "state");
                            if (!DebugLog.getRegisteredDomains().contains(domain)) {
                                ctx.getSource().sendFailure(Component.literal(
                                    "Unknown debug domain: " + domain
                                    + ". Known: " + sortedDomains()));
                                return 0;
                            }
                            DebugLog.setEnabled(domain, state);
                            ctx.getSource().sendSuccess(
                                () -> Component.literal(
                                    domain + " debug logging: " + (state ? "ON" : "OFF")),
                                true);
                            return 1;
                        })
                    )
                )
            );

        // Diagnostics actions (enable/disable/notify/dsn are now config keys). `preview` is always available —
        // read-only privacy transparency; `test` is dev-only.
        LiteralArgumentBuilder<CommandSourceStack> diagnostics = Commands.literal("diagnostics")
            .then(Commands.literal("preview")
                .executes(ctx -> {
                    CrashReporting.logPreviewReport();
                    ctx.getSource().sendSuccess(LogisticsCommandTree::diagnosticsPreviewSummary, false);
                    return 1;
                }));
        if (inDevelopmentEnvironment()) {
            diagnostics.then(Commands.literal("test")
                .executes(ctx -> {
                    if (!CrashReporting.captureTestReport()) {
                        ctx.getSource().sendFailure(Component.literal(
                            "Crash reporting is not active. Set /logistics config crash_reporting_enabled"
                            + " true, then /logistics reload-configs."));
                        return 0;
                    }
                    ctx.getSource().sendSuccess(
                        () -> Component.literal("Sent a temporary test crash report to Sentry."), false);
                    return 1;
                }));
        }
        root.then(diagnostics);

        // Configory generates the /logistics config <domain> <key> surface + reload-configs. Each registered
        // per-domain config is a group (named by its config id) so keys stay short (e.g. `config pipes
        // max_speed`). All inherit this literal's operator gate.
        var configCommands = ConfigCommands.builder(CONFIG_FEEDBACK);
        for (Config config : LogisticsConfigHost.domainConfigs()) {
            configCommands.group(config);
        }
        configCommands.buildInto(root);
        return root;
    }

    /**
     * Resolves the development-environment flag defensively: any failure is treated as production
     * (false) so dev-only command nodes can never abort registration of the whole command tree.
     */
    private static boolean inDevelopmentEnvironment() {
        try {
            return PlatformService.INSTANCE.isDevelopmentEnvironment();
        } catch (Throwable t) {
            LOGGER.warn("Could not determine development environment; treating as production", t);
            return false;
        }
    }

    /** Chat confirmation for {@code /logistics diagnostics preview} — the full report goes to the log. */
    private static Component diagnosticsPreviewSummary() {
        return Component.empty()
            .append(Component.literal("Wrote an example sanitized crash report to the log.\n")
                .withStyle(ChatFormatting.GRAY))
            .append(Component.literal("Includes: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal("mod/Minecraft/loader/Java/OS versions and a Logistics stack trace.\n")
                .withStyle(ChatFormatting.WHITE))
            .append(Component.literal("Never includes: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal("names, UUIDs, IPs, server addresses, chat, or world data.")
                .withStyle(ChatFormatting.WHITE));
    }
}
