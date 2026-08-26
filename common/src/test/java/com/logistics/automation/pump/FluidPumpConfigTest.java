package com.logistics.automation.pump;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.LogisticsConfigHost;
import com.logistics.LogisticsPipe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Locks in the Pump's documented power/throughput numbers (see wiki/Pump.txt § Usage, § Power) as
 * fast, engine-free regression guards. Live behavior for the same numbers is exercised in
 * {@code fabric/src/gametest/.../FluidPumpGameTest}.
 */
@DisplayName("Fluid Pump config")
class FluidPumpConfigTest {

    /**
     * Wiki claim (Usage): "Holds a 16,000 mB (16 buckets) internal buffer" / "Consumes 100 RF per
     * source block drained."
     *
     * @see <a href="https://logistics.fandom.com/wiki/Pump#Usage">wiki/Pump.txt § Usage</a>
     */
    @Test
    @DisplayName("tank capacity and energy-per-source config defaults match the wiki")
    void tankCapacityAndEnergyPerSourceMatchWiki() {
        assertThat(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_TANK_CAPACITY_MB)).isEqualTo(16_000);
        assertThat(LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_ENERGY_PER_SOURCE)).isEqualTo(100L);
    }

    /**
     * NOTE: wiki/Pump.txt § Usage claims "Outputs up to 62.5 mB/t into an adjacent tank or fluid
     * pipe." The code's push-rate constant (FLUID_PUMP_PUSH_RATE_MB) is 400 — not 62.5. The wiki's
     * figure is exactly {@code 1000 mB (one bucket) / FLUID_PUMP_INTERVAL_TICKS}, i.e. the
     * *sustained average intake rate* the pump's own drain step is bottlenecked to, mislabeled here
     * as the push/output rate. Both numbers are real config values; the wiki just names the wrong
     * one "output." See WIKI_DISCREPANCIES.md § Pump.
     */
    @Test
    @DisplayName("push rate (400 mB/t) is not the wiki's 62.5 mB/t figure, which is the intake interval's average instead")
    void pushRateDiffersFromWikiFigureWhichIsActuallyTheIntakeAverage() {
        int pushRateMb = LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_PUSH_RATE_MB);
        int intervalTicks = LogisticsConfigHost.get(LogisticsPipe.CONFIG.FLUID_PUMP_INTERVAL_TICKS);

        assertThat(pushRateMb).isEqualTo(400);
        assertThat(intervalTicks).isEqualTo(16);

        double averageIntakeRate = 1_000.0 / intervalTicks;
        assertThat(averageIntakeRate).isEqualTo(62.5);
        assertThat((double) pushRateMb).isNotEqualTo(averageIntakeRate);
    }
}
