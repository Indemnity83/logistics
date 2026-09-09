package com.logistics.automation.transposer;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsConfigHost;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Locks in the Transposer's documented power/tank numbers (see wiki/Transposer.txt § Usage, §
 * Power) as fast, engine-free regression guards.
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
    void drainRateMatchesWikiBucketTiming() throws IOException {
        // Both halves of the claim are read from where the game reads them: the drain rate from
        // config, the job cost from the shipped recipe. Retuning either one now fails this test.
        long energyPerTick = LogisticsConfigHost.get(LogisticsAutomation.CONFIG.TRANSPOSER_ENERGY_PER_TICK);
        long bucketCost = bucketRecipeEnergy("data/logistics/recipe/transposer/fill_water_bucket.json");

        long ticksToComplete = bucketCost / energyPerTick;
        assertThat(ticksToComplete).isEqualTo(40L); // 40 ticks = 2 seconds, matching "about 2 seconds"

        // Emptying is documented as costing the same, so it must take the same time.
        long emptyCost = bucketRecipeEnergy("data/logistics/recipe/transposer/empty_water_bucket.json");
        assertThat(emptyCost / energyPerTick).isEqualTo(40L);
    }

    private static long bucketRecipeEnergy(String path) throws IOException {
        try (InputStream stream = TransposerConfigTest.class.getClassLoader().getResourceAsStream(path)) {
            assertThat(stream).as("recipe resource on the test classpath: %s", path).isNotNull();
            JsonObject recipe =
                    JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            return recipe.get("energy").getAsLong();
        }
    }
}
