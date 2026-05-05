package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.test.FakePipeAccess;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("WeatheringModule")
class WeatheringModuleTest {

    private WeatheringModule module;
    private FakePipeAccess access;
    private PipeContext ctx;

    @BeforeEach
    void setUp() {
        module = new WeatheringModule();
        access = new FakePipeAccess();
        ctx = new PipeContext(null, BlockPos.ZERO, null, access);
    }

    // ==================== Stage constants ====================

    @Test
    @DisplayName("STAGE_UNAFFECTED is 0")
    void stageConstants_unaffectedIsZero() {
        assertThat(WeatheringModule.STAGE_UNAFFECTED).isEqualTo(0);
    }

    @Test
    @DisplayName("STAGE_EXPOSED is 1")
    void stageConstants_exposedIsOne() {
        assertThat(WeatheringModule.STAGE_EXPOSED).isEqualTo(1);
    }

    @Test
    @DisplayName("STAGE_WEATHERED is 2")
    void stageConstants_weatheredIsTwo() {
        assertThat(WeatheringModule.STAGE_WEATHERED).isEqualTo(2);
    }

    @Test
    @DisplayName("STAGE_OXIDIZED is 3")
    void stageConstants_oxidizedIsThree() {
        assertThat(WeatheringModule.STAGE_OXIDIZED).isEqualTo(3);
    }

    @Test
    @DisplayName("stage constants are ordered: UNAFFECTED < EXPOSED < WEATHERED < OXIDIZED")
    void stageConstants_areOrdered() {
        assertThat(WeatheringModule.STAGE_UNAFFECTED)
                .isLessThan(WeatheringModule.STAGE_EXPOSED)
                .isLessThan(WeatheringModule.STAGE_WEATHERED)
                .isLessThan(WeatheringModule.STAGE_OXIDIZED);
    }

    // ==================== Default state ====================

    @Test
    @DisplayName("getOxidationStage defaults to STAGE_UNAFFECTED (0)")
    void getOxidationStage_defaultsToUnaffected() {
        assertThat(module.getOxidationStage(ctx)).isEqualTo(WeatheringModule.STAGE_UNAFFECTED);
    }

    @Test
    @DisplayName("isWaxed defaults to false")
    void isWaxed_defaultsFalse() {
        assertThat(module.isWaxed(ctx)).isFalse();
    }

    // ==================== Stage NBT round-trip ====================

    @ParameterizedTest(name = "stage {0}")
    @DisplayName("getOxidationStage reads stage stored directly in NBT")
    @ValueSource(ints = {0, 1, 2, 3})
    void getOxidationStage_readsFromNbt(int stage) {
        ctx.saveInt(module, "oxidation_stage", stage);
        assertThat(module.getOxidationStage(ctx)).isEqualTo(stage);
    }

    @Test
    @DisplayName("isWaxed returns true when NBT has waxed=1")
    void isWaxed_trueWhenNbtIsOne() {
        ctx.saveInt(module, "waxed", 1);
        assertThat(module.isWaxed(ctx)).isTrue();
    }

    @Test
    @DisplayName("isWaxed returns false when NBT has waxed=0")
    void isWaxed_falseWhenNbtIsZero() {
        ctx.saveInt(module, "waxed", 0);
        assertThat(module.isWaxed(ctx)).isFalse();
    }

    // ==================== Waxed + stage combination ====================

    @Test
    @DisplayName("stage and waxed state are independent in NBT")
    void stageAndWaxed_areIndependent() {
        ctx.saveInt(module, "oxidation_stage", WeatheringModule.STAGE_WEATHERED);
        ctx.saveInt(module, "waxed", 1);

        assertThat(module.getOxidationStage(ctx)).isEqualTo(WeatheringModule.STAGE_WEATHERED);
        assertThat(module.isWaxed(ctx)).isTrue();
    }
}
