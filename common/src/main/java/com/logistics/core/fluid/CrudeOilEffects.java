package com.logistics.core.fluid;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Per-tick gameplay effects for standing in Crude Oil: Nausea while the head is submerged, Poison after
 * continued exposure, and Slowness while in the fluid.
 */
public final class CrudeOilEffects {
    // Re-applied periodically (not every tick) while submerged; left alone otherwise, so the effect
    // naturally fades a few seconds after surfacing rather than needing an explicit on-exit clear. The
    // refresh interval stays well under the duration so it never visibly lapses between refreshes.
    private static final int NAUSEA_DURATION_TICKS = 100; // 5s
    private static final int NAUSEA_REFRESH_INTERVAL_TICKS = 60; // 3s
    private static final int POISON_DURATION_TICKS = 60; // 3s
    // How long a player must be continuously head-submerged before Poison kicks in on top of Nausea.
    private static final int POISON_EXPOSURE_THRESHOLD_TICKS = 100; // 5s
    private static final int SLOWNESS_DURATION_TICKS = 100; // 5s
    private static final int SLOWNESS_REFRESH_INTERVAL_TICKS = 60; // 3s
    // Amplifier 1 = Slowness II (~30% reduction). Anchor value, not final — tune by playtest.
    private static final int SLOWNESS_AMPLIFIER = 1;

    // In-memory only; resets on logout/relog, which is fine for a transient environmental timer.
    private static final Map<UUID, Integer> exposureTicks = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> submersionTicks = new ConcurrentHashMap<>();

    private CrudeOilEffects() {}

    public static void tickAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tick(player);
        }
    }

    private static void tick(ServerPlayer player) {
        if (CrudeOilSubmersion.isEyeInCrudeOil(player)) {
            int exposure = exposureTicks.merge(player.getUUID(), 1, Integer::sum);
            if ((exposure - 1) % NAUSEA_REFRESH_INTERVAL_TICKS == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, NAUSEA_DURATION_TICKS, 0, true, false));
            }
            if (exposure >= POISON_EXPOSURE_THRESHOLD_TICKS) {
                player.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_DURATION_TICKS, 0, true, false));
            }
        } else {
            exposureTicks.remove(player.getUUID());
        }

        if (CrudeOilSubmersion.isBodyInCrudeOil(player)) {
            int submersion = submersionTicks.merge(player.getUUID(), 1, Integer::sum);
            if ((submersion - 1) % SLOWNESS_REFRESH_INTERVAL_TICKS == 0) {
                player.addEffect(new MobEffectInstance(
                        MobEffects.SLOWNESS, SLOWNESS_DURATION_TICKS, SLOWNESS_AMPLIFIER, true, false));
            }
        } else {
            submersionTicks.remove(player.getUUID());
        }
    }
}
