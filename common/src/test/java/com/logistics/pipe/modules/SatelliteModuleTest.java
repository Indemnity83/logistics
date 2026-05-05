package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.test.FakePipeAccess;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("SatelliteModule")
class SatelliteModuleTest {

    private SatelliteModule module;
    private FakePipeAccess access;
    private PipeContext ctx;

    @BeforeEach
    void setUp() {
        module = new SatelliteModule();
        access = new FakePipeAccess();
        ctx = new PipeContext(null, BlockPos.ZERO, null, access);
    }

    // ==================== Satellite ID — defaults ====================

    @Test
    @DisplayName("getSatelliteId returns empty string by default")
    void getSatelliteId_emptyByDefault() {
        assertThat(module.getSatelliteId(ctx)).isEmpty();
    }

    @Test
    @DisplayName("getSatelliteIdInt returns 0 when no ID is set")
    void getSatelliteIdInt_zeroByDefault() {
        assertThat(module.getSatelliteIdInt(ctx)).isEqualTo(0);
    }

    // ==================== setSatelliteId(String) ====================

    @Test
    @DisplayName("setSatelliteId(String) persists and getSatelliteId reads it back")
    void setSatelliteIdString_persistsAndReadsBack() {
        module.setSatelliteId(ctx, "furnace_input");
        assertThat(module.getSatelliteId(ctx)).isEqualTo("furnace_input");
    }

    @Test
    @DisplayName("setSatelliteId(String) trims surrounding whitespace")
    void setSatelliteIdString_trimsWhitespace() {
        module.setSatelliteId(ctx, "  my_satellite  ");
        assertThat(module.getSatelliteId(ctx)).isEqualTo("my_satellite");
    }

    @Test
    @DisplayName("setSatelliteId(String null) stores empty string")
    void setSatelliteIdString_null_storesEmpty() {
        module.setSatelliteId(ctx, "something");
        module.setSatelliteId(ctx, (String) null);
        assertThat(module.getSatelliteId(ctx)).isEmpty();
    }

    @Test
    @DisplayName("setSatelliteId(empty string) clears the ID")
    void setSatelliteIdString_emptyString_clearsId() {
        module.setSatelliteId(ctx, "test");
        module.setSatelliteId(ctx, "");
        assertThat(module.getSatelliteId(ctx)).isEmpty();
    }

    // ==================== setSatelliteId(int) ====================

    @Test
    @DisplayName("setSatelliteId(int) persists positive value and getSatelliteIdInt reads it back")
    void setSatelliteIdInt_positiveValue_persistsAndReadsBack() {
        module.setSatelliteId(ctx, 42);
        assertThat(module.getSatelliteIdInt(ctx)).isEqualTo(42);
    }

    @Test
    @DisplayName("setSatelliteId(int) zero clears the ID")
    void setSatelliteIdInt_zeroClears() {
        module.setSatelliteId(ctx, 5);
        module.setSatelliteId(ctx, 0);
        assertThat(module.getSatelliteId(ctx)).isEmpty();
        assertThat(module.getSatelliteIdInt(ctx)).isEqualTo(0);
    }

    @Test
    @DisplayName("getSatelliteId returns the string form of the integer ID")
    void getSatelliteId_returnsStringFormOfIntId() {
        module.setSatelliteId(ctx, 7);
        assertThat(module.getSatelliteId(ctx)).isEqualTo("7");
    }

    @Test
    @DisplayName("getSatelliteIdInt returns 0 when stored value is not a number")
    void getSatelliteIdInt_nonNumericString_returnsZero() {
        module.setSatelliteId(ctx, "furnace_input"); // non-numeric
        assertThat(module.getSatelliteIdInt(ctx)).isEqualTo(0);
    }

    // ==================== Tick counter ====================

    @Test
    @DisplayName("onTick increments counter when below SYNC_INTERVAL (20)")
    void onTick_incrementsCounter_belowInterval() {
        for (int i = 0; i < 5; i++) {
            module.onTick(ctx);
        }

        int ticks = ctx.getInt(module, "sync_ticks", 0);
        assertThat(ticks).isEqualTo(5);
    }

    @Test
    @DisplayName("onTick counter reaches 19 before sync trigger")
    void onTick_counterReaches19BeforeTrigger() {
        for (int i = 0; i < 19; i++) {
            module.onTick(ctx);
        }

        int ticks = ctx.getInt(module, "sync_ticks", 0);
        assertThat(ticks).isEqualTo(19);
    }

    @Test
    @DisplayName("onTick resets counter at tick 20 when network is null")
    void onTick_resetsCounterAtInterval_nullNetwork() {
        // tick 20 tries to register satellite but network is null — safe no-op, counter resets
        for (int i = 0; i < 20; i++) {
            module.onTick(ctx);
        }

        int ticks = ctx.getInt(module, "sync_ticks", 0);
        assertThat(ticks).isEqualTo(0);
    }

    // ==================== KEY_ID constant ====================

    @Test
    @DisplayName("KEY_ID constant is 'satellite_id'")
    void keyIdConstant_value() {
        assertThat(SatelliteModule.KEY_ID).isEqualTo("satellite_id");
    }
}
