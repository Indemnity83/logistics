package com.logistics.core;

import com.logistics.core.crash.CrashReporting;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

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
 * <p>/logistics config list            — list all config keys, values, and descriptions
 * /logistics config get &lt;key&gt;        — show current value for a config key
 * /logistics config set &lt;key&gt; &lt;val&gt; — set a config value and save to disk
 * /logistics config reload            — reload config from disk
 */
public final class LogisticsCommandTree {
    private LogisticsCommandTree() {}

    private static final SuggestionProvider<CommandSourceStack> DOMAIN_SUGGESTIONS =
        (ctx, builder) -> SharedSuggestionProvider.suggest(sortedDomains(), builder);

    private static final SuggestionProvider<CommandSourceStack> CONFIG_KEY_SUGGESTIONS =
        (ctx, builder) -> SharedSuggestionProvider.suggest(LogisticsConfig.ENTRIES.keySet(), builder);

    private static List<String> sortedDomains() {
        List<String> domains = new ArrayList<>(DebugLog.getRegisteredDomains());
        Collections.sort(domains);
        return domains;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("logistics")
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
            )
            .then(Commands.literal("config")
                .then(Commands.literal("list")
                    .executes(ctx -> {
                        StringBuilder sb = new StringBuilder("Logistics config:");
                        for (LogisticsConfig.ConfigEntry<?> entry : LogisticsConfig.ENTRIES.values()) {
                            sb.append("\n  ").append(entry.key())
                              .append(" = ").append(entry.getAsString())
                              .append("  \u00a77(").append(entry.description()).append(")\u00a7r");
                        }
                        String msg = sb.toString();
                        ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
                        return 1;
                    }))
                .then(Commands.literal("get")
                    .then(Commands.argument("key", StringArgumentType.word())
                        .suggests(CONFIG_KEY_SUGGESTIONS)
                        .executes(ctx -> {
                            String key = StringArgumentType.getString(ctx, "key");
                            LogisticsConfig.ConfigEntry<?> entry = LogisticsConfig.ENTRIES.get(key);
                            if (entry == null) {
                                ctx.getSource().sendFailure(Component.literal(
                                    "Unknown config key: " + key));
                                return 0;
                            }
                            ctx.getSource().sendSuccess(
                                () -> Component.literal(key + " = " + entry.getAsString()), false);
                            return 1;
                        })))
                .then(Commands.literal("set")
                    .then(Commands.argument("key", StringArgumentType.word())
                        .suggests(CONFIG_KEY_SUGGESTIONS)
                        .then(Commands.argument("value", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                String key = StringArgumentType.getString(ctx, "key");
                                String value = StringArgumentType.getString(ctx, "value");
                                LogisticsConfig.ConfigEntry<?> entry = LogisticsConfig.ENTRIES.get(key);
                                if (entry == null) {
                                    ctx.getSource().sendFailure(Component.literal(
                                        "Unknown config key: " + key));
                                    return 0;
                                }
                                try {
                                    entry.setFromString(value);
                                } catch (Exception e) {
                                    ctx.getSource().sendFailure(Component.literal(
                                        "Invalid value for " + key + ": " + e.getMessage()));
                                    return 0;
                                }
                                LogisticsConfig.save();
                                ctx.getSource().sendSuccess(
                                    () -> Component.literal("Set " + key + " = " + entry.getAsString()),
                                    true);
                                return 1;
                            }))))
                .then(Commands.literal("reload")
                    .executes(ctx -> {
                        LogisticsConfig.reload();
                        ctx.getSource().sendSuccess(
                            () -> Component.literal("Reloaded logistics config from disk"), true);
                        return 1;
                    })))
            .then(Commands.literal("crashreports")
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal(CrashReporting.statusText()), false);
                    return 1;
                })
                .then(Commands.literal("enable")
                    .executes(ctx -> {
                        ctx.getSource().sendSuccess(
                            () -> Component.literal(CrashReporting.setReportingEnabled(true)), true);
                        return 1;
                    }))
                .then(Commands.literal("disable")
                    .executes(ctx -> {
                        ctx.getSource().sendSuccess(
                            () -> Component.literal(CrashReporting.setReportingEnabled(false)), true);
                        return 1;
                    }))
                .then(Commands.literal("preview")
                    .executes(ctx -> {
                        ctx.getSource().sendSuccess(
                            () -> Component.literal(
                                "Example sanitized report (NOT sent — this is a preview):\n"
                                + CrashReporting.previewReport()),
                            false);
                        return 1;
                    }))
                .then(Commands.literal("notify")
                    .then(Commands.literal("on")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(
                                () -> Component.literal(CrashReporting.setJoinNotice(true)), true);
                            return 1;
                        }))
                    .then(Commands.literal("off")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(
                                () -> Component.literal(CrashReporting.setJoinNotice(false)), true);
                            return 1;
                        }))));
    }
}
