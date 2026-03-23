package com.logistics.pipe.modules;

import com.logistics.core.lib.pipe.PipeContext;
import com.logistics.test.FakePipeAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PipeMarkingModule")
class PipeMarkingModuleTest {

    private PipeMarkingModule module;
    private FakePipeAccess access;
    private PipeContext ctx;

    @BeforeEach
    void setUp() {
        module = new PipeMarkingModule();
        access = new FakePipeAccess();
        ctx = new PipeContext(null, BlockPos.ZERO, null, access);
    }

    // ==================== Constants ====================

    @Test
    @DisplayName("COLOR_KEY is 'pipe_color'")
    void colorKey_value() {
        assertThat(PipeMarkingModule.COLOR_KEY).isEqualTo("pipe_color");
    }

    // ==================== Default state ====================

    @Test
    @DisplayName("getStoredColor returns null when no color has been set")
    void getStoredColor_nullByDefault() {
        assertThat(module.getStoredColor(ctx)).isNull();
    }

    // ==================== NBT round-trip ====================

    @ParameterizedTest
    @DisplayName("getStoredColor reads the DyeColor stored by name in NBT")
    @EnumSource(DyeColor.class)
    void getStoredColor_readsFromNbt(DyeColor color) {
        ctx.saveString(module, PipeMarkingModule.COLOR_KEY, color.getName());
        assertThat(module.getStoredColor(ctx)).isEqualTo(color);
    }

    @Test
    @DisplayName("getStoredColor returns null for an unrecognized color name")
    void getStoredColor_unknownName_returnsNull() {
        ctx.saveString(module, PipeMarkingModule.COLOR_KEY, "not_a_color");
        assertThat(module.getStoredColor(ctx)).isNull();
    }

    @Test
    @DisplayName("getStoredColor returns null after color string is cleared")
    void getStoredColor_afterClear_returnsNull() {
        ctx.saveString(module, PipeMarkingModule.COLOR_KEY, DyeColor.RED.getName());
        ctx.remove(module, PipeMarkingModule.COLOR_KEY);
        assertThat(module.getStoredColor(ctx)).isNull();
    }
}
