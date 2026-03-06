package com.logistics.core;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.Set;

/**
 * Registers /logistics commands.
 * /logistics debug                  — show registered domains and enabled state
 * /logistics debug <domain> true    — enable debug logging for a named domain
 * /logistics debug <domain> false   — disable debug logging for a named domain
 *
 * Registered domains come from DebugLog.register() calls in bootstrap.
 * Example: /logistics debug network true
 */
public final class LogisticsCommands {
    private LogisticsCommands() {}

    private static final SuggestionProvider<CommandSourceStack> DOMAIN_SUGGESTIONS =
        (ctx, builder) -> SharedSuggestionProvider.suggest(DebugLog.getRegisteredDomains(), builder);

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(
                Commands.literal("logistics")
                    .then(Commands.literal("debug")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> {
                            Set<String> registered = DebugLog.getRegisteredDomains();
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
            )
        );
    }
}
