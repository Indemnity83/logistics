package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.test.FakePipeAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ExtractionModule")
class ExtractionModuleTest {

    private ExtractionModule module;
    private FakePipeAccess access;
    private PipeContext ctx;

    @BeforeEach
    void setUp() {
        module = new ExtractionModule();
        access = new FakePipeAccess();
        ctx = new PipeContext(null, BlockPos.ZERO, null, access);
    }

    // ==================== acceptsLowTierEnergyFrom ====================

    @ParameterizedTest
    @DisplayName("acceptsLowTierEnergyFrom returns true for every direction")
    @EnumSource(Direction.class)
    void acceptsLowTierEnergyFrom_returnsTrue_allDirections(Direction direction) {
        assertThat(module.acceptsLowTierEnergyFrom(ctx, direction)).isTrue();
    }

    // ==================== Tick counter ====================

    @Test
    @DisplayName("onTick increments counter for ticks 1-7 (cooldown — never extracts)")
    void onTick_incrementsCounter_duringCooldown() {
        for (int i = 0; i < 7; i++) {
            module.onTick(ctx);
        }

        int ticks = ctx.getInt(module, "ticks_since_pull", 0);
        assertThat(ticks).isEqualTo(7);
    }

    @Test
    @DisplayName("onTick counter continues incrementing at tick 8-15 when no inventory direction is set")
    void onTick_continuesIncrementing_ticks8to15_noDirection() {
        // At ticks 8-15: shouldExtract returns true (energy=0 >= 0*RF/item),
        // but autoSelectDirection returns null (no connections) → early return, no reset.
        for (int i = 0; i < 15; i++) {
            module.onTick(ctx);
        }

        int ticks = ctx.getInt(module, "ticks_since_pull", 0);
        assertThat(ticks).isEqualTo(15);
    }

    @Test
    @DisplayName("onTick counter continues incrementing at tick 16+ when energy is 0")
    void onTick_continuesIncrementing_tick16plus_zeroEnergy() {
        // At ticks 16+: shouldExtract returns false (0 < RF_PER_ITEM=10) → early return, no reset.
        for (int i = 0; i < 20; i++) {
            module.onTick(ctx);
        }

        int ticks = ctx.getInt(module, "ticks_since_pull", 0);
        assertThat(ticks).isEqualTo(20);
    }
}
