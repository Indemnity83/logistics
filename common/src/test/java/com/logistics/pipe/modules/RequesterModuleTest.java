package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.test.FakePipeAccess;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("RequesterModule")
class RequesterModuleTest {

    private RequesterModule module;
    private FakePipeAccess access;
    private PipeContext ctx;

    @BeforeEach
    void setUp() {
        module = new RequesterModule();
        access = new FakePipeAccess();
        ctx = new PipeContext(null, BlockPos.ZERO, null, access);
    }

    // ==================== Request config — read ====================

    @Test
    @DisplayName("getRequestConfigs returns empty list when nothing is configured")
    void getRequestConfigs_emptyByDefault() {
        assertThat(module.getRequestConfigs(ctx)).isEmpty();
    }

    // ==================== Request config — write/read round-trip ====================

    @Test
    @DisplayName("setRequestConfig persists and can be read back")
    void setRequestConfig_persistsAndReadsBack() {
        module.setRequestConfig(ctx, 0, "minecraft:diamond", 16);

        List<RequesterModule.RequestConfig> configs = module.getRequestConfigs(ctx);
        assertThat(configs).hasSize(1);
        assertThat(configs.get(0).itemId()).isEqualTo("minecraft:diamond");
        assertThat(configs.get(0).amount()).isEqualTo(16);
    }

    @Test
    @DisplayName("setRequestConfig in multiple slots are independent")
    void setRequestConfig_multipleSlotsAreIndependent() {
        module.setRequestConfig(ctx, 0, "minecraft:diamond", 8);
        module.setRequestConfig(ctx, 4, "minecraft:iron_ingot", 64);

        List<RequesterModule.RequestConfig> configs = module.getRequestConfigs(ctx);
        assertThat(configs).hasSize(2);
        assertThat(configs).anyMatch(c -> c.itemId().equals("minecraft:diamond") && c.amount() == 8);
        assertThat(configs).anyMatch(c -> c.itemId().equals("minecraft:iron_ingot") && c.amount() == 64);
    }

    @Test
    @DisplayName("setRequestConfig clears a slot when amount is 0")
    void setRequestConfig_clearSlotWithZeroAmount() {
        module.setRequestConfig(ctx, 0, "minecraft:diamond", 16);
        module.setRequestConfig(ctx, 0, "minecraft:diamond", 0);

        assertThat(module.getRequestConfigs(ctx)).isEmpty();
    }

    @Test
    @DisplayName("setRequestConfig clears a slot when itemId is empty")
    void setRequestConfig_clearSlotWithEmptyId() {
        module.setRequestConfig(ctx, 0, "minecraft:diamond", 16);
        module.setRequestConfig(ctx, 0, "", 16);

        assertThat(module.getRequestConfigs(ctx)).isEmpty();
    }

    @Test
    @DisplayName("setRequestConfig clamps amount to MAX_REQUEST_AMOUNT")
    void setRequestConfig_clampsToMaxAmount() {
        int overLimit = RequesterModule.MAX_REQUEST_AMOUNT + 100;
        module.setRequestConfig(ctx, 0, "minecraft:diamond", overLimit);

        List<RequesterModule.RequestConfig> configs = module.getRequestConfigs(ctx);
        assertThat(configs.get(0).amount()).isEqualTo(RequesterModule.MAX_REQUEST_AMOUNT);
    }

    @Test
    @DisplayName("setRequestConfig accepts exactly MAX_REQUEST_AMOUNT")
    void setRequestConfig_acceptsExactlyMaxAmount() {
        module.setRequestConfig(ctx, 0, "minecraft:diamond", RequesterModule.MAX_REQUEST_AMOUNT);

        assertThat(module.getRequestConfigs(ctx).get(0).amount())
                .isEqualTo(RequesterModule.MAX_REQUEST_AMOUNT);
    }

    @Test
    @DisplayName("setRequestConfig throws for negative slot index")
    void setRequestConfig_rejectsNegativeSlot() {
        assertThatThrownBy(() -> module.setRequestConfig(ctx, -1, "minecraft:diamond", 16))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("setRequestConfig throws for slot >= MAX_REQUEST_SLOTS")
    void setRequestConfig_rejectsSlotAtOrBeyondMax() {
        assertThatThrownBy(() -> module.setRequestConfig(ctx, RequesterModule.MAX_REQUEST_SLOTS, "minecraft:diamond", 16))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ==================== Constants ====================

    @Test
    @DisplayName("MAX_REQUEST_SLOTS is 9")
    void maxRequestSlots_isNine() {
        assertThat(RequesterModule.MAX_REQUEST_SLOTS).isEqualTo(9);
    }

    @Test
    @DisplayName("MAX_REQUEST_AMOUNT is 576 (9 stacks)")
    void maxRequestAmount_is576() {
        assertThat(RequesterModule.MAX_REQUEST_AMOUNT).isEqualTo(576);
    }

    // ==================== Tick counter ====================

    @Test
    @DisplayName("onTick increments the tick counter without triggering processRequests")
    void onTick_incrementsCounter() {
        // Call fewer than REQUEST_INTERVAL (20) ticks so processRequests (which needs world) is not triggered
        for (int i = 0; i < 5; i++) {
            module.onTick(ctx);
        }

        int ticks = ctx.getInt(module, "ticks_since_request", 0);
        assertThat(ticks).isEqualTo(5);
    }

    @Test
    @DisplayName("onTick counter starts at 0 and reaches 19 before reset trigger")
    void onTick_counterReaches19BeforeReset() {
        // After 19 ticks counter should be 19 (reset happens at tick 20)
        for (int i = 0; i < 19; i++) {
            module.onTick(ctx);
        }

        int ticks = ctx.getInt(module, "ticks_since_request", 0);
        assertThat(ticks).isEqualTo(19);
    }

    // ==================== RequestConfig record ====================

    @Test
    @DisplayName("RequestConfig record stores itemId and amount")
    void requestConfig_storesFields() {
        var config = new RequesterModule.RequestConfig("minecraft:diamond", 16);
        assertThat(config.itemId()).isEqualTo("minecraft:diamond");
        assertThat(config.amount()).isEqualTo(16);
    }
}
