package com.logistics.core.lib.power;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.lib.power.AbstractEngineBlockEntity.HeatStage;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class EngineHeatTintTest extends MinecraftTestEnvironment {

    /**
     * The heat tint is rendered in the block-entity renderer (the engine core is drawn and tinted
     * per-frame from the live {@link AbstractEngineBlockEntity#STAGE}), so there is no model-group /
     * tint-cache caveat. {@link EngineHeatTint#RELEVANT_PROPERTIES} is retained for parity with the
     * other branches and must still include STAGE.
     */
    @Test
    void relevantProperties_includeHeatStage() {
        assertThat(EngineHeatTint.RELEVANT_PROPERTIES).contains(AbstractEngineBlockEntity.STAGE);
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
