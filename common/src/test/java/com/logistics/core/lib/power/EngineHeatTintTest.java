package com.logistics.core.lib.power;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.test.MinecraftTestEnvironment;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class EngineHeatTintTest extends MinecraftTestEnvironment {

    /**
     * On 1.21.1 the heat tint is rendered in the block-entity renderer (the engine core is drawn and
     * tinted per-frame from the live {@link HeatStage#STAGE}), so there is no
     * model-group / tint-cache caveat. {@link EngineHeatTint#RELEVANT_PROPERTIES} is retained for
     * parity with the other branches and must still include STAGE.
     */
    @Test
    void relevantProperties_includeHeatStage() {
        assertThat(EngineHeatTint.RELEVANT_PROPERTIES).contains(HeatStage.STAGE);
    }

    @Test
    void color_isDistinctForEveryHeatStage() {
        long distinctColors =
                Arrays.stream(HeatStage.values()).mapToInt(EngineHeatTint::color).distinct().count();

        assertThat(distinctColors).isEqualTo(HeatStage.values().length);
    }

    @Test
    void color_isFullyOpaque() {
        for (HeatStage stage : HeatStage.values()) {
            int alpha = (EngineHeatTint.color(stage) >>> 24) & 0xFF;
            assertThat(alpha).as("alpha for %s", stage).isEqualTo(0xFF);
        }
    }
}
