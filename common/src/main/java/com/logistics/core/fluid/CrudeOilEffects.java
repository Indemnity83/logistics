package com.logistics.core.fluid;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/**
 * Per-tick gameplay effects for standing in Crude Oil: Nausea while the head is submerged, Poison after
 * continued exposure, and swim-speed damping while in the fluid. Wired into each loader's existing
 * network tick handler (mirrors {@code NetworkRegistry.tickNetworks}), matching
 * {@link com.logistics.core.fluid.CrudeOilSubmersion}'s loader-agnostic detection.
 */
public final class CrudeOilEffects {
    // Refreshed every tick while submerged; left alone otherwise, so the effect naturally fades a few
    // seconds after surfacing rather than needing an explicit on-exit clear.
    private static final int NAUSEA_DURATION_TICKS = 100; // 5s
    private static final int POISON_DURATION_TICKS = 60; // 3s
    // How long a player must be continuously head-submerged before Poison kicks in on top of Nausea.
    private static final int POISON_EXPOSURE_THRESHOLD_TICKS = 100; // 5s
    // Anchor value, not final — issue suggests 50-70% of normal water swim speed; tune by playtest.
    private static final double MOVEMENT_SCALE = 0.6;

    // In-memory only; resets on logout/relog, which is fine for a transient environmental timer.
    private static final Map<UUID, Integer> exposureTicks = new ConcurrentHashMap<>();

    private CrudeOilEffects() {}

    public static void tickAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tick(player);
        }
    }

    private static void tick(ServerPlayer player) {
        if (CrudeOilSubmersion.isEyeInCrudeOil(player)) {
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, NAUSEA_DURATION_TICKS, 0, true, false));

            int exposure = exposureTicks.merge(player.getUUID(), 1, Integer::sum);
            if (exposure >= POISON_EXPOSURE_THRESHOLD_TICKS) {
                player.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_DURATION_TICKS, 0, true, false));
            }
        } else {
            exposureTicks.remove(player.getUUID());
        }

        if (CrudeOilSubmersion.isBodyInCrudeOil(player)) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.multiply(MOVEMENT_SCALE, MOVEMENT_SCALE, MOVEMENT_SCALE));
        }
    }
}
