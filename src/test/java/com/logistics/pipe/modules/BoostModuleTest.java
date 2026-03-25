package com.logistics.pipe.modules;

import com.logistics.LogisticsPipe;
import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.test.FakePipeAccess;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("BoostModule")
class BoostModuleTest {

    private static final float ACCELERATION = 0.05f;

    private BoostModule module;
    private FakePipeAccess access;
    private PipeContext ctx;

    @BeforeEach
    void setUp() {
        module = new BoostModule(ACCELERATION);
        access = new FakePipeAccess();
        ctx = new PipeContext(null, BlockPos.ZERO, null, access);
    }

    // ==================== Acceleration ====================

    @Test
    @DisplayName("getAcceleration returns 0 when pipe is not powered")
    void getAcceleration_unpowered_returnsZero() {
        access.setPowered(false);
        assertThat(module.getAcceleration(ctx)).isEqualTo(0f);
    }

    @Test
    @DisplayName("getAcceleration returns accelerationRate when pipe is powered")
    void getAcceleration_powered_returnsRate() {
        access.setPowered(true);
        assertThat(module.getAcceleration(ctx)).isEqualTo(ACCELERATION);
    }

    @Test
    @DisplayName("getAcceleration is powered-dependent: switching power changes acceleration")
    void getAcceleration_switchingPower_changesValue() {
        access.setPowered(false);
        float unpowered = module.getAcceleration(ctx);

        access.setPowered(true);
        float powered = module.getAcceleration(ctx);

        assertThat(unpowered).isEqualTo(0f);
        assertThat(powered).isEqualTo(ACCELERATION);
    }

    // ==================== Max speed ====================

    @Test
    @DisplayName("getMaxSpeed returns PIPE_MAX_SPEED * 4")
    void getMaxSpeed_returnsFourTimesBaseSpeed() {
        float expected = LogisticsPipe.CONFIG.PIPE_MAX_SPEED * 4.0f;
        assertThat(module.getMaxSpeed(ctx)).isEqualTo(expected);
    }

    @Test
    @DisplayName("getMaxSpeed is not affected by powered state")
    void getMaxSpeed_notAffectedByPower() {
        access.setPowered(false);
        float unpowered = module.getMaxSpeed(ctx);

        access.setPowered(true);
        float powered = module.getMaxSpeed(ctx);

        assertThat(unpowered).isEqualTo(powered);
    }

    @Test
    @DisplayName("getMaxSpeed is the same across different BoostModule instances with different accelerationRates")
    void getMaxSpeed_sameForDifferentAccelerationRates() {
        BoostModule other = new BoostModule(0.02f);
        assertThat(module.getMaxSpeed(ctx)).isEqualTo(other.getMaxSpeed(ctx));
    }
}
