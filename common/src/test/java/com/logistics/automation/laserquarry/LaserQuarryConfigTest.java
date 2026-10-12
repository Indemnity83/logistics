package com.logistics.automation.laserquarry;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsConfigHost;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Locks in the Laser Quarry's documented config defaults (see wiki/Laser Quarry.txt § Mining area
 * and § Chunk loading) as a fast, engine-free regression guard.
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

    /**
     * Wiki claim (Chunk loading): "{{Config|...|load_chunks|Keep the quarry and its work area
     * chunk-loaded while running.|false|Boolean}}" — chunk loading is opt-in.
     *
     * @see <a href="https://logistics.fandom.com/wiki/Laser_Quarry#Chunk_loading">wiki/Laser Quarry.txt § Chunk loading</a>
     */
    @Test
    @DisplayName("chunk loading is off by default, as the wiki documents")
    void loadChunksDefaultsOff() {
        assertThat(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.QUARRY_LOAD_CHUNKS)).isFalse();
    }
}
