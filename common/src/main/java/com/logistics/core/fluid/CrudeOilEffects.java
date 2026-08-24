package com.logistics.core.fluid;

import com.logistics.core.lib.resource.ResourceId;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Per-tick gameplay effects for standing in Crude Oil: Nausea while the head is submerged, Poison after
 * continued exposure, and swim-speed damping while in the fluid.
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
    // Anchor value, not final — tune by playtest.
    private static final double MOVEMENT_SCALE = 0.6;
    private static final ResourceId MOVEMENT_MODIFIER_ID = ResourceId.in("logistics", "crude_oil_swim_slow");
    // ADD_MULTIPLIED_TOTAL applies once, multiplicatively, on top of every other modifier — a stable
    // MOVEMENT_SCALE reduction that doesn't compound tick over tick like scaling velocity directly would.
    private static final AttributeModifier MOVEMENT_MODIFIER = new AttributeModifier(
            MOVEMENT_MODIFIER_ID.toIdentifier(), -(1 - MOVEMENT_SCALE), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

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

        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            boolean submerged = CrudeOilSubmersion.isBodyInCrudeOil(player);
            boolean hasModifier = speed.hasModifier(MOVEMENT_MODIFIER_ID.toIdentifier());
            if (submerged && !hasModifier) {
                speed.addTransientModifier(MOVEMENT_MODIFIER);
            } else if (!submerged && hasModifier) {
                speed.removeModifier(MOVEMENT_MODIFIER_ID.toIdentifier());
            }
        }
    }
}
