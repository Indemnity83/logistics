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
    // Re-applied periodically (not every tick) while submerged; left alone otherwise, so the effects
    // naturally fade a few seconds after surfacing rather than needing an explicit on-exit clear. The
    // refresh interval stays well under the duration so it never visibly lapses between refreshes.
    private static final int EFFECT_DURATION_TICKS = 100; // 5s
    private static final int EFFECT_REFRESH_INTERVAL_TICKS = 60; // 3s
    // Amplifier 1 = Slowness II (~30% reduction). Anchor value, not final — tune by playtest.
    private static final int SLOWNESS_AMPLIFIER = 1;

    // In-memory only; resets on logout/relog, which is fine for a transient environmental timer.
    private static final Map<UUID, Integer> submersionTicks = new ConcurrentHashMap<>();

    private CrudeOilEffects() {}

    public static void tickAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tick(player);
        }
    }

    private static void tick(ServerPlayer player) {
        if (!CrudeOilSubmersion.isEyeInCrudeOil(player)) {
            submersionTicks.remove(player.getUUID());
            return;
        }

        int ticks = submersionTicks.merge(player.getUUID(), 1, Integer::sum);
        if ((ticks - 1) % EFFECT_REFRESH_INTERVAL_TICKS != 0) {
            return;
        }

        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, EFFECT_DURATION_TICKS, 0, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.POISON, EFFECT_DURATION_TICKS, 0, true, false));
        player.addEffect(new MobEffectInstance(
                MobEffects.SLOWNESS, EFFECT_DURATION_TICKS, SLOWNESS_AMPLIFIER, true, false));
    }
}
