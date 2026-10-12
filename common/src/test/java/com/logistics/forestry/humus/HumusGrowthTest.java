package com.logistics.forestry.humus;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsForestry;
import com.logistics.test.MinecraftTestEnvironment;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Humus growth decisions")
class HumusGrowthTest extends MinecraftTestEnvironment {

    /** A random source whose every {@code nextDouble} is {@code roll}, counting how often it is asked. */
    private static final class FixedRoll extends SingleThreadedRandomSource {
        private final double roll;
        private int draws;

        FixedRoll(double roll) {
            super(0L);
            this.roll = roll;
        }

        @Override
        public double nextDouble() {
            draws++;
            return roll;
        }
    }

    @Test
    @DisplayName("config defaults: 25% boost chance, 50% degrade chance")
    void configDefaults() {
        assertThat(LogisticsConfigHost.get(LogisticsForestry.CONFIG.HUMUS_GROWTH_CHANCE)).isEqualTo(0.25);
        assertThat(LogisticsConfigHost.get(LogisticsForestry.CONFIG.HUMUS_DEGRADE_CHANCE)).isEqualTo(0.5);
    }

    @Test
    @DisplayName("a boost happens only when the roll is under the growth chance")
    void boostOnlyWhenRollIsUnderChance() {
        assertThat(HumusGrowth.shouldBoost(new FixedRoll(0.0), 0.25)).isTrue();
        assertThat(HumusGrowth.shouldBoost(new FixedRoll(0.249), 0.25)).isTrue();
        assertThat(HumusGrowth.shouldBoost(new FixedRoll(0.25), 0.25)).isFalse();
        assertThat(HumusGrowth.shouldBoost(new FixedRoll(0.9), 0.25)).isFalse();
    }

    @Test
    @DisplayName("a zero growth chance never boosts and a full one always does")
    void growthChanceBounds() {
        assertThat(HumusGrowth.shouldBoost(new FixedRoll(0.0), 0.0)).isFalse();
        assertThat(HumusGrowth.shouldBoost(new FixedRoll(Math.nextDown(1.0)), 1.0)).isTrue();
    }

    @Test
    @DisplayName("boosts land at about the configured rate")
    void boostRateMatchesChance() {
        RandomSource random = RandomSource.create(42L);
        int boosts = 0;
        for (int i = 0; i < 4000; i++) {
            if (HumusGrowth.shouldBoost(random, 0.25)) {
                boosts++;
            }
        }
        assertThat(boosts).isBetween(850, 1150);
    }

    @Test
    @DisplayName("humus degrades only after a tree grew and the roll is under the degrade chance")
    void degradeOnlyAfterGrowthAndChance() {
        assertThat(HumusGrowth.shouldDegrade(true, new FixedRoll(0.1), 0.5)).isTrue();
        assertThat(HumusGrowth.shouldDegrade(true, new FixedRoll(0.5), 0.5)).isFalse();
        assertThat(HumusGrowth.shouldDegrade(false, new FixedRoll(0.0), 0.5)).isFalse();
        assertThat(HumusGrowth.shouldDegrade(false, new FixedRoll(0.0), 1.0)).isFalse();
        assertThat(HumusGrowth.shouldDegrade(true, new FixedRoll(0.0), 0.0)).isFalse();
    }

    @Test
    @DisplayName("a sapling that only advanced a stage spends no degrade roll")
    void noRollWithoutGrowth() {
        FixedRoll random = new FixedRoll(0.0);
        HumusGrowth.shouldDegrade(false, random, 0.5);
        assertThat(random.draws).isZero();
    }

    @Test
    @DisplayName("growth needs the same light as a vanilla sapling")
    void lightThreshold() {
        assertThat(HumusGrowth.hasLightToGrow(8)).isFalse();
        assertThat(HumusGrowth.hasLightToGrow(9)).isTrue();
        assertThat(HumusGrowth.hasLightToGrow(15)).isTrue();
        assertThat(HumusGrowth.hasLightToGrow(0)).isFalse();
    }
}
