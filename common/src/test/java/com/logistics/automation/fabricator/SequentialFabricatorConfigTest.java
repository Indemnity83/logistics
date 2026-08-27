package com.logistics.automation.fabricator;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsConfigHost;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Locks in the Sequential Fabricator's documented power numbers (see wiki/Sequential
 * Fabricator.txt § Power) as fast, engine-free regression guards.
 */
@DisplayName("Sequential Fabricator config")
class SequentialFabricatorConfigTest {

    /**
     * Wiki claim (Power): "It holds 100,000 RF and accepts up to 128 RF/t, spending 80 RF/t while
     * working."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Sequential_Fabricator#Power">wiki/Sequential Fabricator.txt § Power</a>
     */
    @Test
    @DisplayName("power config defaults match the wiki")
    void powerConfigMatchesWiki() {
        assertThat(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.FABRICATOR_ENERGY_CAPACITY)).isEqualTo(100_000L);
        assertThat(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.FABRICATOR_MAX_ENERGY_INPUT)).isEqualTo(128L);
        assertThat(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.FABRICATOR_ENERGY_PER_TICK)).isEqualTo(80L);
    }
}
