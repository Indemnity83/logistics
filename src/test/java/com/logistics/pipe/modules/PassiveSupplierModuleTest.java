package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.test.FakePipeAccess;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PassiveSupplierModule")
class PassiveSupplierModuleTest {

    private PassiveSupplierModule module;
    private FakePipeAccess access;
    private PipeContext ctx;

    @BeforeEach
    void setUp() {
        module = new PassiveSupplierModule(3);
        access = new FakePipeAccess();
        ctx = new PipeContext(null, BlockPos.ZERO, null, access);
    }

    // ==================== Inherited config from SupplierModule ====================

    @Test
    @DisplayName("getSupplyConfigs returns empty list when nothing is configured")
    void getSupplyConfigs_emptyByDefault() {
        assertThat(module.getSupplyConfigs(ctx)).isEmpty();
    }

    @Test
    @DisplayName("setSupplyConfig/getSupplyConfigs round-trip is inherited from SupplierModule")
    void setSupplyConfig_persistsAndReadsBack() {
        module.setSupplyConfig(ctx, 0, "minecraft:diamond", 16);

        List<SupplierModule.SupplyConfig> configs = module.getSupplyConfigs(ctx);
        assertThat(configs).hasSize(1);
        assertThat(configs.get(0).itemId()).isEqualTo("minecraft:diamond");
        assertThat(configs.get(0).amount()).isEqualTo(16);
    }

    @Test
    @DisplayName("getMode defaults to PARTIAL (inherited)")
    void getMode_defaultsToPartial() {
        assertThat(module.getMode(ctx)).isEqualTo(SupplierModule.SupplyMode.PARTIAL);
    }

    // ==================== Tick counter ====================

    @Test
    @DisplayName("onTick increments counter when below SYNC_INTERVAL (20)")
    void onTick_incrementsCounter_belowInterval() {
        for (int i = 0; i < 5; i++) {
            module.onTick(ctx);
        }

        int ticks = ctx.getInt(module, "ticks_since_sync", 0);
        assertThat(ticks).isEqualTo(5);
    }

    @Test
    @DisplayName("onTick counter reaches 19 before sync trigger")
    void onTick_counterReaches19BeforeTrigger() {
        for (int i = 0; i < 19; i++) {
            module.onTick(ctx);
        }

        int ticks = ctx.getInt(module, "ticks_since_sync", 0);
        assertThat(ticks).isEqualTo(19);
    }

    @Test
    @DisplayName("onTick resets counter at tick 20 when network is null")
    void onTick_resetsCounterAtInterval_nullNetwork() {
        // tick 20 calls syncInterests which checks ctx.network() — null → no-op, counter resets
        for (int i = 0; i < 20; i++) {
            module.onTick(ctx);
        }

        int ticks = ctx.getInt(module, "ticks_since_sync", 0);
        assertThat(ticks).isEqualTo(0);
    }
}
