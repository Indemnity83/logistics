package com.logistics.automation.refinery;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsConfigHost;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Locks in the Refinery's documented power/tank numbers (see wiki/Refinery.txt § Power) as fast,
 * engine-free regression guards.
 */
@DisplayName("Refinery config")
class RefineryConfigTest {

    /**
     * Wiki claim (Power): "It holds 20,000 RF and accepts up to 128 RF/t, spending 20 RF/t while
     * working... Its input tank holds 4,000 mB and its output tank 10,000 mB."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Refinery#Power">wiki/Refinery.txt § Power</a>
     */
    @Test
    @DisplayName("power and tank config defaults match the wiki")
    void powerAndTankConfigMatchWiki() {
        assertThat(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.REFINERY_ENERGY_CAPACITY)).isEqualTo(20_000L);
        assertThat(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.REFINERY_MAX_ENERGY_INPUT)).isEqualTo(128L);
        assertThat(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.REFINERY_ENERGY_PER_TICK)).isEqualTo(20L);
        assertThat(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.REFINERY_INPUT_TANK_MB)).isEqualTo(4_000L);
        assertThat(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.REFINERY_OUTPUT_TANK_MB)).isEqualTo(10_000L);
    }
}
