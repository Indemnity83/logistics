package com.logistics.automation.alloysmelter;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsConfigHost;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Locks in the Alloy Smelter's documented power numbers (see wiki/Alloy Smelter.txt § Power) as
 * fast, engine-free regression guards.
 */
@DisplayName("Alloy Smelter config")
class AlloySmelterConfigTest {

    /**
     * Wiki claim (Power): "It holds 10,000 RF and accepts up to 128 RF/t, spending 20 RF/t while
     * working."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Alloy_Smelter#Power">wiki/Alloy Smelter.txt § Power</a>
     */
    @Test
    @DisplayName("power config defaults match the wiki")
    void powerConfigMatchesWiki() {
        assertThat(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.ALLOY_SMELTER_ENERGY_CAPACITY)).isEqualTo(10_000L);
        assertThat(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.ALLOY_SMELTER_MAX_ENERGY_INPUT)).isEqualTo(128L);
        assertThat(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.ALLOY_SMELTER_ENERGY_PER_TICK)).isEqualTo(20L);
    }
}
