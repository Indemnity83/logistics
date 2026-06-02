package com.logistics.core.lib.power;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EngineHeatModelTest {
    @Test
    void temperature_scalesBetweenFloorAndMaximumByEnergyLevel() {
        double temperature = EngineHeatModel.temperature(500, 1_000, 20, 250);

        assertThat(temperature).isEqualTo(135.0);
    }

    @Test
    void energyLevel_returnsZeroWhenCapacityIsInvalid() {
        assertThat(EngineHeatModel.energyLevel(500, 0)).isZero();
    }

    @Test
    void heatLevel_returnsZeroWhenMaximumTemperatureIsInvalid() {
        assertThat(EngineHeatModel.heatLevel(100, 0)).isZero();
    }

    @Test
    void stage_usesHeatRatioThresholds() {
        assertThat(EngineHeatModel.stage(20, 250, true, false)).isEqualTo(AbstractEngineBlockEntity.HeatStage.COLD);
        assertThat(EngineHeatModel.stage(80, 250, true, false)).isEqualTo(AbstractEngineBlockEntity.HeatStage.COOL);
        assertThat(EngineHeatModel.stage(140, 250, true, false)).isEqualTo(AbstractEngineBlockEntity.HeatStage.WARM);
        assertThat(EngineHeatModel.stage(200, 250, true, false)).isEqualTo(AbstractEngineBlockEntity.HeatStage.HOT);
    }

    @Test
    void stage_overheatsOnlyWhenEnabled() {
        assertThat(EngineHeatModel.stage(250, 250, true, false)).isEqualTo(AbstractEngineBlockEntity.HeatStage.OVERHEAT);
        assertThat(EngineHeatModel.stage(250, 250, false, false)).isEqualTo(AbstractEngineBlockEntity.HeatStage.HOT);
    }

    @Test
    void stage_flashesWarmForNonOverheatingCompression() {
        assertThat(EngineHeatModel.stage(250, 250, false, true)).isEqualTo(AbstractEngineBlockEntity.HeatStage.WARM);
    }

    @Test
    void pistonSpeed_matchesHeatBands() {
        assertThat(EngineHeatModel.pistonSpeed(20, 250, true)).isEqualTo(0.01f);
        assertThat(EngineHeatModel.pistonSpeed(80, 250, true)).isEqualTo(0.02f);
        assertThat(EngineHeatModel.pistonSpeed(140, 250, true)).isEqualTo(0.04f);
        assertThat(EngineHeatModel.pistonSpeed(200, 250, true)).isEqualTo(0.08f);
    }

    @Test
    void pistonSpeed_stopsWhenOverheatedButNotWhenOverheatDisabled() {
        assertThat(EngineHeatModel.pistonSpeed(250, 250, true)).isZero();
        assertThat(EngineHeatModel.pistonSpeed(250, 250, false)).isEqualTo(0.08f);
    }
}
