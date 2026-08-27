package com.logistics.automation.macerator;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsConfigHost;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Locks in the Macerator's documented power numbers (see wiki/Macerator.txt § Power) as fast,
 * engine-free regression guards.
 */
@DisplayName("Macerator config")
class MaceratorConfigTest {

    /**
     * Wiki claim (Power): "It holds 10,000 RF, accepts up to 128 RF/tick, and consumes 10 RF/tick
     * while active."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Macerator#Power">wiki/Macerator.txt § Power</a>
     */
    @Test
    @DisplayName("capacity, insert-rate, and drain-rate config defaults match the wiki")
    void powerConfigMatchesWiki() {
        assertThat(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.MACERATOR_ENERGY_CAPACITY)).isEqualTo(10_000L);
        assertThat(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.MACERATOR_MAX_ENERGY_INPUT)).isEqualTo(128L);
        assertThat(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.MACERATOR_ENERGY_PER_TICK)).isEqualTo(10L);
    }
}
