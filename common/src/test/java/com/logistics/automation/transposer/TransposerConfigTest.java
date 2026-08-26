package com.logistics.automation.transposer;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsConfigHost;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Locks in the Transposer's documented power/tank numbers (see wiki/Transposer.txt § Usage, §
 * Power) as fast, engine-free regression guards. All four match the wiki exactly — no discrepancy
 * found here.
 */
@DisplayName("Transposer config")
class TransposerConfigTest {

    /**
     * Wiki claim (Power): "It holds 20,000 RF and accepts up to 128 RF/tick." (Usage): "The tank
     * holds 16,000 mB (16 buckets) by default."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Transposer#Power">wiki/Transposer.txt § Power</a>
     */
    @Test
    @DisplayName("capacity, insert-rate, and tank-capacity config defaults match the wiki")
    void powerAndTankConfigMatchWiki() {
        assertThat(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.TRANSPOSER_ENERGY_CAPACITY)).isEqualTo(20_000L);
        assertThat(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.TRANSPOSER_MAX_ENERGY_INPUT)).isEqualTo(128L);
        assertThat(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.TRANSPOSER_TANK_CAPACITY_MB)).isEqualTo(16_000L);
    }

    /**
     * Wiki claim (Power): "A bucket fill/empty costs 800 RF, drawn at 20 RF/tick (about 2 seconds)."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Transposer#Power">wiki/Transposer.txt § Power</a>
     */
    @Test
    @DisplayName("drain rate produces the wiki's ~2-second bucket fill/empty time")
    void drainRateMatchesWikiBucketTiming() {
        long energyPerTick = LogisticsConfigHost.get(LogisticsAutomation.CONFIG.TRANSPOSER_ENERGY_PER_TICK);
        assertThat(energyPerTick).isEqualTo(20L);

        long bucketCost = 800L;
        long ticksToComplete = bucketCost / energyPerTick;
        assertThat(ticksToComplete).isEqualTo(40L); // 40 ticks = 2 seconds, matching "about 2 seconds"
    }
}
