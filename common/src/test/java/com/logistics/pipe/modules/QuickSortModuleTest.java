package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.test.FakePipeAccess;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("QuickSortModule")
class QuickSortModuleTest {

    private QuickSortModule module;
    private FakePipeAccess access;
    private PipeContext ctx;

    @BeforeEach
    void setUp() {
        module = new QuickSortModule();
        access = new FakePipeAccess();
        ctx = new PipeContext(null, BlockPos.ZERO, null, access);
    }

    // ==================== Tick counter — normal delay (6) ====================

    @Test
    @DisplayName("onTick increments counter when below normal delay (6)")
    void onTick_incrementsCounter_belowNormalDelay() {
        for (int i = 0; i < 5; i++) {
            module.onTick(ctx);
        }

        int ticks = ctx.getInt(module, "ticks_since_action", 0);
        assertThat(ticks).isEqualTo(5);
    }

    @Test
    @DisplayName("onTick at tick 6 resets counter to 0 when no inventory is connected")
    void onTick_resetsCounterAtNormalDelay_noInventory() {
        // 6 ticks reaches NORMAL_DELAY; no inventory direction → resets and returns early
        for (int i = 0; i < 6; i++) {
            module.onTick(ctx);
        }

        int ticks = ctx.getInt(module, "ticks_since_action", 0);
        assertThat(ticks).isEqualTo(0);
    }

    // ==================== Stall delay (24) ====================

    @Test
    @DisplayName("onTick uses stall delay (24) when stalled flag is set")
    void onTick_usesStallDelay_whenStalled() {
        // Force stalled state
        ctx.saveInt(module, "stalled", 1);

        // 23 ticks — below stall delay; counter increments
        for (int i = 0; i < 23; i++) {
            module.onTick(ctx);
        }

        int ticks = ctx.getInt(module, "ticks_since_action", 0);
        assertThat(ticks).isEqualTo(23);
    }

    @Test
    @DisplayName("onTick at tick 24 resets counter when stalled and no inventory")
    void onTick_resetsCounterAtStallDelay_noInventory() {
        ctx.saveInt(module, "stalled", 1);

        for (int i = 0; i < 24; i++) {
            module.onTick(ctx);
        }

        int ticks = ctx.getInt(module, "ticks_since_action", 0);
        assertThat(ticks).isEqualTo(0);
    }

    // ==================== Stalled defaults ====================

    @Test
    @DisplayName("stalled flag defaults to 0 (not stalled)")
    void stalled_defaultsToZero() {
        assertThat(ctx.getInt(module, "stalled", 0)).isEqualTo(0);
    }
}
