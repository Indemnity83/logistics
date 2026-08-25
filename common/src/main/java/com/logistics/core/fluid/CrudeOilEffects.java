package com.logistics.core.fluid;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Per-tick gameplay effects for standing in Crude Oil: Nausea, Poison, and Slowness, all applied
 * together while the head is submerged.
 */
public final class CrudeOilEffects {
    // Re-applied periodically while submerged; fades naturally after surfacing.
    private static final int EFFECT_DURATION_TICKS = 100; // 5s
    private static final int EFFECT_REFRESH_INTERVAL_TICKS = 60; // 3s
    // Amplifier 2 = Slowness III (~45% reduction, close to the 50-70%-of-normal target).
    private static final int SLOWNESS_AMPLIFIER = 2;

    private static final Map<UUID, Integer> submersionTicks = new ConcurrentHashMap<>();

    private CrudeOilEffects() {}

    public static void tickAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tick(player);
        }
    }

    /** Clears a player's timer, e.g. on disconnect, so relogging doesn't resume a stale one. */
    public static void clearPlayer(UUID id) {
        submersionTicks.remove(id);
    }

    private static void tick(ServerPlayer player) {
        if (!CrudeOilSubmersion.isEyeInCrudeOil(player)) {
            clearPlayer(player.getUUID());
            return;
        }

        int ticks = submersionTicks.merge(player.getUUID(), 1, Integer::sum);
        if ((ticks - 1) % EFFECT_REFRESH_INTERVAL_TICKS != 0) {
            return;
        }

        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, EFFECT_DURATION_TICKS, 0, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.POISON, EFFECT_DURATION_TICKS, 0, true, false));
        player.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN, EFFECT_DURATION_TICKS, SLOWNESS_AMPLIFIER, true, false));
    }
}
