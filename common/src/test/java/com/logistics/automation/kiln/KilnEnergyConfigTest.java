package com.logistics.automation.kiln;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.LogisticsAutomation;
import com.logistics.LogisticsConfigHost;
import com.logistics.core.machine.component.RecipeProcessPlan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Locks in the Kiln's documented power numbers (see wiki/Kiln.txt § Power) as fast, engine-free
 * regression guards. Wiring/live-behavior assertions for the same numbers live in
 * {@code fabric/src/gametest/.../KilnGameTest.testKilnSmeltsWithEnergy}; this class is the pure-math
 * counterpart that needs no {@code Level}/GameTest to run.
 */
@DisplayName("Kiln energy config and smelt math")
class KilnEnergyConfigTest {

    /**
     * Wiki claim (Power): "It holds 10,000 RF and accepts up to 128 RF/tick."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Kiln#Power">wiki/Kiln.txt § Power</a>
     */
    @Test
    @DisplayName("capacity and insert-rate config defaults match the wiki")
    void capacityAndInsertRateMatchWiki() {
        assertThat(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.KILN_ENERGY_CAPACITY)).isEqualTo(10_000L);
        assertThat(LogisticsConfigHost.get(LogisticsAutomation.CONFIG.KILN_MAX_ENERGY_INPUT)).isEqualTo(128L);
    }

    /**
     * NOTE: wiki/Kiln.txt § Power claims "about 4,000 RF for a standard 10-second smelt (20
     * RF/tick)". The code computes cookingTime(200) * KILN_RF_PER_COOK_TICK(10) = 2,000 RF, drained
     * at KILN_ENERGY_PER_TICK(20) RF/t = 100 ticks (5s) — half the wiki's claimed cost and time. See
     * WIKI_DISCREPANCIES.md § Kiln for the tracked mismatch; this test asserts the code's actual
     * behavior, not the wiki's.
     */
    @Test
    @DisplayName("drain rate and cook-tick cost config defaults produce 2,000 RF / 100 ticks for a standard smelt")
    void standardSmeltCostsTwoThousandRfOverOneHundredTicks() {
        long energyPerTick = LogisticsConfigHost.get(LogisticsAutomation.CONFIG.KILN_ENERGY_PER_TICK);
        long rfPerCookTick = LogisticsConfigHost.get(LogisticsAutomation.CONFIG.KILN_RF_PER_COOK_TICK);
        assertThat(energyPerTick).isEqualTo(20L);
        assertThat(rfPerCookTick).isEqualTo(10L);

        long standardCookingTime = 200; // vanilla furnace recipe default (10s)
        long energyRequired = standardCookingTime * rfPerCookTick;
        assertThat(energyRequired).isEqualTo(2_000L);

        RecipeProcessPlan.Result result = null;
        int ticks = 0;
        while (result == null || !result.complete()) {
            long spentSoFar = result == null ? 0 : result.energySpent();
            result = RecipeProcessPlan.advance(spentSoFar, energyRequired, energyPerTick, energyPerTick, true);
            ticks++;
        }

        assertThat(ticks).isEqualTo(100);
        assertThat(result.energySpent()).isEqualTo(2_000L);
    }
}
