package com.logistics.power.block;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPower;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Config-backed numbers behind the battery ladder. */
@DisplayName("Battery tiers")
class BatteryTierTest {

    /**
     * The single untiered Battery shipped at 100,000 RF with 1,000 RF/t per side and a 200 RF/t
     * push. {@code power/battery} now aliases to Copper, so those values must survive verbatim —
     * otherwise loading a pre-tier world silently re-rates every battery in it.
     */
    @Test
    @DisplayName("Copper keeps the values the untiered Battery shipped with, with Tin added below it")
    void copperPreservesTheLegacyBatteryNumbers() {
        assertThat(BatteryTier.COPPER.capacity()).isEqualTo(100_000L);
        assertThat(BatteryTier.COPPER.maxIo()).isEqualTo(1_000L);
        assertThat(BatteryTier.COPPER.outputPerSide()).isEqualTo(200L);
        assertThat(BatteryTier.TIN.capacity())
                .as("Tin was added underneath Copper, not in place of it")
                .isLessThan(BatteryTier.COPPER.capacity());
    }

    @Test
    @DisplayName("capacity and throughput both rise with every step up the ladder")
    void ladderIsStrictlyIncreasing() {
        List<BatteryTier> ladder = Arrays.asList(BatteryTier.values());
        for (int i = 1; i < ladder.size(); i++) {
            BatteryTier lower = ladder.get(i - 1);
            BatteryTier upper = ladder.get(i);
            assertThat(upper.capacity())
                    .as("%s capacity must exceed %s", upper, lower)
                    .isGreaterThan(lower.capacity());
            assertThat(upper.maxIo())
                    .as("%s max I/O must exceed %s", upper, lower)
                    .isGreaterThan(lower.maxIo());
            assertThat(upper.outputPerSide())
                    .as("%s per-side push must exceed %s", upper, lower)
                    .isGreaterThan(lower.outputPerSide());
        }
    }

    /**
     * Each accessor is a five-armed switch over per-tier config keys, so an arm wired to the wrong
     * tier's key would read a plausible number and go unnoticed. Anchoring every tier against the
     * capacity key it is supposed to read catches that.
     */
    @ParameterizedTest(name = "{0}")
    @EnumSource(BatteryTier.class)
    @DisplayName("every tier reads its own config section")
    void eachTierReadsItsOwnKeys(BatteryTier tier) {
        assertThat(tier.capacity()).isEqualTo(LogisticsConfigHost.get(switch (tier) {
            case TIN -> LogisticsPower.CONFIG.BATTERY_TIN_CAPACITY;
            case COPPER -> LogisticsPower.CONFIG.BATTERY_COPPER_CAPACITY;
            case GOLD -> LogisticsPower.CONFIG.BATTERY_GOLD_CAPACITY;
            case AMETHYST -> LogisticsPower.CONFIG.BATTERY_AMETHYST_CAPACITY;
            case ECHO -> LogisticsPower.CONFIG.BATTERY_ECHO_CAPACITY;
        }));
        assertThat(tier.id()).isEqualTo(tier.name().toLowerCase(java.util.Locale.ROOT) + "_battery");
    }

    @Test
    @DisplayName("a tier's per-side push never exceeds its own I/O ceiling")
    void pushNeverExceedsIoCeiling() {
        for (BatteryTier tier : BatteryTier.values()) {
            assertThat(tier.outputPerSide())
                    .as("%s per-side push vs its own max I/O", tier)
                    .isLessThanOrEqualTo(tier.maxIo());
        }
    }
}
