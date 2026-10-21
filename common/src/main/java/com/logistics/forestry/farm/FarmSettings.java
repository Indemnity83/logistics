package com.logistics.forestry.farm;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsForestry;
import com.logistics.core.machine.tier.MachineTier;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * The farm's tunables in one snapshot. Production reads them from config each tick
 * ({@link #fromConfig()}); tests construct them directly. Out-of-range values are normalised, so
 * every snapshot is workable: costs never negative and every {@link FarmCost} has one, at least one
 * read per check, the idle back-off cap never below its base, and every tier has a profile.
 *
 * <p>{@code readsPerCheck} is how many block reads one check buys; inspecting a position costs one
 * check.
 *
 * <p>A tier's energy capacity and maximum input size the machine's buffer when the
 * block entity is built and whenever its tier changes; the rest apply live.
 */
public record FarmSettings(
        Map<FarmCost, Long> costs,
        int readsPerCheck,
        int idleBackoffBaseTicks,
        int idleBackoffMaxTicks,
        Map<MachineTier, FarmTierProfile> tiers) {

    public static final int DEFAULT_READS_PER_CHECK = 64;
    public static final int DEFAULT_IDLE_BACKOFF_BASE_TICKS = 20;
    public static final int DEFAULT_IDLE_BACKOFF_MAX_TICKS = 200;

    public FarmSettings {
        costs = normaliseCosts(costs);
        readsPerCheck = Math.max(1, readsPerCheck);
        idleBackoffBaseTicks = Math.max(1, idleBackoffBaseTicks);
        idleBackoffMaxTicks = Math.max(idleBackoffBaseTicks, idleBackoffMaxTicks);
        tiers = normalise(tiers);
    }

    /** The shipped settings. */
    public static FarmSettings defaults() {
        return new FarmSettings(
                FarmCost.defaults(),
                DEFAULT_READS_PER_CHECK,
                DEFAULT_IDLE_BACKOFF_BASE_TICKS,
                DEFAULT_IDLE_BACKOFF_MAX_TICKS,
                defaultTiers());
    }

    /** RF one {@code cost} action takes. */
    public long cost(FarmCost cost) {
        return costs.get(cost);
    }

    /** The profile of {@code tier}. */
    public FarmTierProfile profile(MachineTier tier) {
        return tiers.get(tier);
    }

    public FarmSettings withCost(FarmCost cost, long rf) {
        Map<FarmCost, Long> changed = new EnumMap<>(costs);
        changed.put(cost, rf);
        return new FarmSettings(changed, readsPerCheck, idleBackoffBaseTicks, idleBackoffMaxTicks, tiers);
    }

    public FarmSettings withReadsPerCheck(int reads) {
        return new FarmSettings(costs, reads, idleBackoffBaseTicks, idleBackoffMaxTicks, tiers);
    }

    public FarmSettings withIdleBackoff(int baseTicks, int maxTicks) {
        return new FarmSettings(costs, readsPerCheck, baseTicks, maxTicks, tiers);
    }

    public FarmSettings withTiers(Map<MachineTier, FarmTierProfile> tiers) {
        return new FarmSettings(costs, readsPerCheck, idleBackoffBaseTicks, idleBackoffMaxTicks, tiers);
    }

    /** {@code profile} at every tier. */
    public static Map<MachineTier, FarmTierProfile> everyTier(FarmTierProfile profile) {
        Map<MachineTier, FarmTierProfile> tiers = new EnumMap<>(MachineTier.class);
        for (MachineTier tier : MachineTier.values()) {
            tiers.put(tier, profile);
        }
        return tiers;
    }

    /** The shipped profile of every tier. */
    public static Map<MachineTier, FarmTierProfile> defaultTiers() {
        Map<MachineTier, FarmTierProfile> tiers = new EnumMap<>(MachineTier.class);
        for (MachineTier tier : MachineTier.values()) {
            tiers.put(tier, FarmTierProfile.defaults(tier));
        }
        return tiers;
    }

    public static FarmSettings fromConfig() {
        Map<MachineTier, FarmTierProfile> tiers = new EnumMap<>(MachineTier.class);
        for (MachineTier tier : MachineTier.values()) {
            tiers.put(tier, FarmTierProfile.fromConfig(tier));
        }
        Map<FarmCost, Long> costs = new EnumMap<>(FarmCost.class);
        for (FarmCost cost : FarmCost.values()) {
            costs.put(cost, LogisticsConfigHost.get(LogisticsForestry.CONFIG.farmCost(cost)));
        }
        return new FarmSettings(
                costs,
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_READS_PER_CHECK),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_IDLE_BACKOFF_BASE_TICKS),
                LogisticsConfigHost.get(LogisticsForestry.CONFIG.FARM_IDLE_BACKOFF_MAX_TICKS),
                tiers);
    }

    private static Map<FarmCost, Long> normaliseCosts(Map<FarmCost, Long> costs) {
        Map<FarmCost, Long> result = new EnumMap<>(FarmCost.class);
        for (FarmCost cost : FarmCost.values()) {
            Long rf = costs.get(cost);
            result.put(cost, rf == null ? cost.defaultRf() : Math.max(0, rf));
        }
        return Collections.unmodifiableMap(result);
    }

    /** Fills missing tiers with their defaults. */
    private static Map<MachineTier, FarmTierProfile> normalise(Map<MachineTier, FarmTierProfile> tiers) {
        Map<MachineTier, FarmTierProfile> result = new EnumMap<>(MachineTier.class);
        for (MachineTier tier : MachineTier.values()) {
            result.put(tier, tiers.getOrDefault(tier, FarmTierProfile.defaults(tier)));
        }
        return Collections.unmodifiableMap(result);
    }
}
