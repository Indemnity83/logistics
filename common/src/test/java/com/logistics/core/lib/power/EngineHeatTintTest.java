package com.logistics.core.lib.power;

import static org.assertj.core.api.Assertions.assertThat;

import com.logistics.core.lib.power.AbstractEngineBlockEntity.HeatStage;
import com.logistics.test.MinecraftTestEnvironment;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class EngineHeatTintTest extends MinecraftTestEnvironment {

    /**
     * In 26.1 block tint is baked per "model group", and the renderer only re-bakes a fresh tint when
     * a block-state property that the tint source declares as relevant changes (see
     * {@code ModelGroupCollector} / {@code BlockColors.getColoringProperties}). If the engine tint
     * source does not declare {@link AbstractEngineBlockEntity#STAGE} as relevant, every heat stage
     * collapses into one cached tint and the heat color never visibly changes in-world.
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
