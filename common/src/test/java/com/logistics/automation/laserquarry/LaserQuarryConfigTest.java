package com.logistics.automation.laserquarry;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsConfigHost;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Locks in the Laser Quarry's documented default mining area (see wiki/Laser Quarry.txt § Mining
 * area) as a fast, engine-free regression guard.
 */
@DisplayName("Laser Quarry config")
class LaserQuarryConfigTest {

    /**
     * Wiki claim (Mining area): "Default (no markers): mines a 16×16 area centered on the quarry's
     * placement."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Laser_Quarry#Mining_area">wiki/Laser Quarry.txt § Mining area</a>
     */
    @Test
    @DisplayName("default mining area config matches the wiki's 16x16 claim")
    void defaultAreaMatchesWiki() {
        assertThat(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.QUARRY_AREA)).isEqualTo(16);
    }
}
