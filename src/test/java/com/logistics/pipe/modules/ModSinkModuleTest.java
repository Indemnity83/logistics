package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.test.FakePipeAccess;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ModSinkModule")
class ModSinkModuleTest {

    private ModSinkModule module;
    private FakePipeAccess access;
    private PipeContext ctx;

    @BeforeEach
    void setUp() {
        module = new ModSinkModule(5);
        access = new FakePipeAccess();
        ctx = new PipeContext(null, BlockPos.ZERO, null, access);
    }

    // ==================== Constants ====================

    @Test
    @DisplayName("MAX_FILTERS is 10")
    void maxFilters_isTen() {
        assertThat(ModSinkModule.MAX_FILTERS).isEqualTo(10);
    }

    @Test
    @DisplayName("MOD_IDS constant is 'mod_ids'")
    void modIds_constant() {
        assertThat(ModSinkModule.MOD_IDS).isEqualTo("mod_ids");
    }

    // ==================== getFilterItemIds — defaults ====================

    @Test
    @DisplayName("getFilterItemIds returns array of length MAX_FILTERS")
    void getFilterItemIds_returnsCorrectLength() {
        assertThat(module.getFilterItemIds(ctx)).hasSize(ModSinkModule.MAX_FILTERS);
    }

    @Test
    @DisplayName("getFilterItemIds returns all empty strings when nothing is configured")
    void getFilterItemIds_allEmptyByDefault() {
        String[] ids = module.getFilterItemIds(ctx);
        for (String id : ids) {
            assertThat(id).isEmpty();
        }
    }

    // ==================== getFilterItemIds — after direct NBT write ====================

    @Test
    @DisplayName("getFilterItemIds reads item IDs stored directly in NBT")
    void getFilterItemIds_readsFromNbt() {
        ctx.saveString(module, ModSinkModule.MOD_IDS + "_0", "minecraft:diamond");
        ctx.saveString(module, ModSinkModule.MOD_IDS + "_3", "create:iron_sheet");

        String[] ids = module.getFilterItemIds(ctx);
        assertThat(ids[0]).isEqualTo("minecraft:diamond");
        assertThat(ids[3]).isEqualTo("create:iron_sheet");
        assertThat(ids[1]).isEmpty();
        assertThat(ids[2]).isEmpty();
    }

    // ==================== removeModId ====================

    @Test
    @DisplayName("getFilterItemIds reads back manually-written mod IDs")
    void getFilterItemIds_readsBackManuallyWrittenIds() {
        // addModId/removeModId call ctx.world().isClientSide() and are unsafe with null world.
        // Populate via direct NBT ops and verify the read API only; full removeModId behavior
        // is covered in integration tests.
        ctx.saveString(module, ModSinkModule.MOD_IDS + "_0", "minecraft:diamond");
        ctx.saveString(module, ModSinkModule.MOD_IDS + "_1", "create:iron_sheet");

        String[] ids = module.getFilterItemIds(ctx);
        assertThat(ids[0]).isEqualTo("minecraft:diamond");
        assertThat(ids[1]).isEqualTo("create:iron_sheet");
    }

    @Test
    @DisplayName("getFilterItemIds returns empty slots as empty strings even after partial population")
    void getFilterItemIds_emptySlotsBetweenPopulated() {
        ctx.saveString(module, ModSinkModule.MOD_IDS + "_0", "minecraft:diamond");
        ctx.saveString(module, ModSinkModule.MOD_IDS + "_9", "somemod:somethingelse");

        String[] ids = module.getFilterItemIds(ctx);
        assertThat(ids[0]).isEqualTo("minecraft:diamond");
        assertThat(ids[9]).isEqualTo("somemod:somethingelse");
        // Slots 1-8 are empty
        for (int i = 1; i < 9; i++) {
            assertThat(ids[i]).isEmpty();
        }
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
    @DisplayName("onTick counter reaches 19 before sync triggers")
    void onTick_counterReaches19BeforeTrigger() {
        for (int i = 0; i < 19; i++) {
            module.onTick(ctx);
        }

        int ticks = ctx.getInt(module, "ticks_since_sync", 0);
        assertThat(ticks).isEqualTo(19);
    }
}
