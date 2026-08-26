package com.logistics.automation.crucible;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsConfigHost;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Locks in the Crucible's documented power/tank numbers (see wiki/Crucible.txt § Power, § Usage) as
 * fast, engine-free regression guards. All four match the wiki exactly — no discrepancy found here.
 */
@DisplayName("Crucible config")
class CrucibleConfigTest {

    /**
     * Wiki claim (Power): "It holds 40,000 RF and accepts up to 128 RF/t, spending 40 RF/t while
     * melting." (Usage): "...its 10,000 mB output-only tank."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Crucible#Power">wiki/Crucible.txt § Power</a>
     */
    @Test
    @DisplayName("power and tank config defaults match the wiki")
    void powerAndTankConfigMatchWiki() {
        assertThat(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.CRUCIBLE_ENERGY_CAPACITY)).isEqualTo(40_000L);
        assertThat(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.CRUCIBLE_MAX_ENERGY_INPUT)).isEqualTo(128L);
        assertThat(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.CRUCIBLE_ENERGY_PER_TICK)).isEqualTo(40L);
        assertThat(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.CRUCIBLE_TANK_CAPACITY_MB)).isEqualTo(10_000L);
    }
}
