package com.logistics.automation.laserquarry.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.indemnity83.configory.Config;
import com.indemnity83.configory.ConfigRegistry;
import com.logistics.LogisticsAutomation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("QuarryEnergy")
class QuarryEnergyTest {

    // The registry is shared with every other class in the fork, so put back what was there
    // rather than the literals that happen to be today's defaults.
    private Config machines;
    private long originalEnergyPerBlock;
    private double originalMultiplier;

    @BeforeEach
    void captureConfig() {
        machines = ConfigRegistry.config(LogisticsAutomation.CONFIG.QUARRY_ENERGY_PER_BLOCK.configId());
        originalEnergyPerBlock = machines.get(LogisticsAutomation.CONFIG.QUARRY_ENERGY_PER_BLOCK);
        originalMultiplier = machines.get(LogisticsAutomation.CONFIG.QUARRY_ENERGY_MULTIPLIER);
    }

    @AfterEach
    void restoreConfig() {
        machines.set(LogisticsAutomation.CONFIG.QUARRY_ENERGY_PER_BLOCK, originalEnergyPerBlock);
        machines.set(LogisticsAutomation.CONFIG.QUARRY_ENERGY_MULTIPLIER, originalMultiplier);
    }

    @Test
    @DisplayName("derives energy values from the quarry config keys")
    void derivesFromConfig() {
        machines.set(LogisticsAutomation.CONFIG.QUARRY_ENERGY_PER_BLOCK, 60L);
        machines.set(LogisticsAutomation.CONFIG.QUARRY_ENERGY_MULTIPLIER, 2.0);

        assertThat(QuarryEnergy.energyPerBlockMultiplier()).isEqualTo(60 * 2.0 * 2); // 240
        assertThat(QuarryEnergy.energyCapacity()).isEqualTo((long) (128.0 * 60 * 2.0)); // 15360
        assertThat(QuarryEnergy.maxEnergyInput()).isEqualTo((long) (1_000L * 2.0)); // 2000
    }

    @Test
    @DisplayName("clamps to non-negative even at the zero boundary")
    void clampsAtZero() {
        machines.set(LogisticsAutomation.CONFIG.QUARRY_ENERGY_MULTIPLIER, 0.0);

        assertThat(QuarryEnergy.energyPerBlockMultiplier()).isZero();
        assertThat(QuarryEnergy.energyCapacity()).isZero();
        assertThat(QuarryEnergy.maxEnergyInput()).isZero();
    }
}
