package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.test.FakePipeAccess;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Extractor Modules")
class ExtractorModulesTest {

    private FakePipeAccess access;
    private PipeContext ctx;

    @BeforeEach
    void setUp() {
        access = new FakePipeAccess();
        ctx = new PipeContext(null, BlockPos.ZERO, null, access);
    }

    // ==================== BasicExtractorModule ====================

    @Nested
    @DisplayName("BasicExtractorModule")
    class BasicExtractorModuleTests {

        private BasicExtractorModule module;

        @BeforeEach
        void setUp() {
            // 1 item every 100 ticks — safe to tick many times without triggering extraction
            module = new BasicExtractorModule(1, 100);
        }

        @Test
        @DisplayName("onTick increments counter when below ticksBetweenPulls")
        void onTick_incrementsCounter_belowInterval() {
            for (int i = 0; i < 5; i++) {
                module.onTick(ctx);
            }

            int ticks = ctx.getInt(module, "ticks_since_pull", 0);
            assertThat(ticks).isEqualTo(5);
        }

        @Test
        @DisplayName("onTick counter reaches 99 before the pull triggers")
        void onTick_counterReaches99BeforePull() {
            for (int i = 0; i < 99; i++) {
                module.onTick(ctx);
            }

            int ticks = ctx.getInt(module, "ticks_since_pull", 0);
            assertThat(ticks).isEqualTo(99);
        }

        @Test
        @DisplayName("onTick resets counter to 0 at ticksBetweenPulls when no direction is set")
        void onTick_resetsCounterAtInterval_noDirection() {
            // 100 ticks triggers extraction attempt, but no direction → no-op, counter resets
            for (int i = 0; i < 100; i++) {
                module.onTick(ctx);
            }

            int ticks = ctx.getInt(module, "ticks_since_pull", 0);
            assertThat(ticks).isEqualTo(0);
        }
    }

    // ==================== AdvancedExtractorModule ====================

    @Nested
    @DisplayName("AdvancedExtractorModule")
    class AdvancedExtractorModuleTests {

        private AdvancedExtractorModule module;

        @BeforeEach
        void setUp() {
            // 1 item every 100 ticks — safe to tick without triggering extraction
            module = new AdvancedExtractorModule(1, 100);
        }

        // ==================== Constants ====================

        @Test
        @DisplayName("MAX_FILTER_SLOTS is 9")
        void maxFilterSlots_isNine() {
            assertThat(AdvancedExtractorModule.MAX_FILTER_SLOTS).isEqualTo(9);
        }

        // ==================== Filter configuration ====================

        @Test
        @DisplayName("getFilterItems returns empty FilterSlots by default")
        void getFilterItems_emptyByDefault() {
            assertThat(module.getFilterItems(ctx).isEmpty()).isTrue();
        }

        @Test
        @DisplayName("setFilterItem persists item ID and is readable back")
        void setFilterItem_persistsAndReadsBack() {
            module.setFilterItem(ctx, 0, "minecraft:diamond");

            var filters = module.getFilterItems(ctx);
            assertThat(filters.asList()).contains("minecraft:diamond");
        }

        @Test
        @DisplayName("setFilterItem in multiple slots are independent")
        void setFilterItem_multipleSlotsAreIndependent() {
            module.setFilterItem(ctx, 0, "minecraft:diamond");
            module.setFilterItem(ctx, 4, "minecraft:iron_ingot");

            var filters = module.getFilterItems(ctx);
            assertThat(filters.asList()).contains("minecraft:diamond", "minecraft:iron_ingot");
        }

        @Test
        @DisplayName("setFilterItem with empty string clears the slot")
        void setFilterItem_emptyStringClearsSlot() {
            module.setFilterItem(ctx, 0, "minecraft:diamond");
            module.setFilterItem(ctx, 0, "");

            var filters = module.getFilterItems(ctx);
            assertThat(filters.asList().stream().filter(s -> !s.isEmpty())).isEmpty();
        }

        @Test
        @DisplayName("setFilterItem silently ignores out-of-bounds slot index")
        void setFilterItem_ignoresOutOfBoundsSlot() {
            assertThatCode(() -> module.setFilterItem(ctx, -1, "minecraft:diamond")).doesNotThrowAnyException();
            assertThatCode(() -> module.setFilterItem(ctx, AdvancedExtractorModule.MAX_FILTER_SLOTS, "minecraft:diamond")).doesNotThrowAnyException();
        }

        // ==================== Filter inversion ====================

        @Test
        @DisplayName("isFilterInverted defaults to false")
        void isFilterInverted_defaultsFalse() {
            assertThat(module.isFilterInverted(ctx)).isFalse();
        }

        @Test
        @DisplayName("setFilterInverted(true) makes isFilterInverted return true")
        void setFilterInverted_true_isInvertedReturnsTrue() {
            module.setFilterInverted(ctx, true);
            assertThat(module.isFilterInverted(ctx)).isTrue();
        }

        @Test
        @DisplayName("setFilterInverted(false) after true restores to false")
        void setFilterInverted_falseAfterTrue_restoresToFalse() {
            module.setFilterInverted(ctx, true);
            module.setFilterInverted(ctx, false);
            assertThat(module.isFilterInverted(ctx)).isFalse();
        }

        // ==================== Tick counter ====================

        @Test
        @DisplayName("onTick increments counter when below ticksBetweenPulls")
        void onTick_incrementsCounter_belowInterval() {
            for (int i = 0; i < 5; i++) {
                module.onTick(ctx);
            }

            int ticks = ctx.getInt(module, "ticks_since_pull", 0);
            assertThat(ticks).isEqualTo(5);
        }

        @Test
        @DisplayName("onTick resets counter to 0 at ticksBetweenPulls when no direction is set")
        void onTick_resetsCounterAtInterval_noDirection() {
            for (int i = 0; i < 100; i++) {
                module.onTick(ctx);
            }

            int ticks = ctx.getInt(module, "ticks_since_pull", 0);
            assertThat(ticks).isEqualTo(0);
        }
    }
}
