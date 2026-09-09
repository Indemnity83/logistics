package com.logistics.pipe.modules;

import com.indemnity83.configory.Config;
import com.indemnity83.configory.ConfigRegistry;
import com.logistics.LogisticsConfigHost;
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

    private BoostModule module;
    private FakePipeAccess access;
    private PipeContext ctx;

    @BeforeEach
    void setUp() {
        module = new BoostModule();
        access = new FakePipeAccess();
        ctx = new PipeContext(null, BlockPos.ZERO, null, access);
    }

    private static float configuredAcceleration() {
        return LogisticsConfigHost.get(LogisticsPipe.CONFIG.PIPE_ACCELERATION);
    }

    // ==================== Acceleration ====================

    @Test
    @DisplayName("getAcceleration returns 0 when pipe is not powered")
    void getAcceleration_unpowered_returnsZero() {
        access.setPowered(false);
        assertThat(module.getAcceleration(ctx)).isEqualTo(0f);
    }

    @Test
    @DisplayName("getAcceleration returns pipe.acceleration from config when pipe is powered")
    void getAcceleration_powered_returnsRate() {
        access.setPowered(true);
        assertThat(module.getAcceleration(ctx)).isEqualTo(configuredAcceleration());
    }

    @Test
    @DisplayName("getAcceleration is powered-dependent: switching power changes acceleration")
    void getAcceleration_switchingPower_changesValue() {
        access.setPowered(false);
        float unpowered = module.getAcceleration(ctx);

        access.setPowered(true);
        float powered = module.getAcceleration(ctx);

        assertThat(unpowered).isEqualTo(0f);
        assertThat(powered).isEqualTo(configuredAcceleration());
    }

    @Test
    @DisplayName("getAcceleration tracks pipe.acceleration changed after the module was constructed")
    void getAcceleration_tracksConfigReload() {
        access.setPowered(true);

        Config pipes = ConfigRegistry.config(LogisticsPipe.CONFIG.PIPE_ACCELERATION.configId());
        float original = configuredAcceleration();
        try {
            assertThat(pipes.trySet(LogisticsPipe.CONFIG.PIPE_ACCELERATION, 0.01f)).isTrue();

            assertThat(module.getAcceleration(ctx)).isEqualTo(0.01f);
        } finally {
            pipes.set(LogisticsPipe.CONFIG.PIPE_ACCELERATION, original);
        }
    }

    // ==================== Max speed ====================

    @Test
    @DisplayName("getMaxSpeed returns PIPE_MAX_SPEED * 4")
    void getMaxSpeed_returnsFourTimesBaseSpeed() {
        float expected = LogisticsConfigHost.get(LogisticsPipe.CONFIG.PIPE_MAX_SPEED) * 4.0f;
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
    @DisplayName("getMaxSpeed tracks pipe.max_speed changed after the module was constructed")
    void getMaxSpeed_tracksConfigReload() {
        Config pipes = ConfigRegistry.config(LogisticsPipe.CONFIG.PIPE_MAX_SPEED.configId());
        float original = LogisticsConfigHost.get(LogisticsPipe.CONFIG.PIPE_MAX_SPEED);
        try {
            assertThat(pipes.trySet(LogisticsPipe.CONFIG.PIPE_MAX_SPEED, 0.32f)).isTrue();

            assertThat(module.getMaxSpeed(ctx)).isEqualTo(0.32f * 4.0f);
        } finally {
            pipes.set(LogisticsPipe.CONFIG.PIPE_MAX_SPEED, original);
        }
    }
}
