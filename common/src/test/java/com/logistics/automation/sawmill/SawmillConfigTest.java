package com.logistics.automation.sawmill;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsConfigHost;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Locks in the Sawmill's documented power numbers (see wiki/Sawmill.txt § Power) as fast,
 * engine-free regression guards.
 */
@DisplayName("Sawmill config")
class SawmillConfigTest {

    /**
     * Wiki claim (Power): "It holds 10,000 RF and accepts up to 128 RF/t."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Sawmill#Power">wiki/Sawmill.txt § Power</a>
     */
    @Test
    @DisplayName("capacity and insert-rate config defaults match the wiki")
    void capacityAndInsertRateMatchWiki() {
        assertThat(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.SAWMILL_ENERGY_CAPACITY)).isEqualTo(10_000L);
        assertThat(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.SAWMILL_MAX_ENERGY_INPUT)).isEqualTo(128L);
    }
}
