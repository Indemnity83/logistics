package com.logistics.gametest.pipe;

import com.logistics.LogisticsCore;
import com.logistics.LogisticsPipe;
import com.logistics.core.lib.fluids.FluidUnits;
import com.logistics.core.lib.fluids.SimpleFluidKey;
import com.logistics.pipe.block.FluidPipeBlock;
import com.logistics.pipe.block.GlassTankBlock;
import com.logistics.pipe.block.entity.FluidPipeBlockEntity;
import com.logistics.pipe.block.entity.GlassTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Light-emitting fluids (issue #677): a pipe or tank holding lava or a glowing custom fluid drives its
 * block's {@code LIGHT_LEVEL} state, and stops glowing once drained.
 */
public class FluidLightGameTest {

    private static Fluid liquidGlowstone() {
        return BuiltInRegistries.FLUID.get(LogisticsCore.resource("liquid_glowstone").toIdentifier());
    }

    private static void assertTankLight(GameTestHelper context, BlockPos pos, Fluid fluid, long mb, int expected) {
        context.setBlock(pos, LogisticsPipe.BLOCK.GLASS_TANK);
        GlassTankBlockEntity tank = (GlassTankBlockEntity) context.getBlockEntity(pos);
        context.assertTrue(tank != null, "Glass tank should have a block entity");
        tank.tank().setContents(SimpleFluidKey.of(fluid), FluidUnits.mb(mb));
        context.succeedWhen(() -> context.assertBlockProperty(pos, GlassTankBlock.LIGHT_LEVEL, expected));
    }

    /** A tank of lava glows at 15. */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void tankOfLavaGlows(GameTestHelper context) {
        assertTankLight(context, new BlockPos(0, 1, 0), Fluids.LAVA, 1_000, 15);
    }

    /** A tank of a glowing custom fluid glows at its luminance. */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void tankOfLiquidGlowstoneGlows(GameTestHelper context) {
        assertTankLight(context, new BlockPos(0, 1, 0), liquidGlowstone(), 1_000, 15);
    }

    /** A tank of a non-glowing fluid stays dark. */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void tankOfWaterStaysDark(GameTestHelper context) {
        assertTankLight(context, new BlockPos(0, 1, 0), Fluids.WATER, 1_000, 0);
    }

    /** Draining a glowing tank turns its light back off. */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void drainedTankStopsGlowing(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPipe.BLOCK.GLASS_TANK);
        GlassTankBlockEntity tank = (GlassTankBlockEntity) context.getBlockEntity(pos);
        context.assertTrue(tank != null, "Glass tank should have a block entity");
        tank.tank().setContents(SimpleFluidKey.of(Fluids.LAVA), FluidUnits.mb(1_000));
        context.runAfterDelay(2, () -> {
            context.assertBlockProperty(pos, GlassTankBlock.LIGHT_LEVEL, 15);
            tank.tank().setContents(SimpleFluidKey.BLANK, 0);
            context.succeedWhen(() -> context.assertBlockProperty(pos, GlassTankBlock.LIGHT_LEVEL, 0));
        });
    }

    /** A pipe carrying a glowing fluid emits its light. */
    @GameTest(template = "fabric-gametest-api-v1:empty", timeoutTicks = 40)
    public void pipeOfLavaGlows(GameTestHelper context) {
        BlockPos pos = new BlockPos(0, 1, 0);
        context.setBlock(pos, LogisticsPipe.BLOCK.COPPER_FLUID_PIPE);
        FluidPipeBlockEntity pipe = (FluidPipeBlockEntity) context.getBlockEntity(pos);
        context.assertTrue(pipe != null, "Fluid pipe should have a block entity");
        pipe.fluidStorage(null).insert(SimpleFluidKey.of(Fluids.LAVA), FluidUnits.mb(100), false);
        context.succeedWhen(() -> context.assertBlockProperty(pos, FluidPipeBlock.LIGHT_LEVEL, 15));
    }
}
